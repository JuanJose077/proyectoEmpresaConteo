package com.pasantia.proyecto.roadsafety.security;

public record UserPrincipal(
        Long id,
        String email,
        String role,
        boolean mustChangePassword
) {}
