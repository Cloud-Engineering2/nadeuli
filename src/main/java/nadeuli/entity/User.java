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
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.constant.UserRole;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id; // 사용자 고유 ID

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail; // 이메일 (OAuth에서 제공)

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // OAuth 제공자 (Google, Kakao 등)

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName; // 사용자 이름

    @Column(name = "profile_image", columnDefinition = "TEXT", nullable = true)
    private String profileImage; // OAuth 프로필 이미지 URL


    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole; // 사용자 역할 (예: ROLE_MEMBER)

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken; // ✅ 기존의 userToken 필드를 accessToken으로 변경

    @Column(name = "refresh_token", columnDefinition = "TEXT DEFAULT ''", nullable = true)
    private String refreshToken; // JWT 리프레시 토큰

    /**
     * ✅ OAuth 로그인 시 사용하는 생성자 (userToken, refreshToken 없음)
     */
    public User(String userEmail, String userName, String profileImage, String provider) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.accessToken = "";
        this.userRole = UserRole.MEMBER;
        this.refreshToken = "";
    }

    /**
     * ✅ OAuth 로그인 후 JWT 저장을 위한 생성자 (userToken, refreshToken 포함)
     */
    public User(Long id, String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        this.id = id;
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.accessToken = "";
        this.userRole = UserRole.MEMBER;
        this.refreshToken = refreshToken;
    }

    /**
     * ✅ static factory method - User 객체 생성 (OAuth 로그인 후 저장)
     */
    public static User of(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        return new User(null, userEmail, userName, profileImage, provider, refreshToken);
    }
}


