package org.example.back.repository;

import org.example.back.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUsername(String username); // 회원 ID 중복 확인
    
    boolean existsByNickname(String nickname); // 닉네임 중복 확인
    
    Optional<Member> findById(Long id); // 회원 정보 조회
    
    Optional<Member> findByUsername(String username); // 회원 정보 조회
}