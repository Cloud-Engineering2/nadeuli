document.addEventListener("DOMContentLoaded", async () => {
    await getUserInfo(); // ✅ 사용자 정보 불러오기

    // ✅ 프로필 이미지 변경 이벤트 (사진 클릭)
    document.getElementById("profileInput").addEventListener("change", saveProfileImage);
});

// ✅ 사용자 정보 불러오기 (자동 입력)
async function getUserInfo() {
    try {
        const response = await fetch("/auth/user/me", {
            method: "GET",
            credentials: "include",
            headers: { "Authorization": `Bearer ${getCookie("accessToken")}` }
        });

        if (!response.ok) throw new Error("사용자 정보 불러오기 실패");

        const data = await response.json();

        // ✅ 프로필 이미지 로드
        document.getElementById("profileImage").src = data.profileImage && data.profileImage.trim() !== ""
            ? data.profileImage
            : "/images/default_profile.png";

        // ✅ 사용자 정보 자동 입력
        document.getElementById("userName").value = data.userName || "이름 없음";
        document.getElementById("userEmail").value = data.userEmail || "이메일 없음";

    } catch (error) {
        console.error("🚨 사용자 정보 불러오기 오류:", error);
    }
}

// ✅ 프로필 이미지 업로드
async function saveProfileImage(event) {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/auth/user/profile", {
            method: "POST",
            body: formData,
            credentials: "include",
            headers: { "Authorization": `Bearer ${getCookie("accessToken")}` }
        });

        if (!response.ok) throw new Error("프로필 이미지 업로드 실패");
        alert("✅ 프로필 이미지가 성공적으로 변경되었습니다.");
        window.location.reload();
    } catch (error) {
        console.error("🚨 프로필 이미지 업로드 실패:", error);
    }
}

// ✅ 쿠키에서 JWT 가져오기
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(";").shift();
}
