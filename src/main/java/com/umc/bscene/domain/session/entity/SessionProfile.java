package com.umc.bscene.domain.session.entity;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "session_profiles")
public class SessionProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_profile_id")
    private Long sessionProfileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "part", nullable = false, length = 30)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 30)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 30)
    private SessionGenre genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 30)
    private SessionRegion region;

    @Column(name = "intro", length = 500)
    private String intro;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "sessionProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionProfileLink> portfolioLinks = new ArrayList<>();

    @Builder
    private SessionProfile(
            Long userId,
            String nickname,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String intro
    ) {
        this.userId = userId;
        this.nickname = nickname;
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.intro = intro;
    }

    public void updateProfile(
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String intro
    ) {
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.intro = intro;
    }

    public void clearPortfolioLinks() {
        this.portfolioLinks.clear();
    }

    public void addPortfolioLink(SessionProfileLink portfolioLink) {
        this.portfolioLinks.add(portfolioLink);
    }
}