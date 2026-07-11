package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.StreamReplay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StreamReplayRepository extends JpaRepository<StreamReplay, Long> {

    boolean existsByS3Key(String s3Key);

    // 라이브의 첫(대표) 세그먼트
    Optional<StreamReplay> findFirstByAudioStream_IdOrderByCreatedAtAsc(Long audioStreamId);

    // 원자적 증가로 동시 시청자 환경의 lost update 방지
    @Modifying
    @Query("update StreamReplay r set r.viewCount = r.viewCount + 1 where r.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
