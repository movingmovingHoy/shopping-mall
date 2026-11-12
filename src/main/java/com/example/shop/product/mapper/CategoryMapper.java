package com.example.shop.product.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 카테고리는 상품에 딸린 데이터라 product 도메인 안에 둔다. (설계서 패키지 구조에 category 도메인이 따로 없음)
 * 지금은 상품 등록 때 "있는 카테고리인지"만 확인하면 돼서 Category 도메인 클래스는 아직 만들지 않았다.
 */
@Mapper
public interface CategoryMapper {

    boolean existsById(Long id);
}
