package com.umc.bscene.domain.band.entity;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Band")
public class Band extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 밴드를 개설한 오너 (멤버 초대/제거 권한을 가짐)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", nullable = false)
    private User owner;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Region region;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 1000)
    private String description;

    public void update(
            String name,
            Genre genre,
            Region region,
            String profileImageUrl,
            String description
    ) {
        if (name != null) this.name = name;
        if (genre != null) this.genre = genre;
        if (region != null) this.region = region;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (description != null) this.description = description;
    }

    public void transferOwnership(User newOwner) {
        this.owner = newOwner;
    }

    public void deleteProfileImage() {
        this.profileImageUrl = null;
    }
}
