/* Person.java
 * Person
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-03-09
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.03.09   클래스 생성
 * 고민정    2025.03.11   total 필드 삭제
 * ========================================================
 */

package nadeuli.dto;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Person {
    private Map<String, Long> sendedMoney = new HashMap<>();            // 지불
    private Map<String, Long> receivedMoney = new HashMap<>();          // 받기

    public void send(String to, Long money) {
        sendedMoney.put(to, sendedMoney.getOrDefault(to, 0L) + money);
    }

    public void receive(String from, Long money) {
        receivedMoney.put(from, receivedMoney.getOrDefault(from, 0L) + money);
    }

}