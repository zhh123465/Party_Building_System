package com.party.build.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private Long branchId;
    private Long personId;
    private boolean mustChangePassword;
}