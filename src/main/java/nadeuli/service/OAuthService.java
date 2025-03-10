/* OAuthService.java
 * OAuth 로그인 성공 시 사용자 등록 및 업데이트 처리
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       OAuth 로그인 및 회원가입 로직 초안
 * 국경민      03-05       기존 회원 정보 업데이트 기능 추가
 * 국경민      03-05       User 객체 생성 방식 변경 (팩토리 메서드 적용)
 * 국경민      03-05       provider 값 업데이트 반영
 * 국경민      03-05       OAuth 사용자 정보 예외 처리 및 로그 추가
 * 국경민      03-06       OAuth 사용자 정보 예외 처리 및 로그 추가
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * ✅ OAuth 로그인 성공 시 사용자 등록 또는 업데이트
     */
    @Transactional
    public UserDTO processOAuthUser(OAuth2User user, String provider, OAuth2AuthenticationToken authentication) {
        if (user == null) {
            log.error("🚨 [processOAuthUser] OAuth2User 정보가 없습니다!");
            throw new IllegalArgumentException("OAuth2User 정보가 존재하지 않습니다.");
        }

        // ✅ 기본 OAuth 사용자 정보 가져오기
        final String email = getSafeAttribute(user, "email");
        final String name = getSafeAttribute(user, "name");
        final String profileImage = getSafeAttribute(user, "picture");
        final String refreshToken;

        // ✅ Google 로그인 시 `refresh_token` 저장 (최초 로그인에서만 제공됨)
        if ("google".equals(provider)) {
            Map<String, Object> details = authentication.getPrincipal().getAttributes();
            String newRefreshToken = getSafeAttribute(details, "refresh_token");
            refreshToken = newRefreshToken.isEmpty() ? "" : newRefreshToken;
        } else {
            refreshToken = "";
        }

        // ✅ 현재 시간 기준으로 로그인 및 Refresh Token 만료 시간 설정
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime refreshTokenExpiryAt = now.plusDays(14);  // 기본 2주 만료

        // ✅ 기존 사용자가 있으면 업데이트, 없으면 새로 저장
        User userEntity = userRepository.findByUserEmail(email)
                .map(existingUser -> {
                    String updatedRefreshToken = refreshToken.isEmpty() ? existingUser.getRefreshToken() : refreshToken;

                    log.info("✅ [processOAuthUser] Refresh Token 갱신 여부 - 기존: {}, 신규: {}",
                            existingUser.getRefreshToken(), updatedRefreshToken);

                    // ✅ 150일 이상 경과 시 Refresh Token 갱신
                    final LocalDateTime refreshExpiry = existingUser.getRefreshTokenExpiryAt();
                    if (refreshExpiry == null || refreshExpiry.isBefore(now.minusDays(150))) {
                        log.info("🔄 [processOAuthUser] Refresh Token 갱신 필요 - Email: {}", email);
                        refreshTokenService.updateRefreshToken(email, updatedRefreshToken);
                    }

                    // ✅ 사용자 정보 업데이트
                    existingUser.updateProfile(
                            name,
                            profileImage,
                            provider,
                            updatedRefreshToken,
                            now,  // ✅ 마지막 로그인 시간 업데이트
                            refreshTokenExpiryAt  // ✅ Refresh Token 만료일 업데이트
                    );

                    log.info("✅ [processOAuthUser] 기존 사용자 정보 업데이트 완료 - Email: {}", email);
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", email);

                    return userRepository.save(User.createNewUser(
                            email,
                            name,
                            profileImage,
                            provider,
                            refreshToken,
                            now,  // ✅ 계정 생성 시 마지막 로그인 시간 설정
                            refreshTokenExpiryAt  // ✅ Refresh Token 만료일 설정
                    ));
                });

        return UserDTO.from(userEntity);
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (OAuth2User 버전)
     */
    private String getSafeAttribute(OAuth2User user, String key) {
        return getSafeAttribute(user.getAttributes(), key);
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (Map 버전)
     */
    private String getSafeAttribute(Map<String, Object> attributes, String key) {
        try {
            Object value = attributes.get(key);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("🚨 [getSafeAttribute] {} 속성을 가져오는 중 오류 발생: {}", key, e.getMessage());
            return "";
        }
    }
}




