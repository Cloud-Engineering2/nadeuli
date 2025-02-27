/* TravelerResponse.java
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

package nadeuli.dto;

import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class TravelerResponse {
    private List<TravelerInfo> travelers;
    private Integer numberOfTravelers;

    public static TravelerResponse toResponse(List<TravelerDTO> travelerDtos) {
        if (travelerDtos == null) {
            return null;
        }

        List<TravelerInfo> travelerList = travelerDtos.stream()
                .map(traveler -> new TravelerInfo(traveler.getId(), traveler.getTravelerName()))
                .collect(Collectors.toList());

        return TravelerResponse.builder()
                .travelers(travelerList)
                .numberOfTravelers(travelerList.size())
                .build();
    }


    @Getter
    @AllArgsConstructor
    public static class TravelerInfo {
        private Integer id;
        private String name;
    }


}




