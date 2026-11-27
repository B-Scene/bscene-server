package com.umc.bscene.domain.stream.controller.docs;

import com.umc.bscene.domain.stream.dto.request.ReportUserRequest;
import com.umc.bscene.domain.stream.dto.response.LiveAlarmToggleResponse;
import com.umc.bscene.domain.stream.dto.response.LiveHomeResponse;
import com.umc.bscene.domain.stream.dto.response.LiveStreamResponse;
import com.umc.bscene.domain.stream.dto.response.ReplayResponse;
import com.umc.bscene.domain.stream.dto.response.ReportUserResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamRoomResponse;
import com.umc.bscene.domain.stream.dto.response.UpcomingLiveResponse;
import com.umc.bscene.domain.stream.enums.ReplaySort;
import com.umc.bscene.global.response.CursorPage;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import com.umc.bscene.global.swagger.FanLiveApiResponseSchemas;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface FanLiveApi {

    @Operation(tags = {"팬모드_라이브", "밴드모드_라이브"}, summary = "라이브 홈 조회",
            description = "팬 모드에서는 현재 라이브 최대 3개, 다시보기 최대 8개, 예정 라이브 최대 3개를 반환합니다. 밴드 모드에서는 진행·예정 목록과 내 라이브 여부를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 홈 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.LiveHomeResponseEnvelope.class), examples = {
                            @ExampleObject(name = "팬 모드", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_5",
                                      "message": "라이브 홈 조회에 성공했습니다.",
                                      "result": {
                                        "liveNow": [
                                          {
                                            "liveId": 1,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "viewerCount": 68
                                          }
                                        ],
                                        "replays": [
                                          {
                                            "liveId": 3,
                                            "thumbnailImageUrl": "https://cdn.bscene.com/lives/3/thumbnail.png",
                                            "title": "합주실 라이브",
                                            "bandName": "WAVY",
                                            "viewCount": 6785
                                          }
                                        ],
                                        "scheduled": [
                                          {
                                            "liveId": 4,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "scheduledAt": "2026-11-29 20:00:00",
                                            "notificationEnabled": true
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """),
                            @ExampleObject(name = "밴드 모드", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_5",
                                      "message": "라이브 홈 조회에 성공했습니다.",
                                      "result": {
                                        "userId": 5,
                                        "liveNow": [
                                          {
                                            "liveId": 1,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "viewerCount": 68,
                                            "isMine": true,
                                            "coHost": [
                                              5
                                            ]
                                          }
                                        ],
                                        "scheduled": [
                                          {
                                            "liveId": 4,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "scheduledAt": "2026-11-29 20:00:00",
                                            "isMine": true,
                                            "coHost": [
                                              5
                                            ]
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE403_1", ref = "#/components/examples/LIVE403_1")
                    }))
    })
    ResponseEntity<SuccessResponse<LiveHomeResponse>> getLiveHome(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = {"팬모드_라이브", "밴드모드_라이브"}, summary = "라이브 방 진입",
            description = "밴드 모드에서는 송출자와 공동 진행 초대를 수락했거나 공동 송출을 승인받은 사용자만 입장할 수 있습니다. "
                    + "라이브 메타데이터와 재생 정보를 조회합니다. 청취자는 playback.playbackUrl을 HLS 플레이어에 전달합니다. "
                    + "MediaMTX의 매니페스트(index.m3u8)와 미디어 세그먼트 요청에는 HTTP Basic 인증의 비밀번호로 서비스 Access Token을 전달합니다. "
                    + "송출자와 공동 진행자는 반환된 WHIP URL에 application/sdp로 SDP offer를 POST합니다. "
                    + "WHIP 요청에도 HTTP Basic 인증의 비밀번호로 서비스 Access Token을 전달합니다. "
                    + "WHIP 201 응답의 본문은 SDP answer입니다. 송출을 중단하려면 Location을 WHIP 요청 URL 기준으로 해석한 주소에 "
                    + "DELETE를 보내고 브라우저의 오디오 트랙과 연결을 닫습니다. "
                    + "전체 라이브 방송 종료는 별도 종료 API를 사용합니다. 시청자 수 갱신은 SSE를 구독합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 방 진입에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.StreamRoomResponseEnvelope.class), examples = {
                            @ExampleObject(name = "청취자", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_1",
                                      "message": "라이브 방 진입에 성공했습니다.",
                                      "result": {
                                        "liveId": 1,
                                        "isLive": true,
                                        "nickname": "세진",
                                        "startedAt": "2026-11-28 19:00:00",
                                        "viewCount": 68,
                                        "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                        "title": "신곡 데모 첫 공개!",
                                        "bandName": "WAVY",
                                        "description": "미공개 데모를 라이브로 들려드려요.",
                                        "playback": {
                                          "role": "LISTENER",
                                          "protocol": "HLS",
                                          "playbackUrl": "https://media.example.com/hls/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764/index.m3u8"
                                        },
                                        "coPublishers": null
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """),
                            @ExampleObject(name = "밴드 송출자", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_1",
                                      "message": "라이브 방 진입에 성공했습니다.",
                                      "result": {
                                        "liveId": 1,
                                        "isLive": true,
                                        "nickname": "WAVY",
                                        "startedAt": "2026-11-28 19:00:00",
                                        "viewCount": 68,
                                        "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                        "title": "신곡 데모 첫 공개!",
                                        "bandName": "WAVY",
                                        "description": "미공개 데모를 라이브로 들려드려요.",
                                        "playback": {
                                          "role": "BROADCASTER",
                                          "protocol": "WHIP",
                                          "playbackUrl": "https://media.example.com/rtc/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764-m11/whip"
                                        },
                                        "coPublishers": [
                                          {
                                            "userId": 2,
                                            "whepUrl": "https://media.example.com/rtc/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764-m12/whep"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE403_1", ref = "#/components/examples/LIVE403_1")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1"),
                            @ExampleObject(name = "LIVE404_2", ref = "#/components/examples/LIVE404_2")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_1", ref = "#/components/examples/LIVE409_1"),
                            @ExampleObject(name = "LIVE409_5", ref = "#/components/examples/LIVE409_5")
                    }))
    })
    ResponseEntity<SuccessResponse<StreamRoomResponse>> enter(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = {"팬모드_라이브", "밴드모드_라이브"}, summary = "시청자 수 SSE 구독",
            description = "현재 시청자 수를 최초 한 번과 입·퇴장 시에 viewerCount 이벤트로 전송합니다. "
                    + "15초마다 ping 주석을 보냅니다. SSE 클라이언트에서 Accept: text/event-stream으로 구독하세요. "
                    + "공동 송출 요청 이벤트 coHostUpgradeRequested는 송출자에게, 수락 이벤트 coHostUpgradeAccepted는 요청자에게 전송합니다. "
                    + "진행자에게는 coPublishersChanged로 본인을 제외한 전체 진행자 목록을 보냅니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "시청자 수 이벤트 스트림", content = @Content(
                    mediaType = "text/event-stream", schema = @Schema(type = "string"),
                    examples = {
                        @ExampleObject(name = "시청자 수 갱신과 하트비트", value = """
                            event:viewerCount
                            data:3

                            event:viewerCount
                            data:4

                            :ping

                            """),
                        @ExampleObject(name = "공동 송출 요청", value = """
                            event:coHostUpgradeRequested
                            data:{"userId":2,"nickname":"세진"}

                            """),
                        @ExampleObject(name = "공동 송출 수락", value = """
                            event:coHostUpgradeAccepted
                            data:{"userId":2,"nickname":"세진"}

                            """),
                        @ExampleObject(name = "진행자 목록 갱신", value = """
                            event:coPublishersChanged
                            data:[{"userId":2,"whepUrl":"https://media.example.com/rtc/9a437c83-c1e7-4d35-8ef0-fbca9cdf3764-m12/whep"}]

                            """)
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "라이브가 없거나 방송 중이 아닙니다. SSE 요청은 HTTP 상태만 반환합니다.")
    })
    SseEmitter subscribeViewer(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId,
            @Parameter(description = "true면 시청자 수에 포함되지 않고 이벤트만 받습니다. 기본값은 false입니다.") boolean watchOnly,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(tags = "팬모드_라이브", summary = "라이브 나가기",
            description = "라이브 방에서 나가고 시청자 집계에서 제외됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 방에서 나갔습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_2",
                                      "message": "라이브 방에서 나갔습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> leave(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "팬모드_라이브", summary = "특정 사용자 신고",
            description = "신고 유형과 대상을 보냅니다. 채팅 내용과 추가 설명은 선택이며 각각 최대 500자입니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReportUserRequest.class), examples = {
                            @ExampleObject(name = "요청", value = """
                                    {
                                      "targetUserId": 1,
                                      "reportType": "ABUSE",
                                      "chatMessage": "비속어 사용 등 불쾌감을 주는 문장",
                                      "comment": null
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "201", description = "유저 신고가 접수되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.ReportUserResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "LIVE201_2",
                                      "message": "유저 신고가 접수되었습니다.",
                                      "result": {
                                        "targetUserId": 1
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "LIVE400_4", ref = "#/components/examples/LIVE400_4"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1"),
                            @ExampleObject(name = "LIVE404_5", ref = "#/components/examples/LIVE404_5")
                    }))
    })
    ResponseEntity<SuccessResponse<ReportUserResponse>> reportUser(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId,
            @Valid ReportUserRequest request
    );

    @Operation(tags = "팬모드_라이브", summary = "팔로우한 밴드 라이브 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "팔로우한 밴드의 라이브 중인 목록을 조회하는데 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.LiveStreamResponsePageEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_6",
                                      "message": "팔로우한 밴드의 라이브 중인 목록을 조회하는데 성공했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "liveId": 1,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "viewCount": 68
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE403_2", ref = "#/components/examples/LIVE403_2")
                    }))
    })
    ResponseEntity<SuccessResponse<CursorPage<LiveStreamResponse>>> getFollowingLiveStreams(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 pageInfo.nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이고 1~15를 허용합니다.") @Min(1) @Max(15) int size
    );

    @Operation(tags = "팬모드_라이브", summary = "라이브 중인 전체 밴드 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "현재 라이브 중인 목록을 조회하는데 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.LiveStreamResponsePageEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_3",
                                      "message": "현재 라이브 중인 목록을 조회하는데 성공했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "liveId": 1,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "viewCount": 68
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<LiveStreamResponse>>> getInLiveStreams(
            @Parameter(description = "이전 응답의 pageInfo.nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이고 1~15를 허용합니다.") @Min(1) @Max(15) int size
    );

    @Operation(tags = "팬모드_라이브", summary = "예정된 라이브 조회 [팔로우/전체]",
            description = "following=true면 팔로우한 밴드, false면 전체 밴드의 예정 라이브를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "예정된 라이브 목록을 조회하는데 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.UpcomingLiveResponsePageEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_7",
                                      "message": "예정된 라이브 목록을 조회하는데 성공했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "liveId": 4,
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "title": "신곡 데모 첫 공개!",
                                            "bandName": "WAVY",
                                            "scheduledAt": "2026-11-29 20:00:00",
                                            "isAlarmSet": true
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<UpcomingLiveResponse>>> getUpcomingLives(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "팔로우 밴드만 조회할지 여부. 기본값은 false입니다.") boolean following,
            @Parameter(description = "이전 응답의 pageInfo.nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이고 1~15를 허용합니다.") @Min(1) @Max(15) int size
    );

    @Operation(tags = "팬모드_라이브", summary = "예정된 라이브 단건 알림 예약",
            description = "예정 상태인 라이브의 알림 설정을 켜거나 끕니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 알림 설정 변경에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.LiveAlarmToggleResponseEnvelope.class), examples = {
                            @ExampleObject(name = "알림 설정", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_8",
                                      "message": "라이브 알림 설정 변경에 성공했습니다.",
                                      "result": {
                                        "alarmSet": true
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """),
                            @ExampleObject(name = "알림 해제", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_8",
                                      "message": "라이브 알림 설정 변경에 성공했습니다.",
                                      "result": {
                                        "alarmSet": false
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE400_2", ref = "#/components/examples/LIVE400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<LiveAlarmToggleResponse>> toggleLiveAlarm(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "팬모드_라이브", summary = "팔로우한 밴드 다시보기 목록 조회",
            description = "팔로우한 밴드의 다시보기를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "다시보기 목록 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.ReplayResponsePageEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_14",
                                      "message": "다시보기 목록 조회에 성공했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "liveId": 3,
                                            "thumbnailImageUrl": "https://cdn.bscene.com/lives/3/thumbnail.png",
                                            "title": "합주실 라이브",
                                            "bandName": "WAVY",
                                            "viewCount": 6785,
                                            "durationSec": 5025
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<ReplayResponse>>> getFollowingReplays(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "이전 응답의 pageInfo.nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이고 1~15를 허용합니다.") @Min(1) @Max(15) int size,
            @Parameter(description = "최신순(LATEST, 기본) 또는 인기순(POPULAR)") ReplaySort sort
    );

    @Operation(tags = "팬모드_라이브", summary = "다시보기 전체 목록 조회",
            description = "전체 밴드의 다시보기를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "다시보기 목록 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.ReplayResponsePageEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_14",
                                      "message": "다시보기 목록 조회에 성공했습니다.",
                                      "result": {
                                        "items": [
                                          {
                                            "liveId": 3,
                                            "thumbnailImageUrl": "https://cdn.bscene.com/lives/3/thumbnail.png",
                                            "title": "합주실 라이브",
                                            "bandName": "WAVY",
                                            "viewCount": 6785,
                                            "durationSec": 5025
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursor": null,
                                          "hasNext": false
                                        }
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<CursorPage<ReplayResponse>>> getAllReplays(
            @Parameter(description = "이전 응답의 pageInfo.nextCursor. 첫 페이지에서는 생략합니다.") Long cursor,
            @Parameter(description = "페이지 크기. 기본값은 10이고 1~15를 허용합니다.") @Min(1) @Max(15) int size,
            @Parameter(description = "최신순(LATEST, 기본) 또는 인기순(POPULAR)") ReplaySort sort
    );

    @Operation(tags = "팬모드_라이브", summary = "라이브 다시보기",
            description = "다시보기 재생 정보를 조회하고 조회수를 1 증가시킵니다. "
                    + "playbackUrl에 API 기본 경로(/api)를 붙여 HLS 매니페스트를 요청합니다. "
                    + "매니페스트 요청에는 서비스 Bearer 토큰 또는 access_token 쿠키가 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "다시보기 재생에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.StreamReplayResponseEnvelope.class), examples = {
                            @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_9",
                                      "message": "다시보기 재생에 성공했습니다.",
                                      "result": {
                                        "title": "합주실 라이브",
                                        "bandName": "WAVY",
                                        "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                        "viewCount": 6785,
                                        "durationSec": 5025,
                                        "playbackUrl": "/lives/3/replay/playlist"
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_3", ref = "#/components/examples/LIVE404_3")
                    }))
    })
    ResponseEntity<SuccessResponse<StreamReplayResponse>> watchReplay(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "팬모드_라이브", summary = "다시보기 재생 목록 조회",
            description = "다시보기 응답의 playbackUrl로 요청합니다. HLS 플레이어가 미디어 조각을 순서대로 "
                    + "재생할 수 있도록 임시 서명 URL이 담긴 M3U8을 제공합니다. "
                    + "서비스 Bearer 토큰 또는 access_token 쿠키로 인증합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "다시보기 HLS 매니페스트", content = @Content(
                    mediaType = "application/vnd.apple.mpegurl", schema = @Schema(type = "string"),
                    examples = @ExampleObject(name = "M3U8 재생 목록", value = """
                            #EXTM3U
                            #EXT-X-VERSION:7
                            #EXT-X-TARGETDURATION:32
                            #EXT-X-PLAYLIST-TYPE:VOD
                            #EXT-X-MAP:URI="https://media.example.com/replays/3/segment.mp4?X-Amz-Signature=example"
                            #EXTINF:32.0,
                            https://media.example.com/replays/3/segment.mp4?X-Amz-Signature=example
                            #EXT-X-ENDLIST
                            """))),
            @ApiResponse(responseCode = "400", description = "라이브 ID 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3"))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "다시보기 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "LIVE404_3", ref = "#/components/examples/LIVE404_3")))
    })
    String getReplayPlaylist(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "3") Long liveId
    );
}
