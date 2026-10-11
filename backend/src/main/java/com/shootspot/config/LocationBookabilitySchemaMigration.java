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
        jdbcTemplate.execute(
                "ALTER TABLE locations ADD COLUMN IF NOT EXISTS bookable BOOLEAN NOT NULL DEFAULT FALSE");
    }
}
