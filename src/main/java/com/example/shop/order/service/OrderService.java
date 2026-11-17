package com.example.shop.order.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.dto.OrderCreateRequest;
import com.example.shop.order.dto.OrderItemRequest;
import com.example.shop.order.dto.OrderItemResponse;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.mapper.OrderItemMapper;
import com.example.shop.order.mapper.OrderMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import com.example.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;

    /**
     * 주문 생성. 아래 전체가 하나의 트랜잭션이다. 중간에 예외가 나면(재고 부족 등) 이미 깎은 재고도 모두 롤백된다.
     *  1) 주문할 (상품, 수량) 목록을 정한다. items가 있으면 그것, 없으면 내 장바구니 전체
     *  2) 상품마다: 존재 확인(404) → 판매중 확인(409) → 재고 차감(부족하면 409) → 재고 UPDATE
     *  3) 총액 계산 → orders 저장 → order_item 저장 (이때 현재 가격을 order_price에 복사)
     *  4) 장바구니 주문이었다면 장바구니를 비운다
     *
     * 하나라도 실패하면 주문 전체가 실패한다. (일부 상품만 주문되지 않는다)
     * 재고 동시성 제어(동시에 같은 상품을 주문하는 경우)는 MVP 범위가 아니라서 하지 않았다. 확장 단계에서 다룬다.
     */
    @Transactional
    public OrderResponse create(Long memberId, OrderCreateRequest request) {
        boolean cartOrder = request.isCartOrder();
        Map<Long, Integer> quantities = cartOrder ? collectFromCart(memberId) : collectFromRequest(request);
        if (quantities.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER);
        }

        // 2) 상품 검증 + 재고 차감. order_item은 order id가 필요하므로 아직 저장하지 않고 모아 둔다.
        List<Product> products = new ArrayList<>();
        long totalPrice = 0;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productMapper.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
            }

            product.decreaseStock(entry.getValue());
            productMapper.updateStock(product);   // MyBatis는 변경 감지가 없으므로 직접 UPDATE

            products.add(product);
            totalPrice += (long) product.getPrice() * entry.getValue();
        }

        // 3) 주문 저장 → 생성된 id로 주문상품 저장
        Order order = Order.create(memberId, Math.toIntExact(totalPrice),
                request.getReceiverName(), request.getReceiverAddress());
        orderMapper.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (Product product : products) {
            // 현재 가격을 order_price에 복사한다. 이후 상품 가격이 바뀌어도 이 주문의 금액은 변하지 않는다.
            OrderItem item = OrderItem.create(order.getId(), product.getId(),
                    product.getPrice(), quantities.get(product.getId()));
            orderItemMapper.save(item);
            itemResponses.add(OrderItemResponse.of(item, product.getName()));
        }

        // 4) 장바구니 주문일 때만 비운다. 단건 주문은 장바구니를 건드리지 않는다.
        if (cartOrder) {
            cartItemMapper.deleteAllByMemberId(memberId);
        }

        return OrderResponse.of(order, itemResponses);
    }

    /** 장바구니 전체를 (상품 id → 수량)으로 바꾼다. 장바구니는 (회원, 상품)이 유니크라 중복이 없다 */
    private Map<Long, Integer> collectFromCart(Long memberId) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItem item : cartItemMapper.findAllForOrder(memberId)) {
            quantities.put(item.getProductId(), item.getQuantity());
        }
        return quantities;
    }

    /**
     * 요청의 items를 (상품 id → 수량)으로 바꾼다. 같은 상품이 여러 줄이면 수량을 합친다.
     * 합치지 않으면 같은 상품의 재고를 두 번 따로 차감하게 되어 계산이 꼬이기 쉽다.
     */
    private Map<Long, Integer> collectFromRequest(OrderCreateRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : request.getItems()) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }
}
