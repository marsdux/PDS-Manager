package com.pds.ui;

import com.pds.model.PdsRecord;
import com.pds.model.RecordStatus;
import com.pds.ui.sections.*;

import javax.swing.*;
import java.awt.*;

public class RecordEditorPanel extends JPanel {

    private final PersonalInfoPanel personalInfoPanel = new PersonalInfoPanel();
    private final FamilyBackgroundPanel familyPanel = new FamilyBackgroundPanel();
    private final EducationPanel educationPanel = new EducationPanel();
    private final EligibilityPanel eligibilityPanel = new EligibilityPanel();
    private final WorkExperiencePanel workPanel = new WorkExperiencePanel();
    private final LearningDevelopmentPanel ldPanel = new LearningDevelopmentPanel();
    private final VoluntaryWorkPanel voluntaryPanel = new VoluntaryWorkPanel();
    private final OtherInfoPanel otherInfoPanel = new OtherInfoPanel();
    private final QuestionnairePanel questionnairePanel = new QuestionnairePanel();
    private final NotarizationPanel notarizationPanel = new NotarizationPanel();
    private final AttachmentsPanel attachmentsPanel = new AttachmentsPanel();

    private final JComboBox<RecordStatus> statusCombo = new JComboBox<>(RecordStatus.values());
    private final JTextField remarks = new JTextField();
    private final JLabel idLabel = new JLabel(" ");

    private PdsRecord current;

    public RecordEditorPanel() {
        super(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("I. Personal Info", personalInfoPanel);
        tabs.addTab("II. Family Background", familyPanel);
        tabs.addTab("III. Education", educationPanel);
        tabs.addTab("IV. Eligibility", eligibilityPanel);
        tabs.addTab("V. Work Experience", workPanel);
        tabs.addTab("VI. Voluntary Work", voluntaryPanel);
        tabs.addTab("VII. L&D", ldPanel);
        tabs.addTab("VIII. Other Info", otherInfoPanel);
        tabs.addTab("IX-X. Questionnaire & Refs", questionnairePanel);
        tabs.addTab("Notarization", notarizationPanel);
        tabs.addTab("Attachments", attachmentsPanel);
        // Ctrl+Right / Ctrl+Left already cycle JTabbedPane tabs by default in Swing;
        // Tab/Shift+Tab move field-to-field inside whichever tab is showing.
        // Switching TO the Attachments tab syncs every other tab's in-memory edits
        // into the record first, so newly-attached files show up immediately.
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == attachmentsPanel) {
                attachmentsPanel.refresh(commitToRecord());
            }
        });
        add(tabs, BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Record status:"));
        top.add(statusCombo);
        top.add(new JLabel("Remarks:"));
        remarks.setColumns(30);
        top.add(remarks);
        top.add(idLabel);
        add(top, BorderLayout.NORTH);
    }

    public void loadRecord(PdsRecord r) {
        this.current = r;
        personalInfoPanel.loadFrom(r.personalInfo);
        familyPanel.loadFrom(r.familyBackground);
        educationPanel.loadFrom(r.education);
        eligibilityPanel.loadFrom(r.eligibility);
        workPanel.loadFrom(r.workExperience);
        ldPanel.loadFrom(r.learningDevelopment);
        voluntaryPanel.loadFrom(r.voluntaryWork);
        otherInfoPanel.loadFrom(r.otherInfo);
        questionnairePanel.loadFrom(r.questionnaire);
        notarizationPanel.loadFrom(r.notarization);
        attachmentsPanel.refresh(r);
        statusCombo.setSelectedItem(r.status);
        remarks.setText(r.remarks);
        idLabel.setText("ID: " + r.id.substring(0, 8) + "...");
    }

    /** Writes every field back into the currently loaded record and returns it. */
    public PdsRecord commitToRecord() {
        if (current == null) return null;
        personalInfoPanel.saveTo(current.personalInfo);
        familyPanel.saveTo(current.familyBackground);
        educationPanel.saveTo(current.education);
        eligibilityPanel.saveTo(current.eligibility);
        workPanel.saveTo(current.workExperience);
        ldPanel.saveTo(current.learningDevelopment);
        voluntaryPanel.saveTo(current.voluntaryWork);
        otherInfoPanel.saveTo(current.otherInfo);
        questionnairePanel.saveTo(current.questionnaire);
        notarizationPanel.saveTo(current.notarization);
        current.status = (RecordStatus) statusCombo.getSelectedItem();
        current.remarks = remarks.getText().trim();
        return current;
    }

    public PdsRecord getCurrent() { return current; }

    public void clear() {
        current = null;
        idLabel.setText(" ");
    }
}
