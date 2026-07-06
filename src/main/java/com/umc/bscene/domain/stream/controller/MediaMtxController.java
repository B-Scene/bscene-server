package com.umc.bscene.domain.stream.controller;

import com.umc.bscene.domain.stream.dto.request.MtxAuthRequest;
import com.umc.bscene.domain.stream.service.StreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/internal/mediamtx")
public class MediaMtxController {

    private final StreamService streamService;

    public ResponseEntity<Void> authorize(
            @Valid @RequestBody MtxAuthRequest request
    ) {
        boolean allowed = switch(request.action()) {
            /*
             * request.path != null이고,
             * request.path != "" 이면서
             * JWT의 signature를 확인했을 때, 우리 서버가 맞는 경우
             * 반환값을 allowed에 할당
             */
            case "publish" -> request.path() != null && !request.path().isBlank()
                    && streamService.canPublish(request.password(), request.path());

            /*
             * request.path != null이고,
             * request.path != "" 이면서
             * JWT의 signature를 확인했을 때, 우리 서버가 맞는 경우
             * 반환값을 allowed에 할당
             */
            case "read" -> request.path() != null && !request.path().isBlank()
                    && streamService.canRead(request.password(), request.path());
            default -> request.path() == null || request.path().isBlank();
        };

        return allowed ? ResponseEntity.status(HttpStatus.OK).build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /*
     * 전역 예외에서 catch 하지 않고, 컨트롤러에서 핸들링
     * MediaMTX는 Spring에게 인증을 위임하였고,
     * Spring에서 바인딩 실패 -> 인증, 인가에 관련되어 잘못된 정보 전달 -> 인증 실패 흐름으로 이어지기 때문
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Void> onValidFail(
            MethodArgumentNotValidException e
    ) {
        log.warn("| MediaMtxController.java | 필드 검증에 실패: {}", e.getBindingResult().getFieldErrors());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
