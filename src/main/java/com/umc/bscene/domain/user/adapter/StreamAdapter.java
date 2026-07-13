package com.umc.bscene.domain.user.adapter;

import com.umc.bscene.domain.stream.port.UserPort;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class StreamAdapter implements UserPort {

    private final UserRepository userRepository;

    @Override
    public List<User> findAllByIds(Collection<Long> userIds) {
        return userRepository.findAllById(userIds);
    }
}