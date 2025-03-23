document.addEventListener("DOMContentLoaded", () => {
    // 프로필 이미지 업로드 이벤트 연결
    document.getElementById("profileInput").addEventListener("change", saveProfileImage);

    // 드롭다운 버튼 및 메뉴
    const dropdownBtn = document.getElementById("profileDropdownBtn");
    const dropdownMenu = document.getElementById("profileDropdown");

    // 드롭다운 토글
    dropdownBtn.addEventListener("click", (event) => {
        event.stopPropagation();
        const isActive = dropdownMenu.style.display === "block";
        dropdownMenu.style.display = isActive ? "none" : "block";
        dropdownBtn.classList.toggle("active", !isActive);
    });

    dropdownMenu.addEventListener("click", (event) => {
        event.stopPropagation();
    });

    document.addEventListener("click", () => {
        dropdownMenu.style.display = "none";
        dropdownBtn.classList.remove("active");
    });

    // 이름 저장 버튼 클릭 이벤트 연결
    document.getElementById("saveNameBtn").addEventListener("click", saveUserName);

    // 사용자 정보 자동 로딩
    getUserInfo();
});

// ✅ 사용자 정보 불러오기
async function getUserInfo() {
    try {
        const response = await fetch("/auth/user/me", {
            method: "GET",
            credentials: "include",
        });

        if (!response.ok) throw new Error("사용자 정보 불러오기 실패");

        const data = await response.json();

        document.getElementById("profileImage").src =
            data.profileImage && data.profileImage.trim() !== ""
                ? data.profileImage
                : "/images/default_profile.png";

        document.getElementById("userName").value = data.userName || "이름 없음";
        document.getElementById("userEmail").value = data.userEmail || "이메일 없음";
    } catch (error) {
        console.error("🚨 사용자 정보 불러오기 오류:", error);
    }
}

// ✅ 이름 변경 버튼 (드롭다운에서 호출)
function triggerNameEdit() {
    const input = document.getElementById("userName");
    const saveBtn = document.getElementById("saveNameBtn");

    input.removeAttribute("readonly");
    input.focus();
    saveBtn.style.display = "inline-block";

    // 드롭다운 닫기
    document.getElementById("profileDropdown").style.display = "none";
    document.getElementById("profileDropdownBtn").classList.remove("active");
}

// ✅ 이름 저장
async function saveUserName() {
    const input = document.getElementById("userName");
    const newName = input.value;

    try {
        const res = await fetch("/auth/user/profile/name", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ name: newName })
        });

        if (!res.ok) throw new Error("이름 저장 실패");

        alert("✅ 이름이 저장되었습니다.");
        input.setAttribute("readonly", true);
        document.getElementById("saveNameBtn").style.display = "none";
    } catch (err) {
        alert("🚨 이름 저장 실패: " + err.message);
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

// ✅ 사진 변경 input 트리거
function triggerFileUpload() {
    document.getElementById("profileInput").click();
}

// ✅ 사진 저장
function downloadProfileImage() {
    window.location.href = "/auth/user/profile/download";
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
