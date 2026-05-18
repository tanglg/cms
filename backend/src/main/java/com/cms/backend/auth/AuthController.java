package com.cms.backend.auth;

import com.cms.backend.account.UserAccount;
import com.cms.backend.account.UserAccountRepository;
import com.cms.backend.security.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository accounts;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserAccountRepository accounts, AuthSessionRepository sessions, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = accounts.findByPhoneNumber(request.phoneNumber())
                .filter(UserAccount::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("手机号或密码错误"));
        String token = UUID.randomUUID().toString();
        sessions.save(new AuthSession(token, account, Instant.now()));
        return new LoginResponse(token, AccountResponse.from(account));
    }

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal CurrentAccount current) {
        return AccountResponse.from(current.account());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@AuthenticationPrincipal CurrentAccount current, @Valid @RequestBody ChangePasswordRequest request) {
        UserAccount account = current.account();
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        account.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        accounts.save(account);
    }

    @DeleteMapping("/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@org.springframework.web.bind.annotation.RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization.startsWith("Bearer ")) {
            sessions.deleteById(authorization.substring("Bearer ".length()));
        }
    }

    public record LoginRequest(@NotBlank String phoneNumber, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
    }

    public record LoginResponse(String token, AccountResponse account) {
    }

    public record AccountResponse(String id, String name, String phoneNumber, List<String> roles, boolean active) {
        public static AccountResponse from(UserAccount account) {
            var roles = new java.util.ArrayList<String>();
            if (account.isSalesMember()) {
                roles.add("SALES_MEMBER");
            }
            if (account.isManager()) {
                roles.add("MANAGER");
            }
            return new AccountResponse(account.getId(), account.getName(), account.getPhoneNumber(), List.copyOf(roles), account.isActive());
        }
    }
}
