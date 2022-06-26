package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.dto.dojo.OwnerDTO;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.services.DojoOwnerService;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/owner")
public class DojoOwnerController {
    @Autowired
    private DojoOwnerService ownerService;

    private Logger logger = LoggerFactory.getLogger(DojoOwnerController.class);

    @GetMapping(value = "/{userId}")
    public @ResponseBody Collection<Dojo> getDojos(@PathVariable(value = "userId") String email) {
        Optional<DojoOwner> owner = ownerService.getOwnerByEmail(email);
        if(owner.isPresent()) {
            return owner.get().getManagedDojos();
        } else {
            logger.error("Could not find an owner with the Email %s", email);
            return Collections.EMPTY_SET;
        }
    }

    @PostMapping(value = "/add")
    public @ResponseBody DojoOwner addDojoOwner(@RequestBody OwnerDTO owner) {
        if(!owner.getFirstName().isEmpty() 
        && !owner.getLastName().isEmpty() 
        && !owner.getEmail().isEmpty()){
                    
            DojoOwner newOwner = new DojoOwner(owner.getFirstName(), owner.getLastName(), owner.getEmail(), owner.getPassword());
            
            logger.debug("New Owner Created.");
            return ownerService.saveDojoOwner(newOwner);
        }
        
        return new DojoOwner();
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<DojoOwner> getAllOwners() {
        return ownerService.getAllOwners();
    }
}
