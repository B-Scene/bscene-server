package com.umc.bscene.domain.band.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "Band",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_band_name_status",
                columnNames = {"name", "status"}
        )
)
public class Band extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 밴드를 개설한 오너 (멤버 초대/제거 권한을 가짐)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    // 기본 PENDING(fail-closed): 상태 지정을 잊어도 미검수 밴드가 노출되지 않는 방향으로 실패
    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BandStatus status = BandStatus.PENDING;

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

    @JsonBackReference
    @OneToMany(mappedBy = "band")
    private List<BandMember> bandMembers;

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

    public void accept() {
        this.status = BandStatus.ACCEPTED;
    }

    public boolean isPending() {
        return this.status == BandStatus.PENDING;
    }
}
