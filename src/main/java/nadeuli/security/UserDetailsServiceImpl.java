/* UserDetailsServiceImpl.java
 * Spring Security 사용자 인증을 위한 UserDetailsService 구현
 * 해당 파일 설명
 * 작성자 : 코파일럿
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민    2025-02-26   초기 UserDetailsService 작성
 * ========================================================
 */

package nadeuli.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserEmail())
                .password(user.getUserToken()) // 비밀번호 필드 이름에 따라 변경
                .roles(user.getUserRole().name())
                .build();
    }
}

