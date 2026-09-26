package com.pds;

import com.pds.crypto.CryptoService;
import com.pds.model.*;
import com.pds.storage.ObjectTextCodec;
import com.pds.storage.PdsRecordStore;
import com.pds.util.IdGen;
import com.pds.util.Validator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A dependency-free smoke test suite. Run via `ant test`. Exits with a
 * non-zero status (and Ant fails the build) on the first failed assertion.
 */
public class SelfTest {

    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        testCryptoRoundTrip();
        testCryptoWrongPasswordFails();
        testCryptoTamperDetected();
        testCodecRoundTrip();
        testStoreCrudLifecycle();
        testValidator();
        testLargeAttachmentsFitWithinGuard();
        System.out.println("\nSelfTest: ALL " + checks + " CHECKS PASSED");
    }

    private static void testCryptoRoundTrip() {
        CryptoService crypto = new CryptoService();
        byte[] plain = "Hello PDS Manager! \u00f1\u00e9\u00fc".getBytes(StandardCharsets.UTF_8);
        char[] pw = "correct horse battery staple".toCharArray();
        byte[] enc = crypto.encrypt(plain, pw);
        byte[] dec = crypto.decrypt(enc, pw);
        assertTrue("crypto round-trip", java.util.Arrays.equals(plain, dec));
    }

    private static void testCryptoWrongPasswordFails() {
        CryptoService crypto = new CryptoService();
        byte[] plain = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] enc = crypto.encrypt(plain, "rightpassword".toCharArray());
        boolean threw = false;
        try {
            crypto.decrypt(enc, "wrongpassword".toCharArray());
        } catch (SecurityException e) {
            threw = true;
        }
        assertTrue("wrong password rejected", threw);
    }

    private static void testCryptoTamperDetected() {
        CryptoService crypto = new CryptoService();
        byte[] plain = "untampered".getBytes(StandardCharsets.UTF_8);
        char[] pw = "pw123456".toCharArray();
        byte[] enc = crypto.encrypt(plain, pw);
        enc[enc.length - 1] ^= 0x01; // flip a bit in the auth tag / ciphertext
        boolean threw = false;
        try {
            crypto.decrypt(enc, pw);
        } catch (SecurityException e) {
            threw = true;
        }
        assertTrue("tampering detected", threw);
    }

    private static void testCodecRoundTrip() {
        PdsRecord r = sampleRecord();
        Map<String, String> map = ObjectTextCodec.encode(r);
        String text = ObjectTextCodec.toText(map);
        Map<String, String> parsed = ObjectTextCodec.fromText(text);
        PdsRecord r2 = ObjectTextCodec.decode(parsed, PdsRecord.class);

        assertEquals("id round-trip", r.id, r2.id);
        assertEquals("surname round-trip", r.personalInfo.surname, r2.personalInfo.surname);
        assertEquals("status round-trip", r.status, r2.status);
        assertEquals("children count round-trip", r.familyBackground.children.size(), r2.familyBackground.children.size());
        assertEquals("child name round-trip", r.familyBackground.children.get(0).fullName, r2.familyBackground.children.get(0).fullName);
        assertEquals("education count round-trip", r.education.size(), r2.education.size());
        assertEquals("questionnaire yes/no round-trip", r.questionnaire.q34a3rdDegree.answer, r2.questionnaire.q34a3rdDegree.answer);
        assertEquals("photo base64 round-trip", r.personalInfo.photoBase64, r2.personalInfo.photoBase64);
        assertEquals("dual citizenship country round-trip", r.personalInfo.dualCitizenshipCountry, r2.personalInfo.dualCitizenshipCountry);
        assertEquals("education attachment filename round-trip", r.education.get(0).attachmentFileName, r2.education.get(0).attachmentFileName);
        assertEquals("education attachment bytes round-trip", r.education.get(0).attachmentBase64, r2.education.get(0).attachmentBase64);
        assertEquals("distinction count round-trip", r.otherInfo.nonAcademicDistinctions.size(), r2.otherInfo.nonAcademicDistinctions.size());
        assertEquals("distinction text round-trip", r.otherInfo.nonAcademicDistinctions.get(0).text, r2.otherInfo.nonAcademicDistinctions.get(0).text);
        assertEquals("distinction attachment round-trip", r.otherInfo.nonAcademicDistinctions.get(0).attachmentBase64, r2.otherInfo.nonAcademicDistinctions.get(0).attachmentBase64);
        assertEquals("notary public name round-trip", r.notarization.notaryPublicName, r2.notarization.notaryPublicName);
        assertEquals("notary attachment round-trip", r.notarization.attachmentBase64, r2.notarization.attachmentBase64);
    }

    private static void testStoreCrudLifecycle() throws Exception {
        Path tmp = Files.createTempDirectory("pdsmanager-selftest");
        try {
            PdsRecordStore store = new PdsRecordStore(tmp);
            char[] pw = "unittestpassword".toCharArray();
            assertTrue("not initialized yet", !store.isInitialized());
            store.initialize(pw);
            assertTrue("initialized now", store.isInitialized());

            PdsRecord r = sampleRecord();
            store.save(r);

            PdsRecordStore.LoadResult loaded = store.loadAll();
            assertEquals("one record loaded", 1, loaded.records.size());
            assertTrue("no corrupted files", loaded.corruptedFiles.isEmpty());
            assertEquals("loaded surname matches", r.personalInfo.surname, loaded.records.get(0).personalInfo.surname);

            r.personalInfo.surname = "UPDATED";
            store.save(r);
            PdsRecord reloaded = store.load(r.id);
            assertEquals("update persisted", "UPDATED", reloaded.personalInfo.surname);

            store.delete(r.id);
            PdsRecordStore.LoadResult afterDelete = store.loadAll();
            assertEquals("record gone after delete", 0, afterDelete.records.size());

            // wrong-password unlock must fail, correct one must succeed
            PdsRecordStore store2 = new PdsRecordStore(tmp);
            assertTrue("wrong pw rejected on reopen", !store2.unlock("nope-not-it".toCharArray()));
            assertTrue("correct pw accepted on reopen", store2.unlock(pw));
        } finally {
            deleteRecursively(tmp);
        }
    }

    private static void testValidator() {
        PdsRecord empty = new PdsRecord(IdGen.newId());
        List<String> errors = Validator.validateForCompletion(empty);
        assertTrue("empty record has validation errors", !errors.isEmpty());

        PdsRecord full = sampleRecord();
        List<String> errors2 = Validator.validateForCompletion(full);
        assertTrue("filled sample record has no validation errors", errors2.isEmpty());
    }

    /** Simulates several near-max-size (5 MB raw / ~6.7 MB Base64) attachments on one
     *  record, to prove the record-level size guard was actually raised to accommodate
     *  the attachment feature (it used to be 8 MB total, which even two attachments
     *  would have blown past). */
    private static void testLargeAttachmentsFitWithinGuard() throws Exception {
        Path tmp = Files.createTempDirectory("pdsmanager-selftest-attach");
        try {
            PdsRecordStore store = new PdsRecordStore(tmp);
            char[] pw = "attachtestpassword".toCharArray();
            store.initialize(pw);

            PdsRecord r = sampleRecord();
            String fakeBase64 = "A".repeat(7_000_000); // ~7 MB of Base64 text, close to one real 5 MB file's encoded size
            for (int i = 0; i < 3; i++) {
                EducationEntry e = new EducationEntry("COLLEGE", "SCHOOL " + i, "COURSE", "2000", "2004", "GRAD", "2004", "");
                e.attachmentFileName = "proof" + i + ".pdf";
                e.attachmentBase64 = fakeBase64;
                r.education.add(e);
            }
            store.save(r); // must NOT throw the "record too large" guard for a realistic multi-attachment record
            PdsRecord reloaded = store.load(r.id);
            assertEquals("large-attachment record reloads with same number of education entries",
                    r.education.size(), reloaded.education.size());
            assertEquals("large attachment bytes survive round-trip", fakeBase64, reloaded.education.get(1).attachmentBase64);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private static PdsRecord sampleRecord() {
        PdsRecord r = new PdsRecord(IdGen.newId());
        r.status = RecordStatus.ONGOING;
        r.personalInfo.surname = "DELA CRUZ";
        r.personalInfo.firstName = "JUAN";
        r.personalInfo.dateOfBirth = "01/15/1990";
        r.personalInfo.placeOfBirth = "MANILA";
        r.personalInfo.sexAtBirth = "Male";
        r.personalInfo.civilStatus = "Single";
        r.personalInfo.resCityMunicipality = "QUEZON CITY";
        r.personalInfo.permCityMunicipality = "QUEZON CITY";
        r.personalInfo.emailAddress = "juan.delacruz@example.com";
        r.familyBackground.children.add(new Child("MARIA DELA CRUZ", "05/20/2015"));
        EducationEntry e = new EducationEntry("COLLEGE", "SAMPLE UNIVERSITY", "BS COMPUTER SCIENCE",
                "2008", "2012", "COLLEGE GRADUATE", "2012", "NONE");
        r.education.add(e);
        r.questionnaire.q34a3rdDegree.answer = Boolean.FALSE;
        r.personalInfo.citizenDual = true;
        r.personalInfo.dualCitizenshipCountry = "United States";
        r.personalInfo.photoBase64 = "/9j/4AAQSkZJRgABAQEAYABgAAD=="; // fake sample bytes, just exercising the codec
        e.attachmentFileName = "diploma.pdf";
        e.attachmentBase64 = "JVBERi0xLjQKZmFrZQ=="; // fake sample bytes
        AttachableText distinction = new AttachableText("Employee of the Year 2020");
        distinction.attachmentFileName = "award.jpg";
        distinction.attachmentBase64 = "ZmFrZWJ5dGVz";
        r.otherInfo.nonAcademicDistinctions.add(distinction);
        r.notarization.notaryPublicName = "Atty. Juan Dela Cruz";
        r.notarization.notaryCommissionNo = "N-2026-001";
        r.notarization.attachmentFileName = "notarized-pds.pdf";
        r.notarization.attachmentBase64 = "ZmFrZS1ub3RhcnktcGRm";
        return r;
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) return;
        Files.walk(path).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
            try { Files.delete(p); } catch (Exception ignored) {}
        });
    }

    private static void assertTrue(String label, boolean condition) {
        checks++;
        if (!condition) {
            System.err.println("FAILED: " + label);
            throw new AssertionError("SelfTest failed: " + label);
        }
        System.out.println("ok - " + label);
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        assertTrue(label + " (expected=" + expected + ", actual=" + actual + ")",
                java.util.Objects.equals(expected, actual));
    }
}
