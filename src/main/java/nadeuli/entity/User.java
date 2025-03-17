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
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_account", columnNames = {"user_email", "provider"})
        }
)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
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

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "refresh_token_expiry_at", nullable = false)
    private LocalDateTime refreshTokenExpiryAt;

    public void updateRefreshToken(String refreshToken, LocalDateTime expiryAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiryAt = expiryAt;
    }

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
                          LocalDateTime lastLoginAt, LocalDateTime createdAt, String refreshToken, LocalDateTime refreshTokenExpiryAt) {
        return new User(id, userEmail, provider, userName, profileImage, userRole, userToken, lastLoginAt, createdAt, refreshToken, refreshTokenExpiryAt);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.refreshTokenExpiryAt == null) {
            this.refreshTokenExpiryAt = LocalDateTime.now().plusDays(7);
        }
    }

    public static User createNewUser(String userEmail, String userName, String profileImage, String provider,
                                     String userToken, LocalDateTime lastLoginAt, String refreshToken, LocalDateTime refreshTokenExpiryAt) {
        return new User(
                null,
                userEmail,
                provider,
                userName,
                profileImage,
                UserRole.MEMBER,
                userToken,
                lastLoginAt,
                LocalDateTime.now(),
                refreshToken,
                refreshTokenExpiryAt
        );
    }
}

