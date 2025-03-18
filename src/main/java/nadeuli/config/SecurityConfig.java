package nadeuli.config;

import lombok.extern.slf4j.Slf4j;
import nadeuli.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll()
                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
                        .requestMatchers("/auth/user/me").authenticated() // ✅ 사용자 정보 조회는 인증 필요
                        .requestMatchers("/auth/user/register", "/auth/user/reset-password").permitAll() // ✅ 회원가입 & 비밀번호 재설정 허용
                        .requestMatchers("/login", "/hello").permitAll() // 🔹 슬래시(`/`) 추가하여 URL 오류 방지
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/mypage", true) // ✅ 로그인 성공 시 마이페이지로 이동
                        .failureUrl("/login?error=true").permitAll()
                )
                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable()); // ✅ JWT 사용하므로 CSRF 보호 비활성화

        return http.build();
    }
}
