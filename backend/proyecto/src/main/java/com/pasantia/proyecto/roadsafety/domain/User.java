package com.pasantia.proyecto.roadsafety.domain;

import java.time.LocalDateTime;

public class User {
    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final boolean active;
    private final boolean mustChangePassword;
    private final Long createdBy;
    private final LocalDateTime createdAt;

    public User(
            Long id,
            String email,
            String password,
            String role,
            boolean active,
            boolean mustChangePassword,
            Long createdBy,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    public String role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public Long createdBy() {
        return createdBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
