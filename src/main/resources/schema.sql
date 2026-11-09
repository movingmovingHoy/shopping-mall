-- 쇼핑몰 스키마 (MySQL 8)
-- 위치: src/main/resources/schema.sql
-- spring.sql.init.mode=always 로 앱 실행 시마다 실행된다.
-- 실행할 때마다 테이블을 DROP 후 다시 만들므로 데이터는 매번 초기화된다. (개발 단계 전용)

-- 기존 테이블 삭제: FK로 참조하는 쪽(자식)부터 지워야 오류가 나지 않는다.
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS member;

-- 회원
CREATE TABLE member (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(100) NOT NULL,              -- BCrypt 해시 저장
    name        VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER', -- USER, ADMIN
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 카테고리
CREATE TABLE category (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 상품
CREATE TABLE product (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    category_id     BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    price           INT          NOT NULL,
    stock_quantity  INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ON_SALE', -- ON_SALE, STOPPED
    description     TEXT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_category (category_id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 장바구니 (회원별 담은 상품 한 줄)
CREATE TABLE cart_item (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    member_id   BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_member_product (member_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_member  FOREIGN KEY (member_id)  REFERENCES member (id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주문 (order는 예약어라 orders)
CREATE TABLE orders (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    member_id         BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'CREATED', -- CREATED, PAID, CANCELED
    total_price       INT          NOT NULL,
    receiver_name     VARCHAR(50)  NOT NULL,
    receiver_address  VARCHAR(255) NOT NULL,
    ordered_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_orders_member_ordered (member_id, ordered_at),
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT ck_orders_total CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주문상품 (주문 시점의 가격을 order_price에 복사 저장)
CREATE TABLE order_item (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    order_id     BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    order_price  INT    NOT NULL,
    quantity     INT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_order_item_price    CHECK (order_price >= 0),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 결제 (주문 1건당 최대 1건)
CREATE TABLE payment (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    order_id  BIGINT      NOT NULL,
    amount    INT         NOT NULL,
    status    VARCHAR(20) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, CANCELED
    paid_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
