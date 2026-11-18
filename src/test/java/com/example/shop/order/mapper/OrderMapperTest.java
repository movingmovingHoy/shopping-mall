package com.example.shop.order.mapper;

import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.domain.OrderStatus;
import com.example.shop.product.domain.Product;
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

/** 다른 Mapper 테스트와 같은 설정. orders는 회원, order_item은 주문/상품을 FK로 참조하므로 먼저 저장해 둔다. (테스트 후 롤백) */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderMapperTest {

    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    @Autowired
    MemberMapper memberMapper;
    @Autowired
    ProductMapper productMapper;

    Member member;
    Product product;

    @BeforeEach
    void setUp() {
        member = Member.create("order@test.com", "encoded", "홍길동");
        memberMapper.save(member);
        product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        productMapper.save(product);
    }

    @Test
    void 주문을_저장하면_id가_채워지고_CREATED로_조회된다() {
        Order order = Order.create(member.getId(), 30000, "홍길동", "서울시 어딘가 1");

        orderMapper.save(order);

        assertThat(order.getId()).isNotNull();
        Order found = orderMapper.findById(order.getId()).orElseThrow();
        assertThat(found.getMemberId()).isEqualTo(member.getId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.CREATED);   // 문자열 → enum 매핑 확인
        assertThat(found.getTotalPrice()).isEqualTo(30000);
        assertThat(found.getReceiverName()).isEqualTo("홍길동");
        assertThat(found.getReceiverAddress()).isEqualTo("서울시 어딘가 1");
        assertThat(found.getOrderedAt()).isNotNull();                   // DB 기본값(CURRENT_TIMESTAMP)
    }

    @Test
    void 없는_주문은_조회되지_않는다() {
        assertThat(orderMapper.findById(999_999L)).isEmpty();
    }

    @Test
    void 주문상품을_저장하면_주문_시점_가격과_함께_조회된다() {
        Order order = Order.create(member.getId(), 30000, "홍길동", "서울시 어딘가 1");
        orderMapper.save(order);

        OrderItem item = OrderItem.create(order.getId(), product.getId(), 15000, 2);
        orderItemMapper.save(item);

        assertThat(item.getId()).isNotNull();
        List<OrderItem> items = orderItemMapper.findAllByOrderId(order.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(product.getId());
        assertThat(items.get(0).getOrderPrice()).isEqualTo(15000);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void 없는_회원으로_주문을_저장하면_FK_위반() {
        Order order = Order.create(999_999L, 1000, "홍길동", "서울시 어딘가 1");

        assertThatThrownBy(() -> orderMapper.save(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
