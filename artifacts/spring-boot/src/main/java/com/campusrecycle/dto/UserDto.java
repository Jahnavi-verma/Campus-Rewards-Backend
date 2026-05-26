package com.campusrecycle.dto;

import com.campusrecycle.model.User;
import java.time.LocalDateTime;

public class UserDto {

    private Long id;
    private String email;
    private String name;
    private String avatarUrl;
    private int points;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.name = user.getName();
        dto.avatarUrl = user.getAvatarUrl();
        dto.points = user.getPoints();
        dto.role = user.getRole();
        dto.createdAt = user.getCreatedAt();
        dto.lastLoginAt = user.getLastLoginAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getPoints() { return points; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
