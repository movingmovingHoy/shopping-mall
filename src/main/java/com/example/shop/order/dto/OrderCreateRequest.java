package com.example.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청. 누가 주문하는지(memberId)는 받지 않는다. 로그인 세션에서 @Login으로 꺼낸다.
 *
 * items로 두 가지 주문을 구분한다.
 *   - items가 있으면: 그 상품들만 주문 (단건/직접 주문). 장바구니는 건드리지 않는다.
 *   - items가 없거나 비어 있으면: 내 장바구니 전체를 주문하고, 주문 후 장바구니를 비운다.
 * 글자 수 제한은 orders 테이블 컬럼 길이(receiver_name 50, receiver_address 255)와 맞춘다. (넘으면 DB 오류로 500이 나므로 미리 400으로 막는다)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "받는 사람 이름은 필수입니다.")
    @Size(max = 50, message = "받는 사람 이름은 50자 이하여야 합니다.")
    private String receiverName;

    @NotBlank(message = "배송 주소는 필수입니다.")
    @Size(max = 255, message = "배송 주소는 255자 이하여야 합니다.")
    private String receiverAddress;

    @Valid   // 리스트 안의 OrderItemRequest까지 검증한다 (없으면 안쪽 @NotNull/@Min이 동작하지 않는다)
    private List<OrderItemRequest> items;

    /** 장바구니 전체 주문인지. items가 null이거나 비어 있으면 true */
    public boolean isCartOrder() {
        return items == null || items.isEmpty();
    }
}
