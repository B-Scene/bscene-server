package com.umc.bscene.domain.band.port;

import com.umc.bscene.global.notification.message.PushMessage;

public interface NotifyPort {

    void notify(Long receiverId, PushMessage message);
}