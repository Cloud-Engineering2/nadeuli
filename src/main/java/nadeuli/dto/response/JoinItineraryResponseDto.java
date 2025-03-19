/*
 * JoinItineraryResponseDto.java
 * 사용자가 공유 일정에 가입할 때, 복호화된 itineraryId와 가입 메시지를 함께 반환하는 응답 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성
 * ========================================================
 */
package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JoinItineraryResponseDto {
    private String message;
    private Long itineraryId;
}
