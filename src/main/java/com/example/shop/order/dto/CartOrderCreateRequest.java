package com.example.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 주문 요청 (POST /api/orders/cart). 주문할 상품은 받지 않는다. 서버가 내 장바구니(cart_item)에서 꺼낸다.
 * 배송 정보는 OrderCreateRequest와 같은 규칙이다. 필드 2개라 상속으로 묶지 않고 각자 둔다. (요청마다 형태가 바로 보이도록)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartOrderCreateRequest {

    @NotBlank(message = "받는 사람 이름은 필수입니다.")
    @Size(max = 50, message = "받는 사람 이름은 50자 이하여야 합니다.")
    private String receiverName;

    @NotBlank(message = "배송 주소는 필수입니다.")
    @Size(max = 255, message = "배송 주소는 255자 이하여야 합니다.")
    private String receiverAddress;
}
