package com.ist.common.security;

import com.ist.user_management.model.User;
import com.ist.user_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        return UserDetailsImpl.build(user);
    }

    public UserDetails loadUserByGoogleId(String googleId) throws UsernameNotFoundException {
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with Google ID: " + googleId));

        return UserDetailsImpl.build(user);
    }
}