package org.lab.model;

import java.util.UUID;

public record User(
        UUID id,
        String username,
        String email,
        String fullName
) {
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }
    }

    public static User create(String username, String email, String fullName) {
        return new User(UUID.randomUUID(), username, email, fullName);
    }

    public String shortInfo() {
        return STR."\{username} (\{email})";
    }
}
