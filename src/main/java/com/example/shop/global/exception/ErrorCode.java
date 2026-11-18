package com.example.shop.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 오류 종류를 한곳에서 관리한다.
 * 응답 JSON의 code 값은 enum 이름(name())을 그대로 사용한다.
 * 도메인별 오류(DUPLICATE_EMAIL 등)는 해당 기능을 만들 때 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 회원
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    // 이메일이 없을 때와 비밀번호가 틀릴 때를 구분하지 않는다 (가입된 이메일인지 알아내지 못하게)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    // 로그인이 필요한 API에 세션 없이 요청했을 때 (LoginCheckInterceptor)
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    // 세션에는 회원 id가 있는데 DB에 회원이 없을 때 (로그인 중 회원이 삭제된 경우 등)
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    // 로그인은 했지만 관리자가 아닌 회원이 /api/admin/** 에 요청했을 때 (AdminCheckInterceptor)
    // 401(누군지 모름)이 아니라 403(누군지는 알지만 권한 없음)
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),

    // 상품
    // 상품 등록 시 없는 categoryId를 보냈을 때. 미리 확인하지 않으면 FK 위반 예외가 나서 500으로 응답된다.
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),

    // 장바구니
    // 없는 항목이거나 내 장바구니의 항목이 아닐 때. 둘을 구분하지 않는다 (403으로 알려 주면 그 id가 존재한다는 정보가 샌다)
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    // 판매중지(STOPPED) 상품을 담으려 할 때. 상품은 있지만 현재 상태가 허용하지 않으므로 409
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중인 상품이 아닙니다."),

    // 주문
    // 재고보다 많이 주문했을 때. 요청 형식은 맞지만 현재 재고 상태가 허용하지 않으므로 409 (PRODUCT_NOT_ON_SALE과 같은 이유)
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    // 장바구니 주문(POST /api/orders/cart)인데 장바구니가 비어 있을 때. 주문할 상품이 없는 잘못된 요청이므로 400
    // (바로 구매에서 items가 비어 있는 경우는 @NotEmpty 검증의 INVALID_INPUT으로 처리된다)
    EMPTY_CART(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다.");

    private final HttpStatus status;
    private final String message;
}
