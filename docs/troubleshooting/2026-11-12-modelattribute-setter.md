# @ModelAttribute DTO에 setter가 없으면 쿼리 파라미터가 조용히 무시되는 문제

- 발생일: 2026-11-12
- 관련 기능/파일: 상품 목록 / `ProductSearchRequest.java`, `ProductController.java`

## 문제
상품 목록 API는 검색 조건을 쿼리 파라미터로 받는다.

```
GET /api/products?categoryId=1&page=2&size=10
```

이 값을 `ProductSearchRequest` 객체로 받는데, 다른 요청 DTO 규칙(`@Getter` + `@NoArgsConstructor`)대로만 만들면 **파라미터를 보내도 값이 들어가지 않는다.**

실제 운영에서 겪은 건 아니다. 만들기 전에 예상했던 문제라, `@Setter`를 일부러 빼고 테스트를 돌려 직접 확인했다.

에러 메시지는 **없다.** 이게 이 문제의 가장 위험한 점이다. 앱은 정상으로 동작하는 것처럼 보인다.

```
@Setter를 뺐을 때 실패한 테스트 (ProductControllerTest)
- 쿼리_파라미터가_검색_조건으로_들어간다   → page=2, size=10을 보냈는데 1, 20이 들어옴
- page가_0이면_400                       → 400을 기대했는데 200
- size가_100을_넘으면_400                 → 400을 기대했는데 200
```

마지막 두 개가 특히 중요하다. 값이 무시되니까 `@Min`, `@Max` **검증도 함께 무력화**된다. `?size=1000000`을 보내도 에러 없이 기본값 20으로 조회된다. 이번에는 기본값이 안전해서 큰일은 없지만, 검증이 동작한다고 믿고 있는데 실제로는 안 되는 상태다.

## 원인
요청 DTO에 값을 넣어 주는 담당자가 **두 종류**다.

| | 상품 등록 | 상품 목록 |
|---|---|---|
| 값이 오는 곳 | 요청 본문 (JSON) | URL 쿼리 파라미터 |
| 애노테이션 | `@RequestBody` | `@ModelAttribute` |
| 넣어 주는 담당 | Jackson | 스프링 데이터 바인더 (WebDataBinder) |
| 넣는 방법 | private 필드에 직접 | **setter 호출** |

- Jackson은 리플렉션으로 private 필드에 직접 값을 넣는다. 그래서 `ProductCreateRequest`, `MemberSignupRequest`는 setter가 없어도 된다.
- 데이터 바인더는 기본 생성자로 객체를 만든 뒤 `setPage(2)`처럼 **setter를 호출**한다. setter가 없으면 에러를 내지 않고 **그 파라미터를 건너뛴다.**
- 건너뛴 필드에는 초기값(`page = 1`, `size = 20`)이 그대로 남는다.
- 검증(`@Valid`)은 값을 다 넣은 **다음에** 실행된다. 그래서 초기값 1, 20을 검사하고 통과시킨다.

## 해결
`ProductSearchRequest`에만 `@Setter`를 붙였다. CLAUDE.md의 요청 DTO 규칙에는 예외라서 클래스 주석에 이유를 남겼다.

```java
@Getter
@Setter          // @ModelAttribute는 setter로 값을 넣는다 (요청 DTO 규칙의 예외)
@NoArgsConstructor
public class ProductSearchRequest {
    private Long categoryId;

    @Min(1)
    private int page = 1;      // 파라미터를 안 보내면 setter가 호출되지 않아 이 값이 남는다

    @Min(1) @Max(100)
    private int size = 20;
}
```

### 테스트
응답은 가짜 서비스(`@MockitoBean`)가 만든 것이라 응답만 봐서는 바인딩이 됐는지 알 수 없다. 그래서 `ArgumentCaptor`로 **서비스에 실제로 넘어간 DTO를 꺼내서** 확인했다.

```java
mockMvc.perform(get("/api/products").param("page", "2").param("size", "10"))
        .andExpect(status().isOk());

ArgumentCaptor<ProductSearchRequest> captor = ArgumentCaptor.forClass(ProductSearchRequest.class);
verify(productService).getProducts(captor.capture());
assertThat(captor.getValue().getPage()).isEqualTo(2);   // @Setter가 빠지면 1이라서 실패
```

### 확인하다 막힌 점
`@Setter`를 빼고 돌리니 처음에는 테스트가 실패한 게 아니라 **컴파일 에러**가 났다. `ProductServiceTest`가 검색 조건을 만들 때 `request.setPage(1)`을 쓰고 있었기 때문이다. 그 테스트 파일을 잠깐 치워 두고 다시 돌려서 위 결과를 확인했다.

## 배운 점
- 같은 "요청 DTO"라도 **어디로 들어오느냐**(JSON 본문 / 쿼리 파라미터)에 따라 값을 넣는 담당자와 방법이 다르다.
- 에러가 나지 않는다고 동작하는 게 아니다. **조용히 무시되는 문제**는 테스트로만 잡을 수 있다.
- 검증 애노테이션은 값이 들어온 **다음에** 검사한다. 값이 안 들어오면 검증도 의미가 없다.
- 응답만으로 확인할 수 없는 것은 `verify`나 `ArgumentCaptor`로 **안에서 무엇이 넘어갔는지** 본다.
- 테스트가 정말 문제를 잡는지 확인하려면 **일부러 코드를 고장 내 보면** 된다.
