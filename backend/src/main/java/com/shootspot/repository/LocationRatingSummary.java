package com.shootspot.repository;

public interface LocationRatingSummary {
    Long getLocationId();

    Double getAverageRating();

    Long getReviewCount();
}
