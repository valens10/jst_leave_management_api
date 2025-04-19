package com.ist.common.security;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final String EMAIL_ATTRIBUTE = "email";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        Map<String, Object> attributes = user.getAttributes();

        // Ensure required attributes are present
        if (!attributes.containsKey(EMAIL_ATTRIBUTE)) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 user attributes");
        }

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                EMAIL_ATTRIBUTE);
    }
}