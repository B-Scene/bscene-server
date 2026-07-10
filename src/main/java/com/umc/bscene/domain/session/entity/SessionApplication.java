package com.umc.bscene.domain.session.entity;

import com.umc.bscene.domain.band.entity.BandMember;
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
@Table(name = "session_applications")
public class SessionApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_application_id")
    private Long sessionApplicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Column(name = "purpose", nullable = false, length = 20)
    private String purpose;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

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

    @OneToMany(mappedBy = "sessionApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionApplicationLink> portfolioLinks = new ArrayList<>();

    // 이 프로필로 소속된 밴드 멤버십 (하나의 프로필로 여러 밴드에 소속 가능)
    @OneToMany(mappedBy = "sessionApplication")
    private List<BandMember> bandMembers = new ArrayList<>();

    @Builder
    private SessionApplication(
            Long userId,
            String nickname,
            String title,
            String purpose,
            String profileImageUrl,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String intro
    ) {
        this.userId = userId;
        this.nickname = nickname;
        this.title = title;
        this.purpose = purpose;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.intro = intro;
    }

    public void updateApplication(
            String title,
            String purpose,
            String profileImageUrl,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String intro
    ) {
        this.title = title;
        this.purpose = purpose;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        this.part = part;
        this.skillLevel = skillLevel;
        this.genre = genre;
        this.region = region;
        this.intro = intro;
    }

    public void clearPortfolioLinks() {
        this.portfolioLinks.clear();
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void addPortfolioLink(SessionApplicationLink portfolioLink) {
        this.portfolioLinks.add(portfolioLink);
    }
}
