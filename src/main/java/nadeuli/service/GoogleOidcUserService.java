package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
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

        return processOidcUser(oidcUser, provider);
    }

    private OidcUser processOidcUser(OidcUser oidcUser, String provider) {
        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String picture = getSafeString(attributes, "picture");

        log.info("[Google OIDC] Email: {}, Name: {}, Picture: {}", email, name, picture);

        User userEntity = userRepository.findByUserEmail(email)
                .map(existing -> updateExistingUser(existing, name, picture))
                .orElseGet(() -> createNewUser(email, name, picture));

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


    private User updateExistingUser(User user, String name, String profileImage) {
        user.updateProfile(name, profileImage, "google", null, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage) {
        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "google", null, LocalDateTime.now(), refreshTokenResponse.token, refreshTokenResponse.expiryAt);
    }

    private String getSafeString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}
