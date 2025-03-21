document.addEventListener("DOMContentLoaded", () => {
    // 프로필 이미지 업로드 이벤트 연결
    document.getElementById("profileInput").addEventListener("change", saveProfileImage);

    // 드롭다운 메뉴 토글
    const dropdownBtn = document.getElementById("profileDropdownBtn");
    const dropdownMenu = document.getElementById("profileDropdown");

    dropdownBtn.addEventListener("click", () => {
        dropdownMenu.style.display = dropdownMenu.style.display === "block" ? "none" : "block";
    });

    // 사용자 정보 자동 로딩
    getUserInfo();
});

// ✅ 사용자 정보 불러오기 (자동 입력)
async function getUserInfo() {
    try {
        const response = await fetch("/auth/user/me", {
            method: "GET",
            credentials: "include",
        });

        if (!response.ok) throw new Error("사용자 정보 불러오기 실패");

        const data = await response.json();

        // ✅ 프로필 이미지 로드
        document.getElementById("profileImage").src =
            data.profileImage && data.profileImage.trim() !== ""
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
        });

        if (!response.ok) throw new Error("프로필 이미지 업로드 실패");
        alert("✅ 프로필 이미지가 성공적으로 변경되었습니다.");
        window.location.reload();
    } catch (error) {
        console.error("🚨 프로필 이미지 업로드 실패:", error);
    }
}

// ✅ 사진 변경 버튼 → 업로드 input 열기
function triggerFileUpload() {
    document.getElementById("profileInput").click();
}

// ✅ 사진 저장
function downloadProfileImage() {
    window.location.href = "/auth/user/profile/download"; // 백엔드에서 해당 경로로 파일 다운로드 응답 필요
}

// ✅ 회원 탈퇴
function unlink() {
    if (!confirm("정말로 회원 탈퇴하시겠습니까?")) {
        return;
    }

    fetch("/auth/unlink", {
        method: "DELETE",
        credentials: "include"
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                alert("회원 탈퇴 완료되었습니다.");
                fetch("/auth/logout", { method: "POST", credentials: "include" })
                    .finally(() => location.href = "/");
            } else {
                alert("회원 탈퇴 실패: " + data.message);
            }
        })
        .catch(error => {
            console.error("회원 탈퇴 중 오류 발생:", error);
            alert("회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.");
        });
}
