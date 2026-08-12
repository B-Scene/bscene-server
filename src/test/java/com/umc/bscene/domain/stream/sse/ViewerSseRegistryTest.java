package com.umc.bscene.domain.stream.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.TestSseHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ViewerSseRegistry의 레퍼런스 카운팅·유저당 1연결 제한·브로드캐스트 내성 검증.
 * <p>
 * 성능 튜닝(자료구조 교체, 락 제거, 배치 전송 등) 시에도 아래 불변식은 유지되어야 한다:
 * - 한 유저의 마지막 연결이 끊길 때만 onLastGone이 정확히 1회 실행된다
 * - 죽은 emitter 하나가 브로드캐스트/하트비트 순회 전체를 중단시키지 않는다
 * - counted=false(송출자)는 ping은 받되 프레젠스 갱신 대상에서 제외된다
 */
class ViewerSseRegistryTest {

    private final ViewerSseRegistry registry = new ViewerSseRegistry();

    private static final long LIVE_A = 1L;
    private static final long LIVE_B = 2L;
    private static final long USER = 100L;

    /** 카운트 이벤트 payload에 실제 숫자가 실려 나갔는지 확인용 토큰. */
    private static String countToken(long count) {
        return String.valueOf(count);
    }

    @Nested
    @DisplayName("register - 레퍼런스 카운팅")
    class Register {

        @Test
        @DisplayName("구독 후 브로드캐스트하면 해당 emitter가 카운트를 받는다")
        void broadcastReachesRegisteredEmitter() {
            SseEmitter emitter = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler handler = TestSseHandler.attach(emitter);

            registry.broadcast(LIVE_A, 7);

            assertThat(handler.sentEvents()).hasSize(1);
            assertThat(handler.sentEvents().getFirst())
                    .contains("viewerCount")
                    .contains(countToken(7));
        }

        @Test
        @DisplayName("한 유저의 연결이 여러 개면 마지막 하나가 끊길 때까지 onLastGone이 실행되지 않는다")
        void onLastGoneOnlyAfterFinalConnectionCloses() {
            AtomicInteger lastGone = new AtomicInteger();

            SseEmitter first = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler firstHandler = TestSseHandler.attach(first);

            // 같은 방 재접속: 새 emitter가 먼저 등록되므로 presence가 비지 않는다
            SseEmitter second = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler secondHandler = TestSseHandler.attach(second);

            // 유저당 1연결 제한으로 이전 연결은 complete 처리된다
            assertThat(firstHandler.isCompleted()).isTrue();

            firstHandler.fireCompletion();
            assertThat(lastGone.get())
                    .as("아직 second 연결이 살아 있으므로 프레젠스를 지우면 안 된다")
                    .isZero();

            secondHandler.fireCompletion();
            assertThat(lastGone.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("onLastGone은 마지막 연결 종료 시 정확히 1회만 실행된다")
        void onLastGoneRunsExactlyOnce() {
            AtomicInteger lastGone = new AtomicInteger();

            SseEmitter emitter = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler handler = TestSseHandler.attach(emitter);

            handler.fireCompletion();
            // 컨테이너가 completion을 중복 통지해도 멱등해야 한다
            handler.fireCompletion();

            assertThat(lastGone.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("다른 방으로 이동하면 이전 방의 onLastGone이 실행된다")
        void movingToAnotherRoomReleasesPreviousRoom() {
            AtomicInteger goneFromA = new AtomicInteger();
            AtomicInteger goneFromB = new AtomicInteger();

            SseEmitter inA = registry.register(LIVE_A, USER, true, goneFromA::incrementAndGet);
            TestSseHandler handlerA = TestSseHandler.attach(inA);

            SseEmitter inB = registry.register(LIVE_B, USER, true, goneFromB::incrementAndGet);
            TestSseHandler.attach(inB);

            assertThat(handlerA.isCompleted())
                    .as("유저당 SSE 연결은 전역에서 1개")
                    .isTrue();

            handlerA.fireCompletion();

            assertThat(goneFromA.get()).isEqualTo(1);
            assertThat(goneFromB.get()).isZero();
        }

        @Test
        @DisplayName("이전 연결이 정리돼도 새 연결의 전역 인덱스는 덮어쓰이지 않는다")
        void staleCleanupDoesNotEvictCurrentConnection() {
            SseEmitter first = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler firstHandler = TestSseHandler.attach(first);

            SseEmitter second = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler secondHandler = TestSseHandler.attach(second);

            firstHandler.fireCompletion();

            // 새 연결이 여전히 방에 남아 브로드캐스트를 받아야 한다
            registry.broadcast(LIVE_A, 3);
            assertThat(secondHandler.sentEvents()).hasSize(1);

            // 세 번째 연결이 붙으면 두 번째가 끊긴다 = 전역 인덱스가 second를 가리키고 있었다는 뜻
            registry.register(LIVE_A, USER, true, () -> {
            });
            assertThat(secondHandler.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("타임아웃 콜백은 emitter를 완료시키고 프레젠스를 정리한다")
        void timeoutCompletesAndCleansUp() {
            AtomicInteger lastGone = new AtomicInteger();

            SseEmitter emitter = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler handler = TestSseHandler.attach(emitter);

            handler.fireTimeout();

            assertThat(handler.isCompleted()).isTrue();
            assertThat(lastGone.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("에러 콜백도 프레젠스를 정리한다")
        void errorCleansUp() {
            AtomicInteger lastGone = new AtomicInteger();

            SseEmitter emitter = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler handler = TestSseHandler.attach(emitter);

            handler.fireError(new IllegalStateException("boom"));

            assertThat(lastGone.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("등록된 적 없는 방/유저의 정리 호출은 조용히 무시된다")
        void cleanupOfUnknownRoomIsNoop() {
            AtomicInteger lastGone = new AtomicInteger();

            SseEmitter emitter = registry.register(LIVE_A, USER, true, lastGone::incrementAndGet);
            TestSseHandler handler = TestSseHandler.attach(emitter);

            handler.fireCompletion();   // 방이 비면서 rooms에서 제거됨
            handler.fireCompletion();   // 이미 사라진 방에 대한 재통지

            assertThat(lastGone.get()).isEqualTo(1);
            // 빈 방 브로드캐스트도 예외 없이 통과해야 한다
            registry.broadcast(LIVE_A, 0);
        }
    }

    @Nested
    @DisplayName("registerWatchOnly - 보기 전용 구독")
    class WatchOnly {

        @Test
        @DisplayName("보기 전용 구독도 카운트 브로드캐스트를 받는다")
        void watcherReceivesBroadcast() {
            SseEmitter watcher = registry.registerWatchOnly(LIVE_A);
            TestSseHandler handler = TestSseHandler.attach(watcher);

            registry.broadcast(LIVE_A, 42);

            assertThat(handler.sentEvents()).hasSize(1);
            assertThat(handler.sentEvents().getFirst()).contains(countToken(42));
        }

        @Test
        @DisplayName("보기 전용은 유저당 1연결 제한을 받지 않아 여러 방을 동시 구독할 수 있다")
        void watchOnlyAllowsMultipleConcurrentSubscriptions() {
            SseEmitter watcherA = registry.registerWatchOnly(LIVE_A);
            SseEmitter watcherB = registry.registerWatchOnly(LIVE_B);
            TestSseHandler handlerA = TestSseHandler.attach(watcherA);
            TestSseHandler handlerB = TestSseHandler.attach(watcherB);

            registry.broadcast(LIVE_A, 1);
            registry.broadcast(LIVE_B, 2);

            assertThat(handlerA.isCompleted()).isFalse();
            assertThat(handlerB.isCompleted()).isFalse();
            assertThat(handlerA.sentEvents()).hasSize(1);
            assertThat(handlerB.sentEvents()).hasSize(1);
        }

        @Test
        @DisplayName("연결이 끊긴 보기 전용 구독은 이후 브로드캐스트를 받지 않는다")
        void closedWatcherIsRemoved() {
            SseEmitter watcher = registry.registerWatchOnly(LIVE_A);
            TestSseHandler handler = TestSseHandler.attach(watcher);

            handler.fireCompletion();
            registry.broadcast(LIVE_A, 5);

            assertThat(handler.sentEvents()).isEmpty();
        }

        @Test
        @DisplayName("보기 전용 구독자는 시청자 수 프레젠스 갱신 대상이 아니다")
        void watcherIsNotReportedAsAlive() {
            SseEmitter watcher = registry.registerWatchOnly(LIVE_A);
            TestSseHandler handler = TestSseHandler.attach(watcher);

            List<Long> alive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> alive.add(userId));

            assertThat(handler.countOfEventsContaining("ping"))
                    .as("keep-alive ping은 받아야 한다")
                    .isEqualTo(1);
            assertThat(alive).isEmpty();
        }
    }

    @Nested
    @DisplayName("broadcast - 죽은 커넥션 내성")
    class Broadcast {

        @Test
        @DisplayName("죽은 emitter가 있어도 나머지 구독자에게 카운트가 전달된다")
        void deadEmitterDoesNotAbortBroadcast() {
            SseEmitter dead = registry.register(LIVE_A, 1L, true, () -> {
            });
            SseEmitter alive = registry.register(LIVE_A, 2L, true, () -> {
            });
            SseEmitter watcher = registry.registerWatchOnly(LIVE_A);

            TestSseHandler deadHandler = TestSseHandler.attach(dead);
            TestSseHandler aliveHandler = TestSseHandler.attach(alive);
            TestSseHandler watcherHandler = TestSseHandler.attach(watcher);

            deadHandler.failSubsequentSends();

            registry.broadcast(LIVE_A, 9);

            assertThat(aliveHandler.sentEvents()).hasSize(1);
            assertThat(watcherHandler.sentEvents()).hasSize(1);
            assertThat(deadHandler.completedWithErrors())
                    .as("죽은 emitter는 completeWithError로 정리 유도")
                    .hasSize(1);
        }

        @Test
        @DisplayName("죽은 보기 전용 구독자도 순회를 중단시키지 않는다")
        void deadWatcherDoesNotAbortBroadcast() {
            SseEmitter deadWatcher = registry.registerWatchOnly(LIVE_A);
            SseEmitter liveWatcher = registry.registerWatchOnly(LIVE_A);

            TestSseHandler deadHandler = TestSseHandler.attach(deadWatcher);
            TestSseHandler liveHandler = TestSseHandler.attach(liveWatcher);

            deadHandler.failSubsequentSends();

            registry.broadcast(LIVE_A, 4);

            assertThat(liveHandler.sentEvents()).hasSize(1);
            assertThat(deadHandler.completedWithErrors()).hasSize(1);
        }

        @Test
        @DisplayName("구독자가 없는 방 브로드캐스트는 예외 없이 통과한다")
        void broadcastToEmptyRoomIsSafe() {
            registry.broadcast(999L, 0);
        }
    }

    @Nested
    @DisplayName("pingAndCollectAlive - 하트비트")
    class Heartbeat {

        @Test
        @DisplayName("살아있는 유저는 counted 여부와 함께 보고된다 (송출자는 counted=false)")
        void aliveUsersAreReportedWithCountedFlag() {
            SseEmitter broadcaster = registry.register(LIVE_A, 10L, false, () -> {
            });
            SseEmitter listener = registry.register(LIVE_A, 20L, true, () -> {
            });
            TestSseHandler broadcasterHandler = TestSseHandler.attach(broadcaster);
            TestSseHandler listenerHandler = TestSseHandler.attach(listener);

            List<Long> countedAlive = new ArrayList<>();
            List<Long> uncountedAlive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> {
                if (counted) countedAlive.add(userId);
                else uncountedAlive.add(userId);
            });

            // 시청자 수 프레젠스는 counted=true(청취자)만, 진행자 프레젠스 갱신을 위해 송출자도 counted=false로 보고된다
            assertThat(countedAlive).containsExactly(20L);
            assertThat(uncountedAlive).containsExactly(10L);
            assertThat(broadcasterHandler.countOfEventsContaining("ping"))
                    .as("송출자도 keep-alive ping은 받는다")
                    .isEqualTo(1);
            assertThat(listenerHandler.countOfEventsContaining("ping")).isEqualTo(1);
        }

        @Test
        @DisplayName("모든 연결이 죽은 유저는 살아있음으로 보고되지 않는다")
        void deadUserIsNotReportedAlive() {
            SseEmitter emitter = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler handler = TestSseHandler.attach(emitter);
            handler.failSubsequentSends();

            List<Long> alive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> alive.add(userId));

            assertThat(alive).isEmpty();
            assertThat(handler.completedWithErrors()).hasSize(1);
        }

        @Test
        @DisplayName("연결 하나만 살아 있어도 유저는 살아있음으로 보고된다")
        void userWithOneLiveConnectionIsReported() {
            SseEmitter first = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler firstHandler = TestSseHandler.attach(first);
            SseEmitter second = registry.register(LIVE_A, USER, true, () -> {
            });
            TestSseHandler.attach(second);

            firstHandler.failSubsequentSends();

            List<Long> alive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> alive.add(userId));

            assertThat(alive).containsExactly(USER);
        }

        @Test
        @DisplayName("죽은 유저가 섞여 있어도 하트비트 순회가 중단되지 않는다")
        void deadUserDoesNotAbortHeartbeat() {
            SseEmitter dead = registry.register(LIVE_A, 1L, true, () -> {
            });
            SseEmitter alive = registry.register(LIVE_A, 2L, true, () -> {
            });
            TestSseHandler deadHandler = TestSseHandler.attach(dead);
            TestSseHandler.attach(alive);

            deadHandler.failSubsequentSends();

            List<Long> reported = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> reported.add(userId));

            assertThat(reported).containsExactly(2L);
        }

        @Test
        @DisplayName("구독자가 없으면 아무도 보고되지 않는다")
        void emptyRegistryReportsNobody() {
            List<Long> alive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> alive.add(userId));

            assertThat(alive).isEmpty();
        }
    }

    @Nested
    @DisplayName("동시성")
    class Concurrency {

        @Test
        @DisplayName("서로 다른 유저가 동시에 구독·해제해도 onLastGone은 유저당 정확히 1회")
        void concurrentRegisterAndReleaseKeepsRefCountExact() throws Exception {
            int userCount = 200;
            AtomicInteger lastGone = new AtomicInteger();
            ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(userCount);

            try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
                for (int i = 0; i < userCount; i++) {
                    long userId = 1000L + i;
                    pool.execute(() -> {
                        try {
                            start.await();
                            SseEmitter emitter = registry.register(
                                    LIVE_A, userId, true, lastGone::incrementAndGet);
                            TestSseHandler handler = TestSseHandler.attach(emitter);
                            registry.broadcast(LIVE_A, 1);
                            handler.fireCompletion();
                        } catch (Throwable t) {
                            failures.add(t);
                        } finally {
                            done.countDown();
                        }
                    });
                }

                start.countDown();
                assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            }

            assertThat(failures).isEmpty();
            assertThat(lastGone.get()).isEqualTo(userCount);

            // 모든 연결이 정리되면 방도 비어 브로드캐스트가 아무에게도 가지 않는다
            List<Long> alive = new ArrayList<>();
            registry.pingAndCollectAlive((liveId, userId, counted) -> alive.add(userId));
            assertThat(alive).isEmpty();
        }

        @Test
        @DisplayName("브로드캐스트 중 다른 스레드가 구독을 해제해도 예외가 나지 않는다")
        void broadcastIsSafeWhileConnectionsClose() throws Exception {
            int userCount = 100;
            List<TestSseHandler> handlers = new ArrayList<>();

            for (int i = 0; i < userCount; i++) {
                SseEmitter emitter = registry.register(LIVE_A, 2000L + i, true, () -> {
                });
                handlers.add(TestSseHandler.attach(emitter));
            }

            ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 50; i++) registry.broadcast(LIVE_A, i);
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
                pool.execute(() -> {
                    try {
                        start.await();
                        handlers.forEach(TestSseHandler::fireCompletion);
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });

                start.countDown();
                assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            }

            assertThat(failures).isEmpty();
        }
    }

    @Nested
    @DisplayName("sendToUsers - 진행자 타겟 전송")
    class SendToUsers {

        private static final String EVENT = "coPublisherJoined";

        @Test
        @DisplayName("지정한 유저의 emitter에만 이벤트가 전송되고, 비대상 유저는 받지 못한다")
        void deliversOnlyToTargetUsers() {
            SseEmitter target = registry.register(LIVE_A, 100L, true, () -> { });
            TestSseHandler targetHandler = TestSseHandler.attach(target);
            SseEmitter other = registry.register(LIVE_A, 200L, true, () -> { });
            TestSseHandler otherHandler = TestSseHandler.attach(other);

            registry.sendToUsers(LIVE_A, List.of(100L), EVENT, "payload-1");

            assertThat(targetHandler.sentEvents()).hasSize(1);
            assertThat(targetHandler.sentEvents().getFirst())
                    .contains(EVENT)
                    .contains("payload-1");
            assertThat(otherHandler.sentEvents())
                    .as("[공격] 비대상 유저(청취자 연결)에 멤버 path 정보가 새면 안 된다")
                    .isEmpty();
        }

        @Test
        @DisplayName("[공격] 보기 전용(watchOnly) 구독자는 타겟 이벤트를 절대 받지 못한다")
        void watchersNeverReceiveTargetedEvents() {
            SseEmitter watcher = registry.registerWatchOnly(LIVE_A);
            TestSseHandler watcherHandler = TestSseHandler.attach(watcher);
            SseEmitter target = registry.register(LIVE_A, 100L, true, () -> { });
            TestSseHandler targetHandler = TestSseHandler.attach(target);

            registry.sendToUsers(LIVE_A, List.of(100L), EVENT, "payload-1");

            assertThat(targetHandler.sentEvents()).hasSize(1);
            assertThat(watcherHandler.sentEvents()).isEmpty();
        }

        @Test
        @DisplayName("[공격] 다른 방에 접속한 같은 userId의 연결에는 전송되지 않는다")
        void otherRoomConnectionOfSameUserNotDelivered() {
            SseEmitter otherRoom = registry.register(LIVE_B, 100L, true, () -> { });
            TestSseHandler otherRoomHandler = TestSseHandler.attach(otherRoom);

            registry.sendToUsers(LIVE_A, List.of(100L), EVENT, "payload-1");

            assertThat(otherRoomHandler.sentEvents()).isEmpty();
        }

        @Test
        @DisplayName("죽은 emitter가 있어도 나머지 대상 전송이 중단되지 않는다")
        void deadEmitterDoesNotStopDelivery() {
            SseEmitter dead = registry.register(LIVE_A, 100L, true, () -> { });
            TestSseHandler deadHandler = TestSseHandler.attach(dead);
            deadHandler.failSubsequentSends();
            SseEmitter alive = registry.register(LIVE_A, 200L, true, () -> { });
            TestSseHandler aliveHandler = TestSseHandler.attach(alive);

            registry.sendToUsers(LIVE_A, List.of(100L, 200L), EVENT, "p");

            assertThat(aliveHandler.sentEvents()).hasSize(1);
        }

        @Test
        @DisplayName("없는 방이나 미접속 유저 대상이면 예외 없이 조용히 무시된다")
        void missingRoomOrUserIsNoop() {
            assertThatCode(() -> registry.sendToUsers(999L, List.of(100L), EVENT, "p"))
                    .doesNotThrowAnyException();

            SseEmitter connected = registry.register(LIVE_A, 100L, true, () -> { });
            TestSseHandler connectedHandler = TestSseHandler.attach(connected);
            assertThatCode(() -> registry.sendToUsers(LIVE_A, List.of(777L), EVENT, "p"))
                    .doesNotThrowAnyException();
            assertThat(connectedHandler.sentEvents()).isEmpty();
        }
    }
}
