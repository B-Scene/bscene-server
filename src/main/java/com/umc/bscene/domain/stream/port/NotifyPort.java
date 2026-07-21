package com.umc.bscene.domain.stream.port;

import com.umc.bscene.global.notification.message.PushMessage;

import java.util.List;

public interface NotifyPort {

    /**
     * 라이브 예약/시작 푸시 알림 발송을 요청하는 메소드입니다.
     * 팬아웃, 재시도, 개별 수신자 실패 처리는 어댑터에서 책임지고 처리해주세요.
     * 발송 결과에 따라 도메인이 취할 후속 동작이 없으므로 반환값은 없습니다. (실패는 어댑터에서 로깅)
     * @param receiverIds 알림을 수신할 사용자 ID 목록을 전달합니다.
     * @param message 발송할 푸시 메시지를 전달합니다.
     */
    void notify(List<Long> receiverIds, PushMessage message);
}
