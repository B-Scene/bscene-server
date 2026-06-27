# 🔥 Project Convention

## 🛠️ Build Info
- **Language** : Java 21
- **Framework** : Spring boot 4.1.0
- **Database** : MySQL

## 🪾 Branching Rule
- 기본적으로 `develop`에서 checkout 하기
  1. checkout 전에 반드시 `pull` 하기!!!
  2. merge가 늦어져서 불가피하게 checkout해야 되는 경우, merge도 checkout한 브랜치로!
     1. ex. `feat/#1`에서 `feat/#2`를 체크아웃한 경우, `feat/#2`의 PR base는 `feat/#1`
  3. 잘 모르겠으면 파트장에게 물어보기

## 📋 Branch Name Convention
브랜치의 이름은 다음과 같은 규칙을 따릅니다.
| type       | name                    | description     |
|------------|-------------------------|-----------------|
| `feat`     | `feat/￼#ISSUE_NUM￼`     | ⚡️ 새로운 기능 추가     |
| `fix`      | `fix/￼#ISSUE_NUM￼`      | 🐛 버그 수정         |
| `docs`     | `docs/￼#ISSUE_NUM￼`     | 📝 문서 수정         |
| `refactor` | `refactor/￼#ISSUE_NUM￼` | 💫 리팩토링          |
| `test`     | `test/￼#ISSUE_NUM￼`     | 🧪 테스트 코드 작성     |
| `chore`    | `chore/￼#ISSUE_NUM￼`    | 🛠️ 빌드, 패키지 관련 수정 |
| `perf`     | `perf/￼#ISSUE_NUM￼`     | 🪄 성능 개선         |
| `ci`       | `ci/￼#ISSUE_NUM￼`       | 🔄 CI 관련 수정      |
| `cd`       | `cd/￼#ISSUE_NUM￼`       | 🔄 CD 관련 수정      |
| `revert`   | `revert/￼#ISSUE_NUM￼`   | ⚠️ 특정 커밋으로 되돌리기  |
이후, 이 브랜치에서 작업하는 내용을 누구나 알 수 있도록 명시합니다.
완성 예시 : `feat/#1-login-api`

## ⚠️ Issue Convention
이슈 제목은 **타입**과 간단한 **설명**을 적습니다.
ex. `[Feat] 상품 CRUD 구현`

## 📄 Commit Convention
- 최소 작업 단위로 가능한 한 **작게 쪼개어 커밋**합니다.
- 하나의 커밋에는 하나의 작업만 포함합니다.
- 커밋 메시지 제목은 작업 내용을 직관적으로 이해하기 쉽게 작성합니다.

커밋 메시지 구조는 다음과 같습니다.
`feat: 상품 조회 기능 구현`
`fix: 상품 조회 중, 잘못된 ID인 경우 예외를 던지도록 수정`

커밋 `Prefix`의 경우, 브랜치 네이밍 타입과 동일한 방식으로 작성합니다.

## 📌 Git Branch Strategy
| branch    | role                                   |
|-----------|----------------------------------------|
| `main`    | - 최종 배포용 브랜치<br>- dev 브랜치에서 안정화 버전만 병합 |
| `develop` | - 개발용 브랜치<br>- 자유롭게 병합                 |
