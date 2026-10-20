package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.dto.dojo.OwnerResponse;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.DojoOwnerService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Profile of the authenticated dojo owner.
 *
 * <p>
 * The owner is derived from the JWT principal; there is no path variable that
 * would let a caller read another owner's dojos.
 * </p>
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/api/v1/owner")
@RequiredArgsConstructor
public class DojoOwnerController {

    private final DojoOwnerService ownerService;

    @GetMapping
    public OwnerResponse getMyOwner(@AuthenticationPrincipal OwnerPrincipal principal) {
        return OwnerResponse.from(ownerService.getOwnerProfile(principal));
    }
}
