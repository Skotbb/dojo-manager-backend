package com.dojomanager.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dojomanager.data.dto.dojo.DojoUpdateRequest;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Address;
import com.dojomanager.data.repositories.dojo.*;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.exceptions.ResourceNotFoundException;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DojoService {
    @Autowired
    private DojoRepository dojoRepo;
    
    @Autowired
    private DojoOwnerService ownerService;

    @Autowired
    private AddressService addressService;
    
    private Logger logger = LoggerFactory.getLogger(DojoService.class);

    /**
     * Returns only the dojos managed by the authenticated principal.
     */
    @Transactional(readOnly = true)
    public List<Dojo> listDojos(OwnerPrincipal principal) {
        DojoOwner owner = ownerService.requireOwner(principal);
        return dojoRepo.findByOwner(owner);
    }

    /**
     * Creates a new dojo owned by the authenticated principal.
     */
    @Transactional
    public Dojo createDojo(OwnerPrincipal principal, DojoUpdateRequest request) {
        DojoOwner owner = ownerService.requireOwner(principal);

        Dojo dojo = new Dojo();
        applyRequest(dojo, request);
        dojo.setOwner(owner);

        return dojoRepo.saveAndFlush(dojo);
    }

    /**
     * Updates one of the authenticated principal's own dojos. A dojo that
     * belongs to another owner is reported as not found so the API never
     * reveals that it exists.
     */
    @Transactional
    public Dojo updateDojo(OwnerPrincipal principal, Long dojoId, DojoUpdateRequest request) {
        DojoOwner owner = ownerService.requireOwner(principal);

        Dojo dojo = dojoRepo.findByOwner(owner).stream()
                .filter(candidate -> dojoId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Dojo " + dojoId + " was not found"));

        applyRequest(dojo, request);

        return dojoRepo.saveAndFlush(dojo);
    }

    private void applyRequest(Dojo dojo, DojoUpdateRequest request) {
        if (request.getName() != null) {
            dojo.setName(request.getName());
        }
        if (request.getWebsite() != null) {
            dojo.setWebsite(request.getWebsite());
        }
        if (request.getAddress() != null) {
            Address address = new Address(request.getAddress(), null, null, null, null);
            dojo.setAddress(addressService.saveAddress(address));
        }
    }

    public Dojo saveDojo(Dojo dojo) {
        return dojoRepo.saveAndFlush(dojo);
    }

    public Set<Dojo> saveAll(Set<Dojo> dojos) {
        return Sets.newHashSet(dojoRepo.saveAll(dojos));
    }

    public DojoOwner addDojoToOwner(Dojo dojo, DojoOwner owner) {
        if(owner.getId() == null) {
            ownerService.saveDojoOwner(owner);
        }
        owner.addDojo(dojo);
        
        dojoRepo.saveAndFlush(dojo);
        return ownerService.saveDojoOwner(owner);
    }

    public boolean isNameAvailable(String name) {
        return dojoRepo.findByName(name.toLowerCase()) == null;
    }

    public Dojo getDojoById(Long id) {
        Optional<Dojo> dojoOption = dojoRepo.findById(id);
        if(dojoOption.isPresent()) {
            return dojoOption.get();
        }
        else {
            logger.warn("Could not find Dojo with id=%s", id);
            return new Dojo();
        }         
    }

    public List<Dojo> getDojosForOwner(DojoOwner owner) {
        return dojoRepo.findByOwner(owner);
    }
}
