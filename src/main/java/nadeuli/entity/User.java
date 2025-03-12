package nadeuli.entity;

import jakarta.persistence.*;
import lombok.*;
import nadeuli.entity.constant.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole;

    @Column(name = "user_token", columnDefinition = "TEXT")
    private String userToken;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateProfile(String userName, String profileImage, String provider, String userToken, LocalDateTime lastLoginAt) {
        this.userName = userName;
        this.profileImage = profileImage;
        this.provider = provider;

        if (userToken != null && !userToken.isEmpty()) {
            this.userToken = userToken;
        }

        this.lastLoginAt = lastLoginAt;
    }

    public static User of(Long id, String userEmail, String provider, String userName,
                          String profileImage, UserRole userRole, String userToken,
                          LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        return new User(id, userEmail, provider, userName, profileImage, userRole, userToken, lastLoginAt, createdAt);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public static User createNewUser(String userEmail, String userName, String profileImage, String provider,
                                     String userToken, LocalDateTime lastLoginAt) {
        return new User(
                null,  // ID는 자동 생성 (DB에서 자동 증가)
                userEmail,
                provider,
                userName,
                profileImage,
                UserRole.MEMBER, // 기본 역할 (MEMBER)
                userToken, // 초기 userToken (null 가능)
                lastLoginAt,
                LocalDateTime.now() // 생성 시간은 현재 시간
        );
    }

}
