package com.example.shop.cart.dto;

import com.example.shop.product.domain.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * cart_item + product 조인 결과 한 줄 (조회 전용).
 * CartItem(도메인)에 상품 정보를 억지로 넣지 않고, 조회용 객체를 따로 둔다.
 *   - 도메인 CartItem은 "회원이 상품을 몇 개 담았나"만 안다.
 *   - 화면에 보여 줄 상품 이름/가격은 조회할 때만 필요하다.
 * 평평한 조인(1:1)이라 resultMap 없이 컬럼 이름 → 필드로 자동 매핑된다. (setter 없이도 MyBatis가 필드에 직접 넣는다)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItemDetail {

    private Long id;
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
    private ProductStatus productStatus;
}
