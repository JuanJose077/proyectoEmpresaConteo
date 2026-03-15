package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.domain.User;
import com.pasantia.proyecto.roadsafety.repository.UserRepository;
import com.pasantia.proyecto.roadsafety.security.JwtService;
import com.pasantia.proyecto.roadsafety.security.UserPrincipal;
import com.pasantia.proyecto.roadsafety.util.PasswordPolicyUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;



    }

    public AuthResult login(String email, String password) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalStateException("Invalid credentials"));
        System.out.println("Email login: " + normalized);
        System.out.println("Password matches: " + passwordEncoder.matches(password, user.password()));




        if (!user.active()) {
            throw new IllegalStateException("User is inactive");
        }

        if (!passwordEncoder.matches(password, user.password())) {
            throw new IllegalStateException("Invalid credentials");
        }

        return buildResult(user);
    }

    public AuthResult changePassword(long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!user.active()) {
            throw new IllegalStateException("User is inactive");
        }

        if (!passwordEncoder.matches(currentPassword, user.password())) {
            throw new IllegalStateException("Current password is incorrect");
        }

        if (!PasswordPolicyUtil.isValid(newPassword)) {
            throw new IllegalArgumentException("Password does not meet policy");
        }

        String hash = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(user.id(), hash, false);

        User updated = new User(
                user.id(),
                user.email(),
                hash,
                user.role(),
                user.active(),
                false,
                user.createdBy(),
                user.createdAt()
        );

        return buildResult(updated);
    }

    private AuthResult buildResult(User user) {
        UserPrincipal principal = new UserPrincipal(user.id(), user.email(), user.role(), user.mustChangePassword());
        String token = jwtService.generateToken(principal);
        Instant expiresAt = Instant.now().plus(jwtService.getExpirationMinutes(), ChronoUnit.MINUTES);

        return new AuthResult(
                token,
                expiresAt.toString(),
                new AuthUser(user.id(), user.email(), user.role(), user.mustChangePassword())
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public record AuthResult(String token, String expiresAt, AuthUser user) {}

    public record AuthUser(Long id, String email, String role, boolean mustChangePassword) {}
}
