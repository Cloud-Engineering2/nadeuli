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
                .csrf(csrf -> csrf.disable()) // ✅ CSRF 보호 비활성화 (API 방식이므로)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll() // ✅ 인증 없이 접근 가능
                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
                        .requestMatchers("/auth/user/**").authenticated() // ✅ 사용자 정보는 인증 필요
                        .requestMatchers("/login", "/oauth2/**").permitAll()
                        .requestMatchers("/fonts/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/mypage").authenticated() // ✅ 비로그인 사용자는 접근 불가
                        .requestMatchers("/auth/oauth2/token").authenticated() // ✅ OAuth 로그인 후 JWT 발급
                        .anyRequest().authenticated() // ✅ 나머지 요청은 인증 필요
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // ✅ OAuth 로그인 유지

                // ✅ 커스텀 로그인 페이지 사용
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/process-login")
                        .defaultSuccessUrl("/mypage", true) // ✅ 로그인 성공 시 마이페이지 이동
                        .permitAll()
                )

                // ✅ OAuth 로그인 후 JWT 발급 후 `/mypage`로 이동
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/auth/oauth2/token", true) // ✅ OAuth 로그인 후 JWT 발급 API 요청
                        .failureUrl("/login?error=true") // 로그인 실패 시 리디렉트
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/{registrationId}")) // ✅ OAuth 리디렉션 설정
                )

                // ✅ 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true") // ✅ 로그아웃 후 로그인 페이지로 이동
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // 쿠키 삭제
                        .permitAll()
                )

                // ✅ JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
