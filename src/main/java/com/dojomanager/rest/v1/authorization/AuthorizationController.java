package com.dojomanager.rest.v1.authorization;

import javax.validation.Valid;

import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.forms.LoginForm;
import com.dojomanager.data.forms.SignupForm;
import com.dojomanager.security.jwt.JwtProvider;
import com.dojomanager.services.DojoOwnerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginForm loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateJwtToken(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername()));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupForm signupRequest) {
        if(ownerService.doesEmailExist(signupRequest.getEmail())) {
            return new ResponseEntity<>("Error: Email already exists", HttpStatus.BAD_REQUEST);
        }

        DojoOwner owner = new DojoOwner(signupRequest.getFirstName(), 
        signupRequest.getLastName(), signupRequest.getEmail(), encoder.encode(signupRequest.getPassword()));

        ownerService.saveDojoOwner(owner);

        return new ResponseEntity<>("User registered successfully!", HttpStatus.OK);
    }
}
