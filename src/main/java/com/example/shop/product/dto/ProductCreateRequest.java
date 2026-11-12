package com.example.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 등록 요청 (관리자).
 * 숫자 필드를 int가 아니라 Integer/Long으로 둔 이유:
 *   int는 JSON에서 값을 빼먹으면 0이 들어가서, "가격 0원"과 "가격을 안 보냄"을 구분할 수 없다.
 *   Integer는 빼먹으면 null이 되므로 @NotNull로 잡을 수 있다.
 * 최대 길이는 schema.sql의 컬럼 길이에 맞췄다. (name 100)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "재고 수량은 필수입니다.")
    @Min(value = 0, message = "재고 수량은 0개 이상이어야 합니다.")
    private Integer stockQuantity;

    // 설명은 선택 (DB 컬럼도 NULL 허용)
    private String description;
}
