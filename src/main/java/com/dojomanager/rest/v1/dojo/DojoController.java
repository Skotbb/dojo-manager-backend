package com.dojomanager.rest.v1.dojo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.repositories.DojoOwnerRepository;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping(path = "/dojo")
public class DojoController {
    @Autowired
    private DojoOwnerService ownerService;
    @Autowired
    private DojoService dojoService;

    private Logger logger = LoggerFactory.getLogger(DojoController.class);

    // @PostMapping(value="/add")
    // public @ResponseBody DojoOwner addDojoOwner(@RequestParam String firstName, 
    // @RequestParam String lastName, @RequestParam String email, @RequestParam String password) {
        
    //     DojoOwner owner = new DojoOwner();
    //     owner.setFirstName(firstName);
    //     owner.setLastName(lastName);
    //     owner.setEmail(email);
    //     owner.setPassword(password);
        
    //     return ownerRepo.save(owner);
    // }
    
    @GetMapping(value="/test/add")
    public @ResponseBody DojoOwner addDojoOwner() {
        
        DojoOwner owner = new DojoOwner();
        owner.setFirstName("Scott");
        owner.setLastName("Thompson");
        owner.setEmail("skotbb@gmail.com");
        owner.setPassword("Password1@");
        ownerService.saveDojoOwner(owner);

        Dojo dojo = new Dojo();
        dojo.setName("Dojo of the Stars!");
        dojo.setWebsite("www.kickyourass.com");
        dojoService.saveDojo(dojo);
        dojoService.addDojoToOwner(dojo, owner);
        
        Dojo dojo1 = new Dojo();
        dojo1.setName("KobraKai");
        dojo1.setWebsite("www.kobra-kai.com");
        dojoService.saveDojo(dojo1);
        dojoService.addDojoToOwner(dojo1, owner);

        try {
            List<Dojo> dojoSet = ownerService.getDojos(owner);

            for(Dojo testDojo : dojoSet) {
                logger.debug(testDojo.getName());
            }
            return owner;
        }
        catch(DuplicateKeyException e) {
            logger.error("DojoOwner " + owner.getEmail() + " already exists.", e);
            return new DojoOwner();
        } 
        catch (Exception e) {
            logger.error("There was an issue saving the DojoOwner", e);
            return new DojoOwner();
        }
    }

    @GetMapping(path="/getAll")
    public @ResponseBody Iterable<DojoOwner> getAllOwners() {
        return ownerService.getAllOwners();
    }
}
