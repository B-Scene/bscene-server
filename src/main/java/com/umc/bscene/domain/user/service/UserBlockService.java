package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.user.entity.UserBlock;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class UserBlockService {
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final AudioStreamRepository audioStreamRepository;

    public void block(Long blockerId, Long blockedId, Long liveId) {
        validateNotSelf(blockerId, blockedId);
        if (!userRepository.existsById(blockedId)) throw new UserException(UserErrorCode.USER_NOT_FOUND);
        var audioStream = audioStreamRepository.findById(liveId)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND));
        if (userBlockRepository.existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                liveId, blockerId, blockedId))
            throw new UserException(UserErrorCode.USER_ALREADY_BLOCKED);
        userBlockRepository.save(UserBlock.builder()
                .blocker(userRepository.getReferenceById(blockerId))
                .blocked(userRepository.getReferenceById(blockedId))
                .audioStream(audioStream).build());
    }

    public void unblock(Long blockerId, Long blockedId, Long liveId) {
        validateNotSelf(blockerId, blockedId);
        if (!userBlockRepository.existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                liveId, blockerId, blockedId)) {
            throw new UserException(UserErrorCode.USER_NOT_BLOCKED);
        }
        userBlockRepository.deleteByAudioStream_IdAndBlocker_IdAndBlocked_Id(
                liveId, blockerId, blockedId);
    }

    private void validateNotSelf(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) throw new UserException(UserErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }
}
