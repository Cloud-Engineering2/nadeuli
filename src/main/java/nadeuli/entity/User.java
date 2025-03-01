/* User.java
 * User 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     생성자 + static factory method 추가 // 컨버터 추가
 * 국경민    2025.02.25     KakaoUser 속성 통합 + 생성자 추가
 * 국경민    2025.03.01     @Enumerated(EnumType.STRING)사용
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
    private Long id;

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail;

    @Lob
    @Column(name = "user_token", columnDefinition = "TEXT DEFAULT ''", nullable = true)
    private String userToken;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = true)
    private String profileImage;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Enumerated(EnumType.STRING) // 🔥 가독성 향상을 위해 추가
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole;

    @Column(name = "refresh_token", columnDefinition = "TEXT DEFAULT ''", nullable = true) // 🔥 필수 아님
    private String refreshToken;

    // 기본 생성자
    public User(String userEmail, String userName, String profileImage, String provider) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.userToken = "";
        this.userRole = UserRole.MEMBER;
        this.refreshToken = "";
    }

    // 새로운 생성자 추가
    public User(Long id, String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        this.id = id;
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.userToken = "";
        this.userRole = UserRole.MEMBER;
        this.refreshToken = refreshToken;
    }

    // static factory method - User 객체 생성
    public static User of(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        return new User(null, userEmail, userName, profileImage, provider, refreshToken);
    }
}

