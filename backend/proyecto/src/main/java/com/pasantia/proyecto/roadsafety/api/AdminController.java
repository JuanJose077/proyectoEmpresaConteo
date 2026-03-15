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
        return adminUserService.listUsers().stream()
                .map(AdminController::toSummary)
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
        adminUserService.deactivateUser(id);
        return Map.of("ok", true);
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

    private static UserSummary toSummary(User user) {
        return new UserSummary(
                user.id(),
                user.email(),
                user.role(),
                user.active(),
                user.mustChangePassword(),
                user.createdBy(),
                user.createdAt()
        );
    }

    public record UserSummary(
            Long id,
            String email,
            String role,
            boolean active,
            boolean mustChangePassword,
            Long createdBy,
            LocalDateTime createdAt
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
