package com.cms.backend.account;

import com.cms.backend.auth.AuthController.AccountResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/members")
public class ManagerMemberController {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public ManagerMemberController(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<AccountResponse> listMembers() {
        return accounts.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createMember(@Valid @RequestBody CreateMemberRequest request) {
        requireUniquePhone(request.phoneNumber());
        requireAtLeastOneRole(request.salesMember(), request.manager());
        UserAccount account = new UserAccount(
                request.name(),
                request.phoneNumber(),
                passwordEncoder.encode(request.initialPassword()),
                request.salesMember(),
                request.manager());
        return AccountResponse.from(accounts.save(account));
    }

    @PatchMapping("/{id}")
    public AccountResponse updateMember(@PathVariable String id, @Valid @RequestBody UpdateMemberRequest request) {
        UserAccount account = findAccount(id);
        if (accounts.existsByPhoneNumberAndIdNot(request.phoneNumber(), id)) {
            throw new IllegalArgumentException("手机号已存在");
        }
        requireAtLeastOneRole(request.salesMember(), request.manager());
        account.rename(request.name());
        account.changePhoneNumber(request.phoneNumber());
        account.changeRoles(request.salesMember(), request.manager());
        return AccountResponse.from(accounts.save(account));
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable String id, @Valid @RequestBody ResetPasswordRequest request) {
        UserAccount account = findAccount(id);
        account.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        accounts.save(account);
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable String id) {
        UserAccount account = findAccount(id);
        account.deactivate();
        accounts.save(account);
    }

    private UserAccount findAccount(String id) {
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("成员不存在"));
    }

    private void requireUniquePhone(String phoneNumber) {
        if (accounts.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("手机号已存在");
        }
    }

    private void requireAtLeastOneRole(boolean salesMember, boolean manager) {
        if (!salesMember && !manager) {
            throw new IllegalArgumentException("账号至少需要一个角色");
        }
    }

    public record CreateMemberRequest(
            @NotBlank String name,
            @NotBlank String phoneNumber,
            @NotBlank String initialPassword,
            boolean salesMember,
            boolean manager) {
    }

    public record UpdateMemberRequest(
            @NotBlank String name,
            @NotBlank String phoneNumber,
            boolean salesMember,
            boolean manager) {
    }

    public record ResetPasswordRequest(@NotBlank String newPassword) {
    }
}
