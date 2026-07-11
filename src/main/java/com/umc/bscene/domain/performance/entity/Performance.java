package com.umc.bscene.domain.performance.entity;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Performance")
public class Performance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공연을 등록한 밴드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false)
    private Band band;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(nullable = false)
    private LocalDate performanceDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Region region;

    @Column(length = 200, nullable = false)
    private String venue;

    @Column(length = 1000, nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer ticketPrice;

    @Column(length = 500)
    private String ticketLink;

    @Column(length = 500)
    private String posterImageUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PerformanceStatus status = PerformanceStatus.ACTIVE;

    // 수정 API에서 전달된 값만 부분 반영
    public void update(
            String title,
            Genre genre,
            LocalDate performanceDate,
            LocalTime startTime,
            String venue,
            Integer ticketPrice,
            String ticketLink,
            String posterImageUrl
    ) {
        if (title != null) this.title = title;
        if (genre != null) this.genre = genre;
        if (performanceDate != null) this.performanceDate = performanceDate;
        if (startTime != null) this.startTime = startTime;
        if (venue != null) this.venue = venue;
        if (ticketPrice != null) this.ticketPrice = ticketPrice;
        if (ticketLink != null) this.ticketLink = ticketLink;
        if (posterImageUrl != null) this.posterImageUrl = posterImageUrl;
    }

    public void delete() {
        this.status = PerformanceStatus.DELETED;
    }
}
