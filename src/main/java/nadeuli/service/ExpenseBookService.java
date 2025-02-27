/* ExpenseBookService.java
 * ExpenseBook 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   service 생성 및 예산 산정 메서드 추가
 *
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.entity.ExpenseBook;
import nadeuli.entity.Itinerary;
import nadeuli.repository.ExpenseBookRepository;
import nadeuli.repository.ItineraryRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseBookService {
    private final ExpenseBookRepository expenseBookRepository;
    private final ItineraryRepository itineraryRepository;

    // 예산 설정
    @Transactional
    public ExpenseBookDTO setBudget(Long iid, Long budget) {
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다. ID: " + iid));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));

        expenseBook.updateBudget(budget);
        return ExpenseBookDTO.from(expenseBook);
    }
}