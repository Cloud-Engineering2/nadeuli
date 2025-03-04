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
 * ========================================================
 */

package nadeuli.config;

import lombok.RequiredArgsConstructor;
import nadeuli.security.JwtTokenFilter;
import nadeuli.service.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenService jwtTokenService;

    /**
     * ✅ Spring Security 설정
     * - OAuth2 로그인 설정
     * - JWT 인증 필터 추가
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (JWT 사용 시 필요 없음)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**").permitAll() // 인증 없이 접근 가능한 URL
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/auth/loginSuccess", true) // 로그인 성공 시 이동할 경로
                        .userInfoEndpoint(userInfo -> userInfo.userService(new DefaultOAuth2UserService())) // 사용자 정보 조회
                )
                .addFilterBefore(new JwtTokenFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class); // JWT 인증 필터 추가
        return http.build();
    }
}
