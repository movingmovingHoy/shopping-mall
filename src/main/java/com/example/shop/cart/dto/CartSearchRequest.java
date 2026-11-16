package com.example.shop.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장바구니 조회 조건. 쿼리 파라미터(?page=2&size=10)로 받는다.
 * ProductSearchRequest와 같은 이유로 @Setter가 있다. (@ModelAttribute는 setter로 값을 넣는다)
 * 어느 회원의 장바구니인지는 받지 않는다. 로그인 세션에서 @Login으로 꺼낸다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CartSearchRequest {

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;

    /** SQL의 OFFSET. page=2, size=10 → 10 */
    public int getOffset() {
        return (page - 1) * size;
    }
}
