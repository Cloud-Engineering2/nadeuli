/* OAuthUserService.java
 * 구글 및 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴 시 DB에서 사용자 삭제
 * 해당 파일 설명
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.25        UserRepository 사용하도록 수정
 *
 * ========================================================
 */

package nadeuli.service;

import nadeuli.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository userRepository;

    @Transactional
    public boolean deleteUserByEmail(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            System.err.println("삭제 실패: 해당 이메일이 데이터베이스에 없음 → " + email);
            return false;
        }

        userRepository.deleteByEmail(email);
        System.out.println("삭제 완료: " + email);
        return true;
    }
}
