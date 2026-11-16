package com.example.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수량 변경 요청. 담기와 달리 "더할 수량"이 아니라 "바꿀 수량"이다. (3을 보내면 수량이 3이 된다)
 * 수량을 0으로 줄여 지우는 건 허용하지 않는다. 삭제는 DELETE API로 한다. (DB도 quantity > 0 제약이 있다)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemUpdateRequest {

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;
}
