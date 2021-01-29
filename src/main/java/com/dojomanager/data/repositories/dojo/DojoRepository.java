package com.dojomanager.data.repositories.dojo;

import java.util.List;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DojoRepository extends JpaRepository<Dojo, Long> {
    Dojo findByName(String name);

    List<Dojo> findByOwner(DojoOwner owner);
}