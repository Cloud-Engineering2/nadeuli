let regionMap = new Map();
let nameToIdMap = new Map();
let levelMap = new Map();

let table = null;
let tbody = null;
let mapReady = false;

let circle =null;
let map = null;
let isCenterLocked = false; // 고정 상태 토글 변수
let lockedCenter = null;

function initMap() {
    console.log("initMap Execute");

    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: 37.5665, lng: 126.9780 },
        zoom: 13,
        fullscreenControl: false,    // 전체화면 버튼 비활성화
        streetViewControl: false,    // 스트리트뷰 버튼 비활성화
        mapTypeControl: false        // 지도 유형 변경 버튼 비활성화
    });

    circle = new google.maps.Circle({
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.2,
        map: map,
        center: map.getCenter(),
        radius: 500, // 기본 반경
        editable: true,  // 사용자가 반경 조절 가능
        draggable: false
    });

    // 지도 로드 완료 후 중심 좌표 출력
    // 중심 변화 감지 (google.maps.event 방식)
    google.maps.event.addListener(map, 'center_changed', function () {
        const center = map.getCenter();
        if (!isCenterLocked) {
            circle.setCenter(center);
            document.getElementById('mapLatitude').value = center.lat().toFixed(6);
            document.getElementById('mapLongitude').value = center.lng().toFixed(6);
        }
    });

    google.maps.event.addListener(circle, 'center_changed', function () {
        if (isCenterLocked) {
            const center = circle.getCenter();
            document.getElementById('mapLatitude').value = center.lat().toFixed(6);
            document.getElementById('mapLongitude').value = center.lng().toFixed(6);
        }
    });


    google.maps.event.addListener(circle, 'radius_changed', function () {
        document.getElementById('mapRadius').value = circle.getRadius().toFixed(0);
    });
    mapReady = true;

}

document.getElementById('toggleCenterLock').addEventListener('click', function () {
    isCenterLocked = !isCenterLocked;
    if (isCenterLocked) {
        lockedCenter = map.getCenter();
        this.textContent = '원 중심 맵고정 해제';
    } else {
        lockedCenter = null;
        this.textContent = '원 중심 맵고정';
    }
});

document.getElementById('mapRadius').addEventListener('input', function () {
    const radius = parseFloat(this.value);
    if (!isNaN(radius) && circle) {
        circle.setRadius(radius);
    }
});

document.addEventListener('DOMContentLoaded', async () => {
    table = document.querySelector('.region-table');
    tbody = table.querySelector('tbody');
    await fetchRegionsData(true); // 최초 실행 시 트리를 생성해야 함
});

document.body.addEventListener('click', async (event) => {
    if (event.target.classList.contains('refresh-btn')) {
        await fetchRegionsData(false);
    }
});

document.getElementById('copyMapToFormBtn').addEventListener('click', function () {
    document.getElementById('regionLatitude').value = document.getElementById('mapLatitude').value;
    document.getElementById('regionLongitude').value = document.getElementById('mapLongitude').value;
    document.getElementById('regionRadius').value = document.getElementById('mapRadius').value;
});


async function fetchRegionsData(initialize = false) {
    try {
        const response = await fetch('/api/regions');
        if (!response.ok) return;

        const newData = await response.json();
        updateRegionData(newData);

        if (initialize) {
            createTableRows(); // 처음 실행할 때만 트리를 생성
        } else {
            updateTableContents(); // 새로고침 시 테이블 내용을 갱신
        }
    } catch (error) {
        alert(error.message);
    }
}


// 데이터 저장 및 매핑
function updateRegionData(newData) {
    regionMap.clear();
    nameToIdMap.clear();
    levelMap.clear();

    newData.forEach(region => {
        regionMap.set(region.id, region);
        nameToIdMap.set(region.name, region.id);

        if (!levelMap.has(region.level)) {
            levelMap.set(region.level, []);
        }
        levelMap.get(region.level).push(region);
    });
}

// 테이블의 기존 행을 유지하면서 내용을 갱신
function updateTableContents() {
    document.querySelectorAll('.region-table tbody tr').forEach(row => {
        const regionId = Number(row.getAttribute('data-id'));
        if (regionMap.has(regionId)) {
            const region = regionMap.get(regionId);

            row.children[0].querySelector('.name-container span').innerText = region.name;
            row.children[1].innerText = region.latitude || '-';
            row.children[2].innerText = region.longitude || '-';
            row.children[3].innerText = region.radius || '-';
            row.children[4].html = region.imageUrl ? `<a href="${region.imageUrl}" target="_blank">📷 이미지 보기</a>` : '-';

        }
    });

    alert('데이터가 갱신되었습니다!');
}




// 트리 구조로 출력
function createTableRows(parentId = null, level = 1, indent = 0) {
    const regions = levelMap.get(level) || [];

    regions
        .filter(region => (parentId === null && !region.parent) || (region.parent && region.parent.id === parentId))
        .forEach(region => {
            const row = document.createElement('tr');
            row.setAttribute('data-id', region.id);
            row.setAttribute('data-level', level);
            if (region.parent) row.setAttribute('data-parent-id', region.parent.id);

            // 자식이 있는지 확인
            const hasChildren = (levelMap.get(level + 1) || []).some(child => child.parent && child.parent.id === region.id);

            // 부모 노드 아이콘 추가 (자식이 있을 경우만)
            const toggleButton = hasChildren
                ? `<span class="toggle-btn" data-id="${region.id}" data-collapsed="false">▶</span>`
                : '';

            row.innerHTML = `
                    <td style="padding-left: ${indent * 20}px;">
                        <div class="name-container">
                            <span>${region.name}</span>
                            ${toggleButton}
                        </div>
                    </td>
                    <td>${region.latitude || '-'}</td>
                    <td>${region.longitude || '-'}</td>
                    <td>${region.radius || '-'}</td>
                    <td>${region.imageUrl ? `<a href="${region.imageUrl}" target="_blank">📷 이미지 보기</a>` : '-'}</td>
                `;

            tbody.appendChild(row);

            // 하위 노드 추가 (초기에는 숨김)
            createTableRows(region.id, level + 1, indent + 1);
        });
}


// 접기/펼치기 이벤트 추가
document.body.addEventListener('click', event => {
    if (event.target.classList.contains('toggle-btn')) {
        const parentId = event.target.getAttribute('data-id');
        const isCollapsed = event.target.getAttribute('data-collapsed') === 'true';

        event.target.innerHTML = isCollapsed ? '▶' : '▼';
        event.target.setAttribute('data-collapsed', !isCollapsed);

        document.querySelectorAll(`tr[data-parent-id="${parentId}"]`).forEach(childRow => {
            childRow.style.display = isCollapsed ? 'table-row' : 'none';

            if (!isCollapsed) {
                const childId = childRow.getAttribute('data-id');
                document.querySelectorAll(`tr[data-parent-id="${childId}"]`).forEach(grandChild => {
                    grandChild.style.display = 'none';
                    const grandToggleBtn = grandChild.querySelector('.toggle-btn');
                    if (grandToggleBtn) {
                        grandToggleBtn.innerHTML = '▶';
                        grandToggleBtn.setAttribute('data-collapsed', 'true');
                    }
                });
            }
        });
    }
});


document.body.addEventListener('click', event => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        const regionId = row.getAttribute('data-id');
        const region = regionMap.get(Number(regionId));

        if (region) {
            console.log(`클릭한 행 ID: ${regionId}`);
            document.getElementById('regionId').value = region.id;
            document.getElementById('regionName').value = region.name || '';
            document.getElementById('regionLatitude').value = region.latitude || '';
            document.getElementById('regionLongitude').value = region.longitude || '';
            document.getElementById('regionRadius').value = region.radius || '';
            document.getElementById('regionImage').value = region.imageUrl || '';

            // ✅ 지도 이동
            if (region.latitude && region.longitude) {
                const lat = parseFloat(region.latitude);
                const lng = parseFloat(region.longitude);
                if (!isNaN(lat) && !isNaN(lng)) {
                    map.setCenter({ lat, lng });
                }
            }

            // ✅ 반경도 원에 적용
            if (region.radius) {
                const radius = parseFloat(region.radius);
                if (!isNaN(radius) && circle) {
                    circle.setRadius(radius);
                    document.getElementById('mapRadius').value = radius.toFixed(0);
                }
            }

            isCenterLocked = true;
            lockedCenter = map.getCenter(); // 또는 { lat, lng } 로 바로 넣어도 됨
            circle.setCenter(lockedCenter); // 원도 바로 고정 위치로 맞춤

            const lockBtn = document.getElementById('toggleCenterLock');
            if (lockBtn) {
                lockBtn.textContent = '원 중심 맵고정 해제';
            }

            // 이미지 프리뷰 업데이트
            const imagePreview = document.getElementById('imagePreview');
            console.log(imagePreview);
            if (region.imageUrl) {
                imagePreview.src = region.imageUrl;
            } else {
                imagePreview.src = '/images/logo-256x256.jpg';
            }
        }
    }
});


document.body.addEventListener('click', event => {
    if (event.target.id === 'saveRegionButton') {
        console.log('저장 버튼 클릭됨');

        const regionId = Number(document.getElementById('regionId').value);
        const latitude = parseFloat(document.getElementById('regionLatitude').value);
        const longitude = parseFloat(document.getElementById('regionLongitude').value);
        const radius = parseFloat(document.getElementById('regionRadius').value);

        if (!regionId || isNaN(latitude) || isNaN(longitude) || isNaN(radius)) {
            alert("입력값이 올바르지 않습니다.");
            return;
        }

        // ✅ 서버에 전송할 데이터 구성 (AdminRegionUpdateRequestDTO 구조)
        const requestData = {
            id: regionId,
            latitude: latitude,
            longitude: longitude,
            radius: radius
        };

        // ✅ apiWithAutoRefresh로 AJAX 요청
        apiWithAutoRefresh({
            url: '/api/admin/region/update',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function () {
                // 서버 응답 성공 시 클라이언트 local regionMap도 업데이트
                if (regionMap.has(regionId)) {
                    const region = regionMap.get(regionId);
                    region.latitude = latitude;
                    region.longitude = longitude;
                    region.radius = radius;

                    // 테이블 UI 동기화
                    const row = document.querySelector(`tr[data-id="${regionId}"]`);
                    if (row) {
                        row.children[1].innerText = latitude.toFixed(6);
                        row.children[2].innerText = longitude.toFixed(6);
                        row.children[3].innerText = radius.toFixed(0);
                    }
                }

                alert("지역 정보가 성공적으로 수정되었습니다!");
            },
            error: function (xhr) {
                console.error(xhr);
                alert("수정 요청 실패: " + (xhr.responseJSON?.message || '서버 오류'));
            }
        });
    }
});
// 파일이 선택되었을 때 미리보기 표시 및 자동 업로드 실행
document.getElementById('imageUpload').addEventListener('change', event => {
    const file = event.target.files[0];

    if (!file) return;

    // 지원하는 이미지 파일인지 확인
    const validImageTypes = ['image/bmp', 'image/gif', 'image/jpeg', 'image/png', 'image/webp'];
    if (!validImageTypes.includes(file.type)) {
        alert('지원하지 않는 이미지 형식입니다.');
        document.getElementById('imageUpload').value = ''; // 선택된 파일 초기화
        return;
    }

    // 파일 크기 제한 (20MB)
    const maxSize = 30 * 1024 * 1024; // 20MB
    if (file.size > maxSize) {
        alert('이미지 크기가 30MB를 초과합니다.');
        document.getElementById('imageUpload').value = ''; // 선택된 파일 초기화
        return;
    }

    // 저작권 입력을 위한 다이얼로그 창 표시
    let watermarkText = prompt(" Copyright @ { 쓸 내용 } ", "쓸내용");

    // 사용자가 입력하지 않고 취소 버튼을 누른 경우
    if (watermarkText === null) {
        alert("이미지 업로드가 취소되었습니다.");
        return;
    }

    watermarkText = "Copyright @" + watermarkText;
    // 미리보기 및 워터마크 추가 후 업로드 실행
    addWatermark(file, watermarkText, uploadImage);
});

// 🔹 이미지에 워터마크를 추가하는 함수
function addWatermark(file, watermarkText, callback) {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = function(event) {
        const img = new Image();
        img.src = event.target.result;
        img.onload = function() {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');

            canvas.width = img.width;
            canvas.height = img.height;

            // 원본 이미지 그리기
            ctx.drawImage(img, 0, 0);

            // 📌 워터마크 폰트 크기 자동 조절 (이미지 너비의 5% 크기)
            const fontSize = Math.max(canvas.width * 0.01, 5); // 최소 10px 이상 유지
            ctx.font = `${fontSize}px Arial`;
            ctx.fillStyle = "rgba(255, 255, 255, 0.7)";
            ctx.textAlign = "right";

            // 📌 워터마크 위치 (우측 하단, 이미지 크기에 맞춰 적절한 여백 설정)
            const margin = fontSize * 1.5; // 폰트 크기의 1.5배 만큼 여백
            const x = canvas.width - margin;
            const y = canvas.height - margin;

            ctx.fillText(watermarkText, x, y);

            // 미리보기 표시
            document.getElementById('imagePreview').src = canvas.toDataURL();
            document.getElementById('imagePreview').classList.remove('d-none');

            // Blob으로 변환 후 업로드 실행
            canvas.toBlob(blob => {
                const watermarkedFile = new File([blob], file.name, { type: file.type });
                callback(watermarkedFile);
            }, file.type);
        };
    };
}

// 🔹 이미지 업로드 함수
function uploadImage(file) {
    const regionId = Number(document.getElementById('regionId').value);

    if (!regionMap.has(regionId)) {
        alert('지역을 선택하세요.');
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("regionId", regionId);

    apiWithAutoRefresh({
        url: `/api/admin/region/upload/photo`,
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (response) {
            const imageUrl = response.imageUrl || response;
            const row = $(`tr[data-id="${regionId}"]`);
            if (row.length) {
                row.find('td').eq(4).html(`<a href="${imageUrl}" target="_blank">📷 이미지 보기</a>`);
            }
            alert('이미지가 업로드되었습니다!');
        },
        error: function (xhr) {
            console.error(xhr);
            alert(xhr.responseJSON?.message || '이미지 업로드에 실패했습니다.');
        }
    });
}



