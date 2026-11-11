package com.example.shop.global.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 로그인한 회원의 id가 들어온다.
 *   public ResponseEntity<...> me(@Login Long memberId)
 * 실제로 값을 넣어 주는 것은 LoginMemberArgumentResolver다. 이 애노테이션 자체는 "표시"일 뿐이다.
 *
 * - @Target(PARAMETER): 메서드 파라미터에만 붙일 수 있다.
 * - @Retention(RUNTIME): 실행 중에도 애노테이션 정보가 남아 있어야 리졸버가 "붙어 있나?"를 확인할 수 있다.
 *   (기본값 CLASS는 실행할 때 사라져서 리졸버가 찾지 못한다)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Login {
}
