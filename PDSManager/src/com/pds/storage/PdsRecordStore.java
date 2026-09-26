package com.pds.storage;

import com.pds.crypto.CryptoService;
import com.pds.model.PdsRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Local, encrypted-at-rest data store for PdsRecord objects.
 *
 * Layout under the data directory:
 *   config.dat            - PBKDF2 salt + salted verifier hash (never the password itself)
 *   records/<id>.pds       - one AES-256-GCM encrypted record per file
 *   records/<id>.pds.bak   - previous version, kept for crash recovery
 *   deleted/<id>-<ts>.pds  - soft-deleted records (audit trail; not shown in the app)
 *
 * Writes are atomic (write to *.tmp, fsync-equivalent flush, then ATOMIC_MOVE)
 * so a crash mid-save cannot corrupt a record - worst case the old version (or
 * the .bak) survives untouched.
 */
public class PdsRecordStore {

    private final Path dataDir;
    private final Path recordsDir;
    private final Path deletedDir;
    private final Path configFile;
    private final CryptoService crypto = new CryptoService();
    private char[] password; // held only while unlocked, cleared on lock()

    public PdsRecordStore(Path dataDir) {
        this.dataDir = dataDir;
        this.recordsDir = dataDir.resolve("records");
        this.deletedDir = dataDir.resolve("deleted");
        this.configFile = dataDir.resolve("config.dat");
    }

    public boolean isInitialized() { return Files.exists(configFile); }

    public void initialize(char[] newPassword) throws IOException {
        Files.createDirectories(recordsDir);
        Files.createDirectories(deletedDir);
        byte[] salt = crypto.newSalt();
        byte[] verifier = crypto.makeVerifier(newPassword, salt);
        String content = base64(salt) + "\n" + base64(verifier) + "\n";
        Files.write(configFile, content.getBytes(StandardCharsets.UTF_8));
        this.password = newPassword.clone();
    }

    /** @return true if the password matched and the store is now unlocked. */
    public boolean unlock(char[] candidatePassword) throws IOException {
        List<String> lines = Files.readAllLines(configFile, StandardCharsets.UTF_8);
        byte[] salt = unbase64(lines.get(0));
        byte[] storedVerifier = unbase64(lines.get(1));
        byte[] candidateVerifier = crypto.makeVerifier(candidatePassword, salt);
        if (crypto.constantTimeEquals(storedVerifier, candidateVerifier)) {
            this.password = candidatePassword.clone();
            return true;
        }
        return false;
    }

    public void lock() {
        if (password != null) Arrays.fill(password, '\0');
        password = null;
    }

    public boolean isUnlocked() { return password != null; }

    /**
     * Re-encrypts every record under a new master password. To avoid ever
     * leaving the store in a half-migrated state (some records under the old
     * key, some under the new, if a failure happens mid-way), every record is
     * first re-encrypted **in memory only**; disk is touched only after all
     * of them have succeeded, and only then is the new verifier committed.
     */
    public void changePassword(char[] oldPassword, char[] newPassword) throws IOException {
        if (!unlock(oldPassword)) throw new SecurityException("Current password is incorrect.");
        List<PdsRecord> all = loadAll().records;

        // Phase 1: encrypt everything with the new password, entirely in memory.
        Map<String, byte[]> reEncrypted = new LinkedHashMap<>();
        for (PdsRecord r : all) {
            reEncrypted.put(r.id, encryptRecordBytes(r, newPassword));
        }
        byte[] salt = crypto.newSalt();
        byte[] verifier = crypto.makeVerifier(newPassword, salt);
        String configContent = base64(salt) + "\n" + base64(verifier) + "\n";

        // Phase 2: everything succeeded above - now commit to disk.
        for (Map.Entry<String, byte[]> e : reEncrypted.entrySet()) {
            writeEncryptedAtomic(e.getKey(), e.getValue());
        }
        Files.write(configFile, configContent.getBytes(StandardCharsets.UTF_8));

        char[] prevPassword = password;
        password = newPassword.clone();
        Arrays.fill(prevPassword, '\0');
    }

    public static class LoadResult {
        public final List<PdsRecord> records = new ArrayList<>();
        public final List<String> corruptedFiles = new ArrayList<>();
    }

    public LoadResult loadAll() throws IOException {
        requireUnlocked();
        LoadResult result = new LoadResult();
        if (!Files.exists(recordsDir)) return result;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(recordsDir, "*.pds")) {
            for (Path p : stream) {
                try {
                    result.records.add(readRecordFile(p));
                } catch (Exception e) {
                    result.corruptedFiles.add(p.getFileName().toString() + " (" + e.getMessage() + ")");
                }
            }
        }
        return result;
    }

    public PdsRecord load(String id) throws IOException {
        requireUnlocked();
        return readRecordFile(recordsDir.resolve(id + ".pds"));
    }

    private PdsRecord readRecordFile(Path p) throws IOException {
        byte[] enc = Files.readAllBytes(p);
        byte[] plain = crypto.decrypt(enc, password);
        Map<String, String> map = ObjectTextCodec.fromText(CryptoService.utf8(plain));
        return ObjectTextCodec.decode(map, PdsRecord.class);
    }

    /** Generous but finite cap on a single record's serialized size - guards against
     *  runaway memory/disk use from pathological input (e.g. a scripted/corrupted
     *  import), while comfortably fitting a real PDS with its 2x2 photo AND several
     *  scanned proof-of-document attachments (each capped at 5 MB raw before the
     *  ~33% Base64 overhead - see DynamicTablePanel.MAX_ATTACHMENT_BYTES). */
    private static final int MAX_RECORD_PLAINTEXT_BYTES = 80 * 1024 * 1024; // 80 MB

    /** Atomic, backed-up save: temp file -> fsync -> rename; previous version preserved as .bak. */
    public void save(PdsRecord record) throws IOException {
        requireUnlocked();
        if (record.id == null || record.id.isEmpty()) {
            throw new IllegalArgumentException("Record must have an id before saving.");
        }
        record.updatedAt = Instant.now().toString();
        if (record.createdAt == null || record.createdAt.isEmpty()) record.createdAt = record.updatedAt;

        byte[] enc = encryptRecordBytes(record, password);
        writeEncryptedAtomic(record.id, enc);
    }

    /** Encodes + encrypts a record to bytes without touching disk. */
    private byte[] encryptRecordBytes(PdsRecord record, char[] withPassword) throws IOException {
        Map<String, String> map = ObjectTextCodec.encode(record);
        byte[] plain = CryptoService.utf8(ObjectTextCodec.toText(map));
        if (plain.length > MAX_RECORD_PLAINTEXT_BYTES) {
            throw new IOException("Record is too large to save (" + plain.length + " bytes, limit "
                    + MAX_RECORD_PLAINTEXT_BYTES + "). Check for an oversized attachment or pasted content.");
        }
        return crypto.encrypt(plain, withPassword);
    }

    /** Writes already-encrypted bytes for a given record id atomically, keeping a .bak of the previous version. */
    private void writeEncryptedAtomic(String id, byte[] enc) throws IOException {
        Path target = recordsDir.resolve(id + ".pds");
        Path tmp = recordsDir.resolve(id + ".pds.tmp");
        Path bak = recordsDir.resolve(id + ".pds.bak");

        Files.createDirectories(recordsDir);
        try (var ch = Files.newByteChannel(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(java.nio.ByteBuffer.wrap(enc));
            if (ch instanceof java.nio.channels.FileChannel) ((java.nio.channels.FileChannel) ch).force(true);
        }
        if (Files.exists(target)) {
            Files.copy(target, bak, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Soft delete: moves the encrypted file to deleted/ with a timestamp, rather than destroying it outright. */
    public void delete(String id) throws IOException {
        requireUnlocked();
        Path target = recordsDir.resolve(id + ".pds");
        if (!Files.exists(target)) return;
        Files.createDirectories(deletedDir);
        Path dest = deletedDir.resolve(id + "-" + System.currentTimeMillis() + ".pds");
        Files.move(target, dest, StandardCopyOption.REPLACE_EXISTING);
        Path bak = recordsDir.resolve(id + ".pds.bak");
        Files.deleteIfExists(bak);
    }

    /** Path to a record's on-disk encrypted file - used for raw file export. */
    public Path recordFilePath(String id) { return recordsDir.resolve(id + ".pds"); }

    /**
     * Imports an externally-supplied encrypted .pdsrecord file (e.g. from
     * {@link #recordFilePath}/export on another machine using the SAME master
     * password). The record is assigned a fresh id so it can never collide
     * with - or silently overwrite - an existing record, then persisted into
     * this store.
     */
    public PdsRecord importRecordFile(Path externalFile) throws IOException {
        requireUnlocked();
        byte[] enc = Files.readAllBytes(externalFile);
        byte[] plain = crypto.decrypt(enc, password);
        Map<String, String> map = ObjectTextCodec.fromText(CryptoService.utf8(plain));
        PdsRecord record = ObjectTextCodec.decode(map, PdsRecord.class);
        record.id = com.pds.util.IdGen.newId();
        save(record);
        return record;
    }

    private void requireUnlocked() {
        if (password == null) throw new IllegalStateException("Data store is locked.");
    }

    private static String base64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    private static byte[] unbase64(String s) { return Base64.getDecoder().decode(s); }
}
