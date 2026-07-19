package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.recommendation.entity.BandRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BandRecommendationLogRepository extends JpaRepository<BandRecommendationLog, Long> {
    // 추천 노출 시마다 여러 건이 한 번에 쌓이므로 JpaRepository가 기본 제공하는
    // saveAll(List<BandRecommendationLog>)로 배치 저장한다. IDENTITY 전략 특성상 실제 JDBC batch insert로는
    // 묶이지 않는다는 점은 BandRecommendationLog 엔티티의 id 필드 주석 참고.
}
