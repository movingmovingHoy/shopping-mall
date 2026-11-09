# 쇼핑몰 백엔드 프로젝트

Spring Boot 기반 쇼핑몰 REST API. 개발자는 백엔드 학습 중인 학생이며, 이 프로젝트의 목적은 백엔드 개발 전 과정을 직접 경험하는 것이다.
설계 기준 문서: `docs/shopping-mall-design.md` (ERD, API 목록, 도메인 규칙). 코드를 작성하기 전에 반드시 참고한다.

## 기술 스택

- Java 17, Spring Boot 4.x (현재 4.0.9), Gradle
- Spring MVC, MyBatis, MySQL 8 (로컬 PC에 직접 설치, DB 이름 `shop`). Docker는 배포 단계에서 도입 예정이므로 지금은 사용하지 않는다.
- 인증: HttpSession + 인터셉터. **MVP 단계에서는 Spring Security를 사용하지 않는다.** (비밀번호 암호화에만 `spring-security-crypto`의 BCryptPasswordEncoder 사용) MVP 완성 후 확장 단계에서 Spring Security로 전환할 예정이다.
- 테스트: JUnit5, 서비스 단위 테스트 + Mapper 통합 테스트
- API 확인은 Postman으로 한다. Swagger/springdoc는 추가하지 않는다.

## 작업 방식 (중요)

- 개발자는 학습 중이다. 코드를 작성할 때 **왜 이렇게 했는지** 핵심 이유를 짧게 설명한다.
- 설계서에 없는 기능, 새 라이브러리, 새 기술은 임의로 추가하지 말고 먼저 제안하고 확인받는다.
- 한 번에 여러 도메인을 건드리지 않는다. 작업 단위는 기능 하나(예: 회원가입)로 작게 유지한다.
- 변경 후에는 무엇을 바꿨는지 파일 단위로 요약한다.
- 핵심 도메인 로직(재고 차감, 주문 취소 등)은 개발자가 먼저 직접 작성해 보고 싶다고 하면 구현하지 말고 리뷰만 한다.

## 패키지 구조

도메인형 구조를 따른다. 각 도메인 아래에 `controller`, `service`, `mapper`, `domain`, `dto`를 둔다.

```
com.example.shop.{member|product|cart|order|payment}
com.example.shop.global.{web|exception|config}
```

- `global.web`: LoginCheckInterceptor, AdminCheckInterceptor, LoginMemberArgumentResolver
- `global.exception`: ErrorCode, 커스텀 예외, ApiExceptionHandler(`@RestControllerAdvice`)
- Mapper XML 위치: `src/main/resources/mapper/{도메인}/`

## 코딩 규칙

- Controller는 요청 검증과 응답 변환만 한다. 비즈니스 로직은 Service 또는 도메인 객체에 둔다.
- 요청/응답은 DTO로 주고받는다. 도메인 객체를 응답으로 직접 반환하지 않는다.
- DTO에 `record`를 쓰지 않고 일반 클래스로 작성한다. 응답 DTO는 `final` 필드 + `@Getter` + 정적 팩토리(`of`/`from`), 요청 DTO는 `@Getter` + `@NoArgsConstructor`.
- 요청 DTO는 Bean Validation(`@NotBlank`, `@Min` 등)으로 검증한다.
- 도메인 객체에 무분별한 setter를 열지 않는다. 상태 변경은 의미 있는 메서드(`decreaseStock`, `cancel`)로 한다.
- `@Transactional`은 Service 계층에 붙이고, 조회 전용 메서드는 `readOnly = true`를 쓴다.
- 비즈니스 예외는 커스텀 예외 + ErrorCode로 던지고 ApiExceptionHandler가 공통 JSON 포맷으로 응답한다.
- 로그인 회원은 `@Login` 애노테이션(ArgumentResolver)으로 컨트롤러에서 받는다.
- 컨트롤러 메서드에서 `HttpSession`을 직접 다루지 않는다.

## MyBatis 규칙

- SQL은 Mapper XML에 작성한다. 파라미터는 `#{}`만 사용한다. `${}`는 SQL 인젝션 위험이 있어 금지한다.
- 컬럼은 snake_case, 자바 필드는 camelCase. `map-underscore-to-camel-case: true`를 사용한다.
- 조인 결과가 중첩될 때는 `resultMap`을 명시한다.
- 목록 조회는 항상 페이징(`LIMIT/OFFSET`)을 적용한다.
- `SELECT *`를 쓰지 않는다.

## DB 규칙

- 테이블/컬럼은 snake_case. 주문 테이블은 예약어 회피를 위해 `orders`.
- 스키마는 `src/main/resources/schema.sql`로 관리한다. `sql.init.mode: always`이므로 앱 실행마다 `DROP TABLE IF EXISTS` 후 다시 생성한다(데이터 초기화). DROP은 FK 자식 테이블부터 한다.
- 설정 파일은 `application.yml`을 사용한다. (`application.properties` 사용하지 않음)
- 가격은 주문 시점 값을 `order_item.order_price`에 복사해 저장한다.
- 상품은 물리 삭제하지 않고 `status`로 판매중지 처리한다.

## 테스트 규칙

- 기능을 추가하면 테스트도 함께 작성한다.
- 서비스 테스트는 정상 케이스와 실패 케이스(재고 부족, 권한 없음, 중복 가입 등)를 둘 다 작성한다.
- Mapper 테스트는 테스트용 DB(로컬 MySQL의 `shop_test` 데이터베이스)를 쓰고 테스트마다 롤백한다.
- HTTP 응답 자체를 검증해야 하는 웹 계층(ApiExceptionHandler, 인터셉터 등)은 `@WebMvcTest` + `MockMvc`로 테스트한다.

## Git 규칙

- 브랜치: `main` + 기능 브랜치(`feature/member-signup` 형태)
- 커밋은 기능 단위로 작게, 메시지는 `feat: 회원가입 API 추가` 형태(feat, fix, refactor, test, docs)
- 커밋하기 전에 테스트가 통과하는지 확인한다.

## 실행

- DB: 로컬 MySQL을 실행해 두고 `shop` 데이터베이스를 사용한다. (접속 정보는 `application.yml`, 비밀번호는 커밋하지 않는다)
- 앱 실행: `./gradlew bootRun`
- 테스트: `./gradlew test`
