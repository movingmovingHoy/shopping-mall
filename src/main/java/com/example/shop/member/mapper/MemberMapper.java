package com.example.shop.member.mapper;

import com.example.shop.member.domain.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * SQL은 resources/mapper/member/MemberMapper.xml에 있다.
 * XML의 namespace가 이 인터페이스의 전체 경로와 같아야 연결된다.
 */
@Mapper
public interface MemberMapper {

    /** 저장 후 DB가 만든 id가 member.id에 채워진다 (useGeneratedKeys) */
    void save(Member member);

    boolean existsByEmail(String email);

    Optional<Member> findById(Long id);
}
