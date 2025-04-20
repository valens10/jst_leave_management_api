package com.ist.common.security;

import com.ist.common.utils.JwtUtils;
import com.ist.user_management.model.User;
import com.ist.user_management.model.Role;
import com.ist.user_management.repository.UserRepository;
import com.ist.user_management.repository.RoleRepository;
import com.ist.common.enums.ERole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/authorization") ||
                path.startsWith("/login/oauth2/code") ||
                path.startsWith("/api/auth/oauth2/callback") ||
                path.startsWith("/api/leaves/attachments/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = jwtUtils.parseJwt(request);
            logger.debug("JWT token found in request: {}", jwt != null);

            if (jwt != null) {
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    Claims claims = jwtUtils.getClaimsFromJwtToken(jwt);
                    logger.debug("Successfully validated JWT token for user: {}", username);

                    // Try to find user, if not found create new user
                    userRepository.findByEmail(username)
                            .orElseGet(() -> {
                                logger.info("User not found, creating new user: {}", username);
                                User newUser = new User();
                                newUser.setEmail(username);
                                newUser.setFirstName(claims.get("firstName", String.class));
                                newUser.setLastName(claims.get("lastName", String.class));
                                newUser.setGoogleId(claims.get("googleId", String.class));
                                newUser.setProfilePicture(claims.get("profilePicture", String.class));

                                // Set default STAFF role
                                Role userRole = roleRepository.findByName(ERole.ROLE_STAFF)
                                        .orElseThrow(() -> new RuntimeException("Error: Role STAFF is not found."));
                                newUser.setRoles(Collections.singleton(userRole));

                                return userRepository.save(newUser);
                            });

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Successfully authenticated user: {}", username);
                } else {
                    logger.warn("Invalid JWT token");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid token\"}");
                    return;
                }
            } else {
                logger.warn("No JWT token found in request");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"No token provided\"}");
                return;
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}