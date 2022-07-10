package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;
import com.dojomanager.services.RankService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping(path = "/dojo")
public class DojoController {
    @Autowired
    private DojoOwnerService ownerService;
    @Autowired
    private DojoService dojoService;
    @Autowired
    private RankService rankService;

    private Logger logger = LoggerFactory.getLogger(DojoController.class);

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
        dojoService.addDojoToOwner(dojo, owner);
        RankName rank = new RankName();
        rank.setName("kyu");
        rank = rankService.saveRankName(rank);

        RankLevel rankLevel = new RankLevel(0, 10, 40, "White", "None", 0);
        rankLevel = rankService.saveRankLevel(rankLevel);

        Dojo dojo1 = new Dojo();
        dojo1.setName("KobraKai");
        dojo1.setWebsite("www.kobra-kai.com");
        dojoService.addDojoToOwner(dojo1, owner);

        try {
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
}
