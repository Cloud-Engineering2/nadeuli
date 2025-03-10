/* CustomOAuth2User.java
 * OAuth2User 구현체
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-06
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-06       CustomOAuth2User 초안
 * ========================================================
 */

package nadeuli.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nadeuli.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, UserDetails {

    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final User user;

    /**
     * ✅ OAuth2User 인터페이스 구현
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return (user != null && user.getUserName() != null) ? user.getUserName() : "Unknown User"; // ✅ Null 체크 추가
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * ✅ UserDetails 인터페이스 구현
     */
    @Override
    public String getPassword() {
        return null; // OAuth 로그인은 비밀번호가 필요 없음
    }

    @Override
    public String getUsername() {
        return (user != null && user.getUserEmail() != null) ? user.getUserEmail() : "unknown@email.com"; // ✅ Null 체크 추가
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
