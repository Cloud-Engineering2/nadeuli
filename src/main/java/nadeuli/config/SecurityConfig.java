/* SecurityConfig.java
 * Spring Security (OAuth + JWT) 보안 설정
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       Spring Security 기본 설정 초안
 * 국경민      03-04       OAuth2 로그인 및 JWT 필터 적용
 * 국경민      03-05       CustomOAuth2UserService 추가 및 인증 보완
 * 국경민      03-05       세션 유지 및 리디렉션 설정 추가
 * 국경민      03-05       JWT 기반 로그아웃 처리 (invalidateHttpSession 제거)
 * 국경민      03-06       세션 상태 무효화 (STATELESS 적용) 및 보안 강화
 * ========================================================
 */

package nadeuli.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.security.CustomOAuth2UserService;
import nadeuli.security.JwtTokenFilter;
import nadeuli.service.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenService jwtTokenService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // ✅ CSRF 비활성화 (REST API)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ JWT 기반 인증 유지
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/", "/auth/**").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            // ✅ SecurityContext 유지
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.info("✅ OAuth2 로그인 성공 - 사용자: {}", authentication.getPrincipal());

                            // ✅ 로그인 성공 시 JSON 응답 반환 (리다이렉트 방지)
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            PrintWriter writer = response.getWriter();
                            writer.println("{ \"message\": \"로그인 성공\", \"status\": 200 }");
                            writer.flush();
                        })
                )
                .addFilterBefore(new JwtTokenFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
