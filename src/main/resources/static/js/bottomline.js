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
 * ========================================================
 */

let this_iid;
let this_ieid = 0;

// map 쪽
let map;
let markerList = [];
let pathCoordinates = [];
let selectedMarker = null;
let selectedPlace = null;
let infowindow;

let itineraryTotalRead;
let ieidList = [];

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

    console.log("fetchBottomLine - itineraryTotalRead : ", itineraryTotalRead); // 데이터 확인
    console.log("Itinerary Total : ", itineraryTotalRead.itineraryTotal);
    console.log("Traveler List : ", itineraryTotalRead.travelerList);
    console.log("Expense Book : ", itineraryTotalRead.expenseBook);

    initMap();
    showExpense();

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
    }
    else {
        // 새로운 마커 선택하면 기존 마커 원래 색으로 되돌리기
        if (selectedMarker) {
            // selectedMarker.setIcon("http://maps.google.com/mapfiles/ms/icons/blue-dot.png");
            selectedMarker.setIcon(getMarkerIcon('skyblue'));
        }
        // marker.setIcon("http://maps.google.com/mapfiles/ms/icons/red-dot.png");
        marker.setIcon(getMarkerIcon('red'));
        selectedMarker = marker;


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


// 장부 - 지출 내역 관련

function showExpense() {

}
