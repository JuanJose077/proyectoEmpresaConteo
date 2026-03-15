package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.domain.User;
import com.pasantia.proyecto.roadsafety.repository.MetasRepository;
import com.pasantia.proyecto.roadsafety.security.UserPrincipal;
import com.pasantia.proyecto.roadsafety.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService;
    private final MetasRepository metasRepository;

    public AdminController(AdminUserService adminUserService, MetasRepository metasRepository) {
        this.adminUserService = adminUserService;
        this.metasRepository = metasRepository;
    }

    @GetMapping("/users")
    public List<UserSummary> listUsers() {
        List<User> users = adminUserService.listUsers();
        Map<Long, String> emailById = new HashMap<>();
        for (User user : users) {
            if (user.id() != null) {
                emailById.put(user.id(), user.email());
            }
        }

        Long firstAdminId = null;
        for (User user : users) {
            if (!"ADMIN".equalsIgnoreCase(user.role())) continue;
            if (firstAdminId == null || (user.id() != null && user.id() < firstAdminId)) {
                firstAdminId = user.id();
            }
        }

        Long finalFirstAdminId = firstAdminId;
        return users.stream()
                .map(user -> toSummary(user, emailById, finalFirstAdminId))
                .toList();
    }

    @PostMapping("/users")
    public CreateUserResponse createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication
    ) {
        Long adminId = resolveAdminId(authentication);

        try {
            AdminUserService.CreateUserResult created = adminUserService.createUser(
                    adminId,
                    request.email(),
                    request.role(),
                    request.temporaryPassword()
            );

            return new CreateUserResponse(
                    created.id(),
                    created.email(),
                    created.role(),
                    created.active(),
                    created.mustChangePassword(),
                    created.createdBy(),
                    created.createdAt(),
                    created.temporaryPassword()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PatchMapping("/users/{id}/deactivate")
    public Map<String, Object> deactivate(@PathVariable long id) {
        try {
            adminUserService.deactivateUser(id);
            return Map.of("ok", true);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResetPasswordResponse resetPassword(
            @PathVariable long id,
            @RequestBody(required = false) ResetPasswordRequest request
    ) {
        String temp = request == null ? null : request.temporaryPassword();

        try {
            AdminUserService.ResetPasswordResult result = adminUserService.resetPassword(id, temp);
            return new ResetPasswordResponse(result.id(), result.temporaryPassword());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PatchMapping("/metas/{id}")
    public Map<String, Object> updateMeta(
            @PathVariable long id,
            @Valid @RequestBody UpdateMetaRequest request
    ) {
        int updated = metasRepository.updateMetaById(id, request.meta(), request.activo());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update or invalid id");
        }
        return Map.of("ok", true);
    }

    private Long resolveAdminId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return principal.id();
    }

    private static UserSummary toSummary(User user, Map<Long, String> emailById, Long firstAdminId) {
        String createdByEmail = null;
        if (user.createdBy() != null) {
            createdByEmail = emailById.get(user.createdBy());
        }
        boolean isFirstAdmin = "ADMIN".equalsIgnoreCase(user.role())
                && firstAdminId != null
                && user.id() != null
                && user.id().longValue() == firstAdminId.longValue();

        return new UserSummary(
                user.id(),
                user.email(),
                user.role(),
                user.active(),
                user.mustChangePassword(),
                createdByEmail,
                user.createdAt(),
                isFirstAdmin
        );
    }

    public record UserSummary(
            Long id,
            String email,
            String role,
            boolean active,
            boolean mustChangePassword,
            String createdByEmail,
            LocalDateTime createdAt,
            boolean firstAdmin
    ) {}

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String role,
            String temporaryPassword
    ) {}

    public record CreateUserResponse(
            long id,
            String email,
            String role,
            boolean active,
            boolean mustChangePassword,
            Long createdBy,
            LocalDateTime createdAt,
            String temporaryPassword
    ) {}

    public record ResetPasswordRequest(String temporaryPassword) {}

    public record ResetPasswordResponse(long id, String temporaryPassword) {}

    public record UpdateMetaRequest(
            Long meta,
            Integer activo
    ) {}
}
