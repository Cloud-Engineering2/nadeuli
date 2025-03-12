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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenService jwtTokenService;
    private final CustomOAuth2UserService customOAuth2UserService;

    /**
     * ✅ Spring Security 설정을 정의하는 메서드
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CSRF 보호 비활성화 (REST API 기반이므로 필요 없음)
                .csrf(csrf -> csrf.disable()) // ✅ 메서드 참조 불가 → 람다 유지

                // ✅ 세션을 사용하지 않고 JWT 기반 인증을 적용
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ API 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/auth/**", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            // ✅ SecurityContext 유지
                            log.info("✅ OAuth2 로그인 성공 - 사용자: {}", authentication.getPrincipal());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().println("{ \"success\": true, \"message\": \"로그인 성공\" }");
                            response.getWriter().flush();
                        })
                )

                // ✅ JWT 인증 필터 추가 (OAuth2 로그인 이후에도 정상 동작 보장)
                .addFilterBefore(new JwtTokenFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ✅ OpenID Connect (OIDC) 사용자 서비스 등록
     */
    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }
}
