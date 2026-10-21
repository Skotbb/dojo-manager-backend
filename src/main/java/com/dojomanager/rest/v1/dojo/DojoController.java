package com.dojomanager.rest.v1.dojo;

import java.util.List;

import com.dojomanager.data.dto.dojo.CreateDojoRequest;
import com.dojomanager.data.dto.dojo.DojoDto;
import com.dojomanager.data.dto.dojo.DojoResponse;
import com.dojomanager.data.dto.dojo.DojoUpdateRequest;
import com.dojomanager.data.dto.dojo.RankDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.DojoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
                .map(dojo -> DojoDto.from(dojo, dojoService.buildRankDtos(dojo)))
                .toList();

        return new DojoResponse(dojos);
    }

    @PostMapping
    public ResponseEntity<DojoDto> createDojo(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody CreateDojoRequest request) {

        Dojo created = dojoService.createDojo(principal, request);
        List<RankDto> ranks = dojoService.getRanksForDojo(principal, created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(DojoDto.from(created, ranks));
    }

    @PutMapping("/{id}")
    public DojoDto updateDojo(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody DojoUpdateRequest request) {

        Dojo updated = dojoService.updateDojo(principal, id, request);
        List<RankDto> ranks = dojoService.getRanksForDojo(principal, id);

        return DojoDto.from(updated, ranks);
    }

    @GetMapping("/{id}/ranks")
    public List<RankDto> getDojoRanks(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {

        return dojoService.getRanksForDojo(principal, id);
    }

    @PostMapping("/{id}/ranks")
    public ResponseEntity<RankDto> addDojoRank(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateDojoRequest.RankRequest request) {

        RankDto created = dojoService.addRankToDojo(principal, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/ranks")
    public List<RankDto> updateDojoRanks(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody List<CreateDojoRequest.RankRequest> ranks) {

        return dojoService.addRanksToDojo(principal, id, ranks);
    }

    @PutMapping("/{id}/ranks/{rankId}")
    public ResponseEntity<RankDto> updateSingleRank(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @PathVariable("rankId") Long rankId,
            @Valid @RequestBody CreateDojoRequest.RankRequest request) {

        return ResponseEntity.ok(dojoService.updateRank(principal, id, rankId, request));
    }

    @DeleteMapping("/{id}/ranks/{rankId}")
    public ResponseEntity<Void> deleteRank(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @PathVariable("rankId") Long rankId) {

        dojoService.deleteRank(principal, id, rankId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/ranks/{rankId}/levels/{rankLevelId}")
    public ResponseEntity<Void> deleteRankLevel(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @PathVariable("rankId") Long rankId,
            @PathVariable("rankLevelId") Long rankLevelId) {

        dojoService.deleteRankLevel(principal, id, rankId, rankLevelId);
        return ResponseEntity.noContent().build();
    }
}
