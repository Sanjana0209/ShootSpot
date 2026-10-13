BEGIN;

ALTER TABLE reviews
    ALTER COLUMN booking_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reviews_location_user_without_booking
    ON reviews (user_id, location_id)
    WHERE booking_id IS NULL;

COMMIT;
