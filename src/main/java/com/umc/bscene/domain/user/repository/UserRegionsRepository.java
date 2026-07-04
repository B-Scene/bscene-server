package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserRegions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRegionsRepository extends JpaRepository<UserRegions, Long> {

    List<UserRegions> findAllByUser(User user);

    void deleteAllByUser(User user);
}