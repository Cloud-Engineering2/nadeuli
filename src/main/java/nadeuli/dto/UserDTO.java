/* UserDTO.java
 * nadeuli Service - 여행
 * User 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * ========================================================
 */

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
    private String userToken;
    private String lastLoginAt;
    private String createdAt;
    private String refreshToken;
    private String refreshTokenExpiryAt;

    // 새로운 생성자 추가 (프로필용)
    public UserDTO(String userName, String userEmail, String profileImage) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.profileImage = profileImage;
    }

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getProfileImage(),
                user.getProvider(),
                user.getUserRole(),
                user.getUserToken(),
                formatDateTime(user.getLastLoginAt()),
                formatDateTime(user.getCreatedAt()),
                user.getRefreshToken(),
                formatDateTime(user.getRefreshTokenExpiryAt())
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
                this.userToken,
                parseDateTime(this.lastLoginAt),
                parseDateTime(this.createdAt),
                this.refreshToken,
                parseDateTime(this.refreshTokenExpiryAt)
        );
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        return (dateTimeStr != null && !dateTimeStr.isEmpty()) ?
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
}