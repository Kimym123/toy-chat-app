package org.example.back.repository;

import org.example.back.domain.auth.RefreshToken;
import org.example.back.domain.member.Member;
import org.example.back.repository.auth.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class RefreshTokenRepositoryTest {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private MemberRepository memberRepository;
    
    private Member member;
    private RefreshToken refreshToken;
    
    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                Member.builder().username("testUser").password("password").nickname("nickname").email("test@email.com")
                        .phone("010-0000-0000").build());
        
        refreshToken = refreshTokenRepository.save(RefreshToken.builder().member(member).token("valid-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7)).build());
    }
    
    @Nested
    @DisplayName("성공 케이스")
    class Success {
        
        @Test
        @DisplayName("Token 으로 RefreshToken 조회 성공")
        void findByToken_성공() {
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("valid-refresh-token");
            assertThat(result).isPresent();
            assertThat(result.get().getMember()).isEqualTo(member);
        }
        
        @Test
        @DisplayName("Member 로 RefreshToken 조회 성공")
        void findByMember_성공() {
            Optional<RefreshToken> result = refreshTokenRepository.findByMember(member);
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("valid-refresh-token");
        }
        
        @Test
        @DisplayName("MemberId 로 RefreshToken 조회 성공")
        void findByMemberId_성공() {
            Optional<RefreshToken> result = refreshTokenRepository.findByMemberId(member.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getMember().getId()).isEqualTo(member.getId());
        }
        
        @Test
        @DisplayName("Member 기준으로 RefreshToken 삭제")
        void deleteByMember_성공() {
            refreshTokenRepository.deleteByMember(member);
            refreshTokenRepository.flush();
            
            Optional<RefreshToken> result = refreshTokenRepository.findByMember(member);
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        
        @Test
        @DisplayName("존재하지 않는 토큰으로 조회 -> Optional.empty()")
        void findByToken_실패() {
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("not-exist-token");
            assertThat(result).isEmpty();
        }
        
        @Test
        @DisplayName("다른 사용자로 조회 -> Optional.empty()")
        void findByMember_실패() {
            Member otherMember = memberRepository.save(
                    Member.builder().username("other").password("pass").nickname("diff").email("other@email.com")
                            .phone("010-1111-1111").build());
            
            Optional<RefreshToken> result = refreshTokenRepository.findByMember(otherMember);
            assertThat(result).isEmpty();
        }
        
        @Test
        @DisplayName("없는 MemberId 로 조회 -> Optional.empty()")
        void findByMemberId_실패() {
            Optional<RefreshToken> result = refreshTokenRepository.findByMemberId(999L);
            assertThat(result).isEmpty();
        }
        
        @Test
        @DisplayName("삭제되지 않은 다른 Member 기준 -> 삭제되지 않음")
        void deleteByMember_실패() {
            Member otherMember = memberRepository.save(
                    Member.builder().username("other").password("pass").nickname("diff").email("other@email.com")
                            .phone("010-1111-1111").build());
            
            refreshTokenRepository.deleteByMember(otherMember);
            refreshTokenRepository.flush();
            
            Optional<RefreshToken> result = refreshTokenRepository.findByMember(member);
            assertThat(result).isPresent();
        }
    }
}
