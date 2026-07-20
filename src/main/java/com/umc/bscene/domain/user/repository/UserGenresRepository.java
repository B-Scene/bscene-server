package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGenresRepository extends JpaRepository<UserGenres, Long> {

    List<UserGenres> findAllByUser(User user);

    // 온보딩 저장이 선택 순서대로 insert하므로 PK 오름차순 = 사용자가 고른 순서
    List<UserGenres> findAllByUserOrderByIdAsc(User user);

    void deleteAllByUser(User user);
}