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
 * ========================================================
 */

package nadeuli.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nadeuli.service.JwtTokenService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1️⃣ 요청에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 2️⃣ 토큰 검증 및 사용자 설정
        if (token != null && jwtTokenService.validateToken(token)) {
            String userEmail = jwtTokenService.getUserEmail(token);

            // 3️⃣ Spring Security 컨텍스트에 사용자 설정
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(userEmail, null, null));
        }

        // 4️⃣ 다음 필터 실행
        chain.doFilter(request, response);
    }

    /**
     * ✅ 요청 헤더에서 JWT 토큰을 추출하는 메서드
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }
}
