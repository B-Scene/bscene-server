package com.umc.bscene.domain.user.adapter;

import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class StreamAdapter implements UserPort {

    private final UserRepository userRepository;
    private final FanProfileRepository fanProfileRepository;

    @Override
    public List<User> findAllByIds(Collection<Long> userIds) {
        return userRepository.findAllById(userIds);
    }

    @Override
    public boolean validAccessAboutStreamInBandMode(Long userId, Collection<Long> coHostIds) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 온보딩 미완료 등으로 currentMode가 null일 수 있으므로 NPE가 나지 않게 enum 쪽 기준으로 비교
        if (UserMode.BAND == user.getCurrentMode() && !coHostIds.contains(user.getId())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isBandMode(Long userId) {
        return userRepository.findById(userId)
                .map(user -> UserMode.BAND == user.getCurrentMode())
                .orElse(false);
    }

    @Override
    public String getFanName(Long userId) {
        return fanProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND))
                .getNickname();
    }
}