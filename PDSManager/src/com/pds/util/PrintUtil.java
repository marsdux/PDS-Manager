package com.pds.util;

import com.pds.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

/**
 * Renders a PdsRecord as a boxed, grid-based document closely modeled on the
 * layout of the official CS Form No. 212 - section headers numbered the same
 * way, labeled field boxes, bordered tables for the repeating sections, a
 * Yes/No checkbox style for the questionnaire, and the 2x2 photo in the
 * corner of page 1 - spread across five pages the same way the original
 * form is (Personal/Family, Education/Eligibility, Work Experience,
 * Voluntary Work/L&D/Other Info/Questionnaire/References, then Certification
 * and Notarization).
 *
 * This is a close approximation, not a pixel-exact reproduction of the CSC's
 * official template (exact government measurements/fonts aren't published
 * for replication) - but every field, box, and section from the form is
 * represented, laid out in the same order and grouping.
 */
public class PrintUtil implements Printable {

    private static final Font TITLE_FONT = new Font(Font.SERIF, Font.BOLD, 14);
    private static final Font SUBTITLE_FONT = new Font(Font.SERIF, Font.PLAIN, 8);
    private static final Font SECTION_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 6);
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    private static final int PAGE_COUNT = 5;

    private final PdsRecord r;

    public PrintUtil(PdsRecord r) { this.r = r; }

    public static void printRecord(Component parent, PdsRecord r) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new PrintUtil(r));
        job.setJobName("PDS - " + r.displayName());
        if (job.printDialog()) job.print();
    }

    @Override
    public int print(Graphics graphics, PageFormat pf, int pageIndex) {
        if (pageIndex >= PAGE_COUNT) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(pf.getImageableX(), pf.getImageableY());
        double w = pf.getImageableWidth();

        pageHeader(g2, w, pageIndex);
        double y = 34;
        switch (pageIndex) {
            case 0 -> y = personalAndFamilyPage(g2, w, y);
            case 1 -> y = educationAndEligibilityPage(g2, w, y);
            case 2 -> y = workExperiencePage(g2, w, y);
            case 3 -> y = voluntaryLdOtherQuestionnairePage(g2, w, y);
            case 4 -> y = certificationAndNotarizationPage(g2, w, y);
        }
        g2.setFont(SUBTITLE_FONT);
        g2.drawString("Page " + (pageIndex + 1) + " of " + PAGE_COUNT + "  -  CS Form No. 212, Revised 2026", 0, (float) (y + 10));
        return PAGE_EXISTS;
    }

    private void pageHeader(Graphics2D g2, double w, int pageIndex) {
        g2.setFont(SUBTITLE_FONT);
        g2.drawString("CS FORM NO. 212", (float) (w - 70), 8);
        g2.setFont(TITLE_FONT);
        String title = "PERSONAL DATA SHEET";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (float) (w / 2 - fm.stringWidth(title) / 2.0), 16);
        g2.setFont(SUBTITLE_FONT);
        String sub = "(Revised 2026) - " + r.displayName() + "  [" + r.status + "]";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (float) (w / 2 - fm.stringWidth(sub) / 2.0), 24);
    }

    // ================= PAGE 1: Personal Info + Family Background =================

    private double personalAndFamilyPage(Graphics2D g2, double w, double y) {
        PersonalInfo p = r.personalInfo;
        double photoSize = 72; // ~1in square at this scale, representing the 2x2 photo box
        double formW = w - photoSize - 6;

        g2.setFont(SECTION_FONT);
        g2.drawString("I. PERSONAL INFORMATION", 0, (float) (y + 6));
        drawPhotoBox(g2, formW + 6, y, photoSize, photoSize);
        y += 10;

        double rowH = 15;
        y = row(g2, 0, y, formW, rowH, f(0.5, "1. SURNAME", p.surname), f(0.5, "NAME EXT.", p.nameExtension));
        y = row(g2, 0, y, formW, rowH, f(0.5, "2. FIRST NAME", p.firstName), f(0.5, "3. MIDDLE NAME", p.middleName));
        y = row(g2, 0, y, formW, rowH, f(0.5, "DATE OF BIRTH", p.dateOfBirth), f(0.5, "4. PLACE OF BIRTH", p.placeOfBirth));
        y = row(g2, 0, y, formW, rowH, f(0.5, "5. SEX", p.sexAtBirth), f(0.5, "6. CIVIL STATUS", displayCivilStatus(p)));
        y = row(g2, 0, y, formW, rowH, f(0.33, "7. HEIGHT (m)", p.height), f(0.33, "8. WEIGHT (kg)", p.weight), f(0.34, "9. BLOOD TYPE", p.bloodType));
        y = row(g2, 0, y, formW, rowH, f(0.5, "10. UMID ID NO.", p.umidIdNo), f(0.5, "11. PAG-IBIG ID NO.", p.pagIbigIdNo));
        y = row(g2, 0, y, formW, rowH, f(0.5, "12. PHILHEALTH NO.", p.philHealthNo), f(0.5, "13. PHILSYS CARD NO.", p.philSysCardNo));
        y = row(g2, 0, y, formW, rowH, f(0.5, "14. TIN NO.", p.tinNo), f(0.5, "15. AGENCY EMPLOYEE NO.", p.agencyEmployeeNo));
        y = row(g2, 0, y, formW, rowH, f(0.5, "16. CITIZENSHIP", displayCitizenship(p)), f(0.5, "IF DUAL, COUNTRY", p.citizenDual ? p.dualCitizenshipCountry : ""));

        y = row(g2, 0, y, w, rowH, f(0.4, "17. RES. HOUSE/BLOCK/LOT/STREET", p.resHouseBlockLot + " " + p.resStreet),
                f(0.3, "SUBDIVISION/BARANGAY", p.resSubdivisionVillage + " " + p.resBarangay),
                f(0.3, "CITY/PROVINCE/ZIP", p.resCityMunicipality + ", " + p.resProvince + " " + p.resZipCode));
        y = row(g2, 0, y, w, rowH, f(0.4, "18. PERM. HOUSE/BLOCK/LOT/STREET", p.permHouseBlockLot + " " + p.permStreet),
                f(0.3, "SUBDIVISION/BARANGAY", p.permSubdivisionVillage + " " + p.permBarangay),
                f(0.3, "CITY/PROVINCE/ZIP", p.permCityMunicipality + ", " + p.permProvince + " " + p.permZipCode));
        y = row(g2, 0, y, w, rowH, f(0.34, "19. TELEPHONE NO.", p.telephoneNo), f(0.33, "20. MOBILE NO.", p.mobileNo), f(0.33, "21. EMAIL ADDRESS", p.emailAddress));

        y += 6;
        g2.setFont(SECTION_FONT);
        g2.drawString("II. FAMILY BACKGROUND", 0, (float) (y + 6));
        y += 10;
        FamilyBackground fb = r.familyBackground;
        y = row(g2, 0, y, w, rowH, f(0.5, "22. SPOUSE'S SURNAME, FIRST, MIDDLE", nameOf(fb.spouseSurname, fb.spouseFirstName, fb.spouseMiddleName)),
                f(0.5, "OCCUPATION / EMPLOYER", fb.spouseOccupation + " / " + fb.spouseEmployerBusinessName));
        y = row(g2, 0, y, w, rowH, f(1.0, "24. FATHER'S SURNAME, FIRST, MIDDLE", nameOf(fb.fatherSurname, fb.fatherFirstName, fb.fatherMiddleName)));
        y = row(g2, 0, y, w, rowH, f(1.0, "25. MOTHER'S MAIDEN NAME (SURNAME, FIRST, MIDDLE)", nameOf(fb.motherMaidenSurname, fb.motherFirstName, fb.motherMiddleName)));

        y += 4;
        g2.setFont(SECTION_FONT);
        g2.drawString("23. NAME OF CHILDREN (write full name and list all)", 0, (float) (y + 6));
        y += 9;
        y = table(g2, 0, y, w, 12, new double[]{0.7, 0.3}, new String[]{"Full Name", "Date of Birth"},
                childRows(fb), 8);
        return y;
    }

    // ================= PAGE 2: Education + Eligibility =================

    private double educationAndEligibilityPage(Graphics2D g2, double w, double y) {
        g2.setFont(SECTION_FONT);
        g2.drawString("III. EDUCATIONAL BACKGROUND", 0, (float) (y + 6));
        y += 9;
        String[][] eduRows = new String[r.education.size()][];
        for (int i = 0; i < r.education.size(); i++) {
            EducationEntry e = r.education.get(i);
            eduRows[i] = new String[]{e.level, e.schoolName, e.degreeCourse, e.periodFrom + "-" + e.periodTo,
                    e.highestLevelUnits, e.yearGraduated, e.honors + proofMark(e.attachmentBase64)};
        }
        y = table(g2, 0, y, w, 20, new double[]{0.1, 0.22, 0.2, 0.1, 0.18, 0.08, 0.12},
                new String[]{"Level", "School", "Degree/Course", "Period", "Units/Level", "Year Grad.", "Honors"}, eduRows, 12);

        y += 6;
        g2.setFont(SECTION_FONT);
        g2.drawString("IV. CIVIL SERVICE ELIGIBILITY", 0, (float) (y + 6));
        y += 9;
        String[][] eligRows = new String[r.eligibility.size()][];
        for (int i = 0; i < r.eligibility.size(); i++) {
            EligibilityEntry e = r.eligibility.get(i);
            eligRows[i] = new String[]{e.careerServiceEligibility, e.rating, e.dateOfExamConferment,
                    e.placeOfExamConferment, e.licenseNumber, e.licenseValidity + proofMark(e.attachmentBase64)};
        }
        y = table(g2, 0, y, w, 20, new double[]{0.32, 0.1, 0.16, 0.22, 0.1, 0.1},
                new String[]{"Eligibility", "Rating", "Date", "Place", "License #", "Validity"}, eligRows, 12);
        return y;
    }

    // ================= PAGE 3: Work Experience =================

    private double workExperiencePage(Graphics2D g2, double w, double y) {
        g2.setFont(SECTION_FONT);
        g2.drawString("V. WORK EXPERIENCE (include private employment; do not include temporary/casual)", 0, (float) (y + 6));
        y += 9;
        String[][] rows = new String[r.workExperience.size()][];
        for (int i = 0; i < r.workExperience.size(); i++) {
            WorkExperienceEntry we = r.workExperience.get(i);
            rows[i] = new String[]{we.dateFrom + "-" + we.dateTo, we.positionTitle, we.departmentAgency,
                    we.monthlySalary, we.salaryGrade, we.statusOfAppointment, we.govtService + proofMark(we.attachmentBase64)};
        }
        y = table(g2, 0, y, w, 22, new double[]{0.14, 0.2, 0.26, 0.1, 0.1, 0.12, 0.08},
                new String[]{"Inclusive Dates", "Position Title", "Dept./Agency", "Salary", "SG", "Status", "Gov't"}, rows, 60);
        return y;
    }

    // ================= PAGE 4: Voluntary Work, L&D, Other Info, Questionnaire, References =================

    private double voluntaryLdOtherQuestionnairePage(Graphics2D g2, double w, double y) {
        g2.setFont(SECTION_FONT);
        g2.drawString("VI. VOLUNTARY WORK / INVOLVEMENT", 0, (float) (y + 6));
        y += 9;
        String[][] vwRows = new String[r.voluntaryWork.size()][];
        for (int i = 0; i < r.voluntaryWork.size(); i++) {
            VoluntaryWorkEntry v = r.voluntaryWork.get(i);
            vwRows[i] = new String[]{v.organizationNameAddress, v.dateFrom + "-" + v.dateTo, v.numberOfHours,
                    v.positionNatureOfWork + proofMark(v.attachmentBase64)};
        }
        y = table(g2, 0, y, w, 15, new double[]{0.4, 0.2, 0.12, 0.28},
                new String[]{"Organization", "Dates", "Hours", "Position/Nature"}, vwRows, 10);

        y += 5;
        g2.setFont(SECTION_FONT);
        g2.drawString("VII. LEARNING & DEVELOPMENT (L&D) INTERVENTIONS", 0, (float) (y + 6));
        y += 9;
        String[][] ldRows = new String[r.learningDevelopment.size()][];
        for (int i = 0; i < r.learningDevelopment.size(); i++) {
            LearningDevelopmentEntry ld = r.learningDevelopment.get(i);
            ldRows[i] = new String[]{ld.title, ld.dateFrom + "-" + ld.dateTo, ld.numberOfHours, ld.type,
                    ld.conductedSponsoredBy + proofMark(ld.attachmentBase64)};
        }
        y = table(g2, 0, y, w, 15, new double[]{0.32, 0.16, 0.1, 0.16, 0.26},
                new String[]{"Title", "Dates", "Hours", "Type", "Conducted/Sponsored By"}, ldRows, 10);

        y += 5;
        g2.setFont(SECTION_FONT);
        g2.drawString("VIII. OTHER INFORMATION", 0, (float) (y + 6));
        y += 9;
        g2.setFont(VALUE_FONT);
        g2.drawString("31. Skills/Hobbies: " + String.join("; ", r.otherInfo.specialSkillsHobbies), 2, (float) (y + 6));
        y += 9;
        g2.drawString("32. Distinctions: " + joinText(r.otherInfo.nonAcademicDistinctions), 2, (float) (y + 6));
        y += 9;
        g2.drawString("33. Memberships: " + joinText(r.otherInfo.membershipAssociations), 2, (float) (y + 6));
        y += 9;
        int docsAttached = countAttachments(r.otherInfo.nonAcademicDistinctions) + countAttachments(r.otherInfo.membershipAssociations);
        if (docsAttached > 0) {
            g2.setFont(LABEL_FONT);
            g2.drawString("(" + docsAttached + " proof document(s) on file for items 32-33 - see the Attachments tab in the app)", 2, (float) (y + 6));
        }
        y += 10;

        g2.setFont(SECTION_FONT);
        g2.drawString("BACKGROUND QUESTIONNAIRE", 0, (float) (y + 6));
        y += 9;
        Questionnaire q = r.questionnaire;
        y = qLine(g2, w, y, "34a. Related within 3rd degree to appointing authority?", q.q34a3rdDegree);
        y = qLine(g2, w, y, "34b. Related within 4th degree (LGU)?", q.q34b4thDegree);
        y = qLine(g2, w, y, "35a. Found guilty of administrative offense?", q.q35aAdminOffense);
        y = qLine(g2, w, y, "35b. Criminally charged before any court?", q.q35bCriminallyCharged);
        y = qLine(g2, w, y, "36. Convicted of any crime/violation of law?", q.q36ConvictedCrime);
        y = qLine(g2, w, y, "37. Separated from service (any reason)?", q.q37SeparatedFromService);
        y = qLine(g2, w, y, "38a. Candidate in a national/local election?", q.q38aCandidateElection);
        y = qLine(g2, w, y, "38b. Resigned to campaign in last election?", q.q38bResignedToCampaign);
        y = qLine(g2, w, y, "39. Acquired immigrant status of another country?", q.q39ImmigrantStatus);
        y = qLine(g2, w, y, "40a. Member of an indigenous group?", q.q40aIndigenousGroup);
        y = qLine(g2, w, y, "40b. Person with disability?" + (isYes(q.q40bPersonWithDisability) ? "  ID#: " + q.q40bIdNo : ""), q.q40bPersonWithDisability);
        y = qLine(g2, w, y, "40c. Solo parent?" + (isYes(q.q40cSoloParent) ? "  ID#: " + q.q40cIdNo : ""), q.q40cSoloParent);

        y += 4;
        g2.setFont(SECTION_FONT);
        g2.drawString("41. REFERENCES", 0, (float) (y + 6));
        y += 9;
        String[][] refRows = new String[r.questionnaire.references.size()][];
        for (int i = 0; i < r.questionnaire.references.size(); i++) {
            ReferenceEntry ref = r.questionnaire.references.get(i);
            refRows[i] = new String[]{ref.name, ref.address, ref.contactNoEmail};
        }
        y = table(g2, 0, y, w, 12, new double[]{0.3, 0.45, 0.25}, new String[]{"Name", "Address", "Contact"}, refRows, 8);

        y += 5;
        g2.setFont(SECTION_FONT);
        g2.drawString("42. GOVERNMENT ISSUED ID", 0, (float) (y + 6));
        y += 9;
        y = row(g2, 0, y, w, 15, f(0.5, "TYPE OF ID", q.govtIssuedId), f(0.5, "ID/LICENSE/PASSPORT NO.", q.idLicensePassportNo));
        y = row(g2, 0, y, w, 15, f(0.34, "DATE OF ISSUANCE", q.dateOfIssuance), f(0.33, "PLACE OF ISSUANCE", q.placeOfIssuance),
                f(0.33, "DATE ACCOMPLISHED", q.dateAccomplished));
        return y;
    }

    // ================= PAGE 5: Certification + Notarization (jurat) =================

    private double certificationAndNotarizationPage(Graphics2D g2, double w, double y) {
        g2.setFont(SECTION_FONT);
        g2.drawString("CERTIFICATION", 0, (float) (y + 6));
        y += 10;
        g2.setFont(VALUE_FONT);
        String certification = "I declare under penalty of law that this Personal Data Sheet has been accomplished by me, and is "
                + "true, correct, and complete information. I authorize the agency head/authorized representative to verify/validate "
                + "the contents stated herein. I agree that any misrepresentation made in this document renders me administratively "
                + "and/or criminally liable under the law, and disqualifies me for employment in the government service.";
        y = wrapText(g2, certification, 0, y, w, 9);

        y += 14;
        double sigW = w / 2 - 10;
        drawField(g2, 0, y, sigW, 22, "SIGNATURE", "");
        drawField(g2, w - sigW, y, sigW, 22, "DATE ACCOMPLISHED", r.questionnaire.dateAccomplished);
        y += 30;

        y += 10;
        g2.setFont(SECTION_FONT);
        g2.drawString("ACKNOWLEDGMENT (NOTARIZATION)", 0, (float) (y + 6));
        y += 10;

        NotarizationInfo n = r.notarization;
        g2.setFont(VALUE_FONT);
        String jurat = "SUBSCRIBED AND SWORN to before me this " + orBlank(n.subscribedDate, "____________")
                + " at " + orBlank(n.subscribedPlace, "____________") + ", affiant exhibiting to me competent proof of identity.";
        y = wrapText(g2, jurat, 0, y, w, 9);
        y += 10;

        double rowH = 15;
        y = row(g2, 0, y, w, rowH, f(0.5, "NOTARY PUBLIC", n.notaryPublicName), f(0.5, "COMMISSION NO.", n.notaryCommissionNo));
        y = row(g2, 0, y, w, rowH, f(0.5, "PTR NO.", n.notaryPtrNo), f(0.5, "IBP NO.", n.notaryIbpNo));
        y = row(g2, 0, y, w, rowH, f(1.0, "ROLL OF ATTORNEYS NO.", n.notaryRollNo));
        y = row(g2, 0, y, w, rowH, f(0.25, "DOC. NO.", n.docNo), f(0.25, "PAGE NO.", n.pageNo),
                f(0.25, "BOOK NO.", n.bookNo), f(0.25, "SERIES OF", n.seriesOf));

        y += 10;
        if (n.attachmentBase64 != null && !n.attachmentBase64.isBlank()) {
            g2.setFont(LABEL_FONT);
            g2.drawString("[Electronic notary document on file: " + n.attachmentFileName + " - see the Attachments tab in the app]", 0, (float) (y + 6));
            y += 8;
        } else {
            g2.setFont(LABEL_FONT);
            g2.drawString("[No electronic notary document attached yet]", 0, (float) (y + 6));
            y += 8;
        }
        return y;
    }

    /** Simple word-wrap into the given width, returning the y position after the last line. */
    private double wrapText(Graphics2D g2, String text, double x, double y, double w, double lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > w && !line.isEmpty()) {
                g2.drawString(line.toString(), (float) x, (float) (y + lineHeight));
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            g2.drawString(line.toString(), (float) x, (float) (y + lineHeight));
            y += lineHeight;
        }
        return y;
    }

    private static String orBlank(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }

    /** Small inline marker appended to a printed cell so a reader can see, at a glance, which rows have a scanned proof on file. */
    private static String proofMark(String attachmentBase64) {
        return (attachmentBase64 != null && !attachmentBase64.isBlank()) ? "  [PROOF ON FILE]" : "";
    }

    // ================= drawing primitives =================

    private record F(double weight, String label, String value) {}
    private static F f(double weight, String label, String value) { return new F(weight, label, value == null ? "" : value); }

    /** Draws one row of adjacent labeled field boxes spanning the given width. */
    private double row(Graphics2D g2, double x, double y, double w, double h, F... fields) {
        double totalWeight = 0;
        for (F fld : fields) totalWeight += fld.weight();
        double cx = x;
        for (F fld : fields) {
            double fw = w * (fld.weight() / totalWeight);
            drawField(g2, cx, y, fw, h, fld.label(), fld.value());
            cx += fw;
        }
        return y + h;
    }

    private void drawField(Graphics2D g2, double x, double y, double w, double h, String label, String value) {
        Rectangle2D.Double box = new Rectangle2D.Double(x, y, w, h);
        g2.draw(box);
        g2.setFont(LABEL_FONT);
        g2.drawString(label, (float) (x + 2), (float) (y + 6));
        g2.setFont(VALUE_FONT);
        Shape oldClip = g2.getClip();
        g2.clip(new Rectangle2D.Double(x + 1, y + 7, w - 3, h - 8));
        g2.drawString(value == null ? "" : value, (float) (x + 2), (float) (y + h - 3));
        g2.setClip(oldClip);
    }

    /** Draws a bordered header row + up to maxRows data rows; extra rows are summarized with a "+N more" note. */
    private double table(Graphics2D g2, double x, double y, double w, double rowH, double[] colWeights, String[] headers, String[][] rows, int maxRows) {
        double totalWeight = 0;
        for (double cw : colWeights) totalWeight += cw;
        double[] colW = new double[colWeights.length];
        for (int i = 0; i < colWeights.length; i++) colW[i] = w * (colWeights[i] / totalWeight);

        g2.setFont(SECTION_FONT.deriveFont(6.5f));
        double cx = x;
        for (int i = 0; i < headers.length; i++) {
            g2.draw(new Rectangle2D.Double(cx, y, colW[i], rowH * 0.7));
            g2.drawString(headers[i], (float) (cx + 2), (float) (y + rowH * 0.5));
            cx += colW[i];
        }
        y += rowH * 0.7;

        int shown = Math.min(rows.length, maxRows);
        g2.setFont(VALUE_FONT);
        for (int rIdx = 0; rIdx < shown; rIdx++) {
            cx = x;
            for (int c = 0; c < headers.length; c++) {
                Rectangle2D.Double cell = new Rectangle2D.Double(cx, y, colW[c], rowH);
                g2.draw(cell);
                Shape oldClip = g2.getClip();
                g2.clip(new Rectangle2D.Double(cx + 1, y + 1, colW[c] - 2, rowH - 2));
                String val = c < rows[rIdx].length && rows[rIdx][c] != null ? rows[rIdx][c] : "";
                g2.drawString(val, (float) (cx + 2), (float) (y + rowH - 4));
                g2.setClip(oldClip);
                cx += colW[c];
            }
            y += rowH;
        }
        if (rows.length == 0) {
            g2.draw(new Rectangle2D.Double(x, y, w, rowH));
            g2.drawString("(none on file)", (float) (x + 2), (float) (y + rowH - 4));
            y += rowH;
        } else if (rows.length > maxRows) {
            g2.drawString("... +" + (rows.length - maxRows) + " more row(s) - see the app for the full list", (float) x, (float) (y + 8));
            y += 10;
        }
        return y;
    }

    private double qLine(Graphics2D g2, double w, double y, String label, YesNoItem item) {
        g2.setFont(VALUE_FONT);
        boolean yes = Boolean.TRUE.equals(item.answer);
        boolean no = Boolean.FALSE.equals(item.answer);
        String box = "[" + (yes ? "X" : " ") + "] Yes   [" + (no ? "X" : " ") + "] No";
        Shape oldClip = g2.getClip();
        g2.clip(new Rectangle2D.Double(0, y - 6, w - 90, 9));
        g2.drawString(label, 2, (float) (y + 2));
        g2.setClip(oldClip);
        g2.drawString(box, (float) (w - 85), (float) (y + 2));
        y += 8;
        if (item.details != null && !item.details.isBlank()) {
            g2.setFont(LABEL_FONT);
            g2.drawString("    Details: " + item.details, 2, (float) (y + 2));
            y += 7;
        }
        return y;
    }

    private boolean isYes(YesNoItem item) { return Boolean.TRUE.equals(item.answer); }

    private void drawPhotoBox(Graphics2D g2, double x, double y, double w, double h) {
        g2.draw(new Rectangle2D.Double(x, y, w, h));
        String b64 = r.personalInfo.photoBase64;
        if (b64 != null && !b64.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                g2.drawImage(img, (int) x + 1, (int) y + 1, (int) w - 2, (int) h - 2, null);
            } catch (Exception ignored) {
                drawPhotoPlaceholder(g2, x, y, w, h);
            }
        } else {
            drawPhotoPlaceholder(g2, x, y, w, h);
        }
    }

    private void drawPhotoPlaceholder(Graphics2D g2, double x, double y, double w, double h) {
        g2.setFont(LABEL_FONT);
        String txt = "2x2 PHOTO";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, (float) (x + w / 2 - fm.stringWidth(txt) / 2.0), (float) (y + h / 2));
    }

    // ================= small text helpers =================

    private static String nameOf(String surname, String first, String middle) {
        String n = (nz(surname) + ", " + nz(first) + " " + nz(middle)).trim();
        return n.replace(" ,", ",");
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String displayCivilStatus(PersonalInfo p) {
        return "Others".equalsIgnoreCase(p.civilStatus) && !p.civilStatusOther.isBlank()
                ? p.civilStatus + " (" + p.civilStatusOther + ")" : p.civilStatus;
    }

    private static String displayCitizenship(PersonalInfo p) {
        if (!p.citizenDual) return "Filipino";
        return "Dual Citizen (" + (p.dualByBirth ? "by birth" : "by naturalization") + ")";
    }

    private static int countAttachments(List<AttachableText> items) {
        int n = 0;
        for (AttachableText t : items) if (t.attachmentBase64 != null && !t.attachmentBase64.isBlank()) n++;
        return n;
    }

    private static String joinText(List<AttachableText> items) {
        StringBuilder sb = new StringBuilder();
        for (AttachableText t : items) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(t.text);
        }
        return sb.toString();
    }

    private static String[][] childRows(FamilyBackground fb) {
        String[][] out = new String[fb.children.size()][];
        for (int i = 0; i < fb.children.size(); i++) {
            Child c = fb.children.get(i);
            out[i] = new String[]{c.fullName, c.dateOfBirth};
        }
        return out;
    }
}
