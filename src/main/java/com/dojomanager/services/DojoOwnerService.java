package com.dojomanager.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.dojo.*;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DojoOwnerService {

    private static Logger logger = LoggerFactory.getLogger(DojoOwnerService.class);

    @Autowired
    private DojoOwnerRepository ownerRepo;

    public DojoOwner saveDojoOwner(DojoOwner owner) {
        if(ownerRepo.existsByEmail(owner.getEmail())) {
            Optional<DojoOwner> dbOwner = getOwnerByEmail(owner.getEmail());
            dbOwner.ifPresent(thisOwner -> owner.setId(thisOwner.getId()) );            
        }
        return ownerRepo.saveAndFlush(owner);
    }

    public Optional<DojoOwner> getOwnerByEmail(String email) throws UsernameNotFoundException{
        return ownerRepo.findByEmail(email.toLowerCase());
    }

    public boolean doesEmailExist(String email) {
        return ownerRepo.existsByEmail(email);
    }

    public DojoOwner getOwnerById(Long id) {
        Optional<DojoOwner> optionalOwner = ownerRepo.findById(id);
        if(optionalOwner.isPresent()) {
            return optionalOwner.get();
        }
        else {
            logger.warn("Could not find the Owner by the ID=%s", id);
            return new DojoOwner();
        }
    }

    public List<DojoOwner> getAllOwners() {
        return Lists.newArrayList(ownerRepo.findAll());
    }

    public Set<Dojo> getDojos(DojoOwner owner) {
        DojoOwner repoOwner = ownerRepo.findById(owner.getId()).orElse(null);
        if(repoOwner != null) {
            return repoOwner.getManagedDojos();
        } else {
            return new HashSet<>();
        }
    }
}
