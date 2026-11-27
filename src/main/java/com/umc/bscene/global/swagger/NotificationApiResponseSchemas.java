package com.umc.bscene.global.swagger;

import com.umc.bscene.domain.notification.dto.response.NotificationListItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingItemResponse;
import com.umc.bscene.domain.notification.dto.response.NotificationSettingsResponse;
import com.umc.bscene.global.response.CursorPage;

/** 알림 문서에서 공통 응답의 실제 result 타입을 표시한다. */
public final class NotificationApiResponseSchemas {

    private NotificationApiResponseSchemas() {
    }

    public abstract static class NotificationListPageEnvelope extends SwaggerResponseEnvelope<CursorPage<NotificationListItemResponse>> {
    }

    public abstract static class NotificationSettingItemResponseEnvelope extends SwaggerResponseEnvelope<NotificationSettingItemResponse> {
    }

    public abstract static class NotificationSettingsResponseEnvelope extends SwaggerResponseEnvelope<NotificationSettingsResponse> {
    }
}
