package com.dojomanager.services;

import java.util.Optional;
import java.util.Set;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.DojoOwnerRepository;
import com.dojomanager.data.repositories.DojoRepository;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DojoService {
    @Autowired
    private DojoRepository dojoRepo;
    
    @Autowired
    private DojoOwnerRepository ownerRepo;
    
    private Logger logger = LoggerFactory.getLogger(DojoService.class);

    public Dojo saveDojo(Dojo dojo) {
        try {            
            dojo = dojoRepo.saveAndFlush(dojo);
            return dojo;
        } catch (Exception e) {
            logger.error("Could not save Dojo " + dojo.getName(), e);
            return dojo;
        }
    }

    public Set<Dojo> saveAll(Set<Dojo> dojos) {
        return Sets.newHashSet(dojoRepo.saveAll(dojos));
    }

    public DojoOwner addDojoToOwner(Dojo dojo, DojoOwner owner) {
        owner.addDojo(dojo);
        
        dojoRepo.save(dojo);
        return ownerRepo.save(owner);
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
            logger.warn("Could not find Dojo with id="+id);
            return new Dojo();
        }         
    }
}
