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
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryCollaboratorService itineraryCollaboratorService;


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


    // 총 예산, 지출, 잔액 조회
    public FinanceResponseDTO calculateTotalMoney(Long itineraryId) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        // ExpenseBook 조회
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));
        // ItineraryPerDay 조회
        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);
        // ItineraryEvent 조회
        List<ItineraryEvent> itineraryEvents = itineraryPerDays.stream()
                .flatMap(itineraryPerDay -> itineraryEventRepository.findByItineraryPerDay(itineraryPerDay).stream())
                .collect(Collectors.toList());

        // owner username
        String owner = itineraryCollaboratorService.getOwner(itinerary);


        // 총 지출
        AtomicReference<Long> expenditure = new AtomicReference<>(0L);


        for (ItineraryEvent itineraryEvent : itineraryEvents) {
            // 정산 완료 후
            Map<String, Person> adjustment = adjustmentExpense(itineraryEvent.getId());

            // UserName과 똑같은 TravelerName을 가진 Traveler 찾기

            Optional<Person> personOptional = Optional.ofNullable(adjustment.get(owner));

            personOptional.ifPresentOrElse(
                    person -> { // 존재할 때
                        Long total = person.getTotal();
                        Long totalSendedMoney = person.getSendedMoney() != null
                                ? person.getSendedMoney().values().stream().mapToLong(Long::longValue).sum()
                                : 0L;

                        Long totalReceivedMoney = person.getReceivedMoney() != null
                                ? person.getReceivedMoney().values().stream().mapToLong(Long::longValue).sum()
                                : 0L;

                        expenditure.updateAndGet(v -> v + (total + totalSendedMoney - totalReceivedMoney));
                    },
                    () -> { // 존재하지 않을 때
                    }
            );

        }
        Long totalExpense = expenditure.get();
        expenseBook.updateExpenses(totalExpense);

        // 총 예산
        Long budget = expenseBook.getTotalBudget();

        // 총 잔액
        Long balance = budget - totalExpense;

        return new FinanceResponseDTO(null, budget, 0L, totalExpense, balance);

    }


    // ItineraryEvent 별 정산
    public FinanceResponseDTO calculateMoney(Long itineraryId, Long itineraryEventId) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        // ExpenseBook 조회
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));
        // ItineraryPerDay 조회
        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);
        // ItineraryEvent 조회
        List<ItineraryEvent> itineraryEvents = itineraryPerDays.stream()
                .flatMap(itineraryPerDay -> itineraryEventRepository.findByItineraryPerDay(itineraryPerDay).stream())
                .collect(Collectors.toList());

        // owner username
        String owner = itineraryCollaboratorService.getOwner(itinerary);

        // 변수 선언
        AtomicReference<Long> expenditure = new AtomicReference<>(0L);
        AtomicReference<Long> currentExpenditure = new AtomicReference<>(0L);

        Map<String, Person> response = new HashMap<>();

        for (ItineraryEvent event : itineraryEvents) {
            if (event.getId() <= itineraryEventId) {
                // 1/n 정산
                Map<String, Person> adjustment = adjustmentExpense(event.getId());

                // UserName과 똑같은 정산자 찾기
                Optional<Person> personOptional = Optional.ofNullable(adjustment.get(owner));

                personOptional.ifPresentOrElse(
                        person -> { // 존재할 때
                            Long total = person.getTotal();
                            Long totalSendedMoney = person.getSendedMoney() != null
                                    ? person.getSendedMoney().values().stream().mapToLong(Long::longValue).sum()
                                    : 0L;

                            Long totalReceivedMoney = person.getReceivedMoney() != null
                                    ? person.getReceivedMoney().values().stream().mapToLong(Long::longValue).sum()
                                    : 0L;

                            expenditure.updateAndGet(v -> v + (total + totalSendedMoney - totalReceivedMoney));
                            currentExpenditure.updateAndGet(v -> total + totalSendedMoney - totalReceivedMoney);
                        },
                        () -> { // 존재하지 않을 때
                        }
                );
                response = adjustment;

            }
        }
        // 총 지출
        Long totalExpense = expenditure.get();
        expenseBook.updateExpenses(totalExpense);

        // 현재 Itinerary Event 지출
        Long currentExpense = currentExpenditure.get();

        // 총 예산
        Long budget = expenseBook.getTotalBudget();

        // 총 잔액
        Long balance = budget - totalExpense;

        return new FinanceResponseDTO(response, budget, currentExpense, totalExpense, balance);

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