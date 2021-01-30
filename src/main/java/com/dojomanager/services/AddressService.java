package com.dojomanager.services;

import com.dojomanager.data.entities.people.Address;
import com.dojomanager.data.repositories.people.AddressRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressService {
    
    @Autowired
    private AddressRepository addressRepo;

    public Address saveAddress(Address address) {
        return addressRepo.save(address);
    }
}
