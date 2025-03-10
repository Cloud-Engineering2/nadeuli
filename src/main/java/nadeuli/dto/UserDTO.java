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

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String userEmail;
    private String userToken;
    private String userName;
    private String provider;
    private UserRole userRole;


    // static factory method - UserDTO 객체 생성
    public static UserDTO of(String userEmail, String userName, String provider) {
        return UserDTO.of(userEmail, userName, provider);
    }

    // static factory method - UserDTO 객체 생성
    public static UserDTO of(Long id, String userEmail, String userToken, String userName, String provider, UserRole userRole) {
        return new UserDTO(id, userEmail, userToken, userName, provider, userRole);
    }

    // entity -> dto
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUserEmail(),
                user.getUserToken(),
                user.getUserName(),
                user.getProvider(),
                user.getUserRole()
        );
    }

    // dto => entity
    public User toEntity() {
        return User.of(userEmail, userName, provider);
    }

}
