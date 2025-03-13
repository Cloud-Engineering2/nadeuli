package nadeuli.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equals(provider)) {
            log.warn("[GoogleOidcUserService] Unsupported provider '{}' - Only 'google' is allowed.", provider);
            return oidcUser;
        }

        return processOidcUser(oidcUser, provider, request, response);
    }

    private OidcUser processOidcUser(OidcUser oidcUser, String provider,
                                     HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String picture = getSafeString(attributes, "picture");
        String accessToken = oidcUser.getIdToken().getTokenValue();

        log.info("[Google OIDC] Email: {}, Name: {}, Picture: {}, AccessToken: {}", email, name, picture, accessToken);

        User userEntity = userRepository.findByUserEmail(email)
                .map(existing -> updateExistingUser(existing, name, picture, accessToken))
                .orElseGet(() -> createNewUser(email, name, picture, accessToken));

        userRepository.save(userEntity);
        log.info("[Google OIDC] 사용자 저장 완료: {}", email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userEntity, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        log.info("SecurityContext 업데이트 완료 - 사용자: {}", email);

        HttpSession session = request.getSession(true);  // 세션 가져오기 (없으면 생성)
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        log.info("SecurityContext가 세션에 저장됨 (Google OIDC)");

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
