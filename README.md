# 쇼핑몰 백엔드 (MVP)

Spring Boot 기반 쇼핑몰 REST API. 백엔드 개발 전 과정을 직접 경험하기 위한 학습 프로젝트.

- 설계서: [docs/shopping-mall-design.md](docs/shopping-mall-design.md)

## 기술 스택

- Java 17, Spring Boot 4.x, Gradle
- Spring MVC, MyBatis, MySQL 8
- 인증: HttpSession + 인터셉터
- 테스트: JUnit5, Postman

## 진행 상태

- [x] 1. 환경 세팅
- [x] 2. 공통 기반 (응답/예외 포맷)
- [x] 3. 회원 (가입, 로그인/로그아웃, 인터셉터)
- [x] 4. 상품/카테고리 (조회, 페이징, 관리자 등록/수정)
- [x] 5. 장바구니 (담기, 조회, 수량 변경, 삭제)
- [ ] 6. 주문/결제 (트랜잭션, 재고 차감, 취소)
  - [x] 주문 생성 (바로 구매, 장바구니 주문)
  - [x] 주문 목록/상세
  - [ ] 주문 취소 (재고 복구)
  - [ ] 모의 결제
- [ ] 7. 테스트 (서비스 단위, Mapper 통합)

## 개발 일지

- [docs/devlog](docs/devlog/)

## 트러블슈팅

| 번호 | 제목 |
|---|---|
| 1 | [동시 가입 시 UK 위반이 409가 아니라 500으로 응답되는 문제](docs/troubleshooting/2026-11-10-concurrent-signup-500.md) |
| 2 | [로그인 응답 시간으로 가입 여부가 노출되는 문제 (타이밍 공격)](docs/troubleshooting/2026-11-10-login-timing-attack.md) |
| 3 | [@ModelAttribute DTO에 setter가 없으면 쿼리 파라미터가 조용히 무시되는 문제](docs/troubleshooting/2026-11-12-modelattribute-setter.md) |
