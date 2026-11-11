package com.party.build.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long userId;
    private final String username;
    private final String realName;
    private final String roleCode;
    private final Long branchId;
    private final Long personId;
    private final Integer tokenVersion;
    private final boolean enabled;

    public UserPrincipal(Long userId, String username, String realName, String roleCode,
                         Long branchId, Long personId, Integer tokenVersion, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCode = roleCode;
        this.branchId = branchId;
        this.personId = personId;
        this.tokenVersion = tokenVersion;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}