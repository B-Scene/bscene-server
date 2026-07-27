package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * SseEmitter 테스트용 Handler 더블.
 * <p>
 * ResponseBodyEmitter#initialize(Handler)와 Handler 인터페이스가 패키지 전용이라
 * 이 클래스는 스프링과 같은 패키지에 위치한다.
 * <p>
 * 프로덕션 코드가 emitter를 내부에서 생성(new SseEmitter(0L))하므로 mock 주입이 불가능하다.
 * 대신 실제 emitter에 이 핸들러를 붙여 전송 내역을 관찰하고, 연결 끊김(completion/timeout/error)을
 * 명시적으로 발화시켜 레지스트리의 정리 로직을 검증한다.
 */
public final class TestSseHandler implements ResponseBodyEmitter.Handler {

    private final List<String> sentEvents = new CopyOnWriteArrayList<>();
    private final List<Throwable> completedWithErrors = new CopyOnWriteArrayList<>();

    private volatile boolean failSend;
    private volatile boolean completed;

    private volatile Runnable timeoutCallback;
    private volatile Runnable completionCallback;
    private volatile Consumer<Throwable> errorCallback;

    /** 실제 emitter에 이 핸들러를 연결한다. 연결 이후의 send가 관찰 대상이 된다. */
    public static TestSseHandler attach(SseEmitter emitter) {
        TestSseHandler handler = new TestSseHandler();
        try {
            emitter.initialize(handler);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return handler;
    }

    /** 이후의 모든 send를 IOException으로 실패시킨다(끊긴 커넥션 모사). */
    public void failSubsequentSends() {
        this.failSend = true;
    }

    /** 컨테이너가 비동기 요청을 완료했을 때처럼 onCompletion 콜백을 발화시킨다. */
    public void fireCompletion() {
        Runnable callback = this.completionCallback;
        if (callback != null) callback.run();
    }

    /** 컨테이너 타임아웃처럼 onTimeout 콜백을 발화시킨다. */
    public void fireTimeout() {
        Runnable callback = this.timeoutCallback;
        if (callback != null) callback.run();
    }

    /** 컨테이너 에러처럼 onError 콜백을 발화시킨다. */
    public void fireError(Throwable throwable) {
        Consumer<Throwable> callback = this.errorCallback;
        if (callback != null) callback.accept(throwable);
    }

    /** send 1회당 1개의 문자열. 이벤트 이름·데이터가 이어붙은 형태. */
    public List<String> sentEvents() {
        return List.copyOf(sentEvents);
    }

    public long countOfEventsContaining(String token) {
        return sentEvents.stream().filter(e -> e.contains(token)).count();
    }

    public List<Throwable> completedWithErrors() {
        return List.copyOf(completedWithErrors);
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void send(Object data, MediaType mediaType) throws IOException {
        if (failSend) throw new IOException("connection reset by peer (test)");
        sentEvents.add(String.valueOf(data));
    }

    @Override
    public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
        if (failSend) throw new IOException("connection reset by peer (test)");

        // SseEmitter는 "event:", 이름, "data:", 값 등을 여러 조각으로 나눠 보낸다. 한 번의 send를 한 문자열로 합친다.
        List<String> parts = new ArrayList<>();
        for (ResponseBodyEmitter.DataWithMediaType item : items)
            parts.add(String.valueOf(item.getData()));

        sentEvents.add(parts.stream().collect(Collectors.joining()));
    }

    @Override
    public void complete() {
        this.completed = true;
    }

    @Override
    public void completeWithError(Throwable failure) {
        this.completed = true;
        completedWithErrors.add(failure);
    }

    @Override
    public void onTimeout(Runnable callback) {
        this.timeoutCallback = callback;
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
        this.errorCallback = callback;
    }

    @Override
    public void onCompletion(Runnable callback) {
        this.completionCallback = callback;
    }
}
