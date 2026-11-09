package com.example.shop.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 서비스/도메인에서 던지는 예외.
 * RuntimeException을 상속하는 이유:
 *  1) 호출하는 메서드마다 throws를 선언하지 않아도 된다.
 *  2) @Transactional은 기본적으로 RuntimeException에서만 롤백한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
