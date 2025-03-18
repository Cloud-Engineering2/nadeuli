// auth.js - JWT 관리 및 로그인 상태 체크

// ✅ JWT 저장 함수
function saveTokens(accessToken, refreshToken) {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
}

// ✅ JWT 삭제 (로그아웃 시 사용)
function clearTokens() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
}

// ✅ JWT 토큰 갱신 함수
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) {
        console.warn("🚨 리프레시 토큰이 없습니다. 로그인 페이지로 이동합니다.");
        window.location.href = "/login";
        return;
    }

    try {
        const response = await fetch("/auth/refresh/access", {
            method: "POST",
            credentials: "include",
            headers: { "Authorization": `Bearer ${refreshToken}` }
        });

        if (!response.ok) {
            throw new Error("Access Token 갱신 실패");
        }

        const data = await response.json();
        localStorage.setItem("accessToken", data.accessToken);
        console.log("✅ Access Token 갱신 성공");
    } catch (error) {
        console.error("🚨 Access Token 갱신 실패:", error);
        clearTokens();
        window.location.href = "/login";
    }
}

// ✅ 로그인 상태 확인 함수
async function checkAuthAndRedirect() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        console.warn("🚨 인증되지 않은 사용자! 로그인 페이지로 이동합니다.");
        window.location.href = "/login";
        return false;
    }

    try {
        const response = await fetch("/auth/user/me", {
            method: "GET",
            headers: { "Authorization": `Bearer ${accessToken}` }
        });

        if (!response.ok) {
            throw new Error("사용자 정보 조회 실패");
        }
        console.log("✅ 사용자 인증 성공");
        return true;
    } catch (error) {
        console.warn("🚨 Access Token이 만료됨. 리프레시 토큰으로 갱신 시도");
        await refreshAccessToken();
    }
}
