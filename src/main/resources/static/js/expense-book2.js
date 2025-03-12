/***** *****/
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
        },
        error: function (xhr, status, error) {
            console.error("Error fetching itinerary:", error);
        }
    });
});