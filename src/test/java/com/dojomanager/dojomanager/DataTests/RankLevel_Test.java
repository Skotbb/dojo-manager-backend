package com.dojomanager.dojomanager.DataTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.dojomanager.AbstractTest;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;
import com.dojomanager.services.RankService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RankLevel_Test extends AbstractTest{
    @Autowired
    private RankService rankService;
    
    @Autowired
    private DojoService dojoService;

    @Autowired
    private DojoOwnerService ownerService;

    private Dojo TEST_DOJO;
    private DojoOwner TEST_OWNER;
    private RankName TEST_NAME;
    private RankLevel TEST_LEVEL;

    // @BeforeEach
    // public void setup() {
    //     saveTestItems();
    // }

    private void saveTestItems() {
        ownerService.getOwnerByEmail("email").ifPresentOrElse((owner) -> {
            // Owner exists. Hurray
        }, () -> {
            TEST_OWNER = new DojoOwner("firstName", "lastName", "email", "password");
            TEST_DOJO = new Dojo("test dojo", "www.test.com");
            dojoService.addDojoToOwner(TEST_DOJO, TEST_OWNER);
        });

        // if(TEST_OWNER == null || !TEST_OWNER.getEmail().equals("email")) {
            
        // }

        TEST_NAME = new RankName("Kyu");
        rankService.saveRankName(TEST_NAME);

        TEST_LEVEL = new RankLevel(0, 10, 150, "White", "", 0);
        rankService.saveRankLevel(TEST_LEVEL);

    }

    // @Test
    // public void rank_addToDojo() {
    //     assertEquals(TEST_DOJO, TEST_LEVEL.getRankName().getDojo(), "The RankLevel is attached to the correct Dojo.");
    //     assertEquals(TEST_NAME, TEST_COLOR.getRankLevel().getRankName(), "The BeltColor is attached to the correct RankName.");
    // }

    @Test
    public void rank_addToSetting() {
        RankSetting setting = new RankSetting(TEST_DOJO, TEST_NAME, TEST_LEVEL);
        rankService.saveRankSetting(setting);

        assertNotNull(setting.getId(), "Setting saved and has an ID");
    }

    @Test
    public void rank_getSettingsForDojo() {
        saveTestItems();

        RankSetting setting1 = new RankSetting(TEST_DOJO, TEST_NAME, TEST_LEVEL);

        RankLevel ninthKyu = new RankLevel(1, 9, 150, "Blue", "", 0);
        rankService.saveRankLevel(ninthKyu);

        RankSetting setting2 = new RankSetting(TEST_DOJO, TEST_NAME, ninthKyu);

        rankService.saveRankSetting(setting1);
        rankService.saveRankSetting(setting2);

        assertEquals(2, rankService.getRankSettingsByDojo(TEST_DOJO).size(), "The Dojo has two Rank Settings.");
    }    
}