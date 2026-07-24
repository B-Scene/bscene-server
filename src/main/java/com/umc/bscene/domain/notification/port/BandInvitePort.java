package com.umc.bscene.domain.notification.port;

import com.umc.bscene.domain.notification.dto.response.BandInviteNotificationDetailResponse;

import java.util.Collection;
import java.util.Map;

public interface BandInvitePort {

    Map<Long, BandInviteNotificationDetailResponse> getBandInviteDetails(
            Long userId,
            Collection<Long> bandMemberIds
    );
}