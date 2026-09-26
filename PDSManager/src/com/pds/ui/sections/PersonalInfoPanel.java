package com.pds.ui.sections;

import com.pds.model.PersonalInfo;
import com.pds.ui.FormUtil;
import com.pds.util.Countries;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class PersonalInfoPanel extends JScrollPane {

    private final JTextField surname = FormUtil.tf(), firstName = FormUtil.tf(), middleName = FormUtil.tf(),
            nameExtension = FormUtil.tf(), dob = FormUtil.tf(), placeOfBirth = FormUtil.tf(),
            height = FormUtil.tf(), weight = FormUtil.tf(),
            umid = FormUtil.tf(), pagibig = FormUtil.tf(), philhealth = FormUtil.tf(), philsys = FormUtil.tf(),
            tin = FormUtil.tf(), agencyEmpNo = FormUtil.tf(),
            resHouse = FormUtil.tf(), resStreet = FormUtil.tf(), resSubd = FormUtil.tf(), resBrgy = FormUtil.tf(),
            resCity = FormUtil.tf(), resProvince = FormUtil.tf(), resZip = FormUtil.tf(),
            permHouse = FormUtil.tf(), permStreet = FormUtil.tf(), permSubd = FormUtil.tf(), permBrgy = FormUtil.tf(),
            permCity = FormUtil.tf(), permProvince = FormUtil.tf(), permZip = FormUtil.tf(),
            telephone = FormUtil.tf(), mobile = FormUtil.tf(), email = FormUtil.tf();

    private final JComboBox<String> sex = new JComboBox<>(new String[]{"", "Male", "Female"});
    private final JComboBox<String> civilStatus = new JComboBox<>(
            new String[]{"", "Single", "Married", "Widow/er", "Separated", "Solo Parent", "Others"});
    private final JTextField civilStatusOther = FormUtil.tf();
    private final JComboBox<String> bloodType = new JComboBox<>(
            new String[]{"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});

    // ---- 16. Citizenship: Filipino, or Dual Citizenship (which unlocks its own follow-up questions) ----
    private final JRadioButton citizenFilipino = new JRadioButton("Filipino", true);
    private final JRadioButton citizenDualRb = new JRadioButton("Dual Citizenship");
    private final JRadioButton dualByBirth = new JRadioButton("by birth", true);
    private final JRadioButton dualByNaturalization = new JRadioButton("by naturalization");
    private final JComboBox<String> dualCountry = new JComboBox<>(Countries.LIST);

    // ---- 2x2 ID photo, stored as a square-cropped, downscaled JPEG/Base64 ----
    private static final int PHOTO_STORED_DIM = 400;  // px, saved/printed resolution
    private static final int PHOTO_PREVIEW_DIM = 200; // px on screen (represents the 2in x 2in square)
    private final JLabel photoPreview = new JLabel("<html><center>2x2<br>No Photo</center></html>", SwingConstants.CENTER);
    private String photoBase64 = "";

    public PersonalInfoPanel() {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        setViewportView(content);
        getVerticalScrollBar().setUnitIncrement(16);

        ButtonGroup citizenshipGroup = new ButtonGroup();
        citizenshipGroup.add(citizenFilipino);
        citizenshipGroup.add(citizenDualRb);
        ButtonGroup dualTypeGroup = new ButtonGroup();
        dualTypeGroup.add(dualByBirth);
        dualTypeGroup.add(dualByNaturalization);
        updateDualFieldsEnabled();
        citizenFilipino.addActionListener(e -> updateDualFieldsEnabled());
        citizenDualRb.addActionListener(e -> updateDualFieldsEnabled());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(photoSection(), BorderLayout.WEST);
        top.add(basicInfoSection(), BorderLayout.CENTER);
        content.add(top);
        content.add(addressSection("17. Residential Address", resHouse, resStreet, resSubd, resBrgy, resCity, resProvince, resZip));
        content.add(addressSection("18. Permanent Address", permHouse, permStreet, permSubd, permBrgy, permCity, permProvince, permZip));
        content.add(contactSection());
    }

    private void updateDualFieldsEnabled() {
        boolean dual = citizenDualRb.isSelected();
        dualByBirth.setEnabled(dual);
        dualByNaturalization.setEnabled(dual);
        dualCountry.setEnabled(dual);
    }

    private JPanel photoSection() {
        JPanel p = FormUtil.section("2x2 ID Photo");
        photoPreview.setPreferredSize(new Dimension(PHOTO_PREVIEW_DIM, PHOTO_PREVIEW_DIM));
        photoPreview.setOpaque(true);
        photoPreview.setBackground(Color.WHITE);
        photoPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton attach = new JButton("Attach...");
        JButton remove = new JButton("Remove");
        attach.addActionListener(e -> attachPhoto());
        remove.addActionListener(e -> { photoBase64 = ""; renderPhoto(); });

        GridBagConstraints gbc = FormUtil.baseConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        p.add(photoPreview, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        p.add(attach, gbc);
        gbc.gridx = 1;
        p.add(remove, gbc);
        return p;
    }

    private void attachPhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage original = ImageIO.read(chooser.getSelectedFile());
            if (original == null) {
                JOptionPane.showMessageDialog(this, "That file doesn't look like a supported image.",
                        "Invalid Image", JOptionPane.ERROR_MESSAGE);
                return;
            }
            BufferedImage square = cropToSquare(original);
            BufferedImage scaled = scaleTo(square, PHOTO_STORED_DIM);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "jpg", out);
            photoBase64 = Base64.getEncoder().encodeToString(out.toByteArray());
            renderPhoto();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Center-crops the largest possible square out of the source image, matching a 2x2 ID photo's 1:1 aspect ratio. */
    private static BufferedImage cropToSquare(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2, y = (h - side) / 2;
        return src.getSubimage(x, y, side, side);
    }

    private static BufferedImage scaleTo(BufferedImage src, int dim) {
        BufferedImage out = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim, dim);
        g2.drawImage(src, 0, 0, dim, dim, null);
        g2.dispose();
        return out;
    }

    private void renderPhoto() {
        if (photoBase64.isBlank()) {
            photoPreview.setIcon(null);
            photoPreview.setText("<html><center>2x2<br>No Photo</center></html>");
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(photoBase64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            Image scaledForScreen = img.getScaledInstance(PHOTO_PREVIEW_DIM, PHOTO_PREVIEW_DIM, Image.SCALE_SMOOTH);
            photoPreview.setText(null);
            photoPreview.setIcon(new ImageIcon(scaledForScreen));
        } catch (Exception ex) {
            photoPreview.setIcon(null);
            photoPreview.setText("(unreadable)");
        }
    }

    private JPanel basicInfoSection() {
        var p = FormUtil.section("I. Personal Information");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "1. Surname", surname, "Name Extension (Jr./Sr.)", nameExtension);
        row = FormUtil.addRow(p, gbc, row, "2. First Name", firstName, "3. Middle Name", middleName);
        row = FormUtil.addRow(p, gbc, row, "Date of Birth (dd/mm/yyyy)", dob, "4. Place of Birth", placeOfBirth);
        row = FormUtil.addRow(p, gbc, row, "5. Sex at Birth", sex, "6. Civil Status", civilStatus);
        row = FormUtil.addRow(p, gbc, row, "  If \"Others\", specify", civilStatusOther, "7. Height (m)", height);
        row = FormUtil.addRow(p, gbc, row, "8. Weight (kg)", weight, "9. Blood Type", bloodType);
        row = FormUtil.addRow(p, gbc, row, "10. UMID ID No.", umid, "11. Pag-IBIG ID No.", pagibig);
        row = FormUtil.addRow(p, gbc, row, "12. PhilHealth No.", philhealth, "13. PhilSys Card Number", philsys);
        row = FormUtil.addRow(p, gbc, row, "14. TIN No.", tin, "15. Agency Employee No.", agencyEmpNo);
        row = citizenshipRows(p, gbc, row);
        return p;
    }

    /** Item 16: Citizenship - Filipino, or Dual Citizenship with its own by-birth/naturalization + country follow-up. */
    private int citizenshipRows(JPanel p, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
        p.add(new JLabel("16. Citizenship"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        JPanel choicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        choicePanel.add(citizenFilipino);
        choicePanel.add(citizenDualRb);
        p.add(choicePanel, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel("  If dual, acquired"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        JPanel byPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        byPanel.add(dualByBirth);
        byPanel.add(dualByNaturalization);
        p.add(byPanel, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel("  If dual, country"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        p.add(dualCountry, gbc);
        return row + 1;
    }

    private JPanel addressSection(String title, JTextField house, JTextField street, JTextField subd,
                                   JTextField brgy, JTextField city, JTextField province, JTextField zip) {
        var p = FormUtil.section(title);
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "House/Block/Lot No.", house, "Street", street);
        row = FormUtil.addRow(p, gbc, row, "Subdivision/Village", subd, "Barangay", brgy);
        row = FormUtil.addRow(p, gbc, row, "City/Municipality", city, "Province", province);
        row = FormUtil.addRow(p, gbc, row, "Zip Code", zip, "", new JLabel());
        return p;
    }

    private JPanel contactSection() {
        var p = FormUtil.section("Contact Information");
        var gbc = FormUtil.baseConstraints();
        int row = 0;
        row = FormUtil.addRow(p, gbc, row, "19. Telephone No.", telephone, "20. Mobile No.", mobile);
        row = FormUtil.addRow(p, gbc, row, "21. Email Address", email, "", new JLabel());
        return p;
    }

    public void loadFrom(PersonalInfo p) {
        surname.setText(p.surname); firstName.setText(p.firstName); middleName.setText(p.middleName);
        nameExtension.setText(p.nameExtension); dob.setText(p.dateOfBirth); placeOfBirth.setText(p.placeOfBirth);
        sex.setSelectedItem(p.sexAtBirth); civilStatus.setSelectedItem(p.civilStatus);
        civilStatusOther.setText(p.civilStatusOther);
        height.setText(p.height); weight.setText(p.weight); bloodType.setSelectedItem(p.bloodType);
        umid.setText(p.umidIdNo); pagibig.setText(p.pagIbigIdNo); philhealth.setText(p.philHealthNo);
        philsys.setText(p.philSysCardNo); tin.setText(p.tinNo); agencyEmpNo.setText(p.agencyEmployeeNo);

        if (p.citizenDual) citizenDualRb.setSelected(true); else citizenFilipino.setSelected(true);
        if (p.dualByBirth) dualByBirth.setSelected(true); else dualByNaturalization.setSelected(true);
        dualCountry.setSelectedItem(p.dualCitizenshipCountry);
        updateDualFieldsEnabled();

        photoBase64 = p.photoBase64 == null ? "" : p.photoBase64;
        renderPhoto();
        resHouse.setText(p.resHouseBlockLot); resStreet.setText(p.resStreet); resSubd.setText(p.resSubdivisionVillage);
        resBrgy.setText(p.resBarangay); resCity.setText(p.resCityMunicipality); resProvince.setText(p.resProvince);
        resZip.setText(p.resZipCode);
        permHouse.setText(p.permHouseBlockLot); permStreet.setText(p.permStreet); permSubd.setText(p.permSubdivisionVillage);
        permBrgy.setText(p.permBarangay); permCity.setText(p.permCityMunicipality); permProvince.setText(p.permProvince);
        permZip.setText(p.permZipCode);
        telephone.setText(p.telephoneNo); mobile.setText(p.mobileNo); email.setText(p.emailAddress);
    }

    public void saveTo(PersonalInfo p) {
        p.surname = surname.getText().trim(); p.firstName = firstName.getText().trim();
        p.middleName = middleName.getText().trim(); p.nameExtension = nameExtension.getText().trim();
        p.dateOfBirth = dob.getText().trim(); p.placeOfBirth = placeOfBirth.getText().trim();
        p.sexAtBirth = str(sex.getSelectedItem()); p.civilStatus = str(civilStatus.getSelectedItem());
        p.civilStatusOther = civilStatusOther.getText().trim();
        p.height = height.getText().trim(); p.weight = weight.getText().trim();
        p.bloodType = str(bloodType.getSelectedItem());
        p.umidIdNo = umid.getText().trim(); p.pagIbigIdNo = pagibig.getText().trim();
        p.philHealthNo = philhealth.getText().trim(); p.philSysCardNo = philsys.getText().trim();
        p.tinNo = tin.getText().trim(); p.agencyEmployeeNo = agencyEmpNo.getText().trim();

        p.citizenDual = citizenDualRb.isSelected();
        p.citizenFilipino = citizenFilipino.isSelected();
        p.dualByBirth = dualByBirth.isSelected();
        p.dualCitizenshipCountry = citizenDualRb.isSelected() ? str(dualCountry.getSelectedItem()) : "";
        p.photoBase64 = photoBase64;

        p.resHouseBlockLot = resHouse.getText().trim(); p.resStreet = resStreet.getText().trim();
        p.resSubdivisionVillage = resSubd.getText().trim(); p.resBarangay = resBrgy.getText().trim();
        p.resCityMunicipality = resCity.getText().trim(); p.resProvince = resProvince.getText().trim();
        p.resZipCode = resZip.getText().trim();
        p.permHouseBlockLot = permHouse.getText().trim(); p.permStreet = permStreet.getText().trim();
        p.permSubdivisionVillage = permSubd.getText().trim(); p.permBarangay = permBrgy.getText().trim();
        p.permCityMunicipality = permCity.getText().trim(); p.permProvince = permProvince.getText().trim();
        p.permZipCode = permZip.getText().trim();
        p.telephoneNo = telephone.getText().trim(); p.mobileNo = mobile.getText().trim();
        p.emailAddress = email.getText().trim();
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
