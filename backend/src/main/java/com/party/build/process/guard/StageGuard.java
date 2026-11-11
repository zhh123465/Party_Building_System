package com.party.build.process.guard;

public interface StageGuard {
    String name();

    GuardResult check(GuardContext context);
}