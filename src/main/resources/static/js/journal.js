// 글 수정
function editContent() {
    const contentSection = document.getElementById('contentSection');
    const contentText = document.getElementById('contentText');
    contentSection.innerHTML = `
        <textarea id="contentInput" rows="5" cols="50">${contentText.textContent}</textarea>
        <button class="btn" onclick="saveContent()">저장</button>
    `;
}

// 글 저장
function saveContent() {
    const contentInput = document.getElementById('contentInput').value;
    axios.put(`/api/itineraries/{iid}/events/{ieid}/content`, { content: contentInput })
        .then(response => {
            alert('기행문이 수정되었습니다.');
            document.getElementById('contentSection').innerHTML = `
                <p id="contentText">${contentInput}</p>
                <button id="editContentBtn" class="btn" onclick="editContent()">수정</button>
                <button id="deleteContentBtn" class="btn" onclick="deleteContent()">삭제</button>
            `;
        })
        .catch(error => {
            console.error(error);
            alert('수정에 실패했습니다.');
        });
}

// 글 삭제
function deleteContent() {
    axios.delete(`/api/itineraries/{iid}/events/{ieid}/content`)
        .then(response => {
            alert('기행문이 삭제되었습니다.');
            document.getElementById('contentSection').innerHTML = `
                <p>소중한 나들이 순간을 기록하세요.</p>
                <button id="writeContentBtn" class="btn" onclick="writeContent()">작성하기</button>
            `;
        })
        .catch(error => {
            console.error(error);
            alert('삭제에 실패했습니다.');
        });
}

// 사진 수정
function editPhoto() {
    const newPhoto = prompt("새로운 사진을 업로드할 URL을 입력하세요:");
    if (newPhoto) {
        axios.put(`/api/itineraries/{iid}/events/{ieid}/photo`, { imageURL: newPhoto })
            .then(response => {
                alert('사진이 수정되었습니다.');
                document.getElementById('photo').src = newPhoto;
            })
            .catch(error => {
                console.error(error);
                alert('사진 수정에 실패했습니다.');
            });
    }
}

// 사진 삭제
function deletePhoto() {
    axios.delete(`/api/itineraries/{iid}/events/{ieid}/photo`)
        .then(response => {
            alert('사진이 삭제되었습니다.');
            document.getElementById('photoArea').innerHTML = `
                <button class="add-photo-btn" onclick="uploadPhoto()">+</button>
            `;
        })
        .catch(error => {
            console.error(error);
            alert('사진 삭제에 실패했습니다.');
        });
}

// 사진 업로드
function uploadPhoto() {
    const photoInput = document.createElement('input');
    photoInput.type = 'file';
    photoInput.accept = 'image/*';
    photoInput.onchange = function(event) {
        const file = event.target.files[0];
        const formData = new FormData();
        formData.append('file', file);

        axios.post(`/api/itineraries/{iid}/events/{ieid}/photo`, formData)
            .then(response => {
                alert('사진이 업로드되었습니다.');
                document.getElementById('photoArea').innerHTML = `
                    <img id="photo" src="${response.data.imageUrl}" alt="사진" class="photo">
                    <button class="edit-btn" onclick="editPhoto()">➕</button>
                `;
            })
            .catch(error => {
                console.error(error);
                alert('사진 업로드에 실패했습니다.');
            });
    };
    photoInput.click();
}
