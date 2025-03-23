/* TravelBottomLineRestController.java
 * nadeuli Service - 여행
 * 일정 최종 결산 관련 처리 Rest Controller
 * 여행 끝 => 총 정리 같은 느낌
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.03.07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : TravelBottomLineController
 * 이홍비    2025.03.10     최종 결과물 페이지 조회 시 필요한 것 처리
 * 이홍비    2025.03.16     showBottomlinePage : 반환 시 itinerary 앞 / 제거
 * 이홍비    2025.03.20     Controller 와 RestController 분리 : exception handler
 *                         로그인 인증 관련 처리
 * ========================================================
 */


package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.JournalDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.response.AdjustmentResponseDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.dto.response.ItineraryEventSimpleDTO;
import nadeuli.dto.response.ItineraryTotalReadResponseDTO;
import nadeuli.auth.oauth.CustomUserDetails;
import nadeuli.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/itineraries/{iid}/bottomline")
public class TravelBottomLineRestController {
    private final ItineraryService itineraryService;
    private final ExpenseBookService expenseBookService;
    private final ExpenseItemService expenseItemService;
    private final JournalService journalService;
    private final TravelerService travelerService;
    private final ItineraryCollaboratorService itineraryCollaboratorService;


    @GetMapping()
    public ResponseEntity<Map<String, Object>> getItineraryData(@PathVariable Long iid, @AuthenticationPrincipal CustomUserDetails userDetails) { //, @RequestParam Long userId) {
        // 방문지 선택 안 했을 때 - 처음 접속했을 때 실행하는 함수

        Long uid = userDetails.getUser().getId();
        itineraryCollaboratorService.checkViewPermission(uid, iid); // 로그인 인증

        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, uid); // if exception 발생 => error 창으로 가도록 처리
        List<TravelerDTO> travelerDTOList = travelerService.listTravelers(iid);
        AdjustmentResponseDTO finalSettlement = expenseBookService.getFinalSettlement(iid);
//        ExpenseBookDTO expenseBookDTO = expenseBookService.getExpenseBook(iid);

        response.put("itineraryTotal", itineraryTotalDTO);
        response.put("travelerList", travelerDTOList);
        response.put("finalSettlement", finalSettlement);
//        response.put("expenseBook", expenseBookDTO);

        System.out.println("📌 최종 결과물 - 방문지 선택 x : " + response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ieid}")
    public ResponseEntity<Map<String, Object>> getItineraryDataSelected(@PathVariable Long iid, @PathVariable Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 방문지 선택했을 때 실행하는 함수

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        Map<String, Object> response = new HashMap<>();

//        List<ExpenseItemDTO> expenseItemDTOList = expenseItemService.getAll(ieid);
        JournalDTO journalDTO = journalService.getJournal(ieid);
        FinanceResponseDTO partialSettlement = expenseBookService.calculateMoney(iid, ieid);

//        response.put("expenseItemList", expenseItemDTOList);
        response.put("journal", journalDTO);
        response.put("partialSettlement", partialSettlement);

        System.out.println("📌 최종 결과물 - 방문지 선택 : " + response);

        return ResponseEntity.ok(response);
    }


    // frontend 쪽에서 자체적으로 이전, 다음 버튼에 대한 처리 진행함
    // 고로, 아래 함수는 현재 안 씀
    @GetMapping("/{ieid}/next")
    public ResponseEntity<Map<String, Object>> getItineraryDataNext(@PathVariable Long iid, @PathVariable Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 방문지 선택 => 해당 방문지 기행문 열람 => 거기서 다음 버튼 눌렀을 때 호출

        Long uid = userDetails.getUser().getId();
        itineraryCollaboratorService.checkViewPermission(uid, iid); // 로그인 인증

        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, uid); // if exception 발생 => error 창으로 가도록 처리

        int currentIndex = -1;
        int i = 0;
        for (ItineraryEventSimpleDTO itineraryEvent : itineraryTotalDTO.getItineraryEvents()) {
            if (itineraryEvent.getId().equals(ieid)) {
                currentIndex = i;

                break;
            }

            i++;
        }

        List<ExpenseItemDTO> expenseItemDTOList = null;
        JournalDTO journalDTO = null;
        if ((currentIndex != -1) && (currentIndex + 1 < itineraryTotalDTO.getItineraryEvents().size())) {
            Long nextIeid = itineraryTotalDTO.getItineraryEvents().get(currentIndex + 1).getId();
            expenseItemDTOList = expenseItemService.getAll(nextIeid);
            journalDTO = journalService.getJournal(nextIeid);
        }

//        response.put("itineraryTotal", itineraryTotalDTO);
        response.put("expenseItemList", expenseItemDTOList);
        response.put("journal", journalDTO);

        System.out.println("📌 최종 결과물 - 방문지 선택한 상태 => 기행문 next 버튼 : " + response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ieid}/prev")
    public ResponseEntity<Map<String, Object>> getItineraryDataPrev(@PathVariable Long iid, @PathVariable Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 방문지 선택 => 해당 방문지 기행문 열람 => 거기서 이전 버튼 눌렀을 때 호출

        Long uid = userDetails.getUser().getId();
        itineraryCollaboratorService.checkViewPermission(uid, iid); // 로그인 인증

        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, uid); // if exception 발생 => error 창으로 가도록 처리

        int currentIndex = -1;
        int i = 0;
        for (ItineraryEventSimpleDTO itineraryEvent : itineraryTotalDTO.getItineraryEvents()) {
            if (itineraryEvent.getId().equals(ieid)) {
                currentIndex = i;

                break;
            }

            i++;
        }

        List<ExpenseItemDTO> expenseItemDTOList = null;
        JournalDTO journalDTO = null;
        if ((currentIndex != -1) && (currentIndex - 1 >= 0)) {
            Long nextIeid = itineraryTotalDTO.getItineraryEvents().get(currentIndex - 1).getId();
            expenseItemDTOList = expenseItemService.getAll(nextIeid);
            journalDTO = journalService.getJournal(nextIeid);
        }

//        response.put("itineraryTotal", itineraryTotalDTO);
        response.put("expenseItemList", expenseItemDTOList);
        response.put("journal", journalDTO);

        System.out.println("📌 최종 결과물 - 방문지 선택한 상태 => 기행문 prev 버튼 : " + response);

        return ResponseEntity.ok(response);
    }


}
