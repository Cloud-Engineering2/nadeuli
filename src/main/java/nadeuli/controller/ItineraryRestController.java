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
 * 박한철    2025.02.26     페이징 방식의 내 일정리스트 조회로 변경
 * 박한철    2025.02.27     DB 구조 변경으로 인한 일정 생성 파트 임시 주석처리
 * 박한철    2025.02.27     일정 생성 파트 주석해제후 수정완료
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.response.ItineraryCreateResponseDTO;
import nadeuli.dto.response.ItineraryResponseDTO;
import nadeuli.dto.response.ItineraryTotalResponseDTO;
import nadeuli.service.ItineraryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ItineraryCreateResponseDTO> createItinerary(@RequestBody ItineraryCreateRequestDTO requestDTO) {
        ItineraryCreateResponseDTO itinerary = itineraryService.createItinerary(
                requestDTO,
                1L // 비로그인 테스트중이라 id 하드코딩
        );
        return ResponseEntity.ok(itinerary);
    }

    // ===========================
    //  READ: 내 일정 리스트 조회 - !!수정필요 : 세션이 현재 없어서 다음과 같이 userID를 받도록 임시로 설정
    // ===========================

    @GetMapping("/mylist")
    public ResponseEntity<Page<ItineraryResponseDTO>> getMyItineraries(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "createdDate") String sortBy, // 정렬 기준 추가
            @RequestParam(required = false, defaultValue = "DESC") String direction, // 정렬 방향 추가
            @PageableDefault(size = 10) Pageable pageable) {

        // 동적으로 정렬 기준 적용
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<ItineraryResponseDTO> itineraries = itineraryService.getMyItineraries(userId, sortedPageable);
        return ResponseEntity.ok(itineraries);
    }


    // ===========================
    //  READ: 특정 일정표 조회 (이벤트 포함)
    // ===========================
    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryTotalResponseDTO> getItineraryTotal(@PathVariable Long itineraryId) {
        ItineraryTotalResponseDTO response = itineraryService.getItineraryTotal(itineraryId, 1L);
        return ResponseEntity.ok(response);
    }


}
