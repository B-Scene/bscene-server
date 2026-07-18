package com.umc.bscene.domain.recommendation.entity;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "BandRecommendationLog")
public class BandRecommendationLog extends BaseEntity {

    // GenerationType.IDENTITY는 PK를 DB가 채번하므로, saveAll()로 여러 건을 넘겨도 Hibernate가
    // insert를 batch_size 단위로 묶지 못하고 건별로 flush한다(각 insert 직후 채번된 id를 즉시 알아야 하기 때문).
    // 노출 로그가 대량으로 쌓여 배치 insert 성능이 문제가 되면 SEQUENCE 전략 전환을 고려할 것.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 50)
    private String algorithmVersion;
}
