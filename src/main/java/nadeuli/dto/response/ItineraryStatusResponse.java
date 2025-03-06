package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ItineraryStatusResponse {
    private boolean isShared;
    private boolean hasGuest;
    private String userRole;
    private List<CollaboratorResponse> collaborators;
}