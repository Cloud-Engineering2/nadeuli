package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Person2 {
    private Long totalBudget = 0L;                                    // 예산
    private Long totalExpense = 0L;                                   // 지출
    private Map<String, Long> sendedMoney = new HashMap<>();          // 지불
    private Map<String, Long> receivedMoney = new HashMap<>();        // 받기



    public void send(String to, Long money) {
        sendedMoney.put(to, sendedMoney.getOrDefault(to, 0L) + money);
    }

    public void receive(String from, Long money) {
        receivedMoney.put(from, receivedMoney.getOrDefault(from, 0L) + money);
    }

    public void updateTotalExpense(Long totalExpense) {
        this.totalExpense = totalExpense;
    }

}