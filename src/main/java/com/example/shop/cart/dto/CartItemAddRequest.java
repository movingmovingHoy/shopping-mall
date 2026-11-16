package com.example.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 담기 요청. 누가 담는지(memberId)는 받지 않는다. 로그인 세션에서 @Login으로 꺼낸다.
 *   (본문으로 받으면 남의 장바구니에 담는 요청을 보낼 수 있다)
 * 숫자 필드를 Integer/Long으로 둔 이유는 ProductCreateRequest와 같다. (빼먹으면 null → @NotNull로 잡는다)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAddRequest {

    @NotNull(message = "상품은 필수입니다.")
    private Long productId;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;
}
