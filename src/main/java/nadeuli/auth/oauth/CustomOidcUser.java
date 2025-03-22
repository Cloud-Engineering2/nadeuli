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
