/* bottomline.js
 * nadeuli Service - 여행
 * bottomline.html 에서 사용할 js 함수 정리
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.03.10
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.03.10     최초 작성 : bottomline.js
 * 이홍비    2025.03.12     방문지 선택 => 기행문 조회 처리
 * 이홍비    2025.03.13     방문지 선택한 상태 - 이전, 다음 방문지 조회 버튼 관련 처리
 * 이홍비    2025.03.14     기행문 - 본문 출력 시 왼쪽 정렬 + 줄 바꿈 유지
 *                         방문지 선택 => 그에 해당하는 경비 출력
 *                         지도 - 기행문 - 경비 연동 완료
 *                         style.whiteSpace = "pre"; 추가 => 내어 쓰기 느낌
 * ========================================================
 */

let this_iid;
let this_ieid;
let this_index; // 현재 선택한 방문지 번호

// map 쪽
let map;
let markerList = [];
let pathCoordinates = [];
let selectedMarker = null;
let selectedPlace = null;
let infowindow;

let itineraryTotalRead;
let ieidList = [];

let travler;

let finalSettlement; // 방문지 선택 x - 최종 정산
let partialSettlement; // 방문지 선택 o - 방문지별 정산
let journal; // 방문지 선택 o - 기행문
let expense; //



// 시간 출력 형태 -
// fetchJournal(), next() 사용
const timeFormat = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false // 24시간
}

async function fetchBottomLine(iid) {
    this_iid = iid;

    const response = await fetch(`/api/itineraries/${iid}/bottomline`);
    itineraryTotalRead = await response.json();
    travler = itineraryTotalRead.travelerList;
    finalSettlement = itineraryTotalRead.finalSettlement;

    console.log("fetchBottomLine - itineraryTotalRead : ", itineraryTotalRead); // 데이터 확인
    console.log("Itinerary Total : ", itineraryTotalRead.itineraryTotal);
    console.log("Traveler List : ", itineraryTotalRead.travelerList);
    console.log("Final Settlement : ", itineraryTotalRead.finalSettlement);
    // console.log("Expense Book : ", itineraryTotalRead.expenseBook);

    noChoice();

    // axios.get(`/api/itineraries/${this_iid}/adjustment`)
    //     .then(response => {
    //         adjustment = response.data;
    //         console.log("adjustment : ", adjustment);
    //     })
    //     .catch(error => console.error("Error saving content:", error));

    initMap();

    // updateContentView();
    // updatePhotoView();
}

window.onload = function() {

    loadGoogleMapsApi();

    // iid 추출 과정
    const pathParts = window.location.pathname.split("/");
    console.log("pathParts:", pathParts); // pathParts 값 출력

    // 예상 URL 구조: /itineraries/{iid}/bottomline
    const iid = pathParts[2]; // 2번째 요소 (0-based index)
    // const ieid = pathParts[4]; // 4번째 요소

    console.log("iid : ", iid);
    // console.log("ieid : ", ieid);

    fetchBottomLine(iid);
};


// 지도 관련

// google map api key
function loadGoogleMapsApi() {
    fetch('/api/google-places/apikey')
        .then(response => response.text())
        .then(apiKey => {
            let script = document.createElement("script");
            script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places&callback=initMap`;
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
            initMap();
        })
        .catch(error => console.error("API Key 가져오기 실패:", error));
}

// 맵 초기화
function initMap() {
    if (!itineraryTotalRead || !itineraryTotalRead.itineraryTotal || !itineraryTotalRead.itineraryTotal.itineraryEvents.length) {
        console.error("🚨 방문지 데이터가 없습니다.");
        return;
    }

    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: 37.5665, lng: 126.9780 }, // 기본 서울 중심
        // center: centerLatLng,
        zoom: 10
    });

    console.log("itineraryTotalRead.itineraryTotal.itineraryEvents : " + itineraryTotalRead.itineraryTotal.itineraryEvents);

    infowindow = new google.maps.InfoWindow(); // 정보 창 초기화

    let latitude = 0;
    let longitude = 0;
    const itineraryEventList = itineraryTotalRead.itineraryTotal.itineraryEvents;
    itineraryEventList.forEach((event, index) => {
        console.log("event : ", event);
        console.log("index : ", index);
        addMarker(event.placeDTO, index + 1); // 방문 순서대로 번호 추가

        ieidList.push(event.id);

        latitude += event.placeDTO.latitude;
        longitude += event.placeDTO.longitude;
    });
    console.log("markerList : ", markerList);


    let avgLatitude = latitude / itineraryEventList.length;
    let avgLongitude = longitude / itineraryEventList.length;
    let centerLatLng = { lat: avgLatitude, lng: avgLongitude };

    map.setCenter(centerLatLng);
    console.log("📌 지도 중심 위치:", centerLatLng);

    noChoice();
}

function addMarker(place, order) {
    const marker = new google.maps.Marker({
        position: { lat: place.latitude, lng: place.longitude },
        map,
        title: place.placeName,
        label: {
            text: order.toString(), // 순서 숫자
            color: "black",         // 라벨 텍스트 색상
            fontSize: "14px",       // 라벨 텍스트 크기
            fontWeight: "bold"      // 라벨 텍스트 굵기
        },
        // icon: "http://maps.google.com/mapfiles/ms/icons/blue-dot.png" // 기본 파란색 마커
        icon: getMarkerIcon('skyblue')
    });

    // marker.addListener("click", () => toggleMarker(marker));
    marker.addListener("click", () => {
        toggleMarker(marker);

        if (selectedMarker !== null) {
            const content = `
<!--            <div style="text-align: center;">-->
            <div class="place-info">
                <img src="${place.imageUrl}" style="width:80%; max-width:300px; margin-bottom:10px;" alt="">
                <h2>${place.placeName}</h2>
                <p>${place.address}</p>
            </div>
        `;
            infowindow.setContent(content);
            infowindow.open(map, marker);
        }
    });

    markerList.push(marker);

    // 경로 좌표 추가
    pathCoordinates.push({ lat: place.latitude, lng: place.longitude });

    // 경로를 지도에 그리기
    const path = new google.maps.Polyline({
        path: pathCoordinates,
        geodesic: true,
        strokeColor: "#FF0000",
        strokeOpacity: 1.0,
        strokeWeight: 2
    });
    path.setMap(map);
}

function toggleMarker(marker) {
    if (selectedMarker === marker) {
        // 이미 선택된 장소 => 해제

        if (infowindow) {
            // 장소 정보 출력 창 켜져 있으면 꺼지도록
            infowindow.close();
        }

        // marker.setIcon("http://maps.google.com/mapfiles/ms/icons/blue-dot.png");
        marker.setIcon(getMarkerIcon('skyblue')); // 색상을 기본 색으로 되돌림
        selectedMarker = null;

        this_index = -1; // 아예 관련 없는 값으로 처리
        noChoice();
    }
    else {
        // 새로운 마커 선택하면 기존 마커 원래 색으로 되돌리기
        if (selectedMarker) {
            if (infowindow) {
                // 장소 정보 출력 창 켜져 있으면 꺼지도록
                infowindow.close();
            }

            // selectedMarker.setIcon("http://maps.google.com/mapfiles/ms/icons/blue-dot.png");
            selectedMarker.setIcon(getMarkerIcon('skyblue'));
        }
        // marker.setIcon("http://maps.google.com/mapfiles/ms/icons/red-dot.png");
        marker.setIcon(getMarkerIcon('red'));
        selectedMarker = marker;

        const index = marker.getLabel().text;
        console.log("Label Text : ", index);
        this_index = index - 1;

        // index - 1 : label 값 설정할 때 index + 1 해서 기록함 : index 1 부터 시작
        // ieidList 에서 index 는 0 부터 시작함
        this_ieid = ieidList[this_index];
        // console.log("ieid : ", ieid);

        console.log("this_iid : ", this_iid, "this_ieid : ", this_ieid, "this_index : ", this_index);
        console.log("itineraryTotalRead:", itineraryTotalRead);
        console.log("itineraryTotalRead.itineraryEvents:", itineraryTotalRead?.itineraryEvents);

        console.log("itineraryTotalRead.itineraryEvents[this_index] : ", itineraryTotalRead.itineraryTotal.itineraryEvents[this_index]);
        console.log("itineraryTotalRead.itineraryEvents[this_index].placeDTO.imageUrl : ", itineraryTotalRead.itineraryTotal.itineraryEvents[this_index].placeDTO.imageUrl);

        // 장소 정보 창 업데이트
        const content = `
        <div class="place-info">
            <img src="${itineraryTotalRead.itineraryTotal.itineraryEvents[this_index].placeDTO.imageUrl}" style="width:80%; max-width:300px; margin-bottom:10px;" alt="">
            <h2>${itineraryTotalRead.itineraryTotal.itineraryEvents[this_index].placeDTO.placeName}</h2>
            <p>${itineraryTotalRead.itineraryTotal.itineraryEvents[this_index].placeDTO.address}</p>
        </div>`;
        infowindow.setContent(content);
        infowindow.open(map, marker);


        axios.get(`/api/itineraries/${this_iid}/bottomline/${this_ieid}`)
            .then(response => {
                journal = response.data.journal;
                expense = response.data.expenseItemList;
                partialSettlement = response.data.partialSettlement;

                console.log("Expense Item List:", expense);
                console.log("Journal:", journal);
                console.log("partialSettlement:", partialSettlement);

                hasChoice(index);

            })
            .catch(error => console.error("Error saving content:", error));
    }
}

function getMarkerIcon(color) {
    return {
        path: google.maps.SymbolPath.CIRCLE, // 둥근 마커
        scale: 12, // 마커 크기
        fillColor: color, // 내부 색상
        fillOpacity: 1, // 색상 투명도
        strokeColor: "white", // 테두리 색상
        strokeWeight: 2, // 테두리 두께
        labelOrigin: { x: 0, y: 0 } // 라벨의 중심 위치 설정
    };
}


// 화면 출력 - 방문지 선택 x
function noChoice() {
    // 방문지 선택 x

    // 이전, 다음 버튼 제거
    const prevBtn = document.getElementById("prev-btn");
    const nextBtn = document.getElementById("next-btn");
    prevBtn.disabled = true;
    // prevBtn.style.display = "none";
    prevBtn.style.visibility = "hidden";
    nextBtn.disabled = true;
    // nextBtn.style.display = "none";
    nextBtn.style.visibility = "hidden";

    // 날짜 출력 null 처리
    const datetime = document.getElementById("date-time");
    datetime.textContent = null;
    datetime.style.display = "none";

    // 기행문 - 사진 로고 출력
    document.getElementById("journal-image").src = "/pic-icon/logo-letter-o.png";

    // 기행문 - 출력 내용 변경
    // document.getElementById("journal-no-choice").display = "block";
    const journalContent = document.getElementById("journal-content-p");
    journalContent.innerText = "방문지를 선택해 주세요!"
    journalContent.style.textAlign = "center";

    document.getElementById("go-to-journal").style.display = "none";


    // 기행문 - content 쪽 null 처리
    // const noContent = document.getElementById("no-content");
    // noContent.textContent = null;
    // noContent.style.display = "none";

    // const hasContent = document.getElementById("has-content");
    // hasContent.textContent = null;
    // hasContent.style.display = "none";


    // 장부 출력
    removeAndCreateExpense("noChoice");


}

// 화면 출력 - 방문지 선택 o
function hasChoice(index) {
    // journal

    // 이전, 다음 버튼 관련 처리
    const prevBtn = document.getElementById("prev-btn");
    const nextBtn = document.getElementById("next-btn");
    if (parseInt(index) === 1) {
        // index ; 첫 번째 방문지 => prev 버튼 비활성화
        console.log("index === 1");
        prevBtn.disabled = true;
        // prevBtn.style.display = "none";
        prevBtn.style.visibility = "hidden";
        nextBtn.disabled = false;
        // nextBtn.style.display = "block";
        nextBtn.style.visibility = "visible";
    }
    else if (parseInt(index) === ieidList.length) {
        // index ; 마지막 방문지 => next 버튼 비활성화
        console.log("index === ", ieidList.length);
        prevBtn.disabled = false;
        // prevBtn.style.display = "block";
        prevBtn.style.visibility = "visible";
        nextBtn.disabled = true;
        // nextBtn.style.display = "none";
        nextBtn.style.visibility = "hidden";
    }
    else {
        // 1 < index < length
        console.log("1 < index < length");
        prevBtn.disabled = false;
        // prevBtn.style.display = "block";
        prevBtn.style.visibility = "visible";
        nextBtn.disabled = false;
        // nextBtn.style.display = "block";
        nextBtn.style.visibility = "visible";
    }

    // 사진, 본문 출력 관련 처리
    const journalImage = document.getElementById("journal-image");
    // const journalNoContent = document.getElementById("no-content");
    // const journalHasContent = document.getElementById("has-content");
    const journalContent = document.getElementById("journal-content-p");
    const goToJournal = document.getElementById("go-to-journal");
    const datetime = document.getElementById('date-time');

    console.log("journal.imageUrl:", journal.imageUrl);
    console.log("journal.content:", journal.content);
    // console.log("journalNoContent:", journalNoContent);
    // console.log("journalHasContent:", journalHasContent);
    if ((journal.imageUrl === null) && (journal.content === null)) {
        datetime.textContent = null;
        datetime.style.display = "none";

        journalImage.src = "/pic-icon/logo-letter-o.png";
        journalContent.innerText = "기억이 옅어지기 전에 소중한 순간을 남겨 주세요!";
        journalContent.style.textAlign = "center";
        goToJournal.style.display = "block";
    }
    else {
        datetime.textContent = new Intl.DateTimeFormat('ko-KR', timeFormat).format(new Date(journal.modifiedAt));
        datetime.style.display = "block";

        if ((journal.imageUrl !== null) && (journal.content !== null)) {
            journalImage.src = journal.imageUrl;
            journalContent.textContent = journal.content;
            journalContent.style.textAlign = "left";
            goToJournal.style.display = "none";
            // journalHasContent.textContent = journal.content;
            // journalHasContent.style.display = "block";
            // journalNoContent.style.display = "none";
        }
        else if ((journal.imageUrl !== null) && (journal.content === null)) {
            journalImage.src = journal.imageUrl;
            journalContent.innerText = "기억이 옅어지기 전에 소중한 순간을 남겨 주세요!";
            journalContent.style.textAlign = "center";
            goToJournal.style.display = "block";
            // journalHasContent.style.display = "none";
            // journalNoContent.style.display = "block";
        }
        else if ((journal.imageUrl === null) && (journal.content !== null)) {
            journalImage.src = "/pic-icon/logo-letter-o.png";
            journalContent.innerText = journal.content;
            journalContent.style.textAlign = "left";
            goToJournal.style.display = "none";
            // journalHasContent.textContent = journal.content;
            // journalHasContent.style.display = "block";
            // journalNoContent.style.display = "none";
        }
    }

    // 장부
    removeAndCreateExpense("hasChoice");

}


function goToJournal() {
    window.location.href = `/itineraries/${this_iid}/events/${this_ieid}/journal`;
}

function prevJournal() {
    // if (document.getElementById("prev-btn").disabled) return; // 버튼 비활성화 => 종료
    console.log("prevJournal");

    this_index -= 1;
    toggleMarker(markerList[this_index]);
}

function nextJournal() {
    // if (document.getElementById("next-btn").disabled) return; // 버튼 비활성화 =>  종료
    console.log("nextJournal");

    this_index += 1;
    toggleMarker(markerList[this_index]);
}

function removeAndCreateExpense(kind) {
    // p 태그 삭제
    const existingTags = document.querySelectorAll('.dynamic-expense');
    if (existingTags && existingTags.length > 0) {
        existingTags.forEach(tag => tag.remove());
    }

    // 장부 출력
    // p 태그 생성 -> dynamic-expense classList 추가 + p 태그 내용 설정 -> container 자식으로 추가
    // 생성한 p 태그 : dynamic-expense 로 관리
    // const expensesContainer = document.querySelector('.expenses-container');
    const jointExpenseContainer = document.querySelector('.joint-expense-container');
    const personalExpenseContainer = document.querySelector('.personal-expense-container');

    if (kind === "noChoice") {
        // 방문지 선택 x
        const adjustment = finalSettlement.adjustment;
        console.log("adjustment", adjustment);

        // 공동 경비 정산
        const jointExpenseTitle = document.createElement("p");
        jointExpenseTitle.classList.add('dynamic-expense');
        jointExpenseTitle.textContent = "경비 정산";
        // expensesContainer.appendChild(expenseTitle);
        jointExpenseContainer.appendChild(jointExpenseTitle);

        const jointExpenseTitleSeparator = document.createElement('hr');
        jointExpenseTitleSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        jointExpenseContainer.appendChild(jointExpenseTitleSeparator);

        let moneyFormat = formatKoreanMoney(finalSettlement.expenseBookDTO.totalBudget);
        const totalBudget = document.createElement("p");
        totalBudget.classList.add('dynamic-expense');
        totalBudget.textContent = `예산 : ${moneyFormat}원`;
        // expensesContainer.appendChild(totalBudget);
        jointExpenseContainer.appendChild(totalBudget);

        moneyFormat = formatKoreanMoney(finalSettlement.expenseBookDTO.totalExpenses);
        const totalExpenses = document.createElement("p");
        totalExpenses.classList.add('dynamic-expense');
        totalExpenses.textContent = `총지출 : ${moneyFormat}원`;
        // expensesContainer.appendChild(totalBudget);
        jointExpenseContainer.appendChild(totalExpenses);

        moneyFormat = formatKoreanMoney(finalSettlement.totalBalance);
        const totalBalance = document.createElement("p");
        totalBalance.classList.add('dynamic-expense');
        totalBalance.textContent = `잔액 : ${moneyFormat}원`;
        // expensesContainer.appendChild(totalBudget);
        jointExpenseContainer.appendChild(totalBalance);

        const expenseSeparator = document.createElement('hr');
        expenseSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        jointExpenseContainer.appendChild(expenseSeparator);


        Object.keys(adjustment).forEach(key => {
            // Key 출력
            const keyTag = document.createElement('p');
            keyTag.classList.add('dynamic-expense');
            keyTag.textContent = `${key} 님`;
            // expensesContainer.appendChild(keyTag);
            jointExpenseContainer.appendChild(keyTag);


            // "송금" 출력
            const sendTag = document.createElement('p');
            sendTag.classList.add('dynamic-expense');
            sendTag.textContent = `송금`;
            // expensesContainer.appendChild(sendTag);
            jointExpenseContainer.appendChild(sendTag);


            // 송금 항목 출력
            if (adjustment[key].sendedMoney && Object.keys(adjustment[key].sendedMoney).length > 0) {
                // 송금할 게 있다
                Object.keys(adjustment[key].sendedMoney).forEach(sendKey => {
                    moneyFormat = formatKoreanMoney(adjustment[key].sendedMoney[sendKey]);

                    const sendDetailTag = document.createElement('p');
                    sendDetailTag.classList.add('dynamic-expense');
                    sendDetailTag.textContent = `    ${sendKey} 님 : ${moneyFormat}원`;
                    sendDetailTag.style.whiteSpace = "pre";
                    // expensesContainer.appendChild(sendDetailTag);
                    jointExpenseContainer.appendChild(sendDetailTag);

                });
            }
            else {
                // 송금할 게 없다
                const noSendDataTag = document.createElement('p');
                noSendDataTag.classList.add('dynamic-expense');
                noSendDataTag.textContent = "    없음";
                noSendDataTag.style.whiteSpace = "pre";
                // expensesContainer.appendChild(noSendDataTag);
                jointExpenseContainer.appendChild(noSendDataTag);

            }

            // "수금" 출력
            const receiveTag = document.createElement('p');
            receiveTag.classList.add('dynamic-expense');
            receiveTag.textContent = `수금`;
            // expensesContainer.appendChild(receiveTag);
            jointExpenseContainer.appendChild(receiveTag);


            // 수금 항목 출력
            if (adjustment[key].receivedMoney && Object.keys(adjustment[key].receivedMoney).length > 0) {
                // 수금 받을 게 있다
                Object.keys(adjustment[key].receivedMoney).forEach(receiveKey => {
                    moneyFormat = formatKoreanMoney(adjustment[key].receivedMoney[receiveKey]);

                    const receiveDetailTag = document.createElement('p');
                    receiveDetailTag.classList.add('dynamic-expense');
                    receiveDetailTag.textContent = `    ${receiveKey} 님 : ${moneyFormat}원`;
                    receiveDetailTag.style.whiteSpace = "pre";
                    // expensesContainer.appendChild(receiveDetailTag);
                    jointExpenseContainer.appendChild(receiveDetailTag);

                });
            }
            else {
                // 수금 받을 게 없다
                const noReceiveDataTag = document.createElement('p');
                noReceiveDataTag.classList.add('dynamic-expense');
                noReceiveDataTag.textContent = "    없음"
                noReceiveDataTag.style.whiteSpace = "pre";
                // expensesContainer.appendChild(noReceiveDataTag);
                jointExpenseContainer.appendChild(noReceiveDataTag);

            }

            // <hr> 태그 추가 (dynamic-expense 클래스 포함)
            const separator = document.createElement('hr');
            separator.classList.add('dynamic-expense', 'separator');
            // expensesContainer.appendChild(separator);
            jointExpenseContainer.appendChild(separator);

        });

        // 개인별 예산, 지출 출력
        // const eachExpense = finalSettlement.eachExpense;
        // console.log("eachExpense", eachExpense);

        const personalExpenseTitle = document.createElement("p");
        personalExpenseTitle.classList.add('dynamic-expense');
        personalExpenseTitle.textContent = "여행자별 경비";
        // expensesContainer.appendChild(expenseTitle);
        personalExpenseContainer.appendChild(personalExpenseTitle);

        const personalExpenseTitleSeparator = document.createElement('hr');
        personalExpenseTitleSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        personalExpenseContainer.appendChild(personalExpenseTitleSeparator);


        Object.keys(travler).forEach(key => {
            // 이름 출력
            const nameTag = document.createElement('p');
            nameTag.classList.add('dynamic-expense');
            nameTag.textContent = `${travler[key].travelerName} 님`;
            // expensesContainer.appendChild(nameTag);
            personalExpenseContainer.appendChild(nameTag);

            // 예산 출력
            moneyFormat = formatKoreanMoney(travler[key].totalBudget)
            const totalBudget = document.createElement('p');
            totalBudget.classList.add('dynamic-expense');
            totalBudget.textContent = `예산 : ${moneyFormat}원`;
            // expensesContainer.appendChild(nameTag);
            personalExpenseContainer.appendChild(totalBudget);

            // 총지출 출력
            moneyFormat = formatKoreanMoney(travler[key].totalExpense)
            const totalExpense = document.createElement('p');
            totalExpense.classList.add('dynamic-expense');
            totalExpense.textContent = `총지출 : ${moneyFormat}원`;
            // expensesContainer.appendChild(nameTag);
            personalExpenseContainer.appendChild(totalExpense);

            // <hr> 태그 추가 (dynamic-expense 클래스 포함)
            const separator = document.createElement('hr');
            separator.classList.add('dynamic-expense', 'separator');
            // expensesContainer.appendChild(separator);
            personalExpenseContainer.appendChild(separator);

        });
    }
    else if (kind === "hasChoice") {
        // 방문지 선택 o
        console.log("partialSettlement : ", partialSettlement);

        const adjustment = partialSettlement.adjustment;
        console.log("adjustment", adjustment);

        // 공동 경비 정산
        const jointExpenseTitle = document.createElement("p");
        jointExpenseTitle.classList.add('dynamic-expense');
        jointExpenseTitle.textContent = "경비 정산";
        // expensesContainer.appendChild(expenseTitle);
        jointExpenseContainer.appendChild(jointExpenseTitle);

        const jointExpenseTitleSeparator = document.createElement('hr');
        jointExpenseTitleSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        jointExpenseContainer.appendChild(jointExpenseTitleSeparator);

        let moneyFormat = formatKoreanMoney(partialSettlement.totalExpense);
        const totalExpenses = document.createElement("p");
        totalExpenses.classList.add('dynamic-expense');
        totalExpenses.textContent = `총지출 : ${moneyFormat}원`;
        // expensesContainer.appendChild(totalBudget);
        jointExpenseContainer.appendChild(totalExpenses);

        const expenseSeparator = document.createElement('hr');
        expenseSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        jointExpenseContainer.appendChild(expenseSeparator);

        Object.keys(adjustment).forEach(key => {
            // Key 출력
            const keyTag = document.createElement('p');
            keyTag.classList.add('dynamic-expense');
            keyTag.textContent = `${key} 님`;
            // expensesContainer.appendChild(keyTag);
            jointExpenseContainer.appendChild(keyTag);


            // "송금" 출력
            const sendTag = document.createElement('p');
            sendTag.classList.add('dynamic-expense');
            sendTag.textContent = `송금`;
            // expensesContainer.appendChild(sendTag);
            jointExpenseContainer.appendChild(sendTag);


            // 송금 항목 출력
            if (adjustment[key].sendedMoney && Object.keys(adjustment[key].sendedMoney).length > 0) {
                // 송금할 게 있다
                Object.keys(adjustment[key].sendedMoney).forEach(sendKey => {
                    moneyFormat = formatKoreanMoney(adjustment[key].sendedMoney[sendKey]);

                    const sendDetailTag = document.createElement('p');
                    sendDetailTag.classList.add('dynamic-expense');
                    sendDetailTag.textContent = `    ${sendKey} 님 : ${moneyFormat}원`;
                    sendDetailTag.style.whiteSpace = "pre";
                    // expensesContainer.appendChild(sendDetailTag);
                    jointExpenseContainer.appendChild(sendDetailTag);

                });
            }
            else {
                // 송금할 게 없다
                const noSendDataTag = document.createElement('p');
                noSendDataTag.classList.add('dynamic-expense');
                noSendDataTag.textContent = "    없음";
                noSendDataTag.style.whiteSpace = "pre";
                // expensesContainer.appendChild(noSendDataTag);
                jointExpenseContainer.appendChild(noSendDataTag);

            }

            // "수금" 출력
            const receiveTag = document.createElement('p');
            receiveTag.classList.add('dynamic-expense');
            receiveTag.textContent = `수금`;
            // expensesContainer.appendChild(receiveTag);
            jointExpenseContainer.appendChild(receiveTag);


            // 수금 항목 출력
            if (adjustment[key].receivedMoney && Object.keys(adjustment[key].receivedMoney).length > 0) {
                // 수금 받을 게 있다
                Object.keys(adjustment[key].receivedMoney).forEach(receiveKey => {
                    moneyFormat = formatKoreanMoney(adjustment[key].receivedMoney[receiveKey]);

                    const receiveDetailTag = document.createElement('p');
                    receiveDetailTag.classList.add('dynamic-expense');
                    receiveDetailTag.textContent = `    ${receiveKey} 님 : ${moneyFormat}원`;
                    receiveDetailTag.style.whiteSpace = "pre";
                    // expensesContainer.appendChild(receiveDetailTag);
                    jointExpenseContainer.appendChild(receiveDetailTag);

                });
            }
            else {
                // 수금 받을 게 없다
                const noReceiveDataTag = document.createElement('p');
                noReceiveDataTag.classList.add('dynamic-expense');
                noReceiveDataTag.textContent = "    없음";
                noReceiveDataTag.style.whiteSpace = "pre";
                // expensesContainer.appendChild(noReceiveDataTag);
                jointExpenseContainer.appendChild(noReceiveDataTag);

            }

            // <hr> 태그 추가 (dynamic-expense 클래스 포함)
            const separator = document.createElement('hr');
            separator.classList.add('dynamic-expense', 'separator');
            // expensesContainer.appendChild(separator);
            jointExpenseContainer.appendChild(separator);

        });

        // 개인 경비 총지출 출력
        const personalExpenseTitle = document.createElement("p");
        personalExpenseTitle.classList.add('dynamic-expense');
        personalExpenseTitle.textContent = "여행자별 경비";
        // expensesContainer.appendChild(expenseTitle);
        personalExpenseContainer.appendChild(personalExpenseTitle);

        const personalExpenseTitleSeparator = document.createElement('hr');
        personalExpenseTitleSeparator.classList.add('dynamic-expense', 'separator');
        // expensesContainer.appendChild(expenseTitleSeparator);
        personalExpenseContainer.appendChild(personalExpenseTitleSeparator);

        const eachExpense = partialSettlement.eachExpenses;
        console.log("eachExpense", eachExpense);
        Object.keys(eachExpense).forEach(key => {
            // 이름 : 총지출 출력
            const nameTag = document.createElement('p');
            nameTag.classList.add('dynamic-expense');
            nameTag.textContent = `${key} 님`;
            // expensesContainer.appendChild(nameTag);
            personalExpenseContainer.appendChild(nameTag);

            moneyFormat = formatKoreanMoney(eachExpense[key]);
            const expense = document.createElement('p');
            expense.classList.add('dynamic-expense');
            expense.textContent = `총지출 : ${moneyFormat}원`;
            // expensesContainer.appendChild(expense);
            personalExpenseContainer.appendChild(expense);

            // <hr> 태그 추가 (dynamic-expense 클래스 포함)
            const separator = document.createElement('hr');
            separator.classList.add('dynamic-expense', 'separator');
            // expensesContainer.appendChild(separator);
            personalExpenseContainer.appendChild(separator);

        });
    }
}

function formatKoreanMoney(value) {
    // 숫자를 문자열로 변환
    const stringValue = String(value);

    // 정규식을 사용하여 4자리 단위로 나눔
    return stringValue.replace(/\B(?=(\d{4})+(?!\d))/g, ',');
}