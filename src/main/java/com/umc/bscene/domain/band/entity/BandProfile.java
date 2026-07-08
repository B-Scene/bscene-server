package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
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
@Table(name = "band_profiles")
public class BandProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "band_profile_id")
    private Long bandProfileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "part", length = 30)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 30)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", length = 30)
    private SessionGenre genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 30)
    private SessionRegion region;

    @Column(name = "intro", length = 500)
    private String intro;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "bandProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BandProfileLink> portfolioLinks = new ArrayList<>();

    @OneToMany(mappedBy = "bandProfile")
    private List<BandMember> bandMembers = new ArrayList<>();

    @Builder
    private BandProfile(
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
            String nickname,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String intro
    ) {
        this.nickname = nickname;
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.intro = intro;
    }

    public void clearPortfolioLinks() {
        this.portfolioLinks.clear();
    }

    public void addPortfolioLink(BandProfileLink portfolioLink) {
        this.portfolioLinks.add(portfolioLink);
    }
}