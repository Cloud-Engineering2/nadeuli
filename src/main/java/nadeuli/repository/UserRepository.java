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
 * 국경민      03-04       db 토큰사용 제거
 * 국경민      03-07       비활성 사용자 및 Refresh Token 만료 사용자 조회 추가
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository  // ✅ JPA Repository임을 명시
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ✅ 이메일을 기반으로 사용자 찾기
     */
    Optional<User> findByUserEmail(String email);

    /**
     * ✅ Refresh Token을 기반으로 사용자 찾기
     */
    Optional<User> findByRefreshToken(String refreshToken);

    /**
     * ✅ 이메일을 기반으로 사용자 삭제 (트랜잭션 적용)
     */
    @Transactional
    void deleteByUserEmail(String email);

    /**
     * ✅ 5개월 이상 로그인하지 않은 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt <= :fiveMonthsAgo")
    List<User> findInactiveUsersSince(@Param("fiveMonthsAgo") LocalDateTime fiveMonthsAgo);

    /**
     * ✅ Refresh Token 만료 30일 전 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.refreshToken IS NOT NULL AND u.refreshTokenExpiryAt <= :thirtyDaysFromNow")
    List<User> findUsersWithExpiringRefreshTokens(@Param("thirtyDaysFromNow") LocalDateTime thirtyDaysFromNow);
}
