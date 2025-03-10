/* RefreshTokenService.java
 * Refresh Token 관리 및 갱신 로직
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-07      RefreshTokenService 초안
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;

    /**
     * ✅ Refresh Token 갱신 (로그인 시 5개월(150일) 이상 사용되지 않은 경우 새롭게 발급)
     */
    @Transactional
    public void updateRefreshToken(String userEmail, String newRefreshToken) {
        Optional<User> userOpt = userRepository.findByUserEmail(userEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            LocalDateTime newExpiry = LocalDateTime.now().plusMonths(6); // 6개월 후 만료

            user.updateProfile(user.getUserName(), user.getProfileImage(), user.getProvider(), newRefreshToken, user.getLastLoginAt(), newExpiry);
            userRepository.save(user);

            log.info("✅ [updateRefreshToken] Refresh Token 갱신 완료 - Email: {}, Expiry: {}", userEmail, newExpiry);
        } else {
            log.warn("🚨 [updateRefreshToken] 해당 사용자를 찾을 수 없음 - Email: {}", userEmail);
        }
    }

    /**
     * ✅ Refresh Token을 검증하고 새로운 Access Token 발급
     */
    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        Optional<User> userOpt = userRepository.findByRefreshToken(refreshToken);

        if (userOpt.isEmpty()) {
            log.warn("🚨 [refreshAccessToken] 유효하지 않거나 존재하지 않는 Refresh Token");
            return Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."
            );
        }

        User user = userOpt.get();

        // ✅ Refresh Token 만료 검사
        if (user.getRefreshTokenExpiryAt() != null && user.getRefreshTokenExpiryAt().isBefore(LocalDateTime.now())) {
            log.warn("🚨 [refreshAccessToken] Refresh Token 만료됨 - Email: {}", user.getUserEmail());
            return Map.of(
                    "success", false,
                    "message", "🚨 Refresh Token이 만료되었습니다. 다시 로그인해주세요."
            );
        }

        // ✅ 새로운 Access Token 발급
        String newAccessToken = generateAccessToken(user.getUserEmail());

        log.info("✅ [refreshAccessToken] 새로운 Access Token 발급 완료 - Email: {}", user.getUserEmail());
        return Map.of(
                "success", true,
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", "30분"
        );
    }

    /**
     * ✅ Access Token 발급 메서드 (JWT 발급 로직 연결 필요)
     */
    private String generateAccessToken(String userEmail) {
        // JWT 발급 로직 적용 필요
        return "newAccessTokenFor_" + userEmail;
    }
}

