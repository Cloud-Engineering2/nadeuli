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
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.JwtTokenService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        // 1️⃣ 요청에서 JWT 토큰 추출
        String token = resolveToken(request);

        if (token != null) {
            if (jwtTokenService.validateToken(token)) {
                String userEmail = jwtTokenService.getUserEmail(token);

                if (userEmail != null && !userEmail.isEmpty()) { // ✅ Null 및 빈 값 체크 추가
                    // 2️⃣ UserDetails 객체 생성
                    UserDetails userDetails = User.withUsername(userEmail)
                            .password("") // 비밀번호는 JWT에서 관리하지 않음
                            .authorities(Collections.emptyList()) // 권한 없음
                            .build();

                    // 3️⃣ SecurityContextHolder에 인증 정보 설정
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("✅ [JwtTokenFilter] 인증 완료 - Email: {}", userEmail);
                } else {
                    log.warn("🚨 [JwtTokenFilter] JWT에서 이메일 추출 실패 - 유효한 이메일 없음");
                }
            } else {
                log.warn("🚨 [JwtTokenFilter] 유효하지 않은 토큰!");
            }
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
