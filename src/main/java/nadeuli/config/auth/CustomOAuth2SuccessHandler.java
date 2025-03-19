/*
 * CustomOAuth2SuccessHandler.java
 * OAuth2 로그인 성공 후 처리 핸들러
 * - 로그인 성공 시 JWT Access/Refresh Token 생성 및 쿠키 저장
 * - state 파라미터(Base64 인코딩된 redirect URL) 디코딩하여 최종 리디렉션 처리
 *
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성 - OAuth2 성공 후 JWT 발급 및 리디렉션 처리 구현
 * ========================================================
 */

package nadeuli.config.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {


        String state = request.getParameter("state");
        String redirectUri = "/itinerary/mylist"; // 기본

        if (state != null) {
            try {
                redirectUri = new String(Base64.getUrlDecoder().decode(state));
            } catch (Exception e) {
                log.warn("Invalid redirect state. Using default.");
            }
        }

        log.info("✅ onAuthenticationSuccess 진입");
        try {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String email = authToken.getPrincipal().getAttribute("email");

        String accessToken = jwtTokenService.generateAccessToken(email);
        JwtTokenService.TokenResponse refreshToken = jwtTokenService.generateRefreshToken(email);

        User user = userRepository.findByUserEmail(email).orElseThrow();
        user.updateRefreshToken(refreshToken.token, refreshToken.expiryAt);
        userRepository.save(user);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken.token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 쿠키 저장 완료 후 메인으로 리디렉션
        response.sendRedirect(redirectUri);
        } catch (Exception e) {
            log.error("[OAuth2SuccessHandler] 오류 발생: {}", e.getMessage(), e);
            response.sendRedirect("/login?error=true"); // fallback 처리
        }
    }
}