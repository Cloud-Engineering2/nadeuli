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
 * 이홍비    2025.03.02     journal.html 에서 사용할 함수 정리
 * 이홍비    2025.03.03     사진 crud 관련 js 처리 + 다운로드 처리
 *                         첨부 가능한 사진 파일, 파일 크기 제약 추가
 *                         url 에서 iid, ieid 추출
 * 이홍비    2025.03.13     no-content 일 때 매번 innerText 작성하던 것
 *                         html 에 문구 작성해 두고 그냥 display block, none 만 하는 것으로 처리
 * ========================================================
 */

import { showNadeuliAlert } from "./common.js";

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

async function fetchJournal(iid, ieid) {
    this_iid = iid;
    this_ieid = ieid;

    const response = await fetch(`/api/itineraries/${iid}/events/${ieid}/journal`);
    journal = await response.json();
    console.log("fetchJournal - journal : " + journal); // 데이터 확인
    console.log("fetchJournal - journal.imageUrl : " + journal.imageUrl); // 데이터 확인
    console.log("fetchJournal - journal.content : " + journal.content); // 데이터 확인

    updateContentView();
    updatePhotoView();
}


window.onload = function() {

    // iid, ieid 추출 과정
    const pathParts = window.location.pathname.split("/");

    // 예상 URL 구조: /itineraries/{iid}/events/{ieid}/journal
    const iid = pathParts[2]; // 2번째 요소 (0-based index)
    const ieid = pathParts[4]; // 4번째 요소

    console.log("iid : ", iid);
    console.log("ieid : ", ieid);

    fetchJournal(iid, ieid);
};

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
        // showContentAlert();
        showNadeuliAlert("content-alert");
        // alert("소중한 나들이 순간을 기록하세요!");

        return;
    }

    const formData = new FormData();
    formData.append("content", newContent);

    const originalContent = journal.content;
    if (originalContent === null) {
        // 처음 글 작성하는 것 => Post 호출
        axios.post(`/api/itineraries/${this_iid}/events/${this_ieid}/content`, formData)
            .then(response => {
                journal = response.data;
                updateContentView();
            })
            .catch(error => console.error("Error saving content:", error));
    }
    else {
        // 글 수정 => put
        if (newContent === originalContent.trim()) {
            // 변경 사항 x
            cancelEdit();

            return;
        }

        axios.put(`/api/itineraries/${this_iid}/events/${this_ieid}/content`, formData)
            .then(response => {
                journal = response.data;
                updateContentView();
            })
            .catch(error => console.error("Error saving content:", error));
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
    axios.delete(`/api/itineraries/${this_iid}/events/${this_ieid}/content`)
        .then(response => {
            journal = response.data;
            updateContentView();
        })
        .catch(error => console.error("Error deleting content:", error));
}


function uploadPhoto() {
    // 사진 업로드 로직 추가
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/*";
    fileInput.onchange = event => {
        const file = event.target.files[0];

        // 첨부 가능한 사진 파일 종륲 제한
        const validImageTypes = ['image/bmp', 'image/gif', 'image/jpeg', 'image/png', 'image/webp'];
        if (!validImageTypes.includes(file.type)) {
            showNadeuliAlert('photo-type-alert');

            return;
        }

        // 파일 크기 제한 (20MB)
        const maxSize = 20 * 1024 * 1024; // 20MB
        if (file.size > maxSize) {
            showNadeuliAlert('photo-size-alert');

            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        axios.post(`/api/itineraries/${this_iid}/events/${this_ieid}/photo`, formData)
            .then(response => {
                journal = response.data;
                updatePhotoView();
            })
            .catch(error => console.error(error));
    };
    fileInput.click();
}

function modifyPhoto() {
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/*";
    fileInput.onchange = event => {
        const file = event.target.files[0];

        // 첨부 가능한 사진 파일 종륲 제한
        const validImageTypes = ['image/bmp', 'image/gif', 'image/jpeg', 'image/png', 'image/webp'];
        if (!validImageTypes.includes(file.type)) {
            showNadeuliAlert('photo-type-alert');

            return;
        }

        // 파일 크기 제한 (20MB)
        const maxSize = 20 * 1024 * 1024; // 20MB
        if (file.size > maxSize) {
            showNadeuliAlert('photo-size-alert');

            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        axios.put(`/api/itineraries/${this_iid}/events/${this_ieid}/photo`, formData)
            .then(response => {
                journal = response.data;
                updatePhotoView();
            })
            .catch(error => console.error(error));
    };
    fileInput.click();

}

function confirmPhotoDelete() {
    if (confirm("사진을 삭제하시겠습니까?")) {
        deletePhoto();
    }
}

function deletePhoto() {
    axios.delete(`/api/itineraries/${this_iid}/events/${this_ieid}/photo`)
        .then(response => {
            journal = response.data;
            updatePhotoView();
        })
        .catch(error => console.error("Error deleting photo:", error));
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
    axios.get(`/api/itineraries/${this_iid}/events/${this_ieid}/photo/download`, { responseType: 'arraybuffer' })
        .then(response => {
            // console.log(response.data);
            // console.log(response.headers);
            // console.log("Response data type: ", response.data instanceof ArrayBuffer);
            // console.log("Response data type: ", response.data instanceof Blob);

            const disposition = response.headers['content-disposition'];
            // const matches = disposition && disposition.match(/filename="?([^";]+)"?/);
            // const fileName = matches && matches[1] ? matches[1].trim() : 'photo.jpg'

            const matches = disposition && disposition.match(/filename\*?=['"]?UTF-8''([^;]+)['"]?|filename="?([^";]+)"?/);
            const encodedFileName = matches && (matches[1] || matches[2]) ? (matches[1] || matches[2]).trim() : 'photo.jpg';
            const fileName = decodeURIComponent(encodedFileName);

            // console.log("disposition : " + disposition);
            // console.log("matches : " + matches);
            // console.log("fileName : " + fileName);

            const blob = new Blob([response.data], { type: 'application/octet-stream' });
            const imageUrl = URL.createObjectURL(blob); // Blob을 URL로 변환
            // const imageUrl = URL.createObjectURL(response.data); // Blob을 URL로 변환
            const link = document.createElement('a'); // 다운로드 링크 생성
            link.href = imageUrl; // 이미지 URL을 링크에 설정
            link.download = fileName; // 파일 이름 설정
            link.click(); // 다운로드 실행

            // URL 객체 해제 (메모리 누수 방지)
            URL.revokeObjectURL(imageUrl);
        })
        .catch(error => console.error("Error deleting photo:", error));
}

// 전역 범위 할달
window.enableEditMode = enableEditMode;
window.saveContent = saveContent;
window.confirmDelete = confirmDelete;
window.cancelEdit = cancelEdit;
window.uploadPhoto = uploadPhoto;
window.modifyPhoto = modifyPhoto;
window.confirmPhotoDelete = confirmPhotoDelete;
window.downloadPhoto = downloadPhoto;
