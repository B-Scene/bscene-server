<img width="4000" height="2250" alt="12341234-1(드래그함)" src="https://github.com/user-attachments/assets/01ceed9e-20c3-4605-8f14-fc0fc5ee642e" />

# 🎸 B:SCENE Back-End
> ## Be the Scene! 당신이 무대가 되는 순간

신생/인디 밴드를 위한 플랫폼 서비스 B:SCENE의 백엔드 저장소입니다.<br>
[B:SCENE BE 기술 문서 바로가기](https://bscene-be.notion.site/B-SCENE-Back-End-3bb419aa1e678038ace9fa5aba952ed5)
## 🛠️ 기술 스택
### Backend
![Java](https://img.shields.io/badge/Java%2021-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=gradle&logoColor=white)

### Database & Search
![MySQL](https://img.shields.io/badge/MySQL-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

### Streaming
![WebRTC](https://img.shields.io/badge/WebRTC-333333?style=for-the-badge&logo=webrtc&logoColor=white)
![MediaMTX](https://img.shields.io/badge/MediaMTX-1E88E5?style=for-the-badge)
![GStreamer](https://img.shields.io/badge/GStreamer-E01F27?style=for-the-badge&logo=gstreamer&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-007808?style=for-the-badge&logo=ffmpeg&logoColor=white)

### ML
![Python](https://img.shields.io/badge/Python%203.13-3776AB?style=for-the-badge&logo=python&logoColor=white)
![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?style=for-the-badge&logo=pytorch&logoColor=white)
![Hugging Face](https://img.shields.io/badge/Hugging%20Face-FFD21E?style=for-the-badge&logo=huggingface&logoColor=black)

### Infra & Monitoring
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana Loki](https://img.shields.io/badge/Grafana%20Loki-F46800?style=for-the-badge&logo=grafana&logoColor=white)

### External Services
![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase%20FCM-DD2C00?style=for-the-badge&logo=firebase&logoColor=white)
![CoolSMS](https://img.shields.io/badge/CoolSMS-2C7DF0?style=for-the-badge)
## ✨ 주요 기능
### 팬모드
- 홈 화면에서 밴드를 추천받고, 관심 있는 밴드를 팔로우해요.
- 팔로우한 밴드의 소식을 알림, 홈 화면 상단에서 빠르게 접해요.
  - 팔로우한 밴드가 없는 경우, 추천된 밴드의 소식을 홈 화면에 노출시킵니다.
- 밴드들의 공연 일정을 확인하고, 알림을 받을 수 있어요.
- 인상 깊었던 밴드의 이름을 검색해 팔로우해요.
- 관심 가는 밴드의 오디오 라이브를 들으며 채팅으로 소통할 수 있어요.
### 밴드모드
- 자신의 파트, 실력을 기입하여 세션 지원서를 작성해요.
- 지원서를 노출시켜 밴드로부터 가입 제안을 받을 수 있고, 밴드가 올린 모집 공고에 지원할 수 있어요.
- 모집 공고를 통해 해당 공고를 게시한 밴드와 쪽지를 주고 받을 수 있어요.
- 자신이 속한 밴드에 대해 팬들에게 알려요.
  - 공연 일정, 피드 게시글, 밴드에 대해 알리는 외부 링크를 게시할 수 있습니다.
- 라이브를 통해 팬들과 소통할 수 있어요.
  - 다른 밴드 멤버들을 초대해서 함께 오디오 송출을 진행할 수 있습니다.

## 🛠️ System Architecture

<img width="1536" height="1024" alt="bscene_system_diagram" src="https://github.com/user-attachments/assets/7a539eaf-6530-421f-9b4d-ee6c1db8fca6" />

## 📍 Service Modules

<table border="0">
  <tr>
    <td width="50%" valign="top">


### Authentication
- `auth`
  - 로컬 로그인 및 회원가입
  - 리프레시 토큰 및 RTR 구현
  - 문자 메시지 기반 실 사용자 체크
  - 온보딩 (선호 장르·지역, 팬 닉네임 설정)
- `oauth`
  - 소셜 로그인 및 회원가입
    - 백엔드 주도 리다이렉트 형태
    - 카카오, 구글, 애플
### Band
- `band`
  - 밴드 생성 및 프로필 관리
  - 멤버 초대·수락·강퇴 및 초대 링크
  - 밴드 멤버 프로필 (다중 프로필, 활성 프로필 전환)
  - 대표곡 음악 링크
  - 상호작용 기반 밴드 추천 조회
- `follow`
  - 밴드 팔로우 / 언팔로우
- `post`
  - 밴드 게시글 CRUD 및 썸네일
  - 댓글, 좋아요
- `performance`
  - 공연 등록·수정·삭제 및 상세 조회
  - 관심 공연, 공연 시작 알람
  - 밴드 멤버 공연 참여 확정 / 거절
  - 공연 리마인더 스케줄러
### Session
- `session`
  - 세션 모집 공고 CRUD, 검색, 관심 등록, 최근 본 공고
  - 세션 지원서 작성·제출·공개 범위 설정
  - 세션 기본 프로필 및 포트폴리오
  - 모집 마감 리마인더 스케줄러
### Chatting
- `chat`
  - 채팅방 생성·목록·나가기
  - WebSocket 티켓 발급 (일반 채팅 / 라이브 채팅)
  - WebSocket 기반 실시간 메시지 송수신

    </td>
    <td width="50%" valign="top" style="border: none;">

### Live Streaming
- `stream`
  - MediaMTX 연동 라이브 스트리밍
    - WHIP 송출 인증 훅
    - 라이브 상태 폴링
    - 공동 송출 (오디오 믹서 파이프라인)
  - 라이브 입장·퇴장 및 SSE 기반 시청자 수 집계
  - 다시보기 (녹화본 S3 업로드, HLS 재생)
  - 라이브 예약, 시작 알림 및 리마인더
  - 라이브 내 신고 알림 (실패 재시도 스케줄러)
### Discovery
- `search`
  - Elasticsearch 기반 탐색 통합검색
  - 최근 검색어 관리
  - 색인 동기화 이벤트 및 전체 재색인 스케줄러
- `recommendation`
  - 유저-밴드 상호작용(클릭) 기록
  - 밴드 유사도 및 추천 노출 로그 적재
### My
- `fanhome`
  - 팬 홈 화면 집계
    - 팔로잉 밴드 소식
    - 다가오는 공연, 추천 공연
    - 공연 캘린더 및 날짜별 공연 조회
- `user`
  - 마이페이지 (내 정보 조회·수정, 공연 참여 이력, 관심 공연, 팔로우 밴드)
  - 팬 / 밴드 모드 전환
  - 세션 지원 수락 및 최종 확정
  - 유저 차단
### Notification
- `notification`
  - FCM 푸시 토큰 등록 / 삭제
  - 알림 목록 조회 및 읽음 처리
  - 알림 유형별 수신 설정
  - 오래된 알림 정리 스케줄러

  </td>
  </tr>
</table>

## 🧑🏻‍💻 Developer
| BE | BE | BE | BE | BE |
|:---:|:---:|:---:|:---:|:---:|
| <img width="480" height="480" alt="taeyoung" src="https://github.com/user-attachments/assets/9d219f0e-8da7-4c4a-99ed-9632e493f4b7" /> | <img width="460" height="460" alt="heejin" src="https://github.com/user-attachments/assets/710848db-57e8-4c16-a162-489ce0ecf8fd" /> | <img width="460" height="460" alt="juyoung" src="https://github.com/user-attachments/assets/0f13be20-e650-4875-b1e2-0ac26b90db33" /> | <img width="460" height="460" alt="joonseok" src="https://github.com/user-attachments/assets/cbed48e0-951d-4cbe-b7e3-0de8ac0da522" /> | <img width="460" height="460" alt="sejin" src="https://github.com/user-attachments/assets/f258884a-8bec-46b9-b40a-664e5e9a2ce0" /> |
| 중앙대학교 | 가톨릭대학교 | 중앙대학교 | 한성대학교 | 중앙대학교 |
| 고태영 | 김희진 | 이주영 | 이준석 | 전세진 |
| 로컬 인증, 인가 기능 구현<br>FCM 기반 모바일 푸시 알림 기능 구현<br>밴드 공동 구현 | 세션 도메인 구현<br>WebSocket 기반 라이브 채팅, 세션 쪽지 구현 | 밴드 도메인 구현<br>AWS S3 인프라 구축<br>유저 행동 및 BERT 모델 기반 추천 알고리즘 구현 | Git Actions, Docker 등 운영환경 구축<br>WebRTC/LL-HLS 기반 라이브 도메인 구현<br>마이 공동 구현 | 소셜 인증, 인가 기능 구현<br>공연 도메인 구현<br>밴드, 마이 공동 구현<br>Elastic Search 기반 탐색 기능 구현 |
| [@Yarlang](https://github.com/Yarlang) | [@heejin0283](https://github.com/heejin0283) | [@LeeJuYoung12](https://github.com/LeeJuYoung12) | [@Joonseok-Lee](https://github.com/Joonseok-Lee) | [@newjini9](https://github.com/newjini9) |

---
**🎸 B:SCENE을 사용해보고 싶다면...?**<br>
아래의 링크로 접속하면 실제 서비스 중인 B:SCENE을 만나보실 수 있습니다.<br>
[B:SCENE 즐기기 🎵](https://www.bscene.app)
