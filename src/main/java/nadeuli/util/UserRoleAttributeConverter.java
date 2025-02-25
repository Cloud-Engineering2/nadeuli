/* UserRoleAttributeConverter.java
 * nadeuli Service - 여행
 * 열거형 UserRole <=> DB 문자열 값
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

package nadeuli.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import nadeuli.entity.constant.UserRole;

@Convert
public class UserRoleAttributeConverter implements AttributeConverter<UserRole, String> {

    // 열거형 UserRole => DB 문자열
    @Override
    public String convertToDatabaseColumn(UserRole attribute) {

        System.out.println("🔄 convertToDatabaseColumn() - 열거형 -> 문자열 : " + attribute);

        return (attribute == null) ? null : attribute.getUserRole(); // null 고려
    }

    // DB 문자열 => 열거형 UserRole
    @Override
    public UserRole convertToEntityAttribute(String dbData) {

        System.out.println("🔄 convertToEntityAttribute() - 문자열 -> 열거형 : " + dbData +  " - " + UserRole.getInstance(dbData));

        return ((dbData == null) || dbData.isEmpty()) ? null : UserRole.getInstance(dbData); // null 고려
    }
}