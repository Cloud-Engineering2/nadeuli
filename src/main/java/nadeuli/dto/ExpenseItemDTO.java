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
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.Traveler;

import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseItemDTO {
    private Long id;
    private ExpenseBookDTO expenseBookDTO;
    private ItineraryEventDTO itineraryEventDTO;
    private TravelerDTO travelerDTO;
    private String content;
    private Integer expense;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;


    // static factory method
    public static ExpenseItemDTO of (Long id, ExpenseBookDTO expenseBookDTO, ItineraryEventDTO itineraryEventDTO, TravelerDTO travelerDTO, String content, Integer expense, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new ExpenseItemDTO(id, expenseBookDTO, itineraryEventDTO, travelerDTO, content, expense, createdAt, modifiedAt);
    }

    public static ExpenseItemDTO of (ExpenseBookDTO expenseBookDTO, ItineraryEventDTO itineraryEventDTO, TravelerDTO travelerDTO, String content, Integer expense, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new ExpenseItemDTO(null, expenseBookDTO, itineraryEventDTO, travelerDTO, content, expense, createdAt, modifiedAt);
    }

    // entity -> dto
    public static ExpenseItemDTO from(ExpenseItem expenseItem) {
        return new ExpenseItemDTO(
                expenseItem.getId(),
                ExpenseBookDTO.from(expenseItem.getEbid()),
                ItineraryEventDTO.from(expenseItem.getIeid()),
                TravelerDTO.from(expenseItem.getPayer()),
                expenseItem.getContent(),
                expenseItem.getExpense(),
                expenseItem.getCreatedDate(),
                expenseItem.getModifiedDate()
        );
    }

    // dto => entity
    public ExpenseItem toEntity(Traveler traveler) {
        return ExpenseItem.of(expenseBookDTO.toEntity(), itineraryEventDTO.toEntity(), traveler, content, expense);
    }
}
