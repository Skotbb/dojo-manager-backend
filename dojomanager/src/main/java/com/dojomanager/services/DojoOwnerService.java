package com.dojomanager.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.DojoOwnerRepository;
import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class DojoOwnerService {
    @Autowired
    private DojoOwnerRepository ownerRepo;
    @Autowired
    private EntityManager entityManager;

    public DojoOwner saveDojoOwner(DojoOwner owner) {
        return ownerRepo.save(owner);
    }

    public DojoOwner getOwnerByEmail(String email) {
        return ownerRepo.findByEmail(email.toLowerCase());
    }

    public List<DojoOwner> getAllOwners() {
        return Lists.newArrayList(ownerRepo.findAll());
    }

    public List<Dojo> getDojos(DojoOwner owner) {
        Optional<DojoOwner> repoOwner = ownerRepo.findById(owner.getId());
        if(repoOwner.isPresent()) {
            return repoOwner.get().getManagedDojos();
        } else {
            return new ArrayList<>();
        }
    }

    // public DojoOwner getById(Integer id) {
    //     return ownerRepo.;
    // }
}
