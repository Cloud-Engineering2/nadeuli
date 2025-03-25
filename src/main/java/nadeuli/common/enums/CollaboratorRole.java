package nadeuli.common.enums;

public enum CollaboratorRole {
    ROLE_OWNER,
    ROLE_GUEST;

    public static CollaboratorRole from(String roleStr) {
        return CollaboratorRole.valueOf(roleStr.toUpperCase());
    }
}