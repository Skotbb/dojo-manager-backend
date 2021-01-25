package com.dojomanager.services;

import java.util.HashSet;
import java.util.List;
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
            dojo = dojoRepo.save(dojo);
            // dojo.getOwner().addDojo(dojo);
            return dojo;
        } catch (Exception e) {
            logger.error("Could not save Dojo " + dojo.getName(), e);
            return dojo;
        }
    }

    public Set<Dojo> saveAll(Set<Dojo> dojos) {
        return Sets.newHashSet(dojoRepo.saveAll(dojos));
    }

    public boolean isNameAvailable(String name) {
        return dojoRepo.findByName(name.toLowerCase()) == null;
    }

    public List<Dojo> addDojoToOwner(Dojo dojo, DojoOwner owner) {
        List<Dojo> currentDojos = owner.getManagedDojos();

        if(currentDojos.isEmpty() || !currentDojos.contains(dojo)) {
            dojo.setOwner(owner);   // why the fuck
            // dojoRepo.save(dojo);
            currentDojos.add(dojo);
            
            // ownerRepo.save(owner);

            return currentDojos;
        }
        else{
            logger.info("Dojo is already attached to " + owner.getEmail());
            return currentDojos;
        }
    }
}
