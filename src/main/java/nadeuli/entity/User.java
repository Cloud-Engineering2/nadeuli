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
@Setter // ✅ DTO 변환을 위해 Setter 추가
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // ✅ 객체 생성 제한
@AllArgsConstructor(access = AccessLevel.PRIVATE) // ✅ static factory method 사용 유도
@Builder // ✅ DTO 변환을 위해 빌더 추가
@Table(name = "users") // ✅ 테이블 이름 매핑
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    @Column(name = "user_email", nullable = false, unique = true, length = 255)
    private String userEmail;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    @Builder.Default // ✅ 기본값 설정
    private UserRole userRole = UserRole.MEMBER;

    @Column(name = "refresh_token", columnDefinition = "TEXT", nullable = true)
    @Builder.Default // ✅ 기본값 설정 (null 방지)
    private String refreshToken = "";

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(name = "refresh_token_expiry_at", nullable = true)
    private LocalDateTime refreshTokenExpiryAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

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
     * ✅ static factory method - 새로운 회원 가입 시 사용
     */
    public static User createNewUser(String userEmail, String userName, String profileImage, String provider, String refreshToken, LocalDateTime lastLoginAt, LocalDateTime refreshTokenExpiryAt) {
        return User.builder()
                .userEmail(userEmail)
                .provider(provider)
                .userName(userName)
                .profileImage(profileImage)
                .userRole(UserRole.MEMBER) // ✅ 기본값 설정
                .refreshToken(refreshToken != null ? refreshToken : "") // ✅ Null 방지
                .lastLoginAt(lastLoginAt != null ? lastLoginAt : LocalDateTime.now()) // ✅ Null 방지
                .refreshTokenExpiryAt(refreshTokenExpiryAt)
                .createdAt(LocalDateTime.now()) // ✅ 기본값 설정
                .build();
    }

    /**
     * ✅ 생성 시 createdAt 및 기본값 설정 (DB `CURRENT_TIMESTAMP` 동기화)
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastLoginAt == null) {
            this.lastLoginAt = LocalDateTime.now();
        }
        if (this.userRole == null) {
            this.userRole = UserRole.MEMBER; // ✅ 기본값 설정
        }
        if (this.refreshToken == null) {
            this.refreshToken = ""; // ✅ 빈 값이라도 저장
        }
    }

    /**
     * ✅ 로그인 시 자동으로 lastLoginAt 업데이트
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
