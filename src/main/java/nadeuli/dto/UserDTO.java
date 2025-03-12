package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.User;
import nadeuli.entity.constant.UserRole;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String userEmail;
    private String userName;
    private String profileImage;
    private String provider;
    private UserRole userRole;
    private String lastLoginAt;
    private String createdAt;

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getProfileImage(),
                user.getProvider(),
                user.getUserRole(),
                formatDateTime(user.getLastLoginAt()),
                formatDateTime(user.getCreatedAt())
        );
    }

    public User toEntity() {
        return User.of(
                this.id,
                this.userEmail,
                this.provider,
                this.userName,
                this.profileImage,
                this.userRole,
                null,  // 🚀 userToken 추가
                parseDateTime(this.lastLoginAt),
                parseDateTime(this.createdAt)
        );
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        return (dateTimeStr != null && !dateTimeStr.isEmpty()) ? LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
}
