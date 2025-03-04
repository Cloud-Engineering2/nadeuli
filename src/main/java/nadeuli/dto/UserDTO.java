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
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.User;
import nadeuli.entity.constant.UserRole;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;               // 사용자 고유 ID
    private String userEmail;       // 이메일 (OAuth에서 제공)
    private String accessToken;     // ✅ JWT 액세스 토큰 (수정됨)
    private String userName;        // 사용자 이름
    private String profileImage;    // OAuth 프로필 이미지 URL
    private String provider;        // OAuth 제공자 (Google, Kakao 등)
    private UserRole userRole;      // 사용자 역할 (예: ROLE_MEMBER)
    private String refreshToken;    // JWT 리프레시 토큰

    /**
     * ✅ static factory method - UserDTO 객체 생성 (OAuth 로그인 시 사용)
     */
    public static UserDTO of(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        return new UserDTO(null, userEmail, "", userName, profileImage, provider, UserRole.MEMBER, refreshToken);
    }

    /**
     * ✅ static factory method - UserDTO 객체 생성 (DB에서 조회 시 사용)
     */
    public static UserDTO of(Long id, String userEmail, String accessToken, String userName, String profileImage, String provider, UserRole userRole, String refreshToken) {
        return new UserDTO(id, userEmail, accessToken, userName, profileImage, provider, userRole, refreshToken);
    }

    /**
     * ✅ Entity → DTO 변환 메서드
     */
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getAccessToken(),  // ✅ 기존 userToken → accessToken으로 변경
                user.getUserName(),
                user.getProfileImage(),
                user.getProvider(),
                user.getUserRole(),
                user.getRefreshToken()
        );
    }

    /**
     * ✅ DTO → Entity 변환 메서드
     */
    public User toEntity() {
        return User.of(
                userEmail,
                userName,
                profileImage,
                provider != null ? provider : "unknown",  // ✅ provider가 null이면 "unknown" 설정
                refreshToken
        );
    }
}


