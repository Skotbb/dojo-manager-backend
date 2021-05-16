package com.dojomanager.data.repositories.dojo;

import java.util.Optional;

import com.dojomanager.data.entities.dojo.DojoOwner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DojoOwnerRepository extends JpaRepository<DojoOwner, Long> {
    Optional<DojoOwner> findByEmail(String email);
    boolean existsByEmail(String email);
}
