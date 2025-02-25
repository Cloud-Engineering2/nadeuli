/* ItineraryRestController.java
 * Itinerary Rest 컨트롤러
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     일정탬플릿 생성, 내 일정리스트 조회, 특정 일정표 조회 (이벤트 포함) 추가
 *
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.response.ItineraryResponseDTO;
import nadeuli.dto.response.ItineraryTotalResponseDTO;
import nadeuli.service.ItineraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryRestController {

    private final ItineraryService itineraryService;

    // ===========================
    //  CREATE: 일정 탬플릿 생성 - !!수정필요 : 세션이 현재 없어서 다음과 같이 id 하드코딩
    // ===========================
    @PostMapping("/create")
    public ResponseEntity<ItineraryDTO> createItinerary(@RequestBody ItineraryDTO requestDTO) {
        ItineraryDTO itinerary = itineraryService.createItinerary(
                requestDTO.getItineraryName(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                1L // 비로그인 테스트중이라 id 하드코딩
        );
        return ResponseEntity.ok(itinerary);
    }

    // ===========================
    //  READ: 내 일정 리스트 조회 - !!수정필요 : 세션이 현재 없어서 다음과 같이 userID를 받도록 임시로 설정
    // ===========================
    @GetMapping("/mylist")
    public ResponseEntity<List<ItineraryResponseDTO>> getMyItineraries(@RequestParam Long userId) {
        List<ItineraryResponseDTO> itineraries = itineraryService.getMyItineraries(userId);
        return ResponseEntity.ok(itineraries);
    }

    // ===========================
    //  READ: 특정 일정표 조회 (이벤트 포함)
    // ===========================
    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryTotalResponseDTO> getItineraryTotal(@PathVariable Long itineraryId) {
        ItineraryTotalResponseDTO itineraryTotal = itineraryService.getItineraryTotal(itineraryId);
        return ResponseEntity.ok(itineraryTotal);
    }


}
