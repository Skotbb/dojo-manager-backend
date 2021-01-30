package com.dojomanager.dojomanager.DataTests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Address;
import com.dojomanager.dojomanager.AbstractTest;
import com.dojomanager.services.AddressService;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class Dojo_Test extends AbstractTest {
    @Autowired
    private DojoService dojoService;
    @Autowired
    private DojoOwnerService ownerService;
    @Autowired
    private AddressService addressService;

    private DojoOwner testOwner;

    @BeforeEach
    public void setup() {
        testOwner = new DojoOwner("test", "owner", "email@place.com", "password");
        ownerService.saveDojoOwner(testOwner);
    }

    @Test
    public void dojo_addAddress() {
        Address address = new Address("21 Partidge St.", "Placeville", "FL", "32738");
        addressService.saveAddress(address);

        Dojo testDojo= new Dojo("Kick dicks", "www.stopkickingthere.com", address);
        dojoService.saveDojo(testDojo);

        assertNotNull(testDojo.getAddress().getId());
    }
}
