package com.dojomanager.data.dto.dojo;

import java.util.List;

public class DojoResponse {
    private List<com.dojomanager.data.entities.dojo.Dojo> dojos;

    public DojoResponse() {}

    public DojoResponse(List<com.dojomanager.data.entities.dojo.Dojo> dojos) {
        this.dojos = dojos;
    }

    public List<com.dojomanager.data.entities.dojo.Dojo> getDojos() {
        return dojos;
    }

    public void setDojos(List<com.dojomanager.data.entities.dojo.Dojo> dojos) {
        this.dojos = dojos;
    }
}
