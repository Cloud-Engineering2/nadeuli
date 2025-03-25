/* JournalSimpleDTO.java
 * nadeuli Service - 여행
 * Journal 관련 DTO - event를 ieid로 받는 심플한버전 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.22    최초 작성
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JournalSimpleDTO {
    private Long id;
    private Long ieid;
    private String content;
    private String imageUrl;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}