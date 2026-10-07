package com.umc.bscene.global.security.service;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void ACTIVE_유저는_AuthMember로_변환한다() {
        User user = user(1L, UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("1");

        assertThat(details).isInstanceOf(AuthMember.class);
        assertThat(((AuthMember) details).getUser()).isSameAs(user);
    }

    @Test
    void SUSPENDED_유저는_LockedException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.SUSPENDED)));

        assertThrows(LockedException.class, () -> service.loadUserByUsername("1"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "DELETED"})
    void INACTIVE_DELETED_유저는_DisabledException(UserStatus status) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, status)));

        AccountStatusException exception = assertThrows(
                DisabledException.class,
                () -> service.loadUserByUsername("1")
        );
        assertThat(exception.getMessage()).contains(status.name());
    }

    @Test
    void 존재하지_않는_유저는_UsernameNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("999"));
    }

    @Test
    void 숫자가_아닌_식별자는_UsernameNotFoundException() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("abc"));
    }

    private User user(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .name("테스트유저")
                .birthDate(LocalDate.of(1999, 1, 1))
                .gender(Gender.MALE)
                .phone("01012345678")
                .currentMode(UserMode.FAN)
                .onboardingCompleted(true)
                .status(status)
                .build();
    }
}
