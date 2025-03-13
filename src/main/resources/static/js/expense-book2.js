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

    // initializeSortable(); // 드래그 & 드롭 기능 활성화
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

// 🎈 일정 드래그 & 드롭 활성화
function initializeSortable() {
    $(".event-container").each(function () {
        // createSortableInstance(this);  // 뭐지?
    });
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