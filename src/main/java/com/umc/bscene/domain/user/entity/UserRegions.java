package com.umc.bscene.domain.user.entity;

import com.umc.bscene.domain.onboarding.enums.Region;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "UserRegions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "region"})
        }
)
public class UserRegions extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 활동 지역을 선택한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 사용자가 선택한 활동 지역
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Region region;
}