package com.shootspot.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateReviewRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsRatingFromOneThroughFiveAndValidComment() {
        assertTrue(validator.validate(new CreateReviewRequest(1, "Good location")).isEmpty());
        assertTrue(validator.validate(new CreateReviewRequest(5, "Great location")).isEmpty());
    }

    @Test
    void rejectsRatingsOutsideOneThroughFiveAndMissingRating() {
        assertFalse(validator.validate(new CreateReviewRequest(0, "Review text")).isEmpty());
        assertFalse(validator.validate(new CreateReviewRequest(6, "Review text")).isEmpty());
        assertFalse(validator.validate(new CreateReviewRequest(null, "Review text")).isEmpty());
    }

    @Test
    void rejectsBlankShortAndOversizedReviewText() {
        assertFalse(validator.validate(new CreateReviewRequest(4, "   ")).isEmpty());
        assertFalse(validator.validate(new CreateReviewRequest(4, "ok")).isEmpty());
        assertFalse(validator.validate(new CreateReviewRequest(4, "x".repeat(501))).isEmpty());
    }
}
