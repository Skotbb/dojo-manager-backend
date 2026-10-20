package com.dojomanager.rest.v1.dojo;

import java.util.List;

import com.dojomanager.data.dto.dojo.DojoDto;
import com.dojomanager.data.dto.dojo.DojoResponse;
import com.dojomanager.data.dto.dojo.DojoUpdateRequest;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.DojoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Dojo management for the authenticated owner.
 *
 * <p>
 * The owner is always taken from the JWT principal; no request path or body can
 * select another owner. Responses are DTOs rather than JPA entities.
 * </p>
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/api/v1/dojos")
@RequiredArgsConstructor
public class DojoController {

    private final DojoService dojoService;

    @GetMapping
    public DojoResponse getMyDojos(@AuthenticationPrincipal OwnerPrincipal principal) {
        List<DojoDto> dojos = dojoService.listDojos(principal).stream()
                .map(DojoDto::from)
                .toList();

        return new DojoResponse(dojos);
    }

    @PostMapping
    public ResponseEntity<DojoDto> createDojo(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestBody DojoUpdateRequest request) {

        Dojo created = dojoService.createDojo(principal, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(DojoDto.from(created));
    }

    @PutMapping("/{id}")
    public DojoDto updateDojo(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody DojoUpdateRequest request) {

        return DojoDto.from(dojoService.updateDojo(principal, id, request));
    }
}
