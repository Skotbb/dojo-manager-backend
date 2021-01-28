package com.dojomanager.data.repositories;

import com.dojomanager.data.entities.dojo.DojoOwner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DojoOwnerRepository extends JpaRepository<DojoOwner, Long> {
    DojoOwner findByEmail(String email);
    boolean existsByEmail(String email);
}
