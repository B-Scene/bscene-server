package com.umc.bscene.domain.stream.controller.docs;

import com.umc.bscene.domain.stream.dto.request.CoHostInvitationDecisionRequest;
import com.umc.bscene.domain.stream.dto.request.CoHostUpgradeAcceptRequest;
import com.umc.bscene.domain.stream.dto.request.ReservationPatchRequest;
import com.umc.bscene.domain.stream.dto.request.StreamCreateRequest;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.dto.response.ReservationEditResponse;
import com.umc.bscene.domain.stream.dto.response.StreamCreateResponse;
import com.umc.bscene.domain.stream.dto.response.StreamSummaryResponse;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface BandLiveApi {

    @Operation(tags = "밴드모드_라이브", summary = "라이브 방 생성",
            description = "활성 밴드 프로필로 라이브 방을 생성합니다. scheduledAt을 생략하면 즉시 방송용 방을 준비하며, 송출자가 방에 입장하면 방송이 시작됩니다. 예약은 송출자당 최대 5개이며 coHost는 초대할 사용자 ID 목록입니다. 썸네일은 미디어 업로드가 끝난 이미지 URL을 전달합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StreamCreateRequest.class), examples = {
                            @ExampleObject(name = "즉시 방송 준비", value = """
                                    {
                                      "title": "오늘 저녁 라이브",
                                      "description": "신곡과 신청곡을 연주합니다.",
                                      "thumbnailImageUrl": "https://cdn.bscene.com/lives/1/thumbnail.jpg"
                                    }
                                    """),
                            @ExampleObject(name = "예약과 공동 진행자 초대", value = """
                                    {
                                      "title": "오늘 저녁 라이브",
                                      "description": "신곡과 신청곡을 연주합니다.",
                                      "thumbnailImageUrl": "https://cdn.bscene.com/lives/1/thumbnail.jpg",
                                      "scheduledAt": "2027-07-23T20:00:00",
                                      "coHost": [
                                        2,
                                        3
                                      ]
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "201", description = "라이브 생성에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.StreamCreateResponseEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "LIVE201_1",
                                      "message": "라이브 생성에 성공했습니다.",
                                      "result": {
                                        "audioStreamId": 1,
                                        "path": "9a437c83-c1e7-4d35-8ef0-fbca9cdf3764",
                                        "title": "오늘 저녁 라이브"
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE400_1", ref = "#/components/examples/LIVE400_1"),
                            @ExampleObject(name = "LIVE400_3", ref = "#/components/examples/LIVE400_3"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE403_1", ref = "#/components/examples/LIVE403_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_2", ref = "#/components/examples/LIVE409_2"),
                            @ExampleObject(name = "LIVE409_3", ref = "#/components/examples/LIVE409_3")
                    }))
    })
    ResponseEntity<SuccessResponse<StreamCreateResponse>> createAudioStream(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid StreamCreateRequest request
    );

    @Operation(tags = "밴드모드_라이브", summary = "공동 송출자 업그레이드 요청",
            description = "방송 중인 라이브와 같은 밴드의 멤버가 공동 송출을 요청합니다. 송출자에게 coHostUpgradeRequested SSE가 전송됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "공동 송출자 업그레이드 요청이 접수되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 201,
                                      "code": "LIVE201_3",
                                      "message": "공동 송출자 업그레이드 요청이 접수되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE409_6", ref = "#/components/examples/LIVE409_6"),
                            @ExampleObject(name = "LIVE409_8", ref = "#/components/examples/LIVE409_8"),
                            @ExampleObject(name = "LIVE409_9", ref = "#/components/examples/LIVE409_9")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> requestCoHostUpgrade(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "공동 송출자 업그레이드 수락",
            description = "송출자가 요청자의 userId로 공동 송출을 승인합니다. 요청자는 coHostUpgradeAccepted SSE를 받으면 라이브 방에 다시 진입해 송출 URL을 받습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CoHostUpgradeAcceptRequest.class), examples = {
                            @ExampleObject(name = "요청자 수락", value = """
                                    {
                                      "userId": 1
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "공동 송출자 업그레이드 요청을 수락했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_17",
                                      "message": "공동 송출자 업그레이드 요청을 수락했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
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
                            @ExampleObject(name = "LIVE404_2", ref = "#/components/examples/LIVE404_2"),
                            @ExampleObject(name = "LIVE404_7", ref = "#/components/examples/LIVE404_7")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_6", ref = "#/components/examples/LIVE409_6"),
                            @ExampleObject(name = "LIVE409_8", ref = "#/components/examples/LIVE409_8")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> acceptCoHostUpgrade(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId,
            @Valid CoHostUpgradeAcceptRequest request
    );

    @Operation(tags = "밴드모드_라이브", summary = "밴드 멤버 조회",
            description = "라이브 방에 입장 중인 송출자와 공동 진행자의 밴드 멤버 프로필을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "밴드 멤버 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.LiveMembersResponseEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_15",
                                      "message": "밴드 멤버 조회에 성공했습니다.",
                                      "result": {
                                        "members": [
                                          {
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "nickname": "해진",
                                            "bandName": "WAVY",
                                            "part": [
                                              "보컬"
                                            ],
                                            "isLeader": true
                                          },
                                          {
                                            "bandProfileImageUrl": "https://cdn.bscene.com/bands/5/profile.png",
                                            "nickname": "세진",
                                            "bandName": "WAVY",
                                            "part": [
                                              "드럼"
                                            ],
                                            "isLeader": false
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<LiveMembersResponse>> getLiveMembers(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 종료",
            description = "송출자가 라이브를 종료하고 종료 시점의 시청자 수를 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 종료에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_4",
                                      "message": "라이브 종료에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> closeAudioStream(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 종료화면 조회",
            description = "종료된 라이브의 재생 길이(초)와 종료 당시 시청자 수를 송출자가 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 종료 화면 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.StreamSummaryResponseEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_10",
                                      "message": "라이브 종료 화면 조회에 성공했습니다.",
                                      "result": {
                                        "title": "오늘 저녁 라이브",
                                        "durationSec": 5025,
                                        "closedViewerCount": 68
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_4", ref = "#/components/examples/LIVE409_4")
                    }))
    })
    ResponseEntity<SuccessResponse<StreamSummaryResponse>> getStreamSummary(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 다시보기 등록",
            description = "송출자가 종료된 라이브의 녹화본 업로드를 요청합니다. 202는 비동기 저장 요청 접수를 뜻합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "202", description = "다시보기 저장 요청이 접수되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 202,
                                      "code": "LIVE202_1",
                                      "message": "다시보기 저장 요청이 접수되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE404_4", ref = "#/components/examples/LIVE404_4")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_4", ref = "#/components/examples/LIVE409_4")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> requestReplayUpload(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 예약 편집화면 조회",
            description = "예정된 라이브의 기존 정보와 공동 진행 후보를 조회합니다. 후보별 status는 OWNER, APPROVED, INVITED, REJECTED 또는 미선택(null)입니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 예약 편집 조회에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = FanLiveApiResponseSchemas.ReservationEditResponseEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_11",
                                      "message": "라이브 예약 편집 조회에 성공했습니다.",
                                      "result": {
                                        "liveId": 1,
                                        "title": "오늘 저녁 라이브",
                                        "description": "신곡과 신청곡을 연주합니다.",
                                        "thumbnailImageUrl": "https://cdn.bscene.com/lives/1/thumbnail.jpg",
                                        "scheduledAt": "2027-07-23T20:00:00",
                                        "coHostCandidates": [
                                          {
                                            "userId": 1,
                                            "bandMemberId": 11,
                                            "bandMemberProfileId": 21,
                                            "bandMemberProfileImageUrl": "https://cdn.bscene.com/members/21/profile.png",
                                            "nickname": "해진",
                                            "part": "보컬",
                                            "status": "OWNER"
                                          },
                                          {
                                            "userId": 2,
                                            "bandMemberId": 12,
                                            "bandMemberProfileId": 22,
                                            "bandMemberProfileImageUrl": null,
                                            "nickname": "세진",
                                            "part": "드럼",
                                            "status": "INVITED"
                                          }
                                        ]
                                      },
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_5", ref = "#/components/examples/LIVE409_5")
                    }))
    })
    ResponseEntity<SuccessResponse<ReservationEditResponse>> getReservationForEdit(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 예약 편집",
            description = "송출자가 예정된 라이브를 편집합니다. 생략하거나 null인 필드는 기존 값을 유지하며, coHost에 빈 배열을 보내면 공동 진행자 지정이 해제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReservationPatchRequest.class), examples = {
                            @ExampleObject(name = "예약 변경", value = """
                                    {
                                      "title": "신곡 데모 첫 공개!",
                                      "description": "미공개 데모를 라이브로 들려드려요",
                                      "thumbnailImageUrl": "https://cdn.bscene.com/lives/1/thumbnail.jpg",
                                      "scheduledAt": "2027-07-23T20:00:00",
                                      "coHost": []
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 예약 수정에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_12",
                                      "message": "라이브 예약 수정에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE400_3", ref = "#/components/examples/LIVE400_3"),
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE403_1", ref = "#/components/examples/LIVE403_1")
                    })),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_5", ref = "#/components/examples/LIVE409_5"),
                            @ExampleObject(name = "LIVE409_6", ref = "#/components/examples/LIVE409_6")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> updateReservation(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId,
            @Valid ReservationPatchRequest request
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 예약 취소",
            description = "송출자가 예정 상태의 라이브 예약을 취소합니다.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "라이브 예약 취소에 성공했습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_13",
                                      "message": "라이브 예약 취소에 성공했습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
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
                            @ExampleObject(name = "LIVE404_1", ref = "#/components/examples/LIVE404_1")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_5", ref = "#/components/examples/LIVE409_5")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> cancelReservation(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId
    );

    @Operation(tags = "밴드모드_라이브", summary = "라이브 공동 진행자 초대 수락, 거절",
            description = "초대받은 사용자가 isAccepted로 공동 진행 초대를 수락하거나 거절합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CoHostInvitationDecisionRequest.class), examples = {
                            @ExampleObject(name = "초대 수락", value = """
                                    {
                                      "isAccepted": true
                                    }
                                    """),
                            @ExampleObject(name = "초대 거절", value = """
                                    {
                                      "isAccepted": false
                                    }
                                    """)
                    })), responses = {
            @ApiResponse(responseCode = "200", description = "공동 진행자 초대 응답이 완료되었습니다.", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class),
                    examples = @ExampleObject(name = "성공", value = """
                                    {
                                      "isSuccess": true,
                                      "status": 200,
                                      "code": "LIVE200_16",
                                      "message": "공동 진행자 초대 응답이 완료되었습니다.",
                                      "result": null,
                                      "timeStamp": "2026-11-28 20:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 형식 오류", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                            @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                    })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE404_6", ref = "#/components/examples/CO_HOST_INVITATION_NOT_FOUND")
                    })),
            @ApiResponse(responseCode = "409", description = "요청이 현재 상태와 충돌함", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "LIVE409_5", ref = "#/components/examples/LIVE409_5"),
                            @ExampleObject(name = "LIVE409_7", ref = "#/components/examples/LIVE409_7")
                    }))
    })
    ResponseEntity<SuccessResponse<Void>> decideCoHostInvitation(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(description = "라이브 ID", example = "1") Long liveId,
            @Valid CoHostInvitationDecisionRequest request
    );
}
