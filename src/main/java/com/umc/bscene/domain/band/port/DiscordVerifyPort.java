package com.umc.bscene.domain.band.port;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;

public interface DiscordVerifyPort {

    // 전송 성공 시 Discord 메시지 ID, 실패(봇 미설정 포함) 시 null
    String sendVerifyMessage(BandVerifyMessage message);
}
