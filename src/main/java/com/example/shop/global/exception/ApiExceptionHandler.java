package com.example.shop.global.exception;

import com.example.shop.global.exception.ErrorResponse.InvalidField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 컨트롤러에서 던져진 예외를 잡아 ErrorResponse(JSON)로 변환한다.
 * 덕분에 컨트롤러/서비스에는 try-catch가 필요 없다.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 비즈니스 규칙 위반 (중복 가입, 재고 부족 등) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.info("BusinessException: {}", e.getErrorCode());
        return toResponse(e.getErrorCode());
    }

    /** @Valid 검증 실패 → 어떤 필드가 왜 틀렸는지 목록으로 응답 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<InvalidField> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new InvalidField(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorCode code = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, errors));
    }

    /** JSON 문법 오류, 타입 불일치(?page=abc 같은 경우) */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        log.info("Bad request: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    /*
     * 아래 두 개가 없으면 마지막 Exception 핸들러에 잡혀서
     * 없는 URL이나 잘못된 HTTP 메서드 요청도 500으로 응답하게 된다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return toResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /** 예상하지 못한 오류: 서버에는 스택트레이스를 남기고, 클라이언트에는 내부 정보를 숨긴다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected exception", e);
        return toResponse(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
    }
}
