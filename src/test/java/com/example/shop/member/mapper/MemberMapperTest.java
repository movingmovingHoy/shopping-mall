package com.example.shop.member.mapper;

import com.example.shop.member.domain.Member;
import com.example.shop.member.domain.Role;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @MybatisTest: MyBatis 관련 빈만 띄우고, 테스트마다 자동으로 롤백한다.
 * @AutoConfigureTestDatabase(replace = NONE): 기본값은 내장 DB(H2 등)로 바꿔치기하는 것이라,
 *   그러지 말고 application.yml의 shop_test(MySQL)를 그대로 쓰라고 지정한다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberMapperTest {

    @Autowired
    MemberMapper memberMapper;

    @Test
    void 저장하면_id가_채워지고_조회할_수_있다() {
        Member member = Member.create("user@test.com", "encoded-password", "홍길동");

        memberMapper.save(member);

        assertThat(member.getId()).isNotNull();
        Member found = memberMapper.findById(member.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("user@test.com");
        assertThat(found.getRole()).isEqualTo(Role.USER);
        assertThat(found.getCreatedAt()).isNotNull();   // DB 기본값 CURRENT_TIMESTAMP
    }

    @Test
    void 없는_id로_조회하면_빈_Optional() {
        assertThat(memberMapper.findById(999_999L)).isEmpty();
    }

    @Test
    void 이메일_존재_여부를_확인한다() {
        memberMapper.save(Member.create("user@test.com", "encoded-password", "홍길동"));

        assertThat(memberMapper.existsByEmail("user@test.com")).isTrue();
        assertThat(memberMapper.existsByEmail("none@test.com")).isFalse();
    }

    @Test
    void 이메일로_조회하면_비밀번호까지_가져온다() {
        memberMapper.save(Member.create("user@test.com", "encoded-password", "홍길동"));

        Member found = memberMapper.findByEmail("user@test.com").orElseThrow();

        assertThat(found.getName()).isEqualTo("홍길동");
        assertThat(found.getPassword()).isEqualTo("encoded-password");   // 로그인 때 비교해야 하므로
    }

    @Test
    void 없는_이메일로_조회하면_빈_Optional() {
        assertThat(memberMapper.findByEmail("none@test.com")).isEmpty();
    }

    /**
     * MemberServiceMockTest는 "같은 이메일로 save하면 DuplicateKeyException이 난다"고 가정했다.
     * 그 가정이 진짜 DB에서도 맞는지 확인한다. (existsByEmail을 거치지 않고 save를 바로 두 번)
     * - schema.sql에서 uk_member_email이 빠지면 → 예외가 안 나서 실패
     * - 다른 예외 타입으로 바뀌면 → Service의 catch에 안 걸리므로 여기서 실패
     */
    @Test
    void 같은_이메일로_두_번_저장하면_DuplicateKeyException() {
        memberMapper.save(Member.create("user@test.com", "encoded-password", "홍길동"));

        assertThatThrownBy(() -> memberMapper.save(Member.create("user@test.com", "other-password", "김철수")))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
