package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {

        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equals(provider)) {
            log.warn("[OidcUserService] Unsupported provider '{}' - Only 'google' is allowed.", provider);
            return oidcUser;
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();

        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String picture = getSafeString(attributes, "picture");

        log.info("[Google OIDC] Email: {}, Name: {}, Picture: {}, AccessToken: {}",
                email, name, picture, accessToken);

        if (!email.isEmpty()) {
            User userEntity = userRepository.findByUserEmail(email)
                    .map(existing -> updateExistingUser(existing, name, picture, accessToken))
                    .orElseGet(() -> createNewUser(email, name, picture, accessToken));
            userRepository.save(userEntity);
            log.info("[Google OIDC] 사용자 저장 완료: {}", email);
        } else {
            log.warn("[Google OIDC] 이메일 정보가 없음 - OAuth 로그인 실패");
        }

        return oidcUser;
    }

    private User updateExistingUser(User user, String name, String profileImage, String accessToken) {
        user.updateProfile(name, profileImage, "google", accessToken, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String accessToken) {
        return User.createNewUser(email, name, profileImage, "google", accessToken, LocalDateTime.now());
    }

    private String getSafeString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}
