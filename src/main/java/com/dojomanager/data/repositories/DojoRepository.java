package com.dojomanager.data.repositories;

import com.dojomanager.data.entities.dojo.Dojo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DojoRepository extends JpaRepository<Dojo, Long> {
    Dojo findByName(String name);
}