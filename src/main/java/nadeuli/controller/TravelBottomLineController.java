/* TravelBottomLineController.java
 * nadeuli Service - 여행
 * 일정 최종 결산 관련 처리 controller
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
 * ========================================================
 */


package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.JournalDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.response.ItineraryEventSimpleDTO;
import nadeuli.dto.response.ItineraryTotalReadResponseDTO;
import nadeuli.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class TravelBottomLineController {
    private final ItineraryService itineraryService;
    private final ExpenseBookService expenseBookService;
    private final ExpenseItemService expenseItemService;
    private final JournalService journalService;
    private final TravelerService travelerService;
    private final WithWhomService withWhomService;


    @GetMapping("/itineraries/{iid}/bottomline")
    public String showBottomlinePage(@PathVariable Long iid) { //, @RequestParam Long userId) {
        ItineraryTotalReadResponseDTO itineraryTotalReadResponseDTO = itineraryService.getItineraryTotal(iid, 1L); // if exception 발생 => error 창으로 가도록 처리

        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO);
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItinerary());
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItineraryPerDays());
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItineraryEvents());


        return "/itinerary/bottomline";
    }

    @ResponseBody
    @GetMapping("/api/itineraries/{iid}/bottomline")
    public ResponseEntity<Map<String, Object>> getItineraryData(@PathVariable Long iid) { //, @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, 1L); // if exception 발생 => error 창으로 가도록 처리
        List<TravelerDTO> travelerDTOList = travelerService.listTravelers(iid + 1L);
        ExpenseBookDTO expenseBookDTO = expenseBookService.getExpenseBook(iid);


        response.put("itineraryTotal", itineraryTotalDTO);
        response.put("travelerList", travelerDTOList);
        response.put("expenseBook", expenseBookDTO);

        System.out.println("📌 최종 결과물 - 방문지 선택 x : " + response);

        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @GetMapping("/api/itineraries/{iid}/bottomline/{ieid}")
    public ResponseEntity<Map<String, Object>> getItineraryDataSelected(@PathVariable Long iid, @PathVariable Long ieid) {
        Map<String, Object> response = new HashMap<>();

        List<ExpenseItemDTO> expenseItemDTOList = expenseItemService.getAll(ieid);
        JournalDTO journalDTO = journalService.getJournal(ieid);

        response.put("expenseItemList", expenseItemDTOList);
        response.put("journal", journalDTO);

        System.out.println("📌 최종 결과물 - 방문지 선택 : " + response);

        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @GetMapping("/api/itineraries/{iid}/bottomline/{ieid}/next")
    public ResponseEntity<Map<String, Object>> getItineraryDataNext(@PathVariable Long iid, @PathVariable Long ieid) { //, @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, 1L); // if exception 발생 => error 창으로 가도록 처리

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

    @ResponseBody
    @GetMapping("/api/itineraries/{iid}/bottomline/{ieid}/prev")
    public ResponseEntity<Map<String, Object>> getItineraryDataPrev(@PathVariable Long iid, @PathVariable Long ieid) { //, @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        ItineraryTotalReadResponseDTO itineraryTotalDTO = itineraryService.getItineraryTotal(iid, 1L); // if exception 발생 => error 창으로 가도록 처리

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
