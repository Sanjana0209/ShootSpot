package com.shootspot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_favorites_user_location", columnNames = { "user_id", "location_id" })
})
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_favorites_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_favorites_location"))
    private Location location;

    protected UserFavorite() {
    }

    public UserFavorite(User user, Location location) {
        this.user = user;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Location getLocation() {
        return location;
    }
}
