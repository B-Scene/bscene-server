package com.umc.bscene.domain.stream.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 시청자 SSE emitter를 in-memory로 보관한다.
 * liveId -> userId -> emitter들 구조로, 같은 유저의 다중 탭(다중 연결)을 레퍼런스 카운팅한다.
 * 프레젠스(ZSet) 갱신·제거는 이 레지스트리가 아니라 ViewerSsePresence가 담당한다.
 */
public class ViewerSseRegistry {

    private final Map<Long, Map<Long, Set<SseEmitter>>> rooms = new ConcurrentHashMap<>();

    /**
     * @param onLastGone 해당 유저의 마지막 연결이 끊겼을 때 1회 실행(ZREM + broadcast 용도)
     */
    public SseEmitter register(Long liveId, Long userId, Runnable onLastGone) {
        // 0L => 타임아웃 없음. 수명은 하트비트/연결 끊김으로 관리한다.
        SseEmitter emitter = new SseEmitter(0L);

        rooms.computeIfAbsent(liveId, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
             .add(emitter);

        Runnable cleanup = () -> removeEmitter(liveId, userId, emitter, onLastGone);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError(e -> cleanup.run());
        return emitter;
    }

    private void removeEmitter(Long liveId, Long userId, SseEmitter emitter, Runnable onLastGone) {
        Map<Long, Set<SseEmitter>> users = rooms.get(liveId);
        if (users == null) return;
        Set<SseEmitter> set = users.get(userId);
        if (set == null) return;

        set.remove(emitter);
        if (set.isEmpty()) {
            users.remove(userId);
            if (users.isEmpty()) rooms.remove(liveId);
            onLastGone.run(); // 이 유저의 마지막 연결 → 프레젠스 제거 + 카운트 반영
        }
    }

    /** liveId 방의 모든 시청자에게 현재 카운트를 전송한다. */
    public void broadcast(Long liveId, long count) {
        Map<Long, Set<SseEmitter>> users = rooms.get(liveId);
        if (users == null) return;
        users.forEach((userId, set) -> set.forEach(emitter -> {
            try { emitter.send(SseEmitter.event().name("viewerCount").data(count)); }
            catch (IOException | IllegalStateException e) { emitter.completeWithError(e); } // → onError → cleanup
        }));
    }

    /**
     * 하트비트: 살아있는 연결에 ping을 보낸다.
     * 전송에 성공한(=살아있는) (liveId, userId)만 onAlive로 넘겨 프레젠스 score를 갱신하게 하고,
     * 전송에 실패한 연결은 completeWithError로 정리를 유발한다.
     */
    public void pingAndCollectAlive(BiConsumer<Long, Long> onAlive) {
        rooms.forEach((liveId, users) -> users.forEach((userId, set) -> {
            boolean alive = false;
            for (SseEmitter emitter : set) {
                try { emitter.send(SseEmitter.event().comment("ping")); alive = true; }
                catch (IOException | IllegalStateException e) { emitter.completeWithError(e); }
            }
            if (alive) onAlive.accept(liveId, userId);
        }));
    }
}
