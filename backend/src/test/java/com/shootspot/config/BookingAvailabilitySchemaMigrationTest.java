package com.shootspot.config;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import org.mockito.Mockito;

class BookingAvailabilitySchemaMigrationTest {

    @Test
    void removesLegacyUniqueConstraintThatBlocksRebookingCancelledSlots() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        BookingAvailabilitySchemaMigration migration =
                new BookingAvailabilitySchemaMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute(
                "ALTER TABLE bookings DROP CONSTRAINT IF EXISTS uk_booking_location_date_time");
    }

    @Test
    void addsBookabilityColumnWithSafeDefaultIdempotently() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        LocationBookabilitySchemaMigration migration =
                new LocationBookabilitySchemaMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute(
                "ALTER TABLE locations ADD COLUMN IF NOT EXISTS bookable BOOLEAN NOT NULL DEFAULT FALSE");
    }
}
