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
 * ========================================================
 */


// alert 창
function showNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'block';
}

function closeNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'none';
}

window.closeNadeuliAlert = closeNadeuliAlert;




function apiWithAutoRefresh(options) {
    $.ajax({
        ...options,
        success: options.success,
        error: function (xhr, status, error) {
            if (xhr.status === 401 && xhr.responseJSON?.recoveryHint === '/auth/refresh-rest') {
                $.post('/auth/refresh-rest', null, function (data) {
                    if (data.success) {
                        $.ajax(options);
                    } else {
                        alert('다시 로그인 해주세요.');
                        window.location.href = '/login';
                    }
                }).fail(function () {
                    alert('다시 로그인 해주세요.');
                    window.location.href = '/login';
                });
            } else {
                if (options.error) options.error(xhr, status, error);
            }
        }
    });
}

async function fetchWithAutoRefresh(url, fetchOptions = {}) {
    let response = await fetch(url, fetchOptions);

    // AccessToken 만료 + recoveryHint가 있는 경우 → refresh-rest 호출 후 재요청
    if (response.status === 401) {
        try {
            const errorData = await response.json();
            if (errorData?.recoveryHint === '/auth/refresh-rest') {
                const refreshResponse = await fetch('/auth/refresh-rest', { method: 'POST' });
                const refreshResult = await refreshResponse.json();

                if (refreshResult.success) {
                    // 토큰 재발급 성공 → 원래 요청 다시 수행
                    return await fetch(url, fetchOptions);
                } else {
                    alert('다시 로그인 해주세요.');
                    window.location.href = '/login';
                    throw new Error('Token refresh failed');
                }
            }
        } catch (e) {
            console.error('토큰 갱신 중 오류', e);
            alert('다시 로그인 해주세요.');
            window.location.href = '/login';
            throw new Error('Token refresh failed');
        }
    }

    return response;
}