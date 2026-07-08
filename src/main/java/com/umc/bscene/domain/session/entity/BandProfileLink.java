package com.umc.bscene.domain.session.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "session_profile_links")
public class BandProfileLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_profile_link_id")
    private Long sessionProfileLinkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_profile_id", nullable = false)
    private BandProfile sessionProfile;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private BandProfileLink(
            BandProfile sessionProfile,
            String url
    ) {
        this.sessionProfile = sessionProfile;
        this.url = url;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}