package com.campusrecycle.security;

import com.campusrecycle.model.User;
import com.campusrecycle.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Fetch user from Supabase via Repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        // 2. Map your User model to Spring's UserDetails
        // Note: We use the fully qualified name for the Spring User to avoid conflict with your Model User
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword()) // This must be the hashed password
                .authorities(user.getRole())  // e.g., "STUDENT"
                .build();
    }
}