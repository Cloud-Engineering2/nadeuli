window.isJournalModal = true;

// Event 전역변수
let itinerary = null;
const perDayMap = new Map();
const eventMap = new Map();
const eventIdToHashIdMap = new Map();
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
let summaryMap = new Map();
let totalBudget = null;
let totalExpense = null;
window.globaleventId = null;

const travelerMap = new Map();
// 모달 전역변수
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
    itineraryId = pathSegments[pathSegments.length - 1]; // 마지막 부분이 ID라고 가정

    $(document).on('click', '.navigate-edit-button', function () {
        window.location.href = `/itinerary/edit/${itineraryId}`;
    });

    apiWithAutoRefresh({
        url: `/api/itinerary/${itineraryId}`,
        method: "GET",
        dataType: "json",
        success: function (data) {
            itineraryData = data;
            createData(data);

            apiWithAutoRefresh({
                url: `/api/itineraries/${itineraryId}/expense-summary`,
                method: "GET",
                dataType: "json",
                success: function (expenseSummary) {
                    expenseSummary.summaries.forEach(item => {
                        summaryMap.set(item.eventId, item.totalExpense);
                    });
                    totalBudget = expenseSummary.totalBudget;
                    totalExpense = expenseSummary.totalExpenses;

                    console.log(summaryMap);
                    renderItinerary();
                    refreshJournalUI();
                    dataReady = true;
                    tryRenderMarkerAll();
                },
                error: function (xhr, status, error) {
                    console.error("Error fetching expense summary:", error);
                }
            });
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
        eventIdToHashIdMap.set(event.id, eventHashId);
        groupedByDay[dayKey].push({
            id: editedEvent.id,
            hashId: eventHashId,
            placeDTO: event.placeDTO,
            startMinute: baseStartMinutes + editedEvent.startMinuteSinceStartDay,
            endMinute: baseStartMinutes + editedEvent.endMinuteSinceStartDay,
            movingMinuteFromPrevPlace: event.movingMinuteFromPrevPlace,
            movingDistanceFromPrevPlace: event.movingDistanceFromPrevPlace
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

    renderTotalBudgetExpenseSummary();

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

    updateTabs();
}

function renderTotalBudgetExpenseSummary() {
    const $wrap = $('.total-budget-expense-wrap');
    $wrap.empty();

    // 예산 출력
    const budgetHtml = `
<!--        <div class="total-budget">예산: ${totalBudget.toLocaleString()} 원</div>-->
        <label class="total-budget-label" for="total-budget">예산 : </label>
        <input type="text" class="total-budget" id="totalBudget" name="name" placeholder="(원)">
        <button type="button" class="budget-confirm-button" id="budgetConfirmButton">
            <i class="fa-solid fa-check budget-confirm-icon"></i> <!-- 체크 아이콘 -->
        </button>
    `;

    // 지출/수익 계산
    $.ajax({
        url: `/api/itineraries/${itineraryId}/adjustment`,
        method: "GET",
        dataType: "json",
        success: function (response) {
            console.log("📝 장부 총 지출 업데이트 목적");
        }
    });
    let expenseHtml = '';
    if (totalExpense === 0) {
        expenseHtml = `<div class="total-expense">지출: 0 원</div>`;
    } else {
        const isProfit = totalExpense < 0;
        const displayAmount = isProfit ? `+ ${Math.abs(totalExpense).toLocaleString()}` : `- ${totalExpense.toLocaleString()}`;
        const colorClass = isProfit ? "profit-expense" : "cost-expense";

        expenseHtml = `<div class="total-expense ${colorClass}">지출: ${displayAmount} 원</div>`;
    }

    $wrap.append(budgetHtml);
    $wrap.append(expenseHtml);

    // 예산
    $.ajax({
        url: `/api/itineraries/${itineraryId}/expense-book`,
        method: "GET",
        dataType: "json",
        success: function (response) {
            if (response.totalBudget === 0) {

            }
            else {
                document.getElementById("budgetConfirmButton").style.display = "none";
                const div = document.createElement("div");
                div.textContent = response.totalBudget + " 원";
                div.style.fontWeight = "bold";
                div.style.marginTop = "5px";

                const budgetInput = document.getElementById("totalBudget");
                budgetInput.replaceWith(div);
            }
        }
    });
}


// 예산 입력 Enter 이벤트
$(document).on("click", ".budget-confirm-button", function() {
    // 변수
    let user = null;
    let userId = null;
    let userEmail = null;
    let travelerList = null;

    // 예산 입력값
    const budgetInput = document.getElementById("totalBudget");
    const budget = budgetInput.value.trim();

    // 예산 입력
    $.ajax({
        url: `/api/itineraries/${itineraryId}/budget`,
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            totalBudget: budget // 예산 값 설정
        }),
        success: function (expensebook) {

            const div = document.createElement("div");
            div.textContent = budget + " 원";
            budgetInput.replaceWith(div);
            div.style.fontWeight = "bold";
            div.style.marginTop = "5px";

            document.getElementById("budgetConfirmButton").style.display = "none";

            // user -> traveler에 추가
            $.ajax({  // user 조회
                url: `/api/itinerary/${itineraryId}/user/owner`,
                method: "GET",
                dataType: "json",
                success: function (userInfo) {
                    user = userInfo.userName;
                    userId = userInfo.id
                    userEmail = userInfo.userEmail;

                    $.ajax({  // traveler에 user가 존재하는지 확인
                        url: `/api/itinerary/${itineraryId}/travelers`,
                        method: "GET",
                        dataType: "json",
                        success: function (travelersInfo) {
                            travelerList = travelersInfo.travelers.map(traveler => traveler.name)
                            if (travelerList.includes(user)) {
                                console.log(`${user}는 여행자 목록에 있습니다.`);
                            } else {
                                console.log(`${user}를 여행자 목록에 추가합니다.`);

                                $.ajax({ // traveler에 추가
                                    url: `/api/itinerary/${itineraryId}/traveler`,
                                    method: "POST",
                                    contentType: "application/json",
                                    data: JSON.stringify({
                                        travelerName: user,
                                        totalBudget: budget // 예산 값 설정
                                    }),
                                    success: function (response) {
                                        console.log(`${user}를 여행자 목록에 추가했습니다.`);
                                        location.reload();
                                    },
                                    error: function (error) {
                                        console.error(`${user}를 여행자 목록에 추가하지 못했습니다`);
                                    }
                                });
                            }
                            console.log("여기는 여행자 목록");
                        },
                        error: function (error) {
                            console.error(`${user}를 조회하지 못했습니다`);
                        }
                    });
                },
                error: function (error) {
                    console.log(`${user}가 존재하지 않습니다`);
                }
            });
        },
        error: function (status, error) {
            console.log("예산 설정에 실패하였습니다");
        }
    });
});


// 이벤트 요소 생성 함수 (장소 보관함 & 일반 이벤트 공통 사용)
function createEventElement(event, index = null, totalEvents = null, isSavedPlace = false) {


    const totalExpense = summaryMap.get(event.id) ?? 0;
    console.log("Event Object:", event.id, totalExpense);
    let expenseHtml = '';
    if (totalExpense === 0) {
        expenseHtml = `
            <div class="expense-item-list-addition" id="expenseItemListAddition" data-iid='${itinerary.id}' data-ieid='${event.id}'>+ 경비 내역 추가</div>
        `;
    } else {
        const isProfit = totalExpense < 0;
        const displayAmount = isProfit ? Math.abs(totalExpense).toLocaleString() : `- ${totalExpense.toLocaleString()}`;
        const colorClass = isProfit ? "profit-expense" : "cost-expense";

        expenseHtml = `
            <div class="event-total-expense ${colorClass}" id="eventTotalExpense" data-iid='${itinerary.id}' data-ieid='${event.id}'>
                ${displayAmount} 원
            </div>
        `;
    }

    return $(`
                        <div class='event' data-id='${event.hashId}'>
                            <div class="event-wrapper">
                                <div class="travel-info input-inline">
                                    <div class="travel-info-left">
                                         ${isSavedPlace ? "" : `
                                        이동시간 ${event.movingMinuteFromPrevPlace}분
                                    `}(${formatDistance(event.movingDistanceFromPrevPlace)})
                                    </div>
                                    <div class="travel-info-mid">
                                    </div>
                                    

                                </div>
                                <div class="event-content">
                                    <div class="event-order">
                                        <div class="event-order-line top ${index === 0 ? "transparent" : ""}"></div>
                                        <div class="event-order-circle">${isSavedPlace ? "X" : index + 1}</div>
                                        <div class="event-order-line bottom ${index === totalEvents - 1 ? "transparent" : ""}"></div>
                                    </div>
                                    <div class="event-main" data-iid='${itinerary.id}' data-ieid='${event.id}'>
                                        <div class="event-left">
                                            ${isSavedPlace ? "" : `<div class='event-time'>${formatTime(event.startMinute)} ~ ${formatTime(event.endMinute)}</div>`}
                                            <div class='event-title'>${event.placeDTO.placeName}</div>
                                            <div class='event-place-type' data-place-type='${event.placeDTO.placeType}'>${getKoreanLabel(event.placeDTO.placeType)}</div>
                                            <div class="event-under-content">
                                               <div class="expense-wrap">
                                                    ${expenseHtml}
                                               </div>
                                            </div>
                                        </div>
                                        <div class="event-right">
                                            <div class="event-image-wrap">
                                                <img src="/images/journal_default.png" alt="event image" class="event-image" />
                                                <div class="event-memo-icon" data-active="false">
                                                    <i class="far fa-sticky-note"></i>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `);
}

function refreshExpenseSummary() {
    apiWithAutoRefresh({
        url: `/api/itineraries/${itineraryId}/expense-summary`,
        method: "GET",
        dataType: "json",
        success: function (expenseSummary) {
            summaries = expenseSummary.summaries;
            totalBudget = expenseSummary.totalBudget;
            totalExpense = expenseSummary.totalExpenses;
            summaryMap = new Map(summaries.map(item => [item.eventId, item.totalExpense]));

            renderTotalBudgetExpenseSummary();
            $('.day-column').each(function () {
                const $dayColumn = $(this);
                if ($dayColumn.hasClass('savedPlace')) return;

                $dayColumn.find('.event').each(function () {
                    const $event = $(this);
                    const hashId = $event.data('id');
                    const event = getEventById(hashId);
                    const eventId = event?.id;

                    const total = summaryMap.get(eventId) ?? 0;
                    const $wrap = $event.find('.expense-wrap');
                    $wrap.empty();

                    if (total === 0) {
                        $wrap.append(`
                            <div class="expense-item-list-addition" id="expenseItemListAddition" data-iid='${itineraryId}' data-ieid='${eventId}'>+ 경비 내역 추가</div>
                        `);
                    } else {
                        const isProfit = total < 0;
                        const displayAmount = isProfit ? Math.abs(total).toLocaleString() : `- ${total.toLocaleString()}`;
                        const colorClass = isProfit ? "profit-expense" : "cost-expense";

                        $wrap.append(`
                            <div class="event-total-expense ${colorClass}" id="eventTotalExpense" data-iid='${itineraryId}' data-ieid='${eventId}'>
                                ${displayAmount} 원
                            </div>
                        `);
                    }
                });
            });

            console.log("💰 Expense summary refreshed.");
        },
        error: function (xhr, status, error) {
            console.error("Error refreshing expense summary:", error);
        }
    });
}




function formatDistance(distanceInMeters) {
    if (distanceInMeters >= 1000) {
        return (distanceInMeters / 1000).toFixed(2) + ' km';
    } else {
        return Math.floor(distanceInMeters) + ' m';
    }
}



//day-? 에서 ? 추출
function extractDayId(toDayId) {
    const match = toDayId.match(/^day-(\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
}

//사이드바 크기 조절 기능
//사이드바 크기 조절 기능 초기화
// function initSidebarResize() {
//     $("#resize-handle").mousedown(function (e) {
//         e.preventDefault();
//         $(document).mousemove(resizeSidebar);
//         $(document).mouseup(stopSidebarResize);
//     });
// }
//
// //마우스 이동에 따라 사이드바 너비 조절
// function resizeSidebar(e) {
//     let newWidth = e.pageX;
//     if (newWidth >= 300 && newWidth <= 2000) {
//         $("#sidebar").css("width", newWidth + "px");
//         $("#resize-handle").css("left", newWidth + "px");
//     }
// }
//
// //마우스 버튼을 놓으면 크기 조절 종료
// function stopSidebarResize() {
//     $(document).off("mousemove", resizeSidebar);
//     $(document).off("mouseup", stopSidebarResize);
// }

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

$(".refresh-button").click(refresh_btn_func);

function refresh_btn_func() {
    refreshExpenseSummary();
    refreshJournalUI();
}

// 장소추가 관련 코드
// =================================================================


let sideMap;
let markers = [];
const placeTypeTranslations ={"car_dealer":"자동차 딜러","car_rental":"렌터카","car_repair":"자동차 정비소","car_wash":"세차장","electric_vehicle_charging_station":"전기차 충전소","gas_station":"주유소","parking":"주차장","rest_stop":"휴게소","corporate_office":"기업 사무실","farm":"농장","ranch":"목장","art_gallery":"미술관","art_studio":"예술 작업실","auditorium":"강당","cultural_landmark":"문화 랜드마크","historical_place":"유적지","monument":"기념비","museum":"박물관","performing_arts_theater":"공연 예술 극장","sculpture":"조각상","library":"도서관","preschool":"유치원","primary_school":"초등학교","school":"학교","secondary_school":"중·고등학교","university":"대학교","adventure_sports_center":"익스트림 스포츠 센터","amphitheatre":"원형 극장","amusement_center":"오락 센터","amusement_park":"놀이공원","aquarium":"수족관","banquet_hall":"연회장","barbecue_area":"바베큐 구역","botanical_garden":"식물원","bowling_alley":"볼링장","casino":"카지노","childrens_camp":"어린이 캠프","comedy_club":"코미디 클럽","community_center":"커뮤니티 센터","concert_hall":"콘서트 홀","convention_center":"컨벤션 센터","cultural_center":"문화 센터","cycling_park":"자전거 공원","dance_hall":"댄스홀","dog_park":"애견 공원","event_venue":"이벤트 장소","ferris_wheel":"대관람차","garden":"정원","hiking_area":"등산로","historical_landmark":"역사적 랜드마크","internet_cafe":"인터넷 카페","karaoke":"노래방","marina":"마리나 (항구)","movie_rental":"비디오 대여점","movie_theater":"영화관","national_park":"국립공원","night_club":"나이트클럽","observation_deck":"전망대","off_roading_area":"오프로드 지역","opera_house":"오페라 하우스","park":"공원","philharmonic_hall":"필하모닉 홀","picnic_ground":"소풍 장소","planetarium":"천문관","plaza":"광장","roller_coaster":"롤러코스터","skateboard_park":"스케이트 공원","state_park":"주립공원","tourist_attraction":"관광명소","video_arcade":"비디오 아케이드","visitor_center":"방문자 센터","water_park":"워터파크","wedding_venue":"웨딩홀","wildlife_park":"야생동물 공원","wildlife_refuge":"야생동물 보호구역","zoo":"동물원","public_bath":"대중목욕탕","public_bathroom":"공중화장실","stable":"마구간","accounting":"회계 사무소","atm":"ATM","bank":"은행","acai_shop":"아사이 볼 전문점","afghani_restaurant":"아프가니스탄 음식점","african_restaurant":"아프리카 음식점","american_restaurant":"아메리칸 레스토랑","asian_restaurant":"아시안 레스토랑","bagel_shop":"베이글 가게","bakery":"베이커리","bar":"바","bar_and_grill":"바 & 그릴","barbecue_restaurant":"바베큐 레스토랑","brazilian_restaurant":"브라질 음식점","breakfast_restaurant":"조식 전문점","brunch_restaurant":"브런치 레스토랑","buffet_restaurant":"뷔페 레스토랑","cafe":"카페","cafeteria":"구내식당","candy_store":"캔디샵","cat_cafe":"고양이 카페","chinese_restaurant":"중식당","chocolate_factory":"초콜릿 공장","chocolate_shop":"초콜릿 가게","coffee_shop":"커피숍","confectionery":"과자점","deli":"델리","dessert_restaurant":"디저트 레스토랑","dessert_shop":"디저트 가게","diner":"다이너","dog_cafe":"강아지 카페","donut_shop":"도넛 가게","fast_food_restaurant":"패스트푸드점","fine_dining_restaurant":"파인다이닝 레스토랑","food_court":"푸드코트","french_restaurant":"프랑스 음식점","greek_restaurant":"그리스 음식점","hamburger_restaurant":"햄버거 가게","ice_cream_shop":"아이스크림 가게","indian_restaurant":"인도 음식점","indonesian_restaurant":"인도네시아 음식점","italian_restaurant":"이탈리아 음식점","japanese_restaurant":"일식당","juice_shop":"주스 전문점","korean_restaurant":"한식당","lebanese_restaurant":"레바논 음식점","meal_delivery":"배달 전문점","meal_takeaway":"테이크아웃 전문점","mediterranean_restaurant":"지중해 음식점","mexican_restaurant":"멕시코 음식점","middle_eastern_restaurant":"중동 음식점","pizza_restaurant":"피자 가게","pub":"펍","ramen_restaurant":"라멘 전문점","restaurant":"레스토랑","sandwich_shop":"샌드위치 가게","seafood_restaurant":"해산물 레스토랑","spanish_restaurant":"스페인 음식점","steak_house":"스테이크 하우스","sushi_restaurant":"스시 레스토랑","tea_house":"찻집","thai_restaurant":"태국 음식점","turkish_restaurant":"터키 음식점","vegan_restaurant":"비건 레스토랑","vegetarian_restaurant":"채식 레스토랑","vietnamese_restaurant":"베트남 음식점","wine_bar":"와인 바","administrative_area_level_1":"광역 행정구역","administrative_area_level_2":"지방 행정구역","country":"국가","locality":"지역","postal_code":"우편번호","school_district":"학군","city_hall":"시청","courthouse":"법원","embassy":"대사관","fire_station":"소방서","government_office":"정부 기관","local_government_office":"지방 정부 기관","neighborhood_police_station":"지구대 (일본만 해당)","police":"경찰서","post_office":"우체국","chiropractor":"카이로프랙틱","dental_clinic":"치과 클리닉","dentist":"치과 의사","doctor":"의사","drugstore":"약국","hospital":"병원","massage":"마사지샵","medical_lab":"의료 실험실","pharmacy":"약국","physiotherapist":"물리 치료사","sauna":"사우나","skin_care_clinic":"피부 관리 클리닉","spa":"스파","tanning_studio":"태닝 스튜디오","wellness_center":"웰니스 센터","yoga_studio":"요가 스튜디오","apartment_building":"아파트 건물","apartment_complex":"아파트 단지","condominium_complex":"콘도미니엄 단지","housing_complex":"주택 단지","bed_and_breakfast":"B&B 숙소","budget_japanese_inn":"일본 저가 숙소","campground":"캠핑장","camping_cabin":"캠핑용 오두막","cottage":"코티지","extended_stay_hotel":"장기 체류 호텔","farmstay":"팜스테이","guest_house":"게스트하우스","hostel":"호스텔","hotel":"호텔","inn":"여관","japanese_inn":"료칸","lodging":"숙박시설","mobile_home_park":"이동식 주택 단지","motel":"모텔","private_guest_room":"개인 게스트룸","resort_hotel":"리조트 호텔","rv_park":"RV 주차장","beach":"해변","church":"교회","hindu_temple":"힌두교 사원","mosque":"모스크","synagogue":"유대교 회당","astrologer":"점성술사","barber_shop":"이발소","beautician":"미용 전문가","beauty_salon":"미용실","body_art_service":"바디아트 서비스","catering_service":"출장 요리 서비스","cemetery":"공동묘지","child_care_agency":"보육 기관","consultant":"컨설팅 서비스","courier_service":"택배 서비스","electrician":"전기 기사","florist":"꽃집","food_delivery":"음식 배달 서비스","foot_care":"발 관리 서비스","funeral_home":"장례식장","hair_care":"헤어 관리","hair_salon":"미용실","insurance_agency":"보험 대리점","laundry":"세탁소","lawyer":"변호사","locksmith":"열쇠 수리점","makeup_artist":"메이크업 아티스트","moving_company":"이사 업체","nail_salon":"네일숍","painter":"도장업체","plumber":"배관공","psychic":"심령술사","real_estate_agency":"부동산 중개업","roofing_contractor":"지붕 공사업체","storage":"창고","summer_camp_organizer":"여름 캠프 기획사","tailor":"재단사","telecommunications_service_provider":"통신 서비스 제공업체","tour_agency":"여행사","tourist_information_center":"관광 안내소","travel_agency":"여행사","veterinary_care":"동물 병원","asian_grocery_store":"아시아 식료품점","auto_parts_store":"자동차 부품 상점","bicycle_store":"자전거 가게","book_store":"서점","butcher_shop":"정육점","cell_phone_store":"휴대폰 매장","clothing_store":"의류 매장","convenience_store":"편의점","department_store":"백화점","discount_store":"할인 매장","electronics_store":"전자제품 매장","food_store":"식료품점","furniture_store":"가구 매장","gift_shop":"기념품 가게","grocery_store":"슈퍼마켓","hardware_store":"철물점","home_goods_store":"생활용품 매장","home_improvement_store":"DIY/인테리어 매장","jewelry_store":"보석 가게","liquor_store":"주류 판매점","market":"시장","pet_store":"애완동물 가게","shoe_store":"신발 가게","shopping_mall":"쇼핑몰","sporting_goods_store":"스포츠 용품점","store":"상점","supermarket":"대형 마트","warehouse_store":"창고형 매장","wholesaler":"도매점","arena":"경기장","athletic_field":"운동장","fishing_charter":"낚시 여행","fishing_pond":"낚시터","fitness_center":"헬스장","golf_course":"골프장","gym":"체육관","ice_skating_rink":"아이스링크","playground":"놀이터","ski_resort":"스키 리조트","sports_activity_location":"스포츠 활동 장소","sports_club":"스포츠 클럽","sports_coaching":"스포츠 코칭 센터","sports_complex":"스포츠 복합 시설","stadium":"스타디움","swimming_pool":"수영장","airport":"공항","airstrip":"소형 비행장","bus_station":"버스 터미널","bus_stop":"버스 정류장","ferry_terminal":"페리 터미널","heliport":"헬리포트","international_airport":"국제공항","light_rail_station":"경전철 역","park_and_ride":"환승 주차장","subway_station":"지하철역","taxi_stand":"택시 승강장","train_station":"기차역","transit_depot":"교통 환승센터","transit_station":"대중교통 환승역","truck_stop":"트럭 정류장","administrative_area_level_3":"행정구역 (레벨 3)","administrative_area_level_4":"행정구역 (레벨 4)","administrative_area_level_5":"행정구역 (레벨 5)","administrative_area_level_6":"행정구역 (레벨 6)","administrative_area_level_7":"행정구역 (레벨 7)","archipelago":"군도","colloquial_area":"비공식 지역명","continent":"대륙","establishment":"시설","finance":"금융","floor":"층","food":"음식","general_contractor":"종합 건설업체","geocode":"지리적 코드","health":"건강","intersection":"교차로","landmark":"랜드마크","natural_feature":"자연 지형","neighborhood":"주변 지역","place_of_worship":"예배 장소","plus_code":"플러스 코드","point_of_interest":"관심 지점","political":"정치적 구역","post_box":"우편함","postal_code_prefix":"우편번호 접두사","postal_code_suffix":"우편번호 접미사","postal_town":"우편도시","premise":"건물","room":"방","route":"경로","street_address":"도로명 주소","street_number":"도로명 주소 번호","sublocality":"하위 지역","sublocality_level_1":"하위 지역 (레벨 1)","sublocality_level_2":"하위 지역 (레벨 2)","sublocality_level_3":"하위 지역 (레벨 3)","sublocality_level_4":"하위 지역 (레벨 4)","sublocality_level_5":"하위 지역 (레벨 5)","subpremise":"건물 내 구역","town_square":"타운 스퀘어"};
const excludedPlaceTypes = ["administrative_area_level_1", "administrative_area_level_2", "administrative_area_level_3", "administrative_area_level_4", "administrative_area_level_5", "administrative_area_level_6", "administrative_area_level_7", "colloquial_area", "continent", "country", "locality", "neighborhood", "political", "postal_code", "postal_code_prefix", "postal_code_suffix", "postal_town", "school_district", "sublocality", "sublocality_level_1", "sublocality_level_2", "sublocality_level_3", "sublocality_level_4", "sublocality_level_5", "plus_code", "establishment", "floor", "premise", "subpremise", "room", "street_address", "street_number", "intersection", "route", "corporate_office", "general_contractor", "real_estate_agency", "insurance_agency", "lawyer", "accounting", "finance", "storage", "telecommunications_service_provider", "moving_company", "electrician", "plumber", "roofing_contractor", "courier_service", "warehouse_store", "wholesaler", "auto_parts_store", "butcher_shop", "beauty_salon", "nail_salon", "hair_salon", "barber_shop", "tanning_studio", "makeup_artist", "foot_care", "psychic", "astrologer", "apartment_building", "apartment_complex", "condominium_complex", "housing_complex", "mobile_home_park", "church", "hindu_temple", "mosque", "synagogue", "place_of_worship", "chiropractor", "physiotherapist", "skin_care_clinic", "medical_lab", "wellness_center", "child_care_agency", "summer_camp_organizer", "consultant", "painter", "tailor", "point_of_interest"]


// ➡️ 나들이 장소 검색 관련
//===============================================================================


//필터 타입 맵핑
function getKoreanLabel(filterType) {
    const filterMap = {
        LANDMARK: "명소",
        RESTAURANT: "식당",
        LODGING: "숙소",
        CAFE: "카페",
        TRANSPORTATION: "교통",
        ATTRACTION: "여가 시설",
        CONVENIENCE: "편의 시설"
    };

    return filterMap[filterType] || "알 수 없음";
}



// ➡️ 구글 Place TextSearch 관련
//===============================================================================

// 구글 Place 장소타입 한글 변환 함수
function translatePlaceTypes(types) {
    return types
        .filter(type => !excludedPlaceTypes.includes(type)) // 제외 리스트 필터링
        .map(type => placeTypeTranslations[type] || `#${type.replace(/_/g, ' ')}`); // 한글 변환 or 해시태그 스타일
}


// ➡️ 기타 유틸리티
//=======================================================================

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
    // 최초 아이콘/라벨 정보 저장
    if (!marker._originalIcon) {
        marker._originalIcon = marker.getIcon();
    }
    if (!marker._originalLabel) {
        marker._originalLabel = marker.getLabel();
    }

    // 기존 타이머 클리어
    if (marker._resetTimerId) {
        clearTimeout(marker._resetTimerId);
        marker._resetTimerId = null;
    }

    const originalIcon = marker._originalIcon;
    const originalLabel = marker._originalLabel;

    // 확대 아이콘
    const biggerIcon = {
        ...originalIcon,
        scale: (originalIcon.scale || 1) * scaleFactor
    };

    // 확대 라벨
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




$(document).on("click", ".schedule-header-left", function () {
    markerState=0;
    renderMarkerByMarkerState();
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



// // 수정후 브라우저 뒤로가기,나가기, 새로고침시 경고 메세지
// window.addEventListener("beforeunload", function (e) {
//     if (isDirty) {
//         e.preventDefault();  // 크롬 기준 필요
//         e.returnValue = '저장되지 않은 변경 사항이 있습니다. 정말 페이지를 나가시겠습니까?';
//     }
// });
//
// // 수정후 링크 이동시 경고 메세지
// function handleDirtyNavigation(targetUrl) {
//     if (!isDirty) {
//         window.location.href = targetUrl;
//         return;
//     }
//
//     Swal.fire({
//         title: '저장되지 않은 변경사항이 있습니다.',
//         icon: 'warning',
//         showCancelButton: true,
//         confirmButtonText: '나가기',
//         cancelButtonText: '취소',
//         reverseButtons: true,
//         customClass: {
//             title: 'swal2-sm-title'
//         }
//     }).then((result) => {
//         if (result.isConfirmed) {
//             window.location.href = targetUrl;
//         }
//     });
// }


// 수정후 링크 이동시 경고 메세지 event 핸들러
// $("a[href]").click(function(e) {
//     const href = $(this).attr("href");
//     const target = $(this).attr("target");
//
//     if (!href || e.ctrlKey || e.metaKey || target === "_blank") return;
//
//     e.preventDefault();
//     handleDirtyNavigation(href);
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
                             alt="장소 사진" 
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

/* 여행자, 예산 추가 */
// 모달 닫기 버튼 클릭 시 모달 숨기기
document.querySelector(".traveler-close").addEventListener("click", function() {
    document.getElementById("travelerModal").style.display = "none";
});

// 모달 바깥 부분 클릭 시 모달 닫기
window.addEventListener("click", function(event) {
    if (event.target === document.getElementById("travelerModal")) {
        document.getElementById("travelerModal").style.display = "none";
    }
});

// 모달 창 열기
$(document).on("click", ".traveler-addition-button", function() {
    // 여행 ID 가져오기

    document.getElementById("travelerModal").style.display = "block";
    // 리스트 조회
    loadTravelerList();

});

// 여행자 추가 모달
document.getElementById("travelerSendButton").addEventListener("click", function() {

    const travelerName = document.getElementById("travelerName").value;
    // const travelerBudget = document.getElementById("travelerBudget").value;

    if (travelerName) { // 이름과 예산 모두 입력
        // 서버로 전송하는 경우
        $.ajax({
            url: `/api/itinerary/${itineraryId}/traveler`,
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                travelerName: travelerName,
                // totalBudget: travelerBudget // 예산 값 설정
            }),
            success: function (response) {
                console.log("여행자 추가 성공:", response);
                loadTravelerList();
            },
            error: function (error) {
                const errorMessage = error.responseJSON?.message || "여행자 추가 중 오류가 발생했습니다.";
                alert(errorMessage);
            }
        });
    }
});


// 여행자 삭제 버튼 클릭 시 처리
$(document).on("click", ".traveler-delete-button", function () {
    const iid = $(this).data("iid"); // 여행 ID
    const tid = $(this).data("tid"); // 여행자 ID
    const travelerName = $(this).data("tname"); // 여행자 이름

    if (!confirm(`${travelerName} 여행자를 정말 삭제하시겠습니까?`)) {
        return;
    }

    // 여행자 삭제 요청
    $.ajax({
        url: `/api/itinerary/${iid}/traveler/${tid}`,
        method: "DELETE",
        dataType: "json",
        success: function (response) {
            console.log("여행자 삭제 성공:", response);
            loadTravelerList();
        },
        error: function (error) {
            const errorMessage = error.responseJSON?.message || "여행자 삭제 중 오류가 발생했습니다.";
            alert(errorMessage);
        }
    });
});

// 여행자 수정 버튼 클릭 시 처리
$(document).off("click", ".traveler-edit-button").on("click", ".traveler-edit-button", async function() {
    const iid = $(this).data("iid"); // 여행 ID
    const tid = $(this).data("tid"); // 여행자 ID
    const travelerName = $(this).data("tname"); // 여행자 이름

    const travelerBox = $(this).closest(".traveler-box"); // 해당 버튼이 속한 여행자 박스를 찾음
    const travelerNameContent = travelerBox.find(".traveler-name")[0]; // 해당 박스 내의 .traveler-name 요소 찾기
    travelerNameContent.setAttribute("contenteditable", "true");
    const wrap = travelerBox.find(".traveler-name-wrap");
    wrap.addClass("traveler-editing");
    travelerBox.find(".traveler-edit-button").hide(); // 수정 버튼 숨기기
    travelerBox.find(".traveler-confirm-button").show(); // 확인 버튼 보이기
});

// 여행자 수정 후 체크버튼 클릭 시 처리
$(document).off("click", ".traveler-confirm-button").on("click", ".traveler-confirm-button", async function () {
    const iid = $(this).data("iid");
    const tid = $(this).data("tid");

    const travelerBox = $(this).closest(".traveler-box");
    const travelerNameContent = travelerBox.find(".traveler-name")[0];
    travelerNameContent.setAttribute("contenteditable", "false");
    const newTravelerName = travelerBox.find(".traveler-name")[0].innerText.trim();

    if (!newTravelerName) {
        alert("이름을 입력해주세요.");
        return;
    }

    // 서버로 수정된 이름 전송
    $.ajax({
        url: `/api/itinerary/${iid}/traveler/${tid}/change-name`,
        method: "PUT",
        contentType: "application/json",
        data: JSON.stringify({ name: newTravelerName }),
        success: function (response) {
            console.log("이름 수정 성공", response);

            const wrap = travelerBox.find(".traveler-name-wrap");
            wrap.removeClass("traveler-editing");

            travelerBox.find(".traveler-edit-button").show();
            travelerBox.find(".traveler-confirm-button").hide();
            loadTravelerList();
        },
        error: function (error) {
            const errorMessage = error.responseJSON?.message || "이름 수정 중 오류가 발생했습니다.";
            alert(errorMessage);
            // 수정 실패했으니 다시 편집 모드로
            travelerNameContent.setAttribute("contenteditable", "true");
        }
    });
});



// 여행자 목록을 모달에 로드하는 함수
function loadTravelerList() {

    $.ajax({
        url: `/api/itinerary/${itineraryId}/travelers`,
        method: "GET",
        dataType: "json",
        success: function(response) {
            const travelerList = document.getElementById("travelerList");
            travelerList.innerHTML = ""; // 기존 리스트 초기화
            travelerMap.clear(); // 기존 travelerMap도 초기화

            // 여행자 배열을 순회하여 HTML 요소로 추가
            travelerList.innerHTML =
                response.travelers.map(traveler => {
                    const isDeletable = !(traveler.consumer || traveler.payer); // 삭제 가능 여부
                    travelerMap.set(traveler.id, traveler);
                return `
                <div class="traveler-box" id="travelerBox" style="display: flex; align-items: center;">
                    <div class="traveler-name-wrap">
                        <div class="traveler-name">${traveler.name}</div>
                        <button type="button" class="traveler-edit-button" data-iid="${itineraryId}" data-tid="${traveler.id}" data-tname="${traveler.name}">
                            <i class="fa-solid fa-pen traveler-edit-icon"></i>
                        </button>
                        <button type="button" class="traveler-confirm-button" style="display: none;" data-iid="${itineraryId}" data-tid="${traveler.id}" data-tname="${traveler.name}">
                            <i class="fa-solid fa-check traveler-confirm-icon"></i>
                        </button>
                    </div>
        
                    <div class="traveler-budget-wrap">
                        <label>예산: </label>
                        <input type="number" class="traveler-budget-input" value="${traveler.totalBudget}" readonly>
                        <button type="button" class="traveler-budget-edit-button" data-iid="${itineraryId}" data-tid="${traveler.id}">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <button type="button" class="traveler-budget-confirm-button" style="display: none;" data-iid="${itineraryId}" data-tid="${traveler.id}">
                            <i class="fa-solid fa-check"></i>
                        </button>
                    </div>
        
                    <button type="button" class="traveler-delete-button ${isDeletable ? "" : "disabled"}"
                            
                            data-iid="${itineraryId}" data-tid="${traveler.id}" data-tname="${traveler.name}">
                            <i class="fa fa-trash traveler-delete-icon"></i>
                        </button>
                </div>
                `;
                        }).join("");

        },
        error: function(error) {
            console.error("여행자 목록 조회 실패:", error);
        }
    });
}


$(document).on("click", ".toggle-map-button", function () {
    const $mapPanel = $(".right-side-map");
    const $expensePanel = $(".right-side-expense");
    // const $resizeHandle = $("#resize-handle");
    $mapPanel.toggleClass("hidden");
    $expensePanel.toggleClass("expand");
    // $resizeHandle.toggleClass("hidden");
    $(this).toggleClass("on");
    isMapPanelOpen = $(this).hasClass("on"); // 👉 상태 동기화
    // 툴팁 변경
    if ($(this).hasClass("on")) {
        $(this).attr("title", "지도 숨기기");
        // const sidebarWidth = $("#sidebar").outerWidth();
        // $resizeHandle.css("left", sidebarWidth + "px");
    } else {
        $(this).attr("title", "지도 보기");
    }
});



function updateTabs() {
    const tabContainer = document.querySelector(".tab-wrapper");
    tabContainer.innerHTML = ""; // 기존 탭 초기화

    // "전체 일정" 탭 추가
    let allTab = document.createElement("span");
    allTab.classList.add("tab-btn");
    allTab.textContent = "전체 일정";
    allTab.setAttribute("data-day", "all");
    allTab.addEventListener("click", function () {
        showSchedule("all");
    });
    tabContainer.appendChild(allTab);

    // 여행 일차별 탭 추가
    for (let i = 1; i <= itinerary.totalDays; i++) {
        let tab = document.createElement("span");
        tab.classList.add("tab-btn");
        tab.textContent = `${i}일차`;
        tab.setAttribute("data-day", i);
        tab.addEventListener("click", function () {
            showSchedule(i);
        });
        tabContainer.appendChild(tab);
    }
}

// 특정 날짜의 탭 표시
function showSchedule(day) {
    const allEvents = document.querySelectorAll(".event-container");

    allEvents.forEach(container => {
        container.parentElement.style.display = ((day === "all" && container.id !== 'day-0') || container.id === `day-${day}`) ? "block" : "none";
    });
}


document.addEventListener("DOMContentLoaded", function () {
    const tabContainer = document.querySelector(".tab-container");
    let isDragging = false;
    let startX, scrollLeft;

    tabContainer.addEventListener("mousedown", (e) => {
        isDragging = true;
        startX = e.pageX - tabContainer.offsetLeft;
        scrollLeft = tabContainer.scrollLeft;
        tabContainer.style.cursor = "grabbing"; // 드래그 중 손 모양 변경
    });

    tabContainer.addEventListener("mouseleave", () => {
        isDragging = false;
        tabContainer.style.cursor = "grab"; // 원래 손 모양으로 복구
    });

    tabContainer.addEventListener("mouseup", () => {
        isDragging = false;
        tabContainer.style.cursor = "grab";
    });

    tabContainer.addEventListener("mousemove", (e) => {
        if (!isDragging) return;
        e.preventDefault();
        const x = e.pageX - tabContainer.offsetLeft;
        const walk = (x - startX) * 0.8; // 드래그 속도 조절
        tabContainer.scrollLeft = scrollLeft - walk;
    });
});

function refreshJournalUI() {
    apiWithAutoRefresh({
        url: `/api/itineraries/${itineraryId}/journals`,
        method: "GET",
        dataType: "json",
        success: function (journals) {
            const journalMap = new Map();
            journals.forEach(j => {
                if (j && j.ieid) {
                    journalMap.set(Number(j.ieid), j);
                }
            });

            console.log('journalMap', journalMap);
            eventIdToHashIdMap.forEach((hashId, eventId) => {
                console.log(`hashId ${hashId}, eventId ${eventId}`);
                const journal = journalMap.get(eventId);
                console.log(`journal`, journal);
                const $eventEl = $(`.event[data-id='${hashId}']`);
                const $imageEl = $eventEl.find(".event-image");
                const $memoIconEl = $eventEl.find(".event-memo-icon");
                console.log($eventEl);

                // 이미지 최적화 렌더링
                const targetImageUrl = journal?.imageUrl || "/images/journal_default.png";
                console.log($imageEl.attr("src"), targetImageUrl);
                if ($imageEl.attr("src") !== targetImageUrl) {
                    $imageEl.attr("src", targetImageUrl);
                }

                // 메모 아이콘 상태 최적화 렌더링
                const hasContent = journal && journal.content && journal.content.trim() !== "";
                const currentActive = $memoIconEl.attr("data-active") === "true";

                if (hasContent !== currentActive) {
                    $memoIconEl.attr("data-active", hasContent ? "true" : "false");
                    $memoIconEl.toggleClass("active", hasContent);
                }
            });

            console.log("📘 Journal UI refreshed with optimized DOM updates.");
        },
        error: function (xhr, status, error) {
            console.error("Error loading journals:", error);
        }
    });
}
window.refreshJournalUI = refreshJournalUI;
$(document).on("click", ".event-image-wrap", async function () {
    const hashId = $(this).closest(".event").data("id");
    const event = getEventById(hashId);
    if (!event || !event.id) {
        console.warn("해당 방문지 정보를 찾을 수 없습니다.");
        return;
    }

    const eventId = event.id;
    console.log("🖼 클릭한 방문지 ID:", eventId, ", hashId:", hashId);

    await fetchJournal(itineraryId, eventId); // 저널 데이터 가져오기
    openJournalModal(); // 모달 열기
});


$(document).off("click", ".traveler-budget-edit-button").on("click", ".traveler-budget-edit-button", function() {
    const budgetWrap = $(this).closest(".traveler-budget-wrap");
    const input = budgetWrap.find(".traveler-budget-input");

    input.prop("readonly", false);
    input.focus();
    budgetWrap.addClass("traveler-budget-editing");
    budgetWrap.find(".traveler-budget-edit-button").hide();
    budgetWrap.find(".traveler-budget-confirm-button").show();
});

$(document).off("click", ".traveler-budget-confirm-button").on("click", ".traveler-budget-confirm-button", function () {
    const iid = $(this).data("iid");
    const tid = $(this).data("tid");

    const budgetWrap = $(this).closest(".traveler-budget-wrap");
    const input = budgetWrap.find(".traveler-budget-input");
    const newBudget = parseFloat(input.val());

    if (isNaN(newBudget) || newBudget < 0) {
        alert("유효한 예산을 입력해주세요.");
        return;
    }

    // 서버로 예산 수정 요청
    $.ajax({
        url: `/api/itinerary/${iid}/traveler/${tid}/change-budget`,
        method: "PUT",
        contentType: "application/json",
        data: JSON.stringify({ totalBudget: newBudget }),
        success: function (response) {
            console.log("예산 수정 성공:", response);

            // UI 업데이트
            budgetWrap.removeClass("traveler-budget-editing");
            input.prop("readonly", true);
            budgetWrap.find(".traveler-budget-edit-button").show();
            budgetWrap.find(".traveler-budget-confirm-button").hide();
            loadTravelerList();
            // 예산 텍스트 혹시 동기화 필요 시 여기에 적용 가능
            // 예: budgetWrap.find(".traveler-budget-display").text(newBudget.toLocaleString());
        },
        error: function (error) {
            const errorMessage = error.responseJSON?.message || "예산 수정 중 오류가 발생했습니다.";
            alert(errorMessage);

            // 수정 실패 시 편집 상태 유지
            input.prop("readonly", false);
        }
    });
});
