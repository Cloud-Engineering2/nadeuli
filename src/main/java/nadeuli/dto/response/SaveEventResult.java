/* SaveEventResult.java
 * 서비스단에서 사용되는 result object
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.03.12
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.12     최초작성
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryEventDTO;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SaveEventResult {
    private List<ItineraryEventDTO> events;
    private List<CreatedEventMapping> createdMappings;
}