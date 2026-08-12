package com.umc.bscene.domain.recommendation.entity;

import com.umc.bscene.domain.band.entity.Band;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Python 배치가 description 임베딩으로 계산해 적재하는 테이블이므로 BaseEntity(감사 필드)를 상속하지 않는다.
// Spring에서는 SELECT만 하며 INSERT/UPDATE 코드를 만들지 않는다.
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "BandSimilarity")
public class BandSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similarBandId", nullable = false)
    private Band similarBand;

    @Column(nullable = false)
    private Double score;

    // 배치가 적재한 값을 읽기만 하는 필드. Spring 쪽에서 값을 채우지 않으므로 insert/update 대상에서 제외한다.
    @Column(insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
