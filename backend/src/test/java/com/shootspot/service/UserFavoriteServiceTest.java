package com.shootspot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shootspot.entity.Location;
import com.shootspot.entity.User;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.UserFavoriteRepository;
import com.shootspot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceTest {

    @Mock
    private UserFavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    private UserFavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new UserFavoriteService(
                favoriteRepository, userRepository, locationRepository);
    }

    @Test
    void returnsOnlyFavoritesBelongingToTheAuthenticatedUser() {
        User userA = user(101L, "a@example.com");
        User userB = user(202L, "b@example.com");
        Location locationA = location(11L);
        Location locationB = location(22L);
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(userA));
        when(userRepository.findByEmailIgnoreCase("b@example.com")).thenReturn(Optional.of(userB));
        when(favoriteRepository.findLocationsByUserIdOrderByFavoriteIdDesc(101L))
                .thenReturn(List.of(locationA));
        when(favoriteRepository.findLocationsByUserIdOrderByFavoriteIdDesc(202L))
                .thenReturn(List.of(locationB));

        assertEquals(List.of(locationA), favoriteService.getFavorites("a@example.com"));
        assertEquals(List.of(locationB), favoriteService.getFavorites("b@example.com"));
        verify(favoriteRepository).findLocationsByUserIdOrderByFavoriteIdDesc(101L);
        verify(favoriteRepository).findLocationsByUserIdOrderByFavoriteIdDesc(202L);
    }

    @Test
    void addsFavoriteForAuthenticatedUserOnlyWhenNotAlreadyPresent() {
        User user = user(101L, "a@example.com");
        Location location = location(11L);
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(user));
        when(locationRepository.findById(11L)).thenReturn(Optional.of(location));
        when(favoriteRepository.existsByUser_IdAndLocation_Id(101L, 11L)).thenReturn(true);

        favoriteService.addFavorite(11L, "a@example.com");

        verify(favoriteRepository).existsByUser_IdAndLocation_Id(101L, 11L);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void removesFavoriteUsingAuthenticatedUserIdAndLocationId() {
        User user = user(101L, "a@example.com");
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(user));

        favoriteService.removeFavorite(11L, "a@example.com");

        verify(favoriteRepository).deleteByUserIdAndLocationId(101L, 11L);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Location location(Long id) {
        Location location = new Location();
        location.setId(id);
        return location;
    }
}
