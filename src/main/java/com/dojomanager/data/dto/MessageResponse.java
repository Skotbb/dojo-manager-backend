package com.dojomanager.data.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageResponse {
    private String message;
    private String error;

    public MessageResponse(String message) {
        this.message = message;
    }
}
