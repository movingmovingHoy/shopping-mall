package com.example.shop.cart.mapper;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 다른 Mapper 테스트와 같은 설정. cart_item은 회원/상품을 FK로 참조하므로 먼저 저장해 둔다. (테스트 후 롤백) */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartItemMapperTest {

    @Autowired
    CartItemMapper cartItemMapper;
    @Autowired
    MemberMapper memberMapper;
    @Autowired
    ProductMapper productMapper;

    Member member;
    Product product;

    @BeforeEach
    void setUp() {
        member = Member.create("cart@test.com", "encoded", "홍길동");
        memberMapper.save(member);
        product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        productMapper.save(product);
    }

    @Test
    void 저장하면_id가_채워지고_회원과_상품으로_조회할_수_있다() {
        CartItem item = CartItem.create(member.getId(), product.getId(), 2);

        cartItemMapper.save(item);

        assertThat(item.getId()).isNotNull();
        CartItem found = cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId()).orElseThrow();
        assertThat(found.getMemberId()).isEqualTo(member.getId());   // member_id → memberId 매핑 확인
        assertThat(found.getProductId()).isEqualTo(product.getId());
        assertThat(found.getQuantity()).isEqualTo(2);
    }

    @Test
    void 담지_않은_상품은_조회되지_않는다() {
        assertThat(cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId())).isEmpty();
    }

    @Test
    void 수량을_바꾸면_DB에_반영된다() {
        CartItem item = CartItem.create(member.getId(), product.getId(), 2);
        cartItemMapper.save(item);

        item.addQuantity(3);
        cartItemMapper.updateQuantity(item);

        assertThat(cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId())
                .orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void 같은_회원이_같은_상품을_두_줄로_저장하면_유니크_제약으로_실패한다() {
        cartItemMapper.save(CartItem.create(member.getId(), product.getId(), 1));

        assertThatThrownBy(() -> cartItemMapper.save(CartItem.create(member.getId(), product.getId(), 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
