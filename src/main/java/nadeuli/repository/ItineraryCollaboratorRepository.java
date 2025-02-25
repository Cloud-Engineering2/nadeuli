/* ItineraryCollaboratorRepository.java
 * ItineraryCollaborator 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.ItineraryCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryCollaboratorRepository extends JpaRepository<ItineraryCollaborator, Integer> {
    List<ItineraryCollaborator> findByUser_Id(Long userId); // User의 id로 찾기 Collborator
}