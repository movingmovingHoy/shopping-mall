package com.example.shop.product.dto;

import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import lombok.Getter;

/**
 * 상품 응답. 상품 등록 결과로 쓰고, 나중에 상품 상세 조회에서도 쓸 예정이다.
 * createdAt은 넣지 않았다. INSERT 직후 Product 객체에는 DB 기본값(CURRENT_TIMESTAMP)이 채워지지 않아 null이기 때문이다.
 * (MemberResponse도 같은 이유로 createdAt이 없다)
 */
@Getter
public final class ProductResponse {

    private final Long id;
    private final Long categoryId;
    private final String name;
    private final int price;
    private final int stockQuantity;
    private final ProductStatus status;
    private final String description;

    private ProductResponse(Long id, Long categoryId, String name, int price,
                            int stockQuantity, ProductStatus status, String description) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.description = description;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getCategoryId(), product.getName(), product.getPrice(),
                product.getStockQuantity(), product.getStatus(), product.getDescription());
    }
}
