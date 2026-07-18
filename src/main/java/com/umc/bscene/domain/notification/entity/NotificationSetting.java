package com.umc.bscene.domain.notification.entity;

import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "NotificationSettings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ukNotificationSettingsUserType",
                        columnNames = {"userId", "settingType"}
                )
        }
)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationSettingId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "settingType", nullable = false)
    private NotificationSettingType settingType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    public static NotificationSetting of(
            User user,
            NotificationSettingType settingType
    ) {
        return NotificationSetting.builder()
                .user(user)
                .settingType(settingType)
                .enabled(settingType.isDefaultEnabled())
                .build();
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}