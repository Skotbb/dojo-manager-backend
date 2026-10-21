package com.dojomanager.data.dto.authorization;

import com.dojomanager.data.dto.MessageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for {@code POST /api/auth/forgot-password}.
 *
 * <p>In development the logging email transport also returns the generated
 * reset link and raw token ({@code resetUrl}, {@code token}) so the UI can show
 * a clickable link. Real transports - and the prod profile - leave both null,
 * and {@link JsonInclude.Include#NON_NULL} keeps them out of the JSON entirely,
 * so the production shape is identical whether or not the account exists.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgotPasswordResponse extends MessageResponse {

    private String resetUrl;
    private String token;

    public ForgotPasswordResponse(String message, String resetUrl, String token) {
        super(message);
        this.resetUrl = resetUrl;
        this.token = token;
    }
}
