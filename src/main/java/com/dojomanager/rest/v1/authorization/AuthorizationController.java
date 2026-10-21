package com.dojomanager.rest.v1.authorization;

import java.util.Optional;

import jakarta.validation.Valid;

import com.dojomanager.data.dto.MessageResponse;
import com.dojomanager.data.dto.authorization.ForgotPasswordResponse;
import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.forms.ForgotPasswordForm;
import com.dojomanager.data.forms.LoginForm;
import com.dojomanager.data.forms.ResetPasswordForm;
import com.dojomanager.data.forms.SignupForm;
import com.dojomanager.security.jwt.JwtProvider;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.PasswordResetService;
import com.dojomanager.services.PasswordResetService.ResetLink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthorizationController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    DojoOwnerService ownerService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    PasswordResetService passwordResetService;

    /**
     * Always answers with the same 200 message, whether or not the email exists,
     * so the endpoint cannot be used to enumerate accounts. Development builds
     * additionally return {@code resetUrl}/{@code token} (see
     * {@link ForgotPasswordResponse}).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordForm form) {
        Optional<ResetLink> resetLink = passwordResetService.requestReset(form.getEmail());

        String message = "If an account exists for that email, a password reset link has been sent";
        String resetUrl = resetLink.map(ResetLink::resetUrl).orElse(null);
        String token = resetLink.map(ResetLink::token).orElse(null);

        return ResponseEntity.ok(new ForgotPasswordResponse(message, resetUrl, token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordForm form) {
        passwordResetService.resetPassword(form.getToken(), form.getPassword());
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginForm loginRequest) {
        try {
            return ResponseEntity.ok(authenticateUser(loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            MessageResponse messageResponse = new MessageResponse("Invalid email or password");
            messageResponse.setError("Invalid email or password");
            return new ResponseEntity<>(messageResponse, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            MessageResponse messageResponse = new MessageResponse("An error occurred during authentication");
            messageResponse.setError("Bad Request");
            return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupForm signupRequest) {
        if (ownerService.doesEmailExist(signupRequest.getEmail())) {
            return new ResponseEntity<>(new MessageResponse("Error: Email already exists"), HttpStatus.BAD_REQUEST);
        }

        DojoOwner owner = new DojoOwner(signupRequest.getFirstName(), signupRequest.getLastName(),
                signupRequest.getEmail(), encoder.encode(signupRequest.getPassword()));

        ownerService.saveDojoOwner(owner);

        JwtResponse authResponse = authenticateUser(signupRequest.getEmail(), signupRequest.getPassword());
        return ResponseEntity.ok(authResponse);
    }

    private JwtResponse authenticateUser(final String email, final String password) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(email, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateJwtToken(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return new JwtResponse(jwt, userDetails.getUsername());
    }
}
