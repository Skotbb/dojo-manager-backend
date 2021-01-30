package com.dojomanager.data.repositories.people;

import com.dojomanager.data.entities.people.Address;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long>{
    
}
