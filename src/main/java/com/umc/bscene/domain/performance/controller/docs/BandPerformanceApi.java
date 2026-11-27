package com.umc.bscene.domain.performance.controller.docs;

import com.umc.bscene.domain.performance.dto.request.PerformanceCreateRequest;
import com.umc.bscene.domain.performance.dto.request.PerformanceUpdateRequest;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.BandContentResponseSchemas;
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

public interface BandPerformanceApi {

    @Operation(tags = "밴드모드_내밴드(공연)", summary = "공연 등록",
            description = "검수를 통과한 밴드의 오너 또는 가입이 완료된 멤버·세션이 공연을 등록할 수 있습니다. 지난 날짜는 등록할 수 없으며 태그는 최대 8개입니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PerformanceCreateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "title": "WAVY 단독 공연",
                                              "genre": "METAL",
                                              "performanceDate": "2027-08-17",
                                              "startTime": "19:00",
                                              "region": "SEOUL",
                                              "venue": "홍대 롤링홀",
                                              "description": "여름 정기 공연",
                                              "ticketPrice": "20,000원",
                                              "ticketLink": "https://ticket.example.com/wavy",
                                              "posterImageUrl": "https://cdn.bscene.com/poster/wavy.png",
                                              "ageRating": "ALL",
                                              "tags": ["인디","밴드공연","홍대"]
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PerformanceEnvelope.class), examples = {
                                    @ExampleObject(name = "SHOW201_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 201,
                                              "code": "SHOW201_1",
                                              "message": "공연이 등록됐어요.",
                                              "result": {"performanceId":4,"title":"WAVY 단독 공연","genre":"METAL","performanceDate":"2027-08-17","startTime":"19:00:00","region":"SEOUL","venue":"홍대 롤링홀","description":"여름 정기 공연","ticketPrice":"20,000원","ticketLink":"https://ticket.example.com/wavy","posterImageUrl":"https://cdn.bscene.com/poster/wavy.png","ageRating":"ALL","tags":["인디","밴드공연","홍대"],"interestCount":0,"isInterested":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW400_1", ref = "#/components/examples/SHOW400_1"),
                                    @ExampleObject(name = "SHOW400_4", ref = "#/components/examples/SHOW400_4"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW403_1", ref = "#/components/examples/SHOW403_1"),
                                    @ExampleObject(name = "BAND403_6", ref = "#/components/examples/BAND403_6")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            @Valid PerformanceCreateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(공연)", summary = "공연 목록",
            description = "밴드의 활성 공연을 공연 날짜 오름차순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PerformanceListEnvelope.class), examples = {
                                    @ExampleObject(name = "SHOW200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SHOW200_1",
                                              "message": "공연 목록을 조회했습니다.",
                                              "result": {"performances":[{"performanceId":3,"title":"2026 인디밴드 합동 공연","performanceDate":"2027-08-15","venue":"홍대 롤링홀","posterImageUrl":"https://cdn.example.com/posters/performance-123.jpg"},{"performanceId":4,"title":"WAVY 단독 공연","performanceDate":"2027-08-17","venue":"홍대 롤링홀","posterImageUrl":"https://cdn.bscene.com/poster/wavy.png"}]},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceListResponse>> getPerformances(
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "밴드모드_내밴드(공연)", summary = "공연 상세",
            description = "공연 정보와 관심 공연 등록 수, 본인의 관심 등록 여부를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PerformanceEnvelope.class), examples = {
                                    @ExampleObject(name = "SHOW200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SHOW200_2",
                                              "message": "공연 상세를 조회했습니다.",
                                              "result": {"performanceId":4,"title":"WAVY 단독 공연","genre":"METAL","performanceDate":"2027-08-17","startTime":"19:00:00","region":"SEOUL","venue":"홍대 롤링홀","description":"여름 정기 공연","ticketPrice":"20,000원","ticketLink":"https://ticket.example.com/wavy","posterImageUrl":"https://cdn.bscene.com/poster/wavy.png","ageRating":"ALL","tags":["인디","밴드공연","홍대"],"interestCount":0,"isInterested":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceResponse>> getPerformanceDetail(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "4") Long performanceId
    );

    @Operation(tags = "밴드모드_내밴드(공연)", summary = "공연 수정",
            description = "해당 밴드의 오너 또는 가입이 완료된 멤버·세션이 공연을 수정할 수 있습니다. 전달한 필드만 반영하며, 날짜를 변경할 때는 오늘 이후 날짜를 사용합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PerformanceUpdateRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "startTime": "20:00",
                                              "ticketPrice": "25,000원"
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.PerformanceEnvelope.class), examples = {
                                    @ExampleObject(name = "SHOW200_3", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SHOW200_3",
                                              "message": "공연 정보가 수정됐어요.",
                                              "result": {"performanceId":4,"title":"WAVY 단독 공연","genre":"METAL","performanceDate":"2027-08-17","startTime":"20:00:00","region":"SEOUL","venue":"홍대 롤링홀","description":"여름 정기 공연","ticketPrice":"25,000원","ticketLink":"https://ticket.example.com/wavy","posterImageUrl":"https://cdn.bscene.com/poster/wavy.png","ageRating":"ALL","tags":["인디","밴드공연","홍대"],"interestCount":0,"isInterested":false},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW400_1", ref = "#/components/examples/SHOW400_1"),
                                    @ExampleObject(name = "SHOW400_4", ref = "#/components/examples/SHOW400_4"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW403_1", ref = "#/components/examples/SHOW403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<PerformanceResponse>> updatePerformance(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "4") Long performanceId,
            PerformanceUpdateRequest request
    );

    @Operation(tags = "밴드모드_내밴드(공연)", summary = "공연 삭제",
            description = "해당 밴드의 오너 또는 가입이 완료된 멤버·세션이 공연을 삭제할 수 있습니다. 삭제한 공연은 목록과 상세 조회에서 제외됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyApiResponseSchemas.EmptyEnvelope.class), examples = {
                                    @ExampleObject(name = "SHOW200_4", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "SHOW200_4",
                                              "message": "공연이 삭제됐어요.",
                                              "result": null,
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "COMMON_400_3", ref = "#/components/examples/COMMON_400_3")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW403_1", ref = "#/components/examples/SHOW403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "SHOW404_1", ref = "#/components/examples/SHOW404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<Void>> deletePerformance(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "4") Long performanceId
    );
}
