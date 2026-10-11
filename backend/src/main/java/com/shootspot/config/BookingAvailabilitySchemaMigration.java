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
        jdbcTemplate.execute(
                "ALTER TABLE bookings DROP CONSTRAINT IF EXISTS uk_booking_location_date_time");
    }
}
