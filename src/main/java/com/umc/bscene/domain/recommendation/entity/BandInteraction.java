package com.umc.bscene.domain.recommendation.entity;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "BandInteraction",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "bandId"})
        }
)
public class BandInteraction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @Builder.Default
    @Column(nullable = false)
    private Integer clickCount = 0;

    private LocalDateTime lastInteractedAt;
}
