package com.party.build.common.api;

import java.util.UUID;

public final class TraceId {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceId() {
    }

    public static String current() {
        String id = HOLDER.get();
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            HOLDER.set(id);
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}