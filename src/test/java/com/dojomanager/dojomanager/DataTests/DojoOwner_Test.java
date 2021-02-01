package com.dojomanager.dojomanager.DataTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.dojomanager.AbstractTest;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DojoOwner_Test extends AbstractTest{
    
    @Autowired
    DojoOwnerService ownerService;
    @Autowired
    DojoService dojoService;

    static DojoOwner TEST_OWNER;
    static Dojo TEST_DOJO1, TEST_DOJO2;

    @BeforeAll
    public static void setup(){
        TEST_OWNER = new DojoOwner("Test", "Lastname", "test@email.com", "weakPassword");
        TEST_OWNER = new DojoOwner("Test", "Lastname", "test@email.com", "weakPassword");

        TEST_DOJO1 = new Dojo("Kobra-Kai", "www.sweep-the-leg.com");
        TEST_DOJO2 = new Dojo("Test Dojo", "www.goodwebsite.com");
    }

    @Test
    public void saveDojoOwner_idExists() {
        DojoOwner savedOwner = ownerService.saveDojoOwner(TEST_OWNER);

        assertNotNull(savedOwner.getId(), "DojoOwner save and received an ID.");
    }

    @Test
    public void saveDojoOwner_addDojos() {
        /*DojoOwner savedOwner = */ownerService.saveDojoOwner(TEST_OWNER);

        dojoService.addDojoToOwner(TEST_DOJO1, TEST_OWNER);
        dojoService.addDojoToOwner(TEST_DOJO2, TEST_OWNER);

        List<Dojo> dojos = dojoService.getDojosForOwner(TEST_OWNER);

        assertNotNull(dojos);
        assertEquals(2, dojos.size(), "Dojo size should be 2!");
    }

    @Test
    public void saveDojoOwner_noDupEmail() {
        DojoOwner firstOwner = ownerService.saveDojoOwner(TEST_OWNER);

        DojoOwner dupEmailOwner = new DojoOwner("duplicate", "email", "test@email.com", "dontlook");
        dupEmailOwner = ownerService.saveDojoOwner(dupEmailOwner);

        assertNotEquals(dupEmailOwner.getFirstName(), firstOwner.getFirstName());
    }
}
