package com.dojomanager.data.dto.authorization;

import lombok.Data;

@Data
public class JwtResponse {
    private String token, email;
    private String type = "Bearer";

    public JwtResponse(String accessToken, String email){
        this.token = accessToken;
        this.email = email;
    }
}
