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
 * 국경민      03-12       서명 키 캐싱 및 Redis 키 네이밍 정리
 * 국경민      03-12       서명 키 캐싱 최적화 및 동기화 문제 해결
 * 국경민      03-12       JWT 예외 처리 방식 개선 및 성능 최적화
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

    private volatile Key signingKey; // ✅ 멀티스레드 환경에서 안전한 키 캐싱

    /**
     * 🔹 JWT 서명 키 생성 (최초 1회 캐싱)
     */
    private Key getSigningKey() {
        if (signingKey == null) {
            synchronized (this) {
                if (signingKey == null) { // ✅ 이중 체크로 불필요한 생성 방지
                    signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return signingKey;
    }

    /**
     * ✅ JWT 액세스 토큰 생성
     */
    public String createAccessToken(String userEmail) {
        return generateToken(userEmail, ACCESS_TOKEN_VALID_TIME);
    }

    /**
     * ✅ JWT 리프레시 토큰 생성
     */
    public String createRefreshToken(String userEmail) {
        return generateToken(userEmail, REFRESH_TOKEN_VALID_TIME);
    }

    /**
     * ✅ JWT 토큰 생성 공통 메서드
     */
    private String generateToken(String userEmail, long expirationTime) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userEmail)
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
            log.warn("🚨 [validateToken] 만료된 토큰");
        } catch (MalformedJwtException e) {
            log.warn("🚨 [validateToken] 변조된 토큰");
        } catch (UnsupportedJwtException e) {
            log.warn("🚨 [validateToken] 지원되지 않는 JWT");
        } catch (IllegalArgumentException e) {
            log.warn("🚨 [validateToken] 빈 토큰");
        } catch (Exception e) {
            log.warn("🚨 [validateToken] 유효하지 않은 JWT: {}", e.getMessage());
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
            log.warn("🚨 [getUserEmail] 만료된 토큰에서 이메일 추출");
            return e.getClaims().getSubject(); // ✅ 만료된 토큰에서도 이메일 추출 가능
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
        String redisKey = "jwt:" + key; // ✅ 일관된 키 네이밍 적용
        redisTemplate.opsForValue().set(redisKey, token, duration, TimeUnit.MILLISECONDS);
        log.info("✅ [storeToken] Redis 저장 완료 - key: {}, duration: {}ms", redisKey, duration);
    }

    /**
     * ✅ Redis에서 JWT 삭제
     */
    public boolean deleteTokens(String key) {
        String redisKey = "jwt:" + key;
        Boolean isDeleted = redisTemplate.delete(redisKey);
        log.info("✅ [deleteTokens] Redis 토큰 삭제 - key: {}, 결과: {}", redisKey, isDeleted);
        return Boolean.TRUE.equals(isDeleted);
    }
}
