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
import nadeuli.security.CustomOAuth2User;
import nadeuli.security.CustomOAuth2UserService;
import nadeuli.security.JwtTokenFilter;
import nadeuli.service.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                .csrf(csrf -> csrf.disable()) // ✅ CSRF 보호 비활성화 (REST API 기반이므로 필요 없음)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ JWT 기반 인증 적용

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/auth/**", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 로그인 설정 (SecurityContext 유지 문제 해결)
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            log.info("✅ OAuth2 로그인 성공 - 사용자: {}", authentication.getPrincipal());

                            if (authentication.getPrincipal() instanceof CustomOAuth2User customUser) {
                                // ✅ SecurityContext에 인증 정보 강제 저장 (Redis 또는 In-Memory)
                                saveSecurityContext(customUser);

                                log.info("✅ SecurityContext 업데이트 완료 - Email: {}", customUser.getUsername());

                                // ✅ JWT 발급
                                String accessToken = jwtTokenService.createAccessToken(customUser.getUsername());
                                String refreshToken = jwtTokenService.createRefreshToken(customUser.getUsername());

                                // ✅ JSON 응답으로 반환
                                sendJsonResponse(response, true, accessToken, refreshToken);
                            } else {
                                sendJsonResponse(response, false, null, null);
                            }
                        })
                )

                // ✅ SecurityContext 유지 필터 추가 (중요)
                .addFilterBefore(new SecurityContextPersistenceFilter(), UsernamePasswordAuthenticationFilter.class)

                // ✅ JWT 인증 필터 추가 (OAuth2 인증 이후에도 SecurityContext 유지)
                .addFilterBefore(new JwtTokenFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ✅ SecurityContext 저장 (Redis 또는 In-Memory)
     */
    private void saveSecurityContext(CustomOAuth2User customUser) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                customUser, null, customUser.getAuthorities()
        ));
        SecurityContextHolder.setContext(securityContext);
        log.info("✅ SecurityContextHolder에 사용자 정보 강제 저장 완료 - Email: {}", customUser.getUsername());
    }

    /**
     * ✅ OpenID Connect (OIDC) 사용자 서비스 등록
     */
    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    /**
     * ✅ JSON 응답 전송 메서드 (에러 처리 포함)
     */
    private void sendJsonResponse(HttpServletResponse response, boolean success, String accessToken, String refreshToken) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_UNAUTHORIZED);

        // ✅ JSON 객체 생성
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", success);

        if (success) {
            jsonResponse.put("accessToken", accessToken);
            jsonResponse.put("refreshToken", refreshToken);
        } else {
            jsonResponse.put("message", "OAuth2 사용자 정보가 존재하지 않습니다.");
        }

        // ✅ JSON 변환 후 응답
        new ObjectMapper().writeValue(response.getWriter(), jsonResponse);
        response.getWriter().flush();
    }
}
