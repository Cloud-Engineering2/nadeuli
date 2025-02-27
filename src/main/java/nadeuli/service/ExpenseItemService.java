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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.ExpenseItemUpdateRequest;
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
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseItemService {
    private final ExpenseItemRepository expenseItemRepository;
    private final ExpenseBookRepository expenseBookRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final TravelerRepository travelerRepository;

    // 지출 내역 추가
    @Transactional
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


    // 지출 내역 조회
    @Transactional
    public List<ExpenseItemDTO> getAll(Long itineraryEventId) {
        List<ExpenseItem> expenseItems = expenseItemRepository.findAllByIeid(itineraryEventId);
        List<ExpenseItemDTO> expenseItemDtos = expenseItems.stream()
                                                    .map(ExpenseItemDTO::from)
                                                    .collect(Collectors.toList());
        return expenseItemDtos;
    }

    // 지출 내역 수정
    @Transactional
    public ExpenseItemDTO updateExpenseItem(Long expenseItemId, ExpenseItemUpdateRequest expenseItemUpdateRequest) {
        ExpenseItem expenseItem = expenseItemRepository.findById(expenseItemId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ExpenseItem이 존재하지 않습니다"));

        if (expenseItemUpdateRequest.getExpense() != null) {
            Long expense = Long.valueOf(expenseItemUpdateRequest.getExpense());
            expenseItem.updateExpense(expense);
        }
        if (expenseItemUpdateRequest.getContent() != null) {
            String content = expenseItemUpdateRequest.getContent();
            expenseItem.updateContent(content);
        }
        if (expenseItemUpdateRequest.getPayer() != null) {
            String payerName = expenseItemUpdateRequest.getPayer();
            Traveler payer = travelerRepository.findByTravelerName(payerName)
                    .orElseThrow(() -> new EntityNotFoundException("해당 Traveler가 존재하지 않습니다"));
            expenseItem.updatePayer(payer);
        }

        return ExpenseItemDTO.from(expenseItem);

    }

    // 지출 내역 삭제
    @Transactional
    public void deleteExpenseItem(Long expenseItemId) {
        expenseItemRepository.deleteById(expenseItemId);

    }
}
