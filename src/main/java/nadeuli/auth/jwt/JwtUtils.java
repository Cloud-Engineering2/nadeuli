package nadeuli.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import nadeuli.common.util.CookieUtils;
import nadeuli.common.enums.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;


@Slf4j
@Component
public class JwtUtils {

    private final RedisTemplate<String, String> redisTemplate;
    private final Key key;

    public static final String EXCEPTION_ATTRIBUTE = "exception";
    private static final long ACCESS_TOKEN_EXPIRATION = 50 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    public JwtUtils(@Value("${jwt.secret}") String secretKey,
                           RedisTemplate<String, String> redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
    }

    /**
     * JWT 토큰 생성 공통 로직
     */
    private String generateToken(String email, long expirationTime) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * AccessToken을 생성합니다.
     */
    public String generateAccessToken(String email) {
        return generateToken(email, ACCESS_TOKEN_EXPIRATION);
    }

    /**
     * RefreshToken을 생성하고 만료시간 포함하여 반환합니다.
     */
    public TokenResponse generateRefreshToken(String email) {
        String token = generateToken(email, REFRESH_TOKEN_EXPIRATION);
        LocalDateTime expiryAt = getRefreshTokenExpiry();
        log.info("✅ Refresh Token 생성: {} | 만료: {}", token, expiryAt);
        return new TokenResponse(token, expiryAt);
    }

    /**
     * JWT 토큰의 유효성을 검사합니다.
     */
    public Optional<ErrorCode> validateToken(String token) {
        try {
            extractAllClaims(token);
            return Optional.empty(); // 검증 성공
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("INVALID_TOKEN_SIGNATURE: {}", e.getMessage());
            return Optional.of(ErrorCode.INVALID_TOKEN_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.warn("EXPIRED_TOKEN: {}", e.getMessage());
            return Optional.of(ErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("UNSUPPORTED_TOKEN: {}", e.getMessage());
            return Optional.of(ErrorCode.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("INVALID_TOKEN: {}", e.getMessage());
            return Optional.of(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.warn("처리되지 않은 JWT 오류입니다: {}", e.getMessage());
            return Optional.of(ErrorCode.UNEXPECTED_TOKEN);
        }
    }

    /**
     * JWT에서 email(subject) 추출
     */
    public String extractEmail(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (JwtException e) {
            log.warn("JWT에서 이메일 추출 실패: {} | Token: {}", e.getMessage(), token);
            throw new JwtException("이메일 추출 실패 - 잘못된 JWT입니다.");
        }
    }

    /**
     * JWT 토큰에서 Claims(페이로드 데이터)를 파싱하여 반환합니다.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * RefreshToken 만료 시각 계산
     */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusDays(7);
    }

    /**
     * Redis 키 구성 - jwt:refresh:{email}:{sessionId}
     */
    private String buildRedisKey(String email, String sessionId) {
        return "jwt:refresh:" + URLEncoder.encode(email, StandardCharsets.UTF_8) + ":" + sessionId;
    }

    /**
     * RefreshToken을 Redis에 저장
     */
    public void storeRefreshTokenInRedis(String email, String sessionId, TokenResponse refreshToken) {
        String redisKey = buildRedisKey(email, sessionId);
        Duration expireDuration = Duration.between(LocalDateTime.now(), refreshToken.expiryAt);
        redisTemplate.opsForValue().set(redisKey, refreshToken.token, expireDuration);
    }


    /**
     * Redis에 저장된 RefreshToken과 요청된 토큰 일치 여부 검증
     */
    public boolean validateRefreshTokenInRedis(String email, String sessionId, String token) {
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
     * Redis에서 RefreshToken 삭제
     */
    public void deleteRefreshTokenFromRedis(String accessToken, String sessionId) {
        try {
            String email = extractEmail(accessToken);
            String key = buildRedisKey(email, sessionId);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis 삭제 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * AccessToken 블랙리스트 등록
     */
    public void blacklistAccessToken(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            Duration ttl = Duration.ofMillis(expiration.getTime() - System.currentTimeMillis());
            redisTemplate.opsForValue().set("jwt:blacklist:" + token, "blacklisted", ttl);
        } catch (Exception e) {
            log.error("AccessToken 블랙리스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * AccessToken 블랙리스트 여부 확인
     */
    public boolean isBlacklistedAccessToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token));
    }

    /**
     * RefreshToken 블랙리스트 등록
     */
    public void blacklistRefreshToken(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
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
     * RefreshToken 유효 시 토큰 재발급 및 쿠키 세팅
     */
    public TokenReissueResult reissueTokensAndSetCookies(String refreshToken, String sessionId, HttpServletResponse response) {
        try {
            // 1. 블랙리스트 체크
            if (isBlacklistedRefreshToken(refreshToken)) {
                throw new IllegalStateException("Blacklisted Refresh Token");
            }

            // 2. 토큰에서 이메일 추출
            String email = extractEmail(refreshToken);

            // 3. Redis에 저장된 RefreshToken과 비교
            if (!validateRefreshTokenInRedis(email, sessionId, refreshToken)) {
                throw new IllegalStateException("Invalid Refresh Token in Redis");
            }

            // 4. 새로운 Access/RefreshToken 발급
            String newAccessToken = generateAccessToken(email);
            TokenResponse newRefreshToken = generateRefreshToken(email);

            // 5. 기존 RefreshToken → 블랙리스트 등록
            blacklistRefreshToken(refreshToken);

            // 6. Redis에 새로운 RefreshToken 저장
            storeRefreshTokenInRedis(email, sessionId, newRefreshToken);

            // 7. 쿠키 설정
            ResponseCookie accessTokenCookie = CookieUtils.createAccessTokenCookie(newAccessToken);
            ResponseCookie refreshTokenCookie = CookieUtils.createRefreshTokenCookie(newRefreshToken.token);
            ResponseCookie sessionIdCookie = CookieUtils.createSessionIdCookie(sessionId);
            CookieUtils.addCookies(response, accessTokenCookie, refreshTokenCookie, sessionIdCookie);

            return new TokenReissueResult(true, email, newAccessToken, newRefreshToken.token);

        } catch (Exception e) {
            log.error("토큰 재발급 실패: {}", e.getMessage(), e);
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

    /** 토큰 응답 DTO */
    public static class TokenResponse {
        public final String token;
        public final LocalDateTime expiryAt;

        public TokenResponse(String token, LocalDateTime expiryAt) {
            this.token = token;
            this.expiryAt = expiryAt;
        }
    }
}
