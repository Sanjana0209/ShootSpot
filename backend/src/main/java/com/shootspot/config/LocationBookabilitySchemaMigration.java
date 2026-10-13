package com.shootspot.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocationBookabilitySchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public LocationBookabilitySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = 'locations' " +
                "AND column_name = 'bookable'",
                Integer.class
        );

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE locations ADD COLUMN bookable BOOLEAN NOT NULL DEFAULT FALSE"
            );
        }
    }
}