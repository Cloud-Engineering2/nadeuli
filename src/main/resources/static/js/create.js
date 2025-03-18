let travelModal;
let selectedDates = [];
let currentModalStep = 1; // 시작은 지역 선택 단계
let prevDayCount = null;

let stepRegionSelection ;
let stepDateSelection;
let stepTimeSelection ;
let modalTitle ;
let nextButton ;
let backButton ;
const regionImageMap = new Map();

let treeData = null;
let regionMap = new Map();
let locations = null;

toastr.options = {
    "closeButton": false,
    "debug": false,
    "newestOnTop": false,
    "progressBar": false,
    "positionClass": "toast-top-center",
    "preventDuplicates": false,
    "onclick": null,
    "showDuration": "300",
    "hideDuration": "1000",
    "timeOut": "1500",
    "extendedTimeOut": "1500",
    "showEasing": "swing",
    "hideEasing": "linear",
    "showMethod": "fadeIn",
    "hideMethod": "fadeOut"
}

$(document).ready(function () {
    $.ajax({
        url: '/api/regions/image-urls',
        method: 'GET',
        success: function (response) {
            // response: [{ regionId: 1, imageUrl: "..." }, ...]
            response.forEach(item => {
                regionImageMap.set(item.regionId, item.imageUrl);
            });
        },
        error: function () {
            console.error('지역 이미지 정보를 불러오는데 실패했습니다.');
        }
    });
});

document.getElementById("travelModal").addEventListener("show.bs.modal", function () {
    loadRegionTreeIfNeeded();  // ✅ 한 번만 로딩됨
});



function loadRegionTreeIfNeeded() {
    if (treeData !== null) return; // 이미 로드된 경우 생략

    $.ajax({
        url: '/api/regions/tree',
        method: 'GET',
        success: function (response) {
            treeData = response;

            // ✅ treeData 로드된 후 regionMap과 locations 초기화
            regionMap = new Map();
            let queue = [...treeData];
            while (queue.length > 0) {
                let node = queue.shift();
                let { children, ...regionData } = node;
                regionMap.set(regionData.id, regionData);
                if (children) queue.push(...children);
            }
            locations = flattenTree(treeData);
        },
        error: function () {
            console.error('지역 트리 데이터를 불러오는 데 실패했습니다.');
        }
    });
}

function flattenTree(tree) {
    let result = [];

    function traverse(nodes, parentPath = "", displayName = "", parentId = null) {
        nodes.forEach(node => {

            // (1) 기본: 부모 Path > 내 이름
            let parent_path_child_name = parentPath ? `${parentPath} > ${node.name}` : node.name;
            // (2) displayName(표시용): 역시 부모 displayName > 내 이름
            let display_name_child_name = parentPath ? `${displayName} > ${node.name}` : node.name;

            // (3) 기본 지역 push
            result.push({
                id: node.id,
                name: node.name,                         // 검색 시 사용되는 'name'
                fullName: parent_path_child_name,         // 검색 시 사용되는 경로
                displayName: display_name_child_name,     // 실제 화면에 표시될 경로
                parentId: parentId
            });

            // (4) alias(별칭)가 있으면, 별칭으로도 한 번 더 push
            if (node.alias) {
                let parent_path_child_alias = parentPath ? `${parentPath} > ${node.alias}` : node.alias;

                result.push({
                    id: node.id,
                    name: node.alias,                     // 별칭도 'name'으로 추가
                    fullName: parent_path_child_alias,    // 검색 시 사용할 alias 경로
                    displayName: display_name_child_name, // 화면에 표시되는 건 여전히 'display_name_child_name'
                    parentId: parentId
                });
            }

            // (5) 하위 children 순회
            if (node.children && node.children.length > 0) {
                // 5-1) 원래 이름 경로로 하위 지역 탐색
                traverse(node.children, parent_path_child_name, display_name_child_name, node.id);

                // 5-2) alias가 있으면, 별칭 경로로도 하위 지역 탐색
                if (node.alias) {
                    let parent_path_child_alias = parentPath ? `${parentPath} > ${node.alias}` : node.alias;
                    traverse(node.children, parent_path_child_alias, display_name_child_name, node.id);
                }
            }
        });
    }
    traverse(tree);
    result.sort((a, b) => a.fullName.localeCompare(b.fullName));
    return result;
}




// (1) 선택된 지역을 담을 배열
let selectedLocations = [];

$("#search-box").on("input", function () {
    let keyword = $(this).val().toLowerCase().trim();
    let keywords = keyword.replace(/\s+/g, " ").split(" ");

    let filtered = locations.filter(item =>
        keywords.every(kw =>
            item.fullName.toLowerCase().replace(/\s+/g, "").includes(kw) ||
            (item.name && item.name.toLowerCase().replace(/\s+/g, "").includes(kw))
        )
    );

    // 선택된 지역의 ID가 같은 모든 하위 지역도 포함하기 위해,
    // parentId를 체크(원하시는 로직에 따라 조정 가능)
    let selectedIds = new Set(filtered.map(item => item.id));
    let expandedResults = locations.filter(item =>
        selectedIds.has(item.parentId) || selectedIds.has(item.id)
    );

    // displayName 기준 중복 제거
    let seen = new Set();
    let uniqueResults = [];
    for (let item of expandedResults) {
        if (!seen.has(item.displayName)) {
            uniqueResults.push(item);
            seen.add(item.displayName);
        }
    }

    // 검색 결과 표시
    let $suggestions = $("#suggestions");
    $suggestions.empty();

    if (keyword.length > 0) {
        uniqueResults.forEach(item => {
            $suggestions.append(`<li data-id="${item.id}">${item.displayName}</li>`);
        });
        $suggestions.show();
    } else {
        $suggestions.hide();
    }
});

// 특정 ID를 가진 노드를 treeData에서 찾는 함수 (DFS)
function findNodeById(tree, id) {
    let stack = [...tree];
    while (stack.length > 0) {
        let node = stack.pop();
        if (node.id === id) return node;
        if (node.children) stack.push(...node.children);
    }
    return null;
}

// 특정 노드의 모든 하위 자식(손자 포함) ID를 가져오는 함수 (BFS)
function getAllDescendantIds(node) {
    let descendantIds = new Set();
    let queue = node.children ? [...node.children] : [];

    while (queue.length > 0) {
        let current = queue.shift();
        descendantIds.add(current.id);
        if (current.children) {
            queue.push(...current.children);
        }
    }
    return descendantIds;
}
// 특정 노드가 부모 노드인지 확인하는 함수
function hasParentSelected(nodeId) {
    let parentNode = locations.find(loc => loc.id === nodeId);
    while (parentNode && parentNode.parentId) {
        if (selectedLocations.some(loc => loc.id === parentNode.parentId)) {
            return true; // 부모가 이미 선택됨
        }
        parentNode = locations.find(loc => loc.id === parentNode.parentId);
    }
    return false;
}

// 추천목록에서 클릭 시, 선택된 지역 목록에 추가 (수정된 코드)
$("#suggestions").on("click", "li", function () {
    let selectedId = $(this).data("id");
    let selectedText = $(this).text();

    // locations 배열에서 해당 id & displayName에 맞는 항목 찾기
    let foundItem = locations.find(loc => loc.id === selectedId && loc.displayName === selectedText);
    if (!foundItem) return;

    // 부모가 이미 선택된 경우 경고 메시지 표시 후 중단
    if (hasParentSelected(foundItem.id)) {
        toastr.warning("이미 상위 지역이 선택되었습니다. <br>먼저 상위 지역을 삭제해주세요.");

        return;
    }

    // treeData에서 해당 노드 찾기
    let regionNode = findNodeById(treeData, foundItem.id);
    let childIds = new Set();

    // 해당 노드가 존재하고 children이 있으면 하위 자식 ID 가져오기
    if (regionNode && regionNode.children) {
        childIds = getAllDescendantIds(regionNode);
    }

    // selectedLocations에서 해당 자식 ID가 포함된 항목 제거
    selectedLocations = selectedLocations.filter(loc => !childIds.has(loc.id));

    // 현재 선택된 항목이 중복인지 확인하고 추가
    let alreadySelected = selectedLocations.some(loc => loc.id === foundItem.id && loc.displayName === foundItem.displayName);
    if (!alreadySelected) {
        if(selectedLocations.length >= 2){
            toastr.warning("최대 2개의 지역까지 선택 가능합니다.");
            return;
        }
        selectedLocations.push(foundItem);
    }
    console.log(selectedLocations);
    // 화면에 표시
    renderSelectedList();

    // 검색창 및 추천 목록 초기화
    $("#search-box").val("");
    $("#suggestions").hide();
});

// // 선택된 지역 목록을 다시 그리는 함수
// function renderSelectedList() {
//     let $list = $("#selectedList");
//     $list.empty();
//
//     selectedLocations.forEach(item => {
//         let parentItem = regionMap.get(item.parentId);
//         $list.append(`
//                     <li data-id="${item.id}">
//                     <span class="item-name">${item.name}</span>
//                     <span class="parent-name">${parentItem ? parentItem.name : ""}</span>
//                     </li>
//                  `);
//     });
// }
function renderSelectedList() {
    let $container = $("#selectedList");
    $container.empty();
    $container.addClass("region-card-container");

    selectedLocations.forEach((item, index) => {
        let parentItem = regionMap.get(item.parentId);

        let imageUrl = regionImageMap.get(item.id) || `https://picsum.photos/600/600`;

        let cardHTML = `
            <div class="region-card half" data-id="${item.id}" style="--bg-url: url('${imageUrl}')">
                <button class="remove-btn" data-id="${item.id}">&times;</button>
                <div class="region-info">
                    <div class="region-name">${item.name}</div>
                    <div class="region-parent">${parentItem ? parentItem.name : ''}</div>
                </div>
            </div>
        `;
        $container.append(cardHTML);
    });

    $('.region-card').each(function () {
        let url = $(this).css('--bg-url');
        $(this).get(0).style.setProperty('--bg-url', url);
        $(this).css('background-image', url); // Fallback for non-::before
    });

    if (selectedLocations.length === 0) {
        $('#name-box').attr("placeholder", "여행 이름을 입력해주세요");
    } else {
        let regionNames = selectedLocations.map(loc => loc.name);
        let lastPart = "(으)로의 여행";
        if (regionNames.length === 1) {
            // 조사 자동 처리 (으로 / 로)
            $('#name-box').attr("placeholder", `${regionNames[0]}${lastPart}`);
        } else {
            $('#name-box').attr("placeholder", `${regionNames.join(", ")}${lastPart}`);
        }
    }
}

// 카드 내부 제거 버튼 클릭 이벤트
$("#selectedList").on("click", ".remove-btn", function () {
    let removeId = $(this).data("id");
    selectedLocations = selectedLocations.filter(loc => loc.id !== removeId);
    renderSelectedList();
});


// 바깥 영역 클릭 시 자동완성 목록 숨김
$(document).on("click", function (e) {
    if (!$(e.target).closest("#searchBox, #suggestions").length) {
        $("#suggestions").hide();
    }
});


$(document).ready(function () {
    console.log("📅 캘린더 스크립트 로드됨");

    stepRegionSelection = document.getElementById("step-region-selection");
    stepDateSelection = document.getElementById("step-date-selection");
    stepTimeSelection = document.getElementById("step-time-selection");
    modalTitle = document.getElementById("modal-title");
    nextButton = document.getElementById("next-btn");
    backButton = document.getElementById("back-btn");

    nextButton.addEventListener("click", function () {
        if (currentModalStep === 1) {
            // Step 1 → Step 2 (지역 → 날짜)

            if (selectedLocations.length === 0) {
                toastr.warning("여행 지역를 선택하세요.");
                return;
            }
            console.log("📌 Step 1 → Step 2: 지역 선택 완료");

            // 지역 선택 유효성 검사 넣고 싶으면 여기에 추가
            // ex) if (!validateRegionSelection()) return;

            stepRegionSelection.style.opacity = "0";
            stepRegionSelection.style.zIndex = "1";
            stepRegionSelection.style.visibility = "hidden";

            stepDateSelection.style.zIndex = "2";
            stepDateSelection.style.visibility = "visible";
            stepDateSelection.style.opacity = "1";

            modalTitle.textContent = "여행 기간을 설정해주세요";
            backButton.style.visibility = "visible";
            currentModalStep = 2;

        } else if (currentModalStep === 2) {
            // Step 2 → Step 3 (날짜 → 시간)
            console.log("📌 Step 2 → Step 3: 날짜 선택 완료");

            if (selectedDates.length === 0) {
                toastr.warning("날짜를 선택하세요.");
                return;
            } else if (selectedDates.length > 7) {
                toastr.warning("여행 일자는 최대 7일까지 설정 가능합니다.");
                return;
            }

            if (!prevDayCount) {
                prevDayCount = initTimeSelectionUI(selectedDates.length);
            } else {
                prevDayCount = renewTimeSelectionUI(prevDayCount, selectedDates.length);
            }

            stepDateSelection.style.opacity = "0";
            stepDateSelection.style.zIndex = "1";
            stepDateSelection.style.visibility = "hidden";

            stepTimeSelection.style.zIndex = "2";
            stepTimeSelection.style.visibility = "visible";
            stepTimeSelection.style.opacity = "1";

            modalTitle.textContent = "시작 시간을 설정해주세요";
            currentModalStep = 3;
            nextButton.textContent = "완료";
        } else {
            // Step 3 → 완료 (모달 닫기)
            console.log("✅ 여행 시간 설정 완료");
            itineraryCreateSubmit();
            // travelModal.hide();
        }
    });

    backButton.addEventListener("click", function () {
        if (currentModalStep === 2) {
            // Step 2 → Step 1 (날짜 → 지역)
            stepDateSelection.style.opacity = "0";
            stepDateSelection.style.zIndex = "1";
            stepDateSelection.style.visibility = "hidden";

            stepRegionSelection.style.zIndex = "2";
            stepRegionSelection.style.visibility = "visible";
            stepRegionSelection.style.opacity = "1";

            modalTitle.textContent = "지역을 설정해주세요";
            backButton.style.visibility = "hidden";
            currentModalStep = 1;

        } else if (currentModalStep === 3) {
            // Step 3 → Step 2 (시간 → 날짜)
            stepTimeSelection.style.opacity = "0";
            stepTimeSelection.style.zIndex = "1";
            stepTimeSelection.style.visibility = "hidden";

            stepDateSelection.style.zIndex = "2";
            stepDateSelection.style.visibility = "visible";
            stepDateSelection.style.opacity = "1";

            modalTitle.textContent = "여행 기간을 설정해주세요";
            backButton.style.visibility = "visible";
            nextButton.textContent = "다음";
            currentModalStep = 2;
        }
    });


    travelModal = new bootstrap.Modal(document.getElementById("travelModal"));
    travelModal.show();
});

$('#date-range-container').dateRangePicker({
    parentEl: "modal-body",
    inline: true,
    container: '#date-range-container',
    alwaysOpen: true,
    stickyMonths: true,
    language: 'ko',
    format: 'YYYY-MM-DD',
    separator: ' ~ ',
    startOfWeek: 'sunday',
    showShortcuts: false,
    customTopBar: ' ',
    hoveringTooltip: false,
    autoClose: true
}).bind('datepicker-change', function (event, obj) {
    let tempSelectedDates = [];
    let start = moment(obj.date1);
    let end = moment(obj.date2);

    while (start <= end) {
        tempSelectedDates.push(start.format('YYYY-MM-DD'));
        start.add(1, 'days');
    }

    selectedDates = tempSelectedDates;
    console.log("✅ 선택된 날짜:", selectedDates);

    $('#selected-date')
        .text(obj.value)
        .css({opacity: 0})
        .animate({opacity: 1}, 300);
});


// 시간선택 UI 초기화
function initTimeSelectionUI(dayCounts) {

    // 1부터 dayCounts까지의 리스트 생성
    let dayList = Array.from({length: dayCounts}, (_, i) => i + 1);

    let timeSelectionHTML = "";
    dayList.forEach(index => {
        // perDayMap에서 startTime을 가져옴 (없을 경우 기본값 설정)
        let startTime = "09:00:00"; // 기본값 설정

        // HH:MM 포맷으로 변환 (TT:MM:SS → HH:MM)
        let formattedStartTime = startTime.substring(0, 5);

        timeSelectionHTML += `
            <div class="time-container mb-3">
                <span class="date-label">${index}일차</span>
                <input type="time" class="form-control time-input" id="start-${index}" value="${formattedStartTime}">
                <button id="apply-global-time" class="btn btn-secondary" style="visibility: hidden;">전체 적용</button>
            </div>
        `;
    });

    $('#time-selection-container').html(timeSelectionHTML);
    return dayCounts;
}

// 이미 초기화 되어있는 시간선택 UI를 갱신
function renewTimeSelectionUI(prevDayCounts, dayCounts) {
    if (prevDayCounts === dayCounts) return dayCounts;

    let container = $('#time-selection-container');

    if (prevDayCounts > dayCounts) {
        // 기존 요소 중 불필요한 것 제거
        for (let i = dayCounts + 1; i <= prevDayCounts; i++) {
            container.find(`#start-${i}`).parent().remove();
        }
    } else if (prevDayCounts < dayCounts) {
        // 부족한 만큼 추가
        let timeSelectionHTML = "";
        for (let i = prevDayCounts + 1; i <= dayCounts; i++) {
            timeSelectionHTML += `
                <div class="time-container mb-3">
                    <span class="date-label">${i}일차</span>
                    <input type="time" class="form-control time-input" id="start-${i}" value="09:00">
                    <button id="apply-global-time" class="btn btn-secondary" style="visibility: hidden;">전체 적용</button>
                </div>
            `;
        }
        container.append(timeSelectionHTML);
    }

    return dayCounts;
}



function generateItineraryJSON() {

    let nameInput = $('#name-box').val().trim();
    let placeholderName = $('#name-box').attr('placeholder');
    let itineraryName = nameInput !== '' ? nameInput : placeholderName;

    console.log('val:', $('#name-box').val());
    console.log('trimmed:', $('#name-box').val().trim());
    console.log('placeholder:', $('#name-box').attr('placeholder'));

    // itineraryDTO 객체 생성
    const startDateStr = moment(selectedDates[0]).format("YYYY-MM-DD") + "T00:00:00";
    const itinerary = {
        itineraryName: itineraryName,
        startDate: startDateStr,
        totalDays: selectedDates.length,
        transportationType: 1
    };

    // ItineraryPerDays 배열 생성
    const ItineraryPerDays = [
        { dayCount: 0, startTime: "00:00:00", dayOfWeek: 0 }, // 기본 첫 항목
        ...selectedDates.map((date, index) => ({
            dayCount: index + 1,
            startTime: $(`#start-${index + 1}`).val() + ":00",
            dayOfWeek: moment(date).isoWeekday()
        }))
    ];

    // 5. 선택된 지역 ID 목록
    const selectedRegionIds = selectedLocations.map(loc => loc.id);

    // 6. 최종 JSON 데이터 구성
    const itineraryJSON = {
        itinerary: itinerary,
        itineraryPerDays: ItineraryPerDays,
        selectedRegionIds: selectedRegionIds
    };

    console.log(JSON.stringify(itineraryJSON, null, 2));
    return itineraryJSON;
}

function itineraryCreateSubmit(){
    // Step 3 → 완료 (모달 닫기 + 저장 처리)
    console.log("✅ 플래너 생성 시도중 ");

    // 버튼 및 입력요소 비활성화
    nextButton.disabled = true;
    nextButton.textContent = "생성중...";
    $('#time-selection-container input').prop('disabled', true);
    $('#name-box').prop('disabled', true);
    $('.remove-btn').prop('disabled', true);
    $('#back-btn').prop('disabled', true);

    const itineraryJSON = generateItineraryJSON();

    $.ajax({
        url: "/api/itinerary/create",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify(itineraryJSON),
        beforeSend: function () {
            $('#loading-spinner').show(); // 스피너 요소가 있다면
        },
        success: function (response) {
            console.log("✅ 일정 저장 성공:", response);
            if (response && response.itinerary && response.itinerary.id) {
                window.location.href = `/itinerary/edit/${response.itinerary.id}`;
            } else {
                toastr.error("일정 저장에 성공했으나, id를 리턴하지 않았습니다. 내 일정에서 확인하세요.");
            }
        },
        error: function (xhr, status, error) {
            console.error("❌ 일정 저장 실패:", error);
            toastr.error("일정 저장에 실패했습니다. 다시 시도해주세요.");
        },
        complete: function () {
            // UI 복원
            nextButton.disabled = false;
            nextButton.textContent = "완료";
            $('#time-selection-container input').prop('disabled', false);
            $('#name-box').prop('disabled', false);
            $('.remove-btn').prop('disabled', false);
            $('#back-btn').prop('disabled', false);
            $('#loading-spinner').hide();
        }
    });
}


$('#apply-global-time').click(function () {
    let globalStart = $('#start-global').val();
    console.log("📌 [전체 적용] 시작시간:", globalStart);
    // 1부터 dayCounts까지의 리스트 생성
    let dayList = Array.from({length: selectedDates.length}, (_, i) => i + 1);
    dayList.forEach(index => {
        $(`#start-${index}`).val(globalStart);
    });
});