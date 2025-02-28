package nadeuli.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.response.ItineraryEventSimpleDTO;
import nadeuli.dto.response.ItineraryPerDaySimpleDTO;
import nadeuli.dto.response.ItineraryResponseDTO;

import java.util.List;
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryTotalCreateRequestDTO {

    private ItineraryDTO itinerary;
    private List<ItineraryPerDaySimpleDTO> itineraryPerDays;
    private List<ItineraryEventSimpleDTO> itineraryEvents;


}
