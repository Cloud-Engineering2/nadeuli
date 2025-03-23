/* JwtRedisService.java
 * nadeuli Service - 여행
 * JWT RefreshToken 및 블랙리스트 Redis 관리 유틸리티 클래스
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.23
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.23     최초 작성 : RefreshToken 저장/검증/삭제 및 블랙리스트 관리 구현
 * 박한철    2025.03.23     AccessToken 블랙리스트 관리 로직 추가 (현재 성능상 미사용)
 * 박한철    2025.03.23     토큰 재발급 및 쿠키 응답 처리 기능 구현
 */

package nadeuli.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.common.util.JwtUtils;
import nadeuli.dto.response.TokenResponse;
import nadeuli.common.util.CookieUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis 키 구성 - jwt:refresh:{email}:{sessionId}
     */
    public String buildRedisKey(String email, String sessionId) {
        return "jwt:refresh:" + URLEncoder.encode(email, StandardCharsets.UTF_8) + ":" + sessionId;
    }

    /**
     * RefreshToken 저장
     */
    public void storeRefreshToken(String email, String sessionId, TokenResponse refreshToken) {
        String redisKey = buildRedisKey(email, sessionId);
        Duration expireDuration = Duration.between(LocalDateTime.now(), refreshToken.expiryAt);
        redisTemplate.opsForValue().set(redisKey, refreshToken.token, expireDuration);
    }

    /**
     * RefreshToken 검증
     */
    public boolean validateRefreshToken(String email, String sessionId, String token) {
        String key = buildRedisKey(email, sessionId);
        try {
            String storedToken = redisTemplate.opsForValue().get(key);
            return token.equals(storedToken);
        } catch (Exception e) {
            log.error("Redis 조회 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * RefreshToken 삭제
     */
    public void deleteRefreshToken(String accessToken, String sessionId) {
        try {
            String email = JwtUtils.extractEmail(accessToken);
            String key = buildRedisKey(email, sessionId);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis 삭제 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * RefreshToken 블랙리스트 등록
     */
    public void blacklistRefreshToken(String token) {
        try {
            Date expiration = JwtUtils.extractAllClaims(token).getExpiration();
            Duration ttl = Duration.ofMillis(expiration.getTime() - System.currentTimeMillis());
            redisTemplate.opsForValue().set("jwt:blacklist:refresh:" + token, "blacklisted", ttl);
        } catch (Exception e) {
            log.error("RefreshToken 블랙리스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * RefreshToken 블랙리스트 여부 확인
     */
    public boolean isBlacklistedRefreshToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:refresh:" + token));
    }

    /**
     * AccessToken 블랙리스트 등록
     */
    public void blacklistAccessToken(String token) {
        try {
            Date expiration = JwtUtils.extractAllClaims(token).getExpiration();
            Duration ttl = Duration.ofMillis(expiration.getTime() - System.currentTimeMillis());
            redisTemplate.opsForValue().set("jwt:blacklist:" + token, "blacklisted", ttl);
        } catch (Exception e) {
            log.error("AccessToken 블랙리스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * AccessToken 블랙리스트 여부 확인
     * (필터가 매번 redis에 접근하는건 성능상 안좋을거같아서 미사용중 - 엑세스토큰주기를 줄이는걸로 해결)
     */
    public boolean isBlacklistedAccessToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token));
    }

    /**
     * 토큰 재발급 및 쿠키 세팅
     */
    public TokenReissueResult reissueTokensAndSetCookies(String refreshToken, String sessionId, HttpServletResponse response) {
        try {
            if (isBlacklistedRefreshToken(refreshToken)) {
                throw new IllegalStateException("Blacklisted Refresh Token");
            }

            String email = JwtUtils.extractEmail(refreshToken);

            if (!validateRefreshToken(email, sessionId, refreshToken)) {
                throw new IllegalStateException("Invalid Refresh Token in Redis");
            }

            String newAccessToken = JwtUtils.generateAccessToken(email);
            TokenResponse newRefreshToken = JwtUtils.generateRefreshToken(email);

            blacklistRefreshToken(refreshToken);

            storeRefreshToken(email, sessionId, newRefreshToken);

            ResponseCookie accessTokenCookie = CookieUtils.createAccessTokenCookie(newAccessToken);
            ResponseCookie refreshTokenCookie = CookieUtils.createRefreshTokenCookie(newRefreshToken.token);
            ResponseCookie sessionIdCookie = CookieUtils.createSessionIdCookie(sessionId);
            CookieUtils.addCookies(response, accessTokenCookie, refreshTokenCookie, sessionIdCookie);

            return new TokenReissueResult(true, email, newAccessToken, newRefreshToken.token);
        } catch (Exception e) {
            log.error("토큰 재발급 실패: {}", e.getMessage(), e);
            CookieUtils.deleteAuthCookies(response);
            return new TokenReissueResult(false, null, null, null);
        }
    }

    /** 토큰 재발급 결과 응답 DTO */
    public static class TokenReissueResult {
        public final boolean success;
        public final String email;
        public final String accessToken;
        public final String refreshToken;

        public TokenReissueResult(boolean success, String email, String accessToken, String refreshToken) {
            this.success = success;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
