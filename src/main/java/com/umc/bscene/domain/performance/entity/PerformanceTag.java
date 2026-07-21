package com.umc.bscene.domain.performance.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PerformanceTag")
public class PerformanceTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 태그가 속한 공연
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performanceId", nullable = false)
    private Performance performance;

    @Column(nullable = false, length = 30)
    private String tagName;
}
