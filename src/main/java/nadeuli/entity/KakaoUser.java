//package nadeuli.entity;
//
///* KakaoUser.java
// * 카카오 OAuth 2.0 연동 - DB
// * 해당 파일 설명
// * 작성자 : 김대환
// * 최초 작성 날짜 : 2025-02-20
// *
// * ========================================================
// * 프로그램 수정 / 보완 이력
// * ========================================================
// * 작업자       날짜       수정 / 보완 내용
// * ========================================================
// *
// *
// * ========================================================
// */
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "kakao_user")
//@Getter
//@NoArgsConstructor
//public class KakaoUser {
//    @Id
//    private String id;
//
//    private String nickname;
//    private String profileImage;
//    private String email;
//
//    public KakaoUser(String id, String nickname, String profileImage, String email) {
//        this.id = id;
//        this.nickname = nickname;
//        this.profileImage = profileImage;
//        this.email = email;
//    }
//}
