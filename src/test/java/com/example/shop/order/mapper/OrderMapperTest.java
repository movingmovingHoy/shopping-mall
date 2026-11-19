package com.example.shop.order.mapper;

import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.domain.OrderStatus;
import com.example.shop.order.dto.OrderDetail;
import com.example.shop.order.dto.OrderSummary;
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

    // ===== 목록 =====

    @Test
    void 주문_목록은_최근_주문_순이고_주문상품_개수가_들어_있다() {
        Order first = saveOrder(member, 15000, 1);
        Order second = saveOrder(member, 30000, 2);

        List<OrderSummary> orders = orderMapper.findAllByMemberId(member.getId(), 10, 0);

        // 같은 초에 저장되므로 ordered_at이 같다 → id DESC로 나중 주문이 먼저
        assertThat(orders).extracting(OrderSummary::getId).containsExactly(second.getId(), first.getId());
        assertThat(orders.get(0).getItemCount()).isEqualTo(2);
        assertThat(orders.get(0).getTotalPrice()).isEqualTo(30000);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(orders.get(0).getOrderedAt()).isNotNull();
        assertThat(orders.get(1).getItemCount()).isEqualTo(1);
    }

    @Test
    void 주문상품이_여러_개여도_LIMIT은_주문_건수_기준이다() {
        saveOrder(member, 1000, 3);
        saveOrder(member, 1000, 3);
        saveOrder(member, 1000, 3);

        // 상품까지 JOIN 했다면 한 주문이 3행이 되어 2행에서 잘렸을 것이다
        assertThat(orderMapper.findAllByMemberId(member.getId(), 2, 0)).hasSize(2);
        assertThat(orderMapper.findAllByMemberId(member.getId(), 2, 2)).hasSize(1);
        assertThat(orderMapper.countByMemberId(member.getId())).isEqualTo(3);
    }

    @Test
    void 주문_목록에_남의_주문은_나오지_않는다() {
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);
        saveOrder(other, 1000, 1);

        assertThat(orderMapper.findAllByMemberId(member.getId(), 10, 0)).isEmpty();
        assertThat(orderMapper.countByMemberId(member.getId())).isZero();
    }

    // ===== 상세 =====

    @Test
    void 주문_상세는_주문상품과_상품_이름을_함께_담는다() {
        Product jeans = Product.create(1L, "청바지", 30000, 5, null);
        productMapper.save(jeans);
        Order order = Order.create(member.getId(), 60000, "홍길동", "서울시 어딘가 1");
        orderMapper.save(order);
        orderItemMapper.save(OrderItem.create(order.getId(), product.getId(), 15000, 2));
        orderItemMapper.save(OrderItem.create(order.getId(), jeans.getId(), 30000, 1));

        OrderDetail detail = orderMapper.findDetailByIdAndMemberId(order.getId(), member.getId()).orElseThrow();

        // JOIN 결과 2행이 주문 1개로 합쳐지고, 상품 2개가 items에 들어갔는지 (resultMap의 <id>/<collection>)
        assertThat(detail.getId()).isEqualTo(order.getId());
        assertThat(detail.getReceiverName()).isEqualTo("홍길동");
        assertThat(detail.getOrderedAt()).isNotNull();
        assertThat(detail.getItems()).hasSize(2);
        assertThat(detail.getItems().get(0).getProductName()).isEqualTo("반팔 티셔츠");
        assertThat(detail.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(detail.getItems().get(1).getProductName()).isEqualTo("청바지");
        assertThat(detail.getItems().get(1).getProductId()).isEqualTo(jeans.getId());
    }

    @Test
    void 주문_상세의_가격은_상품_현재_가격이_아니라_주문_시점_가격이다() {
        Order order = Order.create(member.getId(), 12000, "홍길동", "서울시 어딘가 1");
        orderMapper.save(order);
        orderItemMapper.save(OrderItem.create(order.getId(), product.getId(), 12000, 1));   // 상품은 지금 15000원

        OrderDetail detail = orderMapper.findDetailByIdAndMemberId(order.getId(), member.getId()).orElseThrow();

        assertThat(detail.getItems().get(0).getOrderPrice()).isEqualTo(12000);
    }

    @Test
    void 남의_주문_상세는_조회되지_않는다() {
        Order order = saveOrder(member, 1000, 1);
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);

        assertThat(orderMapper.findDetailByIdAndMemberId(order.getId(), other.getId())).isEmpty();
        assertThat(orderMapper.findDetailByIdAndMemberId(999_999L, member.getId())).isEmpty();
    }

    @Test
    void 없는_회원으로_주문을_저장하면_FK_위반() {
        Order order = Order.create(999_999L, 1000, "홍길동", "서울시 어딘가 1");

        assertThatThrownBy(() -> orderMapper.save(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 주문 1건 + 같은 상품으로 주문상품 itemCount줄. (테스트용이라 상품이 겹쳐도 상관없다) */
    private Order saveOrder(Member owner, int totalPrice, int itemCount) {
        Order order = Order.create(owner.getId(), totalPrice, "홍길동", "서울시 어딘가 1");
        orderMapper.save(order);
        for (int i = 0; i < itemCount; i++) {
            orderItemMapper.save(OrderItem.create(order.getId(), product.getId(), 1000, 1));
        }
        return order;
    }
}
