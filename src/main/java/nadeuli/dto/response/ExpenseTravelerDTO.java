package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nadeuli.dto.TravelerDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseTravelerDTO {
    private Integer id;
    private Long totalBudget;
    private Long totalExpense;
    private String name;
    private boolean isPayer;
    private boolean isConsumer;
}
