/* PhotoType.java
 * nadeuli Service - 여행
 * 어떤 사진인지 구분할 PhotoType (확장자 x, 프로필 사진 o)
 * 작성자 : 이홍비
 * 최초 작성 일자 : 2025.02.28
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : PhotoType
 * 이홍비    2025.03.05     지역 추가
 * 이홍비    2025.03.10     장소 추가
 * ========================================================
 */

package nadeuli.common.enums;

public enum PhotoType {
    ETC, // 그 외
    JOURNAL, // 여행 사진
    PLACE, // 장소 사진 - google places
    PROFILE, // 프로필 사진
    REGION // 지역 사진
}
