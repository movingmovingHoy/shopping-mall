# 동시 가입 시 UK 위반이 409가 아니라 500으로 응답되는 문제

- 발생일: 2026-11-10
- 관련 기능/파일: 회원가입 / `MemberService.java`, `ApiExceptionHandler.java`, `schema.sql`(`uk_member_email`)

## 문제
두 사람이 **거의 동시에 같은 이메일로** 가입하면 둘 중 한 명은 "이미 가입된 이메일입니다"(409)가 아니라 **500 서버 오류**를 받는다.

실제로 에러를 겪은 건 아니고, 코드를 리뷰하다가 미리 찾은 문제다. 아래는 이 상황에서 발생하는 에러다.

```
org.springframework.dao.DuplicateKeyException:
  ... Duplicate entry 'user@test.com' for key 'member.uk_member_email'
```

## 원인
1. `signup()`은 먼저 `existsByEmail`로 중복을 확인하고 INSERT한다.
2. 일반 `SELECT`는 락 없이 읽는다. 그래서 A가 아직 커밋하기 전이면 B에게도 A의 데이터가 안 보여서 **둘 다 false**를 받는다.
   - `@Transactional`은 락을 걸지 않는다. 그래서 이걸 막아 주지 못한다.
3. 둘 다 INSERT까지 가면 DB의 `UNIQUE KEY uk_member_email`이 하나만 통과시키고 나머지는 Duplicate entry 에러를 낸다.
   ```
   A: existsByEmail → false     B: existsByEmail → false
   A: INSERT 성공               B: INSERT → Duplicate entry
   ```
4. 이 에러는 스프링이 `DuplicateKeyException`으로 바꿔 던진다. `BusinessException`이 아니라서 `ApiExceptionHandler`의 `Exception.class` 핸들러에 걸려 **500**이 된다.
   - DB 에러에는 HTTP 상태 코드가 없다. 몇 번으로 응답할지는 우리가 정해야 한다.

## 해결
`save()`에서 `DuplicateKeyException`만 잡아 `DUPLICATE_EMAIL`(409)로 바꿔 던진다.

```java
try {
    memberMapper.save(member);
} catch (DuplicateKeyException e) {
    log.warn("동시에 같은 이메일로 회원가입 시도: {}", request.getEmail(), e);
    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
}
```

### 해결하면서 한 실수: catch를 너무 넓게 잡음
처음에는 `catch (Exception e)`로 작성했다.
- DB 연결 실패, SQL 오류 같은 **다른 에러도 전부** 409 "이미 존재하는 이메일"로 바뀌었다. 사용자는 자기 잘못인 줄 알게 된다.
- 원래 에러가 사라져 **로그에도 안 남아** 진짜 원인을 찾을 수 없다.

→ `DuplicateKeyException`으로 좁혔다. 다른 에러는 그대로 올라가서 500 + 에러 로그로 남는다.

### 응답 code는 하나로 통일
처음에는 `DUPLICATE_KEY`라는 ErrorCode를 따로 만들었다. 그런데 사용자 입장에서는 `existsByEmail`에 걸리든 UK에 걸리든 같은 상황이다. 프론트는 `code`로 분기하므로 `DUPLICATE_EMAIL` 하나로 통일했다. "UK에서 걸렸다"는 개발자용 정보라 `log.warn`에 남긴다.

### 테스트
`MemberServiceMockTest`에서 Mock으로 상황을 만들었다.
- `existsByEmail` → false, `save` → `DuplicateKeyException`일 때 `DUPLICATE_EMAIL`이 나오는지
- `save` → `DataAccessResourceFailureException`(중복이 아닌 DB 오류)일 때 바꾸지 않고 그대로 던지는지 → catch가 다시 넓어지는 걸 막는다

## 배운 점
- 중복을 막는 최종 방어선은 **DB 제약 조건(UK)**이다. `existsByEmail` 같은 앱 쪽 체크는 대부분의 경우에 친절한 응답을 빨리 주기 위한 용도다.
- `@Transactional` ≠ 락. 락은 DB가 INSERT/UPDATE/DELETE, `SELECT ... FOR UPDATE`에서 건다.
- 자바 `synchronized`는 서버가 여러 대면 못 막는다. DB는 하나라서 UK가 막는다.
- 예외는 **처리할 수 있는 것만 좁게** 잡는다.
- 사용자용 정보는 응답에, 개발자용 정보는 로그에 남긴다.
- 남은 일: Mock 테스트는 "save가 `DuplicateKeyException`을 던진다"고 가정한 것이다. 진짜 DB에서 그 예외가 나오는지 `MemberMapperTest`로 확인해야 한다.
