package com.umc.bscene.domain.session.entity;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.global.entity.BaseEntity;
import com.umc.bscene.domain.session.enums.SessionRegion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "session_recruitment")
public class SessionRecruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionRecruitmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    private Band band;

    @Column(length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionGenre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionRegion region;

    @Column(length = 100)
    private String practiceSchedule;

    @Column(length = 100)
    private String practicePlace;

    @Column(nullable = false)
    private LocalDateTime deadlineAt;

    @Column(nullable = false, length = 500)
    private String qualification;

    @Column
    private LocalDateTime deletedAt;
    @Column(nullable = false, length = 100)
    private String recruitmentTitle;
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
    public void update(
            String recruitmentTitle,
            String content,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String practiceSchedule,
            String practicePlace,
            LocalDateTime deadlineAt,
            String qualification
    ) {
        this.recruitmentTitle = recruitmentTitle;
        this.content = content;
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.practiceSchedule = practiceSchedule;
        this.practicePlace = practicePlace;
        this.deadlineAt = deadlineAt;
        this.qualification = qualification;
    }
}