package com.example.shop.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 단건 주문에서 "어떤 상품을 몇 개" 주문할지. 가격은 받지 않는다. 클라이언트가 보낸 가격을 믿으면 안 되므로 서버가 DB의 현재 가격을 쓴다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "상품은 필수입니다.")
    private Long productId;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;
}
