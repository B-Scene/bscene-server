package com.umc.bscene.domain.user.entity;

import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.enums.UserRole;
import com.umc.bscene.domain.user.enums.UserStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "User")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false, unique = true, length = 11)
    private String phone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column
    @Enumerated(EnumType.STRING)
    private UserMode currentMode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column
    private LocalDateTime onboardingCompletedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column
    private LocalDateTime deletedAt;

    // 온보딩 완료 처리: 현재 모드 설정 + 완료 플래그/시각 기록
    public void completeOnboarding(UserMode currentMode) {
        this.currentMode = currentMode;
        this.onboardingCompleted = true;
        this.onboardingCompletedAt = LocalDateTime.now();
    }

    public void changeMode(UserMode mode) {
        this.currentMode = mode;
    }
}