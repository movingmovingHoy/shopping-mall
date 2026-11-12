package com.example.shop.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 목록 검색 조건. 쿼리 파라미터(?categoryId=1&page=2&size=10)로 받는다.
 *
 * ⚠ 다른 요청 DTO와 달리 @Setter가 있다. (CLAUDE.md의 "요청 DTO는 @Getter + @NoArgsConstructor" 규칙의 예외)
 *   - @RequestBody(JSON)는 Jackson이 필드에 직접 값을 넣을 수 있어서 setter가 필요 없다.
 *   - @ModelAttribute(쿼리 파라미터)는 스프링이 기본 생성자로 객체를 만든 뒤 setter(setPage 등)로 값을 넣는다.
 *     setter가 없으면 에러도 없이 값이 무시되고, 항상 기본값(page=1, size=20)만 쓰게 된다.
 *
 * 기본값: 파라미터를 안 보내면 setter가 호출되지 않으므로 필드 초기값이 그대로 남는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequest {

    /** 선택. null이면 전체 카테고리 (Mapper XML의 <if>에서 조건이 빠진다) */
    private Long categoryId;

    /** 1부터 시작 (사람이 읽는 페이지 번호) */
    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private int page = 1;

    /** 최대 100: ?size=1000000 같은 요청으로 한 번에 너무 많이 조회하는 걸 막는다 */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;

    /**
     * SQL의 OFFSET(건너뛸 개수). 페이지 번호를 DB가 이해하는 값으로 바꾼다.
     * page=1, size=10 → 0  (처음부터 10개)
     * page=2, size=10 → 10 (10개 건너뛰고 10개 = 11~20번째)
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
