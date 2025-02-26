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
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.constant.UserRole;
import nadeuli.util.UserRoleAttributeConverter;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid", nullable = false)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Lob
    @Column(name = "user_token", nullable = false)
    private String userToken;

    @Column(name = "user_name", nullable = false, length = 20)
    private String userName;

    @Column(name = "profile_image")
    private String profileImage; // KakaoUser의 profileImage 속성 추가

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "email", nullable = false)
    private String email; // email 필드 추가

    @Column(name = "user_role", nullable = false, length = 20)
    @Convert(converter = UserRoleAttributeConverter.class)
    private UserRole userRole;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;  // refreshToken 필드 추가

    // 기본 생성자
    public User(String userEmail, String userName, String profileImage, String provider) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.userToken = ""; // 기본 값 설정
        this.userRole = UserRole.MEMBER; // 기본 값 설정
        this.email = userEmail; // email 필드 초기화
        this.refreshToken = ""; // 기본 값 설정
    }

    // 새로운 생성자 추가
    public User(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;
        this.userToken = "";
        this.userRole = UserRole.MEMBER; // 기본 값 설정
        this.email = userEmail; // email 필드 초기화
        this.refreshToken = refreshToken; // 올바르게 초기화
    }

    // static factory method - User 객체 생성
    public static User of(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        return new User(userEmail, userName, profileImage, provider, refreshToken);
    }
}

