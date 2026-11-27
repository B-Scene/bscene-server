package com.umc.bscene.domain.band.controller.docs;

import com.umc.bscene.domain.band.dto.request.MusicLinkSaveRequest;
import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.global.response.SuccessResponse;
import com.umc.bscene.global.security.entity.AuthMember;
import org.springframework.http.ResponseEntity;
import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.swagger.BandContentResponseSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface BandMusicLinkApi {

    @Operation(tags = "밴드모드_내밴드(음원 링크)", summary = "음원 링크 목록",
            description = "밴드의 플랫폼별 음원 링크를 조회합니다. 등록한 링크가 없으면 각 필드가 null로 반환됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.MusicLinkEnvelope.class), examples = {
                                    @ExampleObject(name = "MUSIC200_1", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "MUSIC200_1",
                                              "message": "음원 링크를 조회했습니다.",
                                              "result": {"spotifyUrl":"https://open.spotify.com/artist/xxxx","youtubeUrl":"https://youtube.com/@wavy","soundcloudUrl":null,"etcPlatform":"MELON","etcUrl":"https://melon.com/artist/xxxx","otherUrl":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "등록된 링크 없음", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "MUSIC200_1",
                                              "message": "음원 링크를 조회했습니다.",
                                              "result": {"spotifyUrl":null,"youtubeUrl":null,"soundcloudUrl":null,"etcPlatform":null,"etcUrl":null,"otherUrl":null},
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
    ResponseEntity<SuccessResponse<MusicLinkResponse>> getMusicLink(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId
    );

    @Operation(tags = "밴드모드_내밴드(음원 링크)", summary = "음원 링크 등록·수정·삭제",
            description = "밴드 멤버가 전체 링크를 저장합니다. 삭제할 링크는 null로 전달합니다. 기타 플랫폼과 링크는 함께 입력하거나 함께 비워야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MusicLinkSaveRequest.class), examples = {
                                    @ExampleObject(name = "요청 예시", value = """
                                            {
                                              "spotifyUrl": "https://open.spotify.com/artist/xxxx",
                                              "youtubeUrl": "https://youtube.com/@wavy",
                                              "soundcloudUrl": null,
                                              "etcPlatform": "MELON",
                                              "etcUrl": "https://melon.com/artist/xxxx",
                                              "otherUrl": null
                                            }
                                            """),
                                    @ExampleObject(name = "링크 전체 삭제", value = """
                                            {
                                              "spotifyUrl": null,
                                              "youtubeUrl": null,
                                              "soundcloudUrl": null,
                                              "etcPlatform": null,
                                              "etcUrl": null,
                                              "otherUrl": null
                                            }
                                            """)
                            })))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BandContentResponseSchemas.MusicLinkEnvelope.class), examples = {
                                    @ExampleObject(name = "MUSIC200_2", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "MUSIC200_2",
                                              "message": "저장됐어요.",
                                              "result": {"spotifyUrl":"https://open.spotify.com/artist/xxxx","youtubeUrl":"https://youtube.com/@wavy","soundcloudUrl":null,"etcPlatform":"MELON","etcUrl":"https://melon.com/artist/xxxx","otherUrl":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """),
                                    @ExampleObject(name = "링크 전체 삭제", value = """
                                            {
                                              "isSuccess": true,
                                              "status": 200,
                                              "code": "MUSIC200_2",
                                              "message": "저장됐어요.",
                                              "result": {"spotifyUrl":null,"youtubeUrl":null,"soundcloudUrl":null,"etcPlatform":null,"etcUrl":null,"otherUrl":null},
                                              "timeStamp": "2026-11-26 09:15:00"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MUSIC400_1", ref = "#/components/examples/MUSIC400_1"),
                                    @ExampleObject(name = "COMMON_400_2", ref = "#/components/examples/COMMON_400_2")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "MUSIC403_1", ref = "#/components/examples/MUSIC403_1")
                            })),
            @ApiResponse(responseCode = "404", description = "데이터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                                    @ExampleObject(name = "BAND404_1", ref = "#/components/examples/BAND404_1")
                            })),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AuthenticationRequired")
    })
    ResponseEntity<SuccessResponse<MusicLinkResponse>> saveMusicLink(
            @Parameter(hidden = true) AuthMember authMember,
            @Parameter(example = "1") Long bandId,
            MusicLinkSaveRequest request
    );
}
