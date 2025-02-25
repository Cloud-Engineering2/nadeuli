/* JournalDTO.java
 * nadeuli Service - 여행
 * Journal 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Journal;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JournalDTO {
    private Long id;
    private ItineraryEventDTO itineraryEventDTO;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    // static factory method
    public static JournalDTO of (Long id, ItineraryEventDTO itineraryEventDTO, String content, String imageUrl, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new JournalDTO(id, itineraryEventDTO, content, imageUrl, createdAt, modifiedAt);
    }

    public static JournalDTO of (ItineraryEventDTO itineraryEventDTO, String content, String imageUrl, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new JournalDTO(null, itineraryEventDTO, content, imageUrl, createdAt, modifiedAt);
    }

    // entity -> dto
    public static JournalDTO from(Journal journal) {
        return new JournalDTO(
                journal.getId(),
                ItineraryEventDTO.from(journal.getIeid()),
                journal.getContent(),
                journal.getImageUrl(),
                journal.getCreatedDate(),
                journal.getModifiedDate()
        );
    }

    // dto => entity
    public Journal toEntity() {
        return Journal.of(itineraryEventDTO.toEntity(), content, imageUrl);
    }
}
