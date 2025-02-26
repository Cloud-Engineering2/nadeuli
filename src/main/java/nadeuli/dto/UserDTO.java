/* UserDTO.java
 * nadeuli Service - 여행
 * User 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25    최초 작성
 * 국경민    2025.02.25    생성자 및 profileImage 필드 추가
 * 국경민    2025.02.26    refreshToken 필드를 추가
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

    private Long id;
    private String userEmail;
    private String userToken;
    private String userName;
    private String profileImage; // profileImage 추가
    private String provider;
    private UserRole userRole;
    private String refreshToken;  // refreshToken 필드 추가

    // static factory method - UserDTO 객체 생성
    public static UserDTO of(String userEmail, String userName, String profileImage, String provider, String refreshToken) {
        return new UserDTO(null, userEmail, "", userName, profileImage, provider, UserRole.MEMBER, refreshToken);
    }

    // static factory method - UserDTO 객체 생성
    public static UserDTO of(Long id, String userEmail, String userToken, String userName, String profileImage, String provider, UserRole userRole, String refreshToken) {
        return new UserDTO(id, userEmail, userToken, userName, profileImage, provider, userRole, refreshToken);
    }

    // entity -> dto
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getUserToken(),
                user.getUserName(),
                user.getProfileImage(), // 프로필 이미지 추가
                user.getProvider(),
                user.getUserRole(),
                user.getRefreshToken()  // refreshToken 추가
        );
    }

    // dto -> entity
    public User toEntity() {
        return User.of(userEmail, userName, profileImage, provider, refreshToken);
    }
}

