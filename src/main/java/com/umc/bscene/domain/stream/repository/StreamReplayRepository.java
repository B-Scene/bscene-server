package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.StreamReplay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StreamReplayRepository extends JpaRepository<StreamReplay, Long> {

    boolean existsByS3Key(String s3Key);

    // 라이브의 첫(대표) 세그먼트
    Optional<StreamReplay> findFirstByAudioStream_IdOrderByCreatedAtAsc(Long audioStreamId);

    // 원자적 증가로 동시 시청자 환경의 lost update 방지
    @Modifying
    @Query("update StreamReplay r set r.viewCount = r.viewCount + 1 where r.id = :id")
    int increaseViewCount(@Param("id") Long id);

    /*
     * 팬모드 홈 다시보기 섹션용. 라이브별 대표 세그먼트 1행만 뽑아 최신 업로드순으로 조회
     * 대표 선정은 min(id): 세그먼트 병렬 업로드로 createdAt이 동시각일 수 있어 min(createdAt)은 중복 행 위험
     */
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.id = (select min(r2.id) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
order by r.id desc
""")
    List<StreamReplay> findLatestReplays(Pageable pageable);
}
