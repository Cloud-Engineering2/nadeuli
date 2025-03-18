// mypage.js - 마이페이지 관련 기능만 담당

document.addEventListener("DOMContentLoaded", async () => {
    await checkAuthAndRedirect(); // ✅ 로그인 상태 확인 후 진행
    await getUserInfo();

    // 프로필 이미지 변경 이벤트
    document.getElementById("profileInput").addEventListener("change", previewProfileImage);
    document.getElementById("saveProfile").addEventListener("click", saveProfileImage);

    // 회원 탈퇴 버튼 이벤트
    document.getElementById("deleteAccount").addEventListener("click", async () => {
        if (!confirm("정말로 회원 탈퇴를 진행하시겠습니까?")) {
            alert("회원 탈퇴가 취소되었습니다.");
            return;
        }
        try {
            const response = await fetch("/auth/user/delete", {
                method: "DELETE",
                headers: { "Authorization": `Bearer ${localStorage.getItem("accessToken")}` }
            });
            if (!response.ok) throw new Error("회원 탈퇴 실패");
            alert("✅ 회원 탈퇴가 완료되었습니다.");
            clearTokens();
            window.location.href = "/login";
        } catch (error) {
            console.error("🚨 회원 탈퇴 실패:", error);
        }
    });
});

// ✅ 사용자 정보 불러오기
async function getUserInfo() {
    try {
        const response = await fetch("/auth/user/me", {
            method: "GET",
            headers: { "Authorization": `Bearer ${localStorage.getItem("accessToken")}` }
        });
        if (!response.ok) throw new Error("사용자 정보 불러오기 실패");

        const data = await response.json();
        document.getElementById("profileImage").src = data.profileUrl && data.profileUrl.trim() !== '' ? data.profileUrl : "/static/images/default_profile.png";
        document.getElementById("userName").value = data.userName || "";
        document.getElementById("userEmail").value = data.userEmail || "";
    } catch (error) {
        console.error("🚨 사용자 정보 불러오기 오류:", error);
    }
}

// ✅ 프로필 이미지 미리보기
function previewProfileImage(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById("profileImage").src = e.target.result;
            document.getElementById("saveProfile").style.display = "block";
        };
        reader.readAsDataURL(file);
    }
}

// ✅ 프로필 이미지 저장
async function saveProfileImage() {
    const fileInput = document.getElementById("profileInput");
    const file = fileInput.files[0];
    if (!file) {
        alert("업로드할 파일을 선택해주세요.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/auth/user/profile", {
            method: "POST",
            body: formData,
            headers: { "Authorization": `Bearer ${localStorage.getItem("accessToken")}` }
        });
        if (!response.ok) throw new Error("프로필 이미지 업로드 실패");
        alert("✅ 프로필 이미지가 성공적으로 변경되었습니다.");
        window.location.reload();
    } catch (error) {
        console.error("🚨 프로필 이미지 업로드 실패:", error);
    }
}
