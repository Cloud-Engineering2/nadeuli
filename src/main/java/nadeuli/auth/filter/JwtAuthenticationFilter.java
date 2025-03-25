/*
 * JwtAuthenticationFilter.java
 * Spring Security - JWT 인증 처리 필터
 * - 매 요청마다 JWT 토큰을 검사하고 유효한 경우 사용자 인증 처리
 * - Authorization 헤더 또는 accessToken 쿠키에서 토큰 추출
 * - JWT 토큰에서 사용자 이메일 추출 후 DB 조회 → 인증 컨텍스트에 등록
 *
 * 작성자 : 국경민, 김대환
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 국경민, 김대환   2025.03.19     최초 작성 - JWT 인증 필터 구현 (헤더 & 쿠키 토큰 처리 포함)
 * 박한철     2025.03.23    redis 버전으로 수정
 * 이홍비     2025.03.24    actuator 경로 -> 필터 제외
 * ========================================================
 */

package nadeuli.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.common.enums.ErrorCode;
import nadeuli.common.util.JwtUtils;
import nadeuli.auth.oauth.CustomUserDetails;
import nadeuli.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Actuator 경로 처리
        // String uri = request.getRequestURI().trim();
        // log.info("🔥 uri : " + uri);
        // log.info("🔥 uri.startsWith(/actuator) : " + uri.startsWith("/actuator"));
        // if (uri.startsWith("/actuator")) {
        //     log.info("✅ Actuator 요청 필터 제외: {}", uri);
        //     filterChain.doFilter(request, response);
        //     return;
        // }


        String accessToken = extractAccessToken(request);
        log.warn("✅ JwtAuthenticationFilter 필터진입");
        if (accessToken != null) {
            Optional<ErrorCode> errorCode = JwtUtils.validateToken(accessToken);
            if (errorCode.isEmpty()) {
                log.warn("✅ JWT 인증 성공: {}", JwtUtils.extractEmail(accessToken));
                authenticateUserFromToken(accessToken, request);
            } else {

                request.setAttribute(JwtUtils.EXCEPTION_ATTRIBUTE, errorCode.get());
                log.warn("❌ JWT 인증 실패: {}", errorCode.get());

            }
        } else {
            request.setAttribute(JwtUtils.EXCEPTION_ATTRIBUTE, ErrorCode.INVALID_TOKEN); // 쿠키에 없음
            log.warn("❌ JWT 인증 실패: {}", "Not Cookie in jar");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUserFromToken(String token, HttpServletRequest request) {
        String email = JwtUtils.extractEmail(token);
        userRepository.findByUserEmail(email).ifPresent(user -> {
            UserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }

    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) {
            log.debug("[extractToken] 요청에 쿠키 없음");
            return null;
        }

        return Arrays.stream(cookies)
                .peek(cookie -> log.debug("[extractToken] 쿠키: {} = {}", cookie.getName(), cookie.getValue()))
                .filter(cookie -> "accessToken".equals(cookie.getName()))
                .findFirst()
                .map(cookie -> {
                    log.debug("[extractToken] accessToken 쿠키에서 토큰 추출: {}", cookie.getValue());
                    return cookie.getValue();
                })
                .orElseGet(() -> {
                    log.debug("[extractToken] accessToken 쿠키를 찾지 못함");
                    return null;
                });
    }

}
