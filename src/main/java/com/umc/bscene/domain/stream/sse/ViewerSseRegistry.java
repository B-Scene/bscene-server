package com.umc.bscene.domain.stream.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ViewerSseRegistry {

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long liveId, Runnable onGone) {

        // 0L => 타임 아웃 X, 하트비트로 관리
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(liveId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable cleanup = () -> {
            Set<SseEmitter> set = emitters.get(liveId);
            if (set != null) set.remove(emitter);
            onGone.run();   // ZREM + broadcast
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError(e -> cleanup.run());
        return emitter;
    }

    public void broadcast(Long liveId, long count) {
        Set<SseEmitter> set = emitters.get(liveId);
        if (set == null) return;
        for (SseEmitter emitter : set) {
            try { emitter.send(SseEmitter.event().name("viewerCount")); }
            catch (IOException e) { set.remove(emitter); }  // 죽은 연결 제거
        }
    }

    public Collection<SseEmitter> all() {
        return emitters.values().stream()
                .flatMap(Set::stream)
                .toList();
    }
}
