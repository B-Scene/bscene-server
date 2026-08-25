package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@IncludesPendingBands(reason = "추천 후보는 하이드레이션 단계(findAllByIdInAndStatus 등)에서 ACCEPTED로 걸러지고, 삭제 쿼리는 검수 밴드 정리용이다")
public interface BandSimilarityRepository extends JpaRepository<BandSimilarity, Long> {

    // band는 연관관계 매핑(@ManyToOne)이라 파생 쿼리 메서드(findByBandIdIn)로 안전하게 풀린다는 보장이 없어 @Query로 명시한다.
    @Query("SELECT bs FROM BandSimilarity bs WHERE bs.band.id IN :bandIds")
    List<BandSimilarity> findByBandIdIn(@Param("bandIds") List<Long> bandIds);

    // 검수 거절/더미 교체로 밴드 삭제 시 FK 정리 - 기준/대상 양방향 모두 삭제해야 제약에 걸리지 않는다
    // 호출측 트랜잭션의 영속성 컨텍스트를 비우면 안 되므로 clearAutomatically를 켜지 않는다
    @Modifying
    @Query("DELETE FROM BandSimilarity bs WHERE bs.band.id = :bandId OR bs.similarBand.id = :bandId")
    void deleteAllByBandId(@Param("bandId") Long bandId);
}
