package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGenresRepository extends JpaRepository<UserGenres, Long> {

    List<UserGenres> findAllByUser(User user);

    void deleteAllByUser(User user);
}