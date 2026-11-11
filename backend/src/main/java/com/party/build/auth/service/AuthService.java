package com.party.build.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.dto.ChangePasswordRequest;
import com.party.build.auth.dto.LoginRequest;
import com.party.build.auth.dto.LoginResponse;
import com.party.build.auth.dto.UserInfo;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.auth.security.JwtService;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 10;

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = (user.getFailedCount() == null ? 0 : user.getFailedCount()) + 1;
            user.setFailedCount(failed);
            if (failed >= MAX_FAIL) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedCount(0);
            }
            userMapper.updateById(user);
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        user.setFailedCount(0);
        user.setLockUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtService.generate(user.getUserId(), user.getRoleCode(), user.getBranchId(),
                user.getPersonId(), user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        UserInfo info = toInfo(user);
        return new LoginResponse(token, jwtService.getExpireHours() * 3600, info);
    }

    public UserInfo current(UserPrincipal principal) {
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return toInfo(user);
    }

    public void logout(UserPrincipal principal) {
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user != null) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            userMapper.updateById(user);
        }
    }

    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(0);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    private UserInfo toInfo(SysUser user) {
        return new UserInfo(user.getUserId(), user.getUsername(), user.getRealName(), user.getRoleCode(),
                user.getBranchId(), user.getPersonId(),
                user.getMustChangePassword() != null && user.getMustChangePassword() == 1);
    }
}