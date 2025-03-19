package nadeuli.config.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
        return customizeRequest(request, req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
        return customizeRequest(request, req);
    }

    private OAuth2AuthorizationRequest customizeRequest(HttpServletRequest request, OAuth2AuthorizationRequest req) {
        if (req == null) return null;

        String redirect = request.getParameter("redirect");
        Map<String, Object> additionalParams = new HashMap<>(req.getAdditionalParameters());

        if (redirect != null && !redirect.isBlank()) {
            additionalParams.put("redirect", redirect);
        }

        // 무조건 state 값은 존재해야 함 → redirect가 없더라도 기본값 넣기
        String stateValue = (redirect != null && !redirect.isBlank())
                ? Base64.getUrlEncoder().encodeToString(redirect.getBytes())
                : Base64.getUrlEncoder().encodeToString("/itinerary/mylist".getBytes()); // or UUID.randomUUID().toString()

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(additionalParams)
                .state(stateValue)
                .build();
    }

}
