/* CustomOidcUser.java
 * nadeuli Service - 여행
 * Spring Security OIDC 인증 사용자 커스텀 구현 클래스 (Google OAuth2 용)
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.23
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.23     최초 작성 : OidcUser 구현체 - Google 로그인 사용자 속성 처리
 */

package nadeuli.auth.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class CustomOidcUser implements OidcUser, Serializable {
    private static final long serialVersionUID = 1L;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    private final String nameAttributeKey;

    public CustomOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey
    ) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return attributes.get(nameAttributeKey).toString();
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }
}
