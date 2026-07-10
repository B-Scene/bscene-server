package com.umc.bscene.domain.stream.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 시청자 SSE emitter를 in-memory로 보관한다.
 * liveId -> userId -> 연결 상태(Presence) 구조로, 같은 유저의 다중 탭(다중 연결)을 레퍼런스 카운팅한다.
 * 프레젠스(ZSet) 갱신·제거는 이 레지스트리가 아니라 ViewerSsePresence가 담당한다.
 */
public class ViewerSseRegistry {

    private final Map<Long, Map<Long, Presence>> rooms = new ConcurrentHashMap<>();

    // 유저당 SSE 연결을 전역에서 1개로 제한하기 위한 인덱스: userId -> 현재 유효한 단일 emitter.
    // (방 구조와 별개로, 다른 방으로 이동하거나 다중 탭으로 재접속할 때 이전 연결을 끊기 위함)
    private final Map<Long, SseEmitter> userLatest = new ConcurrentHashMap<>();

    /** 한 유저의 SSE 연결들과, 그 유저를 시청자 수에 포함할지 여부(counted). */
    private static final class Presence {
        final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
        final boolean counted; // 송출자는 false: 브로드캐스트는 받되 시청자 수엔 미포함
        Presence(boolean counted) { this.counted = counted; }
    }

    /**
     * @param counted    시청자 수에 포함할지 여부(송출자면 false)
     * @param onLastGone 이 유저의 마지막 연결이 끊겼을 때 1회 실행(ZREM + broadcast 용도)
     */
    public SseEmitter register(Long liveId, Long userId, boolean counted, Runnable onLastGone) {
        // 0L => 타임아웃 없음. 수명은 하트비트/연결 끊김으로 관리한다.
        SseEmitter emitter = new SseEmitter(0L);

        // 새 연결을 먼저 방 구조에 등록한다.
        // (기존 연결을 끊기 전에 새 연결을 넣어야, 같은 방 재접속 시 기존 emitter의
        //  complete -> removeEmitter가 presence를 비워 onLastGone을 잘못 호출하는 것을 막는다.)
        rooms.computeIfAbsent(liveId, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(userId, k -> new Presence(counted))
             .emitters.add(emitter);

        Runnable cleanup = () -> removeEmitter(liveId, userId, emitter, onLastGone);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError(e -> cleanup.run());

        // 유저당 SSE 연결은 전역에서 1개만 유지한다: 기존 연결이 있으면 닫는다.
        // - 같은 방 재접속: 위에서 새 emitter를 이미 넣었으므로 presence가 비지 않아 카운트가 유지된다.
        // - 다른 방으로 이동: 이전 방 presence가 비면서 onLastGone이 돌아 이전 방 시청자에서 정상 제거된다.
        SseEmitter previous = userLatest.put(userId, emitter);
        if (previous != null && previous != emitter) {
            try { previous.complete(); } catch (Exception ignored) { /* 이미 끊긴 연결이면 무시 */ }
        }

        return emitter;
    }

    private void removeEmitter(Long liveId, Long userId, SseEmitter emitter, Runnable onLastGone) {
        // 이 emitter가 현재 유효 연결일 때만 전역 인덱스에서 제거한다.
        // (이미 새 연결이 replace 했다면 map의 값이 다르므로 지우지 않는다.)
        userLatest.remove(userId, emitter);

        Map<Long, Presence> users = rooms.get(liveId);
        if (users == null) return;
        Presence presence = users.get(userId);
        if (presence == null) return;

        presence.emitters.remove(emitter);
        if (presence.emitters.isEmpty()) {
            users.remove(userId);
            if (users.isEmpty()) rooms.remove(liveId);
            onLastGone.run(); // 이 유저의 마지막 연결 → 프레젠스 제거 + 카운트 반영
        }
    }

    /** liveId 방의 모든 연결(송출자 포함)에 현재 카운트를 전송한다. */
    public void broadcast(Long liveId, long count) {
        Map<Long, Presence> users = rooms.get(liveId);
        if (users == null) return;
        users.forEach((userId, presence) -> presence.emitters.forEach(emitter -> {
            try { emitter.send(SseEmitter.event().name("viewerCount").data(count)); }
            catch (IOException | IllegalStateException e) { emitter.completeWithError(e); } // → onError → cleanup
        }));
    }

    /**
     * 하트비트: 모든 연결에 ping을 보내(끊긴 연결 정리를 유발) keep-alive 한다.
     * 단 시청자 수에 포함되는(counted) 유저만 onAlive로 넘겨 프레젠스 score를 갱신하게 한다.
     * (송출자는 ping은 받아 연결이 유지되지만 프레젠스엔 들어가지 않는다.)
     */
    public void pingAndCollectAlive(BiConsumer<Long, Long> onAlive) {
        rooms.forEach((liveId, users) -> users.forEach((userId, presence) -> {
            boolean alive = false;
            for (SseEmitter emitter : presence.emitters) {
                try { emitter.send(SseEmitter.event().comment("ping")); alive = true; }
                catch (IOException | IllegalStateException e) { emitter.completeWithError(e); }
            }
            if (alive && presence.counted) onAlive.accept(liveId, userId);
        }));
    }
}
