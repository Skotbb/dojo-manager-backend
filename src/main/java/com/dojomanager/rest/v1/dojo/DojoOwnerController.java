package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.dto.dojo.NewDojoDto;
import com.dojomanager.data.dto.dojo.OwnerDTO;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;
import com.dojomanager.services.RankService;

import java.util.List;
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
    @Autowired
    private DojoService dojoService;
    @Autowired
    private RankService rankService;

    private Logger logger = LoggerFactory.getLogger(DojoOwnerController.class);

    @GetMapping(value = "/{userId}")
    public @ResponseBody DojoOwner getDojos(@PathVariable(value = "userId") String email) {
        Optional<DojoOwner> owner = ownerService.getOwnerByEmail(email);
        if(owner.isPresent()) {
            return owner.get();
        } else {
            logger.error("Could not find an owner with the Email %s", email);
            return null;
        }
    }
    
    @PostMapping(value = "/{userId}")
    public @ResponseBody Dojo getDojos(@PathVariable(value = "userId") String email, @RequestBody Dojo newDojo) {
        Optional<DojoOwner> owner = ownerService.getOwnerByEmail(email);
        if(owner.isPresent()) {
            final Dojo createdDojo = dojoService.saveDojo(newDojo);
            dojoService.addDojoToOwner(createdDojo, owner.get());

            return createdDojo;
        } else {
            logger.error("Could not find an owner with the Email %s", email);
            return null;
        }
    }

    @PostMapping(value = "/{userId}/dojo")
    public @ResponseBody Dojo addNewDojo(@PathVariable(value = "userId") String email, @RequestBody NewDojoDto newDojo) {
        Optional<DojoOwner> optOwner = ownerService.getOwnerByEmail(email);

        if(optOwner.isPresent()) {
            Optional<Dojo> createdDojo = dojoService.createDojoForOwner(newDojo.getDojo(), optOwner.get());
            
            if(createdDojo.isPresent()) {
                List<RankSetting> rankSettings = rankService.createRankSettingsForDojo(createdDojo.get(), newDojo.getRankSettings());
                return createdDojo.get();
            }
            else {
                logger.error("Was not able to create a Dojo.");
                return null;
            }
        } 
        else {
            logger.error("Could not find owner with Email: ", email);
            return null;
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
