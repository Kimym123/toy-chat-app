package org.example.back.config;

import lombok.RequiredArgsConstructor;
import org.example.back.security.JwtAuthenticationFilter;
import org.example.back.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // Swagger API Security 설정 (ADMIN 권한)
    @Bean
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")  // Swagger 관련 경로에만 적용
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN")) // Swagger 접근 권한: ADMIN만 허용
                .httpBasic(httpBasic -> httpBasic.realmName("Swagger API"))  // HTTP Basic 인증 사용
                .csrf(csrf -> csrf.disable());  // CSRF 보호 비활성화
        
        return http.build();
    }
    
    // 일반 API Security 설정
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/home", "/api/members/login", "/api/members/register").permitAll()  // 인증 없이 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN만 접근 가능
                        .requestMatchers("/user/**").hasRole("USER") // USER만 접근 가능
                        .anyRequest().authenticated()) // 나머지 모든 요청은 인증 필요
                .csrf(csrf -> csrf.disable())  // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    // In-Memory 사용자 관리
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
}
