package com.example.shop.order.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.domain.OrderStatus;
import com.example.shop.order.dto.CartOrderCreateRequest;
import com.example.shop.order.dto.OrderCreateRequest;
import com.example.shop.order.dto.OrderDetailResponse;
import com.example.shop.order.dto.OrderPageResponse;
import com.example.shop.order.dto.OrderSearchRequest;
import com.example.shop.order.dto.OrderItemRequest;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.mapper.OrderItemMapper;
import com.example.shop.order.mapper.OrderMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 다른 서비스 테스트처럼 shop_test DB를 실제로 쓰고 테스트마다 롤백한다.
 * 주의: 이 테스트는 클래스에 @Transactional이 붙어 있어서, 서비스가 던진 예외로 "롤백되는지"는 확인할 수 없다.
 * (서비스의 트랜잭션이 테스트의 트랜잭션에 합류하므로 테스트가 끝날 때까지 변경이 DB에 그대로 보인다)
 * 그래서 실패 케이스는 "예외 종류"와 "실패 전에 아무것도 바뀌지 않은 경우"만 확인한다.
 */
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    @Autowired
    CartItemMapper cartItemMapper;
    @Autowired
    MemberMapper memberMapper;
    @Autowired
    ProductMapper productMapper;

    Member member;
    Product tshirt;   // 15000원, 재고 10
    Product jeans;    // 30000원, 재고 5

    @BeforeEach
    void setUp() {
        member = Member.create("order@test.com", "encoded", "홍길동");
        memberMapper.save(member);
        tshirt = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        jeans = Product.create(1L, "청바지", 30000, 5, null);
        productMapper.save(tshirt);
        productMapper.save(jeans);
    }

    // ===== 단건(직접) 주문 =====

    @Test
    void 단건_주문하면_주문이_저장되고_재고가_줄어든다() {
        OrderCreateRequest request = request(new OrderItemRequest(tshirt.getId(), 3));

        OrderResponse response = orderService.create(member.getId(), request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getTotalPrice()).isEqualTo(45000);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("반팔 티셔츠");

        assertThat(orderMapper.findById(response.getId()).orElseThrow().getMemberId()).isEqualTo(member.getId());
        List<OrderItem> items = orderItemMapper.findAllByOrderId(response.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getOrderPrice()).isEqualTo(15000);
        assertThat(items.get(0).getQuantity()).isEqualTo(3);
        assertThat(stockOf(tshirt)).isEqualTo(7);
    }

    @Test
    void 여러_상품을_주문하면_총액은_가격x수량의_합이다() {
        OrderCreateRequest request = request(
                new OrderItemRequest(tshirt.getId(), 2),
                new OrderItemRequest(jeans.getId(), 1));

        OrderResponse response = orderService.create(member.getId(), request);

        assertThat(response.getTotalPrice()).isEqualTo(15000 * 2 + 30000);
        assertThat(orderItemMapper.findAllByOrderId(response.getId())).hasSize(2);
        assertThat(stockOf(tshirt)).isEqualTo(8);
        assertThat(stockOf(jeans)).isEqualTo(4);
    }

    @Test
    void 같은_상품이_여러_줄이면_수량을_합쳐_한_줄로_주문한다() {
        OrderCreateRequest request = request(
                new OrderItemRequest(tshirt.getId(), 2),
                new OrderItemRequest(tshirt.getId(), 3));

        OrderResponse response = orderService.create(member.getId(), request);

        List<OrderItem> items = orderItemMapper.findAllByOrderId(response.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
        assertThat(stockOf(tshirt)).isEqualTo(5);
    }

    @Test
    void 단건_주문은_장바구니를_건드리지_않는다() {
        cartItemMapper.save(CartItem.create(member.getId(), jeans.getId(), 1));

        orderService.create(member.getId(), request(new OrderItemRequest(tshirt.getId(), 1)));

        assertThat(cartItemMapper.findAllForOrder(member.getId())).hasSize(1);
    }

    @Test
    void 재고와_같은_수량을_주문하면_재고가_0이_된다() {
        orderService.create(member.getId(), request(new OrderItemRequest(tshirt.getId(), 10)));

        assertThat(stockOf(tshirt)).isZero();
    }

    @Test
    void 주문_후_상품_가격이_바뀌어도_주문_가격은_그대로다() {
        OrderResponse response = orderService.create(member.getId(),
                request(new OrderItemRequest(tshirt.getId(), 1)));

        Product changed = productMapper.findById(tshirt.getId()).orElseThrow();
        changed.update(1L, "반팔 티셔츠", 99000, 9, null);
        productMapper.update(changed);

        assertThat(orderItemMapper.findAllByOrderId(response.getId()).get(0).getOrderPrice()).isEqualTo(15000);
    }

    // ===== 장바구니 주문 =====

    @Test
    void 장바구니_주문은_장바구니_전체를_주문하고_장바구니를_비운다() {
        cartItemMapper.save(CartItem.create(member.getId(), tshirt.getId(), 2));
        cartItemMapper.save(CartItem.create(member.getId(), jeans.getId(), 1));

        OrderResponse response = orderService.createFromCart(member.getId(), cartRequest());

        assertThat(response.getTotalPrice()).isEqualTo(15000 * 2 + 30000);
        assertThat(orderItemMapper.findAllByOrderId(response.getId())).hasSize(2);
        assertThat(stockOf(tshirt)).isEqualTo(8);
        assertThat(stockOf(jeans)).isEqualTo(4);
        assertThat(cartItemMapper.findAllForOrder(member.getId())).isEmpty();
    }

    @Test
    void 장바구니_주문은_남의_장바구니를_건드리지_않는다() {
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);
        cartItemMapper.save(CartItem.create(member.getId(), tshirt.getId(), 1));
        cartItemMapper.save(CartItem.create(other.getId(), jeans.getId(), 1));

        orderService.createFromCart(member.getId(), cartRequest());

        assertThat(cartItemMapper.findAllForOrder(other.getId())).hasSize(1);
        assertThat(stockOf(jeans)).isEqualTo(5);   // 남의 장바구니 상품은 주문되지 않았다
    }

    @Test
    void 장바구니가_비어_있으면_EMPTY_CART() {
        assertThatThrownBy(() -> orderService.createFromCart(member.getId(), cartRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPTY_CART);
    }

    @Test
    void 장바구니에_재고_부족_상품이_있으면_주문_실패하고_장바구니는_그대로다() {
        cartItemMapper.save(CartItem.create(member.getId(), jeans.getId(), 6));   // 재고 5

        assertThatThrownBy(() -> orderService.createFromCart(member.getId(), cartRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);
        assertThat(cartItemMapper.findAllForOrder(member.getId())).hasSize(1);
    }

    // ===== 실패 케이스 =====

    @Test
    void 재고보다_많이_주문하면_OUT_OF_STOCK이고_재고는_그대로다() {
        assertThatThrownBy(() -> orderService.create(member.getId(),
                request(new OrderItemRequest(tshirt.getId(), 11))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);
        assertThat(stockOf(tshirt)).isEqualTo(10);
    }

    @Test
    void 판매중지_상품이_있으면_PRODUCT_NOT_ON_SALE이고_재고는_그대로다() {
        tshirt.stop();
        productMapper.updateStatus(tshirt);

        assertThatThrownBy(() -> orderService.create(member.getId(),
                request(new OrderItemRequest(tshirt.getId(), 1))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE);
        assertThat(stockOf(tshirt)).isEqualTo(10);
    }

    @Test
    void 없는_상품이면_PRODUCT_NOT_FOUND() {
        assertThatThrownBy(() -> orderService.create(member.getId(),
                request(new OrderItemRequest(999_999L, 1))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    // ===== 주문 목록/상세 =====

    @Test
    void 주문_목록은_내_주문만_최근_순으로_페이징해서_준다() {
        OrderResponse first = orderService.create(member.getId(), request(new OrderItemRequest(tshirt.getId(), 1)));
        OrderResponse second = orderService.create(member.getId(), request(
                new OrderItemRequest(tshirt.getId(), 1), new OrderItemRequest(jeans.getId(), 1)));
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);
        orderService.create(other.getId(), request(new OrderItemRequest(jeans.getId(), 1)));

        OrderSearchRequest search = new OrderSearchRequest();
        search.setSize(1);
        OrderPageResponse page1 = orderService.getOrders(member.getId(), search);

        assertThat(page1.getTotalCount()).isEqualTo(2);   // 남의 주문은 세지 않는다
        assertThat(page1.getOrders()).hasSize(1);
        assertThat(page1.getOrders().get(0).getId()).isEqualTo(second.getId());
        assertThat(page1.getOrders().get(0).getItemCount()).isEqualTo(2);

        search.setPage(2);
        OrderPageResponse page2 = orderService.getOrders(member.getId(), search);
        assertThat(page2.getOrders()).extracting(o -> o.getId()).containsExactly(first.getId());
    }

    @Test
    void 주문이_없으면_빈_목록() {
        OrderPageResponse page = orderService.getOrders(member.getId(), new OrderSearchRequest());

        assertThat(page.getOrders()).isEmpty();
        assertThat(page.getTotalCount()).isZero();
    }

    @Test
    void 주문_상세는_주문_시점_가격과_상품을_준다() {
        OrderResponse created = orderService.create(member.getId(), request(
                new OrderItemRequest(tshirt.getId(), 2), new OrderItemRequest(jeans.getId(), 1)));

        OrderDetailResponse detail = orderService.getOrder(member.getId(), created.getId());

        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(detail.getTotalPrice()).isEqualTo(60000);
        assertThat(detail.getOrderedAt()).isNotNull();
        assertThat(detail.getItems()).hasSize(2);
        assertThat(detail.getItems().get(0).getProductName()).isEqualTo("반팔 티셔츠");
        assertThat(detail.getItems().get(0).getOrderPrice()).isEqualTo(15000);
        assertThat(detail.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void 남의_주문_상세는_ORDER_NOT_FOUND() {
        OrderResponse created = orderService.create(member.getId(), request(new OrderItemRequest(tshirt.getId(), 1)));
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);

        assertThatThrownBy(() -> orderService.getOrder(other.getId(), created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void 없는_주문_상세는_ORDER_NOT_FOUND() {
        assertThatThrownBy(() -> orderService.getOrder(member.getId(), 999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    private OrderCreateRequest request(OrderItemRequest... items) {
        return new OrderCreateRequest("홍길동", "서울시 어딘가 1", List.of(items));
    }

    private CartOrderCreateRequest cartRequest() {
        return new CartOrderCreateRequest("홍길동", "서울시");
    }

    private int stockOf(Product product) {
        return productMapper.findById(product.getId()).orElseThrow().getStockQuantity();
    }
}
