package com.example.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 수정 요청 (관리자). PUT이므로 "전체 교체": 모든 값을 다 보내야 한다.
 * (일부만 보내고 나머지는 그대로 두는 건 PATCH의 방식이다)
 *
 * 지금은 ProductCreateRequest와 필드가 똑같지만 따로 만들었다.
 *   등록과 수정은 나중에 달라질 수 있다. (예: 수정 때는 카테고리 변경 금지, 수정 사유 추가)
 *   하나를 같이 쓰면 한쪽 규칙을 바꿀 때 다른 쪽까지 바뀐다.
 * status는 받지 않는다. 판매중지는 POST /api/admin/products/{id}/stop 으로만 한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

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

    // 선택. PUT은 전체 교체라서 보내지 않으면 기존 설명이 지워진다(null).
    private String description;
}
