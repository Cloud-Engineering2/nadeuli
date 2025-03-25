/* TravelerResponseDTO.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   Res DTO 생성
 *
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.*;
import nadeuli.dto.TravelerDTO;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class TravelerResponseDTO2 {
    private List<ExpenseTravelerDTO> travelers;
    private Integer numberOfTravelers;

    public static TravelerResponseDTO2 toResponse(List<ExpenseTravelerDTO> travelerDTOs) {
        return TravelerResponseDTO2.builder()
                .travelers(travelerDTOs)
                .numberOfTravelers(travelerDTOs != null ? travelerDTOs.size() : 0)
                .build();
    }

}




