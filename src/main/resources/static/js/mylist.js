let itineraryMap = new Map();
let currentPage = 0; // 현재 페이지
const userId = 1;  // 실제 로그인한 사용자 ID
let sortBy = sessionStorage.getItem("sortBy") || "createdDate";
let direction = sessionStorage.getItem("direction") || "DESC";
let isDebug = false; // 디버그 모드 활성화
let prevMenuOwner = null;
let currentMenuOwner = null;
let debugData = {
    "content": [
        {
            "id": 21,
            "itineraryName": "서울 여행",
            "startDate": "2025-03-06T00:00:00",
            "totalDays": 4,
            "transportationType": 1,
            "createdDate": "2025-03-04T03:09:29",
            "modifiedDate": "2025-03-04T03:09:29",
            "role": "ROLE_OWNER",
            "isShared": false,
            "hasGuest": false
        },
        {
            "id": 20,
            "itineraryName": "서울 여행",
            "startDate": "2025-02-12T00:00:00",
            "totalDays": 7,
            "transportationType": 1,
            "createdDate": "2025-03-04T03:07:29",
            "modifiedDate": "2025-03-04T03:07:29",
            "role": "ROLE_OWNER",
            "isShared": true,
            "hasGuest": false
        },
        {
            "id": 19,
            "itineraryName": "서울 여행",
            "startDate": "2025-04-23T00:00:00",
            "totalDays": 3,
            "transportationType": 1,
            "createdDate": "2025-03-01T21:02:59",
            "modifiedDate": "2025-03-01T21:02:59",
            "role": "ROLE_GUEST",
            "isShared": true,
            "hasGuest": true
        },
        {
            "id": 18,
            "itineraryName": "서울 여행",
            "startDate": "2025-03-17T00:00:00",
            "totalDays": 15,
            "transportationType": 1,
            "createdDate": "2025-02-28T15:14:54",
            "modifiedDate": "2025-02-28T15:14:54",
            "role": "ROLE_OWNER",
            "isShared": true,
            "hasGuest": false
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 4,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "last": false,
    "totalElements": 17,
    "totalPages": 5,
    "size": 4,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "first": true,
    "numberOfElements": 4,
    "empty": false
};



// 페이지 로드 시 첫 데이터 로드
$(document).ready(function () {

    loadItineraries();

    $("#dynamicDropdown").hide();
    $(".dropdown-arrow").hide();

    $(".itinerary-list").hide();
    $(".itinerary-list.total").show();

    $(".section").hide();
    $(".section.itinerary-list-wrap").show();
});



// 🛠️ itinerary 데이터 관리
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
//새로운 이벤트를 `ItineraryMap`에 추가하고 ID 반환
function addItinerary(itinerary) {
    const id = generateUniqueId(itineraryMap);
    itinerary.hashId = id;
    itineraryMap.set(id, itinerary);
    return id;
}
//주어진 ID로 `ItineraryMap`에서 이벤트 조회
function getItineraryById(id) {
    return itineraryMap.get(id) || null;
}


// 🛠️ itinerary 데이터 fetch 및 렌더링
// ---------------------------------------------------

// 일정 데이터를 가져오는 함수
function fetchItineraryData(reset) {
    if (isDebug) {
        return Promise.resolve(createData(debugData)); // 디버그 모드일 경우, Promise로 감싸서 반환
    }

    return new Promise((resolve, reject) => {
        $.ajax({
            url: `/api/itinerary/mylist`,
            type: "GET",
            data: {
                userId: userId,
                page: currentPage,
                size: 12,
                sortBy: sortBy,
                direction: direction
            },
            success: function (response) {
                resolve(createData(response)); // 성공 시 Promise 완료
            },
            error: function (xhr, status, error) {
                reject(error); // 실패 시 Promise 거부
            }
        });
    });
}


// 일정 불러오기 함수
async function loadItineraries(reset = false) {
    if (reset) {
        itineraryMap.clear();
        currentPage = 0;
    }

    try {
        let [renderList, isEmpty, isLast] = await fetchItineraryData(reset);
        renderItineraries(".itinerary-list.total", renderList, isEmpty, isLast, reset);
        renderItineraries(".itinerary-list.sharing", renderList, isEmpty, isLast, reset);
        renderItineraries(".itinerary-list.shared", renderList, isEmpty, isLast, reset);

        // 현재 페이지 증가
        currentPage++;
    } catch (error) {
        console.error("일정 데이터를 불러오는 중 오류 발생:", error);
    }
}

// 데이터 처리
function createData(data) {
    let itineraries = Object.values(data.content); // Convert object to array
    console.log(itineraries);

    let renderList = [];
    itineraries.forEach(itinerary => {
        let itineraryHashId = addItinerary(itinerary); // 이벤트 추가 후 ID 생성
        renderList.push(
            itineraryHashId
        );
    });

    // 더보기 버튼 활성화 여부 결정
    if (data.last) {
        $("#loadMoreBtn").hide();
    } else {
        $("#loadMoreBtn").show();
    }

    return [renderList, itineraries.length === 0, data.last];
}

// 일정을 렌더링하는 함수
function renderItineraries(divType = ".itinerary-list", renderList, isEmpty, isLast, reset = false) {
    if (reset) {
        $(divType).empty(); // 기존 목록 초기화
        // 페이지 리셋
    }

    // 데이터가 없을 경우 "더보기" 버튼 숨김
    if (isEmpty) {
        $("#loadMoreBtn").hide();
        return;
    }

    // 더보기 버튼 활성화 여부 결정
    if (isLast) {
        $("#loadMoreBtn").hide();
    } else {
        $("#loadMoreBtn").show();
    }

    console.log(renderList);
    // 일정 목록 추가
    renderList.forEach(hashId => {
        let itinerary = getItineraryById(hashId);
        if (divType === ".itinerary-list.total") {
            $(divType).append(createItineraryElement(itinerary, true));
        } else if (divType === ".itinerary-list.sharing"){
            $(divType).append(createItineraryElement(itinerary, (itinerary.isShared || itinerary.hasGuest) && (itinerary.role === "ROLE_OWNER")));
        } else if (divType === ".itinerary-list.shared"){
            $(divType).append(createItineraryElement(itinerary, (itinerary.isShared || itinerary.hasGuest) && (itinerary.role === "ROLE_GUEST")));
        }

    });

}



function refreshItineraryElement(hashId, role, isShared, hasGuest, exitGuest = false) {
    // 모든 itinerary-list에서 해당 itinerary 찾기
    let $itineraryElements = $(`.card-itinerary[data-id="${hashId}"]`);

    $itineraryElements.each(function () {
        let $itinerary = $(this);
        let $badge = $itinerary.find(".badge-share");

        // 📌 전체 일정 목록 (`total`)
        if ($itinerary.closest(".itinerary-list").hasClass("total")) {
            if (exitGuest) {
                $itinerary.addClass("hide"); // 나간 경우 숨김
            } else {
                $itinerary.removeClass("hide");
            }

            // 호스트 뱃지 업데이트
            if (isShared || hasGuest) {
                $badge.removeClass("not-share host guest").addClass(role === "Owner" ? "host" : "guest");
            } else {
                $badge.removeClass("host guest").addClass("not-share");
            }
        }
        if(!exitGuest){
            // 📌 공유 중인 일정 목록 (`sharing`)
            if ($itinerary.closest(".itinerary-list").hasClass("sharing")) {
               console.log(isShared, hasGuest, role);

                if ((isShared || hasGuest) && (role === "Owner")) {
                    $itinerary.removeClass("hide");
                } else {
                    $itinerary.addClass("hide");
                }

                // 호스트 뱃지 업데이트
                if (isShared || hasGuest) {
                    $badge.removeClass("not-share host guest").addClass(role === "Owner" ? "host" : "guest");
                }
            }
        }
        // 📌 공유받은 일정 목록 (`shared`)
        if ($itinerary.closest(".itinerary-list").hasClass("shared")) {
            if (exitGuest) {
                $itinerary.addClass("hide"); // 게스트에서 나가면 숨김
            }
        }
    });
}


// card element 리턴
function createItineraryElement(itinerary, isDisplay) {
    console.log("itinerary Object:", itinerary);

    // 현재 날짜
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 여행 시작일 및 종료일 계산
    const startDate = new Date(itinerary.startDate);
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + itinerary.totalDays - 1);

    // 날짜 포맷 함수 (YYYY. MM. DD -> MM. DD. and YYYY. MM. DD)
    const formatDate = (date, year = true) => {
        const month = String(date.getMonth() + 1).padStart(2, '0'); // MM
        const day = String(date.getDate()).padStart(2, '0'); // DD
        return year ? `${date.getFullYear()}. ${month}. ${day}` : `${month}. ${day}.`;
    };

    // 여행 상태 확인 (여행 전 D-Day, 여행 중 표시)
    let travelStatus = `D-${Math.ceil((startDate - today) / (1000 * 60 * 60 * 24))}`;
    if (today > startDate){
        travelStatus = `D+${Math.ceil((today - startDate) / (1000 * 60 * 60 * 24))}`;
    } else if (today >= startDate && today <= endDate) {
        travelStatus = "여행중";
    }

    // 대표 지역 표시 (없을 경우 "미정")
    const locationName = itinerary.locations && itinerary.locations.length > 0 ? itinerary.locations[0] : "미정";

    return $(`
    <div class="card-itinerary ${isDisplay ? "" : "hide"}" data-id="${itinerary.hashId}">
      <div class="card-thumbnail">
        <img src="https://fastly.picsum.photos/id/477/1000/1000.jpg?hmac=y2Qqhq8lLe7PrjRPIxa3UvcKHX_Q4TV-eaTBqhcBCUE"
             alt="${itinerary.itineraryName} 이미지"
             onerror="this.style.display='none'">
        <!-- 항상 표시되는 오버레이 -->
        <div class="card-overlay">
          <div class="card-header">
            <div class="card-header-left">
              <span class="badge-days">${travelStatus}</span>
              <span class="location-name">${locationName}</span>
            </div>
            <div class="card-header-right">
              <span class="badge-share ${itinerary.isShared ||  itinerary.hasGuest ? (itinerary.role === 'ROLE_OWNER' ? 'host' : 'guest') : 'not-share'}">
                ${itinerary.role === 'ROLE_OWNER' ? '호스트' : '게스트'}
              </span>
            </div>
          </div>
          <div class="card-footer">
            <div class="card-footer-left">
              <div><span class="name">${itinerary.itineraryName}(으)로 여행</span></div>
              <div><span class="date">${formatDate(startDate, false)} - ${formatDate(endDate)}</span></div>
            </div>
            <div class="card-footer-right dropdown">
              <button class="btn btn-link text-white p-0 menu-btn" type="button">
                ⋮
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
`);
}


// Event Listener
// ---------------------------------------------------


// 정렬 변경 시 데이터 다시 로드
$("#sortSelect").on("change", function () {
    sortBy = $(this).val(); // 선택한 정렬 기준
    direction = $(this).find("option:selected").data("direction"); // 정렬 방향

    sessionStorage.setItem("sortBy", sortBy);
    sessionStorage.setItem("direction", direction);

    loadItineraries(true); // 기존 데이터 초기화 후 다시 로드
});


// 여정 카드 클릭시
$(document).on("click", ".card-itinerary", function (event) {
    event.stopPropagation();
    if (!$(event.target).closest(".menu-btn, .card-footer-right").length) {
        console.log("카드 클릭됨: ", $(this).data("id"));
        // 여기에 원하는 동작 추가
    }
});


// menu 버튼 이벤트
$(document).on("click", ".menu-btn", function (event) {
    event.stopPropagation(); // 이벤트 버블링 방지

    let button = $(this);
    let cardItinerary = button.closest(".card-itinerary"); // 가장 가까운 부모 요소 찾기
    let itineraryHashId = cardItinerary.data("id"); // data-id 값 가져오기

    console.log("Itinerary ID:", itineraryHashId); // 콘솔에 출력


    let dropdown = $("#dynamicDropdown");
    let arrow = $(".dropdown-arrow"); // 삼각형 요소 선택
    let menu = $(".dropdown-menu");
    let itemEdit = $(".dropdown-item.edit");
    let itemShared = $(".dropdown-item.itemShared");

    if(prevMenuOwner && prevMenuOwner === itineraryHashId && dropdown.is(":visible")) {
        dropdown.hide();
        arrow.hide();
        return;
    }
    currentMenuOwner = itineraryHashId;

    let itinerary = getItineraryById(currentMenuOwner);
    if(itinerary.role === "ROLE_OWNER"){
        itemEdit.show();
    } else {
        if(itinerary.isShared || itinerary.hasGuest){
            itemShared.show();
        } else {
            itemShared.hide();
        }
        itemEdit.hide();
    }


    // 현재 클릭한 버튼 위치 가져오기
    let buttonOffset = button.offset();
    let buttonHeight = button.outerHeight();
    let menuWidth = menu.outerWidth();
    let menuHeight = menu.outerHeight();

    // 드롭다운 위치 설정
    dropdown.css({
        top: buttonOffset.top + buttonHeight + 10 + "px",
        left: buttonOffset.left - menuWidth + 30 + "px",
        display: "block"
    });

    // 삼각형 위치 설정
    arrow.css({
        top: buttonOffset.top + buttonHeight + 2 + "px", // 드롭다운보다 살짝 위쪽
        left: buttonOffset.left + (button.outerWidth() / 2) - (arrow.outerWidth() / 2) + 8 + "px",
        display: "block"
    });

    prevMenuOwner = itineraryHashId;
});

// 다른 곳 클릭 시 드롭다운 & 삼각형 닫기
$(document).on("click", function (event) {
    if (!$(event.target).closest("#dynamicDropdown, .menu-btn").length) {
        $("#dynamicDropdown").hide();
        $(".dropdown-arrow").hide();
    }
});

$(document).ready(function () {
// "더보기" 버튼 클릭 이벤트
    $("#loadMoreBtn").on("click", function () {
        loadItineraries();
    });

// 전체 탭 선택
    $(".tab").on("click", function () {
        // 모든 탭의 'active' 클래스를 제거
        $(".tab").removeClass("active");
        // 클릭한 탭에 'active' 클래스 추가
        $(this).addClass("active");

        // 모든 섹션 목록 숨기기
        $(".section").hide();

        // 탭에 따라 해당 리스트 표시
        let index = $(this).index();
        if (index === 0) {
            $(".section.itinerary-list-wrap").show();
        } else if (index === 1) {
            $(".section.photos").show();
        }
    });

// 일정 탭 선택
    $(".itinerary-tab").on("click", function () {
        // 모든 탭의 'active' 클래스를 제거
        $(".itinerary-tab").removeClass("active");
        // 클릭한 탭에 'active' 클래스 추가
        $(this).addClass("active");

        // 모든 일정 목록 숨기기
        $(".itinerary-list").hide();

        // 탭에 따라 해당 리스트 표시
        let index = $(this).index();
        if (index === 0) {
            $(".itinerary-list.total").show();
        } else if (index === 1) {
            $(".itinerary-list.sharing").show();
        } else if (index === 2) {
            $(".itinerary-list.shared").show();
        }
    });


    // 편집 버튼 클릭 이벤트
    $(document).on("click", ".dropdown-item.edit", function (event) {
        event.preventDefault();
        if (currentMenuOwner) {
            let itinerary = getItineraryById(currentMenuOwner);
            if (itinerary && itinerary.id) {
                window.location.href = `/itinerary/edit/${itinerary.id}`;
            } else {
                console.error("Itinerary 정보를 찾을 수 없습니다.");
            }
        }
    });

    // 삭제 버튼 클릭 이벤트
    $(document).on("click", ".dropdown-item.delete", function (event) {
        event.preventDefault();
        if (currentMenuOwner) {
            let itinerary = getItineraryById(currentMenuOwner);

            if (!itinerary) {
                console.error("Itinerary 정보를 찾을 수 없습니다.");
                return;
            }

            if (itinerary.role === "ROLE_OWNER") {
                let message = "정말 삭제하시겠습니까?";
                if (itinerary.isShared || itinerary.hasGuest) {
                    message += "\n※ 이 일정은 공유된 상태입니다. 삭제하면 공유된 사용자도 접근할 수 없습니다.";
                }
                if (confirm(message)) {
                    console.log("삭제 요청 보냄 (OWNER):", itinerary.id);
                    // 삭제 요청 실행 로직 추가
                }
            }

            if (itinerary.role === "ROLE_GUEST") {
                let message = "정말 이 공유받은 일정을 제거하시겠습니까?\n※ 제거하면 공유받은 일정 목록에서 접근할수 없습니다.";
                if (confirm(message)) {
                    removeGuestMine(itinerary.id);
                    $("#dynamicDropdown").hide();
                    $(".dropdown-arrow").hide();
                }
            }
        }
    });


// 공유 버튼 클릭 이벤트
    $(document).on("click", ".dropdown-item.share", function (event) {
        event.preventDefault();
        if (currentMenuOwner) {
            let itinerary = getItineraryById(currentMenuOwner);

            // fetch 완료 후 모달 표시 및 refresh 실행
            fetchItineraryStatus(itinerary.id, true);
        }
    });


});


// Share 파트
// =================================================
let itineraryStatus = null;

function fetchItineraryStatus(iid, showModal = false, callback = null) {
    $.ajax({
        url: `/api/share/status?itineraryId=${iid}`,
        type: "GET",
        success: function (response) {
            // API 응답 반영
            itineraryStatus = {
                id: iid,
                hasGuest: response.hasGuest,
                isShared: response.shared,
                role: response.userRole === "ROLE_OWNER" ? "Owner" : "Guest",
                collaborators: response.collaborators.map(collab => ({
                    name: collab.userName,
                    role: collab.icRole === "ROLE_OWNER" ? "Owner" : "Guest",
                    userId: collab.userId
                }))
            };

            updateModalUI();

            if (showModal) {
                $("#shareModal").modal("show");
            }

            // 완료 후 `callback` 실행
            if (typeof callback === "function") {
                callback(itineraryStatus);
            }
        },
        error: function (xhr) {
            alert("일정 정보를 불러오는 데 실패했습니다.");
        }
    });
}


function updateModalUI() {

    $("#ownerList").empty();
    $("#guestList").empty();

    let isOwner = itineraryStatus.role === "Owner";

    itineraryStatus.collaborators.forEach(collab => {
        let listItem = `<li class="list-group-item d-flex justify-content-between align-items-center">
                        ${collab.name}`;

        // Owner일 때만 삭제 버튼 추가
        if (isOwner && collab.role === "Guest") {
            listItem += `<button class="btn btn-sm btn-danger remove-guest" data-user-id="${collab.userId}">
                        <i class="bi bi-x"></i>
                     </button>`;
        }

        listItem += `</li>`;

        if (collab.role === "Owner") {
            $("#ownerList").append(listItem);
        } else {
            $("#guestList").append(listItem);
        }
    });


    if (itineraryStatus.isShared) {
        $("#shareStatus").text("ON").removeClass("bg-secondary").addClass("bg-success");
        $("#shareLinkContainer").removeClass("d-none");

        $.ajax({
            url: `/api/share/token?itineraryId=${itineraryStatus.id}`,
            type: "GET",
            success: function (token) {
                $("#shareLink").val(`${window.location.origin}/join/${token}`);
            },
            error: function () {
                $("#shareLink").val("공유 링크를 불러올 수 없습니다.");
            }
        });


        $("#disableShareBtn").toggleClass("d-none", !isOwner);
        $("#generateLinkBtn").addClass("d-none");

    } else {
        $("#shareStatus").text("OFF").removeClass("bg-success").addClass("bg-secondary");
        $("#shareLinkContainer").addClass("d-none");
        $("#disableShareBtn").addClass("d-none");

        if (isOwner) {
            $("#generateLinkBtn").removeClass("d-none").prop("disabled", false);
        } else {
            $("#generateLinkBtn").addClass("d-none");
        }
    }

    // GUEST 삭제 버튼 이벤트 바인딩
    $(".remove-guest").click(function () {
        let targetUserId = $(this).data("user-id");
        removeGuest(targetUserId);
    });

}
function removeGuestMine(iid) {
    $.ajax({
        url: `/api/share/remove-mine?itineraryId=${iid}`,
        type: "DELETE",
        success: function () {
            alert("GUEST가 삭제되었습니다.");

                refreshItineraryElement(currentMenuOwner, false, false, false, true);
        },
        error: function () {
            alert("GUEST 삭제에 실패했습니다.");
        }
    });
}


function removeGuest(targetUserId) {
    $.ajax({
        url: `/api/share/remove?itineraryId=${itineraryStatus.id}&targetUserId=${targetUserId}`,
        type: "DELETE",
        success: function () {
            alert("GUEST가 삭제되었습니다.");
            fetchItineraryStatus(itineraryStatus.id, true, function (status) {
                refreshItineraryElement(currentMenuOwner, status.role, status.isShared, status.hasGuest, false);
            });

        },
        error: function () {
            alert("GUEST 삭제에 실패했습니다.");
        }
    });
}
// 공유 링크 생성 (OWNER만 가능)
$("#generateLinkBtn").click(function () {
    $.ajax({
        url: `/api/share/create?itineraryId=${itineraryStatus.id}`,
        type: "POST",
        success: function (token) {
            itineraryStatus.isShared = true;
            updateModalUI();
            refreshItineraryElement(currentMenuOwner, itineraryStatus.role, itineraryStatus.isShared,
                itineraryStatus.hasGuest, false);

        },
        error: function (xhr) {
            alert("공유 링크 생성에 실패했습니다.");
        }
    });
});

// 공유 링크 삭제 (OWNER만 가능)
$("#disableShareBtn").click(function () {
    $.ajax({
        url: `/api/share/delete?itineraryId=${itineraryStatus.id}`,
        type: "DELETE",
        success: function () {
            itineraryStatus.isShared = false;
            updateModalUI();
            refreshItineraryElement(currentMenuOwner, itineraryStatus.role, itineraryStatus.isShared,
                itineraryStatus.hasGuest, false);
        },
        error: function (xhr) {
            alert("공유 링크 삭제에 실패했습니다.");
        }
    });
});

// 링크 복사 기능
$("#copyLinkBtn").click(function () {
    let copyText = $("#shareLink");
    copyText.select();
    document.execCommand("copy");
    alert("링크가 복사되었습니다.");
});

