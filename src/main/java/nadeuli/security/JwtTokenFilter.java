/* JwtTokenFilter.java
 * JWT 인증 필터 (Spring Security용)
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       JWT 필터 생성 초안
 * 국경민      03-12       JWT 검증 로직 개선 및 예외 처리 강화
 * 국경민      03-12       SecurityContextHolder 설정 최적화 및 중복 방지
 * 국경민      03-12       JWT 인증 실패 시 SecurityContext 초기화 및 예외 처리 강화
 * ========================================================
 */

package nadeuli.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.JwtTokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    /**
     * ✅ JWT를 검증하고 SecurityContext에 저장
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtTokenService.validateToken(token)) {
                    String userEmail = jwtTokenService.getUserEmail(token);
                    if (userEmail != null) {
                        UserDetails userDetails = User.withUsername(userEmail)
                                .password("")
                                .authorities(Collections.emptyList()) // ✅ ROLE 적용 가능
                                .build();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        // ✅ SecurityContext 생성 및 설정
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);

                        log.info("✅ JWT 인증 성공 - 사용자: {}", userEmail);
                    } else {
                        log.warn("🚨 JWT에서 이메일 추출 실패");
                        SecurityContextHolder.clearContext();
                        sendUnauthorizedResponse(response, "🚨 JWT에서 이메일을 추출할 수 없습니다.");
                        return;
                    }
                } else {
                    log.warn("🚨 유효하지 않은 JWT 토큰");
                    SecurityContextHolder.clearContext();
                    sendUnauthorizedResponse(response, "🚨 유효하지 않은 JWT 토큰입니다.");
                    return;
                }
            } catch (Exception e) {
                log.error("🚨 JWT 필터 처리 중 오류 발생: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                sendUnauthorizedResponse(response, "🚨 JWT 오류: " + e.getMessage());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * ✅ 요청 헤더에서 JWT 토큰을 추출하는 메서드
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer ")) ? bearerToken.substring(7) : null;
    }

    /**
     * ✅ 401 Unauthorized JSON 응답 반환 메서드
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\": false, \"message\": \"" + message + "\"}");
    }
}
