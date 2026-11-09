package com.example.shop.global.exception;

import lombok.Getter;

import java.util.List;

/**
 * 모든 오류 응답의 공통 JSON 형식.
 * {
 *   "code": "INVALID_INPUT",
 *   "message": "입력값이 올바르지 않습니다.",
 *   "errors": [ { "field": "email", "message": "이메일 형식이 아닙니다." } ]
 * }
 * errors는 검증 오류일 때만 채워지고, 그 외에는 빈 배열이다.
 * (null 대신 빈 배열이면 클라이언트가 null 체크 없이 바로 순회할 수 있다)
 *
 * - final 클래스 + final 필드 + setter 없음 → 한 번 만들면 값이 바뀌지 않는다(불변).
 * - @Getter: Jackson이 getter로 값을 읽어 JSON을 만들기 때문에 필요하다.
 * - 생성자는 private → 객체는 반드시 of(...)로만 만들게 강제한다.
 */
@Getter
public final class ErrorResponse {

    private final String code;
    private final String message;
    private final List<InvalidField> errors;

    private ErrorResponse(String code, String message, List<InvalidField> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<InvalidField> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }

    /** 검증 오류 목록의 한 줄 (어떤 필드가, 왜 틀렸는지) */
    @Getter
    public static final class InvalidField {

        private final String field;
        private final String message;

        public InvalidField(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
