package org.example.back.config;

import lombok.RequiredArgsConstructor;
import org.example.back.security.JwtAuthenticationFilter;
import org.example.back.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    // Password 암호화 설정
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // JwtAuthenticationFilter Bean 등록 (직접 new 로 만들지 않기 위함)
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
    
    // Swagger API Security 설정 (ADMIN 권한 + HTTP Basic)
    @Bean
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")  // Swagger 관련 경로에만 적용
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN")) // Swagger 접근 권한: ADMIN만 허용
                .httpBasic(httpBasic -> httpBasic.realmName("Swagger API"))  // HTTP Basic 인증 사용
                .csrf(csrf -> csrf.disable());  // CSRF 보호 비활성화
        
        return http.build();
    }
    
    // 일반 API Security 설정 (JWT + Role 기반 인가 + Stateless)
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/home", "/api/members/login", "/api/members/register").permitAll()  // 인증 없이 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN 만 접근 가능
                        .requestMatchers("/user/**").hasRole("USER") // USER 만 접근 가능
                        .anyRequest().authenticated()) // 나머지 모든 요청은 인증 필요
                .csrf(csrf -> csrf.disable())  // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    // dev 환경 전용 In-Memory 사용자 (Swagger 접근 테스트용)
    @Bean
    @Profile("dev")
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder().username("admin").password(passwordEncoder().encode("1234")).roles("ADMIN")
                .build();
        
        UserDetails user = User.builder().username("user").password(passwordEncoder().encode("1234"))
                .roles("USER") // USER 역할 부여
                .build();
        
        return new InMemoryUserDetailsManager(admin, user);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // AuthenticationManager는 로그인 요청 등에서 직접 인증이 필요할 때 사용되며,
        // AuthenticationConfiguration을 통해 Spring Security의 내부 인증 메커니즘을 가져와 빈으로 등록합니다.
        return config.getAuthenticationManager();
    }
}
