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

package nadeuli.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.common.util.CookieUtils;
import nadeuli.common.util.JwtUtils;
import nadeuli.repository.UserRepository;
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

            // ????OidcUserService에서 attribute에 넣어준 값들 그대로 꺼냄
            String email = authToken.getPrincipal().getAttribute("email");
            String refreshToken = authToken.getPrincipal().getAttribute("refreshToken");
            String sessionId = authToken.getPrincipal().getAttribute("sessionId");

            if (refreshToken == null || sessionId == null) {
                log.error("🔴 refreshToken 또는 sessionId가 null");
                response.sendRedirect("/login?error=true");
                return;
            }

            // AccessToken 발급
            String accessToken = JwtUtils.generateAccessToken(email);

            CookieUtils.addCookies(response,
                    CookieUtils.createAccessTokenCookie(accessToken),
                    CookieUtils.createRefreshTokenCookie(refreshToken),
                    CookieUtils.createSessionIdCookie(sessionId)
            );

            // ✅ 최종 리디렉션
            response.sendRedirect(redirectUri);

        } catch (Exception e) {
            log.error("[OAuth2SuccessHandler] 오류 발생: {}", e.getMessage(), e);
            response.sendRedirect("/login?error=true");
        }
    }
}