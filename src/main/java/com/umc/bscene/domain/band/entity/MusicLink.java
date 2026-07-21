package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.band.enums.EtcMusicPlatform;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "MusicLink")
public class MusicLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 음원 링크가 속한 밴드 (밴드당 1행)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bandId", nullable = false, unique = true)
    private Band band;

    @Column(length = 500)
    private String spotifyUrl;

    @Column(length = 500)
    private String youtubeUrl;

    @Column(length = 500)
    private String soundcloudUrl;

    @Enumerated(EnumType.STRING)
    private EtcMusicPlatform etcPlatform;

    @Column(length = 500)
    private String etcUrl;

    @Column(length = 500)
    private String otherUrl;

    // 저장하기 API에서 전달된 값으로 전체 덮어쓰기
    public void update(
            String spotifyUrl,
            String youtubeUrl,
            String soundcloudUrl,
            EtcMusicPlatform etcPlatform,
            String etcUrl,
            String otherUrl
    ) {
        this.spotifyUrl = spotifyUrl;
        this.youtubeUrl = youtubeUrl;
        this.soundcloudUrl = soundcloudUrl;
        this.etcPlatform = etcPlatform;
        this.etcUrl = etcUrl;
        this.otherUrl = otherUrl;
    }
}
