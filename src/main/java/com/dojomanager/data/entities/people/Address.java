package com.dojomanager.data.entities.people;

import javax.persistence.Entity;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class Address extends AbstractEntity {
    
    @Getter @Setter
    private String addressLine1;
    
    @Getter @Setter
    private String addressLine2;

    @Getter @Setter
    private String city;

    @Getter @Setter
    private String state;

    @Getter @Setter
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
