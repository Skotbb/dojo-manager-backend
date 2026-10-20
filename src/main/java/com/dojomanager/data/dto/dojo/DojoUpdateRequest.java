package com.dojomanager.data.dto.dojo;

/**
 * Body for creating or updating a dojo.
 *
 * <p>
 * The legacy {@code contact} field was removed: the {@code Dojo} entity has no
 * contact field, so it was accepted and silently discarded.
 * </p>
 */
public class DojoUpdateRequest {
    private String name;
    private String website;
    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
