package com.umc.bscene.domain.session.exception;

import com.umc.bscene.domain.session.controller.BandProfileController;
import com.umc.bscene.domain.session.controller.SessionRecruitmentController;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {
        BandProfileController.class,
        SessionRecruitmentController.class
})
@Slf4j
public class BandProfileExceptionHandler {

    @ExceptionHandler(BandProfileException.class)
    public ResponseEntity<ErrorResponse<?>> handleBandProfileException(
            BandProfileException e
    ) {
        ErrorResponse<?> errorResponse =
                ErrorResponse.from(e.getBaseResponseCode());

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        ErrorResponse<?> errorResponse =
                ErrorResponse.from(SessionErrorCode.INVALID_SESSION_RECRUITMENT_REQUEST);

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
}