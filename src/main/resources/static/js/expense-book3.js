/************* 🧳 전역 변수 선언 🧳 *************/
let itinerary = null;                   // Itinerary
const perDayMap = new Map();    // ItineraryPerDay - ex) { 1: 1일차 itineraryPerDay 객체 }
const groupedByDay = {};                  // 렌더링용 - perDay 별로 정렬된 event - ex) { 1:[event1, event2, ..., ], 2:[] } (1,2,..,일차)
const eventMap = new Map();

//디버깅용
let isDEBUG = false;

/***** 🧳 Itinerary 상세 조회 기본 페이지 🧳 *****/
$(document).ready(function () {
    // 현재 페이지 URL에서 iid 추출
    let pathSegments = window.location.pathname.split('/');
    let itineraryId = pathSegments[pathSegments.length - 1]; // 마지막 부분이 ID라고 가정

    // 여행 상세 조회 api 호출
    $.ajax({
        url: `/api/itinerary/${itineraryId}`,
        method: "GET",
        dataType: "json",
        success: function (data) {
            createData(data);
            renderItinerary();
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
        editedEvent.stayMinute = editedEvent.endMinuteSinceStartDay - editedEvent.startMinuteSinceStartDay; // 머문 시간

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
    console.log("groupedByDay entries:", Object.entries(groupedByDay));  // key-value

    // 💡일자별 탭 컨테이너
    const tabContainer = $("#tabContainer").empty();

    Object.keys(groupedByDay).forEach(dayKey =>  {
        const dayNumber = parseInt(dayKey);
        // itinerary event 기간 (장소에 머무는 시간)
        const startTime = perDayMap.get(dayNumber)?.startTime?.substring(0, 5) || "00:00";
        console.log(dayKey);

        // 💡탭 버튼
        const tab = $(`
            <div class="tab ${dayNumber === 0 ? "active" : ""}" data-day="${dayNumber}">
                ${dayNumber === 0 ? "장소보관함" : dayNumber + "일차 (" + startTime + ")"}
            </div>
            <!--  장소보관함 수정 필요 -->
        `);
        tabContainer.append(tab);

        // 💡탭 콘텐츠
        const dayColumn = $(`
                <div class="tab-content ${dayNumber === 1 ? "active" : ""}" id="tab-content-${dayNumber}" data-day="${dayNumber}">
                    <div class="event-container" id="day-${dayNumber}"></div>
                </div>
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
    $(".tab-button").on("click", function () {
        const selectedDay = $(this).data("day");

        // 탭 활성화(active)
        $(".tab-button").removeClass("active");
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
    console.log("Event Object:", event);
    const itineraryEventDiv = $(`
                        <div class='event' data-id='${event.id}'>
                            <div class="event-wrapper">
                                <div class="travel-info">${isSavedPlace ? "" : `이동 시간 ${event.movingMinute}분`}</div>
                                <div class="event-content">
                                    <!-- 순서 번호 -->
                                    <div class="event-order">
                                        <div class="event-order-line top ${index === 0 ? "transparent" : ""}"></div>
                                        <div class="event-order-circle">${isSavedPlace ? "X" : index + 1}</div>
                                        <div class="event-order-line bottom ${index === totalEvents - 1 ? "transparent" : ""}"></div>
                                    </div>
                                    <div class="event-main">                                        
                                        <div class='event-title'>${event.placeDTO.placeName}</div> 
                                            ${isSavedPlace ? "" : `<div class='event-time'>${formatTime(event.startMinute)} ~ ${formatTime(event.endMinute)}</div>`}
                                            <!-- 총 지출 --> 
                                            <div class="event-total-expense" id="eventTotalExpense" data-iid='${itinerary.id}' data-ieid='${event.id}'>
                                                0 원
                                            </div>
                                            <!-- 경비 내역 추가 -->
                                            <div class="expense-item-addition" id="expenseItemAddition" data-iid='${itinerary.id}' data-ieid='${event.id}'>+ 경비 내역 추가</div>                                        
                                        </div>
                                    </div>
                                </div>
                            </div>
                     `);

    return itineraryEventDiv;
}

// 일정 드래그 & 드롭 활성화
function initializeSortable() {
    $(".event-container").each(function () {
        createSortableInstance(this);  // 뭐지?
    });
}

// events 장소 보관함에 생성
function updateSavedPlaceUI(events) {
    const savedPlaceContainer = $("#day-0"); // day-0 event-container
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

        // 🚀 `schedule-container`에 추가
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
        $("#sidebar").css("width", newWidth + "px");
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



//******** 🧳 여행 기간 및 시간 설정 🧳 ********//

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







/************ 🧳 자주 쓰는 함수들 🧳************/
// 🎈 api 호출하여 json data 반환
async function callApiAt(url, method, requestData) {
    try {
        const response = await fetch(url, {
            method: method, // 예: "POST", "GET")
            headers: { "Content-Type": "application/json" },
            body: requestData ? JSON.stringify(requestData) : null,
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



/************ 🧳 이벤트 리스너 🧳************/
// 🎈왼쪽 패널 - 현재 총 지출액 클릭 : Itinerary Event 별 정산 정보 오른쪽 패널에 로드
$(document).on("click", ".event-total-expense", function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    console.log(`Clicked total expense: iid=${iid}, ieid=${ieid}`);

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

        const adjustmentInfo = $("#itineraryEventAdjustmentInfo");
        if (!adjustmentInfo.length) {
            console.error("ItineraryEvent Adjustment Info element not found!");
            return;
        }

        // 데이터 추출
        const { totalBudget, totalExpenses, currentExpense, totalBalance, adjustment } = adjustmentData;

        // 개인별 adjustment 데이터를 테이블 형태로 변환
        let adjustmentDetails = "<table class='table table-bordered'>";
        adjustmentDetails += "<thead><tr><th>이름</th><th>수금</th><th>지불</th></tr></thead><tbody>";

        for (const [name, details] of Object.entries(adjustment)) {
            const received = Object.entries(details.receivedMoney || {})
                .map(([from, amount]) => `${from} → ${amount.toLocaleString()}원 (received)`)
                .join("<br>") || "-";

            const sended = Object.entries(details.sendedMoney || {})
                .map(([to, amount]) => `${to} → ${amount.toLocaleString()}원 (sended)`)
                .join("<br>") || "-";

            adjustmentDetails += `<tr>
                <td>${name}</td>
                <td>${received}</td>
                <td>${sended}</td>
                <td>${details.total.toLocaleString()} 원</td>
            </tr>`;
        }

        adjustmentDetails += "</tbody></table>";
        console.log("📌 HTML로 추가될 데이터:", adjustmentDetails);
        // HTML 업데이트
        adjustmentInfo.html(`
            <h3>💰 정산 정보</h3>
            <p><strong>총 예산:</strong> ${totalBudget.toLocaleString()} 원</p>
            <p><strong>현재 총 지출:</strong> ${totalExpenses.toLocaleString()} 원</p>
            <p><strong>현재 이벤트 지출:</strong> ${currentExpense.toLocaleString()} 원</p>
            <p><strong>남은 금액:</strong> ${totalBalance.toLocaleString()} 원</p>
            <h4>🧾 개인별 정산 내역</h4>
            ${adjustmentDetails}
        `);

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}


//🎈 왼쪽 패널 - +경비 내역 추가 클릭 시 -> 오른쪽 패널에 경비 내역 로드
$(document).on("click", ".expense-item-addition", function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    console.log(`Clicked expense item addition: iid=${iid}, ieid=${ieid}`);

    // expense-right.html을 오른쪽 화면`#detailContainer` 영역에 로드
    fetch(`/itinerary/${iid}/events/${ieid}/expense-right`) // fetch("/expense-book/expense-right.html")
        .then(response => response.text())
        .then(html => {
            $("#detailContainer").html(html);
            getExpenseBookForWritingByItineraryEvent(iid, ieid);
            document.getElementById("expenseItemCreation").innerHTML = getExpenseItemForm(iid, ieid);
        })
        .catch(error => console.error("Error loading expense-right.html:", error));
});

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
                console.log(`Fetching withWhom for expense ${expenseItem.id}`, expenseItem);
                const withWhomResponse = await fetch(`/api/itineraries/${iid}/expense/${expenseItem.id}/withWhom`);
                const withWhomData = await withWhomResponse.json();
                console.log(`withWhomData for expense ${expenseItem.id}:`, withWhomData);

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

// html : expense item 추가 폼
function getExpenseItemForm(itineraryId, itineraryEventId) {
    return `<form class="expense-item-creation-form" id="expenseItemCreationForm">
                <input type="text" class="expense-item-creation-content" id="expenseItemCreationContent" name="content" value="항목">
                <input type="number" class="expense-item-creation-expenditure" id="expenseItemCreationExpenditure" name="expenditure" required value="지출액">
                <input type="text" class="expense-item-creation-payer" id="expenseItemCreationPayer" name="payer" required value="지출자">
                <input type="text" class="expense-item-creation-withWhom" id="expenseItemCreationWithWhom"  name="withWhom">
<!--            <div class="expense-item-creation-button-group" id="expenseItemCreationButtonGroup">-->
<!--                <button type="submit" class="expense-item-creation-button" id="expenseItemCreationButton" >추가</button>-->
<!--                <button type="button" class="expense-item-creation-button-close" id="expenseItemCreationButtonClose">닫기</button>-->
<!--            </div>-->
                <!-- Expense Item 추가 + 버튼 -->
                <button type="submit" class="expense-item-addition-button" id="expenseItemAdditionPlusButton" data-iid='${itineraryId}' data-ieid='${itineraryEventId}'>
                    <i class="fa-solid fa-plus plus-icon"></i>
                </button>
            </form>`;
}