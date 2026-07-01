package com.umc.bscene.global.security.entity;

import com.umc.bscene.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
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
        return List.of();
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