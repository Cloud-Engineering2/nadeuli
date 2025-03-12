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
     * ✅ Refresh Token 갱신 (로그인 시 5개월(150일) 이상 사용되지 않은 경우 새롭게 발급)
     */
    @Transactional
    public void updateRefreshToken(String userEmail, String newRefreshToken) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("🚨 [updateRefreshToken] 사용자를 찾을 수 없음 - Email: " + userEmail));

        LocalDateTime newExpiry = LocalDateTime.now().plusMonths(6); // 6개월 후 만료

        // ✅ 기존 Refresh Token이 없거나 다르면 갱신
        if (user.getRefreshToken() == null || !newRefreshToken.equals(user.getRefreshToken())) {
            user.updateProfile(user.getUserName(), user.getProfileImage(), user.getProvider(), newRefreshToken, user.getLastLoginAt(), newExpiry);
            userRepository.save(user);
            log.info("✅ [updateRefreshToken] Refresh Token 갱신 완료 - Email: {}, Expiry: {}", userEmail, newExpiry);
        } else {
            log.info("🔹 [updateRefreshToken] 기존 Refresh Token과 동일하여 갱신하지 않음 - Email: {}", userEmail);
        }
    }

    /**
     * ✅ Refresh Token을 검증하고 새로운 Access Token 발급
     */
    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        // 🚨 Null 또는 빈 Refresh Token 검증 (불필요한 DB 조회 방지)
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("🚨 [refreshAccessToken] 제공된 Refresh Token이 null 또는 빈 값임");
            return generateErrorResponse("🚨 유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElse(null);

        // 🚨 Refresh Token이 존재하지 않거나 유효하지 않음
        if (user == null) {
            log.warn("🚨 [refreshAccessToken] 유효하지 않거나 존재하지 않는 Refresh Token");
            return generateErrorResponse("🚨 유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
        }

        // 🚨 Refresh Token 만료 검사
        if (isRefreshTokenExpired(user)) {
            log.warn("🚨 [refreshAccessToken] Refresh Token 만료됨 - Email: {}", user.getUserEmail());

            // ✅ 보안 강화: 만료된 Refresh Token 삭제
            user.updateProfile(user.getUserName(), user.getProfileImage(), user.getProvider(), null, user.getLastLoginAt(), null);
            userRepository.save(user);

            return generateErrorResponse("🚨 Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
        }

        // ✅ 새로운 Access Token 발급
        String newAccessToken = jwtTokenService.createAccessToken(user.getUserEmail());

        // ✅ 로그인 시간 업데이트 (비정상적인 패턴 감지 가능)
        user.updateProfile(user.getUserName(), user.getProfileImage(), user.getProvider(), user.getRefreshToken(), LocalDateTime.now(), user.getRefreshTokenExpiryAt());
        userRepository.save(user);

        log.info("✅ [refreshAccessToken] 새로운 Access Token 발급 완료 - Email: {}", user.getUserEmail());
        return Map.of(
                "success", true,
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", "30분"
        );
    }

    /**
     * ✅ Refresh Token 만료 여부 확인 메서드
     */
    private boolean isRefreshTokenExpired(User user) {
        return user.getRefreshTokenExpiryAt() != null && user.getRefreshTokenExpiryAt().isBefore(LocalDateTime.now());
    }

    /**
     * ✅ 에러 응답 JSON 생성 메서드
     */
    private Map<String, Object> generateErrorResponse(String message) {
        return Map.of(
                "success", false,
                "message", message
        );
    }
}
