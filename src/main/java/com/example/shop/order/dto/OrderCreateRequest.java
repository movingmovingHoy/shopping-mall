package com.example.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 바로 구매 요청 (POST /api/orders). 요청에 적힌 상품만 주문하고 장바구니는 건드리지 않는다.
 * 장바구니 주문은 CartOrderCreateRequest + POST /api/orders/cart 로 따로 받는다.
 * 누가 주문하는지(memberId)는 받지 않는다. 로그인 세션에서 @Login으로 꺼낸다.
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

    // 바로 구매는 상품이 반드시 있어야 한다. 비어 있으면 400 (예전처럼 장바구니 주문으로 바뀌지 않는다)
    @NotEmpty(message = "주문할 상품을 선택해 주세요.")
    @Valid   // 리스트 안의 OrderItemRequest까지 검증한다 (없으면 안쪽 @NotNull/@Min이 동작하지 않는다)
    private List<OrderItemRequest> items;
}
