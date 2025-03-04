/* UserRepository.java
 * 사용자 정보를 관리하는 JPA Repository
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       사용자 조회 및 삭제 메서드
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ✅ 이메일을 기반으로 사용자 찾기
     */
    Optional<User> findByUserEmail(String email);

    /**
     * ✅ 이메일을 기반으로 사용자 삭제
     */
    void deleteByUserEmail(String email);

    /**
     * ✅ 해당 이메일이 존재하는지 확인
     */
    boolean existsByUserEmail(String email);

    /**
     * ✅ 이메일을 기반으로 사용자의 accessToken 조회
     */
    Optional<User> findByUserEmailAndAccessToken(String email, String accessToken);

    /**
     * ✅ 특정 사용자의 access_token을 업데이트
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.accessToken = :accessToken WHERE u.userEmail = :email")
    void updateAccessTokenByEmail(String email, String accessToken);
}
