/* JwtTokenProvider.java
 * jwtTokenProvider
 * 해당 파일 설명
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.26       jwt 사용하기위해 기본토대
 * 국경민      2.27       jwt를 redis로 사용하기 위해 변경
 * ========================================================
 */
package nadeuli.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RedisTemplate<String, Object> redisTemplate;

    private String secretKey = "yourSecretKey"; // 원하는 시크릿 키로 변경하세요
    private long accessTokenValidTime = 30 * 60 * 1000L; // 30분
    private long refreshTokenValidTime = 7 * 24 * 60 * 60 * 1000L; // 1주일

    // 액세스 토큰 생성 메서드
    public String createAccessToken(String userPk) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidTime);
        return JWT.create()
                .withSubject(userPk)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(secretKey));
    }

    // 리프레시 토큰 생성 메서드
    public String createRefreshToken(String userPk) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidTime);
        return JWT.create()
                .withSubject(userPk)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(secretKey));
    }

    // Redis에 토큰 저장
    public void storeToken(String key, String token, long duration) {
        redisTemplate.opsForValue().set(key, token, duration, TimeUnit.MILLISECONDS);
    }

    // Redis에서 토큰 가져오기
    public String getToken(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // JWT 토큰 검증 메서드
    public DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token);
    }

    // Redis에서 토큰 삭제
    public void deleteToken(String key) {
        redisTemplate.delete(key);
    }

    // JWT 토큰에서 사용자 이메일 가져오기
    public String getUserEmail(String token) {
        DecodedJWT decodedJWT = verifyToken(token);
        return decodedJWT.getSubject();
    }

    // JWT 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

