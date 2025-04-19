package com.ist.common.security;

import com.ist.user_management.model.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class GoogleAuthenticationToken extends AbstractAuthenticationToken {
    private final User user;

    public GoogleAuthenticationToken(User user) {
        super(user.getRoles().stream()
                .map(role -> (GrantedAuthority) () -> role.getName().name())
                .toList());
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }
}