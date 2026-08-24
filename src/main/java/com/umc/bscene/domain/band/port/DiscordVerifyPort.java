package com.umc.bscene.domain.band.port;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;

public interface DiscordVerifyPort {

    // 전송 성공 시 Discord 메시지 ID, 실패(봇 미설정 포함) 시 null
    String sendVerifyMessage(BandVerifyMessage message);

    // 검수 중 밴드 정보가 수정됐을 때 기존 검수 메시지의 임베드를 새 내용으로 교체
    void updateVerifyMessage(String discordMessageId, BandVerifyMessage message);
}
