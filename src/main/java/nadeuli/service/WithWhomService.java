/* WithWhomService.java
 * WithWhom 서비스
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
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.repository.WithWhomRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WithWhomService {
    private final WithWhomRepository withWhomRepository;
}
