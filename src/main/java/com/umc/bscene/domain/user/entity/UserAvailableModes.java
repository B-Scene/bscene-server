package com.umc.bscene.domain.user.entity;

import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "UserAvailableModes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "mode"})
        }
)
public class UserAvailableModes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용 가능 모드를 가진 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // 사용자가 접근 가능한 모드
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserMode mode;
}