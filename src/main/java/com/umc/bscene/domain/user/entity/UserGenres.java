package com.umc.bscene.domain.user.entity;

import com.umc.bscene.domain.onboarding.enums.Genre;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "UserGenres",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "genre"})
        }
)
public class UserGenres extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관심 장르를 선택한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 사용자가 선택한 관심 장르
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Genre genre;
}