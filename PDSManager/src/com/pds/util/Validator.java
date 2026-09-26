package com.pds.util;

import com.pds.model.PdsRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validates a PdsRecord before it is allowed to move to COMPLETED status. */
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validator() {}

    public static List<String> validateForCompletion(PdsRecord r) {
        List<String> errors = new ArrayList<>();
        var p = r.personalInfo;
        require(errors, p.surname, "Surname is required");
        require(errors, p.firstName, "First name is required");
        require(errors, p.dateOfBirth, "Date of birth is required");
        require(errors, p.placeOfBirth, "Place of birth is required");
        require(errors, p.sexAtBirth, "Sex at birth is required");
        require(errors, p.civilStatus, "Civil status is required");
        require(errors, p.resCityMunicipality, "Residential city/municipality is required");
        require(errors, p.permCityMunicipality, "Permanent city/municipality is required");

        if (!p.emailAddress.isBlank() && !EMAIL.matcher(p.emailAddress.trim()).matches()) {
            errors.add("Email address format looks invalid");
        }
        if (r.education.isEmpty()) {
            errors.add("At least one educational background entry is required");
        }
        return errors;
    }

    private static void require(List<String> errors, String value, String message) {
        if (value == null || value.isBlank()) errors.add(message);
    }
}
