package com.party.build.common.enums;

import java.util.List;

public enum Stage {
    S0, S1, S2, S3, S4, S5;

    public static Stage of(String value) {
        if (value == null) {
            return null;
        }
        return Stage.valueOf(value.trim().toUpperCase());
    }

    public Stage next() {
        return switch (this) {
            case S0 -> S1;
            case S1 -> S2;
            case S2 -> S3;
            case S3 -> S4;
            case S4 -> S5;
            case S5 -> null;
        };
    }

    public Stage previous() {
        return switch (this) {
            case S0, S5 -> null;
            case S1 -> S0;
            case S2 -> S1;
            case S3 -> S2;
            case S4 -> S3;
        };
    }

    public static List<Stage> activeStages() {
        return List.of(S0, S1, S2, S3, S4);
    }
}