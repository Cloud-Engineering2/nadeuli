/* User.java
 * 사용자 정보를 저장하는 엔티티 클래스 (OAuth 및 JWT 기반 인증)
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       Entity 생성 초안
 * 국경민      03-05       Serializable 추가 (Redis 저장 문제 해결)
 * 국경민      03-05       User.of() 메서드 수정 (id 추가)
 * 국경민      03-05       updateProfile() 메서드 추가 (기존 사용자 정보 업데이트)
 * ========================================================
 */
package nadeuli.entity;

import jakarta.persistence.*;
import lombok.*;
import nadeuli.entity.constant.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 객체 생성 제한
@AllArgsConstructor(access = AccessLevel.PRIVATE) // static factory method 사용 유도
@Table(name = "users")
public class User implements Serializable {  // Redis 저장을 위해 Serializable 추가

    @Serial
    private static final long serialVersionUID = 1L;  // 직렬화 버전 ID 추가

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id; // 사용자 고유 ID (Primary Key)

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail; // 이메일 (OAuth에서 제공)

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // OAuth 제공자 (Google, Kakao 등)

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName; // 사용자 이름

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage; // OAuth 프로필 이미지 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole; // 사용자 역할 (예: ROLE_MEMBER)

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // OAuth Refresh Token 저장

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt; // 마지막 로그인 시간

    @Column(name = "refresh_token_expiry_at")
    private LocalDateTime refreshTokenExpiryAt; // Refresh Token 만료일 저장

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // MySQL `CURRENT_TIMESTAMP` 사용 (JPA에서 값 설정 안 함)

    /**
     * ✅ OAuth 로그인 시 기존 사용자 정보 업데이트
     */
    public void updateProfile(String userName, String profileImage, String provider, String refreshToken, LocalDateTime lastLoginAt, LocalDateTime refreshTokenExpiryAt) {
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.lastLoginAt = lastLoginAt;
        if (refreshToken != null && !refreshToken.isEmpty()) {
            this.refreshToken = refreshToken;
            this.refreshTokenExpiryAt = refreshTokenExpiryAt;
        }
    }

    /**
     * ✅ static factory method - User 객체 생성 (새로운 회원 가입 시 사용)
     */
    public static User createNewUser(String userEmail, String userName, String profileImage, String provider, String refreshToken, LocalDateTime lastLoginAt, LocalDateTime refreshTokenExpiryAt) {
        return new User(null, userEmail, provider, userName, profileImage, UserRole.MEMBER, refreshToken, lastLoginAt, refreshTokenExpiryAt, LocalDateTime.now());
    }

    /**
     * ✅ static factory method - 기존 회원 정보 불러오기
     */
    public static User of(Long id, String userEmail, String provider, String userName, String profileImage, UserRole userRole, String refreshToken, LocalDateTime lastLoginAt, LocalDateTime refreshTokenExpiryAt, LocalDateTime createdAt) {
        return new User(id, userEmail, provider, userName, profileImage, userRole, refreshToken, lastLoginAt, refreshTokenExpiryAt, createdAt);
    }

    /**
     * ✅ 생성 시 createdAt 자동 설정 (DB `CURRENT_TIMESTAMP` 동기화)
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

