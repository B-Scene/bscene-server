package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserRegions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRegionsRepository extends JpaRepository<UserRegions, Long> {

    List<UserRegions> findAllByUser(User user);

    // 온보딩/내 정보 수정 저장이 선택 순서대로 insert하므로 PK 오름차순 = 사용자가 고른 순서
    List<UserRegions> findAllByUserOrderByIdAsc(User user);

    void deleteAllByUser(User user);
}