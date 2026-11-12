package com.example.shop.product.domain;

/**
 * 상품 판매 상태. DB의 product.status(VARCHAR)에는 enum 이름이 그대로 저장된다.
 * 상품은 물리 삭제하지 않고 STOPPED로 바꿔서 판매중지한다.
 * (과거 주문의 order_item이 product를 FK로 참조하므로 지우면 주문 내역이 깨진다)
 */
public enum ProductStatus {
    ON_SALE,
    STOPPED
}
