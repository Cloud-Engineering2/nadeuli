/* HashIdService.java
 * HashIdService
 * iid 난독화용
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

package nadeuli.service;

import org.hashids.Hashids;
import org.springframework.stereotype.Component;

@Component
public class HashIdService {

    private final Hashids hashids;

    // salt, 최소 길이(minHashLength) 등은 원하는 대로 설정
    public HashIdService() {
        this.hashids = new Hashids("nadeuli-salt", 15);
    }

    /**
     * Long ID -> 해시 문자열
     */
    public String encode(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        return hashids.encode(id);
    }

    /**
     * 해시 문자열 -> Long ID
     */
    public Long decode(String hash) {
        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) {
            throw new IllegalArgumentException("Invalid hash: " + hash);
        }
        return decoded[0];
    }
}