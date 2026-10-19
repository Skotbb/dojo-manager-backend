package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.dto.dojo.DojoResponse;
import com.dojomanager.data.dto.dojo.DojoUpdateRequest;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Address;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;
import com.dojomanager.services.RankService;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private String currentUserEmail() {
        UserDetails details = (UserDetails) SecurityContextHolder.getContext().getAuthentication();
        return details.getUsername();
    }

    @GetMapping(value = "/api/v1/dojo")
    public @ResponseBody DojoResponse getMyDojo() {
        String email = currentUserEmail();
        Optional<DojoOwner> ownerOpt = ownerService.getOwnerByEmail(email);
        if (ownerOpt.isEmpty()) {
            return new DojoResponse(null);
        }
        DojoOwner owner = ownerOpt.get();
        return new DojoResponse(List.copyOf(owner.getManagedDojos()));
    }

    @PutMapping("/api/v1/dojo")
    public @ResponseBody DojoResponse updateDojo(@RequestBody DojoUpdateRequest request) {
        String email = currentUserEmail();
        Optional<DojoOwner> ownerOpt = ownerService.getOwnerByEmail(email);
        if (ownerOpt.isEmpty()) {
            return new DojoResponse(null);
        }
        DojoOwner owner = ownerOpt.get();
        Dojo dojo = new Dojo();
        if (request.getName() != null) {
            dojo.setName(request.getName());
        }
        if (request.getWebsite() != null) {
            dojo.setWebsite(request.getWebsite());
        }
        if (request.getAddress() != null) {
            dojo.setAddress(new Address(request.getAddress(), null, null, null, null));
        }
        dojoService.saveDojo(dojo);
        return new DojoResponse(List.of(dojo));
    }

    private void loggerError(String msg) {
        logger.error(msg);
    }
}
