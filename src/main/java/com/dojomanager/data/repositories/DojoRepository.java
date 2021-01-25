package com.dojomanager.data.repositories;

import com.dojomanager.data.entities.dojo.Dojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface DojoRepository extends CrudRepository<Dojo, Integer> {
    Dojo findByName(String name);
}