/* CookieUtils.java
 * nadeuli Service - 여행
 * JWT 및 세션 관련 쿠키 처리 유틸리티 클래스
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.23
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.23     최초 작성 : Access/Refresh/Session 쿠키 생성 및 삭제 유틸 구현
 */
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

    /* Auth 관련 쿠키 전부 삭제 */
    public static void deleteAuthCookies(HttpServletResponse response) {
        ResponseCookie expiredAccessToken = CookieUtils.expireAccessTokenCookie();
        ResponseCookie expiredRefreshToken = CookieUtils.expireRefreshTokenCookie();
        ResponseCookie expiredSessionId = CookieUtils.expireSessionIdCookie();
        CookieUtils.addCookies(response, expiredAccessToken, expiredRefreshToken, expiredSessionId);
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