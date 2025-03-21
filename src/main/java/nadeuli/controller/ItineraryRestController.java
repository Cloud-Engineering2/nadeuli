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
 * 박한철    2025.02.28     전체 일정 Update 파트 개발 완료
 * 박한철    2025.03.19     mylist의 Page 객체가 isLast를 리턴안해서 PageResponse로 감쌈
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.request.ItineraryTotalUpdateRequestDTO;
import nadeuli.dto.response.*;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.ItineraryCollaboratorService;
import nadeuli.service.ItineraryService;
import nadeuli.service.ShareService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryRestController {
    private final ItineraryCollaboratorService collaboratorService;
    private final ItineraryService itineraryService;
    private final ShareService shareService;

    // ===========================
    //  UPDATE(+CREATE/DELETE): 전체 일정 생성(+수정/삭제) - !!수정필요 : 세션이 현재 없어서 다음과 같이 id 하드코딩
    // ===========================
    @PostMapping("/update")
    public ResponseEntity<ItineraryTotalUpdateResponseDTO> updateTotalItinerary(@RequestBody ItineraryTotalUpdateRequestDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        collaboratorService.checkEditPermission(userId, requestDTO.getItinerary().getId(), false);

        ItineraryTotalUpdateResponseDTO response = itineraryService.saveOrUpdateItinerary(requestDTO);
        return ResponseEntity.ok(response);
    }


    // ===========================
    //  CREATE: 일정 탬플릿 생성 - !!수정필요 : 세션이 현재 없어서 다음과 같이 id 하드코딩
    // ===========================
    @PostMapping("/create")
    public ResponseEntity<ItineraryCreateResponseDTO> createItinerary(@RequestBody ItineraryCreateRequestDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        ItineraryCreateResponseDTO response = itineraryService.createItinerary(
                requestDTO,
                userId
        );
        return ResponseEntity.ok(response);
    }

    // ===========================
    //  READ: 내 일정 리스트 조회 - !!수정필요 : 세션이 현재 없어서 다음과 같이 userID를 받도록 임시로 설정
    // ===========================

    @GetMapping("/mylist")
    public ResponseEntity<PageResponse<ItineraryResponseDTO>> getMyItineraries(
            @RequestParam(required = false, defaultValue = "createdDate") String sortBy, // 정렬 기준 추가
            @RequestParam(required = false, defaultValue = "DESC") String direction, // 정렬 방향 추가
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        // 동적으로 정렬 기준 적용
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<ItineraryResponseDTO> itineraries = itineraryService.getMyItineraries(userId, sortedPageable);
        return ResponseEntity.ok(new PageResponse<>(itineraries));
    }


    // ===========================
    //  READ: 특정 일정표 조회 (이벤트 포함)
    // ===========================
    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryTotalReadResponseDTO> getItineraryTotal(@PathVariable Long itineraryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        ItineraryTotalReadResponseDTO response = itineraryService.getItineraryTotal(itineraryId, userId);
        return ResponseEntity.ok(response);
    }





}
