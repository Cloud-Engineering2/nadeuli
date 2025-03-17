// Event 전역변수
let itinerary = null;
const perDayMap = new Map();
const eventMap = new Map();
let regions = null;
const groupedByDay = {}; // 렌더링용 - perDay 별로 정렬된 event 리스트
let eventPairs = [];
let allMarkers = [];
let allPolylines = [];
let markerState = 0;
let infoWindow=null;
let isDirty = false;
let mapReady = false;
let dataReady = false;
let isPlacePageInitialLoad = false;
let savedPlaceMarker = null;
let selectedRegionLat = null;
let selectedRegionLng = null;
let selectedRegionRadius = null;
let googleRegionLat = null;
let googleRegionLng = null;
let googleRegionRadius = null;


// 모달 전역변수
let currentModalStep = 1;
const modalTitle = document.getElementById("modal-title");
const stepDateSelection = document.getElementById("step-date-selection");
const stepTimeSelection = document.getElementById("step-time-selection");
const backButton = document.getElementById("back-btn");
const nextButton = document.getElementById("next-btn");
let travelModal;
let selectedDates = [];
let prevDayCount = null;
let isMapPanelOpen = true;
//디버깅용
let isDEBUG = false;

// path에서 가져온 iid
let itineraryId = null;

// 🔄 데이터 로딩 및 초기화
//------------------------------------------

$(document).ready(function () {
    let pathSegments = window.location.pathname.split('/');
    let itineraryId = pathSegments[pathSegments.length - 1]; // 마지막 부분이 ID라고 가정


    $.ajax({
        url: `/api/itinerary/${itineraryId}`,
        method: "GET",
        dataType: "json",
        success: function (data) {
            createData(data);
            initRegionSelect();
            renderItinerary();
            initDateRangePickerModal();
            initSidebarResize();
            dataReady = true;
            tryRenderMarkerAll();
        },
        error: function (xhr, status, error) {
            console.error("Error fetching itinerary:", error);
        }
    });


});

// 일정 데이터 생성 함수
function createData(data) {

    // 일정 정보 복사
    itinerary = {...data.itinerary};
    regions = [...data.regions];
    console.log(regions,"지역 리스트")
    // 일차별 일정 복사 및 초기화
    perDayMap.clear();
    data.itineraryPerDays.forEach(dayPerDay => {
        perDayMap.set(dayPerDay.dayCount, {...dayPerDay});
        if (!groupedByDay[dayPerDay.dayCount]) {
            groupedByDay[dayPerDay.dayCount] = [];
        }
    });

    // 이벤트 데이터 변환 및 추가
    data.itineraryEvents.forEach(event => {
        const dayKey = event.dayCount;
        const baseStartTime = perDayMap.get(dayKey)?.startTime || "00:00:00"; // 해당 날짜의 시작 시간
        const baseStartMinutes = timeToMinutes(baseStartTime); // HH:MM:SS → 분 단위 변환

        let editedEvent = {...event};
        editedEvent.stayMinute = editedEvent.endMinuteSinceStartDay - editedEvent.startMinuteSinceStartDay;

        let eventHashId = addEvent(editedEvent); // 이벤트 추가 후 ID 생성

        groupedByDay[dayKey].push({
            hashId: eventHashId,
            placeDTO: event.placeDTO,
            startMinute: baseStartMinutes + editedEvent.startMinuteSinceStartDay,
            endMinute: baseStartMinutes + editedEvent.endMinuteSinceStartDay,
            movingMinute: event.movingMinuteFromPrevPlace
        });
    });

    // 각 일차별 이벤트를 시작 시간 기준으로 정렬
    Object.keys(groupedByDay).forEach(dayKey => {
        groupedByDay[dayKey].sort((a, b) => a.startMinute - b.startMinute);
    });
}

// 🏗️ ui 요소 관리
//------------------------------------------


//일정 UI 요소 생성
function renderItinerary() {
    // 🏷일정 제목 설정
    $(".schedule-header-name").text(itinerary.itineraryName);

    // 일정 기간 표시 (시작 날짜 ~ 종료 날짜)
    let startDate = new Date(itinerary.startDate);
    let endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + itinerary.totalDays - 1);

    let options = {year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short'};
    $(".schedule-header-date").text(
        `${startDate.toLocaleDateString("ko-KR", options)} ~ ${endDate.toLocaleDateString("ko-KR", options)}`
    );

    // 🚀 일정 UI 렌더링
    const scheduleContainer = $("#scheduleContainer").empty();
    console.log("groupedByDay entries:", Object.entries(groupedByDay));

    Object.keys(groupedByDay).forEach(dayKey => {
        const dayNumber = parseInt(dayKey);
        const startTime = perDayMap.get(dayNumber)?.startTime?.substring(0, 5) || "00:00";
        console.log(dayKey);
        // 📌 0일차는 장소 보관함으로 설정
        const dayColumn = $(`
            <div class='day-column ${dayNumber === 0 ? "savedPlace" : ""}' data-day-number='${dayNumber}'>
                <div class='day-header'>${dayNumber === 0 ? `장소보관함 <div class="place-toggle-button">+ 장소 추가</div> ` : `${dayKey}` + `일차 (${startTime})`}
                </div>
                <div class='event-container' id='day-${dayNumber}'></div>
            </div>
        `);

        groupedByDay[dayKey].forEach((event, index) => {

            const eventElement = createEventElement(event, index, groupedByDay[dayKey].length, dayNumber === 0);
            if (dayKey === '0') {
                eventElement.find('.event-time').detach();
            }
            dayColumn.find('.event-container').append(eventElement);
        });

        scheduleContainer.append(dayColumn);
    });

    // 초기 상태에서 각 요일 첫 번째 .travel-info 숨기기
    $(".event-container").each(function () {
        $(this).find(".event .travel-info").first().css("display", "none");
    });

    initializeSortable(); // 드래그 & 드롭 기능 활성화
}

// 이벤트 요소 생성 함수 (장소 보관함 & 일반 이벤트 공통 사용)
function createEventElement(event, index = null, totalEvents = null, isSavedPlace = false) {
    console.log("Event Object:", event);

    return $(`
                        <div class='event' data-id='${event.hashId}'>
                            <div class="event-wrapper">
                                <div class="travel-info input-inline">
                                    ${isSavedPlace ? "" : `
                                        이동 시간 <input type="number" class="travel-minute-input" value="${event.movingMinute}" min="0" step="5"> 분
                                    `}
                                </div>
                                <div class="event-content">
                                    <div class="event-order">
                                        <div class="event-order-line top ${index === 0 ? "transparent" : ""}"></div>
                                        <div class="event-order-circle">${isSavedPlace ? "X" : index + 1}</div>
                                        <div class="event-order-line bottom ${index === totalEvents - 1 ? "transparent" : ""}"></div>
                                    </div>
                                    <div class="event-main">
                                        <div class="event-left">
                                            <div class='event-title'>${event.placeDTO.placeName}</div>
                                            <div class="event-duration-container">
                                                <div class="event-duration-input-container hidden">
                                                    <div class="event-duration-input-box">
                                                        <input type="number" class="event-duration-hours" min="0" max="24" step="1"> 시간
                                                        <input type="number" class="event-duration-minutes" min="0" max="59" step="5"> 분
                                                    </div>
                                                    <div class="event-duration-buttons">
                                                        <button class="event-duration-save">✔️확인</button>
                                                        <button class="event-duration-cancel">✖ 취소</button>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="event-under-content">
                                                                   <div class='event-place-type' data-place-type='${event.placeDTO.placeType}'>${getKoreanLabel(event.placeDTO.placeType)}</div>
                                            ${isSavedPlace ? "" : `<div class='event-time'>${formatTime(event.startMinute)} ~ ${formatTime(event.endMinute)}</div>`}
                                            </div>
                                        </div>
                                        <div class="event-right">
                                        ${isSavedPlace ? `
                                            <button class="event-duplicate place-btn"><i class="fas fa-copy"></i></button>
                                            <button class="event-remove place-btn"><i class="fas fa-trash"></i> </button>` :`
                                            <button class="event-options-button">⋮</button>`}
                                            <div class="event-options hidden">
                                                <button class="event-duration">머무는 시간</button>
                                                <button class="event-duplicate">복제</button>
                                                <button class="event-remove">삭제</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `);
}


//일정 드래그 & 드롭 활성화
function initializeSortable() {
    $(".event-container").each(function () {
        createSortableInstance(this);
    });
}

//events 장소 보관함에 생성
function updateSavedPlaceUI(events) {
    const savedPlaceContainer = $("#day-0");
    if (!savedPlaceContainer.length) return;

    events.forEach(event => {
        const eventElement = createEventElement(event, null, null, true);
        savedPlaceContainer.append(eventElement);
    });

    console.log(`🗂 장소보관함(${events.length}개) 업데이트 완료`);
}

// 새로운 DayColumn 생성
function createNewDayColumn(perDayList) {
    perDayList.forEach(perDay => {
        const {dayCount, startTime = "09:00:00", endTime = "21:00:00"} = perDay;

        console.log(`📅 새로운 day-column 생성: dayCount=${dayCount}, startTime=${startTime}, endTime=${endTime}`);

        // 🚀 새로운 Column 요소 생성
        let dayColumn = $(`
            <div class='day-column'>
                <div class='day-header'>${dayCount}일차 (${startTime.substring(0, 5)})</div>
                <div class='event-container' id='day-${dayCount}'></div>
            </div>
        `);

        // 🚀 `schedule-container`에 추가
        $("#scheduleContainer").append(dayColumn);

        // 🚀 새로운 day-column에 `Sortable` 적용
        initializeSortableForColumn(`#day-${dayCount}`);

        console.log(`✅ ${dayCount}일차 Column 추가 및 Sortable 등록 완료`);
    });
}

// 순서 및 이동시간을 업데이트하는 함수
function updateEventDisplay(dayId, startIndex) {
    console.log('updateEventDisplay 호출 !');
    const container = document.getElementById(dayId);
    console.log('updateEventDisplay 체크완료 ', container);
    if (!container) return;
    const dayHeader = container.parentElement.querySelector('.day-header');


    const dayCount = parseInt(dayId.match(/\d+$/)[0]); // day-숫자 → 숫자 추출

    dayHeader.textContent = `${dayCount}일차 (${perDayMap.get(dayCount)?.startTime.substring(0, 5)})`;

    const items = container.children;
    let order = startIndex + 1; // 새로운 순서값 설정

    // 초기 시간을 가져옴 (해당 dayCount의 startTime)
    let baseStartTime = timeToMinutes(perDayMap.get(dayCount)?.startTime || "00:00:00");

    // 이전 이벤트의 종료 시간 가져오기 (이동시간 반영)
    let prevEndMinute = 0; // startIndex가 0일 경우 기본값 설정
    if (startIndex > 0) {
        const prevEventElement = items[startIndex - 1];
        const prevEventId = prevEventElement.getAttribute("data-id");
        const prevEvent = getEventById(prevEventId);

        if (prevEvent) {
            prevEndMinute = prevEvent.startMinuteSinceStartDay + prevEvent.stayMinute;
        }
    }

    // ✅ 이벤트 업데이트 루프
    for (let i = startIndex; i < items.length; i++) {
        const eventElement = items[i];
        const eventId = eventElement.getAttribute("data-id");
        const event = getEventById(eventId);

        if (!event) continue;

        // ✅ 순서 업데이트 (event-order-circle)
        const orderCircle = eventElement.querySelector(".event-order-circle");
        if (orderCircle) {
            orderCircle.textContent = order;
        }

        // ✅ 이동 시간 업데이트 (travel-info)
        const travelInput = eventElement.querySelector(".travel-minute-input");
        if (travelInput) {
            travelInput.value = event.movingMinuteFromPrevPlace ?? 0;
        }


        // ✅ 이벤트 시간 업데이트 (event-time)
        const eventTimeElement = eventElement.querySelector(".event-time");
        if (eventTimeElement) {
            let startMinute = prevEndMinute + (event.movingMinuteFromPrevPlace ?? 0); // 이동 시간 반영
            let endMinute = startMinute + event.stayMinute; // 머무는 시간 추가

            // 저장되는 값은 baseStartTime을 제외한 상대 값
            event.startMinuteSinceStartDay = startMinute;
            event.endMinuteSinceStartDay = endMinute;

            // UI에 표시할 값은 baseStartTime을 더한 절대 시간
            eventTimeElement.textContent = `${formatTime(startMinute + baseStartTime)} ~ ${formatTime(endMinute + baseStartTime)}`;

            // 다음 이벤트의 시작 시간 업데이트
            prevEndMinute = endMinute;
        }

        // ✅ 최신 이벤트 데이터 업데이트
        eventMap.set(eventId, event);

        order++; // 다음 순서 증가
    }
}

// element 에 Sortable 안전하게 추가
function initializeSortableForColumn(selector) {
    const element = document.querySelector(selector);
    if (!element) {
        console.warn(`⚠️ Sortable 적용 실패: ${selector} 찾을 수 없음`);
        return;
    }
    createSortableInstance(element);
}

// element 에 Sortable 추가
function createSortableInstance(element) {
    return new Sortable(element, {
        group: "shared",
        animation: 200,
        ghostClass: "sortable-ghost",
        dragClass: "sortable-drag",
        handle: ".event-content",
        filter: ".js-remove",
        preventOnFilter: false,
        onStart: function (evt) {
            $(".travel-info").css("visibility", "hidden");
        },
        onAdd: function (evt) {

            let newItem = $(evt.item);
            let eventId = newItem.data("id");
            let event = getEventById(eventId);

            let isPlaceSaved = evt.to.id === 'day-0';
            console.log(createEventElement);
            let eventElement = createEventElement(event, null, null, isPlaceSaved);
            newItem.replaceWith(eventElement);

            console.log("ON ADD !");
        },
        onEnd: function (evt) {

            let fromDayId = evt.from.id;
            let oldIndex = evt.oldIndex;
            let toDayId = evt.to.id;
            let newIndex = evt.newIndex;

            console.log(`[출발] ${fromDayId}, oldIndex: ${oldIndex}`);
            console.log(`[도착] ${toDayId}, newIndex: ${newIndex}`);

            let updateStartIndexFrom = null;
            let updateStartIndexTo = null;

            eventPairs.length = 0;


            if (toDayId === fromDayId) {
                console.log(`- 같은 리스트(${toDayId})에서 이동`);
                console.log(`-- 영향을 받는 인덱스`);
                if (toDayId !== 'day-0') {
                    if (oldIndex !== newIndex) {
                        let movedForward = oldIndex > newIndex;
                        console.log(movedForward ? `--- 요소가 기존보다 앞쪽으로 이동` : `--- 요소가 기존보다 뒤쪽으로 이동`);
                        updateStartIndexFrom = calculateDistanceUpdates(toDayId, oldIndex, newIndex, movedForward);

                    } else {
                        console.log(`--- 이동하지 않음`);
                    }
                }
            } else {
                if (toDayId === 'day-0') {
                    console.log(`- 장소보관함으로 이동: ${fromDayId} → 장소보관함`);
                    changeDayCount(toDayId, newIndex);
                    console.log(`-- [출발 리스트] ${fromDayId}에서 제거 후 영향`);
                    updateStartIndexFrom = calculateRemovalImpact(fromDayId, oldIndex);

                } else if (fromDayId === 'day-0') {
                    console.log(`- 다른 리스트 이동: 장소보관함 → ${toDayId}`);
                    changeDayCount(toDayId, newIndex);
                    console.log(`-- [도착 리스트] ${toDayId}에서 추가 후 영향`);
                    updateStartIndexTo = calculateInsertionImpact(toDayId, newIndex);
                } else {
                    console.log(`- 다른 리스트 이동: ${fromDayId} → ${toDayId}`);
                    changeDayCount(toDayId, newIndex);
                    console.log(`-- [출발 리스트] ${fromDayId}에서 제거 후 영향`);
                    updateStartIndexFrom = calculateRemovalImpact(fromDayId, oldIndex);
                    console.log(`-- [도착 리스트] ${toDayId}에서 추가 후 영향`);
                    updateStartIndexTo = calculateInsertionImpact(toDayId, newIndex);
                }

            }

            (async () => {
                await requestDistanceCalculationEventPairs();

                if (updateStartIndexFrom !== null) {
                    updateEventDisplay(fromDayId, updateStartIndexFrom);
                }
                if (updateStartIndexTo !== null) {
                    updateEventDisplay(toDayId, updateStartIndexTo);
                }

                $(".travel-info").css("visibility", "visible");
                clearSavedPlaceMarker();
                markerState = extractDayId(toDayId);
                renderMarkerByMarkerState();
                isDirty = true;
            })();

            if (toDayId !== 'day-0') {
                $(evt.to).find(".event .travel-info").css("display", "block");
                $(evt.to).find(".event .travel-info").first().css("display", "none");
                $(evt.to).find(".event .event-order-line").removeClass("transparent");
                $(evt.to).find(".event .event-order-line.top").first().addClass("transparent");
                $(evt.to).find(".event .event-order-line.bottom").last().addClass("transparent");
            }

            if (fromDayId !== 'day-0') {
                $(evt.from).find(".event .travel-info").css("display", "block");
                $(evt.from).find(".event .travel-info").first().css("display", "none");
                $(evt.from).find(".event .event-order-line").removeClass("transparent");
                $(evt.from).find(".event .event-order-line.top").first().addClass("transparent");
                $(evt.from).find(".event .event-order-line.bottom").last().addClass("transparent");
            }

        }

    });
}

//day-? 에서 ? 추출
function extractDayId(toDayId) {
    const match = toDayId.match(/^day-(\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
}

//사이드바 크기 조절 기능
//사이드바 크기 조절 기능 초기화
function initSidebarResize() {
    $("#resize-handle").mousedown(function (e) {
        e.preventDefault();
        $(document).mousemove(resizeSidebar);
        $(document).mouseup(stopSidebarResize);
    });
}

//마우스 이동에 따라 사이드바 너비 조절
function resizeSidebar(e) {
    let newWidth = e.pageX;
    if (newWidth >= 300 && newWidth <= 2000) {
        $("#sidebar").css("width", newWidth + "px");
        $("#resize-handle").css("left", newWidth + "px");
    }
}

//마우스 버튼을 놓으면 크기 조절 종료
function stopSidebarResize() {
    $(document).off("mousemove", resizeSidebar);
    $(document).off("mouseup", stopSidebarResize);
}

//Event 복제후 바로 밑에 추가
function cloneAndInsertBelow(originalHashId) {
    const originalEvent = getEventById(originalHashId);
    if (!originalEvent) return;

    // 1. 복제 이벤트 생성
    const clonedEvent = cloneEvent(originalEvent);
    clonedEvent.dayCount = originalEvent.dayCount;

    const container = document.getElementById(`day-${originalEvent.dayCount}`);
    if (!container) return;

    // 2. 원본 이벤트 요소 찾기 및 index 파악
    const eventElements = container.querySelectorAll('.event');
    let insertIndex = -1;
    eventElements.forEach((el, idx) => {
        if (el.getAttribute('data-id') === originalHashId) {
            insertIndex = idx;
        }
    });

    if (insertIndex === -1) return;

    // 3. 복제 이벤트 DOM 생성 및 삽입
    const clonedEventElement = createEventElement(clonedEvent, null, null, clonedEvent.dayCount === 0);

    console.log("클론 엘리먼트:", clonedEventElement);
    console.log("클론 엘리먼트[0]:", clonedEventElement[0]);


    if (insertIndex === eventElements.length - 1) {
        container.appendChild(clonedEventElement[0]); // ✅ DOM 노드로
    } else {
        container.insertBefore(clonedEventElement[0], eventElements[insertIndex + 1]); // ✅ DOM 노드로
    }

    // 4. 시간 및 순서 업데이트
    if(clonedEvent.dayCount !==0){
        updateEventDisplay(`day-${clonedEvent.dayCount}`, insertIndex + 1);
    }
}


// 🛠️ 이벤트 데이터 관리
// ---------------------------------------------------

//지정된 길이의 랜덤 해시 코드 생성
function generateHashCode(length = 10) {
    const array = new Uint8Array(length);
    window.crypto.getRandomValues(array);
    return btoa(String.fromCharCode(...array)).replace(/[^a-zA-Z0-9]/g, '').substring(0, length);
}

//중복되지 않는 고유 ID 생성
function generateUniqueId(map, length = 10) {
    let id;
    do {
        id = generateHashCode(length);
    } while (map.has(id));
    return id;
}

//새로운 이벤트를 `eventMap`에 추가하고 ID 반환
function addEvent(event) {
    const id = generateUniqueId(eventMap);
    event.hashId = id;
    eventMap.set(id, event);
    return id;
}

// 기존 이벤트를 복제하고 새로운 hashId를 부여하여 eventMap에 추가
function cloneEvent(originalEvent) {
    const clonedEvent = structuredClone(originalEvent); // 깊은 복제
    const newId = generateUniqueId(eventMap);
    clonedEvent.id = null;
    clonedEvent.hashId = newId;
    clonedEvent.movingDistanceFromPrevPlace = 0;
    clonedEvent.movingMinuteFromPrevPlace = 0;
    eventMap.set(newId, clonedEvent);
    return clonedEvent;
}


//주어진 ID로 `eventMap`에서 이벤트 조회
function getEventById(id) {
    return eventMap.get(id) || null;
}

// PerDay 삭제로 인한 장소보관함으로의 event들의 이동 함수
function moveDeletedPerDayEventsToSavedPlace(deletedPerDays) {
    console.log(`🚀 날짜 변경 감지: 삭제된 perDay -> 장소보관함 이동`);

    // 삭제된 perDay의 dayCount 리스트
    const deletedDays = new Set(deletedPerDays.map(day => day.dayCount));

    // 삭제 대상 이벤트들을 찾기
    const eventsToMove = new Map();
    eventMap.forEach((event, eventId) => {
        if (deletedDays.has(event.dayCount)) {
            event.dayCount = 0; // `day-0`으로 이동
            event.movingMinuteFromPrevPlace = 0; // 장소보관함은 이동시간 없음
            eventsToMove.set(eventId, event);
        }
    });
    console.log(eventsToMove);
    // UI 업데이트 - 장소보관함에 추가
    if (eventsToMove.size > 0) {
        updateSavedPlaceUI(eventsToMove);
    }

    // 🚀 삭제된 perDay의 Column 삭제 & `perDayMap`에서도 제거
    deletedDays.forEach(dayCount => {
        let dayId = `day-${dayCount}`;
        let dayColumn = $(`#${dayId}`).closest(".day-column");

        if (dayColumn.length) {
            console.log(`🗑️ 일정 Column 삭제: ${dayId}`);
            dayColumn.remove();
        }

        // ✅ `perDayMap`에서 삭제
        if (perDayMap.has(dayCount)) {
            perDayMap.delete(dayCount);
            console.log(`🗑️ perDayMap에서 ${dayCount} 삭제`);
        }
    });

    markerState = 0;
    renderMarkerByMarkerState();
    console.log(`✅ ${eventsToMove.size}개 이벤트 장소보관함 이동 완료, 삭제된 day-column 및 perDayMap 정리 완료`);
}

// Event의 DayCount 상태 변경 함수
function changeDayCount(toDayId, newIndex) {
    const container = document.getElementById(toDayId);
    if (!container) return;
    const items = container.children;
    const eventElement = items[newIndex];
    const eventId = eventElement.getAttribute("data-id");
    const event = getEventById(eventId);
    event.dayCount = parseInt(toDayId.match(/\d+$/)[0]);
}


// 🛠️ 일정 조작 및 거리 계산
//------------------------------------------


// 거리(시간) 계산 요청을 함수로 분리
function calculateDistanceUpdates(dayId, oldIndex, newIndex, movedForward) {
    console.log(`🔄 거리 계산 업데이트: ${dayId}, oldIndex: ${oldIndex} → newIndex: ${newIndex}`);

    const calculatedPairs = new Set();

    //중복 방지 함수
    function safeCalculateDistance(index1, index2) {
        const pairKey = `${index1}-${index2}`;
        if (!calculatedPairs.has(pairKey)) {
            calculatedPairs.add(pairKey);

            eventPairs.push(findEventPairByDayIdAndIndex(dayId, index1, index2));
            console.log(index1,index2,"인덱스추가")
        }
    }

    if (movedForward) {
        safeCalculateDistance(newIndex - 1, newIndex); // 새로 삽입된 위치의 앞쪽 영향
        safeCalculateDistance(newIndex, newIndex + 1); // 새로 삽입된 위치의 뒤쪽 영향
        safeCalculateDistance(oldIndex, oldIndex + 1); // 원래 위치의 뒤쪽 영향

    } else {
        safeCalculateDistance(newIndex - 1, newIndex); // 새로 삽입된 위치의 앞쪽 영향
        safeCalculateDistance(newIndex, newIndex + 1); // 새로 삽입된 위치의 뒤쪽 영향
        safeCalculateDistance(oldIndex - 1, oldIndex); // 원래 위치의 앞쪽 영향
    }
    return movedForward ? newIndex : oldIndex; // updateEventDisplay의 시작 인덱스를 반환
}

// 삭제 시 거리 재계산
function calculateRemovalImpact(dayId, oldIndex) {
    eventPairs.push(findEventPairByDayIdAndIndex(dayId, oldIndex - 1, oldIndex));
    return oldIndex;
}

// 추가 시 거리 재계산
function calculateInsertionImpact(dayId, newIndex) {
    eventPairs.push(findEventPairByDayIdAndIndex(dayId, newIndex - 1, newIndex));
    eventPairs.push(findEventPairByDayIdAndIndex(dayId, newIndex, newIndex + 1));
    return newIndex;
}

function findEventPairByDayIdAndIndex(dayId, index1, index2) {
    const container = document.getElementById(dayId);
    if (!container) return;

    const items = container.children;
    const itemCount = items.length;

    if (index1 < 0 || index2 < 0) {
        console.log(`⚠️ 경고: index1=${index1}, index2=${index2} (index 0의 이동 시간을 0으로 설정)`);

        // 첫 번째 요소의 movingMinuteFromPrevPlace를 0으로 설정
        const firstEventId = items[0]?.getAttribute("data-id");
        if (firstEventId) {
            const firstEvent = getEventById(firstEventId);
            if (firstEvent) {
                return [null, firstEvent];
            }
        }

    }

    // 예외 처리: 끝부분 (범위를 초과하는 경우)
    if (index1 >= itemCount || index2 >= itemCount) {
        console.warn(`❌ 유효하지 않은 인덱스: index1=${index1}, index2=${index2}`);
        return [null,null];
    }

    const eventId1 = items[index1]?.getAttribute("data-id");
    const eventId2 = items[index2]?.getAttribute("data-id");

    if (!eventId1 || !eventId2) {
        console.warn(`❌ 이벤트 ID 없음: index1=${index1}, index2=${index2}`);
        return [null,null];
    }

    const event1 = getEventById(eventId1);
    const event2 = getEventById(eventId2);

    if (event1 && event2) {
        return [event1,event2];
    }
    else {
        console.warn(`❌ 이벤트 조회 실패: eventId1=${eventId1}, eventId2=${eventId2}`);
    }
    return [null,null];
}


// 거리 계산 요청 (element 이동시)
async function requestDistanceCalculationEventPairs(travelMode = "DRIVE") {
    const requestData = [];
    const validToEvents = [];

    if(eventPairs.length === 0 )
        return;

    console.log("eventPairs",eventPairs);

    // 유효한 쌍 추출 및 기본값 처리
    eventPairs.forEach(([from, to]) => {
        if (from && to && from.placeDTO && to.placeDTO) {
            // 같은 장소라면 거리/시간 0 설정
            if (from.placeDTO.id === to.placeDTO.id) {
                to.movingDistanceFromPrevPlace = 0;
                to.movingMinuteFromPrevPlace = 0;
                return;
            }

            // 이동 거리 계산 대상이면 기본값 설정하고 push
            to.movingDistanceFromPrevPlace = 0;
            to.movingMinuteFromPrevPlace = 0;

            requestData.push({
                originLatitude: from.placeDTO.latitude,
                originLongitude: from.placeDTO.longitude,
                destinationLatitude: to.placeDTO.latitude,
                destinationLongitude: to.placeDTO.longitude,
            });
            validToEvents.push(to);
        }
    });

    if (requestData.length === 0) {
        console.log("📭 거리 계산 요청할 쌍이 없습니다.");
        return;
    }

    try {
        const response = await fetch('/api/place/routes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) throw new Error('API 요청 실패');

        const responseData = await response.json();

        responseData.forEach((route, index) => {
            const toEvent = validToEvents[index];

            toEvent.movingDistanceFromPrevPlace = route.distanceMeters || 0;
            toEvent.movingMinuteFromPrevPlace = route.duration;
            console.log(route);
            console.log(`✅ ${toEvent.placeDTO.placeName} 이동정보 적용 완료`);
        });

    } catch (error) {
        console.error("거리 계산 중 오류:", error);
    }
}




// 거리 계산 요청 (트레블 모드 변경시)
function requestDistanceCalculation(event1, event2, travelMode) {
    console.log(`🚗 거리(시간) 계산 요청: ${event1.placeDTO.placeName} → ${event2.placeDTO.placeName}`);
    console.log(event1.placeDTO.googlePlaceId);

    // 랜덤 값 생성 (예제)
    const distance = Math.floor(Math.random() * 50) + 1; // 1 ~ 50km
    const minute = 30;//Math.floor(Math.random() * 120) + 1; // 1 ~ 120분

    return {distance, minute};
}


//======================================================
// 📅 여행 기간 및 시간 설정
//======================================================

// DateRangePickerModal 초기화
function initDateRangePickerModal() {
    console.log("📅 캘린더 스크립트 로드됨");

    travelModal = new bootstrap.Modal(document.getElementById("travelModal"));

    //===============================
    //  📅 캘린더 스크립트
    //================================
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
    setDateRangePickerDate();
}

// DateRangePicker 키기전 itinerary 데이터 로드
function setDateRangePickerDate() {
    let startDate = new Date(itinerary.startDate); // ISO 형식 날짜
    let endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + itinerary.totalDays - 1); // 마지막 날짜 계산

    let startStr = formatDateToYYYYMMDD(startDate);
    let endStr = formatDateToYYYYMMDD(endDate);

    $("#date-range-container").data('dateRangePicker').setDateRange(startStr, endStr);

    let tempSelectedDates = [];
    let start = moment(startStr);
    let end = moment(endStr);

    while (start <= end) {
        tempSelectedDates.push(start.format('YYYY-MM-DD'));
        start.add(1, 'days');
    }

    selectedDates = tempSelectedDates;
    prevDayCount = itinerary.totalDays;
}

// 시간선택 UI 초기화
function initTimeSelectionUI(dayCounts) {

    // 1부터 dayCounts까지의 리스트 생성
    let dayList = Array.from({length: dayCounts}, (_, i) => i + 1);

    let timeSelectionHTML = "";
    dayList.forEach(index => {
        console.log(perDayMap);
        // perDayMap에서 startTime과 endTime을 가져옴 (없을 경우 기본값 설정)
        let startTime = perDayMap.get(index)?.startTime || "09:00:00"; // 기본값 설정
        let endTime = perDayMap.get(index)?.endTime || "23:00:00"; // 기본값 설정

        // HH:MM 포맷으로 변환 (TT:MM:SS → HH:MM)
        let formattedStartTime = startTime.substring(0, 5);
        let formattedEndTime = endTime.substring(0, 5);

        timeSelectionHTML += `
            <div class="time-container mb-3">
                <span class="date-label">${index}일차</span>
                <input type="time" class="form-control time-input" id="start-${index}" value="${formattedStartTime}">
                <input type="time" class="form-control time-input" id="end-${index}" value="${formattedEndTime}">
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
                    <input type="time" class="form-control time-input" id="end-${i}" value="23:00">
                    <button id="apply-global-time" class="btn btn-secondary" style="visibility: hidden;">전체 적용</button>
                </div>
            `;
        }
        container.append(timeSelectionHTML);
    }

    return dayCounts;
}

// 여행 기간 및 여행 시간 설정 완료시 호출 함수
function dateChangeSubmit() {
    const startDateStr = moment(selectedDates[0]).format("YYYY-MM-DD") + "T00:00:00";

    let oldTotalDays = itinerary.totalDays || 0; // 이전 totalDays 저장
    let newTotalDays = selectedDates.length; // 새롭게 선택된 totalDays

    itinerary.startDate = startDateStr;
    itinerary.totalDays = newTotalDays;

    const newPerDayMap = new Map(); // 새롭게 생성된 day 리스트 저장
    let deletedPerDayList = []; // 삭제된 day 리스트 저장

    // 1. 새롭게 생성할 perDayMap 데이터 처리
    selectedDates.forEach((date, index) => {
        const dayMoment = moment(date);
        let dayId = index + 1;
        let newStartTime = $(`#start-${dayId}`).val() + ":00";
        let newEndTime = $(`#end-${dayId}`).val() + ":00";


        let tempPerDay = {
            dayCount: dayId,
            startTime: newStartTime,
            endTime: newEndTime,
            dayOfWeek: dayMoment.isoWeekday()  // 요일 번호 (월=1, ... , 일=7)
        };

        newPerDayMap.set(dayId, {...tempPerDay});
    });


    // 2. totalDays가 줄어든 경우 → 삭제된 데이터 처리
    if (newTotalDays < oldTotalDays) {
        for (let i = newTotalDays + 1; i <= oldTotalDays; i++) {
            if (perDayMap.has(i)) {
                deletedPerDayList.push(perDayMap.get(i)); // 삭제할 일차 데이터 저장
            }
        }
        moveDeletedPerDayEventsToSavedPlace(deletedPerDayList);

        // 1 ~ newTotalDays에 대해 startTime 변경 체크 및 updateEventDisplay 실행
        for (let i = 1; i <= newTotalDays; i++) {
            let newStartTime = newPerDayMap.get(i).startTime;
            console.log('1 ~ newTotalDays');
            if (perDayMap.has(i) && perDayMap.get(i).startTime !== newStartTime) {
                perDayMap.set(i, {...newPerDayMap.get(i)});
                console.log('updateEventDisplay', newPerDayMap.get(i));
                updateEventDisplay(`day-${i}`, 0);
            } else {
                perDayMap.set(i, {...newPerDayMap.get(i)});
            }
        }
    }

    // 3. totalDays가 증가한 경우 → 새로운 column 추가
    if (newTotalDays > oldTotalDays) {
        let newAddedDays = [];
        for (let i = oldTotalDays + 1; i <= newTotalDays; i++) {
            newAddedDays.push(newPerDayMap.get(i));
            perDayMap.set(i, newPerDayMap.get(i));
        }
        createNewDayColumn(newAddedDays);

        // 1 ~ oldTotalDays에 대해 startTime 변경 체크 및 updateEventDisplay 실행
        for (let i = 1; i <= oldTotalDays; i++) {
            let newStartTime = newPerDayMap.get(i).startTime;
            if (perDayMap.has(i) && perDayMap.get(i).startTime !== newStartTime) {
                perDayMap.set(i, {...newPerDayMap.get(i)});
                updateEventDisplay(`day-${i}`, 0);
            } else {
                perDayMap.set(i, {...newPerDayMap.get(i)});
            }

        }
    }

    if (newTotalDays === oldTotalDays) {
        for (let i = 1; i <= oldTotalDays; i++) {
            let newStartTime = newPerDayMap.get(i).startTime;

            if (perDayMap.has(i) && perDayMap.get(i).startTime !== newStartTime) {
                perDayMap.set(i, {...newPerDayMap.get(i)});
                updateEventDisplay(`day-${i}`, 0);
            } else {
                perDayMap.set(i, {...newPerDayMap.get(i)});
            }

        }
    }
    isDirty = true;
    console.log("Updated perDayMap:", perDayMap);
}


// 🎛️ 모달 및 UI 조작
//------------------------------------------
nextButton.addEventListener("click", function () {
    if (currentModalStep === 1) {
        console.log("📌 [버튼 클릭] '다음' 버튼 클릭됨");

        if (selectedDates.length === 0) {
            alert("날짜를 선택하세요.");
            return;
        } else if (selectedDates.length > 7) {
            alert("여행 일자는 최대 7일까지 설정 가능합니다.");
            return;
        }

        if (!prevDayCount) {
            console.log("null ✅ 선택된 날짜:", prevDayCount, selectedDates.length);
            prevDayCount = initTimeSelectionUI(selectedDates.length);
        } else {
            console.log("renew ✅ 선택된 날짜:", prevDayCount, selectedDates.length);
            prevDayCount = renewTimeSelectionUI(prevDayCount, selectedDates.length);
        }
        // 여행 기간 → 여행 시간으로 변경
        stepDateSelection.style.opacity = "0";
        stepDateSelection.style.zIndex = "1";
        stepDateSelection.style.visibility = "hidden";


        stepTimeSelection.style.zIndex = "2";
        stepTimeSelection.style.visibility = "visible";
        stepTimeSelection.style.opacity = "1";


        modalTitle.textContent = "시작 및 종료 시간을 설정해주세요";
        backButton.style.visibility = "visible";
        currentModalStep = 2;
    } else {
        dateChangeSubmit();
        console.log("✅ 여행 시간 설정 완료");
        travelModal.hide();
    }
});
backButton.addEventListener("click", function () {
    if (currentModalStep === 2) {
        stepTimeSelection.style.opacity = "0";
        stepTimeSelection.style.zIndex = "1";
        stepTimeSelection.style.visibility = "hidden";


        stepDateSelection.style.zIndex = "2";
        stepDateSelection.style.visibility = "visible";
        stepDateSelection.style.opacity = "1";


        modalTitle.textContent = "여행 기간을 설정해주세요";
        backButton.style.visibility = "hidden";
        currentModalStep = 1;
    }
});


// 🎛️ 일정 저장 및 API 통신
//------------------------------------------

// JSON 데이터 생성 함수
function generateItineraryJson() {
    // 🎯 불필요한 필드 제거 (createdDate, modifiedDate, role 제외)
    const {createdDate, modifiedDate, role, ...filteredItinerary} = itinerary;

    // 📅 perDayMap을 배열 형태로 변환
    const itineraryPerDays = Array.from(perDayMap.values());

    // 📌 eventMap 변환 (필드 최적화)
    const itineraryEvents = Array.from(eventMap.values()).map(event => ({
        ...event,
        pid: event.placeDTO.id, // placeDTO.id를 pid로 변경
        placeDTO: undefined, // placeDTO 제거
        stayMinute: undefined // stayMinute 제거
    }));

    // 🏁 최종 JSON 반환
    return JSON.stringify({itinerary: filteredItinerary, itineraryPerDays, itineraryEvents});
}

function saveItinerary() {
    const jsonData = generateItineraryJson();

    // 1️⃣ 저장 중 로딩 모달 띄우기
    Swal.fire({
        title: '저장 중...',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    $.ajax({
        url: "/api/itinerary/update",
        method: "POST",
        contentType: "application/json",
        data: jsonData,
        success: function (response) {
            console.log("저장 성공:", response);

            if (response.createdMappings) {
                response.createdMappings.forEach(mapping => {
                    const event = getEventById(mapping.hashId);
                    if (event) {
                        event.id = mapping.eventId;
                    }
                });
            }

            isDirty = false;

            // 2️⃣ 저장 완료 모달 띄우기 (버튼 2개)
            Swal.fire({
                icon: 'success',
                title: '저장 완료!',
                text: '일정이 성공적으로 저장되었습니다.',
                showCancelButton: true,
                confirmButtonText: '일정 보기',
                cancelButtonText: '계속 수정하기',
                reverseButtons: true
            }).then((result) => {
                if (result.isConfirmed) {
                    // ✅ 일정 보기 페이지로 이동
                    window.location.href = `/itinerary/view/${itinerary.itineraryId}`; // 실제 view URL로 수정
                }
                // ❌ 취소 선택 → 아무 것도 안 함 (계속 수정)
            });
        },
        error: function (xhr, status, error) {
            console.error("저장 실패:", error);
            Swal.fire({
                icon: 'error',
                title: '저장 실패',
                text: '저장 중 오류가 발생했습니다. 다시 시도해 주세요.'
            });
        }
    });
}


// 🎨 기타 유틸리티 함수
//------------------------------------------

//날짜 포맷 변환
function formatDateToYYYYMMDD(date) {
    let year = date.getFullYear();
    let month = String(date.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1
    let day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// HH:MM:SS 문자열을 분(minute)으로 변환하는 함수
function timeToMinutes(timeStr) {
    const [hours, minutes] = timeStr.split(':').map(Number);
    return hours * 60 + minutes;
}

// 분(minute) 값을 HH:MM 형태로 변환하는 함수
function formatTime(minutes) {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
}


//  🎭 이벤트 핸들링
//------------------------------------------

$(".save-button").click(saveItinerary);

$('#apply-global-time').click(function () {
    let globalStart = $('#start-global').val();
    let globalEnd = $('#end-global').val();
    console.log("📌 [전체 적용] 시작시간:", globalStart, "종료시간:", globalEnd);
    // 1부터 dayCounts까지의 리스트 생성
    let dayList = Array.from({length: selectedDates.length}, (_, i) => i + 1);
    dayList.forEach(index => {
        $(`#start-${index}`).val(globalStart);
        $(`#end-${index}`).val(globalEnd);
    });
});

$(document).on("click", ".event-date-change", function () {
    setDateRangePickerDate();
    initTimeSelectionUI(prevDayCount);
    travelModal.show();
});
// event 메뉴 열기
$(document).on("click", ".event-options-button", function (event) {
    event.stopPropagation(); // 클릭 이벤트 전파 방지
    let menu = $(this).siblings(".event-options");

    // 다른 열린 메뉴 닫기
    $(".event-options").not(menu).addClass("hidden");

    // 현재 메뉴 토글
    menu.toggleClass("hidden");
});
// 다른 곳을 클릭하면 메뉴 닫기
$(document).on("click", function () {
    $(".event-options").addClass("hidden");
});
// 메뉴 내부 클릭 시 닫히지 않도록 처리
$(document).on("click", ".event-options", function (event) {
    event.stopPropagation();
});

$(document).on("click", ".event-remove", function () {
    const eventElement = $(this).closest(".event");
    const eventId = eventElement.data("id");
    const eventContainer = eventElement.closest(".event-container");
    const dayId = eventContainer.attr("id");

    if (!eventId || !dayId) return;

    // 삭제할 이벤트의 인덱스 찾기
    const items = eventContainer.children();
    const index = items.index(eventElement);

    console.log(`🗑️ 삭제: dayId=${dayId}, eventId=${eventId}, index=${index}`);

    // 삭제
    eventElement.remove();
    eventMap.delete(eventId);

    eventPairs.length = 0;
    // 거리(시간) 재계산
    if (dayId !== "day-0") {
        const updateStartIndex = calculateRemovalImpact(dayId, index);
        (async () => {
            await requestDistanceCalculationEventPairs();

            updateEventDisplay(dayId, updateStartIndex);

            // ✅ 삭제 후 첫 번째 `.travel-info` 숨기기
            $(eventContainer).find(".event .travel-info").css("display", "block");
            $(eventContainer).find(".event .travel-info").first().css("display", "none");

            // ✅ 연결선 업데이트
            $(eventContainer).find(".event .event-order-line").removeClass("transparent");
            $(eventContainer).find(".event .event-order-line.top").first().addClass("transparent");
            $(eventContainer).find(".event .event-order-line.bottom").last().addClass("transparent");

            markerState = extractDayId(dayId);
            renderMarkerByMarkerState();
        })();


    }
    isDirty = true;
});


// "머무는 시간" 버튼 클릭 시 드래그 비활성화 & 입력 UI 표시
$(document).on("click", ".event-duration", function (event) {
    event.stopPropagation();

    $(".event-options").addClass("hidden");

    let eventElement = $(this).closest(".event");
    let inputContainer = eventElement.find(".event-duration-input-container");
    let eventTime = eventElement.find(".event-time");

    let eventId = eventElement.data("id");
    let eventData = eventMap.get(eventId);

    if (!eventData) return;

    // ⏳ 초기값 설정 (stayMinute -> 시/분 변환)
    let hours = Math.floor(eventData.stayMinute / 60);
    let minutes = eventData.stayMinute % 60;

    inputContainer.find(".event-duration-hours").val(hours);
    inputContainer.find(".event-duration-minutes").val(minutes);

    // 📌 드래그 비활성화
    eventElement.addClass("js-remove");

    // 📌 기존 입력창 닫고 현재 입력창만 표시
    $(".event-duration-input-container").not(inputContainer).addClass("hidden");
    inputContainer.removeClass("hidden");

    // 📌 event-time 숨기기
    eventTime.css("visibility", "hidden");
});

// "복제" 버튼 클릭 시
$(document).on("click", ".event-duplicate", function (event) {
    event.stopPropagation();

    $(".event-options").addClass("hidden");

    let eventElement = $(this).closest(".event");
    let eventId = eventElement.data("id");
    let eventData = eventMap.get(eventId);

    if (!eventData) return;

    cloneAndInsertBelow(eventId);
    isDirty = true;
});



// "✔️ 확인" 버튼 클릭 시 값 저장 & 드래그 다시 활성화
$(document).on("click", ".event-duration-save", function (event) {
    event.stopPropagation();

    let inputContainer = $(this).closest(".event-duration-input-container");
    let eventElement = $(this).closest(".event");
    let eventTime = eventElement.find(".event-time");

    let eventId = eventElement.data("id");
    let eventData = eventMap.get(eventId);

    if (!eventData) return;

    // 입력값 가져오기
    let hours = parseInt(inputContainer.find(".event-duration-hours").val(), 10) || 0;
    let minutes = parseInt(inputContainer.find(".event-duration-minutes").val(), 10) || 0;
    let totalMinutes = hours * 60 + minutes;

    // ⏳ 값 저장
    eventData.stayMinute = totalMinutes;
    isDirty = true;
    // 📌 UI 업데이트
    updateEventDisplay(`day-${eventData.dayCount}`, 0);

    // 📌 event-time 다시 표시
    eventTime.css("visibility", "visible");

    // 📌 드래그 다시 활성화
    eventElement.removeClass("js-remove");

    // 📌 입력창 숨기기
    inputContainer.addClass("hidden");


});

//  "✖ 취소" 버튼 클릭 시 입력창 닫기 & 드래그 다시 활성화
$(document).on("click", ".event-duration-cancel", function (event) {
    event.stopPropagation();

    let inputContainer = $(this).closest(".event-duration-input-container");
    let eventElement = $(this).closest(".event");
    let eventTime = eventElement.find(".event-time");

    // 📌 드래그 다시 활성화
    eventElement.removeClass("js-remove");

    // 📌 event-time 다시 표시
    eventTime.css("visibility", "visible");


    // 📌 입력창 숨기기
    inputContainer.addClass("hidden");
});


// 인풋시간 변경시
$(document).on("change", ".travel-minute-input", function () {
    const newValue = parseInt($(this).val(), 10) || 0;
    const eventElement = $(this).closest(".event");
    const eventId = eventElement.data("id");
    const eventData = getEventById(eventId);
    if (!eventData) return;

    eventData.movingMinuteFromPrevPlace = newValue;
    isDirty = true;

    updateEventDisplay(`day-${eventData.dayCount}`, 0); // 전체 시간 재계산
});


// 장소추가 관련 코드
// =================================================================

let map;
let sideMap;
let markers = [];
const placeTypeTranslations ={"car_dealer":"자동차 딜러","car_rental":"렌터카","car_repair":"자동차 정비소","car_wash":"세차장","electric_vehicle_charging_station":"전기차 충전소","gas_station":"주유소","parking":"주차장","rest_stop":"휴게소","corporate_office":"기업 사무실","farm":"농장","ranch":"목장","art_gallery":"미술관","art_studio":"예술 작업실","auditorium":"강당","cultural_landmark":"문화 랜드마크","historical_place":"유적지","monument":"기념비","museum":"박물관","performing_arts_theater":"공연 예술 극장","sculpture":"조각상","library":"도서관","preschool":"유치원","primary_school":"초등학교","school":"학교","secondary_school":"중·고등학교","university":"대학교","adventure_sports_center":"익스트림 스포츠 센터","amphitheatre":"원형 극장","amusement_center":"오락 센터","amusement_park":"놀이공원","aquarium":"수족관","banquet_hall":"연회장","barbecue_area":"바베큐 구역","botanical_garden":"식물원","bowling_alley":"볼링장","casino":"카지노","childrens_camp":"어린이 캠프","comedy_club":"코미디 클럽","community_center":"커뮤니티 센터","concert_hall":"콘서트 홀","convention_center":"컨벤션 센터","cultural_center":"문화 센터","cycling_park":"자전거 공원","dance_hall":"댄스홀","dog_park":"애견 공원","event_venue":"이벤트 장소","ferris_wheel":"대관람차","garden":"정원","hiking_area":"등산로","historical_landmark":"역사적 랜드마크","internet_cafe":"인터넷 카페","karaoke":"노래방","marina":"마리나 (항구)","movie_rental":"비디오 대여점","movie_theater":"영화관","national_park":"국립공원","night_club":"나이트클럽","observation_deck":"전망대","off_roading_area":"오프로드 지역","opera_house":"오페라 하우스","park":"공원","philharmonic_hall":"필하모닉 홀","picnic_ground":"소풍 장소","planetarium":"천문관","plaza":"광장","roller_coaster":"롤러코스터","skateboard_park":"스케이트 공원","state_park":"주립공원","tourist_attraction":"관광명소","video_arcade":"비디오 아케이드","visitor_center":"방문자 센터","water_park":"워터파크","wedding_venue":"웨딩홀","wildlife_park":"야생동물 공원","wildlife_refuge":"야생동물 보호구역","zoo":"동물원","public_bath":"대중목욕탕","public_bathroom":"공중화장실","stable":"마구간","accounting":"회계 사무소","atm":"ATM","bank":"은행","acai_shop":"아사이 볼 전문점","afghani_restaurant":"아프가니스탄 음식점","african_restaurant":"아프리카 음식점","american_restaurant":"아메리칸 레스토랑","asian_restaurant":"아시안 레스토랑","bagel_shop":"베이글 가게","bakery":"베이커리","bar":"바","bar_and_grill":"바 & 그릴","barbecue_restaurant":"바베큐 레스토랑","brazilian_restaurant":"브라질 음식점","breakfast_restaurant":"조식 전문점","brunch_restaurant":"브런치 레스토랑","buffet_restaurant":"뷔페 레스토랑","cafe":"카페","cafeteria":"구내식당","candy_store":"캔디샵","cat_cafe":"고양이 카페","chinese_restaurant":"중식당","chocolate_factory":"초콜릿 공장","chocolate_shop":"초콜릿 가게","coffee_shop":"커피숍","confectionery":"과자점","deli":"델리","dessert_restaurant":"디저트 레스토랑","dessert_shop":"디저트 가게","diner":"다이너","dog_cafe":"강아지 카페","donut_shop":"도넛 가게","fast_food_restaurant":"패스트푸드점","fine_dining_restaurant":"파인다이닝 레스토랑","food_court":"푸드코트","french_restaurant":"프랑스 음식점","greek_restaurant":"그리스 음식점","hamburger_restaurant":"햄버거 가게","ice_cream_shop":"아이스크림 가게","indian_restaurant":"인도 음식점","indonesian_restaurant":"인도네시아 음식점","italian_restaurant":"이탈리아 음식점","japanese_restaurant":"일식당","juice_shop":"주스 전문점","korean_restaurant":"한식당","lebanese_restaurant":"레바논 음식점","meal_delivery":"배달 전문점","meal_takeaway":"테이크아웃 전문점","mediterranean_restaurant":"지중해 음식점","mexican_restaurant":"멕시코 음식점","middle_eastern_restaurant":"중동 음식점","pizza_restaurant":"피자 가게","pub":"펍","ramen_restaurant":"라멘 전문점","restaurant":"레스토랑","sandwich_shop":"샌드위치 가게","seafood_restaurant":"해산물 레스토랑","spanish_restaurant":"스페인 음식점","steak_house":"스테이크 하우스","sushi_restaurant":"스시 레스토랑","tea_house":"찻집","thai_restaurant":"태국 음식점","turkish_restaurant":"터키 음식점","vegan_restaurant":"비건 레스토랑","vegetarian_restaurant":"채식 레스토랑","vietnamese_restaurant":"베트남 음식점","wine_bar":"와인 바","administrative_area_level_1":"광역 행정구역","administrative_area_level_2":"지방 행정구역","country":"국가","locality":"지역","postal_code":"우편번호","school_district":"학군","city_hall":"시청","courthouse":"법원","embassy":"대사관","fire_station":"소방서","government_office":"정부 기관","local_government_office":"지방 정부 기관","neighborhood_police_station":"지구대 (일본만 해당)","police":"경찰서","post_office":"우체국","chiropractor":"카이로프랙틱","dental_clinic":"치과 클리닉","dentist":"치과 의사","doctor":"의사","drugstore":"약국","hospital":"병원","massage":"마사지샵","medical_lab":"의료 실험실","pharmacy":"약국","physiotherapist":"물리 치료사","sauna":"사우나","skin_care_clinic":"피부 관리 클리닉","spa":"스파","tanning_studio":"태닝 스튜디오","wellness_center":"웰니스 센터","yoga_studio":"요가 스튜디오","apartment_building":"아파트 건물","apartment_complex":"아파트 단지","condominium_complex":"콘도미니엄 단지","housing_complex":"주택 단지","bed_and_breakfast":"B&B 숙소","budget_japanese_inn":"일본 저가 숙소","campground":"캠핑장","camping_cabin":"캠핑용 오두막","cottage":"코티지","extended_stay_hotel":"장기 체류 호텔","farmstay":"팜스테이","guest_house":"게스트하우스","hostel":"호스텔","hotel":"호텔","inn":"여관","japanese_inn":"료칸","lodging":"숙박시설","mobile_home_park":"이동식 주택 단지","motel":"모텔","private_guest_room":"개인 게스트룸","resort_hotel":"리조트 호텔","rv_park":"RV 주차장","beach":"해변","church":"교회","hindu_temple":"힌두교 사원","mosque":"모스크","synagogue":"유대교 회당","astrologer":"점성술사","barber_shop":"이발소","beautician":"미용 전문가","beauty_salon":"미용실","body_art_service":"바디아트 서비스","catering_service":"출장 요리 서비스","cemetery":"공동묘지","child_care_agency":"보육 기관","consultant":"컨설팅 서비스","courier_service":"택배 서비스","electrician":"전기 기사","florist":"꽃집","food_delivery":"음식 배달 서비스","foot_care":"발 관리 서비스","funeral_home":"장례식장","hair_care":"헤어 관리","hair_salon":"미용실","insurance_agency":"보험 대리점","laundry":"세탁소","lawyer":"변호사","locksmith":"열쇠 수리점","makeup_artist":"메이크업 아티스트","moving_company":"이사 업체","nail_salon":"네일숍","painter":"도장업체","plumber":"배관공","psychic":"심령술사","real_estate_agency":"부동산 중개업","roofing_contractor":"지붕 공사업체","storage":"창고","summer_camp_organizer":"여름 캠프 기획사","tailor":"재단사","telecommunications_service_provider":"통신 서비스 제공업체","tour_agency":"여행사","tourist_information_center":"관광 안내소","travel_agency":"여행사","veterinary_care":"동물 병원","asian_grocery_store":"아시아 식료품점","auto_parts_store":"자동차 부품 상점","bicycle_store":"자전거 가게","book_store":"서점","butcher_shop":"정육점","cell_phone_store":"휴대폰 매장","clothing_store":"의류 매장","convenience_store":"편의점","department_store":"백화점","discount_store":"할인 매장","electronics_store":"전자제품 매장","food_store":"식료품점","furniture_store":"가구 매장","gift_shop":"기념품 가게","grocery_store":"슈퍼마켓","hardware_store":"철물점","home_goods_store":"생활용품 매장","home_improvement_store":"DIY/인테리어 매장","jewelry_store":"보석 가게","liquor_store":"주류 판매점","market":"시장","pet_store":"애완동물 가게","shoe_store":"신발 가게","shopping_mall":"쇼핑몰","sporting_goods_store":"스포츠 용품점","store":"상점","supermarket":"대형 마트","warehouse_store":"창고형 매장","wholesaler":"도매점","arena":"경기장","athletic_field":"운동장","fishing_charter":"낚시 여행","fishing_pond":"낚시터","fitness_center":"헬스장","golf_course":"골프장","gym":"체육관","ice_skating_rink":"아이스링크","playground":"놀이터","ski_resort":"스키 리조트","sports_activity_location":"스포츠 활동 장소","sports_club":"스포츠 클럽","sports_coaching":"스포츠 코칭 센터","sports_complex":"스포츠 복합 시설","stadium":"스타디움","swimming_pool":"수영장","airport":"공항","airstrip":"소형 비행장","bus_station":"버스 터미널","bus_stop":"버스 정류장","ferry_terminal":"페리 터미널","heliport":"헬리포트","international_airport":"국제공항","light_rail_station":"경전철 역","park_and_ride":"환승 주차장","subway_station":"지하철역","taxi_stand":"택시 승강장","train_station":"기차역","transit_depot":"교통 환승센터","transit_station":"대중교통 환승역","truck_stop":"트럭 정류장","administrative_area_level_3":"행정구역 (레벨 3)","administrative_area_level_4":"행정구역 (레벨 4)","administrative_area_level_5":"행정구역 (레벨 5)","administrative_area_level_6":"행정구역 (레벨 6)","administrative_area_level_7":"행정구역 (레벨 7)","archipelago":"군도","colloquial_area":"비공식 지역명","continent":"대륙","establishment":"시설","finance":"금융","floor":"층","food":"음식","general_contractor":"종합 건설업체","geocode":"지리적 코드","health":"건강","intersection":"교차로","landmark":"랜드마크","natural_feature":"자연 지형","neighborhood":"주변 지역","place_of_worship":"예배 장소","plus_code":"플러스 코드","point_of_interest":"관심 지점","political":"정치적 구역","post_box":"우편함","postal_code_prefix":"우편번호 접두사","postal_code_suffix":"우편번호 접미사","postal_town":"우편도시","premise":"건물","room":"방","route":"경로","street_address":"도로명 주소","street_number":"도로명 주소 번호","sublocality":"하위 지역","sublocality_level_1":"하위 지역 (레벨 1)","sublocality_level_2":"하위 지역 (레벨 2)","sublocality_level_3":"하위 지역 (레벨 3)","sublocality_level_4":"하위 지역 (레벨 4)","sublocality_level_5":"하위 지역 (레벨 5)","subpremise":"건물 내 구역","town_square":"타운 스퀘어"};
const excludedPlaceTypes = ["administrative_area_level_1", "administrative_area_level_2", "administrative_area_level_3", "administrative_area_level_4", "administrative_area_level_5", "administrative_area_level_6", "administrative_area_level_7", "colloquial_area", "continent", "country", "locality", "neighborhood", "political", "postal_code", "postal_code_prefix", "postal_code_suffix", "postal_town", "school_district", "sublocality", "sublocality_level_1", "sublocality_level_2", "sublocality_level_3", "sublocality_level_4", "sublocality_level_5", "plus_code", "establishment", "floor", "premise", "subpremise", "room", "street_address", "street_number", "intersection", "route", "corporate_office", "general_contractor", "real_estate_agency", "insurance_agency", "lawyer", "accounting", "finance", "storage", "telecommunications_service_provider", "moving_company", "electrician", "plumber", "roofing_contractor", "courier_service", "warehouse_store", "wholesaler", "auto_parts_store", "butcher_shop", "beauty_salon", "nail_salon", "hair_salon", "barber_shop", "tanning_studio", "makeup_artist", "foot_care", "psychic", "astrologer", "apartment_building", "apartment_complex", "condominium_complex", "housing_complex", "mobile_home_park", "church", "hindu_temple", "mosque", "synagogue", "place_of_worship", "chiropractor", "physiotherapist", "skin_care_clinic", "medical_lab", "wellness_center", "child_care_agency", "summer_camp_organizer", "consultant", "painter", "tailor", "point_of_interest"]

//
// document.addEventListener("DOMContentLoaded", function () {
//     loadGoogleMapsApi();
// });


let cursorScore = null;
let cursorId = null;
let isLastPage = false;
let isSearchTriggered = false;
let activeSearchQuery = "";

let placeMap = new Map(); // <placeId, placeData>

const nadeuliScrollArea = document.getElementById("nadeuli-search-results");

nadeuliScrollArea.addEventListener("scroll", () => {
    if (nadeuliScrollArea.scrollTop + nadeuliScrollArea.clientHeight >= nadeuliScrollArea.scrollHeight - 100) {
        fetchRecommendedPlaces(10); // 원하는 pageSize
    }
});


// ➡️ 나들이 장소 검색 관련
//===============================================================================

// ✅ 현재 선택된 placeTypes 저장 배열
let selectedPlaceTypes = [];

// ✅ 필터 버튼 클릭 이벤트 바인딩
document.querySelectorAll(".filter-button").forEach(button => {
    button.addEventListener("click", () => {
        button.classList.toggle("active"); // 색상 강조

        // 선택된 필터 타입만 다시 수집
        selectedPlaceTypes = Array.from(document.querySelectorAll(".filter-button.active"))
            .map(btn => btn.getAttribute("data-filter"));

        // 👉 필터 변경되면 기존 목록 초기화하고 다시 추천 요청
        resetRecommendationAndFetch();
    });
});

// ✅ 기존 추천 결과 초기화 + 새 검색 수행
function resetRecommendationAndFetch() {
    // 커서 및 Map 초기화
    cursorScore = null;
    cursorId = null;
    isLastPage = false;
    placeMap.clear();

    // 목록 초기화
    document.getElementById("nadeuli-search-results").innerHTML = "";

    // 검색 재요청
    fetchRecommendedPlaces(10);
}

// 검색 함수
function fetchRecommendedPlaces(pageSize = 10) {
    if (isLastPage) return; // 더 이상 가져올 데이터 없음

    const searchEnabled = isSearchTriggered && activeSearchQuery.length > 0;
    const searchQuery = activeSearchQuery; // ✅ 고정된 검색어만 서버로 보냄

    const requestData = {
        userLng: selectedRegionLng ?? 126.49,
        userLat: selectedRegionLat ?? 33.44,
        radius: selectedRegionRadius ?? 100000,
        pageSize: pageSize,
        cursorScore: cursorScore,
        cursorId: cursorId,
        placeTypes: selectedPlaceTypes,
        searchEnabled: searchEnabled,
        searchQuery: searchQuery
    };

    $.ajax({
        url: "/api/place/recommend",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify(requestData),
        success: function (response) {
            const places = response.places || [];

            // 중복 제거 및 Map에 추가
            places.forEach(place => {
                if (!placeMap.has(place.id)) {
                    placeMap.set(place.id, place);
                }
            });

            // 커서 갱신
            cursorScore = response.nextCursorScore;
            cursorId = response.nextCursorId;

            // 마지막 페이지 여부 판단
            if (places.length < pageSize) {
                isLastPage = true;
            }

            // 출력 함수 호출
            renderRecommendedPlaces([...placeMap.values()]);
        },
        error: function (err) {
            console.error("추천 장소 호출 실패:", err);
        }
    });
}

//필터 타입 맵핑
function getKoreanLabel(filterType) {
    const filterMap = {
        LANDMARK: "명소",
        RESTAURANT: "식당",
        LODGING: "숙소",
        CAFE: "카페",
        TRANSPORTATION: "교통",
        ATTRACTION: "어트랙션",
        CONVENIENCE: "편의시설"
    };

    return filterMap[filterType] || "알 수 없음";
}


// 검색후 렌더링 함수
function renderRecommendedPlaces(placeList) {
    const container = document.getElementById("nadeuli-search-results");
    container.innerHTML = ""; // 초기화 (추가로 붙이려면 append 방식으로 바꾸세요)
    console.log(placeList);
    placeList.forEach(place => {
        const listItem = document.createElement("div");
        listItem.className = "list-item";
        listItem.setAttribute("data-id", place.id);
        listItem.innerHTML = `
            <img src="${place.imageUrl || ''}" alt="장소 이미지" />
            <div class="info">
                <div class="title">${place.placeName}</div>
                <div class="info-line">
                    <div class="place-type" data-place-type='${place.placeType}'>${getKoreanLabel(place.placeType)}</div>
                    <div class="address">${place.address}</div>
                </div>
                <div class="info-line">
                    <div class="like"><i class="bi bi-heart"></i> ${place.googleRatingCount || 0}</div>
                    <div class="star"><i class="bi bi-star-fill"></i> ${place.googleRating || 0}</div>
                </div>
            </div>
            <div class="add-button-wrap">
                <button class="add-button" data-id="${place.id}">+</button>
            </div>
        `;
        container.appendChild(listItem);
    });
}

// 검색어를 갱신하고 resetRecommendationAndFetch 호출
function textSearchNadeuliPlaces() {
    const inputVal = document.getElementById("nadeuli-place-search").value.trim();
    if (!inputVal) return;

    isSearchTriggered = true;
    activeSearchQuery = inputVal;
    resetRecommendationAndFetch();
}

// enter키 감지시 textSearchNadeuliPlaces 호출
function handleNadeuliKeyPress(event) {
    if (event.key === "Enter") {
        event.preventDefault();
        textSearchNadeuliPlaces();
    }
}

//Search 초기화
function clearNadeuliSearch() {
    document.getElementById("nadeuli-place-search").value = "";
    isSearchTriggered = false;
    activeSearchQuery = "";
    resetRecommendationAndFetch();
}


// ➡️ 구글 Place TextSearch 관련
//===============================================================================

// 구글 Place Text Search
function searchGooglePlaces() {
    let query = document.getElementById("google-place-search").value;
    if (!query) return;

    let location = {
        lat: googleRegionLat ?? 33.4996,
        lng: googleRegionLng ?? 126.5312
    };
    let radius = googleRegionRadius ?? 50000;

    $.ajax({
        url: "/api/google-places/search",
        type: "GET",
        data: {
            query: query,
            lat: location.lat,
            lng: location.lng,
            radius: radius
        },
        success: function (data) {
            try {
                let parsedData = typeof data === "string" ? JSON.parse(data) : data; // JSON 문자열인지 확인 후 변환
                let results = parsedData.places || []; // `places` 키에서 데이터 가져오기
                clearGoogleMarkers();
                displayGoogleSearchResults(results);
            } catch (error) {
                console.error("JSON parsing error:", error);
            }
        },
        error: function (error) {
            console.error("Error fetching data from backend:", error);
        }
    });
}

// 구글 Place Text Search 결과 출력
function displayGoogleSearchResults(places) {
    let resultsContainer = document.getElementById(`google-search-results`);
    let resultsHeader = document.getElementById(`google-search-results-header`)

    resultsHeader.innerText = `검색 결과 총${places.length}건`; // 검색 개수 업데이트
    resultsContainer.innerHTML = ""; // 기존 결과 초기화
    places.forEach((place) => {
        let placeName = place.displayName?.text || "이름 없음";
        let address = place.formattedAddress || "주소 정보 없음";
        let location = new google.maps.LatLng(place.location.latitude, place.location.longitude);
        let placeId = place.id || ""; // placeId 저장

        let filteredTypes = place.types ? translatePlaceTypes(place.types) : [];
        let typesText = filteredTypes.length > 0 ? filteredTypes.join(", ") : "";

        let listItem = document.createElement("div");
        listItem.className = "google-result-item";
        listItem.setAttribute("data-id", placeId); // placeId 저장

        listItem.innerHTML = `
            <div class="google-result-content">
                <div class="google-result-title">${placeName}</div>
                <div class="google-result-address">${address}</div>
                <div class="google-result-tags">${typesText}</div>
            </div>
            <button class="btn btn-sm btn-secondary register-btn" onclick="registerPlace(this)">
                등록
            </button>
        `;

        listItem.onclick = () => selectGooglePlace(location);
        resultsContainer.appendChild(listItem);

        let marker = new google.maps.Marker({
            position: location,
            map: map,
            title: placeName,
        });
        markers.push(marker);
    });

    if (places.length > 0) {
        map.setCenter(new google.maps.LatLng(places[0].location.latitude, places[0].location.longitude));
    }
}

// 구글 Place 장소타입 한글 변환 함수
function translatePlaceTypes(types) {
    return types
        .filter(type => !excludedPlaceTypes.includes(type)) // 제외 리스트 필터링
        .map(type => placeTypeTranslations[type] || `#${type.replace(/_/g, ' ')}`); // 한글 변환 or 해시태그 스타일
}

// 구글 Place 검색창 초기화
function clearGoogleSearch() {
    document.getElementById("google-place-search").value = "";
    document.getElementById("google-search-results").innerHTML = "";
    document.getElementById("google-search-results-header").innerText = "검색 결과 총 0건";
    clearGoogleMarkers();
}

//구글맵 마커삭제
function clearGoogleMarkers() {
    markers.forEach((marker) => marker.setMap(null));
    markers = [];
}

// 구글 지역 선택시 지역이동
function selectGooglePlace(location) {
    map.setCenter(location);
    map.setZoom(15);
}

// 구글 Place -> 나들이서버 장소 추가
function registerPlace(button) {
    let listItem = button.closest(".google-result-item");
    let placeId = listItem.getAttribute("data-id"); // data-id에서 placeId 가져오기
    let name = listItem.querySelector(".google-result-title").innerText;
    let address = listItem.querySelector(".google-result-address").innerText;

    if (!placeId) {
        alert("❌ Place ID가 없습니다. 다시 시도해주세요.");
        return;
    }

    // 1️⃣ 사용자 확인
    if (!confirm(`${name} 장소를 등록하시겠습니까?`)) {
        return;
    }

    // 2️⃣ 장소 등록 API 호출
    $.ajax({
        url: "/api/place/register",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            placeId: placeId, // Google Place ID로 등록
        }),
        success: function (response) {
            // 3️⃣ 성공 처리
            if (response.status === 201) {
                alert(`✅ 장소가 성공적으로 등록되었습니다: ${name}`);
            } else if (response.status === 200) {
                alert(`⚠ 이미 등록된 장소입니다: ${name}`);
            }
        },
        error: function (error) {
            // 4️⃣ 실패 처리
            console.error("🚨 장소 등록 실패:", error);
            alert("❌ 장소 등록에 실패했습니다. 다시 시도해주세요.");
        }
    });
}

//구글 검색창에서 엔터키 누를시 검색
function handleGoogleKeyPress(event) {
    if (event.key === "Enter") {
        event.preventDefault();
        searchGooglePlaces();
    }
}


// ➡️ 기타 유틸리티
//=======================================================================

// 장소추가 탭 변경
function showTab(tabId) {
    // 모든 탭 숨기기
    document.querySelectorAll(".tab-pane").forEach(tab => {
        tab.classList.remove("active");
    });

    // 선택한 탭 활성화
    document.getElementById(tabId).classList.add("active");

    // 모든 탭 버튼에서 active 제거
    document.querySelectorAll(".tab-button").forEach(button => {
        button.classList.remove("active");
    });

    // 선택한 버튼 활성화
    if (tabId === "nadeuli-search-tab") {
        document.getElementById("nadeuli-tab-btn").classList.add("active");
    } else {
        document.getElementById("google-tab-btn").classList.add("active");
    }
}


// 지도 마커 그룹별 색상
const groupColors = ["#343434", "#fd7a2b", "#16c35d", "#00b2ff"
    , "#9b59b6", "#c63db8", "#cc3434", "#462ad3"];


// 모든 PerDayMarker 생성
function renderMarkerByMarkerState() {
    console.log("renderMarkerByMarkerState");
    clearMarkers();
    clearPolylines();

    const bounds = new google.maps.LatLngBounds();


    $('.day-column').each(function () {
        const $dayColumn = $(this);

        if ($dayColumn.hasClass('savedPlace')) return;
        const dayNumber = parseInt($dayColumn.data('day-number'));

        if(markerState !== 0 && markerState !== dayNumber)  return;


        const eventIds = $dayColumn.find('.event-container .event').map(function () {
            return $(this).data('id');
        }).get();

        renderMarker(dayNumber, eventIds, false);

        console.log(eventIds);
        // bounds에 포함시킬 좌표 계산
        eventIds.forEach(eventId => {
            const event = getEventById(eventId);
            if (event && event.placeDTO) {
                bounds.extend({ lat: event.placeDTO.latitude, lng: event.placeDTO.longitude });
                console.log("bounds.extend !");
            }
        });

        if(markerState !== 0 && markerState === dayNumber) return false;
    });

    console.log(bounds);

    // 마지막에 한 번만 지도 중심과 줌 조정
    if (!bounds.isEmpty()) {
        sideMap.fitBounds(bounds);
    }
}



function renderMarker(dayNumber, eventIds) {
    let markerData = [[]];

    let idx = 0;
    eventIds.forEach(eventId => {
        const event = getEventById(eventId);
        if (event && event.placeDTO) {
            console.log(event.placeDTO);
            markerData[0].push({
                hashId: event.hashId,
                order: idx,
                lat: event.placeDTO.latitude,
                lng: event.placeDTO.longitude,
                isLodging : event.placeDTO.placeType === "LODGING"
            });
            idx = idx + 1;
        }
    });
    console.log(markerData);
    drawMap(markerData, dayNumber);
}


// 맵에 마커 그리는 함수
function drawMap(markerData, startColorIndex = 0) {
    markerData.forEach((group, groupIndex) => {
        const sortedGroup = group.sort((a, b) => a.order - b.order);

        const colorIndex = (startColorIndex + groupIndex) % groupColors.length;
        const groupColor = groupColors[colorIndex];

        const pathCoords = [];

        // 마커 그리기
        sortedGroup.forEach((item, index) => {
            const marker = new google.maps.Marker({
                hashId: item.hashId,
                position: { lat: item.lat, lng: item.lng },
                map: sideMap,
                label: {
                    text: item.isLodging ? "H" : (index + 1).toString(),
                    fontWeight: "600",
                    fontSize: "13px",
                    color: "#ffffff"
                },
                icon: {
                    path: `
                            M 0,0 
                            m -10,-20 
                            a 10,10 0 1,0 20,0 
                            a 10,10 0 1,0 -20,0 
                            M 0,0 
                            l -7,-10 
                            l 14,0 
                            z
                        `,
                    fillColor: groupColor,
                    fillOpacity: 1,
                    strokeColor: "#ffffff",
                    strokeWeight: 0.5,
                    scale: 1,
                    labelOrigin: new google.maps.Point(0, -20)
                }
            });



            marker.addListener("click", function() {
                clickMarker(marker);
            });


            allMarkers.push(marker);
            pathCoords.push({ lat: item.lat, lng: item.lng });
        });

        // 경로(polyline) 그리기
        const polyline = new google.maps.Polyline({
            path: pathCoords,
            map: sideMap,
            strokeOpacity: 0,
            icons: [{
                icon: {
                    path: 'M 0,-1 0,1',
                    strokeOpacity: 1,
                    scale: 4,
                    strokeColor: groupColor
                },
                offset: '0',
                repeat: '20px'
            }]
        });

        allPolylines.push(polyline);
    });
}

//마커 z-index 초기화
function resetAllMarkersZIndex(markers, defaultZIndex = 1) {
    markers.forEach(m => m.setZIndex(defaultZIndex));
}


//마커 크기를 키우는 함수
function enlargeMarkerTemporarily(marker, scaleFactor = 2, duration = 2000) {
    const originalIcon = marker.getIcon();
    const originalLabel = marker.getLabel();

    // 기존 타이머 있으면 클리어
    if (marker._resetTimerId) {
        clearTimeout(marker._resetTimerId);
        marker._resetTimerId = null;
    }

    // 아이콘 확대
    const biggerIcon = {
        ...originalIcon,
        scale: (originalIcon.scale || 1) * scaleFactor
    };

    // 라벨 확대
    const fontSize = originalLabel?.fontSize || "13px";
    const newFontSize = (parseFloat(fontSize) * scaleFactor) + "px";
    const biggerLabel = {
        ...originalLabel,
        fontSize: newFontSize
    };

    marker.setIcon(biggerIcon);
    marker.setLabel(biggerLabel);


    // 복구 예약
    marker._resetTimerId = setTimeout(() => {
        marker.setIcon(originalIcon);
        marker.setLabel(originalLabel);
        marker._resetTimerId = null;
    }, duration);
}




function clickMarker(marker){
    showPlaceModal(marker.hashId);
    enlargeMarkerTemporarily(marker);
}

function showPlaceModal(hashId, placeId = null) {
    let placeData=null;
    console.log(hashId)
    if(hashId !== null){
        placeData = getEventById(hashId).placeDTO;
    }else{
        placeData = placeMap.get(placeId);
    }

    if(placeData === null)

    console.log(placeData);

    $('#placeModalName').text(placeData.placeName || '-');
    $('#placeModalType').text(getKoreanLabel(placeData.placeType) || '-');
    $('#placeModalRatingCount').text(placeData.googleRatingCount || '0');
    $('#placeModalRating').text(placeData.googleRating || 'N/A');
    $('#placeModalImage').attr('src', placeData.imageUrl || '/default-placeholder.jpg');
    $('#placeModalExplanation').text(placeData.explanation || '-');
    $('#placeModalAddress').text(placeData.address || '-');
    $('#placeModalMapLink').attr('href', placeData.googleURL || '#');

    // 영업 시간 출력
    $('#placeModalHours').empty();
    try {
        const hours = JSON.parse(placeData.regularOpeningHours || '{}');
        if (Array.isArray(hours.weekdayDescriptions)) {
            hours.weekdayDescriptions.forEach(desc => {
                $('#placeModalHours').append(`<li>${desc}</li>`);
            });
        } else {
            $('#placeModalHours').append(`<li>영업 시간 정보 없음</li>`);
        }
    } catch (e) {
        $('#placeModalHours').append(`<li>영업 시간 정보 없음</li>`);
    }

    $('#placeModal').modal('show');
}




function clearMarkers() {
    allMarkers.forEach(marker => marker.setMap(null));
    allMarkers = [];
}

function clearPolylines() {
    allPolylines.forEach(polyline => polyline.setMap(null));
    allPolylines = [];
}



// 맵 초기화
function initMap() {
    console.log("initMap Execute");
    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: googleRegionLat, lng: googleRegionLng },
        zoom: 13,
    });

    sideMap = new google.maps.Map(document.getElementById("side-map"), {
        center: { lat: 37.5665, lng: 126.9780 },
        zoom: 13,
        fullscreenControl: false,    // 전체화면 버튼 비활성화
        streetViewControl: false,    // 스트리트뷰 버튼 비활성화
        mapTypeControl: false        // 지도 유형 변경 버튼 비활성화
    });

    mapReady = true;
    tryRenderMarkerAll();
}

// 데이터 fetch와 map 로딩이 끝났을때 마커 render를 작동
function tryRenderMarkerAll() {
    if (mapReady && dataReady) {
        markerState = 0;
        renderMarkerByMarkerState();
        infoWindow = new google.maps.InfoWindow();
    }
}



//
//
// // 구글 Place api키 GITHUB 및 검색엔진에서 숨기기 위한 함수
// function loadGoogleMapsApi() {
//     fetch('/api/google-places/apikey')
//         .then(response => response.text())
//         .then(apiKey => {
//             let script = document.createElement("script");
//             script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places&callback=initMap`;
//             script.async = true;
//             script.defer = true;
//             document.head.appendChild(script);
//             initMap();
//         })
//         .catch(error => console.error("API Key 가져오기 실패:", error));
// }


//필터 관련 스크롤
const filterButtonsContainer = document.querySelector('.filter-buttons');

let isFilterDragging = false;
let filterDragStartX;
let filterScrollStartX;

filterButtonsContainer.addEventListener('mousedown', (e) => {
    isFilterDragging = true;
    filterButtonsContainer.classList.add('dragging');
    filterDragStartX = e.pageX - filterButtonsContainer.offsetLeft;
    filterScrollStartX = filterButtonsContainer.scrollLeft;
});
filterButtonsContainer.addEventListener('mouseleave', () => {
    isFilterDragging = false;
    filterButtonsContainer.classList.remove('dragging');
});
filterButtonsContainer.addEventListener('mouseup', () => {
    isFilterDragging = false;
    filterButtonsContainer.classList.remove('dragging');
});
filterButtonsContainer.addEventListener('mousemove', (e) => {
    if (!isFilterDragging) return;
    e.preventDefault();
    const currentX = e.pageX - filterButtonsContainer.offsetLeft;
    const moveX = (currentX - filterDragStartX);
    filterButtonsContainer.scrollLeft = filterScrollStartX - moveX;
});
filterButtonsContainer.addEventListener('touchstart', (e) => {
    filterDragStartX = e.touches[0].pageX;
    filterScrollStartX = filterButtonsContainer.scrollLeft;
});
filterButtonsContainer.addEventListener('touchmove', (e) => {
    const currentX = e.touches[0].pageX;
    const moveX = (currentX - filterDragStartX);
    filterButtonsContainer.scrollLeft = filterScrollStartX - moveX;
});


//place 컨테이너 숨기기 버튼 이벤트 리스너
$(document).ready(function() {
    $('.place-container-close').click(function () {
        $('.place-container').removeClass('active');
        $('.place-toggle-button').removeClass('active');
        $('.place-toggle-button').text('+ 장소 추가');
    });
});


$(document).on("click", ".schedule-header-left", function () {
    markerState=0;
    renderMarkerByMarkerState();
});

$(document).on("click", ".place-toggle-button", function () {
    const btn = $('.place-toggle-button');
    btn.toggleClass('active');
    console.log("PRESSED");
    if (btn.hasClass('active')) {
        if(!isPlacePageInitialLoad){
            fetchRecommendedPlaces();
        }
        isPlacePageInitialLoad = true;
        console.log("ON");
        $('.place-container').addClass('active');
        btn.text('완료');
    } else {
        console.log("OFF");
        $('.place-container').removeClass('active');
        btn.text('+ 장소 추가');
    }

});

$(document).on("click", ".add-button", function () {
    const placeId = $(this).closest(".list-item").data("id"); // 부모에서 id 가져옴
    placeToSavedPlace(placeId);

    this.classList.remove("clicked");
    void this.offsetWidth;
    this.classList.add("clicked");

    setTimeout(() => {
        this.classList.remove("clicked");
    }, 2000); // 애니메이션 시간에 맞춰 제거
});

$(document).on('click', '.day-header', function () {
    const $header = $(this);
    const $dayColumn = $header.closest('.day-column');

    // 장소보관함이면 무시
    if ($dayColumn.hasClass('savedPlace')) return;

    // dayNumber 추출
    const dayNumber = parseInt($dayColumn.data('day-number'));

    markerState = dayNumber;
    renderMarkerByMarkerState();
});



function placeToSavedPlace(placeId) {
    console.log(placeMap);
    console.log(placeId);
    const place = placeMap.get(placeId); // placeId로 해당 객체 가져오기

    if (!place) {
        console.warn("해당 placeId에 대한 place가 없습니다:", placeId);
        return;
    }

    const event = {
        dayCount: 0,
        placeDTO: place,
        stayMinute: 60,
        startMinuteSinceStartDay: 0,
        endMinuteSinceStartDay: 0,
        movingMinuteFromPrevPlace: 0,
        movingDistanceFromPrevPlace: 0
    };

    addEvent(event);
    isDirty = true;
    console.log(event.hashId);
    updateSavedPlaceUI([event]);
}


// 수정후 브라우저 뒤로가기,나가기, 새로고침시 경고 메세지
window.addEventListener("beforeunload", function (e) {
    if (isDirty) {
        e.preventDefault();  // 크롬 기준 필요
        e.returnValue = '저장되지 않은 변경 사항이 있습니다. 정말 페이지를 나가시겠습니까?';
    }
});

// 수정후 링크 이동시 경고 메세지
function handleDirtyNavigation(targetUrl) {
    if (!isDirty) {
        window.location.href = targetUrl;
        return;
    }

    Swal.fire({
        title: '저장되지 않은 변경사항이 있습니다.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '나가기',
        cancelButtonText: '취소',
        reverseButtons: true,
        customClass: {
            title: 'swal2-sm-title'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = targetUrl;
        }
    });
}


// 수정후 링크 이동시 경고 메세지 event 핸들러
$("a[href]").click(function(e) {
    const href = $(this).attr("href");
    const target = $(this).attr("target");

    if (!href || e.ctrlKey || e.metaKey || target === "_blank") return;

    e.preventDefault();
    handleDirtyNavigation(href);
});


// event 더블클릭 시 지도 이동 & 마커 강조 & 장소 상세정보 보기
// $(document).on("dblclick", ".event", function () {
//     const eventId = $(this).data("id");
//     const eventData = getEventById(eventId);
//     if (!eventData || !eventData.placeDTO) return;
//
//     const eventDay = eventData.dayCount;
//
//     if(markerState !==0 && markerState !== eventDay && eventDay !== 0){
//         markerState = eventDay
//         renderMarkerByMarkerState();
//     }
//
//     if (eventDay === 0) {
//         renderSavedPlaceMarker();
//     } else {
//         clearSavedPlaceMarker();
//         // ⭐ marker 찾아서 강조
//         const marker = allMarkers.find(m => m.hashId === eventId);
//         if (marker) {
//             enlargeMarkerTemporarily(marker);
//         }
//         sideMap.panTo({ lat: eventData.placeDTO.latitude, lng: eventData.placeDTO.longitude });
//     }
//
//     showPlaceModal(eventId);
// });
$(document).on("dblclick", ".event", function () {
    const eventId = $(this).data("id");
    const eventData = getEventById(eventId);
    if (!eventData || !eventData.placeDTO) return;

    const eventDay = eventData.dayCount;

    if (eventDay !== 0) {
        if (markerState !== 0 && markerState !== eventDay) {
            markerState = eventDay;
            renderMarkerByMarkerState();
        }
        clearSavedPlaceMarker();
    } else {
        renderSavedPlaceMarker();
    }

    if (!isMapPanelOpen) {
        // 맵이 꺼져있으면 바로 모달 띄우기
        showPlaceModal(eventId);
    } else {
        // 맵이 켜져있으면 마커 강조 + InfoWindow 열기
        const marker = allMarkers.find(m => m.hashId === eventId);
        if (marker) {
            enlargeMarkerTemporarily(marker);

            const content = `
                <div style="max-width: 220px; overflow: hidden;">
                    <div style="font-weight: bold; font-size: 14px; margin-bottom: 6px;">
                        ${eventData.placeDTO.placeName}
                    </div>
                    <div style="margin-bottom: 6px;">
                        <img src="${eventData.placeDTO.imageUrl || '/default-placeholder.jpg'}" 
                             alt="장소 이미지" 
                             style="width: 200px; height: 100px; border-radius: 6px; object-fit: cover;">
                    </div>
                    <button class="btn btn-sm btn-outline-primary w-100" 
                            style="font-size: 13px; padding: 4px 8px;" 
                            onclick="showPlaceModal('${eventId}')">
                        세부 정보 보기
                    </button>
                </div>
`;
            infoWindow.setContent(content);
            infoWindow.open(sideMap, marker);
        }
    }
});



function renderSavedPlaceMarker() {
    clearSavedPlaceMarker(); // 기존 마커 제거

    const container = document.getElementById("day-0");
    if (!container) return;

    const firstEventElement = container.querySelector('.event');
    if (!firstEventElement) return;

    const eventId = firstEventElement.getAttribute("data-id");
    const event = getEventById(eventId);
    if (!event || !event.placeDTO) return;

    const { latitude, longitude } = event.placeDTO;

    savedPlaceMarker = new google.maps.Marker({
        hashId: event.hashId,
        position: { lat: event.placeDTO.latitude, lng: event.placeDTO.longitude },
        map: sideMap,
        label: {
            text: "P",
            fontWeight: "600",
            fontSize: "13px",
            color: "#ffffff"
        },
        icon: {
            path: `
                            M 0,0 
                            m -10,-20 
                            a 10,10 0 1,0 20,0 
                            a 10,10 0 1,0 -20,0 
                            M 0,0 
                            l -7,-10 
                            l 14,0 
                            z
                        `,
            fillColor: "#555",
            fillOpacity: 1,
            strokeColor: "#ffffff",
            strokeWeight: 0.5,
            scale: 1,
            labelOrigin: new google.maps.Point(0, -20)
        }
    });

    savedPlaceMarker.addListener("click", function () {
        clickMarker(savedPlaceMarker);
    });

    // 지도 중심 이동
    sideMap.panTo({ lat: latitude, lng: longitude });
}

function clearSavedPlaceMarker() {
    if (savedPlaceMarker) {
        savedPlaceMarker.setMap(null);
        savedPlaceMarker = null;
    }
}

function renderTempMarkerFromPlaceDTO(place) {
    if (!place || !place.latitude || !place.longitude) return;

    const tempMarker = new google.maps.Marker({
        position: { lat: place.latitude, lng: place.longitude },
        map: sideMap,
        label: {
            text: "P",
            fontWeight: "600",
            fontSize: "13px",
            color: "#ffffff"
        },
        icon: {
            path: `
                M 0,0 
                m -10,-20 
                a 10,10 0 1,0 20,0 
                a 10,10 0 1,0 -20,0 
                M 0,0 
                l -7,-10 
                l 14,0 
                z
            `,
            fillColor: "#666",
            fillOpacity: 1,
            strokeColor: "#ffffff",
            strokeWeight: 0.5,
            scale: 1,
            labelOrigin: new google.maps.Point(0, -20)
        }
    });

    // 기존 마커 제거
    clearSavedPlaceMarker();
    savedPlaceMarker = tempMarker;

    tempMarker.addListener("click", function () {
        showPlaceModal(null, place.id); // eventId가 없을 경우 placeId 넘겨서 처리
    });

    // 지도 중심 이동
    sideMap.panTo({ lat: place.latitude, lng: place.longitude });
}


$(document).on("dblclick", ".list-item", function () {
    const placeId = $(this).data("id");
    const place = placeMap.get(placeId);
    if (!place) return;

    renderTempMarkerFromPlaceDTO(place);  // 마커 강조
    showPlaceModal(null, placeId);        // 모달 출력 (eventId 없이 placeId로 처리)
});


$(document).on("click", ".list-item", function () {
    const placeId = $(this).data("id");
    const place = placeMap.get(placeId);
    if (!place) return;

    renderTempMarkerFromPlaceDTO(place);  // 마커 강조
});

$(document).on("click", ".toggle-map-button", function () {
    const $mapPanel = $(".right-side-map");
    const $resizeHandle = $("#resize-handle");
    $mapPanel.toggleClass("hidden");
    $resizeHandle.toggleClass("hidden");
    $(this).toggleClass("on");
    isMapPanelOpen = $(this).hasClass("on"); // 👉 상태 동기화
    // 툴팁 변경
    if ($(this).hasClass("on")) {
        $(this).attr("title", "지도 숨기기");
        const sidebarWidth = $("#sidebar").outerWidth();
        $resizeHandle.css("left", sidebarWidth + "px");
    } else {
        $(this).attr("title", "지도 보기");
    }
});


function initRegionSelect() {
    const $nadeuliRegionSelect = $("#region-select");
    const $googleRegionSelect = $("#google-region-select");

    $nadeuliRegionSelect.empty();
    $googleRegionSelect.empty();

    if (!regions || !Array.isArray(regions) || regions.length <= 1) {
        $nadeuliRegionSelect.addClass("d-none");
        $googleRegionSelect.addClass("d-none");
        return;
    }

    $nadeuliRegionSelect.removeClass("d-none");
    $googleRegionSelect.removeClass("d-none");

    regions.forEach(region => {
        const option = `<option value="${region.regionId}" data-lat="${region.latitude}" data-lng="${region.longitude}" data-radius="${region.radius}">${region.regionName}</option>`;
        $nadeuliRegionSelect.append(option);
        $googleRegionSelect.append(option);
    });
}


$("#region-select").on("change", function () {
    const $selected = $(this).find("option:selected");
    selectedRegionLat = parseFloat($selected.data("lat"));
    selectedRegionLng = parseFloat($selected.data("lng"));
    selectedRegionRadius = parseInt($selected.data("radius"));

    resetRecommendationAndFetch();
});


$("#google-region-select").on("change", function () {
    const $selected = $(this).find("option:selected");
    googleRegionLat = parseFloat($selected.data("lat"));
    googleRegionLng = parseFloat($selected.data("lng"));
    googleRegionRadius = parseInt($selected.data("radius"));



    if (map && googleRegionLat && googleRegionLng) {
        map.setCenter({ lat: googleRegionLat, lng: googleRegionLng });
        map.setZoom(13); // 필요 시 원하는 zoom 조정
    }

    if ($("#google-place-search").val().trim().length > 0) {
        searchGooglePlaces();
    }
});
