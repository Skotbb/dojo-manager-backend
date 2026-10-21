package com.dojomanager.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.dojomanager.data.dto.dojo.CreateDojoRequest;
import com.dojomanager.data.dto.dojo.DojoUpdateRequest;
import com.dojomanager.data.dto.dojo.RankDto;
import com.dojomanager.data.dto.dojo.RankLevelDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Address;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.repositories.dojo.*;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.exceptions.BadRequestException;
import com.dojomanager.services.exceptions.ForbiddenOperationException;
import com.dojomanager.services.exceptions.ResourceNotFoundException;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DojoService {
    @Autowired
    private DojoRepository dojoRepo;
    
    @Autowired
    private DojoOwnerService ownerService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private RankService rankService;
    
    private Logger logger = LoggerFactory.getLogger(DojoService.class);

    /**
     * Returns only the dojos managed by the authenticated principal.
     */
    @Transactional(readOnly = true)
    public List<Dojo> listDojos(OwnerPrincipal principal) {
        DojoOwner owner = ownerService.requireOwner(principal);
        return dojoRepo.findByOwner(owner);
    }

    /**
     * Creates a new dojo owned by the authenticated principal and persists its
     * initial rank graph (rank name → level → belt color → rank setting).
     */
    @Transactional
    public Dojo createDojo(OwnerPrincipal principal, CreateDojoRequest request) {
        DojoOwner owner = ownerService.requireOwner(principal);

        Dojo dojo = new Dojo();
        applyFields(dojo, request.getName(), request.getWebsite(), request.getAddress());
        dojo.setOwner(owner);

        Dojo savedDojo = dojoRepo.saveAndFlush(dojo);
        persistRanks(savedDojo, request.getRanks());

        return savedDojo;
    }

    /**
     * Updates one of the authenticated principal's own dojos. A dojo that
     * belongs to another owner is reported as not found so the API never
     * reveals that it exists.
     */
    @Transactional
    public Dojo updateDojo(OwnerPrincipal principal, Long dojoId, DojoUpdateRequest request) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);

        applyRequest(dojo, request);

        if (request.getRanks() != null && !request.getRanks().isEmpty()) {
            persistRanks(dojo, request.getRanks());
        }

        return dojoRepo.saveAndFlush(dojo);
    }

    public Dojo requireOwnedDojo(OwnerPrincipal principal, Long dojoId) {
        DojoOwner owner = ownerService.requireOwner(principal);

        return dojoRepo.findByOwner(owner).stream()
                .filter(candidate -> dojoId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Dojo " + dojoId + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<RankDto> getRanksForDojo(OwnerPrincipal principal, Long dojoId) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        return buildRankDtos(dojo);
    }

    @Transactional
    public RankDto addRankToDojo(OwnerPrincipal principal, Long dojoId, CreateDojoRequest.RankRequest rankRequest) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        return persistRank(dojo, rankRequest);
    }

    @Transactional
    public List<RankDto> addRanksToDojo(OwnerPrincipal principal, Long dojoId, List<CreateDojoRequest.RankRequest> ranks) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        persistRanks(dojo, ranks);
        return buildRankDtos(dojo);
    }

    @Transactional
    public RankDto updateRank(OwnerPrincipal principal, Long dojoId, Long rankId, CreateDojoRequest.RankRequest request) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        RankName rankName = rankService.findRankNameById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank " + rankId + " was not found"));

        if (!dojo.getId().equals(rankName.getDojo().getId())) {
            throw new ForbiddenOperationException("Rank does not belong to the owned dojo");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            rankName.setName(request.getName());
            rankService.saveRankName(rankName);
        }

        // findSettingsByRankName has no ORDER BY; sort both sides by sortOrder so
        // index-based mapping lines up with the ladder order the UI sends.
        List<RankSetting> existingSettings = rankService.findSettingsByRankName(rankName).stream()
                .sorted(Comparator.comparingInt(rs -> rs.getRankLevel() != null ? rs.getRankLevel().getSortOrder() : 0))
                .toList();
        List<CreateDojoRequest.RankLevelRequest> levelRequests = request.getLevels() != null
                ? request.getLevels().stream()
                        .sorted(Comparator.comparingInt(CreateDojoRequest.RankLevelRequest::getSortOrder))
                        .toList()
                : List.of();

        for (int i = 0; i < levelRequests.size(); i++) {
            CreateDojoRequest.RankLevelRequest req = levelRequests.get(i);
            if (i < existingSettings.size()) {
                RankSetting setting = existingSettings.get(i);
                RankLevel level = setting.getRankLevel();
                if (level != null) {
                    level.setSortOrder(req.getSortOrder());
                    level.setNumber(req.getNumber());
                    level.setHoursRequired(req.getHoursRequired());
                    rankService.saveRankLevel(level);
                }

                BeltColor belt = setting.getBeltColor();
                if (belt != null) {
                    belt.setColor(req.getBeltColor());
                    belt.setStripeColor(req.getStripeColor());
                    belt.setStripeCount(req.getStripeCount());
                    rankService.saveBeltColor(belt);
                }
            } else {
                RankLevel rankLevel = rankService.saveRankLevel(new RankLevel(
                        req.getSortOrder(), req.getNumber(), req.getHoursRequired(), rankName));

                BeltColor beltColor = rankService.saveBeltColor(new BeltColor(
                        req.getBeltColor(), req.getStripeColor(),
                        req.getStripeCount() != null ? req.getStripeCount() : 0, rankLevel));

                rankService.saveRankSetting(new RankSetting(dojo, rankName, rankLevel, beltColor));
            }
        }

        for (int i = levelRequests.size(); i < existingSettings.size(); i++) {
            RankSetting extra = existingSettings.get(i);
            if (!rankService.isRankSettingAssigned(extra)) {
                BeltColor belt = extra.getBeltColor();
                RankLevel level = extra.getRankLevel();
                rankService.deleteRankSetting(extra);
                if (belt != null) rankService.deleteBeltColor(belt);
                if (level != null) rankService.deleteRankLevel(level);
            }
        }

        return buildRankDtos(dojo).stream()
                .filter(r -> rankId.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void deleteRank(OwnerPrincipal principal, Long dojoId, Long rankId) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        RankName rankName = rankService.findRankNameById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank " + rankId + " was not found"));

        if (!dojo.getId().equals(rankName.getDojo().getId())) {
            throw new ForbiddenOperationException("Rank does not belong to the owned dojo");
        }

        List<RankSetting> settings = rankService.findSettingsByRankName(rankName);
        for (RankSetting setting : settings) {
            if (rankService.isRankSettingAssigned(setting)) {
                throw new BadRequestException("Cannot delete rank because students are currently assigned to it");
            }
        }

        for (RankSetting setting : settings) {
            BeltColor belt = setting.getBeltColor();
            RankLevel level = setting.getRankLevel();
            rankService.deleteRankSetting(setting);
            if (belt != null) rankService.deleteBeltColor(belt);
            if (level != null) rankService.deleteRankLevel(level);
        }

        rankService.deleteRankName(rankName);
    }

    /**
     * Deletes a single level inside a rank tier. Students assigned to OTHER
     * levels of the same tier do not block the delete — only an assignment to
     * this exact level does. If the tier ends up with zero levels the tier
     * (RankName) itself is removed as well.
     */
    @Transactional
    public void deleteRankLevel(OwnerPrincipal principal, Long dojoId, Long rankId, Long rankLevelId) {
        Dojo dojo = requireOwnedDojo(principal, dojoId);
        RankName rankName = rankService.findRankNameById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank " + rankId + " was not found"));

        if (!dojo.getId().equals(rankName.getDojo().getId())) {
            throw new ForbiddenOperationException("Rank does not belong to the owned dojo");
        }

        RankSetting target = rankService.findSettingsByRankName(rankName).stream()
                .filter(rs -> rs.getRankLevel() != null && rankLevelId.equals(rs.getRankLevel().getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Rank level " + rankLevelId + " was not found"));

        if (rankService.isRankSettingAssigned(target)) {
            throw new BadRequestException("Cannot delete rank level because students are currently assigned to it");
        }

        BeltColor belt = target.getBeltColor();
        RankLevel level = target.getRankLevel();
        rankService.deleteRankSetting(target);
        if (belt != null) rankService.deleteBeltColor(belt);
        if (level != null) rankService.deleteRankLevel(level);

        // Remove the tier itself once its last level is gone.
        if (rankService.findSettingsByRankName(rankName).isEmpty()) {
            rankService.deleteRankName(rankName);
        }
    }

    public List<RankDto> buildRankDtos(Dojo dojo) {
        List<RankSetting> settings = rankService.getRankSettingsByDojo(dojo);

        Map<RankName, List<RankSetting>> grouped = settings.stream()
                .filter(rs -> rs.getRankName() != null)
                .collect(Collectors.groupingBy(
                        RankSetting::getRankName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RankDto> rankDtos = new ArrayList<>();
        for (Map.Entry<RankName, List<RankSetting>> entry : grouped.entrySet()) {
            RankName rankName = entry.getKey();
            List<RankSetting> rankSettings = entry.getValue();

            List<RankLevelDto> levelDtos = rankSettings.stream()
                    .sorted(Comparator.comparingInt(rs -> rs.getRankLevel() != null ? rs.getRankLevel().getSortOrder() : 0))
                    .map(rs -> RankLevelDto.builder()
                            .id(rs.getRankLevel() != null ? rs.getRankLevel().getId() : null)
                            .rankSettingId(rs.getId())
                            .sortOrder(rs.getRankLevel() != null ? rs.getRankLevel().getSortOrder() : 0)
                            .number(rs.getRankLevel() != null ? rs.getRankLevel().getNumber() : 0)
                            .hoursRequired(rs.getRankLevel() != null ? rs.getRankLevel().getHoursRequired() : 0)
                            .beltColor(rs.getBeltColor() != null ? rs.getBeltColor().getColor() : null)
                            .stripeColor(rs.getBeltColor() != null ? rs.getBeltColor().getStripeColor() : null)
                            .stripeCount(rs.getBeltColor() != null ? rs.getBeltColor().getStripeCount() : 0)
                            .build())
                    .toList();

            rankDtos.add(RankDto.builder()
                    .id(rankName.getId())
                    .name(rankName.getName())
                    .levels(levelDtos)
                    .build());
        }
        return rankDtos;
    }

    private void applyRequest(Dojo dojo, DojoUpdateRequest request) {
        applyFields(dojo, request.getName(), request.getWebsite(), request.getAddress());
    }

    private void applyFields(Dojo dojo, String name, String website, String rawAddress) {
        if (name != null) {
            dojo.setName(name);
        }
        if (website != null) {
            dojo.setWebsite(website);
        }
        if (rawAddress != null) {
            Address address = new Address(rawAddress, null, null, null, null);
            dojo.setAddress(addressService.saveAddress(address));
        }
    }

    /**
     * Persists one {@link RankSetting} per level. A rank name is saved once,
     * then each level fans out into a level, a belt color and the student-facing
     * join row.
     */
    private void persistRanks(Dojo dojo, List<CreateDojoRequest.RankRequest> ranks) {
        if (ranks == null || ranks.isEmpty()) {
            return;
        }

        for (CreateDojoRequest.RankRequest rank : ranks) {
            persistRank(dojo, rank);
        }
    }

    private RankDto persistRank(Dojo dojo, CreateDojoRequest.RankRequest rank) {
        RankName rankName = rankService.saveRankName(new RankName(rank.getName(), dojo));
        List<RankLevelDto> levelDtos = new ArrayList<>();

        if (rank.getLevels() != null) {
            for (CreateDojoRequest.RankLevelRequest level : rank.getLevels()) {
                RankLevel rankLevel = rankService.saveRankLevel(new RankLevel(
                        level.getSortOrder(), level.getNumber(), level.getHoursRequired(), rankName));

                BeltColor beltColor = rankService.saveBeltColor(new BeltColor(
                        level.getBeltColor(), level.getStripeColor(),
                        level.getStripeCount() != null ? level.getStripeCount() : 0, rankLevel));

                RankSetting setting = rankService.saveRankSetting(new RankSetting(dojo, rankName, rankLevel, beltColor));

                levelDtos.add(RankLevelDto.builder()
                        .id(rankLevel.getId())
                        .rankSettingId(setting.getId())
                        .sortOrder(rankLevel.getSortOrder())
                        .number(rankLevel.getNumber())
                        .hoursRequired(rankLevel.getHoursRequired())
                        .beltColor(beltColor.getColor())
                        .stripeColor(beltColor.getStripeColor())
                        .stripeCount(beltColor.getStripeCount())
                        .build());
            }
        }

        return RankDto.builder()
                .id(rankName.getId())
                .name(rankName.getName())
                .levels(levelDtos)
                .build();
    }

    public Dojo saveDojo(Dojo dojo) {
        return dojoRepo.saveAndFlush(dojo);
    }

    public Set<Dojo> saveAll(Set<Dojo> dojos) {
        return Sets.newHashSet(dojoRepo.saveAll(dojos));
    }

    public DojoOwner addDojoToOwner(Dojo dojo, DojoOwner owner) {
        if(owner.getId() == null) {
            ownerService.saveDojoOwner(owner);
        }
        owner.addDojo(dojo);
        
        dojoRepo.saveAndFlush(dojo);
        return ownerService.saveDojoOwner(owner);
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
            logger.warn("Could not find Dojo with id=%s", id);
            return new Dojo();
        }         
    }

    public List<Dojo> getDojosForOwner(DojoOwner owner) {
        return dojoRepo.findByOwner(owner);
    }
}
