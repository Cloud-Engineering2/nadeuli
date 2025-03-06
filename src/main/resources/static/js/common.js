/* common.js
 * nadeuli Service - 여행
 * 전반적으로 사용할 함수 정리
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.03.02
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.03.02     최초 작성 : common.js
 * 이홍비    2025.03.04     Header 쪽 함수 작성
 * ========================================================
 */


// alert 창
export function showNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'block';
}

function closeNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'none';
}

// function showLoggedInUI(id, name, role) {
//     const header = document.getElementById(id);
//
//
// }
//
// function showLoggedOutUI(id, name, role) {
//     const header = document.getElementById(id);
//
// }


window.closeNadeuliAlert = closeNadeuliAlert;
// window.showLoggedInUI = showLoggedInUI;
// window.showLoggedOutUi = showLoggedInUI;
