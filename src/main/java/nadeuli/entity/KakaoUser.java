package nadeuli.entity;

/* KakaoUser.java
 * 카카오 OAuth 2.0 연동 - DB
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 김대환       2.25     카카오 유저 트랜잭션 처리 Email -> UID 및 객체 항목 변경
 *
 * ========================================================
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class KakaoUser {
    @Id
    private Long uid;

    private String user_email;
    private String user_pw;
    private String user_name;
    private String provider;
    private String image_url;
    private String user_role;

    public KakaoUser(Long uid, String user_email, String user_pw, String user_name, String provider, String image_url, String user_role) {
        this.uid = uid;
        this.user_email = user_email;
        this.user_pw = user_pw;
        this.user_name = user_name;
        this.provider = provider;
        this.image_url = image_url;
        this.user_role = user_role;
    }
}
