package com.umc.bscene.domain.auth.service.auth;

import com.umc.bscene.domain.auth.dto.auth.response.LoginIdCheckResponse;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.auth.repository.term.UserTermsRepository;
import com.umc.bscene.domain.auth.service.verification.PhoneVerificationService;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalCredentialRepository localCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private UserTermsRepository userTermsRepository;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                localCredentialRepository,
                passwordEncoder,
                stringRedisTemplate,
                jwtUtil,
                phoneVerificationService,
                userTermsRepository
        );
    }

    // ---------- checkLoginId ----------

    @Test
    void checkLoginId_사용하지_않는_아이디면_사용_가능하다() {
        when(localCredentialRepository.existsByLoginId("available@example.com"))
                .thenReturn(false);

        LoginIdCheckResponse response =
                service.checkLoginId("available@example.com");

        assertThat(response.available()).isTrue();

        verify(localCredentialRepository)
                .existsByLoginId("available@example.com");
    }

    @Test
    void checkLoginId_이미_사용_중인_아이디면_사용_불가능하다() {
        when(localCredentialRepository.existsByLoginId("duplicate@example.com"))
                .thenReturn(true);

        LoginIdCheckResponse response =
                service.checkLoginId("duplicate@example.com");

        assertThat(response.available()).isFalse();

        verify(localCredentialRepository)
                .existsByLoginId("duplicate@example.com");
    }
}