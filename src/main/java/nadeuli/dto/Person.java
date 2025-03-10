package nadeuli.dto;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Person {
    private Long total = 0L;                                // 지출
    private Map<String, Long> sendedMoney = new HashMap<>();            // 지불
    private Map<String, Long> receivedMoney = new HashMap<>();        // 받기



    public void send(String to, Long money) {
        sendedMoney.put(to, sendedMoney.getOrDefault(to, 0L) + money);
    }

    public void receive(String from, Long money) {
        receivedMoney.put(from, receivedMoney.getOrDefault(from, 0L) + money);
    }

    public void setTotal(Long total) {
        this.total = total;
    }

}