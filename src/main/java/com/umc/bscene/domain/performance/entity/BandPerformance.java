package com.umc.bscene.domain.performance.entity;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "BandPerformance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"bandId", "performanceId"})
        }
)
public class BandPerformance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공연에 참여하는 밴드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performanceId", nullable = false)
    private Performance performance;
}
