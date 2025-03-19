/*
 * CustomAuthorizationRequestResolver.java
 * OAuth2 로그인 요청 시 redirect 파라미터(state) 커스터마이징 처리
 * - 요청 파라미터에 redirect가 있으면 Base64로 인코딩하여 state에 포함
 * - 없을 경우 기본값(/itinerary/mylist)으로 처리
 *
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성 - OAuth2 로그인 요청 redirect 파라미터 처리 로직 구현
 * ========================================================
 */

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
