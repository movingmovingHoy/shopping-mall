package com.example.shop.cart.mapper;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemDetail;
import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import com.example.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

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

    // ===== 장바구니 조회 =====

    @Test
    void 조회하면_상품정보와_함께_최근에_담은_순으로_나온다() {
        Product pants = Product.create(1L, "청바지", 30000, 5, null);
        productMapper.save(pants);
        cartItemMapper.save(CartItem.create(member.getId(), product.getId(), 2));
        cartItemMapper.save(CartItem.create(member.getId(), pants.getId(), 1));

        List<CartItemDetail> list = cartItemMapper.findAllByMemberId(member.getId(), 10, 0);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getProductName()).isEqualTo("청바지");   // 나중에 담은 것이 먼저
        assertThat(list.get(1).getProductName()).isEqualTo("반팔 티셔츠");
        CartItemDetail tshirt = list.get(1);
        assertThat(tshirt.getId()).isNotNull();
        assertThat(tshirt.getProductId()).isEqualTo(product.getId());
        assertThat(tshirt.getPrice()).isEqualTo(15000);
        assertThat(tshirt.getQuantity()).isEqualTo(2);
        assertThat(tshirt.getProductStatus()).isEqualTo(ProductStatus.ON_SALE);   // 별칭 product_status → enum 매핑
    }

    @Test
    void 다른_회원의_장바구니는_섞이지_않는다() {
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);
        cartItemMapper.save(CartItem.create(member.getId(), product.getId(), 1));
        cartItemMapper.save(CartItem.create(other.getId(), product.getId(), 7));

        assertThat(cartItemMapper.findAllByMemberId(member.getId(), 10, 0)).hasSize(1);
        assertThat(cartItemMapper.countByMemberId(member.getId())).isEqualTo(1);
        assertThat(cartItemMapper.sumOnSalePriceByMemberId(member.getId())).isEqualTo(15000);
    }

    @Test
    void 페이징하면_size만큼만_가져오고_개수는_전체를_센다() {
        for (int i = 0; i < 3; i++) {
            Product p = Product.create(1L, "상품" + i, 1000, 5, null);
            productMapper.save(p);
            cartItemMapper.save(CartItem.create(member.getId(), p.getId(), 1));
        }

        List<CartItemDetail> page2 = cartItemMapper.findAllByMemberId(member.getId(), 2, 2);

        assertThat(page2).hasSize(1);   // 3개 중 2개를 건너뛰고 나머지 1개
        assertThat(cartItemMapper.countByMemberId(member.getId())).isEqualTo(3);
    }

    @Test
    void 합계는_판매중_상품만_가격x수량으로_더한다() {
        Product stopped = Product.create(1L, "단종 상품", 50000, 5, null);
        productMapper.save(stopped);
        cartItemMapper.save(CartItem.create(member.getId(), product.getId(), 2));   // 15000 x 2
        cartItemMapper.save(CartItem.create(member.getId(), stopped.getId(), 1));
        stopped.stop();
        productMapper.updateStatus(stopped);

        assertThat(cartItemMapper.sumOnSalePriceByMemberId(member.getId())).isEqualTo(30000);
        assertThat(cartItemMapper.findAllByMemberId(member.getId(), 10, 0)).hasSize(2);   // 목록에는 중지 상품도 나온다
    }

    @Test
    void 장바구니가_비어_있으면_목록은_비고_합계는_0이다() {
        assertThat(cartItemMapper.findAllByMemberId(member.getId(), 10, 0)).isEmpty();
        assertThat(cartItemMapper.countByMemberId(member.getId())).isZero();
        assertThat(cartItemMapper.sumOnSalePriceByMemberId(member.getId())).isZero();   // SUM의 NULL이 0으로
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
