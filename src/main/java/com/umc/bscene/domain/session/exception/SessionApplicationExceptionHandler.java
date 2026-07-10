package com.umc.bscene.domain.session.exception;

import com.umc.bscene.domain.session.controller.SessionApplicationController;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.controller.SessionRecruitmentController;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {
        SessionApplicationController.class,
        SessionRecruitmentController.class
})
@Slf4j
public class SessionApplicationExceptionHandler {

    @ExceptionHandler(SessionApplicationException.class)
    public ResponseEntity<ErrorResponse<?>> handleSessionApplicationException(
            SessionApplicationException e
    ) {
        ErrorResponse<?> errorResponse =
                ErrorResponse.from(e.getBaseResponseCode());

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        SessionErrorCode errorCode =
                e.getBindingResult().getTarget() instanceof MySessionApplicationUpdateRequest
                        ? SessionErrorCode.INVALID_SESSION_APPLICATION_REQUEST
                        : SessionErrorCode.INVALID_SESSION_RECRUITMENT_REQUEST;

        ErrorResponse<?> errorResponse = ErrorResponse.from(errorCode);

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
}
