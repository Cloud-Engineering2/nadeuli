/************* 🧳 전역 변수 선언 🧳 *************/
// Event 전역변수
let itinerary = null;                   // Itinerary
const perDayMap = new Map();    // ItineraryPerDay - ex) { 1: 1일차 itineraryPerDay 객체 }
const eventMap = new Map();
const groupedByDay = {}; // 렌더링용 - perDay 별로 정렬된 event - ex) { 1:[event1, event2, ..., ], 2:[] } (1,2,..,일차)

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

//디버깅용
let isDEBUG = false;

/***** 🧳 Itinerary 상세 조회 기본 페이지 🧳 *****/
$(document).ready(function () {

    // 현재 페이지 URL에서 iid 추출
    let pathSegments = window.location.pathname.split('/');
    let itineraryId = pathSegments[pathSegments.length - 1]; // 마지막 부분이 ID라고 가정

    // 여행 상세 조회
    $.ajax({
        url: `/api/itinerary/${itineraryId}`,
        method: "GET",
        dataType: "json",
        success: function (data) {
            createData(data);
            renderItinerary();
            // initDateRangePickerModal();
            initSidebarResize(); // 사이드 바
        },
        error: function (xhr, status, error) {
            console.error("Error fetching itinerary:", error);
        }
    });
});


// 🎈 Itinerary Event data 생성
function createData(data) {

    // 일정 정보 복사
    itinerary = { ...data.itinerary };

    // 일차별 일정 복사 및 초기화
    perDayMap.clear();
    data.itineraryPerDays.forEach(dayPerDay => {
        perDayMap.set(dayPerDay.dayCount, { ...dayPerDay });
        if (!groupedByDay[dayPerDay.dayCount]) {
            groupedByDay[dayPerDay.dayCount] = [];
        }
    });
    // 이벤트 데이터 변환 및 추가
    data.itineraryEvents.forEach(event => {
        const dayKey = event.dayCount;
        const baseStartTime = perDayMap.get(dayKey)?.startTime || "00:00:00"; // 해당 날짜의 시작 시간
        const baseStartMinutes = timeToMinutes(baseStartTime); // HH:MM:SS → 분 단위 변환

        let editedEvent = { ...event };
        editedEvent.stayMinute = editedEvent.endMinuteSinceStartDay - editedEvent.startMinuteSinceStartDay;

        let eventHashId = addEvent(editedEvent); // 이벤트 추가 후 ID 생성

        groupedByDay[dayKey].push({
            id: event.id, // ** event 별 총 지출 조회 시 필요 (data-ieid)
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



// 🎈 렌더링
function renderItinerary() {
    // 💡여행 제목
    $(".schedule-header-name").text(itinerary.itineraryName);

    // 여행 기간 (시작 날짜 ~ 종료 날짜)
    let startDate = new Date(itinerary.startDate);
    let endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + itinerary.totalDays - 1);
    // 여행 기간 : 날짜 형식 변환
    let options = {year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short'};
    $(".schedule-header-date").text(
        `${startDate.toLocaleDateString("ko-KR", options)} ~ ${endDate.toLocaleDateString("ko-KR", options)}`
    );

    // 💡일정 UI 렌더링
    const itineraryEventList = $("#itineraryEventList").empty();

    // 💡일자별 탭 컨테이너
    const tabContainer = $("#tabContainer").empty();

    Object.keys(groupedByDay).forEach(dayKey =>  {
        const dayNumber = parseInt(dayKey);
        const startTime = perDayMap.get(dayNumber)?.startTime?.substring(0, 5) || "00:00";

        // 💡탭 버튼
        const tab = $(`
            <div class="tab ${dayNumber === 0 ? "active" : ""}" data-day="${dayNumber}">
                ${dayNumber === 0 ? "장소보관함" : dayNumber + "일차 (" + startTime + ")"}
            </div>
        `);
        tabContainer.append(tab);


        // 💡탭 콘텐츠 (0일차는 장소 보관함으로 설정)
        const dayColumn = $(`
<!--        <div class='day-column ${dayNumber === 0 ? "savedPlace" : ""}'> -->
                <div class="tab-content ${dayNumber === 0 ? "active" : ""}" id="tab-content-${dayNumber}" data-day="${dayNumber}">
<!--                <div class='day-header'>${dayNumber === 0 ? `장소보관함` : `${dayKey}`+`일차 (${startTime})`}</div> -->
                    <div class="event-container" id="day-${dayNumber}"></div>
                </div>
<!--        </div>-->
        `);

        groupedByDay[dayKey].forEach((event, index) => {
            const eventElement = createEventElement(event, index, groupedByDay[dayKey].length, dayNumber === 0);
            if (dayKey === '0') {
                eventElement.find('.event-time').detach(); // event-time 제거
            }
            dayColumn.find('.event-container').append(eventElement);
        });
        itineraryEventList.append(dayColumn);
    });

    // 💡탭 클릭 이벤트 처리
    $(".tab").on("click", function () {
        const selectedDay = $(this).data("day");

        // 탭 활성화(active)
        $(".tab").removeClass("active");
        $(this).addClass("active");

        // (해당 day) 탭의 콘텐츠 활성화
        $(".tab-content").removeClass("active");
        $(`#tab-content-${selectedDay}`).addClass("active");

        // 처음 클릭 시 .travel-info 숨기기
        $(".event-container").each(function () {
            $(this).find(".event .travel-info").first().css("display", "none");
        });
    });

    // 초기 상태에서 각 요일 첫 번째 .travel-info 숨기기
    $(".event-container").each(function () {
        $(this).find(".event .travel-info").first().css("display", "none");
    });

    initializeSortable(); // 드래그 & 드롭 기능 활성화
}


// 🎈 Itinerary Event 이벤트 요소 생성 (장소 보관함 & 일반 이벤트 공통 사용)
function createEventElement(event, index = null, totalEvents = null, isSavedPlace = false) {

    const itineraryEventDiv = $(`
                        <div class='event' data-id='${event.hashId}'>
                            <div class="event-wrapper">
                                <div class="travel-info">${isSavedPlace ? "" : `이동 시간 ${event.movingMinute}분`}</div>
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
                                            ${isSavedPlace ? "" : `<div class='event-time'>${formatTime(event.startMinute)} ~ ${formatTime(event.endMinute)}</div>`}
                                            <!-- 총 지출 --> 
                                            <div class="event-total-expense" id="eventTotalExpense" data-iid='${itinerary.id}' data-ieid='${event.id}'>
                                                0 원
                                            </div>
                                            <!-- 경비 내역 추가 -->
                                            <div class="expense-item-list-addition" id="expenseItemListAddition" data-iid='${itinerary.id}' data-ieid='${event.id}'>+ 경비 내역 추가</div>
                                        </div>
                                        <div class="event-right">
                                            <button class="event-options-button">⋮</button>
                                            <div class="event-options hidden">
                                                <button class="event-duration">머무는 시간</button>
                                                <button class="event-remove">삭제</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `);

    // 현재 지출액
    getTotalExpenseByItineraryEvent(itinerary.id, event.id).then(totalExpense => {
        itineraryEventDiv.find(".event-total-expense").html(`${totalExpense} 원`);
    }).catch(err => {
        console.error("Error fetching total expense:", err);
        itineraryEventDiv.find(".event-total-expense").html("0 원"); // 에러 발생 시 '0 원'으로 설정
    });

    return itineraryEventDiv;
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
                <div class='event-tab-container' id='day-${dayCount}'></div>
            </div>
        `);

        // 🚀 `itinerary-eventList`에 추가
        $("#itineraryEventList").append(dayColumn);

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
        const travelInfo = eventElement.querySelector(".travel-info");
        if (travelInfo) {
            travelInfo.textContent = `이동 시간 ${event.movingMinuteFromPrevPlace ?? 0}분`;
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
            let eventElement = createEventElement(event,null,null,isPlaceSaved);
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

            if (updateStartIndexFrom !== null) {
                updateEventDisplay(fromDayId, updateStartIndexFrom);
            }
            if (updateStartIndexTo !== null) {
                updateEventDisplay(toDayId, updateStartIndexTo);
            }

            console.log(eventMap);
            $(".travel-info").css("visibility", "visible");


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


/************ 🧳 사이드바 크기 조절 기능 🧳************/
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
        $("#left").css("width", newWidth + "px");
        $("#resize-handle").css("left", newWidth + "px");
    }
}

//마우스 버튼을 놓으면 크기 조절 종료
function stopSidebarResize() {
    $(document).off("mousemove", resizeSidebar);
    $(document).off("mouseup", stopSidebarResize);
}




/************ 🧳 이벤트 데이터 관리 🧳************/

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


/************ 🧳 일정 조작 및 거리 계산 🧳************/
// 거리(시간) 계산 요청을 함수로 분리
function calculateDistanceUpdates(dayId, oldIndex, newIndex, movedForward) {
    console.log(`🔄 거리 계산 업데이트: ${dayId}, oldIndex: ${oldIndex} → newIndex: ${newIndex}`);

    const calculatedPairs = new Set();

    //중복 방지 함수
    function safeCalculateDistance(index1, index2) {
        const pairKey = `${index1}-${index2}`;
        if (!calculatedPairs.has(pairKey)) {
            calculatedPairs.add(pairKey);
            calculateDistanceByIndex(dayId, index1, index2);
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
    calculateDistanceByIndex(dayId, oldIndex - 1, oldIndex);
    return oldIndex;
}

// 추가 시 거리 재계산
function calculateInsertionImpact(dayId, newIndex) {
    calculateDistanceByIndex(dayId, newIndex - 1, newIndex);
    calculateDistanceByIndex(dayId, newIndex, newIndex + 1);
    return newIndex;
}

// dayId 칼럼의 index1, index2의 거리 계산
function calculateDistanceByIndex(dayId, index1, index2) {
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
                firstEvent.movingMinuteFromPrevPlace = 0;
                eventMap.set(firstEventId, firstEvent);
                console.log(`✅ 업데이트 완료: ${firstEventId}의 movingMinuteFromPrevPlace → 0분`);
            }
        }
        return;
    }

    // 예외 처리: 끝부분 (범위를 초과하는 경우)
    if (index1 >= itemCount || index2 >= itemCount) {
        console.warn(`❌ 유효하지 않은 인덱스: index1=${index1}, index2=${index2}`);
        return;
    }

    const eventId1 = items[index1]?.getAttribute("data-id");
    const eventId2 = items[index2]?.getAttribute("data-id");

    if (!eventId1 || !eventId2) {
        console.warn(`❌ 이벤트 ID 없음: index1=${index1}, index2=${index2}`);
        return;
    }

    const event1 = getEventById(eventId1);
    const event2 = getEventById(eventId2);

    if (event1 && event2) {
        const {
            distance,
            minute
        } = requestDistanceCalculation(event1.placeDTO.googlePlaceId, event2.placeDTO.googlePlaceId);

        // event2의 movingMinuteFromPrevPlace 업데이트
        event2.movingMinuteFromPrevPlace = minute;

        // eventMap 업데이트
        eventMap.set(eventId2, event2);

        console.log(`✅ 업데이트 완료: ${eventId2}의 movingMinuteFromPrevPlace → ${minute}분`);
    } else {
        console.warn(`❌ 이벤트 조회 실패: eventId1=${eventId1}, eventId2=${eventId2}`);
    }
}

// 거리 계산 요청 (임시 랜덤 값 반환)
function requestDistanceCalculation(placeId1, placeId2) {
    console.log(`🚗 거리(시간) 계산 요청: ${placeId1} → ${placeId2}`);

    // 랜덤 값 생성 (예제)
    const distance = Math.floor(Math.random() * 50) + 1; // 1 ~ 50km
    const minute = 30;//Math.floor(Math.random() * 120) + 1; // 1 ~ 120분

    return {distance, minute};
}


/******** 🧳 여행 기간 및 시간 설정 🧳 ********/

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

    console.log("Updated perDayMap:", perDayMap);
}



// 🎛️ 일정 저장 및 API 통신
//------------------------------------------

// JSON 데이터 생성 함수
function generateItineraryJson() {
    // 🎯 불필요한 필드 제거 (createdDate, modifiedDate, role 제외)
    const { createdDate, modifiedDate, role, ...filteredItinerary } = itinerary;

    // 📅 perDayMap을 배열 형태로 변환
    const itineraryPerDays = Array.from(perDayMap.values());

    // 📌 eventMap 변환 (필드 최적화)
    const itineraryEvents = Array.from(eventMap.values()).map(event => ({
        ...event,
        pid: event.placeDTO.id, // placeDTO.id를 pid로 변경
        hashId: undefined, // hashId 제거
        placeDTO: undefined, // placeDTO 제거
        stayMinute: undefined // stayMinute 제거
    }));

    // 🏁 최종 JSON 반환
    return JSON.stringify({ itinerary: filteredItinerary, itineraryPerDays, itineraryEvents });
}
function saveItinerary() {
    const $button = $("#save-button");
    $button.prop("disabled", true).text("저장중...");

    const jsonData = generateItineraryJson();

    $.ajax({
        url: "http://localhost:8085/api/itinerary/update",
        method: "POST",
        contentType: "application/json",
        data: jsonData,
        success: function (response) {
            console.log("저장 성공:", response);
            alert("저장이 완료되었습니다!");
        },
        error: function (xhr, status, error) {
            console.error("저장 실패:", error);
            alert("저장 중 오류가 발생했습니다.");
        },
        complete: function () {
            $button.prop("disabled", false).text("저장하기");
        }
    });
}



/************** 🧳 기타 유틸리티 함수 🧳 **************/

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


/************** 🧳 이벤트 핸들링 🧳 **************/
$("#save-button").click(saveItinerary);

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

    // 거리(시간) 재계산
    if (dayId !== "day-0") {
        const updateStartIndex = calculateRemovalImpact(dayId, index);
        updateEventDisplay(dayId, updateStartIndex);

        // ✅ 삭제 후 첫 번째 `.travel-info` 숨기기
        $(eventContainer).find(".event .travel-info").css("display", "block");
        $(eventContainer).find(".event .travel-info").first().css("display", "none");

        // ✅ 연결선 업데이트
        $(eventContainer).find(".event .event-order-line").removeClass("transparent");
        $(eventContainer).find(".event .event-order-line.top").first().addClass("transparent");
        $(eventContainer).find(".event .event-order-line.bottom").last().addClass("transparent");
    }
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



/************** 🧳 경비 & 작성 이벤트 핸들링 🧳 **************/


//🎈 왼쪽 패널 - +경비 내역 추가 클릭 시 -> 오른쪽 패널에 경비 내역 로드
$(document).on("click", ".expense-item-list-addition", function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    // const element = document.getElementById("adjustmentHeaderTabInactive");
    // element.setAttribute("data-iid", iid);  // data-iid 설정
    // element.setAttribute("data-ieid", ieid); // data-ieid 설정

    loadExpenseItemListAndAddition(iid, ieid);
});

//🎈 복붙 : 경비 탭 클릭 시 -> 오른쪽 패널에 경비 내역 로드
// $(document).on("click", ".adjustment-header-tab-inactive", function() {
//     const iid = $(this).data("iid");   // itinerary ID 가져오기
//     const ieid = $(this).data("ieid"); // event ID 가져오기
//
//     oadExpenseItemListAndAddition(iid, ieid);
//
// });


async function loadExpenseItemListAndAddition(iid, ieid) {
    // traveler 조회
    let travelersResponse = callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);
    let travelers = [];
    travelersResponse.then((data) => {
        for (let t of data.travelers) { travelers.push(t.name); }
        })
        .catch((error) => {
            console.error("에러 발생:", error);
        });

    // const totalAdjustmentData = await callApiAt(`/api/itineraries/${iid}/adjustment`, "GET", null);
    // const travelerData = await callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);
    // const adjustmentBasicInfoRemainedBudget = $("#adjustmentBasicInfoRemainedBudget");
    // const adjustmentBasicInfoTraveler = $("#adjustmentBasicInfoTraveler");
    // // 남은 예산
    // const remainedBudget = totalAdjustmentData.totalBalance;
    // // 함께하는 traveler
    // const numberOfTravelers = travelerData.numberOfTravelers;
    //
    // // 남은 예산
    // adjustmentBasicInfoRemainedBudget.html(`남은 예산 : ${remainedBudget} 원`);
    // // 함께하는 traveler
    // adjustmentBasicInfoTraveler.html(`${numberOfTravelers} 명과 함께하고 있습니다`);

    // expense-right.html을 오른쪽 화면`#detailContainer` 영역에 로드
    fetch(`/itinerary/${iid}/events/${ieid}/expense-right`) // fetch("/expense-book/expense-right.html")
        .then(response => response.text())
        .then(html => {
            $("#detailContainer").html(html);
            getExpenseBookForWritingByItineraryEvent(iid, ieid);

            document.getElementById("expenseItemCreation").innerHTML = getExpenseItemForm(iid, ieid);
            setTimeout(() => {
                var withWhom = document.querySelector('input[name=withWhom]');
                var payer = document.querySelector('input[name=payer]');
                var withWhomTag = new Tagify(withWhom, {mode: 'input', whitelist: travelers, enforceWhitelist: true});
                var payerTag = new Tagify(payer, {mode: 'input', whitelist: travelers, maxTags: 1, enforceWhitelist: true});

                // withWhomTag 값을 input 태그의 value로 설정하는 함수
                function insertWithWhomTagToInputValue() {
                    const withWhomValue = withWhomTag.value.map(item => item.value).join(', ');
                    withWhom.value = withWhomValue;
                    console.log("Updated withWhom input value: ", withWhom.value);
                }

                withWhomTag.on('add', function() {
                    insertWithWhomTagToInputValue();
                    console.log("WithWhomTag Value: ", withWhomTag.value);
                });

                // payer와 withWhom 값이 중복되지 않도록
                function compareAndRemoveTag() {
                    const withWhomValue = withWhomTag.value.map(item => item.value);  // withWhomTag에 입력된 값
                    const payerValue = payerTag.value.length > 0 ? payerTag.value[0].value : null;  // payerTag에 입력된 값

                    if (withWhomValue.includes(payerValue)) {
                        // 'remove' 이벤트 트리거로 withWhomTag에서 값 제거
                        withWhomTag.removeTags(payerValue);
                        console.log(`Removed: ${payerValue} from withWhomTag because it matches payerTag value`);
                    }
                }
                payerTag.on('add', function() {
                    compareAndRemoveTag();
                    console.log("PayerTag Value: ", payerTag.value);
                });
                withWhomTag.on('add', function() {
                    compareAndRemoveTag();
                    console.log("WithWhomTag Value: ", withWhomTag.value);
                });
                payerTag.on('remove', function() {
                    compareAndRemoveTag();
                    console.log("PayerTag Value After Remove: ", payerTag.value);
                });
            }, 100);

        })
        .catch(error => console.error("Error loading expense-right.html:", error)
        );

}



// ItineraryEvent 별로 ExpenseItem들 조회
async function getExpenseBookForWritingByItineraryEvent(iid, ieid) {
    try {
        // expenseItem 데이터 가져오기
        const expenseItems = await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense`, "GET", null);

        const expenseItemList = $("#expenseItemList");
        if (!expenseItemList.length) {
            console.error("Expense list element not found!");
            return;
        }

        // 우선 expense item만 리스트에 표시
        expenseItemList.html(
            expenseItems.map(expenseItem =>
                `<div class="expense-item-box" id="expenseItemBox-${expenseItem.id}" style="display: flex;">
                    <div class="expense-item-content" id="expenseItemContent">${expenseItem.content}</div>
                    <div class="expense-item-expenditure" id="expenseItemExpenditure">${expenseItem.expense}원</div>
                    <div class="expense-item-payer" id="expenseItemPayer">${expenseItem.travelerDTO.travelerName}</div>
                    <div class="expense-item-with-whom" id="expenseItemWithWhom-${expenseItem.id}"><small class="with-whom" data-emid="${expenseItem.id}">💡 함께한 사람: 로딩 중...</small></div>
                </div>`
            ).join("")
        );

        // 각 expense item에 대한 withWhom 데이터를 개별적으로 가져와 업데이트
        for (const expenseItem of expenseItems) {
            try {
                const withWhomResponse = await fetch(`/api/itineraries/${iid}/expense/${expenseItem.id}/withWhom`);
                const withWhomData = await withWhomResponse.json();

                // 특정 expense 항목의 withWhom 데이터를 업데이트
                $(`#expenseItemWithWhom-${expenseItem.id} .with-whom`).html(
                    `${withWhomData.map(withWhom => withWhom.travelerDTO.travelerName).join(", ")}`
                );
            } catch (whomError) {
                console.error(`Error loading withWhom data for expense ${expenseItem.id}:`, whomError);

            }
        }

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}

//🎈 오른쪽 패널 - 경비 내역 추가 폼
// html : expense item 추가 폼
function getExpenseItemForm(itineraryId, itineraryEventId) {
    return `<form class="expense-item-creation-form" id="expenseItemCreationForm">
                <input type="text" class="expense-item-creation-content" id="expenseItemCreationContent" name="content" placeholder="지출 내용">
                <input type="number" class="expense-item-creation-expenditure" id="expenseItemCreationExpenditure" name="expenditure" required placeholder="(원)">
                <input type="text" class="expense-item-creation-payer" id="expenseItemCreationPayer" name="payer" required placeholder="지출한 사람">
                <input type="text" class="expense-item-creation-withWhom" id="expenseItemCreationWithWhom"  name="withWhom" placeholder="함께한 사람">
                <!-- Expense Item 추가 + 버튼 -->
                <button type="submit" class="expense-item-addition-button" id="expenseItemAdditionPlusButton" data-iid='${itineraryId}' data-ieid='${itineraryEventId}'>
                    <i class="fa-solid fa-plus plus-icon"></i>
                </button>
            </form>`;
}




// Itinerary Event 별 현재 총 지출액 가져오기 (left)
async function getTotalExpenseByItineraryEvent(itineraryId, eventId) {
    const response = await fetch(`/api/itineraries/${itineraryId}/events/${eventId}/adjustment`, {
        method: 'GET'
    });

    if (!response.ok) {
        console.error('Failed to fetch total expense');
        return '0'; // 실패 시 기본값 '0'
    }

    const data = await response.json();
    const totalExpense = data.totalExpense.toLocaleString();
    return totalExpense;
}



// 🎈왼쪽 패널 - 현재 총 지출액 클릭 : Itinerary Event 별 정산 정보 오른쪽 패널에 로드
$(document).on("click", ".event-total-expense", function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    // adjustment-right.html을 오른쪽 화면`#detailContainer` 영역에 로드
    fetch(`/itinerary/${iid}/events/${ieid}/adjustment-right`)
        .then(response => response.text())
        .then(html => {
            $("#detailContainer").html(html);
            getAdjustmentByItineraryEvent(iid, ieid);
        })
        .catch(error => console.error("Error loading adjustment-right.html:", error));

});

// Itinerary Event 별 정산 정보 조회
async function getAdjustmentByItineraryEvent(iid, ieid) {
    try {
        // adjustment 데이터 가져오기
        const adjustmentData = await callApiAt(`/api/itineraries/${iid}/events/${ieid}/adjustment`, "GET", null);
        const totalAdjustmentData = await callApiAt(`/api/itineraries/${iid}/adjustment`, "GET", null);
        const travelerData = await callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);

        const individualAdjustmentList = $("#individualAdjustmentList");
        const totalExpenditure = $("#totalExpenditure");
        const adjustmentInfo = $("#itineraryEventAdjustmentInfo");
        const individualExpenditureList = $("#individualExpenditureList");
        const adjustmentBasicInfoRemainedBudget = $("#adjustmentBasicInfoRemainedBudget");
        const adjustmentBasicInfoTraveler = $("#adjustmentBasicInfoTraveler");

        if (!adjustmentInfo.length) {
            console.error("ItineraryEvent Adjustment Info element not found!");
            return;
        }

        // 데이터 추출
            // 총 지출, 개인별 지출, 경비 정산
        const { totalExpense, eachExpenses, adjustment } = adjustmentData;
            // 남은 예산
        const remainedBudget = totalAdjustmentData.totalBalance;
            // 함께하는 traveler
        const numberOfTravelers = travelerData.numberOfTravelers;


        // 렌더링

            // 남은 예산
        adjustmentBasicInfoRemainedBudget.html(`남은 예산 : ${remainedBudget} 원`);
            // 함께하는 traveler
        adjustmentBasicInfoTraveler.html(`${numberOfTravelers} 명과 함께하고 있습니다`);
            // 총 지출
        let totalExpenditureDetails = `<p class="total-expenditure-money-align"><span class="total-expenditure-money-label">총 지출</span>    <span class="total-expenditure-money">${totalExpense} 원</span></p>`;
        totalExpenditure.html(totalExpenditureDetails);

            // 개인 지출
        let individualExpenditureDetails = "";
        for (const [name, expense] of Object.entries(eachExpenses)) {
            individualExpenditureDetails += `<p class="individual-expenditure"><span class="individual-expenditure-name">@${name}</span><br><span class="individual-expenditure-label">지출</span> <span class="total-expenditure-money">${expense.toLocaleString()} 원</span></p>`;
        }
        individualExpenditureList.html(individualExpenditureDetails);

            // 경비 정산
        let individualAdjustmentDetails = "";

// 제목행
        individualAdjustmentDetails += `<tr>
    <th>이름</th>
    <th>수금</th>
    <th>송금</th>
</tr>`;

        for (const [name, details] of Object.entries(adjustment)) {
            // 수금(receivedMoney) 항목 처리
            const received = Object.entries(details.receivedMoney || {})
                .map(([from, amount]) => `<p class="individual-received"><span class="individual-received-from">@${from}</span> <span class="individual-received-money">${amount.toLocaleString()} 원</span></p><br>`)
                .join("") || "-";

            // 송금(sendedMoney) 항목 처리
            const sended = Object.entries(details.sendedMoney || {})
                .map(([to, amount]) => `<p class="individual-sended"><span class="individual-sended-to">@${to}</span> <span class="individual-sended-money">${amount.toLocaleString()} 원</span></p><br>`)
                .join("") || "-";

            individualAdjustmentDetails += `<tr>
        <td class="adjustment-subject">@${name}</td>
        <br>
        <td>${received}</td>
        <td>${sended}</td>
    </tr>`;
        }

        individualAdjustmentList.html(individualAdjustmentDetails);



        //     // 💰 개인별 총 지출 테이블 추가
    //     let eachExpensesTable = "<table class='table table-striped'>";
    //     eachExpensesTable += "<thead><tr><th>이름</th><th>총 지출</th></tr></thead><tbody>";
    //
    //     for (const [name, expense] of Object.entries(eachExpenses)) {
    //         eachExpensesTable += `<tr>
    //         <td>${name}</td>
    //         <td>${expense.toLocaleString()} 원</td>
    //     </tr>`;
    //     }
    //     eachExpensesTable += "</tbody></table>";
    //
    //     // HTML 업데이트
    //     adjustmentInfo.html(`
    //     <h3>💰 정산 정보</h3>
    //     <p><strong>현재 총 지출:</strong> ${totalExpense.toLocaleString()} 원</p>
    //     <h4>🧾 개인별 정산 내역</h4>
    //     ${adjustmentDetails}
    //     <h4>💸 개인별 총 지출</h4>
    //     ${eachExpensesTable}
    // `);

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}


//🎈 오른쪽 패널 - +버튼 클릭 시 -> 경비 내역(expense item, with whom) 추가
$(document).on("click", ".expense-item-addition-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID

    // Request Data
    const content = $("#expenseItemCreationContent").val() || null;
    console.log(content);
    const expenditure = $("#expenseItemCreationExpenditure").val();
    console.log(expenditure);

    // payer, withWhom 처리 부분은 그대로
    const payerInput = document.querySelector("#expenseItemCreationPayer");
    console.log(payerInput);
    const payerTag = payerInput ? payerInput._tagify : null;
    console.log(payerTag);
    const payer = payerTag && payerTag.value.length > 0 ? payerTag.value[0].value : null;
    console.log(payer);

    const withWhomInput = document.querySelector("#expenseItemCreationWithWhom");
    const withWhomTag = withWhomInput ? withWhomInput._tagify : null;
    const withWhomList = withWhomTag ? withWhomTag.value.map(item => item.value) : [];

    // 유효성 검사
    if (!expenditure || !payer) {
        alert("금액과 지출자는 반드시 입력해야 합니다.");
        return;
    }

    const expenseItemRequestData = { // RequestBody -> ExpenseItemRequestDTO
        content: content,
        payer: payer,
        expense: parseInt(expenditure)
    };

    const withWhomData = {
        withWhomNames: withWhomList // WithWhomRequestDTO 키 값과 동일해야 함
    };

    try {
        let expenseItemId = await addExpenseItem(iid, ieid, expenseItemRequestData);
        await addWithWhom(iid, expenseItemId, withWhomData);

        // 🎯 폼 입력값 초기화
        $("#expenseItemCreationForm")[0].reset();

        // 🎯 페이지 새로고침 (데이터 반영을 위해)
        location.reload();
    } catch (error) {
        console.error("🚨 데이터 저장 중 오류 발생:", error);
        alert("지출 항목을 추가하는 중 오류가 발생했습니다.");
    }
});

async function addExpenseItem(iid, ieid, expenseItemRequestData) {
    try {
        const response = await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense`, "POST", expenseItemRequestData);

        console.log("ExpenseItem " + expenseItemRequestData.content + ": " + expenseItemRequestData.expense + "(원) - 생성 완료");
        return response.id;

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}


async function addWithWhom(iid, emid, withWhomRequestData) {
    try {
        const response = await callApiAt(`/api/itineraries/${iid}/expense/${emid}/withWhom`, "POST", withWhomRequestData);

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}

/************** 🧳 api 호출 함수 🧳 **************/
async function callApiAt(url, method, requestData) {
    try {
        const response = await fetch(url, {
            method: method, // HTTP 메서드 (예: "POST", "GET")
            headers: { "Content-Type": "application/json" },
            body: method !== "GET" ? JSON.stringify(requestData) : null, // GET 메서드일 경우 body 없이 호출
        });

        if (!response.ok) {
            throw new Error(`HTTP 오류! 상태 코드: ${response.status}`);
        }

        const data = response.headers.get("Content-Length") === "0" ? null : await response.json();
        return data;
    } catch (error) {
        console.error("에러 발생:", error);
        throw error;
    }
}
