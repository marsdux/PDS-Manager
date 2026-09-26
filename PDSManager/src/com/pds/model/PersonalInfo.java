package com.pds.model;

public class PersonalInfo {
    // 1-3
    public String surname = "";
    public String firstName = "";
    public String middleName = "";
    public String nameExtension = "";
    public String dateOfBirth = ""; // dd/mm/yyyy
    // 4-9
    public String placeOfBirth = "";
    public String sexAtBirth = "";       // Male / Female
    public String civilStatus = "";      // Single/Married/Widow-er/Separated/Solo Parent/Others
    public String civilStatusOther = "";
    public String height = "";
    public String weight = "";
    public String bloodType = "";
    // 10-15
    public String umidIdNo = "";
    public String pagIbigIdNo = "";
    public String philHealthNo = "";
    public String philSysCardNo = "";
    public String tinNo = "";
    public String agencyEmployeeNo = "";
    // 16
    public boolean citizenFilipino = true;
    public boolean citizenDual = false;
    public String dualCitizenshipCountry = "";
    public boolean dualByBirth = true; // by birth / by naturalization
    // photo (optional) - Base64-encoded JPEG, downscaled at attach time; kept
    // well under the record size guard in PdsRecordStore
    public String photoBase64 = "";
    // 17 residential address
    public String resHouseBlockLot = "";
    public String resStreet = "";
    public String resSubdivisionVillage = "";
    public String resBarangay = "";
    public String resCityMunicipality = "";
    public String resProvince = "";
    public String resZipCode = "";
    // 18 permanent address
    public String permHouseBlockLot = "";
    public String permStreet = "";
    public String permSubdivisionVillage = "";
    public String permBarangay = "";
    public String permCityMunicipality = "";
    public String permProvince = "";
    public String permZipCode = "";
    // 19-21
    public String telephoneNo = "";
    public String mobileNo = "";
    public String emailAddress = "";
}
