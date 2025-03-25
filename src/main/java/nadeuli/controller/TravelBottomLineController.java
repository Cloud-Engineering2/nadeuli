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
 * 이홍비    2025.03.20     최초 작성 : Controller 와 RestController 분리
 *                         로그인 인증 관련 처리
 * 이홍비    2025.03.25     방문지 o, x 관련 제약
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.response.ItineraryTotalReadResponseDTO;
import nadeuli.auth.oauth.CustomUserDetails;
import nadeuli.service.ItineraryCollaboratorService;
import nadeuli.service.ItineraryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@Controller
public class TravelBottomLineController {

    private final ItineraryService itineraryService;
    private final ItineraryCollaboratorService itineraryCollaboratorService;



    @GetMapping("/itineraries/{iid}/bottomline")
    public String showBottomlinePage(@PathVariable Long iid, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 페이지 이동

        Long uid = userDetails.getUser().getId();
        itineraryCollaboratorService.checkViewPermission(uid, iid);

        ItineraryTotalReadResponseDTO itineraryTotalReadResponseDTO = itineraryService.getItineraryTotal(iid, userDetails.getUser().getId()); // if exception 발생 => error 창으로 가도록 처리

        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO);
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItinerary());
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItineraryPerDays());
        System.out.println("📌 최종 결과물 - 페이지 이동 : " + itineraryTotalReadResponseDTO.getItineraryEvents());

        if (itineraryTotalReadResponseDTO.getItineraryEvents().isEmpty())
        {
            return "itinerary/bottomline-noEvent";
        }
        else {
            return "itinerary/bottomline";
        }
    }
}
