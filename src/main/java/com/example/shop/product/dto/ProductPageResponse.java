package com.example.shop.product.dto;

import lombok.Getter;

import java.util.List;

/**
 * 상품 목록 응답 (한 페이지).
 * {
 *   "products": [ {상품}, {상품}, ... ],
 *   "page": 1, "size": 20, "totalCount": 35
 * }
 * 목록만 주지 않고 page/size/totalCount를 함께 주는 이유:
 *   클라이언트가 "총 2페이지(35 / 20 올림)" 같은 페이지 번호 UI를 그리려면 전체 개수가 필요하다.
 * 목록의 각 항목은 상세 조회와 같은 ProductResponse를 재사용한다.
 */
@Getter
public final class ProductPageResponse {

    private final List<ProductResponse> products;
    private final int page;
    private final int size;
    private final long totalCount;

    private ProductPageResponse(List<ProductResponse> products, int page, int size, long totalCount) {
        this.products = products;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
    }

    /** 값 여러 개를 모아 만들 때는 관례상 of를 쓴다 (객체 하나를 변환할 때는 from) */
    public static ProductPageResponse of(List<ProductResponse> products, int page, int size, long totalCount) {
        return new ProductPageResponse(products, page, size, totalCount);
    }
}
