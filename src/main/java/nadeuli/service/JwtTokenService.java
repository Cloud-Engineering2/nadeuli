/* JwtTokenService.java
 * JWT 발급 및 관리 서비스
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       JWT 생성 및 검증 로직 초안
 * 국경민      03-05       Redis 저장 디버깅을 위한 로그 추가
 * 국경민      03-05       JWT 생성 및 검증 기능 추가
 * 국경민      03-05       JWT 생성 공통화 및 예외 처리 강화
 * 국경민      03-06       JWT 검증 방식 최신화 및 Redis Key 개선
 * ========================================================
 */

package nadeuli.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long ACCESS_TOKEN_VALID_TIME = 60 * 60 * 1000L; // 1시간
    private static final long REFRESH_TOKEN_VALID_TIME = 14 * 24 * 60 * 60 * 1000L; // 2주일

    /**
     * 🔹 JWT 서명 키 생성
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * ✅ JWT 액세스 토큰 생성
     */
    public String createAccessToken(String userPk) {
        return generateToken(userPk, ACCESS_TOKEN_VALID_TIME);
    }

    /**
     * ✅ JWT 리프레시 토큰 생성
     */
    public String createRefreshToken(String userPk) {
        return generateToken(userPk, REFRESH_TOKEN_VALID_TIME);
    }

    /**
     * ✅ JWT 토큰 생성 공통 메서드
     */
    private String generateToken(String userPk, long expirationTime) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userPk)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ JWT 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("🚨 [validateToken] 만료된 토큰: {}", token);
        } catch (Exception e) {
            log.warn("🚨 [validateToken] 유효하지 않은 JWT: {}", token);
        }
        return false;
    }

    /**
     * ✅ JWT에서 사용자 이메일 추출
     */
    public String getUserEmail(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("🚨 [getUserEmail] 만료된 토큰에서 이메일 추출 - {}", e.getClaims().getSubject());
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.error("🚨 [getUserEmail] 토큰에서 이메일 추출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Redis에서 저장된 Refresh Token 가져오기
     */
    public String getRefreshToken(String userEmail) {
        String redisKey = "jwt:refreshToken:" + userEmail;
        try {
            Object token = redisTemplate.opsForValue().get(redisKey);
            if (token instanceof String refreshToken) {
                return refreshToken;
            }
        } catch (Exception e) {
            log.error("🚨 [getRefreshToken] Redis에서 Refresh Token 조회 중 오류 발생: {}", e.getMessage());
        }
        return null;
    }

    /**
     * ✅ Redis에 JWT 저장
     */
    public void storeToken(String key, String token, long duration) {
        redisTemplate.opsForValue().set("jwt:" + key, token, duration, TimeUnit.MILLISECONDS);
        log.info("✅ [storeToken] Redis 저장 완료 - key: {}, duration: {}ms", key, duration);
    }

    /**
     * ✅ Redis에서 JWT 삭제
     */
    public boolean deleteTokens(String key) {
        Boolean isDeleted = redisTemplate.delete("jwt:" + key);
        log.info("✅ [deleteTokens] Redis 토큰 삭제 - key: {}, 결과: {}", key, isDeleted);
        return Boolean.TRUE.equals(isDeleted);
    }
}
