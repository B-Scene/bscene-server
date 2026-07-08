package com.umc.bscene.domain.band.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "band_profile_links")
public class BandProfileLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "band_profile_link_id")
    private Long bandProfileLinkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_profile_id", nullable = false)
    private BandProfile bandProfile;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private BandProfileLink(
            BandProfile bandProfile,
            String url
    ) {
        this.bandProfile = bandProfile;
        this.url = url;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}