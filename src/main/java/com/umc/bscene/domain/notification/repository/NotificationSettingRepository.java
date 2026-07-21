package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.NotificationSetting;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 사용자의 모드별 알림 설정 조회
    List<NotificationSetting> findAllByUser_IdAndSettingTypeIn(
            Long userId,
            Collection<NotificationSettingType> settingTypes
    );

    // 사용자의 특정 알림 설정 조회
    Optional<NotificationSetting> findByUser_IdAndSettingType(
            Long userId,
            NotificationSettingType settingType
    );
}