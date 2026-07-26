package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserBlock;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    private static final Long BLOCKER_ID = 1L;
    private static final Long BLOCKED_ID = 2L;
    private static final Long LIVE_ID = 3L;

    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AudioStreamRepository audioStreamRepository;

    private UserBlockService service;

    @BeforeEach
    void setUp() {
        service = new UserBlockService(
                userBlockRepository,
                userRepository,
                audioStreamRepository
        );
    }

    @Test
    @DisplayName("라이브 방송에서 특정 사용자를 차단한다")
    void blockSuccess() {
        User blocker = User.builder().id(BLOCKER_ID).build();
        User blocked = User.builder().id(BLOCKED_ID).build();
        AudioStream stream = AudioStream.builder().id(LIVE_ID).build();
        when(userRepository.existsById(BLOCKED_ID)).thenReturn(true);
        when(audioStreamRepository.findById(LIVE_ID)).thenReturn(Optional.of(stream));
        when(userBlockRepository
                .existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        LIVE_ID, BLOCKER_ID, BLOCKED_ID
                )).thenReturn(false);
        when(userRepository.getReferenceById(BLOCKER_ID)).thenReturn(blocker);
        when(userRepository.getReferenceById(BLOCKED_ID)).thenReturn(blocked);

        service.block(BLOCKER_ID, BLOCKED_ID, LIVE_ID);

        ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).save(captor.capture());
        assertThat(captor.getValue().getBlocker()).isSameAs(blocker);
        assertThat(captor.getValue().getBlocked()).isSameAs(blocked);
        assertThat(captor.getValue().getAudioStream()).isSameAs(stream);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void blockFailsWhenBlockingSelf() {
        assertThatThrownBy(() -> service.block(
                BLOCKER_ID, BLOCKER_ID, LIVE_ID
        ))
                .isInstanceOf(UserException.class)
                .extracting("baseResponseCode")
                .isEqualTo(UserErrorCode.SELF_BLOCK_NOT_ALLOWED);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 차단할 수 없다")
    void blockFailsWhenUserNotFound() {
        when(userRepository.existsById(BLOCKED_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.block(
                BLOCKER_ID, BLOCKED_ID, LIVE_ID
        ))
                .isInstanceOf(UserException.class)
                .extracting("baseResponseCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(audioStreamRepository, never()).findById(any());
    }

    @Test
    @DisplayName("이미 차단한 사용자를 다시 차단할 수 없다")
    void blockFailsWhenAlreadyBlocked() {
        when(userRepository.existsById(BLOCKED_ID)).thenReturn(true);
        when(audioStreamRepository.findById(LIVE_ID))
                .thenReturn(Optional.of(AudioStream.builder().id(LIVE_ID).build()));
        when(userBlockRepository
                .existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        LIVE_ID, BLOCKER_ID, BLOCKED_ID
                )).thenReturn(true);

        assertThatThrownBy(() -> service.block(
                BLOCKER_ID, BLOCKED_ID, LIVE_ID
        ))
                .isInstanceOf(UserException.class)
                .extracting("baseResponseCode")
                .isEqualTo(UserErrorCode.USER_ALREADY_BLOCKED);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("차단한 사용자의 차단을 해제한다")
    void unblockSuccess() {
        when(userBlockRepository
                .existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        LIVE_ID, BLOCKER_ID, BLOCKED_ID
                )).thenReturn(true);

        service.unblock(BLOCKER_ID, BLOCKED_ID, LIVE_ID);

        verify(userBlockRepository)
                .deleteByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        LIVE_ID, BLOCKER_ID, BLOCKED_ID
                );
    }

    @Test
    @DisplayName("차단하지 않은 사용자는 차단 해제할 수 없다")
    void unblockFailsWhenNotBlocked() {
        when(userBlockRepository
                .existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        LIVE_ID, BLOCKER_ID, BLOCKED_ID
                )).thenReturn(false);

        assertThatThrownBy(() -> service.unblock(
                BLOCKER_ID, BLOCKED_ID, LIVE_ID
        ))
                .isInstanceOf(UserException.class)
                .extracting("baseResponseCode")
                .isEqualTo(UserErrorCode.USER_NOT_BLOCKED);

        verify(userBlockRepository, never())
                .deleteByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                        any(), any(), any()
                );
    }
}
