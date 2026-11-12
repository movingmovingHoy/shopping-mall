package com.example.shop.product.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 도메인. Member와 같은 방식이다.
 * - setter를 열지 않는다. 새 상품은 create()로만 만든다.
 * - 재고 차감, 판매중지 같은 상태 변경은 나중에 의미 있는 메서드(decreaseStock, stop 등)로 추가한다.
 * - 카테고리는 Category 객체가 아니라 id만 가진다. MyBatis는 연관 객체를 자동으로 채워 주지 않으므로
 *   테이블 컬럼(category_id) 그대로 두는 편이 단순하다. 카테고리 이름이 필요하면 그때 조인해서 DTO로 받는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    private Long id;
    private Long categoryId;
    private String name;
    private int price;
    private int stockQuantity;
    private ProductStatus status;
    private String description;
    private LocalDateTime createdAt;

    /** 상품 등록용. 등록하면 바로 판매중(ON_SALE)이다. */
    public static Product create(Long categoryId, String name, int price, int stockQuantity, String description) {
        Product product = new Product();
        product.categoryId = categoryId;
        product.name = name;
        product.price = price;
        product.stockQuantity = stockQuantity;
        product.description = description;
        product.status = ProductStatus.ON_SALE;
        return product;
    }
}
