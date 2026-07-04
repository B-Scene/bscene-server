package com.umc.bscene.global.security.service;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * JWT subject(userId)로 유저를 조회해 UserDetails로 변환
     * JwtAuthFilter에서 토큰 검증 후 호출
     */
    @Override
    public UserDetails loadUserByUsername(
            String userId
    ) throws UsernameNotFoundException {
        try {
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다. userId=" + userId));

            return new AuthMember(user);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("유효하지 않은 사용자 식별자입니다. userId=" + userId);
        }
    }
}