package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nadeuli.entity.ItineraryCollaborator;

@Getter
@AllArgsConstructor
public class CollaboratorResponse {
    private Long userId;
    private String icRole;
    private String userName;

    public static CollaboratorResponse from(ItineraryCollaborator collaborator) {
        return new CollaboratorResponse(
                collaborator.getUser().getId(),
                collaborator.getIcRole(),
                collaborator.getUser().getUserName()
        );
    }
}
