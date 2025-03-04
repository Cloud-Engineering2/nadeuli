/* OAuthService.java
 * OAuth 로그인 성공 시 사용자 등록 및 업데이트 처리
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       OAuth 로그인 및 회원가입 로직 초안
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final UserRepository userRepository;

    /**
     * ✅ OAuth 로그인 성공 시 사용자 등록 또는 업데이트
     */
    @Transactional
    public UserDTO processOAuthUser(OAuth2User user, String provider) { // ✅ provider 추가
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");
        String profileImage = user.getAttribute("picture");

        // ✅ provider가 null이면 기본값 "unknown" 설정
        final String finalProvider = (provider != null && !provider.isEmpty()) ? provider : "unknown";

        User userEntity = userRepository.findByUserEmail(email)
                .orElseGet(() -> userRepository.save(new User(email, name, profileImage, finalProvider))); // ✅ 람다식 내부에서 변경 없이 사용

        return UserDTO.from(userEntity);
    }
}
