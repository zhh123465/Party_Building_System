package com.party.build.common.enums;

public enum Role {
    R1, R2, R3, R4;

    public static Role of(String value) {
        if (value == null) {
            return null;
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}