/* ExpenseItemService.java
 * ExpenseItem 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
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
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.entity.ExpenseBook;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.Traveler;
import nadeuli.repository.ExpenseBookRepository;
import nadeuli.repository.ExpenseItemRepository;
import nadeuli.repository.ItineraryEventRepository;
import nadeuli.repository.TravelerRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseItemService {
    private final ExpenseItemRepository expenseItemRepository;
    private final ExpenseBookRepository expenseBookRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final TravelerRepository travelerRepository;

    // 지출 내역 추가
    public void addExpense(ExpenseItemDTO expenseItemDto) {
        Long ebid = expenseItemDto.getExpenseBookId();
        TravelerDTO payerDto = expenseItemDto.getTravelerDTO();
        Long ieid = expenseItemDto.getItineraryEventId();

        ExpenseBook expenseBook = expenseBookRepository.findById(ebid)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));
        ItineraryEvent itineraryEvent = itineraryEventRepository.findById(ieid)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryEvent가 존재하지 않습니다"));
        String payerName = payerDto.getTravelerName();
        Traveler payer = travelerRepository.findByTravelerName(payerName)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다"));

        ExpenseItem expensItem = expenseItemDto.toEntity(expenseBook, itineraryEvent, payer);
        expenseItemRepository.save(expensItem);
    }

}
