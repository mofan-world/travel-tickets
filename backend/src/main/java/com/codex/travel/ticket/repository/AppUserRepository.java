package com.codex.travel.ticket.repository;

import java.util.Optional;

import com.codex.travel.ticket.entity.AppUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
