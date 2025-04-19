package com.ist.user_management.service;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.utils.JwtUtils;
import com.ist.user_management.dto.GoogleLoginRequestDto;
import com.ist.user_management.dto.JwtResponseDto;
import com.ist.user_management.dto.MessageResponseDto;
import com.ist.user_management.model.Role;
import com.ist.user_management.model.User;
import com.ist.user_management.repository.RoleRepository;
import com.ist.user_management.repository.UserRepository;
import com.ist.common.enums.ERole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class handling authentication operations including Google OAuth2
 * authentication
 * and user management.
 */
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Authenticates a user using Google OAuth2 credentials.
     * Creates a new user if one doesn't exist with the provided email.
     *
     * @param loginRequest DTO containing Google authentication details
     * @return JwtResponseDto containing JWT token and user details
     * @throws RuntimeException if the Google token is invalid
     */
    public JwtResponseDto authenticateGoogleUser(GoogleLoginRequestDto loginRequest) {
        if (!isValidGoogleToken(loginRequest.getGoogleToken())) {
            throw new RuntimeException("Invalid Google token");
        }

        // Find or create user
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseGet(() -> createNewUser(loginRequest));

        // Build user details and collect roles
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Create and set authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new JwtResponseDto(
                jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                user.getProfilePicture(),
                roles);
    }

    /**
     * Creates a new user with default STAFF role.
     *
     * @param loginRequest DTO containing user details
     * @return newly created User entity
     */
    private User createNewUser(GoogleLoginRequestDto loginRequest) {
        User newUser = new User();
        newUser.setEmail(loginRequest.getEmail());
        newUser.setFirstName(loginRequest.getFirstName());
        newUser.setLastName(loginRequest.getLastName());
        newUser.setProfilePicture(loginRequest.getProfilePicture());
        newUser.setGoogleId(loginRequest.getGoogleToken());

        // Set default STAFF role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_STAFF)
                .orElseThrow(() -> new RuntimeException("Error: Role STAFF is not found."));
        roles.add(userRole);
        newUser.setRoles(roles);

        return userRepository.save(newUser);
    }

    /**
     * Basic validation of Google token.
     * TODO: Implement proper Google token validation
     *
     * @param token Google OAuth2 token
     * @return true if token is not null or empty
     */
    private boolean isValidGoogleToken(String token) {
        return token != null && !token.isEmpty();
    }

    /**
     * Logs out the current user by clearing the security context.
     *
     * @return MessageResponseDto indicating successful logout
     */
    public MessageResponseDto logoutUser() {
        SecurityContextHolder.clearContext();
        return new MessageResponseDto("User logged out successfully!");
    }
}