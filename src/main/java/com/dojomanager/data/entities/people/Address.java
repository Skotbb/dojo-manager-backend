package com.dojomanager.data.entities.people;

import javax.persistence.Entity;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Address extends AbstractEntity {
    
    private String addressLine1;
    
    private String addressLine2;

    private String city;

    private String state;

    private String zipCode;


    public Address(String addressLine1, String addressLine2, String city, String state, String zipCode) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    public Address(String addressLine1, String city, String state, String zipCode) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

}
