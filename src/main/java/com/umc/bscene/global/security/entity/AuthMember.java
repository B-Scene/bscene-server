package com.umc.bscene.global.security.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AuthMember implements UserDetails {

    private final User user;

    // 사용자 권한 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == UserRole.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        if (user.getCurrentMode() == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getCurrentMode().name()));
    }

    // 비밀번호 반환
    @Override
    public String getPassword() {
        return null;
    }

    // 사용자 식별자 반환
    @Override
    public String getUsername() {
        return String.valueOf(user.getId());
    }
}