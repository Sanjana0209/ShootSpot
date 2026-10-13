package com.shootspot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shootspot.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}
