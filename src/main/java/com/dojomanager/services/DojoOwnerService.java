package com.dojomanager.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.dojo.*;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.exceptions.ForbiddenOperationException;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Resolves the authenticated owner. Identity always comes from the JWT
     * principal, never from a request path or body.
     */
    public DojoOwner requireOwner(OwnerPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ForbiddenOperationException("An authenticated dojo owner is required");
        }

        return ownerRepo.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Authenticated account no longer exists"));
    }

    /**
     * Loads the authenticated owner together with its managed dojos, so the
     * response can be mapped to a DTO without touching a detached lazy
     * collection.
     */
    @Transactional(readOnly = true)
    public DojoOwner getOwnerProfile(OwnerPrincipal principal) {
        DojoOwner owner = requireOwner(principal);
        owner.getManagedDojos().size();
        return owner;
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
