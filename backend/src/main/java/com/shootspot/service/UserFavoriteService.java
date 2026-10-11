package com.shootspot.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shootspot.entity.Location;
import com.shootspot.entity.User;
import com.shootspot.entity.UserFavorite;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.UserFavoriteRepository;
import com.shootspot.repository.UserRepository;

@Service
public class UserFavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    public UserFavoriteService(
            UserFavoriteRepository favoriteRepository,
            UserRepository userRepository,
            LocationRepository locationRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<Location> getFavorites(String email) {
        User user = findUser(email);
        return favoriteRepository.findLocationsByUserIdOrderByFavoriteIdDesc(user.getId());
    }

    @Transactional
    public void addFavorite(Long locationId, String email) {
        User user = findUser(email);
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
        if (!favoriteRepository.existsByUser_IdAndLocation_Id(user.getId(), locationId)) {
            favoriteRepository.save(new UserFavorite(user, location));
        }
    }

    @Transactional
    public void removeFavorite(Long locationId, String email) {
        User user = findUser(email);
        favoriteRepository.deleteByUserIdAndLocationId(user.getId(), locationId);
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
