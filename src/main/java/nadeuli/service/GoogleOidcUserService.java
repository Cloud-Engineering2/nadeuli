/*
 * GoogleOidcUserService.java
 * Google OIDC 로그인 인증 처리 및 사용자 정보 관리 서비스
 * - 사용자 정보를 기반으로 회원 등록 또는 업데이트 처리
 * - JWT Access/Refresh Token 생성 및 사용자 정보에 저장
 * 작성자 : 김대환, 국경민
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 김대환, 국경민   2025.03.19     최초 작성 - Google OIDC 인증 및 JWT 토큰 발급 로직 구현
 * 김대환 2025.03.19 User_Role 기본값 지정
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.entity.constant.UserRole;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if (!"google".equals(provider)) {
            log.warn("[GoogleOidcUserService] Unsupported provider '{}'", provider);
            return oidcUser;
        }

        String googleAccessToken = userRequest.getAccessToken().getTokenValue();
        log.info("[Google OAuth] Google Access Token: {}", googleAccessToken);

        return processOidcUser(oidcUser, provider, googleAccessToken);
    }

    private OidcUser processOidcUser(OidcUser oidcUser, String provider, String googleAccessToken) {
        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String picture = getSafeString(attributes, "picture");

        log.info("[Google OIDC] Email: {}, Name: {}, Picture: {}", email, name, picture);

       User userEntity = userRepository.findByUserEmail(email)
                .map(existing -> updateExistingUser(existing, name, picture, googleAccessToken))
                .orElseGet(() -> {
                    User newUser = createNewUser(email, name, picture, googleAccessToken);
                    newUser.setUserRole(UserRole.MEMBER);
                    return newUser;
                });

        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        userEntity.updateRefreshToken(refreshTokenResponse.token, refreshTokenResponse.expiryAt);
        userRepository.save(userEntity);

        String accessToken = jwtTokenService.generateAccessToken(email);

        log.info("[Google OIDC] JWT Access Token 발급 완료: {}", accessToken);
        log.info("[Google OIDC] JWT Refresh Token 발급 완료: {}", refreshTokenResponse.token);

        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("accessToken", accessToken);
        updatedAttributes.put("email", email);

        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }

    private User updateExistingUser(User user, String name, String profileImage, String googleAccessToken) {
        boolean tokenUpdated = !googleAccessToken.equals(user.getUserToken());

        if (tokenUpdated) {
            log.info("Google Access Token 변경 감지! 기존 값: {}, 새 값: {}", user.getUserToken(), googleAccessToken);
            user.updateProfile(name, profileImage, "google", googleAccessToken, LocalDateTime.now());

            JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(user.getUserEmail());
            user.updateRefreshToken(refreshTokenResponse.token, refreshTokenResponse.expiryAt);
        } else {
            log.info("기존 Google Access Token 유지: {}", googleAccessToken);
            user.updateProfile(name, profileImage, "google", null, LocalDateTime.now());
        }

        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String googleAccessToken) {
        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "google", googleAccessToken, LocalDateTime.now(), refreshTokenResponse.token, refreshTokenResponse.expiryAt);
    }

    private String getSafeString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}
