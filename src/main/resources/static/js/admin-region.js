let regionMap = new Map();
let nameToIdMap = new Map();
let levelMap = new Map();

let table = null;
let tbody = null;

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
            row.children[4].innerText = region.imageUrl ? "Embed" : '-';
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
                    <td>${region.imageUrl ? "Embed" : '-'}</td>
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
        }
    }
});

document.body.addEventListener('click', event => {
    if (event.target.id === 'saveRegionButton') {
        console.log('저장 버튼 클릭됨');
        const regionId = Number(document.getElementById('regionId').value);
        if (regionMap.has(regionId)) {
            const region = regionMap.get(regionId);
            region.name = document.getElementById('regionName').value;
            region.latitude = document.getElementById('regionLatitude').value;
            region.longitude = document.getElementById('regionLongitude').value;
            region.radius = document.getElementById('regionRadius').value;
            region.imageUrl = document.getElementById('regionImage').value;

            // 테이블 업데이트
            const row = document.querySelector(`tr[data-id="${regionId}"]`);
            if (row) {
                row.children[0].innerHTML = `<div class="name-container"><span>${region.name}</span></div>`;
                row.children[1].innerText = region.latitude || '-';
                row.children[2].innerText = region.longitude || '-';
                row.children[3].innerText = region.radius || '-';
                row.children[4].innerText = region.imageUrl ? "Embed" : '-';
            }

            alert('수정되었습니다!');
        }
    }
});

// 파일이 선택되었을 때 미리보기 표시 및 자동 업로드 실행
document.getElementById('imageUpload').addEventListener('change', event => {
    const file = event.target.files[0];
    if (file && file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = e => {
            document.getElementById('imagePreview').src = e.target.result;
            document.getElementById('imagePreview').classList.remove('d-none');

            // 파일이 정상적으로 선택되었으면 바로 업로드 함수 실행
            uploadImage(e.target.result);
        };
        reader.readAsDataURL(file);
    } else {
        alert('이미지 파일만 선택할 수 있습니다.');
        document.getElementById('imageUpload').value = ''; // 선택된 파일 초기화
    }
});

// 이미지 업로드 처리 함수
function uploadImage(imageSrc) {
    const regionId = Number(document.getElementById('regionId').value);

    if (regionMap.has(regionId)) {
        const region = regionMap.get(regionId);
        region.imageUrl = imageSrc;

        // 테이블 업데이트
        const row = document.querySelector(`tr[data-id="${regionId}"]`);
        if (row) {
            row.children[4].innerText = "Embed";
        }

        alert('이미지가 업로드되었습니다!');
    } else {
        alert('지역을 선택하세요');
    }
}

