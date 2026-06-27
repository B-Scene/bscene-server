package com.umc.bscene.global.exception;

import com.umc.bscene.global.response.ErrorResponse;
import com.umc.bscene.global.response.code.BaseResponseCode;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * RequestBody 객체에 Validation 할 때, 필드에 선언된 검증 조건 위반 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        log.error("MethodArgumentNotValidException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.of(
                GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY,
                e.getFieldError().getDefaultMessage()
        );
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * QueryParam, ModelAttribute 객체에 Validation 할 때, 필드에 선언된 검증 조건 위반 시 발생
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse<?>> handleBindException(BindException e) {
        log.error("BindException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.of(
                GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY,
                e.getFieldError().getDefaultMessage()
        );
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * RequestBody 객체의 JSON 파싱 실패 (JSON 문법 오류, 타입 불일치, 필수 바디 누락 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.error("HttpMessageNotReadableException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_BODY);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 요청 파라미터가 타입 변환에 실패했을 때 (enum type 불일치, 쿼리/경로 파라미터의 타입 변환이 실패)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.error("MethodArgumentTypeMismatchException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 필수 쿼리 파라미터 누락 시 발생
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        log.error("MissingServletRequestParameterException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.of(
                GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER,
                e.getParameterName() + " 파라미터가 필요합니다."
        );
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * Request Part 누락 시 발생
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse<?>> handleMissingServletRequestPartException(
            MissingServletRequestPartException e
    ) {
        log.error("MissingServletRequestPartException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.INVALID_HTTP_MESSAGE_PARAMETER);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 지원하지 않는 HTTP 메소드를 호출할 경우
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        log.error("HttpRequestMethodNotSupportedException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.UNSUPPORTED_HTTP_METHOD);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 존재하지 않는 엔드포인트 호출 시 발생
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse<?>> handleNoHandlerFoundException(
            NoHandlerFoundException e
    ) {
        log.error("NoHandlerFoundException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.NOT_FOUND_ENDPOINT);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 정적 리소스도 찾지 못했을 때 발생
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse<?>> handleNoResourceFoundException(
            NoResourceFoundException e
    ) {
        log.error("NoResourceFoundException : {}", e.getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(GeneralErrorCode.NOT_FOUND_ENDPOINT);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    /**
     * 비즈니스 로직 에러
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse<?>> handleBaseException(
            BaseException e
    ) {
        log.error("BaseException : {}", e.getBaseResponseCode().getMessage(), e);
        ErrorResponse<?> errorResponse = ErrorResponse.from(e.getBaseResponseCode());
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<?>> handleException(
            Exception e
    ) {
        log.error("Exception : {}", e.getMessage(), e);
        ErrorResponse<Object> errorResponse = ErrorResponse.from(GeneralErrorCode.SERVER_ERROR);
        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
}

