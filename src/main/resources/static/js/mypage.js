document.addEventListener("DOMContentLoaded", () => {
    // 프로필 이미지 업로드 이벤트 연결
    document.getElementById("profileInput").addEventListener("change", uploadProfileImage);

    // 드롭다운 버튼 및 메뉴
    const dropdownBtn = document.getElementById("profileDropdownBtn");
    const dropdownMenu = document.getElementById("profileDropdown");

    // ✅ 드롭다운 메뉴 토글 + 테두리 효과
    dropdownBtn.addEventListener("click", (event) => {
        event.stopPropagation(); // 중요!
        const isActive = dropdownMenu.style.display === "block";
        dropdownMenu.style.display = isActive ? "none" : "block";
        dropdownBtn.classList.toggle("active", !isActive);
    });

// ✅ 외부 클릭 시 드롭다운 닫기
    document.addEventListener("click", (event) => {
        if (!dropdownBtn.contains(event.target) && !dropdownMenu.contains(event.target)) {
            dropdownMenu.style.display = "none";
            dropdownBtn.classList.remove("active");
        }
    });

// ✅ 드롭다운 내부 클릭 시 닫기 방지
    dropdownMenu.addEventListener("click", (event) => {
        event.stopPropagation();
    });


    // ✅ 드롭다운 내부 클릭 시 메뉴 유지
    dropdownMenu.addEventListener("click", (event) => {
        event.stopPropagation();
    });


    // 이름 저장 버튼 클릭 이벤트 연결
    document.getElementById("saveNameBtn").addEventListener("click", saveUserName);

    // 사용자 정보 자동 로딩
    //getUserInfo();
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

        const isDefault = !data.profileImage || data.profileImage.trim() === "" || data.profileImage.includes("default_profile.png");

        // ✅ 프로필 이미지 로드
        const profileImg = document.getElementById("profileImage");
        profileImg.src = isDefault ? "/images/default_profile.png" : data.profileImage;

        // ✅ "사진 받기" 버튼 상태 처리
        const downloadBtn = document.getElementById("downloadProfileBtn");
        if (downloadBtn) {
            if (isDefault) {
                downloadBtn.disabled = true;
                downloadBtn.style.opacity = "0.5";
                downloadBtn.style.cursor = "not-allowed";
            } else {
                downloadBtn.disabled = false;
                downloadBtn.style.opacity = "1";
                downloadBtn.style.cursor = "pointer";
            }
        }


        // ✅ 사용자 정보 자동 입력
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
        const res = await fetchWithAutoRefresh("/auth/user/profile/name", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ name: newName })
        });

        if (!res.ok) throw new Error("이름 저장 실패");

        await Swal.fire({
            icon: 'success',
            title: '✅ 이름 저장 완료',
            text: '이름이 성공적으로 저장되었습니다.'
        });

        input.setAttribute("readonly", true);
        document.getElementById("saveNameBtn").style.display = "none";
    } catch (err) {
        await Swal.fire({
            icon: 'error',
            title: '🚨 이름 저장 실패',
            text: err.message || '알 수 없는 오류가 발생했습니다.'
        });
    }
}


// ✅ 프로필 이미지 업로드
async function uploadProfileImage(event) {
    const file = event.target.files[0];

    // ✅ 허용 이미지 타입
    const validImageTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/bmp'];
    if (!validImageTypes.includes(file.type)) {
        await Swal.fire({
            icon: 'error',
            title: '🚫 업로드 실패',
            text: '지원하지 않는 이미지 형식입니다.'
        });
        return;
    }

    // ✅ 20MB 제한
    const maxSize = 20 * 1024 * 1024;
    if (file.size > maxSize) {
        await Swal.fire({
            icon: 'error',
            title: '🚫 업로드 실패',
            text: '파일 크기는 20MB를 초과할 수 없습니다.'
        });
        return;
    }

    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetchWithAutoRefresh("/auth/user/profile", {
            method: "POST",
            body: formData,
            credentials: "include"
        });

        if (!response.ok) throw new Error("프로필 이미지 업로드 실패");

        await Swal.fire({
            icon: 'success',
            title: '✅ 업로드 완료',
            text: '프로필 이미지가 성공적으로 변경되었습니다.'
        });

        window.location.reload();
    } catch (error) {
        console.error("🚨 프로필 이미지 업로드 실패:", error);
        await Swal.fire({
            icon: 'error',
            title: '🚨 업로드 실패',
            text: '프로필 이미지 업로드 중 오류가 발생했습니다.'
        });
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
    Swal.fire({
        title: '정말로 회원 탈퇴하시겠습니까?',
        text: "탈퇴 후 계정 복구는 불가능합니다.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#aaa',
        confirmButtonText: '탈퇴하기',
        cancelButtonText: '취소'
    }).then((result) => {
        if (result.isConfirmed) {
            // 로딩 표시
            Swal.fire({
                title: '탈퇴 처리 중입니다...',
                text: '잠시만 기다려주세요.',
                allowOutsideClick: false,
                allowEscapeKey: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            apiWithAutoRefresh({
                url: "/auth/unlink",
                method: "DELETE",
                xhrFields: { withCredentials: true },
                success: function (data) {
                    if (data.success) {
                        Swal.fire({
                            title: '탈퇴 완료',
                            text: '회원 탈퇴가 완료되었습니다.',
                            icon: 'success',
                            confirmButtonText: '확인'
                        }).then(() => {
                            apiWithAutoRefresh({
                                url: "/auth/logout",
                                method: "POST",
                                xhrFields: { withCredentials: true },
                                complete: function () {
                                    location.href = "/";
                                }
                            });
                        });
                    } else {
                        Swal.fire({
                            title: '탈퇴 실패',
                            text: data.message || '회원 탈퇴에 실패했습니다.',
                            icon: 'error'
                        });
                    }
                },
                error: function () {
                    Swal.fire({
                        title: '오류 발생',
                        text: '회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.',
                        icon: 'error'
                    });
                }
            });
        }
    });
}

