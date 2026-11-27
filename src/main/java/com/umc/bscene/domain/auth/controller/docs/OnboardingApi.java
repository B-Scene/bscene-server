package com.umc.bscene.domain.auth.controller.docs;

import com.umc.bscene.domain.auth.dto.onboarding.request.OnboardingSaveRequest;
import com.umc.bscene.domain.auth.dto.onboarding.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.GenreResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.RegionResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import java.util.List;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.MyApiResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

public interface OnboardingApi {

    @Operation(tags = "온보딩", summary = "온보딩 상태 조회",
            description = "활성 모드와 사용 가능한 모드, 팬 정보, 남은 온보딩 단계를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.OnboardingStatusResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "팬 온보딩 완료", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_1",
                                              "message": "온보딩 상태 조회에 성공했습니다.",
                                              "result": {"completed":true,"currentMode":"FAN","availableModes":["FAN","BAND"],"fanNickname":"BSCENE","selectedGenres":[{"code":"METAL","name":"메탈"},{"code":"BLUES","name":"블루스"},{"code":"INDIE","name":"인디"}],"selectedRegions":[{"code":"SEOUL","name":"서울"},{"code":"GYEONGGI","name":"경기"}],"requiredSteps":[]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "밴드로 시작", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_1",
                                              "message": "온보딩 상태 조회에 성공했습니다.",
                                              "result": {"completed":false,"currentMode":"BAND","availableModes":["BAND"],"fanNickname":null,"selectedGenres":[],"selectedRegions":[],"requiredSteps":[]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "신규 사용자", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_1",
                                              "message": "온보딩 상태 조회에 성공했습니다.",
                                              "result": {"completed":false,"currentMode":null,"availableModes":[],"fanNickname":null,"selectedGenres":[],"selectedRegions":[],"requiredSteps":["MODE","AVAILABLE_MODE"]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<OnboardingStatusResponse>> getMyOnboardingStatus(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(tags = "온보딩", summary = "닉네임 중복 확인",
            description = "앞뒤 공백을 제거한 닉네임의 사용 가능 여부를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.FanNicknameCheckResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "ONBOARDING200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_2",
                                              "message": "팬 닉네임 중복 확인에 성공했습니다.",
                                              "result": {"available":true},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<FanNicknameCheckResponse>> checkFanNickname(
            @Parameter(example = "BSCENE") String nickname
    );

    @Operation(tags = "온보딩", summary = "온보딩 정보 저장",
            description = "초기 모드는 선택한 모드 중 하나여야 합니다. FAN을 선택하면 닉네임·장르·지역이 필수입니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OnboardingSaveRequest.class),
                            examples = {
                                    @ExampleObject(name = "팬 정보 포함", value = """
                                            {
                                              "selectedModes": ["FAN","BAND"],
                                              "initialMode": "FAN",
                                              "fanNickname": "BSCENE",
                                              "genres": ["METAL","BLUES","INDIE"],
                                              "regions": ["SEOUL","GYEONGGI"]
                                            }
                                            """),
                                    @ExampleObject(name = "밴드만 선택", value = """
                                            {
                                              "selectedModes": ["BAND"],
                                              "initialMode": "BAND"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.OnboardingStatusResponseEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "팬 정보 포함", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_3",
                                              "message": "온보딩 정보 저장에 성공했습니다.",
                                              "result": {"completed":true,"currentMode":"FAN","availableModes":["FAN","BAND"],"fanNickname":"BSCENE","selectedGenres":[{"code":"METAL","name":"메탈"},{"code":"BLUES","name":"블루스"},{"code":"INDIE","name":"인디"}],"selectedRegions":[{"code":"SEOUL","name":"서울"},{"code":"GYEONGGI","name":"경기"}],"requiredSteps":[]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "밴드만 선택", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "ONBOARDING200_3",
                                              "message": "온보딩 정보 저장에 성공했습니다.",
                                              "result": {"completed":false,"currentMode":"BAND","availableModes":["BAND"],"fanNickname":null,"selectedGenres":[],"selectedRegions":[],"requiredSteps":[]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ONBOARDING400_1", ref = "#/components/examples/ONBOARDING400_1"),
                                    @ExampleObject(name = "ONBOARDING400_2", ref = "#/components/examples/ONBOARDING400_2"),
                                    @ExampleObject(name = "ONBOARDING400_3", ref = "#/components/examples/ONBOARDING400_3"),
                                    @ExampleObject(name = "ONBOARDING400_4", ref = "#/components/examples/ONBOARDING400_4"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2"),
                                    @ExampleObject(name = "ONBOARDING400_5", ref = "#/components/examples/ONBOARDING400_5")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired"),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ONBOARDING404_1", ref = "#/components/examples/ONBOARDING404_1")
                            })),
            @ApiResponse(responseCode = "409", description = "상태 충돌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "ONBOARDING409_1", ref = "#/components/examples/ONBOARDING409_1"),
                                    @ExampleObject(name = "ONBOARDING409_2", ref = "#/components/examples/ONBOARDING409_2")
                            }))
    })
    ResponseEntity<SuccessResponse<OnboardingStatusResponse>> saveOnboarding(
            @Parameter(hidden = true) AuthMember authMember,
            @Valid OnboardingSaveRequest request
    );

    @Operation(tags = "온보딩", summary = "장르 목록 조회",
            description = "현재 장르 코드와 표시 이름을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.GenreListEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "GENRE200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "GENRE200_1",
                                              "message": "장르 목록 조회에 성공했습니다.",
                                              "result": [{"code":"METAL","name":"메탈"},{"code":"BLUES","name":"블루스"},{"code":"PSYCHEDELIC_ROCK","name":"사이키델릭록"},{"code":"ALTERNATIVE_ROCK","name":"얼터너티브록"},{"code":"INDIE","name":"인디"},{"code":"ELECTRONIC_ROCK","name":"일렉트로닉록"},{"code":"JAZZ","name":"재즈"},{"code":"POP","name":"팝"},{"code":"POP_ROCK","name":"팝록"},{"code":"PUNK_ROCK","name":"펑크록"},{"code":"FOLK_ROCK","name":"포크록"},{"code":"HARD_ROCK","name":"하드록"},{"code":"ETC","name":"etc"}],
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<List<GenreResponse>>> getGenres();

    @Operation(tags = "온보딩", summary = "지역 목록 조회",
            description = "현재 지역 코드와 표시 이름을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.RegionListEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "REGION200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "REGION200_1",
                                              "message": "지역 목록 조회에 성공했습니다.",
                                              "result": [{"code":"SEOUL","name":"서울"},{"code":"GYEONGGI","name":"경기"},{"code":"INCHEON","name":"인천"},{"code":"BUSAN","name":"부산"},{"code":"DAEGU","name":"대구"},{"code":"GWANGJU","name":"광주"},{"code":"DAEJEON","name":"대전"},{"code":"ULSAN","name":"울산"},{"code":"SEJONG","name":"세종"},{"code":"CHUNGBUK","name":"충북"},{"code":"CHUNGNAM","name":"충남"},{"code":"JEONBUK","name":"전북"},{"code":"JEONNAM","name":"전남"},{"code":"GYEONGBUK","name":"경북"},{"code":"GYEONGNAM","name":"경남"},{"code":"GANGWON","name":"강원"},{"code":"JEJU","name":"제주"},{"code":"UNKNOWN","name":"지역 미상"}],
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            }))
    })
    ResponseEntity<SuccessResponse<List<RegionResponse>>> getRegions();
}
