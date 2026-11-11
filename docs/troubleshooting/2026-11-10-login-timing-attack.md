# 로그인 응답 시간으로 가입 여부가 노출되는 문제 (타이밍 공격)

- 발생일: 2026-11-10
- 관련 기능/파일: 로그인 / `MemberService.java`

## 문제
없는 이메일과 있는 이메일(비밀번호 틀림)에 똑같이 `LOGIN_FAILED`(401)를 응답해서 가입 여부를 숨기려고 했다.
그런데 **응답 시간**이 달라서 시간만 재면 가입 여부를 알 수 있었다.

실제로 공격을 받은 건 아니고, 코드를 리뷰하다가 찾은 문제다.

```
없는 이메일    → 수 ms (조회만 하고 바로 예외)
있는 이메일    → 약 100 ms (BCrypt matches 실행)
```

## 원인
```java
Member member = memberMapper.findByEmail(request.getEmail())
        .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));  // 없으면 바로 예외

if (!passwordEncoder.matches(...)) { ... }   // 있을 때만 실행
```
- 이메일이 없으면 비교할 해시가 없어서 `matches()`를 건너뛴다.
- BCrypt는 무차별 대입을 막으려고 **일부러 느리게** 만들어져 있다(cost 10, 약 0.1초). 그래서 두 경우의 시간 차이가 눈에 띄게 난다.
- BCrypt의 약점이 아니라 **우리 코드의 분기** 문제다.

## 해결
이메일이 없어도 **가짜 해시로 `matches()`를 항상 실행**해서 시간을 맞춘다.

```java
Member member = memberMapper.findByEmail(request.getEmail()).orElse(null);

String hash = (member != null) ? member.getPassword() : dummyHash;
boolean matched = passwordEncoder.matches(request.getPassword(), hash);

if (member == null || !matched) {
    throw new BusinessException(ErrorCode.LOGIN_FAILED);
}
```
- `dummyHash`는 생성자에서 `passwordEncoder.encode(...)`로 **한 번만** 만든다.
  - 하드코딩하지 않는 이유: 같은 encoder로 만들어야 cost가 같아서 걸리는 시간도 같다. cost를 바꿔도 자동으로 맞춰진다.
  - 생성자에서 `encode()`를 호출해야 해서 `@RequiredArgsConstructor` 대신 생성자를 직접 작성했다.
- 가짜 해시와 비교하면 결과는 항상 false지만, `member == null` 조건으로 한 번 더 막는다.

### 테스트
- 응답 시간을 직접 재면 결과가 들쭉날쭉해서 테스트가 불안정하다.
- 대신 **시간이 같아지는 원인**인 `matches()` 호출이 일어났는지 `verify`로 확인했다. (`MemberServiceMockTest`)
```java
given(memberMapper.findByEmail(any())).willReturn(Optional.empty());
// ... LOGIN_FAILED 확인
verify(passwordEncoder).matches(eq("password123"), any());   // 예전 코드면 0번 호출 → 실패
```

### Postman 확인 중 헷갈린 점: 처음엔 10배 차이가 났다
- 고친 뒤 Postman으로 재 보니 "없는 이메일"이 "비밀번호 틀림"보다 10배 빨랐다.
- 원인은 방어 코드가 아니라 **앱 시작 후 첫 요청이 느린 것**이었다. 처음 실행되는 코드는 클래스 로딩, JIT 컴파일, DB 연결 준비 때문에 느리다. "비밀번호 틀림"을 먼저 보내서 그쪽에 준비 시간이 붙었다.
- curl로 두 경우를 5번씩 번갈아 요청해 보니 비슷했다.
  ```
  비밀번호 틀림   401  0.160s / 0.157s / 0.132s / 0.135s / 0.137s
  없는 이메일     401  0.138s / 0.136s / 0.137s / 0.146s / 0.132s
  ```
- 이메일을 빈 값(`""`)으로 보내면 `@NotBlank`에서 400으로 막혀서 빠른데, 이건 가입 여부와 관계없이 누구나 같아서 문제없다.

## 배운 점
- 에러 메시지를 똑같이 맞춰도 **응답 시간 같은 다른 경로**로 정보가 샐 수 있다.
- 보안 목적의 분기는 결과뿐 아니라 **걸리는 시간도** 같게 만들어야 한다.
- 시간처럼 불안정한 값은 직접 측정하지 말고, 그 원인이 되는 동작을 검증한다.
- 성능/시간을 비교할 때는 **첫 요청을 빼고 여러 번 반복한 결과**로 본다. 실제 공격자도 수백 번 요청해서 평균을 낸다.
