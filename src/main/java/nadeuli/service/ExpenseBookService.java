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
 * 고민정    2025.02.26   예산 산정 메서드 추가
 * 이홍비    2025.03.10   ExpenseBookDTO 반환 함수 구현
 * ========================================================
 */

package nadeuli.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.Person;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseBookService {
    private final ExpenseBookRepository expenseBookRepository;
    private final ItineraryRepository itineraryRepository;
    private final WithWhomRepository withWhomRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final ExpenseItemRepository expenseItemRepository;

    // 예산 설정
    public ExpenseBookDTO setBudget(Long iid, Long budget) {
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다. ID: " + iid));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));

        expenseBook.updateBudget(budget);
        return ExpenseBookDTO.from(expenseBook);
    }

    // ExpenseBook 조회 by Itinerary
    public Long get(Long iid) {
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary 존재하지 않습니다"));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));
        return expenseBook.getId();
    }


    // 1/n 정산
    public Map<String, Person> adjustmentExpense(Long itineraryEventId) {

        // Itinerary Event 가져오기
        ItineraryEvent itineraryEvent = itineraryEventRepository.findById(itineraryEventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary Event가 존재하지 않습니다"));

        // Expense Item 가져오기
        List<ExpenseItem> expenseItems = expenseItemRepository.findAllByIeid(itineraryEvent);

        Map<String, Person> persons = new HashMap<>();

        for (ExpenseItem expenseItem : expenseItems) {
            // List<WithWhomDTO> withWhomDtos = withWhomRepository.findAllByEmid(expenseItem).stream().map(WithWhomDTO::from).collect(Collectors.toList());
            List<WithWhom> withWhoms = withWhomRepository.findAllByEmid(expenseItem);

            List<String> withWhomNames = withWhoms.stream()
                    .map(whom -> whom.getTid().getTravelerName()).collect(Collectors.toList());

            Long amount = expenseItem.getExpense();
            String payerName = expenseItem.getPayer().getTravelerName();
            Person payer = persons.getOrDefault(payerName, new Person());
            payer.setTotal(payer.getTotal() + amount);
            System.out.println(payerName);
            System.out.println(payer.getTotal());
            persons.put(payerName, payer);

            Long splitAmount = amount / (withWhoms.size()+1);

            for (String with : withWhomNames) {
                Person withWhomPerson = persons.getOrDefault(with, new Person());
                withWhomPerson.send(payerName, splitAmount);
                persons.put(with, withWhomPerson);
                payer.receive(with, splitAmount);
                persons.put(payerName, payer);
            }
        }
        return persons;
    }

    // ExpenseBookDTO 반환
    public ExpenseBookDTO getExpenseBook(Long iid) {
        // 일정 조회
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 존재하지 않습니다."));

        // 장부 조회
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 장부가 존재하지 않습니다."));

        return ExpenseBookDTO.from(expenseBook); // DTO 로 반환
    }

}