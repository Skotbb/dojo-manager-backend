package com.dojomanager.data.repositories;

import java.util.Set;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface DojoOwnerRepository extends CrudRepository<DojoOwner, Integer>{
    DojoOwner findByEmail(String email);
}
