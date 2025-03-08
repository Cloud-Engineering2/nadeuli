/* ItineraryStatusResponse.java
 * ItineraryStatusResponse
 * 공유 정보 관련 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-06
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

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ItineraryStatusResponse {
    private boolean isShared;
    private boolean hasGuest;
    private String userRole;
    private List<CollaboratorResponse> collaborators;
}