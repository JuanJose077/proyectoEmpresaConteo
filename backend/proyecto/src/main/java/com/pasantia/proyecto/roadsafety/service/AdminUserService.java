package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.domain.User;
import com.pasantia.proyecto.roadsafety.repository.UserRepository;
import com.pasantia.proyecto.roadsafety.util.PasswordPolicyUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserService {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public CreateUserResult createUser(Long adminId, String email, String role, String tempPassword) {
        String normalized = normalizeEmail(email);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        validateRole(role);

        if (userRepository.findByEmail(normalized).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        String finalPassword = tempPassword;
        if (finalPassword == null || finalPassword.isBlank()) {
            finalPassword = generateTemporaryPassword();
        }

        if (!PasswordPolicyUtil.isValid(finalPassword)) {
            throw new IllegalArgumentException("Temporary password does not meet policy");
        }

        String hash = passwordEncoder.encode(finalPassword);
        long id = userRepository.createUser(normalized, hash, role.toUpperCase(), true, true, adminId);

        return new CreateUserResult(id, normalized, role.toUpperCase(), true, true, adminId, LocalDateTime.now(), finalPassword);
    }

    public void deactivateUser(long id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("ADMIN".equalsIgnoreCase(target.role())) {
            Long firstAdminId = findFirstAdminId();
            if (firstAdminId != null && firstAdminId == id) {
                throw new IllegalStateException("The first administrator cannot be deleted.");
            }
        }

        userRepository.deactivateUser(id);
    }

    public ResetPasswordResult resetPassword(long id, String tempPassword) {
        String finalPassword = tempPassword;
        if (finalPassword == null || finalPassword.isBlank()) {
            finalPassword = generateTemporaryPassword();
        }

        if (!PasswordPolicyUtil.isValid(finalPassword)) {
            throw new IllegalArgumentException("Temporary password does not meet policy");
        }

        String hash = passwordEncoder.encode(finalPassword);
        userRepository.resetPassword(id, hash);

        return new ResetPasswordResult(id, finalPassword);
    }

    private void validateRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        String r = role.trim().toUpperCase();
        if (!"ADMIN".equals(r) && !"USER".equals(r)) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private Long findFirstAdminId() {
        List<User> users = userRepository.findAll();
        Long firstId = null;
        for (User user : users) {
            if (!"ADMIN".equalsIgnoreCase(user.role())) continue;
            if (firstId == null || (user.id() != null && user.id() < firstId)) {
                firstId = user.id();
            }
        }
        return firstId;
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder();
        sb.append(randomChar(UPPER));
        sb.append(randomChar(DIGITS));
        sb.append(randomChar(SPECIAL));
        sb.append(randomChar(LOWER));

        int targetLength = 12;
        while (sb.length() < targetLength) {
            sb.append(randomChar(ALL));
        }

        return shuffle(sb.toString());
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    public record CreateUserResult(
            long id,
            String email,
            String role,
            boolean active,
            boolean mustChangePassword,
            Long createdBy,
            LocalDateTime createdAt,
            String temporaryPassword
    ) {}

    public record ResetPasswordResult(long id, String temporaryPassword) {}
}
