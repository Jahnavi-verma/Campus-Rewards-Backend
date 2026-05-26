package com.campusrecycle.service;

import com.campusrecycle.model.User;
import com.campusrecycle.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(String githubId, String email, String name, String avatarUrl) {
        Optional<User> existing = userRepository.findByGithubId(githubId);

        if (existing.isPresent()) {
            User user = existing.get();
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        User user = new User();
        user.setGithubId(githubId);
        user.setEmail(email);
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User addPoints(Long userId, int points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setPoints(user.getPoints() + points);
        return userRepository.save(user);
    }
}
