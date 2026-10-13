package com.shootspot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shootspot.entity.Location;
import com.shootspot.entity.UserFavorite;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    @Query("select favorite.location from UserFavorite favorite "
            + "where favorite.user.id = :userId order by favorite.id desc")
    List<Location> findLocationsByUserIdOrderByFavoriteIdDesc(@Param("userId") Long userId);

    boolean existsByUser_IdAndLocation_Id(Long userId, Long locationId);

    @Modifying
    @Query("delete from UserFavorite favorite "
            + "where favorite.user.id = :userId and favorite.location.id = :locationId")
    void deleteByUserIdAndLocationId(
            @Param("userId") Long userId, @Param("locationId") Long locationId);
}
