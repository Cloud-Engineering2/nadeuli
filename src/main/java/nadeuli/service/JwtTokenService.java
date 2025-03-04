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
 * ========================================================
 */

package nadeuli.service;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository; // ✅ DB 업데이트를 위해 추가

    @Value("${jwt.secret}")
    private String secretKey;

    private final long accessTokenValidTime = 30 * 60 * 1000L; // 30분
    private final long refreshTokenValidTime = 7 * 24 * 60 * 60 * 1000L; // 1주일

    /**
     * ✅ JWT 액세스 토큰 생성
     */
    public String createAccessToken(String userPk) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userPk)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenValidTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * ✅ JWT 리프레시 토큰 생성
     */
    public String createRefreshToken(String userPk) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userPk)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * ✅ JWT 유효성 검사 (validateToken)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ✅ JWT에서 사용자 이메일 추출 (getUserEmail)
     */
    public String getUserEmail(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * ✅ Redis에 JWT 저장 (storeToken)
     */
    public void storeToken(String key, String token, long duration) {
        redisTemplate.opsForValue().set(key, token, duration, TimeUnit.MILLISECONDS);
    }

    /**
     * ✅ Redis에서 JWT 삭제 (deleteTokens)
     */
    public void deleteTokens(String key) {
        redisTemplate.delete(key);
    }

    /**
     * ✅ JWT 재발급 및 DB 업데이트
     */
    public String refreshAccessToken(String email, String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token is invalid or expired.");
        }

        String newAccessToken = createAccessToken(email);
        userRepository.updateAccessTokenByEmail(email, newAccessToken); // ✅ DB에 새로운 accessToken 저장
        storeToken("accessToken:" + email, newAccessToken, accessTokenValidTime); // ✅ Redis에도 저장

        return newAccessToken;
    }
}

