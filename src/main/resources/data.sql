-- 초기 데이터 (개발 단계 전용)
-- 위치: src/main/resources/data.sql
-- spring.sql.init.mode=always 이면 schema.sql 다음에 자동으로 실행된다.
-- schema.sql이 매번 테이블을 새로 만들기 때문에 AUTO_INCREMENT도 1부터 다시 시작한다.
-- → 아래 카테고리 id는 항상 1, 2, 3 이다.
-- 주의: src/main/resources에 있으므로 테스트(shop_test)에서도 똑같이 실행된다.

-- 카테고리: 생성 API가 없으므로(MVP 범위 밖) 여기서 넣는다.
INSERT INTO category (name) VALUES ('의류'), ('전자기기'), ('도서');

-- 관리자 계정: 가입 API는 항상 USER로 만들기 때문에 ADMIN은 여기서만 생긴다.
-- 비밀번호 'admin1234'의 BCrypt 해시. DB에는 평문을 넣으면 로그인할 때 matches()가 실패한다.
-- 개발용 계정이라 커밋해도 되지만, 실제 서비스에 배포할 때는 이 방식을 쓰면 안 된다.
INSERT INTO member (email, password, name, role)
VALUES ('admin@shop.com', '$2a$10$eBMXKbEabOcMHiDztxNbuuF/qtTlp3cGpoJR0JBM7wXzM9esmlcbi', '관리자', 'ADMIN');
