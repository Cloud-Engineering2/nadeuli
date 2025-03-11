/* ItineraryEventUpdateDTO.java
 * ItineraryTotalUpdateRequestDTO에 사용되는 서브 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초 작성
 * 박한철    2025.02.28     PerDayID 대신 dayCount를 받도록 , placeDTO 대신 pid를 받도록 수정
 * 박한철    2025.03.11     movingDistanceFromPrevPlace 추가
 * ========================================================
 */
package nadeuli.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryEventUpdateDTO {
    private Long id;
    private int dayCount;
    private Long pid;
    private int startMinuteSinceStartDay;
    private int endMinuteSinceStartDay;
    private int movingMinuteFromPrevPlace;
    private int movingDistanceFromPrevPlace;

}