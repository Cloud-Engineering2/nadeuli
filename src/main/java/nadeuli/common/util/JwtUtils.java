/* JwtUtils.java
 * nadeuli Service - 여행
 * JWT 토큰 생성 및 검증 유틸리티 클래스
 * 작성자 : 김대환, 국경민
 * 최초 작성 일자 : 2025.03.18
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 김대환,국경민 2025.03.18     최초 작성 : Access/RefreshToken 생성 및 유효성 검증 로직 구현
 * 박한철    2025.03.23      리펙토링
 */
package nadeuli.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import nadeuli.dto.response.TokenResponse;
import nadeuli.common.enums.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;


@Slf4j
public class JwtUtils {

    private static Key key;
    private static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    // secretKey 설정 메서드 (초기화 필요)
    public static void init(String secretKey) {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String generateToken(String email, long expirationTime) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateAccessToken(String email) {
        return generateToken(email, ACCESS_TOKEN_EXPIRATION);
    }

    public static TokenResponse generateRefreshToken(String email) {
        String token = generateToken(email, REFRESH_TOKEN_EXPIRATION);
        LocalDateTime expiryAt = getRefreshTokenExpiry();
        log.info("✅ Refresh Token 생성: {} | 만료: {}", token, expiryAt);
        return new TokenResponse(token, expiryAt);
    }

    public static Optional<ErrorCode> validateToken(String token) {
        try {
            extractAllClaims(token);
            return Optional.empty();
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
            log.warn("UNEXPECTED_TOKEN: {}", e.getMessage());
            return Optional.of(ErrorCode.UNEXPECTED_TOKEN);
        }
    }

    public static String extractEmail(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (JwtException e) {
            log.warn("JWT에서 이메일 추출 실패: {} | Token: {}", e.getMessage(), token);
            throw new JwtException("이메일 추출 실패 - 잘못된 JWT입니다.");
        }
    }

    public static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusDays(7);
    }
}

