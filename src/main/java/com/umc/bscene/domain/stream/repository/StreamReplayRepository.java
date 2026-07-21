package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.StreamReplay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StreamReplayRepository extends JpaRepository<StreamReplay, Long> {

    boolean existsByS3Key(String s3Key);

    long countByAudioStream_Id(Long audioStreamId);

    // 라이브의 전체 세그먼트를 재생 순서로 조회. mediamtx 세그먼트 파일명이 시간순이라 s3Key 오름차순 = 녹화 순서
    List<StreamReplay> findAllByAudioStream_IdOrderByS3KeyAsc(Long audioStreamId);

    // 원자적 증가로 동시 시청자 환경의 lost update 방지
    @Modifying
    @Query("update StreamReplay r set r.viewCount = r.viewCount + 1 where r.id = :id")
    int increaseViewCount(@Param("id") Long id);

    /*
     * 팬모드 홈 다시보기 섹션용. 장시간 라이브는 녹화가 세그먼트 여러 행으로 나뉘므로,
     * 목록에 같은 라이브가 중복 노출되지 않도록 라이브당 첫 녹화 파일(s3Key 오름차순 첫 행)만 대표로 뽑는다.
     * watchReplay가 조회수를 증가시키는 행과 동일하다.
     */
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.s3Key = (select min(r2.s3Key) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
order by r.id desc
""")
    List<StreamReplay> findLatestReplays(Pageable pageable);

    // 다시보기 목록 최신순 (전체 탭). 라이브별 대표(첫 녹화 파일) 세그먼트만, id 커서 페이징
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.s3Key = (select min(r2.s3Key) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
    and (:cursor is null or r.id < :cursor)
order by r.id desc
""")
    List<StreamReplay> findReplayPageLatest(@Param("cursor") Long cursor, Pageable pageable);

    // 다시보기 목록 최신순 (팔로우 탭)
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.s3Key = (select min(r2.s3Key) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
    and r.audioStream.bandId in :bandIds
    and (:cursor is null or r.id < :cursor)
order by r.id desc
""")
    List<StreamReplay> findReplayPageLatestByBandIds(
            @Param("bandIds") List<Long> bandIds, @Param("cursor") Long cursor, Pageable pageable
    );

    // 다시보기 목록 인기순 (전체 탭). (viewCount, id) 복합 keyset으로 동률 정렬 안정성 보장
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.s3Key = (select min(r2.s3Key) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
    and (:cursor is null
        or r.viewCount < :cursorViewCount
        or (r.viewCount = :cursorViewCount and r.id < :cursor))
order by r.viewCount desc, r.id desc
""")
    List<StreamReplay> findReplayPagePopular(
            @Param("cursorViewCount") Long cursorViewCount, @Param("cursor") Long cursor, Pageable pageable
    );

    // 다시보기 목록 인기순 (팔로우 탭)
    @Query("""
select r from StreamReplay r
join fetch r.audioStream
where r.s3Key = (select min(r2.s3Key) from StreamReplay r2 where r2.audioStream.id = r.audioStream.id)
    and r.audioStream.bandId in :bandIds
    and (:cursor is null
        or r.viewCount < :cursorViewCount
        or (r.viewCount = :cursorViewCount and r.id < :cursor))
order by r.viewCount desc, r.id desc
""")
    List<StreamReplay> findReplayPagePopularByBandIds(
            @Param("bandIds") List<Long> bandIds,
            @Param("cursorViewCount") Long cursorViewCount, @Param("cursor") Long cursor, Pageable pageable
    );
}
