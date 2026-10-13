package com.shootspot.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shootspot.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select location from Location location where location.id = :id")
    Optional<Location> findByIdForUpdate(@Param("id") Long id);

}
