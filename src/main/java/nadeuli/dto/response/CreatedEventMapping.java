/* CreatedEventMapping.java
 * 새로 생성된 Event의 DOM hashid - eventid를 맵핑할때 쓰는 object
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

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreatedEventMapping {
    private String hashId;   // 프론트에서 전달된 임시 식별자
    private Long eventId;    // 서버 DB에서 생성된 실제 ID

}
