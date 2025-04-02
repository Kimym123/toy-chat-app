package org.example.back.repository;

import jakarta.transaction.Transactional;
import org.example.back.domain.member.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
public class MemberRepositoryTest {
    
    @Autowired
    private MemberRepository memberRepository;
    
    private Member member;
    
    @BeforeEach
    void 준비() {
        member = memberRepository.save(
                Member.builder().username("testUser").password("password").nickname("testNickname")
                        .email("test@google.com").phone("010-1234-5678").build());
    }
    
    @Test
    @DisplayName("사용자이름 기반 조회 테스트")
    void 사용자이름_조회() {
        // when
        Optional<Member> foundMember = memberRepository.findByUsername(member.getUsername());
        
        // then
        assertThat(foundMember).isPresent().get().extracting(Member::getUsername).isEqualTo("testUser");
    }
    
    @Test
    @DisplayName("사용자 ID 기반 조회 테스트")
    void 사용자_ID_조회() {
        // when
        Optional<Member> foundMember = memberRepository.findById(member.getId());
        
        // given
        assertThat(foundMember).isPresent().get().extracting(Member::getId).isEqualTo(member.getId());
    }
    
    @Test
    @DisplayName("중복된 사용자명 존재 여부 확인")
    void 중복_사용자명_확인() {
        // when
        boolean exists = memberRepository.existsByUsername(member.getUsername());
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("중복된 닉네임 존재 여부 확인")
    void 중복_닉네임_확인() {
        // when
        boolean exists = memberRepository.existsByNickname(member.getNickname());
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("사용자 삭제 테스트")
    void 사용자_삭제() {
        // when
        memberRepository.delete(member);
        memberRepository.flush();
        
        Optional<Member> foundMember = memberRepository.findById(member.getId());
        
        // then
        assertThat(foundMember).isEmpty();
    }
    
    @Test
    @DisplayName("비밀번호 변경 시 데이터 반영 테스트")
    void 비밀번호_변경_데이터_반영() {
        // when
        Member foundMember = memberRepository.findById(member.getId()).orElseThrow();
        foundMember.setPassword("newPassword");
        memberRepository.flush();
        
        // then
        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getPassword()).isEqualTo("newPassword");
    }
}
