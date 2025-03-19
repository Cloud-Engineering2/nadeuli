/*
 * JwtTokenService.java
 * JWT 토큰 발급 및 검증 서비스
 * - Access Token / Refresh Token 생성
 * - JWT 유효성 검증 및 이메일 추출 기능 제공
 * - 서명 키(secret key)는 HMAC SHA256 방식 사용
 *
 * 작성자 : 국경민, 김대환
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 국경민, 김대환   2025.03.19     최초 작성 - JWT 발급 및 검증 로직 구현
 * ========================================================
 */

package nadeuli.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenService {

    private final Key key;
    private static final long ACCESS_TOKEN_EXPIRATION =  60 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7;

    public JwtTokenService(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return generateToken(email, ACCESS_TOKEN_EXPIRATION);
    }

    public TokenResponse generateRefreshToken(String email) {
        String token = generateToken(email, REFRESH_TOKEN_EXPIRATION);
        LocalDateTime expiryAt = LocalDateTime.now().plusDays(7);
        log.info("새로운 Refresh Token: {} | 만료 시간: {}", token, expiryAt);
        return new TokenResponse(token, expiryAt);
    }

    private String generateToken(String email, long expirationTime) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException e) {
            log.warn("JWT에서 이메일 추출 실패: {} | Token: {}", e.getMessage(), token);
            throw new JwtException("이메일 추출 실패 - 잘못된 JWT입니다.");
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT: {} | Token: {}", e.getMessage(), token);
            throw new JwtException("Access Token이 만료되었습니다.");
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {} | Token: {}", e.getMessage(), token);
            throw new JwtException("Access Token이 유효하지 않습니다.");
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT: {} | Token: {}", e.getMessage(), token);
            throw e;
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {} | Token: {}", e.getMessage(), token);
            throw e;
        }
    }

    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusDays(7);
    }

    public static class TokenResponse {
        public final String token;
        public final LocalDateTime expiryAt;

        public TokenResponse(String token, LocalDateTime expiryAt) {
            this.token = token;
            this.expiryAt = expiryAt;
        }
    }
}