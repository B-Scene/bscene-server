package com.umc.bscene.domain.user.port;

import com.umc.bscene.global.notification.message.PushMessage;

import java.util.List;

public interface NotifyPort {

    void notify(List<Long> receiverIds, PushMessage message);
}