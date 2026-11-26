package com.tlcn.backend.user.controller;

import com.tlcn.backend.user.dto.AccountRequests;
import com.tlcn.backend.user.dto.AccountResponse;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.UserStatus;
import com.tlcn.backend.user.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public AccountResponse.Listing list(
            @RequestParam(defaultValue = "") @Size(max = 190) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return accounts.list(keyword, role, status, page, size);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accounts.get(id);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal UserSummary admin,
            @Valid @RequestBody AccountRequests.Create request) {
        AccountResponse account = accounts.create(admin.id(), request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.id())).body(account);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@AuthenticationPrincipal UserSummary admin, @PathVariable Long id,
            @Valid @RequestBody AccountRequests.Update request) {
        return accounts.update(admin.id(), id, request);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse setLocked(@AuthenticationPrincipal UserSummary admin, @PathVariable Long id,
            @Valid @RequestBody AccountRequests.Lock request) {
        return accounts.setLocked(admin.id(), id, request.locked());
    }
}
