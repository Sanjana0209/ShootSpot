package com.shootspot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shootspot.entity.Location;
import com.shootspot.service.UserFavoriteService;

@RestController
@RequestMapping("/api/favorites")
public class UserFavoriteController {

    private final UserFavoriteService favoriteService;

    public UserFavoriteController(UserFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<Location> getFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        return favoriteService.getFavorites(userDetails.getUsername());
    }

    @PostMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavorite(
            @PathVariable Long locationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        favoriteService.addFavorite(locationId, userDetails.getUsername());
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable Long locationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        favoriteService.removeFavorite(locationId, userDetails.getUsername());
    }
}
