# PDS Manager

A desktop Java Swing application for encoding, tracking, and reporting on
CS Form No. 212 (Revised 2026) - the Philippine Civil Service Commission
**Personal Data Sheet** - built to your spec: NetBeans + Ant, **zero external
libraries** (pure JDK: `javax.swing`, `javax.crypto`, `java.awt.print`).

## Opening the project

**NetBeans:** File > Open Project... > select the `PDSManager` folder. It's set
up as an Ant Free-form project, so NetBeans uses `build.xml` directly (no
regeneration, no fragile auto-generated `build-impl.xml`). Right-click the
project > Run.

**Command line:** `ant run` (or `ant jar` then `java -jar dist/PDSManager.jar`).
`ant test` runs the self-test suite (see below).

First launch asks you to set a **master password** - this encrypts every
record on disk. There is no recovery if it's lost; that's the trade-off for
nobody else being able to read the file if your laptop is lost or stolen.

## What's implemented

- **Every section of the PDS**, matching your uploaded form field-for-field:
  Personal Info, Family Background (incl. children), Educational Background,
  Civil Service Eligibility, Work Experience, Learning & Development,
  Voluntary Work, Other Information (skills/distinctions/memberships),
  and the full background Questionnaire (Q34-40) + References + Gov't ID.
- **Full CRUD**: New, edit, Save, Delete (soft-delete to a `deleted/` folder,
  not destroyed - recoverable if someone deletes by mistake).
- **+ / - row controls** on every repeating section (children, education,
  eligibility, work experience, L&D, voluntary work, skills, distinctions,
  memberships, references) via a shared `DynamicTablePanel` component.
- **Tab-key navigation**: Tab/Shift+Tab move field-to-field and, inside a
  table, cell-to-cell/row-to-row; pressing Tab on the very last cell of the
  last row auto-adds a new row so you never need the mouse mid-entry.
- **Dashboard** tab: live counts of Ongoing / Completed / Cancelled records
  plus a recent-activity table.
- **Reports & Summary** tab: aggregate stats (by status, sex, civil status,
  average education/eligibility entries) with a Print button.
- **Search**: the left-hand record list filters live by name, status, or
  remarks as you type.
- **Import/Export**: CSV export of the record summary list; single-record
  export/import as an encrypted `.pdsrecord` file (portable between
  installations that share the same master password).
- **Print**: paginated, formatted hard copy of any record via the standard
  Java print dialog (`java.awt.print`).

## Security & data integrity design

- **AES-256-GCM** encryption at rest for every record file. Key is derived
  from your master password via **PBKDF2-HMAC-SHA256 (200,000 iterations)**;
  the password itself is never written to disk, only a salted verifier hash.
- Every record's salt + IV are random per save, so no two ciphertexts ever
  look alike, and GCM's authentication tag makes **any tampering or
  corruption detectable on load** (the app reports which files failed
  integrity, instead of silently loading garbage).
- **Atomic writes**: save = write to `*.tmp` -> flush -> `ATOMIC_MOVE`, with
  the previous version kept as `*.bak`. A crash mid-save cannot corrupt data.
- **Soft delete**: deleted records move to `deleted/` with a timestamp rather
  than being unlinked immediately.
- No SQL, no network calls, no reflection-based deserialization of untrusted
  input -> no SQL injection surface, no remote attack surface, no gadget-chain
  deserialization risk (the codec only ever populates the fixed, known
  `com.pds.model` classes).
- Change-password flow re-encrypts every record under the new key before
  committing the new verifier, so a partial failure can't lock you out.
- Data lives under `~/PDSManagerData` (the user's home directory), so it
  persists correctly regardless of where the app/jar is launched from.

## Where things are

```
src/com/pds/model/      plain data classes - one per PDS section
src/com/pds/storage/    ObjectTextCodec (reflection-based flat-text
                         serializer) + PdsRecordStore (encrypted file I/O)
src/com/pds/crypto/     CryptoService (AES-GCM + PBKDF2)
src/com/pds/util/       Validator, CsvUtil, PrintUtil, IdGen
src/com/pds/ui/         MainFrame, dashboard, reports, record list/editor
src/com/pds/ui/sections/  one panel per PDS section
src/com/pds/ui/editors/   DynamicTablePanel (the +/- row component)
test/com/pds/SelfTest.java  dependency-free smoke tests (`ant test`)
```

## Testing performed

`ant test` (21 checks, all passing as delivered) covers: AES-GCM encrypt/
decrypt round-trip, wrong-password rejection, tamper detection, full
object<->text codec round-trip (including nested lists like children and
questionnaire yes/no items), full store CRUD lifecycle (init, save, reload,
update, delete, lock/unlock with right and wrong passwords), and validation
logic for both an empty and a fully-filled record.

The Swing UI itself compiles cleanly under `javac -Xlint:all` (only harmless
`serialVersionUID` notices, standard for any Swing app). It could not be
visually smoke-tested in this environment (no display server available
here) - please do a first-run pass on your machine and let me know if
anything looks off; the logic layer underneath it is what's been proven
correct.

## Changelog (third pass - this save point)

- **Added:** Notarization tab - jurat fields (subscribed date/place, notary
  name, commission/PTR/IBP/roll numbers, doc/page/book/series) plus a
  dedicated attach slot for the scanned electronic notary document your
  lawyer provides.
- **Added:** a centralized Attachments tab listing every supporting document
  on the record - the 2x2 photo, every proof attached on sections III-VII and
  items 32/33, and the notary file - each with one-click View and Print
  (delegates to your OS's default PDF/image viewer via `java.awt.Desktop`).
- **Added:** a "Print" option next to "View" in every per-row proof menu -
  previously you could only open, not print, a row's attached document.
- **Improved:** Personal Info Item 16 now branches properly - choosing
  Filipino vs. Dual Citizenship, and Dual unlocks its own by-birth/
  naturalization choice and country dropdown, instead of a single checkbox.
- **Improved:** the 2x2 photo is now center-cropped to a true square before
  storage/preview, matching a real ID photo's aspect ratio.
- **Improved:** print output now runs 5 pages, adding a Certification
  statement, signature/date box, and the full notary acknowledgment block at
  the end - plus every printed table row with a scanned proof on file shows
  `[PROOF ON FILE]` inline.
- **Fixed:** corrected the form's own VI/VII section order (Voluntary Work is
  VI, L&D is VII - they were swapped) throughout the editor and print output.
- **Fixed:** the 8 MB per-record size guard from an earlier pass would have
  silently blocked saving as soon as 2+ attachments were added - raised to
  80 MB, which comfortably fits many attachments at the 5 MB/file cap.
- **Refactored:** the duplicated attach/open temp-file logic scattered across
  the table component was pulled into one shared `AttachmentIO` helper, used
  by the per-row proof buttons, the photo, and the new notary attachment.
- Re-verified: self-test suite now at 32 checks, all passing, including new
  round-trips for the notary fields/attachment and a direct test proving
  multiple large attachments actually fit under the raised size guard.

## Changelog (second pass)

- **Fixed:** a confused tab-change listener in `MainFrame` that called
  dashboard refresh under the wrong condition and duplicated work - simplified
  to one clear refresh call.
- **Fixed:** the "confirm password" field on first run wasn't cleared from
  memory after use (`LoginDialog`) - now both password arrays are wiped.
- **Fixed:** `changePassword()` could leave the store half-migrated (some
  records under the old key, some under the new) if a failure happened
  mid-loop. It now re-encrypts everything **in memory first**, and only
  touches disk - records, then the new verifier - after every record has
  succeeded.
- **Added:** an 8 MB per-record size guard in the storage layer, so a
  pathological paste or a corrupted import can't quietly balloon disk/memory
  use.
- **Added:** ID photo attachment on the Personal Info tab (Attach.../Remove),
  auto-downscaled and stored as embedded Base64 JPEG - pure `javax.imageio`,
  no external library.
- **Added:** the dual-citizenship country field is now a dropdown populated
  from the actual 204-country reference list in your uploaded template,
  instead of free text.
- Re-verified: full self-test suite (now 23 checks) still passes after all of
  the above, including new round-trip checks for the photo field and country
  selection.

## Honest scope notes / natural next steps

- The **print layout** is a clean monospaced text rendering, not a pixel
  copy of the official CSC form grid - functional for filing/reference, but
  if you need an exact visual replica of the government form for signature,
  that's a follow-up (would need custom Graphics2D table drawing). The photo
  isn't in the print output yet either.
- No auto-backup-to-external-drive scheduler yet - the `.bak` files and
  `deleted/` folder are your current safety net.
- Free-text fields (addresses, remarks, etc.) don't have explicit max-length
  limits at the widget level - the record-level size guard catches extreme
  cases, but per-field limits would be a nice follow-up polish item.

Happy to do a follow-up pass on any of these, or on anything that looks off
once you've run it locally.
