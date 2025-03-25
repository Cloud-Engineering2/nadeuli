/* ShareRestController.java
 * ShareRestController
 * 공유 관련 API
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.07    최초작성
 *
 * ========================================================
 */


package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.response.ItineraryStatusResponse;
import nadeuli.dto.response.JoinItineraryResponseDto;
import nadeuli.auth.oauth.CustomUserDetails;
import nadeuli.service.ShareService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareRestController {

    private final ShareService shareService;

    /**
     * 일정 공유 링크 생성 (POST /api/share/create)
     */
    @PostMapping("/create")
    public ResponseEntity<String> createShareLink(@RequestParam Long itineraryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            String shareToken = shareService.createShareToken(itineraryId, userId);
            return ResponseEntity.ok(shareToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/token")
    public ResponseEntity<String> getShareToken(@RequestParam Long itineraryId,@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            String shareToken = shareService.getShareToken(itineraryId, userId);
            return ResponseEntity.ok(shareToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 공유 토큰 삭제 (DELETE /api/share/delete)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteShareToken(@RequestParam Long itineraryId,@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            shareService.deleteShareToken(itineraryId, userId);
            return ResponseEntity.ok("공유 링크가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 공유 링크를 통해 Collaborator 등록 (POST /api/share/join)
     */
    @PostMapping("/join")
    public ResponseEntity<JoinItineraryResponseDto> joinItinerary(
            @RequestParam String shareToken,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JoinItineraryResponseDto("로그인이 필요합니다.", null));
        }

        try {
            JoinItineraryResponseDto response = shareService.joinItinerary(shareToken, userDetails.getUser().getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new JoinItineraryResponseDto(e.getMessage(), null));
        }
    }




    /**
     * OWNER가 특정 사용자를 Collaborator에서 삭제 또는 GUEST가 자기자신 삭제(DELETE /api/share/remove)
     */

    @DeleteMapping("/remove-mine")
    public ResponseEntity<String> removeCollaborator(@RequestParam Long itineraryId,@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        Long targetUserId = userId;

        try {
            String message = shareService.removeCollaborator(itineraryId, userId, targetUserId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeCollaborator(@RequestParam Long itineraryId,
                                                     @RequestParam Long targetUserId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        try {
            String message = shareService.removeCollaborator(itineraryId, userId, targetUserId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    /**
     * 일정(lid)의 공유 상태 & 게스트 존재 여부 조회 (GET /api/share/status)
     */
    @GetMapping("/status")
    public ResponseEntity<ItineraryStatusResponse> getItineraryStatus(@RequestParam Long itineraryId,@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        ItineraryStatusResponse response = shareService.getItineraryStatus(userId, itineraryId);
        return ResponseEntity.ok(response);
    }
}
