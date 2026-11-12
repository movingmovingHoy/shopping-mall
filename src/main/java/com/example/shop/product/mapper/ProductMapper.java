package com.example.shop.product.mapper;

import com.example.shop.product.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/** SQL은 resources/mapper/product/ProductMapper.xml에 있다. */
@Mapper
public interface ProductMapper {

    /** 저장 후 DB가 만든 id가 product.id에 채워진다 (useGeneratedKeys) */
    void save(Product product);

    /** 지금은 테스트에서 저장 결과를 확인하는 데 쓰고, 상품 상세 API에서도 쓸 예정이다 */
    Optional<Product> findById(Long id);

    /** product.status 값으로 DB를 바꾼다. 도메인 메서드(stop 등)로 상태를 바꾼 뒤 호출한다 */
    void updateStatus(Product product);

    /** 상품 정보(카테고리, 이름, 가격, 재고, 설명)를 product 값으로 바꾼다. status는 건드리지 않는다 */
    void update(Product product);

    /**
     * 판매중(ON_SALE) 상품 목록, 최신순. categoryId가 null이면 전체.
     * 파라미터가 여러 개라서 @Param으로 이름표를 붙인다. XML에서는 #{categoryId}, #{size}, #{offset}으로 쓴다.
     * (파라미터가 하나일 때는 이름이 상관없지만, 여러 개면 MyBatis가 어느 값이 어느 자리인지 알 수 없다)
     */
    List<Product> findAll(@Param("categoryId") Long categoryId,
                          @Param("size") int size,
                          @Param("offset") int offset);

    /** findAll과 같은 조건의 전체 개수 (LIMIT 없이) */
    long countAll(@Param("categoryId") Long categoryId);
}
