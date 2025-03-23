package nadeuli.controller;


import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.common.util.CookieUtils;
import nadeuli.service.JwtRedisService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtRedisService jwtRedisService;


    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = "accessToken", required = false) String accessTokenFromCookie,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            @CookieValue(name = "sessionId", required = false) String sessionIdFromCookie) {

        log.info("로그아웃 요청 - Authorization Header: {}", authHeader);

        String jwtAccessToken = null;

        if (accessTokenFromCookie != null) {
            jwtAccessToken = accessTokenFromCookie;
        }

        if (jwtAccessToken == null) {
            log.warn("로그아웃 요청 - 인증 정보 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인된 사용자가 없습니다."
            ));
        }

        // 2. AccessToken 블랙리스트 등록
        jwtRedisService.blacklistAccessToken(jwtAccessToken);

        // 3. RefreshToken 삭제 및 블랙리스트 등록
        if (refreshTokenFromCookie != null) {
            jwtRedisService.blacklistRefreshToken(refreshTokenFromCookie);
        }

        if (sessionIdFromCookie != null) {
           jwtRedisService.deleteRefreshToken(jwtAccessToken, sessionIdFromCookie);
        }

        // 4. 쿠키 삭제
        ResponseCookie expiredAccessTokenCookie = CookieUtils.expireAccessTokenCookie();
        ResponseCookie expiredRefreshTokenCookie = CookieUtils.expireRefreshTokenCookie();
        ResponseCookie expiredSessionIdCookie = CookieUtils.expireSessionIdCookie();

        log.info("로그아웃 성공 - 토큰 블랙리스트 등록 및 쿠키 삭제 완료");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredSessionIdCookie.toString())
                .body(Map.of(
                        "success", true,
                        "message", "로그아웃 완료"
                ));
    }

    @GetMapping("/refresh-view")
    public void refreshView(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = "sessionId", required = false) String sessionId,
            @RequestParam(name = "redirect", defaultValue = "/") String redirectUri,
            HttpServletResponse response
    ) throws IOException {
        if (refreshToken == null || sessionId == null) {
            response.sendRedirect("/login?error=sessionExpired");
            return;
        }

        JwtRedisService.TokenReissueResult result = jwtRedisService.reissueTokensAndSetCookies(refreshToken, sessionId, response);

        if (!result.success) {
            response.sendRedirect("/login?error=refreshInvalid");
        } else {
            response.sendRedirect(redirectUri);
        }
    }

    @PostMapping("/refresh-rest")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = "sessionId", required = false) String sessionId,
            HttpServletResponse response
    ) {
        if (refreshToken == null || sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("RefreshToken 또는 sessionId 없음");
        }

        JwtRedisService.TokenReissueResult result = jwtRedisService.reissueTokensAndSetCookies(refreshToken, sessionId, response);

        if (!result.success) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 RefreshToken");
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AccessToken 재발급 완료",
                "accessToken", result.accessToken
        ));
    }

}