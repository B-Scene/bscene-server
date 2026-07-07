package com.umc.bscene.domain.session.exception;

import com.umc.bscene.domain.session.controller.BandProfileController;
import com.umc.bscene.domain.session.enums.code.SessionProfileErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BandProfileController.class)
@Slf4j
public class BandProfileExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        log.error("SessionProfile MethodArgumentNotValidException : {}", e.getMessage(), e);

        ErrorResponse<?> errorResponse =
                ErrorResponse.from(SessionProfileErrorCode.INVALID_SESSION_PROFILE_REQUEST);

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
}