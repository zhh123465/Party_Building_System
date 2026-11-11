package com.party.build.auth.controller;

import com.party.build.auth.dto.ChangePasswordRequest;
import com.party.build.auth.dto.LoginRequest;
import com.party.build.auth.dto.LoginResponse;
import com.party.build.auth.dto.UserInfo;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.auth.service.AuthService;
import com.party.build.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserInfo> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(authService.current(principal));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return Result.ok();
    }
}