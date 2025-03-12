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
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long ACCESS_TOKEN_VALID_TIME = 30 * 60 * 1000L; // ✅ 30분

    /**
     * ✅ JWT 액세스 토큰 생성
     */
    public String createAccessToken(String userEmail) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALID_TIME))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ Redis에서 Access Token 저장
     */
    public void storeAccessToken(String userEmail, String accessToken) {
        String redisKey = "jwt:accessToken:" + userEmail;
        redisTemplate.opsForValue().set(redisKey, accessToken, ACCESS_TOKEN_VALID_TIME, TimeUnit.MILLISECONDS);
        log.info("✅ [storeAccessToken] Redis 저장 완료 - key: {}, TTL: {}ms", redisKey, ACCESS_TOKEN_VALID_TIME);
    }

    /**
     * ✅ Redis에서 Access Token 삭제 (로그아웃 시 호출됨)
     */
    public boolean deleteAccessToken(String userEmail) {
        String redisKey = "jwt:accessToken:" + userEmail;
        Boolean isDeleted = redisTemplate.delete(redisKey);
        log.info("✅ [deleteAccessToken] Redis Access Token 삭제 - key: {}, 결과: {}", redisKey, isDeleted);
        return Boolean.TRUE.equals(isDeleted);
    }

    /**
     * ✅ JWT 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).build().parseClaimsJws(token);
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
            return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("🚨 [getUserEmail] 만료된 토큰에서 이메일 추출");
            return e.getClaims().getSubject(); // ✅ 만료된 토큰에서도 이메일 추출 가능
        } catch (Exception e) {
            log.error("🚨 [getUserEmail] 토큰에서 이메일 추출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}
