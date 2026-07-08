package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AudioStreamRepository extends JpaRepository<AudioStream, Long> {

    // Path 기준으로 쿼리.
    Optional<AudioStream> findByPath(String path);

    // 경로 기입, status는 OPEN으로 고정하여 청취 여부 조회
    Boolean existsByPathAndStatus(String path, StreamStatus status);

    Boolean existsByBroadcasterIdAndStatus(Long userId, StreamStatus status);

    List<AudioStream> findAllByPathIn(List<String> paths);

    // 라이브 세션을 커서 페이지네이션
    @Query("""
select a from AudioStream as a
where a.path in :paths
    and (:cursor is null or a.id < :cursor)
order by a.id desc
""")
    List<AudioStream> findLivePage(
            @Param("paths") List<String> paths,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    Boolean existsByPath(String path);
}
