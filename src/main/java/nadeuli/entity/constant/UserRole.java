/* UserRoleAttributeConverter.java
 * nadeuli Service - 여행
 * 열거형 UserRole
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

package nadeuli.entity.constant;

import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

@ToString
@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    MEMBER("ROLE_MEMBER");

    private String userRole;

    UserRole(String userRole) {
        this.userRole = userRole;
    }

    public static UserRole getInstance(String userRole) { // userRole 의 값을 문자열로 찾는 함수

        return Arrays.stream(UserRole.values()) // UserRole 값 -> 배열 -> 스트림
                .filter(type -> type.getUserRole().equals(userRole)) // userRole 과 type(UserRole) 값 일치 여부로 정제
                .findFirst() // 정제된 것에서 첫 번째 요소 반환
                .orElseThrow(() -> {
                    System.out.println("❌ getInstance() - 변환 실패.. 유효 x 값: " + userRole);
                    return new IllegalArgumentException("getInstance() - Invalid UserRole : " + userRole);
                }); // 값 x => 예외
    }



}