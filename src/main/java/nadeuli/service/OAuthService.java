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
 * 국경민      03-12       Refresh Token 갱신 최적화 및 성능 개선
 * 국경민      03-12       Lambda 변수 final 처리 및 getSafeAttribute 오버로딩 추가
 * 국경민      03-12       불필요한 매개변수 제거 및 코드 정리
 * 국경민      03-12       Refresh Token 전용 메서드 분리 (경고 해결)
 * 국경민      03-12       NullPointerException 방지 및 로직 최적화
 * 국경민      03-12       Refresh Token 저장 누락 문제 해결 및 DB 반영 확인
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
import java.util.Optional;

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

        // ✅ Google 로그인 시 Refresh Token 저장 (최초 로그인에서만 제공됨)
        final String refreshToken = "google".equals(provider)
                ? getRefreshTokenFromGoogle(authentication)
                : "";

        // ✅ 현재 시간 기준으로 로그인 및 Refresh Token 만료 시간 설정
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime refreshTokenExpiryAt = now.plusDays(14);  // 기본 2주 만료

        // ✅ 기존 사용자가 있으면 업데이트, 없으면 새로 저장
        final User userEntity = userRepository.findByUserEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, name, profileImage, provider, refreshToken, now, refreshTokenExpiryAt))
                .orElseGet(() -> createNewUser(email, name, profileImage, provider, refreshToken, now, refreshTokenExpiryAt));

        return UserDTO.from(userEntity);
    }

    /**
     * ✅ 기존 사용자 업데이트 메서드
     */
    private User updateExistingUser(User existingUser, String name, String profileImage, String provider,
                                    String newRefreshToken, LocalDateTime now, LocalDateTime refreshTokenExpiryAt) {

        // ✅ Refresh Token이 있으면 갱신 여부 확인
        final String updatedRefreshToken = Optional.ofNullable(newRefreshToken)
                .filter(rt -> !rt.isEmpty())
                .orElse(existingUser.getRefreshToken());

        // ✅ Refresh Token이 변경된 경우에만 DB 업데이트 수행
        boolean refreshTokenUpdated = !updatedRefreshToken.equals(existingUser.getRefreshToken());

        log.info("✅ [processOAuthUser] Refresh Token 갱신 여부 - 기존: {}, 신규: {}, 갱신여부: {}",
                existingUser.getRefreshToken(), updatedRefreshToken, refreshTokenUpdated);

        // ✅ Refresh Token 만료 시간이 150일 이상 경과한 경우 갱신
        if (refreshTokenUpdated || existingUser.getRefreshTokenExpiryAt() == null
                || existingUser.getRefreshTokenExpiryAt().isBefore(now.minusDays(150))) {
            log.info("🔄 [processOAuthUser] Refresh Token 갱신 필요 - Email: {}", existingUser.getUserEmail());
            refreshTokenService.updateRefreshToken(existingUser.getUserEmail(), updatedRefreshToken);
        }

        // ✅ 사용자 정보 업데이트
        existingUser.updateProfile(name, profileImage, provider, updatedRefreshToken, now, refreshTokenExpiryAt);
        log.info("✅ [processOAuthUser] 기존 사용자 정보 업데이트 완료 - Email: {}", existingUser.getUserEmail());

        return existingUser;
    }

    /**
     * ✅ 신규 사용자 생성 메서드
     */
    private User createNewUser(String email, String name, String profileImage, String provider,
                               String refreshToken, LocalDateTime now, LocalDateTime refreshTokenExpiryAt) {
        log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", email);
        return userRepository.save(User.createNewUser(email, name, profileImage, provider, refreshToken, now, refreshTokenExpiryAt));
    }

    /**
     * ✅ Google의 Refresh Token 가져오기 (경고 해결, 예외 처리 추가)
     */
    private String getRefreshTokenFromGoogle(OAuth2AuthenticationToken authentication) {
        return Optional.ofNullable(authentication)
                .map(OAuth2AuthenticationToken::getPrincipal)
                .map(OAuth2User::getAttributes)
                .map(attributes -> getSafeAttribute(attributes, "refresh_token"))
                .orElse("");
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (OAuth2User 버전)
     */
    private String getSafeAttribute(OAuth2User user, String key) {
        return Optional.ofNullable(user.getAttributes())
                .map(attrs -> attrs.get(key))
                .map(Object::toString)
                .orElse("");
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (Map 버전)
     */
    private String getSafeAttribute(Map<String, Object> attributes, String key) {
        return Optional.ofNullable(attributes)
                .map(attrs -> attrs.get(key))
                .map(Object::toString)
                .orElse("");
    }
}
