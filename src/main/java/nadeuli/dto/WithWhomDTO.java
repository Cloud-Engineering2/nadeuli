/* WithWhomDTO.java
 * nadeuli Service - 여행
 * WithWhom 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 고민정    2025.02.25     필드 수정
 * 고민정    2025.02.28     of 메서드 수정
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.Traveler;
import nadeuli.entity.WithWhom;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WithWhomDTO {
    private Integer id;
    private Long emid;
    private TravelerDTO travelerDTO;


    // static factory method
    public static WithWhomDTO of (Integer id, Long emid, TravelerDTO travelerDTO) {
        return new WithWhomDTO(id, emid, travelerDTO);
    }

    public static WithWhomDTO of (Long emid, TravelerDTO travelerDTO) {
        return new WithWhomDTO(null, emid, travelerDTO);
    }


    public static WithWhomDTO from(WithWhom whom) {
        return new WithWhomDTO(
                whom.getId(),
                whom.getEmid().getId(),
                TravelerDTO.from(whom.getTid())
        );
    }



    // dto => entity
    public WithWhom toEntity(ExpenseItem expenseItem, Traveler traveler) {
        return WithWhom.of(expenseItem, traveler);
    }

}
