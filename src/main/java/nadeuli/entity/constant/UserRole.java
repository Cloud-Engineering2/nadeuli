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

    private final String userRole; // ✅ final 키워드 추가하여 불변성 유지

    UserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * ✅ 문자열을 UserRole로 변환하는 메서드
     */
    public static UserRole getInstance(String userRole) {
        return Arrays.stream(UserRole.values()) // UserRole 값 -> 배열 -> 스트림 변환
                .filter(type -> type.getUserRole().equals(userRole)) // 문자열과 일치하는 enum 찾기
                .findFirst() // 첫 번째 값 반환
                .orElseThrow(() -> {
                    System.out.println("❌ getInstance() - 변환 실패.. 유효하지 않은 값: " + userRole);
                    return new IllegalArgumentException("getInstance() - Invalid UserRole : " + userRole);
                });
    }
}
