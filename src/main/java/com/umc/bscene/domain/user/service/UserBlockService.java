package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.user.entity.UserBlock;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.repository.UserBlockRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class UserBlockService {
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    public void block(Long blockerId, Long blockedId) {
        validateNotSelf(blockerId, blockedId);
        if (!userRepository.existsById(blockedId)) throw new UserException(UserErrorCode.USER_NOT_FOUND);
        if (userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId))
            throw new UserException(UserErrorCode.USER_ALREADY_BLOCKED);
        userBlockRepository.save(UserBlock.builder()
                .blocker(userRepository.getReferenceById(blockerId))
                .blocked(userRepository.getReferenceById(blockedId)).build());
    }

    public void unblock(Long blockerId, Long blockedId) {
        validateNotSelf(blockerId, blockedId);
        if (!userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)) {
            throw new UserException(UserErrorCode.USER_NOT_BLOCKED);
        }
        userBlockRepository.deleteByBlocker_IdAndBlocked_Id(blockerId, blockedId);
    }

    private void validateNotSelf(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) throw new UserException(UserErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }
}
