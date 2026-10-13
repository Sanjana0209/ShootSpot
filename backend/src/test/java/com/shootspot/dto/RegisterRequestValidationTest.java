package com.shootspot.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsNormalizedValidRegistration() {
        RegisterRequest request = new RegisterRequest(
                "  Sai Kumar  ", "  Sai.Name@college.ac.in  ", "9876543210", "GoodPass1!");
        RegisterRequest sixStartingMobile = new RegisterRequest(
                "Sai Kumar", "sai@gmail.com", "6123456789", "GoodPass1!");

        assertTrue(validator.validate(request).isEmpty());
        assertTrue(validator.validate(sixStartingMobile).isEmpty());
        assertEquals("Sai Kumar", request.name());
        assertEquals("Sai.Name@college.ac.in", request.email());
    }

    @Test
    void rejectsInvalidNamesEmailsPhoneNumbersAndPasswords() {
        assertInvalid("", "", "", "");
        assertInvalid("  ", "person@example.com", "9876543210", "GoodPass1!");
        assertInvalid("Al", "person@example.com", "9876543210", "GoodPass1!");
        assertInvalid("A1!", "person@example.com", "9876543210", "GoodPass1!");
        assertInvalid("A".repeat(31), "person@example.com", "9876543210", "GoodPass1!");
        assertInvalid("Person", "person.gmail.com", "9876543210", "GoodPass1!");
        assertInvalid("Person", "person@", "9876543210", "GoodPass1!");
        assertInvalid("Person", "@gmail.com", "9876543210", "GoodPass1!");
        assertInvalid("Person", "person@invalid", "9876543210", "GoodPass1!");
        assertInvalid("Person", "person @example.com", "9876543210", "GoodPass1!");
        assertInvalid("Person", "a".repeat(245) + "@gmail.com", "9876543210", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "5123456789", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "987654321", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "98765432101", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "98AB543210", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "987 543210", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "9876543210 ", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "+919876543210", "GoodPass1!");
        assertInvalid("Person", "person@example.com", "9876543210", "Ab1!");
        assertInvalid("Person", "person@example.com", "9876543210", "weakpass1!");
        assertInvalid("Person", "person@example.com", "9876543210", "GOODPASS1!");
        assertInvalid("Person", "person@example.com", "9876543210", "GoodPass!!");
        assertInvalid("Person", "person@example.com", "9876543210", "GoodPass1");
        assertInvalid("Person", "person@example.com", "9876543210", "GoodPass1!TooLong");
    }

    private void assertInvalid(String name, String email, String phone, String password) {
        RegisterRequest request = new RegisterRequest(name, email, phone, password);
        assertFalse(validator.validate(request).isEmpty());
    }
}
