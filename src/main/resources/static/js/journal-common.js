/* journal-common.js
 * nadeuli Service - 여행
 * journal.html,journal모달 에서 사용할 js 함수 정리
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.27     최초 작성 : journal.js
 * 이홍비    2025.03.02     journal.html 에서 사용할 함수 정리
 * 이홍비    2025.03.03     사진 crud 관련 js 처리 + 다운로드 처리
 *                         첨부 가능한 사진 파일, 파일 크기 제약 추가
 *                         url 에서 iid, ieid 추출
 * 이홍비    2025.03.13     no-content 일 때 매번 innerText 작성하던 것
 *                         html 에 문구 작성해 두고 그냥 display block, none 만 하는 것으로 처리
 * 박한철    2025.03.22     최초 작성 : journal.js 에서 필요한 부분 추출
 *                         axios -> $.ajax로 수정
 * 이홍비    2025.03.23     showNadeuliAlert import 처리
 *                         window.fetchJournal = fetchJournal; - 전역 범위 할당
 * 박한철    2025.03.23     $.ajax를 토큰만료시 재발급받는 apiWithAutoRefresh로 감쌈
 * 박한철    2025.03.23     journal과 jounal-modal의 공통된 파트를 journal-common.js로 분리(window.isJournalModal로 구분함) + showNadeuliAlert를 코드에 포함시킴
 * ========================================================
 */

var journal = {};
var this_iid;
var this_ieid;


// 시간 출력 형태
// fetchJournal(), updateContentView() 사용
const timeFormat = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false // 24시간
}

function fetchJournal(iid, ieid) {
    this_iid = iid;
    this_ieid = ieid;

    apiWithAutoRefresh({
        url: `/api/itineraries/${iid}/events/${ieid}/journal`,
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            journal = response;
            console.log("fetchJournal - journal : ", journal);
            console.log("fetchJournal - journal.imageUrl : ", journal.imageUrl);
            console.log("fetchJournal - journal.content : ", journal.content);

            updateContentView();
            updatePhotoView();
        },
        error: function(xhr, status, error) {
            console.error("Error fetching journal:", error);
        }
    });
}



// window.onload = async function () {
//
//     // // iid, ieid 추출 과정
//     // const pathParts = window.location.pathname.split("/");
//     //
//     // // 예상 URL 구조: /itineraries/{iid}/events/{ieid}/journal
//     // const iid = pathParts[2]; // 2번째 요소 (0-based index)
//     // const ieid = pathParts[4]; // 4번째 요소
//     //
//     // console.log("iid : ", iid);
//     // console.log("ieid : ", ieid);
//
//     await fetchJournal(51, 150);
//     openJournalModal();
// };

function enableEditMode() {
    document.getElementById('content').style.display = 'none';
    document.getElementById('edit-content-area').style.display = 'block';
    document.getElementById('content-input').value = journal.content;
}

function cancelEdit() {
    updateContentView(journal.content);
}

function saveContent() {
    const contentInput = document.getElementById('content-input');
    const newContent = contentInput.value.trim();

    if (newContent === '') {
        showNadeuliAlert("content-alert");
        return;
    }

    const formData = new FormData();
    formData.append("content", newContent);

    const originalContent = journal.content;

    // 최초 작성 (POST)
    if (originalContent === null) {
        apiWithAutoRefresh({
            url: `/api/itineraries/${this_iid}/events/${this_ieid}/content`,
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                journal = response;
                updateContentView();
                if (window.isJournalModal && window.refreshJournalUI) {
                    window.refreshJournalUI();
                }
            },
            error: function(xhr, status, error) {
                console.error("Error saving content:", error);
            }
        });
    }
    // 수정 (PUT)
    else {
        if (newContent === originalContent.trim()) {
            cancelEdit();
            return;
        }

        apiWithAutoRefresh({
            url: `/api/itineraries/${this_iid}/events/${this_ieid}/content`,
            type: 'PUT',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                journal = response;
                updateContentView();
                if (window.isJournalModal && window.refreshJournalUI) {
                    window.refreshJournalUI();
                }
            },
            error: function(xhr, status, error) {
                console.error("Error saving content:", error);
            }
        });
    }
}


function updateContentView() {

    if (journal.content && (journal.content.trim() !== ""))  {
        // 글 작성함
        // document.getElementById("has-content").innerText = journalContent;

        document.getElementById("has-content").innerText = journal.content;

        document.getElementById('has-content').style.display = 'block';
        document.getElementById('no-content').style.display = 'none';

        document.getElementById("write-btn").style.display = "none";
        document.getElementById("edit-btn").style.display = "block";
        document.getElementById("delete-btn").style.display = "block";
    } else {
        // 글 작성 안 함
        document.getElementById('has-content').style.display = 'none';
        document.getElementById('no-content').style.display = 'block';

        document.getElementById("write-btn").style.display = "block";
        document.getElementById("edit-btn").style.display = "none";
        document.getElementById("delete-btn").style.display = "none";
    }

    console.log("journal.modifiedAt : " + journal.modifiedAt);
    document.getElementById('date-time').textContent = new Intl.DateTimeFormat('ko-KR', timeFormat).format(new Date(journal.modifiedAt));

    document.getElementById('edit-content-area').style.display = 'none';
    document.getElementById('content').style.display = 'block';
}

function confirmDelete() {
    if (confirm("글을 삭제하시겠습니까?")) {
        deleteContent();
    }
}

function deleteContent() {
    apiWithAutoRefresh({
        url: `/api/itineraries/${this_iid}/events/${this_ieid}/content`,
        type: 'DELETE',
        success: function(response) {
            journal = response;
            updateContentView();
            if (window.isJournalModal && window.refreshJournalUI) {
                window.refreshJournalUI();
            }
        },
        error: function(xhr, status, error) {
            console.error("Error deleting content:", error);
        }
    });
}



function uploadPhoto() {
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/*";
    fileInput.onchange = function(event) {
        const file = event.target.files[0];

        // 첨부 가능한 사진 파일 종류 제한
        const validImageTypes = ['image/bmp', 'image/gif', 'image/jpeg', 'image/png', 'image/webp'];
        if (!validImageTypes.includes(file.type)) {
            showNadeuliAlert('photo-type-alert');
            return;
        }

        // 파일 크기 제한 (20MB)
        const maxSize = 20 * 1024 * 1024;
        if (file.size > maxSize) {
            showNadeuliAlert('photo-size-alert');
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        apiWithAutoRefresh({
            url: `/api/itineraries/${this_iid}/events/${this_ieid}/photo`,
            type: 'POST',
            data: formData,
            processData: false, // FormData를 jQuery가 자동으로 변환하지 않도록 설정
            contentType: false, // Content-Type 헤더를 자동으로 설정
            success: function(response) {
                journal = response;
                updatePhotoView();
                if (window.isJournalModal && window.refreshJournalUI) {
                    window.refreshJournalUI();
                }
            },
            error: function(xhr, status, error) {
                console.error(error);
            }
        });
    };
    fileInput.click();
}


function modifyPhoto() {
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/*";
    fileInput.onchange = function(event) {
        const file = event.target.files[0];

        // 첨부 가능한 사진 파일 종류 제한
        const validImageTypes = ['image/bmp', 'image/gif', 'image/jpeg', 'image/png', 'image/webp'];
        if (!validImageTypes.includes(file.type)) {
            showNadeuliAlert('photo-type-alert');
            return;
        }

        // 파일 크기 제한 (20MB)
        const maxSize = 20 * 1024 * 1024;
        if (file.size > maxSize) {
            showNadeuliAlert('photo-size-alert');
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        apiWithAutoRefresh({
            url: `/api/itineraries/${this_iid}/events/${this_ieid}/photo`,
            type: 'PUT',
            data: formData,
            processData: false, // FormData 처리 비활성화
            contentType: false, // Content-Type 자동 설정 (multipart/form-data)
            success: function(response) {
                journal = response;
                updatePhotoView();
                if (window.isJournalModal && window.refreshJournalUI) {
                    window.refreshJournalUI();
                }
            },
            error: function(xhr, status, error) {
                console.error(error);
            }
        });

    };
    fileInput.click();
}


function confirmPhotoDelete() {
    if (confirm("사진을 삭제하시겠습니까?")) {
        deletePhoto();
    }
}

function deletePhoto() {
    apiWithAutoRefresh({
        url: `/api/itineraries/${this_iid}/events/${this_ieid}/photo`,
        type: 'DELETE',
        success: function(response) {
            journal = response;
            updatePhotoView();
            if (window.isJournalModal && window.refreshJournalUI) {
                window.refreshJournalUI();
            }
        },
        error: function(xhr, status, error) {
            console.error("Error deleting photo:", error);
        }
    });
}

function updatePhotoView() {

    document.getElementById('date-time').textContent = new Intl.DateTimeFormat('ko-KR', timeFormat).format(new Date(journal.modifiedAt));

    const imagePreview = document.getElementById("image-preview");
    if (journal.imageUrl !== null) {
        imagePreview.src = journal.imageUrl;
        imagePreview.style.display = "block";
        document.getElementById("plus-icon").style.display = "none";
        document.getElementById("image-container").style.backgroundColor="#F3F3F3";

        document.getElementById('upload-btn').style.display = 'none';
        document.getElementById("download-btn").style.display = 'block';
        document.getElementById('modify-btn').style.display = 'block';
        document.getElementById("remove-btn").style.display = 'block';
    }
    else {
        // 사진 삭제
        imagePreview.src = '';
        imagePreview.style.display = 'none';
        document.getElementById('plus-icon').style.display = 'block';
        document.getElementById("image-container").style.backgroundColor="#8E8B82";

        document.getElementById('upload-btn').style.display = 'block';
        document.getElementById("download-btn").style.display = 'none';
        document.getElementById('modify-btn').style.display = 'none';
        document.getElementById("remove-btn").style.display = 'none';
    }
}

function downloadPhoto() {
    apiWithAutoRefresh({
        url: `/api/itineraries/${this_iid}/events/${this_ieid}/photo/download`,
        method: 'GET',
        xhrFields: {
            responseType: 'arraybuffer' // 중요: arraybuffer로 응답받기
        },
        success: function(data, status, xhr) {
            const disposition = xhr.getResponseHeader('Content-Disposition');
            const matches = disposition && disposition.match(/filename\*?=['"]?UTF-8''([^;]+)['"]?|filename="?([^";]+)"?/);
            const encodedFileName = matches && (matches[1] || matches[2]) ? (matches[1] || matches[2]).trim() : 'photo.jpg';
            const fileName = decodeURIComponent(encodedFileName);

            const blob = new Blob([data], { type: 'application/octet-stream' });
            const imageUrl = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = imageUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(imageUrl);
        },
        error: function(xhr, status, error) {
            console.error("Error downloading photo:", error);
        }
    });
}


// 전역 범위 할달
window.fetchJournal = fetchJournal;
window.enableEditMode = enableEditMode;
window.saveContent = saveContent;
window.confirmDelete = confirmDelete;
window.cancelEdit = cancelEdit;
window.uploadPhoto = uploadPhoto;
window.modifyPhoto = modifyPhoto;
window.confirmPhotoDelete = confirmPhotoDelete;
window.downloadPhoto = downloadPhoto;
window.closeNadeuliAlert = closeNadeuliAlert;


function showNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'block';
}

function closeNadeuliAlert(id) {
    document.getElementById(`${id}`).style.display = 'none';
}

window.openJournalModal = function () {
    document.getElementById('journal-modal').style.display = 'flex';
};

window.closeJournalModal = function () {
    document.getElementById('journal-modal').style.display = 'none';
};