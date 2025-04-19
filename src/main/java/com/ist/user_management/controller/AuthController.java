package com.ist.user_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.utils.JwtUtils;
import com.ist.common.enums.ERole;
import com.ist.user_management.model.Role;
import com.ist.user_management.model.User;
import com.ist.user_management.dto.JwtResponseDto;
import com.ist.user_management.dto.MessageResponseDto;
import com.ist.user_management.repository.RoleRepository;
import com.ist.user_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<?> handleGoogleCallback(Authentication authentication) {
        try {
            if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("Invalid authentication"));
            }

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            logger.info("OAuth2 token attributes: {}", attributes);

            String email = getAttributeAsString(attributes, "email");
            if (email == null) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("Email not found in OAuth2 token"));
            }

            logger.info("Processing login request for user: {}", email);
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setFirstName(getAttributeAsString(attributes, "given_name"));
                        newUser.setLastName(getAttributeAsString(attributes, "family_name"));
                        newUser.setGoogleId(getAttributeAsString(attributes, "sub"));
                        newUser.setProfilePicture(getAttributeAsString(attributes, "picture"));

                        Role defaultRole = roleRepository.findByName(ERole.ROLE_STAFF)
                                .orElseThrow(() -> new RuntimeException("Default role not found"));
                        newUser.setRoles(Collections.singleton(defaultRole));

                        return userRepository.save(newUser);
                    });

            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            String jwt = jwtUtils.generateJwtToken(authToken);

            return ResponseEntity.ok(new JwtResponseDto(jwt));
        } catch (Exception e) {
            logger.error("Error processing Google OAuth2 callback", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDto("Error processing Google login: " + e.getMessage()));
        }
    }

    private String getAttributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}