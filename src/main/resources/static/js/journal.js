/* journal.js
 * nadeuli Service - 여행
 * journal.html 에서 사용할 js 함수 정리
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.27     최초 작성 : journal.js
 * 박한철    2025.03.23     journal.js와 journal-modal.js의 공통된파트를 journal-common.js로 이동, 여기는 journal 페이지용 js로 변경
 * ========================================================
 */


window.isJournalModal = false;
window.onload = function() {

    // iid, ieid 추출 과정
    const pathParts = window.location.pathname.split("/");

    // 예상 URL 구조: /itineraries/{iid}/events/{ieid}/journal
    const iid = pathParts[2]; // 2번째 요소 (0-based index)
    const ieid = pathParts[4]; // 4번째 요소

    console.log("iid : ", iid);
    console.log("ieid : ", ieid);

    window.fetchJournal(iid, ieid);
};
