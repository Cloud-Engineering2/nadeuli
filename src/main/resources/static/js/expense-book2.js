/************* 🧳 전역 변수 선언 🧳 *************/
let itinerary = null;                   // Itinerary
const perDayMap = new Map();    // ItineraryPerDay - ex) { 1: 1일차 itineraryPerDay 객체 }
const groupedByDay = {};                  // 렌더링용 - perDay 별로 정렬된 event - ex) { 1:[event1, event2, ..., ], 2:[] } (1,2,..,일차)
const eventMap = new Map();


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
            initSidebarResize();
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
            <div class="tab-button ${dayNumber === 1 ? "active" : ""}" data-day="${dayNumber}">
                ${dayNumber}일차 (${startTime})
            </div>
        `);
        tabContainer.append(tab);

        // 💡탭 콘텐츠
        const dayColumn = $(`
                <div class="tab-content ${dayNumber === 1 ? "active" : ""}" id="tab-content-${dayNumber}" data-day="${dayNumber}">
                    <div class="event-container" id="day-${dayNumber}"></div>
                </div>
        `);

        groupedByDay[dayKey].forEach((event, index) => {
            const eventElement = createEventElement(event, index, groupedByDay[dayKey].length);
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
function createEventElement(event, index = null, totalEvents = null) {
    console.log("Event Object:", event);
    console.log("event.movingMinute", event.movingMinute);
    const itineraryEventDiv = $(`
                        <div class='event' data-id='${event.id}'>
                            <div class="event-wrapper">
                                <div class="travel-info">이동 시간 ${event.movingMinute}분</div>
                                <div class="event-content">
                                    <!-- 순서 번호 -->
                                    <div class="event-order"> 
                                        <div class="event-order-line top ${index === 0 ? "transparent" : ""}"></div>
                                        <div class="event-order-circle">${index + 1}</div>
                                        <div class="event-order-line bottom ${index === totalEvents - 1 ? "transparent" : ""}"></div>
                                    </div>
                                    <div class="event-main">                                        
                                        <div class='place-title'>${event.placeDTO.placeName}</div> 
                                            <div class='event-time'>${formatTime(event.startMinute)} ~ ${formatTime(event.endMinute)}</div>
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

// renderItinerary - 일정 드래그 & 드롭 활성화
function initializeSortable() {
    $(".event-container").each(function () {
        createSortableInstance(this);  // 뭐지?
    });
}

// initializeSortable - element 에 Sortable 추가
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
            let eventElement = createEventElement(event,null,null);
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

// createSortableInstance - 주어진 ID로 `eventMap`에서 이벤트 조회
function getEventById(id) {
    return eventMap.get(id) || null;
}

// createSortableInstance - 거리(시간) 계산 요청을 함수로 분리
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

// createSortableInstance - Event의 DayCount 상태 변경 함수
function changeDayCount(toDayId, newIndex) {
    const container = document.getElementById(toDayId);
    if (!container) return;
    const items = container.children;
    const eventElement = items[newIndex];
    const eventId = eventElement.getAttribute("data-id");
    const event = getEventById(eventId);
    event.dayCount = parseInt(toDayId.match(/\d+$/)[0]);
}

// calculateDistanceUpdates - 삭제 시 거리 재계산
function calculateRemovalImpact(dayId, oldIndex) {
    calculateDistanceByIndex(dayId, oldIndex - 1, oldIndex);
    return oldIndex;
}

// calculateDistanceUpdates - 추가 시 거리 재계산
function calculateInsertionImpact(dayId, newIndex) {
    calculateDistanceByIndex(dayId, newIndex - 1, newIndex);
    calculateDistanceByIndex(dayId, newIndex, newIndex + 1);
    return newIndex;
}

// calculateRemovalImpact, calculateDistanceUpdates - dayId 칼럼의 index1, index2의 거리 계산
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

// calculateDistanceByIndex - 거리 계산 요청 (임시 랜덤 값 반환)
function requestDistanceCalculation(placeId1, placeId2) {
    console.log(`🚗 거리(시간) 계산 요청: ${placeId1} → ${placeId2}`);

    // 랜덤 값 생성 (예제)
    const distance = Math.floor(Math.random() * 50) + 1; // 1 ~ 50km
    const minute = 30;//Math.floor(Math.random() * 120) + 1; // 1 ~ 120분

    return {distance, minute};
}

// createData - HH:MM:SS 문자열을 분(minute)으로 변환하는 함수
function timeToMinutes(timeStr) {
    const [hours, minutes] = timeStr.split(':').map(Number);
    return hours * 60 + minutes;
}

// createData - `eventMap`에 추가하고 ID 반환
function addEvent(event) {
    const id = generateUniqueId(eventMap);
    event.hashId = id;
    eventMap.set(id, event);
    return id;
}

// addEvent - 중복되지 않는 고유 ID 생성
function generateUniqueId(map, length = 10) {
    let id;
    do {
        id = generateHashCode(length);
    } while (map.has(id));
    return id;
}

// generateUniqueId - 지정된 길이의 랜덤 해시 코드 생성
function generateHashCode(length = 10) {
    const array = new Uint8Array(length);
    window.crypto.getRandomValues(array);
    return btoa(String.fromCharCode(...array)).replace(/[^a-zA-Z0-9]/g, '').substring(0, length);
}

// createEventElement - 분(minute) 값을 HH:MM 형태로 변환하는 함수
function formatTime(minutes) {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
}



/************ 🧳 사이드 바 크기 조절 🧳************/
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
    console.log("mousemove detected", e.pageX);
    let newWidth = e.pageX;
    if (newWidth >= 300 && newWidth <= 2000) {
        $("#left").css("width", newWidth + "px");
        $("#resize-handle").css("left", newWidth + "px");
    }
}

//마우스 버튼을 놓으면 크기 조절 종료
function stopSidebarResize() {
    console.log("mouseup detected, removing event listeners");
    $(document).off("mousemove", resizeSidebar);
    $(document).off("mouseup", stopSidebarResize);
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