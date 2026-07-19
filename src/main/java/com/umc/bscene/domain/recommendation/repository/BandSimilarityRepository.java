package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BandSimilarityRepository extends JpaRepository<BandSimilarity, Long> {

    // band는 연관관계 매핑(@ManyToOne)이라 파생 쿼리 메서드(findByBandIdIn)로 안전하게 풀린다는 보장이 없어 @Query로 명시한다.
    @Query("SELECT bs FROM BandSimilarity bs WHERE bs.band.id IN :bandIds")
    List<BandSimilarity> findByBandIdIn(@Param("bandIds") List<Long> bandIds);
}
