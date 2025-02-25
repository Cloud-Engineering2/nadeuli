package nadeuli.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid", nullable = false)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Lob
    @Column(name = "user_token", nullable = false)
    private String userToken;

    @Column(name = "user_name", nullable = false, length = 20)
    private String userName;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole;

}