## 0. 먼저, 큰 그림부터

워크북 버전은 **"동작하는 최소한의 공통 모듈"** 을 빠르게 만드는 데 초점이 있습니다. 그래서 응답도 클래스 하나(`ApiResponse<T>`)로 성공/실패를 모두 처리하고, 예외 핸들러도 꼭 필요한 몇 개만 둡니다. 처음 배울 때는 이게 정답입니다. 한눈에 흐름이 보이거든요.

bscene 버전은 거기서 한 발 더 나아가, **"규모가 커져도 깨지지 않는 구조"** 를 목표로 합니다. 응답을 성공/실패로 나누고, 프레임워크 타입에 대한 의존을 끊고, 예외를 상황별로 잘게 나눕니다. 코드 양은 늘지만, 각 클래스가 하는 일이 분명해집니다.

핵심 한 줄 요약:

> **워크북 = "하나로 다 처리한다"** → **bscene = "역할별로 나누고, 의존 방향을 정리한다"**

아래 표가 전체 변화의 지도입니다. 이후 각 항목을 하나씩 풉니다.

| 영역 | 워크북(study) | bscene | 적용한 원칙 |
|---|---|---|---|
| 패키지 이름 | `apiPayload` | `response` / `exception` / `config` / `constant` 로 분리 | 관심사 분리(SoC) |
| 응답 클래스 | `ApiResponse<T>` 하나로 성공·실패 | `ApiResponse`(부모) + `SuccessResponse` / `ErrorResponse`(자식) | 상속·다형성, SRP |
| 응답 코드 status 타입 | `HttpStatus` (스프링 타입) | `int` (StaticValue 상수) | DIP, 프레임워크 의존 분리 |
| HTTP 상태 코드 관리 | `HttpStatus` enum 직접 사용 | `StaticValue` 상수 클래스 | 상수 캡슐화, 인스턴스화 방어 |
| 예외 핸들러 | 3개 (필수만) | 9개 (상황별 세분화) + 로깅 | 명시적 예외 처리, 관측성 |
| 정적 팩토리 메서드 | `onComplete` / `onFailure` | `ok` / `created` / `accepted` / `from` / `of` 등 | 의미 있는 생성자, 가독성 |
| `BaseEntity` | `createdAt`, `updatedAt`, `deletedAt` | `createdAt(updatable=false)`, `updatedAt` | 불변성 표현 |
 

---

## 1. 응답을 하나로? 둘로? — 상속과 다형성

### 워크북 (study)

성공이든 실패든 `ApiResponse<T>` **클래스 하나**로 처리합니다.

```java
@JsonPropertyOrder({"isSuccess", "code", "message", "result", "timeStamp"})
public class ApiResponse<T> {
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private T result;
    private final String timeStamp = ...;
 
    public static <T> ApiResponse<T> onFailure(BaseResponseCode code, T result) { ... }
    public static <T> ApiResponse<T> onComplete(BaseResponseCode code, T result) { ... }
}
```

`onComplete`(성공)와 `onFailure`(실패)라는 메서드 이름으로 구분할 뿐, 실제 타입은 똑같습니다. 단순하고 직관적입니다.

### bscene

응답을 **부모 1개 + 자식 2개**로 나눴습니다.

```java
// 공통 뼈대 (성공/실패가 항상 갖는 것)
public class ApiResponse {
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final String timeStamp = ...;
}
 
// 성공 전용
public class SuccessResponse<T> extends ApiResponse {
    private final int status;
    private final T result;
 
    public static <T> SuccessResponse<T> ok(T result) { ... }
    public static <T> SuccessResponse<T> created(T result) { ... }
}
 
// 실패 전용
public class ErrorResponse<T> extends ApiResponse {
    private final int status;
    private final T result;
 
    public static <T> ErrorResponse<T> from(BaseResponseCode code) { ... }
}
```

**왜 이렇게 했나?**

- **단일 책임 원칙(SRP):** "성공 응답을 만드는 일"과 "실패 응답을 만드는 일"은 서로 다른 관심사입니다. 한 클래스에 다 넣으면, 성공에만 필요한 변경이 실패 코드에까지 영향을 줄 수 있습니다. 나눠두면 각자 독립적으로 진화합니다.
- **공통 코드 재사용(상속):** `isSuccess`, `code`, `message`, `timeStamp`는 성공이든 실패든 항상 같습니다. 이걸 부모 `ApiResponse`에 두고 `extends`로 물려받으면 중복이 사라집니다.
- **확장 용이성(OCP):** 나중에 "비동기 처리 중" 같은 새로운 응답 종류가 필요하면, 기존 코드를 건드리지 않고 `ApiResponse`를 상속한 새 클래스를 추가하면 됩니다.
> 💡 **처음 프로젝트라면 이 점을 주의하세요.**
> 상속을 쓰면 "공통은 부모, 특수는 자식"이라는 그림이 머릿속에 그려져야 합니다. 부모(`ApiResponse`)만 봐도 "모든 응답의 공통 약속"을 알 수 있고, 자식만 봐도 "이 응답만의 특징"을 알 수 있는 게 좋은 상속입니다. 반대로 공통이 아닌 걸 부모에 억지로 올리면 오히려 더 복잡해지니, "정말 모두가 공유하는가?"를 기준으로 판단하세요.
 
---

## 2. `status` 타입이 `HttpStatus` → `int`로 바뀐 이유 (DIP)

이게 bscene에서 **가장 의도가 분명한 변경**이고, 코드에도 주석으로 이유를 남겨놨습니다.

### 워크북 (study)

```java
public interface BaseResponseCode {
    HttpStatus getStatus();   // 스프링이 제공하는 타입
    String getCode();
    String getMessage();
}
```

### bscene

```java
public interface BaseResponseCode {
    /*
     * HttpStatus의 경우, HTTP 프로토콜에서 사용
     * 통신 단에서 처리되는 타입이므로, 애플리케이션 내에서 사용하기 부적합(DIP 위배)
     * 따라서, int 타입으로 수정
     */
    int getStatus();
    String getCode();
    String getMessage();
}
```

**무슨 뜻일까?**

`HttpStatus`는 스프링 웹(프레임워크)이 주는 타입입니다. 우리 애플리케이션의 "응답 코드"라는 개념이 스프링 웹 타입을 직접 들고 있으면, **우리 핵심 로직이 특정 프레임워크에 묶여버립니다.** 나중에 웹 프레임워크를 바꾸거나, 같은 코드 정의를 웹이 아닌 곳(배치, 메시지 큐 등)에서 재사용하려 할 때 발목을 잡습니다.

그래서 bscene은 status를 **순수한 `int`** 로 바꿨습니다. `200`, `404` 같은 숫자는 HTTP 표준이지 스프링의 소유물이 아니니까요. 이게 **의존 역전 원칙(DIP)** 의 정신입니다: *"고수준 정책(내 응답 코드)이 저수준 도구(스프링 타입)에 의존하지 않게 한다."*

> 💡 **처음엔 과하게 느껴질 수 있습니다.**
> "어차피 스프링 쓸 건데 왜?"라고 생각할 수 있어요. 맞습니다, 작은 프로젝트에선 체감이 안 됩니다. 다만 이런 습관은 "내 도메인 코드가 외부 라이브러리에 새어들어가지 않게 한다"는 감각을 길러주고, 이 감각이 큰 프로젝트에서 큰 차이를 만듭니다. 지금은 "이런 관점이 있구나" 정도로 익혀두면 충분합니다.
 
---

## 3. `StaticValue` — HTTP 상태 코드를 상수로 모으기

DIP 변경(2번)과 짝을 이루는 변경입니다. status를 `int`로 바꿨더니, 코드 안에 `200`, `404` 같은 **매직 넘버**가 흩어질 위험이 생깁니다. bscene은 이걸 한곳에 모았습니다.

```java
public class StaticValue {
    /* 2xx */
    public static final int OK = 200;
    public static final int CREATED = 201;
    ...
    /* 4xx */
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    ...
 
    // 외부에서 생성할 수 없게 private 생성자로 방어
    private StaticValue() {}
}
```

그리고 `GeneralSuccessCode` 등에서 `import static ...StaticValue.*;`로 가져다 씁니다.

**눈여겨볼 점 두 가지:**

1. **상수 캡슐화:** 숫자 `404` 대신 이름 `NOT_FOUND`를 쓰니 의미가 드러나고, 값이 바뀌어도 한 곳만 고치면 됩니다.
2. **`private` 생성자:** `StaticValue`는 상수 모음일 뿐 인스턴스를 만들 이유가 없습니다. 생성자를 `private`으로 막아 `new StaticValue()`를 원천 차단합니다. 이건 "이 클래스를 어떻게 써야 하는지"를 **코드로 강제하는** 작은 OOP 기법입니다. 의도를 주석이 아니라 구조로 표현하는 거죠.
---

## 4. 정적 팩토리 메서드 — 더 많이, 더 의미 있게

### 워크북 (study)

```java
ApiResponse.onComplete(code, result);  // 성공
ApiResponse.onFailure(code, result);   // 실패
```

두 개로 끝. 깔끔합니다.

### bscene

상황별로 의미가 분명한 이름의 메서드를 여럿 제공합니다.

```java
SuccessResponse.ok(result);        // 200
SuccessResponse.created(result);   // 201
SuccessResponse.accepted();        // 202
SuccessResponse.empty(result);     // 204
 
ErrorResponse.from(errorCode);             // 코드만으로 에러 응답
ErrorResponse.of(errorCode, message);      // 메시지 덮어쓰기
ErrorResponse.of(errorCode, data);         // 부가 데이터 포함
```

**왜 좋은가?**

컨트롤러에서 `return SuccessResponse.created(data);`라고 쓰면, 읽는 사람이 **HTTP 201을 내려준다는 의도**를 바로 압니다. 생성자에 `true`, `201` 같은 값을 직접 넘기는 것보다 훨씬 읽기 좋고 실수가 적습니다. 이것이 *정적 팩토리 메서드*의 핵심 장점입니다: **"무엇을 만드는지"를 이름으로 말한다.**

> 💡 **주의:** 메서드가 많다고 무조건 좋은 건 아닙니다. 팀에서 실제로 자주 쓰는 케이스 위주로 만들어야 합니다. 안 쓰는 메서드가 쌓이면 그것도 부담입니다. bscene은 자주 쓰는 2xx/에러 케이스를 골라 만들었다고 보면 됩니다.
 
---

## 5. 예외 핸들러 — 3개에서 9개로, 그리고 로깅

### 워크북 (study) — `GeneralExceptionAdvice`

핵심 3개만 처리합니다.

- `MethodArgumentNotValidException` (검증 실패)
- `BaseException` (비즈니스 예외)
- `Exception` (그 외 전부 → 500)
### bscene — `GlobalExceptionHandler`

상황을 잘게 나눠 9개를 처리하고, 각 핸들러에 **`@Slf4j` 로깅**을 답니다.

- `MethodArgumentNotValidException` — @RequestBody 검증 실패
- `BindException` — 쿼리/폼 파라미터 검증 실패
- `HttpMessageNotReadableException` — JSON 파싱 실패
- `MethodArgumentTypeMismatchException` — 파라미터 타입 변환 실패
- `MissingServletRequestParameterException` — 필수 파라미터 누락
- `MissingServletRequestPartException` — 멀티파트 누락
- `HttpRequestMethodNotSupportedException` — 잘못된 HTTP 메서드
- `NoHandlerFoundException` / `NoResourceFoundException` — 없는 엔드포인트
- `BaseException` — 비즈니스 예외
  **왜 이렇게까지 나누나?**

워크북의 `Exception` 한 방(catch-all)은 "뭔가 터졌다"는 것만 알려줄 뿐, **클라이언트 입장에서 무엇이 잘못됐는지** 알기 어렵습니다. bscene처럼 나누면 "파라미터 타입이 틀렸다", "필수 값이 빠졌다"처럼 **구체적이고 일관된 에러 응답**을 줄 수 있어, 프론트엔드와의 협업이 훨씬 수월해집니다. 각 핸들러가 적절한 `GeneralErrorCode`를 매핑하니, 에러 코드 체계도 풍부해집니다(`COMMON_400_3`, `COMMON_405_1` 등).

`log.error(...)`도 중요합니다. 운영 중 문제가 생겼을 때 로그가 없으면 원인 추적이 사실상 불가능합니다. 예외를 "응답으로 변환만" 하지 말고 "기록도 남긴다"는 게 실무 감각입니다.

> 💡 **한 가지 트레이드오프:** bscene에는 워크북의 `@ExceptionHandler(Exception.class)` 같은 **최종 방어선(catch-all)이 없습니다.** 위에 나열되지 않은 예상치 못한 예외는 스프링 기본 처리로 빠집니다. 처음 프로젝트라면 "예상 못 한 예외도 우리 포맷으로 500을 내려줄지"를 팀과 정하고, 필요하면 `Exception` 핸들러를 하나 추가하는 걸 권합니다. **세분화하되, 마지막 그물망은 남겨두는 것**이 보통 안전합니다.
 
---

## 6. `BaseEntity` — 작지만 의미 있는 차이

### 워크북 (study)

```java
@CreatedDate @Column(nullable = false)
private LocalDateTime createdAt;
 
@LastModifiedDate @Column(nullable = false)
private LocalDateTime updatedAt;
 
private LocalDateTime deletedAt;   // 소프트 삭제용
```

### bscene

```java
@CreatedDate @Column(nullable = false, updatable = false)   // ← updatable=false
private LocalDateTime createdAt;
 
@LastModifiedDate @Column(nullable = false)
private LocalDateTime updatedAt;
// deletedAt 없음
```

- **`updatable = false`:** "생성 시각은 한 번 정해지면 절대 바뀌지 않는다"는 규칙을 DB 레벨에서 강제합니다. 불변이어야 할 값을 불변으로 표현하는, 작지만 좋은 습관입니다.
- **`deletedAt` 유무:** 이건 설계 우열이 아니라 **정책 차이**입니다. 워크북 버전은 소프트 삭제(실제로 지우지 않고 삭제 표시)를 염두에 뒀고, bscene은 해당 시점에 그 정책이 없었던 것뿐입니다. 소프트 삭제가 필요하면 bscene에도 추가하면 됩니다.
---

## 7. 그래서, 무엇을 가져가면 될까

처음 프로젝트를 하는 입장에서 **꼭 이해하고 넘어갈 것**과 **상황 봐서 도입할 것**을 나눠봤습니다.

**바로 체화하면 좋은 것**
- 응답을 성공/실패로 나누고 공통은 부모로 올리는 감각 (1번)
- 정적 팩토리 메서드로 "의도를 이름으로" 드러내기 (4번)
- 예외를 상황별로 나누고 로그를 남기기 (5번) — 단, 최종 방어선은 남기기
  **개념을 이해하되 과하지 않게 도입할 것**
- status를 `int`로 빼는 DIP 적용 (2·3번): 왜 좋은지는 알아두되, 프로젝트 규모와 팀 합의에 맞춰 결정
- 상수 클래스 + `private` 생성자: 매직 넘버가 많아질 때 도입
  **가장 중요한 한 가지**

워크북 버전이 "틀린" 게 아닙니다. **배우기엔 워크북 버전이 더 좋습니다.** bscene 버전은 "프로젝트가 커지면 생기는 문제들"을 미리 손본 것이고, 그 변경마다 *이유(원칙)* 가 붙어 있다는 점이 핵심입니다. 여러분도 코드를 바꿀 땐 "그냥 멋있어서"가 아니라 **"어떤 문제를, 어떤 원칙으로 푸는가"** 를 한 줄 댈 수 있으면 됩니다. bscene이 주석으로 이유를 남긴 것처럼요.