/* UserDTO.java
 * 사용자 정보를 전달하는 DTO (OAuth 사용자 데이터 관리)
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       DTO 생성 초안
 * 국경민      03-05       toEntity() 메서드 수정 (User.of() 메서드와 일치)
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
    private String refreshToken;
    private String lastLoginAt;
    private String createdAt;

    /**
     * ✅ Entity → DTO 변환 메서드
     */
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getProfileImage(),
                user.getProvider(),
                user.getUserRole(),
                user.getRefreshToken(),
                formatDateTime(user.getLastLoginAt()),  // ✅ 날짜 변환 추가
                formatDateTime(user.getCreatedAt())    // ✅ 날짜 변환 추가
        );
    }

    /**
     * ✅ DTO → Entity 변환 메서드
     */
    public User toEntity() {
        return User.of(
                this.id,
                this.userEmail,
                this.provider,
                this.userName,
                this.profileImage,
                this.userRole,
                this.refreshToken,
                parseDateTime(this.lastLoginAt),  // ✅ 문자열 → LocalDateTime 변환 추가
                parseDateTime(this.createdAt),   // ✅ 문자열 → LocalDateTime 변환 추가
                parseDateTime(this.createdAt)    // ✅ User 엔티티에서 사용하도록 변경됨
        );
    }

    /**
     * ✅ LocalDateTime → String 변환
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    /**
     * ✅ String → LocalDateTime 변환
     */
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        return (dateTimeStr != null && !dateTimeStr.isEmpty()) ? LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
}
