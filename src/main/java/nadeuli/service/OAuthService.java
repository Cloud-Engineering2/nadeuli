package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ OAuth 로그인 성공 시 사용자 정보 저장 또는 갱신
     */
    @Transactional
    public UserDTO processOAuthUser(OAuth2User user, String provider) {
        if (user == null) {
            log.error("🚨 [processOAuthUser] OAuth2User 정보가 없습니다!");
            throw new IllegalArgumentException("OAuth2User 정보가 존재하지 않습니다.");
        }

        // ✅ OAuth 사용자 정보 가져오기
        final String email = getSafeAttribute(user, "email");
        final String name = getSafeAttribute(user, "name");
        final String profileImage = getSafeAttribute(user, "picture");
        log.info("🔹 [processOAuthUser] 로그인 요청 - Email: {}, Provider: {}", email, provider);

        // ✅ Google OAuth 사용자의 경우 새로운 Refresh Token 요청
        final String refreshToken = "google".equals(provider)
                ? refreshTokenService.requestNewGoogleRefreshToken(email)
                : getSafeAttribute(user, "refresh_token");

        // ✅ 현재 시간 기준으로 로그인 및 Refresh Token 만료 시간 설정
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime refreshTokenExpiryAt = now.plusMonths(6);

        // ✅ 기존 사용자가 있으면 업데이트 후 저장, 없으면 새로 저장
        final User userEntity = userRepository.findByUserEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, name, profileImage, provider, refreshToken, now, refreshTokenExpiryAt))
                .orElseGet(() -> {
                    UserDTO newUserDTO = new UserDTO(null, email, name, profileImage, provider, null, refreshToken, now.toString(), refreshTokenExpiryAt.toString(), now.toString());
                    return createNewUser(newUserDTO);
                });

        // ✅ Access Token 발급 및 Redis 저장 (30분 TTL)
        String accessToken = jwtTokenService.createAccessToken(email);
        jwtTokenService.storeAccessToken(email, accessToken);
        log.info("✅ [processOAuthUser] Access Token 저장 완료 - Email: {}", email);

        return UserDTO.from(userEntity);
    }

    /**
     * ✅ 기존 사용자 업데이트 후 저장 (Google Refresh Token 5개월마다 갱신)
     */
    private User updateExistingUser(User existingUser, String name, String profileImage, String provider,
                                    String newRefreshToken, LocalDateTime now, LocalDateTime refreshTokenExpiryAt) {

        boolean shouldUpdateRefreshToken = "google".equals(provider) &&
                Optional.ofNullable(existingUser.getRefreshTokenExpiryAt())
                        .map(expiryAt -> expiryAt.isBefore(now.minusDays(150)))
                        .orElse(true);

        if (shouldUpdateRefreshToken) {
            log.info("🔄 [updateExistingUser] Google Refresh Token 갱신 필요 - Email: {}", existingUser.getUserEmail());
            refreshTokenService.updateRefreshToken(existingUser.getUserEmail(), newRefreshToken, provider);
        }

        existingUser.updateProfile(name, profileImage, provider, newRefreshToken, now, refreshTokenExpiryAt);

        // ✅ UserDTO로 변환 후 다시 toEntity() 호출하여 저장
        UserDTO updatedUserDTO = UserDTO.from(existingUser);
        User updatedUser = updatedUserDTO.toEntity();

        return userRepository.save(updatedUser);
    }

    /**
     * ✅ 신규 사용자 생성 후 저장 (toEntity() 적용)
     */
    private User createNewUser(UserDTO userDTO) {
        log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", userDTO.getUserEmail());
        return userRepository.save(userDTO.toEntity());
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기
     */
    private String getSafeAttribute(OAuth2User user, String key) {
        return Optional.ofNullable(user.getAttributes())
                .map(attrs -> attrs.get(key))
                .map(Object::toString)
                .orElse("");
    }
}
