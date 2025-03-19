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