package com.shootspot.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingAvailabilitySchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public BookingAvailabilitySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer constraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = 'bookings' " +
                "AND constraint_name = 'uk_booking_location_date_time'",
                Integer.class
        );

        if (constraintCount != null && constraintCount > 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE bookings DROP INDEX uk_booking_location_date_time"
            );
        }
    }
}