package com.dojomanager.data.dto.dojo;

import java.util.List;

/**
 * Collection wrapper for the authenticated owner's dojos.
 */
public class DojoResponse {
    private List<DojoDto> dojos;

    public DojoResponse() {}

    public DojoResponse(List<DojoDto> dojos) {
        this.dojos = dojos;
    }

    public List<DojoDto> getDojos() {
        return dojos;
    }

    public void setDojos(List<DojoDto> dojos) {
        this.dojos = dojos;
    }
}
