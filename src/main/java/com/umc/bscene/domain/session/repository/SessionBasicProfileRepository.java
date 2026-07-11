package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface SessionBasicProfileRepository
        extends JpaRepository<SessionBasicProfile, Long> {

    Optional<SessionBasicProfile> findByUser_Id(Long userId);

    List<SessionBasicProfile> findAllByUser_IdIn(Collection<Long> userIds);
}
