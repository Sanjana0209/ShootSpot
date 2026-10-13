package com.shootspot.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoginRequestValidationTest {

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
    void trimsEmailAndAcceptsExistingPasswordsWithoutRegistrationComplexity() {
        LoginRequest request = new LoginRequest("  person@example.com  ", "old");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("person@example.com", request.email());
    }

    @Test
    void rejectsMissingAndMalformedEmailAndPassword() {
        assertInvalid("", "");
        assertInvalid("person.gmail.com", "password");
        assertInvalid("@gmail.com", "password");
        assertInvalid("person@", "password");
        assertInvalid("person @example.com", "password");
        assertInvalid("person@example.com", "  ");
        assertInvalid("a".repeat(245) + "@gmail.com", "password");
    }

    private void assertInvalid(String email, String password) {
        assertFalse(validator.validate(new LoginRequest(email, password)).isEmpty());
    }
}
