# PDS Manager

A desktop application for encoding, storing, printing and reporting on the Philippine Civil Service Commission **Personal Data Sheet** (CS Form No. 212, Revised 2026). Records are encrypted on disk with a master password, and the app runs fully offline.

Built with **Java Swing**, **NetBeans** and **Ant**, using **only the JDK** (`javax.swing`, `javax.crypto`, `java.awt.print`). There are no third-party libraries.

<!--
Add screenshots after you upload them to a docs/ folder, then remove these comment markers:
![Dashboard](docs/dashboard.png)
![Record editor](docs/record-editor.png)
-->

## Features

- **Complete PDS coverage:** personal information, family background (including children), educational background, civil service eligibility, work experience, learning and development, voluntary work, other information, the background questionnaire, references and government ID.
- **Full record management:** create, edit, save and delete. Deletes are soft: records move to a `deleted/` folder and can be recovered.
- **Dynamic rows:** `+` / `-` controls on every repeating section, powered by a shared table component.
- **Keyboard-first data entry:** Tab and Shift+Tab move between fields and table cells, and Tab on the last cell of a table adds a new row.
- **Citizenship logic:** Item 16 branches between Filipino and dual citizenship, with a country dropdown.
- **Attachments:** a 2x2 ID photo (auto-cropped to a square), per-row proof attachments, and a central Attachments tab to view or print every scanned document.
- **Notarization tab:** jurat fields plus a slot for the scanned electronic notary document.
- **Dashboard:** live counts of Ongoing, Completed and Cancelled records, with recent activity.
- **Reports and summary:** aggregate statistics by status, sex and civil status, with printing.
- **Search, import and export:** live search by name, status or remarks; CSV export of the record list; encrypted single-record export and import (`.pdsrecord`).
- **Printing:** paginated, formatted 5-page output through the standard Java print dialog, including a certification page and notary acknowledgment.

## Security design

- **Encryption at rest:** every record file uses **AES-256-GCM**. The key is derived from your master password with **PBKDF2-HMAC-SHA256 (200,000 iterations)**. The password itself is never written to disk, only a salted verifier.
- **Tamper detection:** each save uses a fresh random salt and IV, and the GCM tag makes corruption or tampering detectable on load.
- **Safe writes:** saves write to a temporary file, then move atomically into place, keeping the previous version as a `.bak`.
- **Safe password change:** records are re-encrypted in memory first, and the new verifier is committed only after every record succeeds.
- **Small attack surface:** no SQL, no network calls, and no deserialization of untrusted objects. The text codec only fills known classes in `com.pds.model`.
- **No recovery:** there is no password reset. If the master password is lost, the data cannot be read.

Data is stored in `~/PDSManagerData` in the user's home directory. The repository contains no personal records.

## Getting started

**Requirements:** JDK 17 or newer. NetBeans is optional.

**NetBeans:** choose *File > Open Project*, select the `PDSManager` folder, then right-click the project and choose *Run*.

**Command line:**

```bash
cd PDSManager
ant run            # compile and launch
ant jar            # build dist/PDSManager.jar
ant test           # run the dependency-free self-test suite
```

On first launch the app asks you to set a master password.

## Project structure

```text
PDSManager/
├── build.xml
├── src/com/pds/
│   ├── Main.java
│   ├── model/        plain data classes, one per PDS section
│   ├── storage/      ObjectTextCodec and PdsRecordStore (encrypted file I/O)
│   ├── crypto/       CryptoService (AES-GCM, PBKDF2)
│   ├── util/         Validator, CsvUtil, PrintUtil, IdGen, Countries, AttachmentIO
│   └── ui/           MainFrame, dashboard, reports, record list and editor
│       ├── sections/ one panel per PDS section
│       └── editors/  DynamicTablePanel (the +/- row component)
└── test/com/pds/SelfTest.java
```

## Testing

`ant test` runs a self-contained smoke suite with no JUnit dependency. It covers AES-GCM round trips, wrong-password rejection, tamper detection, the full text codec including nested lists, the complete store lifecycle (create, save, reload, update, delete, lock and unlock), and validation rules.

## Known limitations

- The print layout closely approximates, but is not a pixel-exact copy of, the official CSC form.
- There is no scheduled backup to an external drive. The `.bak` files and `deleted/` folder are the safety net.
- Per-field maximum lengths are not enforced in the widgets. A per-record size guard (80 MB) catches extreme cases.

## Author

Built by [@marsdux](https://github.com/marsdux).
