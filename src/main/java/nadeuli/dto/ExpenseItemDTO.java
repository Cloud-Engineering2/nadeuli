/* ExpenseItemDTO.java
 * nadeuli Service - 여행
 * ExpenseItem 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 고민정    2025;02.25     필드 수정
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ExpenseBook;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.Traveler;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseItemDTO {
    private Long id;
    private Long expenseBookId;
    private Long itineraryEventId;
    private TravelerDTO travelerDTO;
    private String content;
    private Long expense;
//    private LocalDateTime createdAt;
//    private LocalDateTime modifiedAt;


    // static factory method
    public static ExpenseItemDTO of (Long id, Long expenseBookId, Long itineraryEventId, TravelerDTO travelerDTO, String content, Long expense) {
        return new ExpenseItemDTO(id, expenseBookId, itineraryEventId, travelerDTO, content, expense);
    }

    public static ExpenseItemDTO of (Long expenseBookId, Long itineraryEventId, TravelerDTO travelerDTO, String content, Long expense) {
        return new ExpenseItemDTO(null, expenseBookId, itineraryEventId, travelerDTO, content, expense);
    }

    // entity -> dto
    public static ExpenseItemDTO from(ExpenseItem expenseItem) {
        return new ExpenseItemDTO(
                expenseItem.getId(),
                expenseItem.getEbid().getId(),
                expenseItem.getIeid().getId(),
                TravelerDTO.from(expenseItem.getPayer()),
                expenseItem.getContent(),
                expenseItem.getExpense()
//                expenseItem.getCreatedDate(),
//                expenseItem.getModifiedDate()
        );
    }

    // dto => entity
    public ExpenseItem toEntity(ExpenseBook expenseBook, ItineraryEvent itineraryEvent, Traveler traveler) {
        return ExpenseItem.of(expenseBook, itineraryEvent, traveler, content, expense);
    }
}
