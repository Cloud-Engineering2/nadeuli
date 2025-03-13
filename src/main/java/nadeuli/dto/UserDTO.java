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
import java.time.format.DateTimeParseException;

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
    private String refreshTokenExpiryAt;
    private String createdAt;

    /**
     * ✅ Entity → DTO 변환 메서드
     * - User 엔티티를 UserDTO로 변환
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
                formatDateTime(user.getLastLoginAt()),
                formatDateTime(user.getRefreshTokenExpiryAt()), // ✅ Refresh Token 만료일 추가
                formatDateTime(user.getCreatedAt())
        );
    }

    /**
     * ✅ DTO → Entity 변환 메서드
     * - User.builder()를 사용하여 엔티티 생성
     * - ID가 null이면 빌더에서 제외 (새로운 사용자 생성 시)
     */
    public User toEntity() {
        User.UserBuilder userBuilder = User.builder()
                .userEmail(this.userEmail)
                .provider(this.provider)
                .userName(this.userName)
                .profileImage(this.profileImage)
                .userRole((this.userRole != null) ? this.userRole : UserRole.MEMBER) // ✅ 기본값 설정
                .refreshToken(this.refreshToken)
                .lastLoginAt(parseDateTime(this.lastLoginAt))
                .refreshTokenExpiryAt(parseDateTime(this.refreshTokenExpiryAt)) // ✅ Refresh Token 만료일 추가
                .createdAt(parseDateTime(this.createdAt));

        // ✅ ID가 null이 아닐 경우에만 설정 (새로운 사용자 생성 시 제외)
        if (this.id != null) {
            userBuilder.id(this.id);
        }

        return userBuilder.build();
    }

    /**
     * ✅ LocalDateTime → String 변환
     * - null이면 null 반환
     * - yyyy-MM-dd HH:mm:ss 형식 유지
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    /**
     * ✅ String → LocalDateTime 변환
     * - null 또는 빈 문자열이면 null 반환
     * - yyyy-MM-dd HH:mm:ss 형식 유지
     */
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            return null; // ✅ 잘못된 형식이면 null 반환
        }
    }
}
