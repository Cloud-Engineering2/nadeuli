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
 * 국경민      03-12      Optional 처리 최적화 및 보안 강화
 * 국경민      03-12      성능 최적화 및 lastLoginAt 업데이트 추가
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ 기존 Refresh Token 반환 또는 새로 생성 (Google 정책 반영)
     */
    @Transactional
    public String getOrGenerateRefreshToken(String userEmail, String provider) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("🚨 [getOrGenerateRefreshToken] 사용자를 찾을 수 없음 - Email: " + userEmail));

        if ("google".equals(provider) &&
                (user.getRefreshToken() == null || user.getRefreshTokenExpiryAt().isBefore(LocalDateTime.now().minusMonths(5)))) {
            log.info("🔄 [getOrGenerateRefreshToken] Google Refresh Token 만료 - 새로 발급");
            String newRefreshToken = requestNewGoogleRefreshToken(userEmail);
            updateRefreshToken(userEmail, newRefreshToken, provider);
            return newRefreshToken;
        }

        return user.getRefreshToken();
    }

    /**
     * ✅ Refresh Token을 이용하여 새로운 Access Token 발급 (로그 추가 및 예외 처리 강화)
     */
    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("🚨 [refreshAccessToken] 제공된 Refresh Token이 null 또는 빈 값임");
            return Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."
            );
        }

        User user = userRepository.findByRefreshToken(refreshToken).orElse(null);

        if (user == null || user.getRefreshTokenExpiryAt().isBefore(LocalDateTime.now())) {
            log.warn("🚨 [refreshAccessToken] 유효하지 않거나 만료된 Refresh Token");
            return Map.of(
                    "success", false,
                    "message", "🚨 Refresh Token이 만료되었습니다. 다시 로그인해주세요."
            );
        }

        String newAccessToken = jwtTokenService.createAccessToken(user.getUserEmail());

        log.info("✅ [refreshAccessToken] 새로운 Access Token 발급 완료 - Email: {}", user.getUserEmail());
        return Map.of(
                "success", true,
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", "30분"
        );
    }

    /**
     * ✅ 회원 탈퇴 시 Refresh Token 삭제
     */
    @Transactional
    public boolean deleteRefreshToken(String userEmail) {
        User user = userRepository.findByUserEmail(userEmail).orElse(null);

        if (user != null) {
            user.updateProfile(user.getUserName(), user.getProfileImage(), user.getProvider(), null, user.getLastLoginAt(), null);
            userRepository.save(user);
            log.info("✅ [deleteRefreshToken] Refresh Token 삭제 완료 - Email: {}", userEmail);
            return true;
        }
        log.warn("🚨 [deleteRefreshToken] 사용자 정보를 찾을 수 없음 - Email: {}", userEmail);
        return false;
    }

    /**
     * ✅ Google Refresh Token 갱신 요청 (Google 사용자 전용)
     */
    public String requestNewGoogleRefreshToken(String email) {
        log.info("🔄 [requestNewGoogleRefreshToken] Google Refresh Token 갱신 요청 - Email: {}", email);
        return "new_refresh_token"; // ✅ 실제 Google API 연동 필요
    }

    /**
     * ✅ Refresh Token 갱신 (Google 사용자의 경우 5개월(150일) 이상 사용되지 않으면 갱신)
     */
    @Transactional
    public void updateRefreshToken(String userEmail, String newRefreshToken, String provider) {
        log.info("🟡 [updateRefreshToken] 실행됨 - Email: {}, Provider: {}", userEmail, provider); // ✅ 로그 추가

        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("🚨 [updateRefreshToken] 사용자를 찾을 수 없음 - Email: " + userEmail));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiry = now.plusMonths(6);

        if ("google".equals(provider)) {
            boolean shouldUpdate = user.getRefreshTokenExpiryAt() == null ||
                    user.getRefreshTokenExpiryAt().isBefore(now.minusDays(150));

            if (shouldUpdate) {
                log.info("🔄 [updateRefreshToken] Google Refresh Token 갱신 - Email: {}", userEmail);
                user.updateProfile(user.getUserName(), user.getProfileImage(), provider, newRefreshToken, user.getLastLoginAt(), newExpiry);
                userRepository.save(user);
                log.info("✅ [updateRefreshToken] Google Refresh Token 저장 완료 - Email: {}", userEmail);
            } else {
                log.info("🔹 [updateRefreshToken] Google Refresh Token 유지 - Email: {}", userEmail);
            }
        }
    }
}
