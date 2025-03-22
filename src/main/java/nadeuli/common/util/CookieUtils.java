package nadeuli.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class CookieUtils {

    private static final String PATH = "/";
    private static final boolean HTTP_ONLY = true;
    private static final boolean SECURE = true;

    /* AccessToken 쿠키 생성 */
    public static ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(60 * 60)
                .build();
    }

    /* RefreshToken 쿠키 생성 */
    public static ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }

    /* SessionId 쿠키 생성 */
    public static ResponseCookie createSessionIdCookie(String sessionId) {
        return ResponseCookie.from("sessionId", sessionId)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }

    /* AccessToken 쿠키 만료 (삭제) */
    public static ResponseCookie expireAccessTokenCookie() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)
                .build();
    }

    /* RefreshToken 쿠키 만료 (삭제) */
    public static ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)
                .build();
    }

    /* SessionId 쿠키 만료 (삭제) */
    public static ResponseCookie expireSessionIdCookie() {
        return ResponseCookie.from("sessionId", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)
                .build();
    }

    /* 다수의 쿠키를 응답에 추가 */
    public static void addCookies(HttpServletResponse response, ResponseCookie... cookies) {
        for (ResponseCookie cookie : cookies) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }
}