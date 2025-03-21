/************* 🧳 전역 변수 선언 🧳 *************/
let eventElementForTab = null;





/************** 🧳 경비 & 정산 안내 탭 🧳 **************/

/* 🎈 정산 탭 클릭했을 때 */
$(document).on("click", "#adjustmentTab", async function () {

    const itineraryId = eventElementForTab.data("iid");
    const eventId = eventElementForTab.data("ieid");


    // 페이지 로드
    await loadAdjustmentPage(itineraryId, eventId);
});

/* 🎈 경비 탭 클릭했을 때 */
$(document).on("click", "#expenditureTab", async function () {

    const itineraryId = eventElementForTab.data("iid");
    const eventId = eventElementForTab.data("ieid");

    await loadExpensePage(itineraryId, eventId);
});



/************** 🧳 경비 & 작성 이벤트 핸들링 🧳 **************/

//🎈 왼쪽 패널 - +경비 내역 추가 클릭 시 -> 오른쪽 패널에 경비 내역 로드
$(document).on("click", ".expense-item-list-addition", async function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    eventElementForTab = $(this);

    await loadExpensePage(iid, ieid);
});


async function loadExpensePage(iid, ieid) {
    let travelers = [];
    try {
        // 여행자 정보 가져오기
        const travelersResponse = await callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);

        // API에서 받은 여행자 데이터로 travelers 배열 채우기
        for (let t of travelersResponse.travelers) {
            travelers.push(t.name);
        }

        // 여행자 정보 화면에 업데이트
        const expenseBasicInfoTraveler = $("#expenseBasicInfoTraveler");
        expenseBasicInfoTraveler.html(`${travelers.length} 명과 함께하고 있습니다`);

        // 남은 예산 정보 가져오기
        const totalAdjustmentData = await callApiAt(`/api/itineraries/${iid}/adjustment`, "GET", null);
        const remainedBudget = totalAdjustmentData.totalBalance;
        const expenseBasicInfoRemainedBudget = $("#expenseBasicInfoRemainedBudget");
        expenseBasicInfoRemainedBudget.html(`남은 예산 : ${remainedBudget} 원`);

    } catch (error) {
        console.error("에러 발생:", error);
    }

    // expense-right.html을 오른쪽 화면`#detailContainer` 영역에 로드
    fetch(`/itinerary/${iid}/events/${ieid}/expense-right`) // fetch("/expense-book/expense-right.html")
        .then(response => response.text())
        .then(async html => {
            $("#detailContainer").html(html);
            await getExpenseBookForWritingByItineraryEvent(iid, ieid);

            document.getElementById("expenseItemCreation").innerHTML = getExpenseItemForm(iid, ieid);
            const withWhomOptions = await createTravelerOption(iid, "expenseItemCreationWithWhom", "👥 함께한 사람 선택");
            const payerOptions = await createTravelerOption(iid, "expenseItemCreationPayer", "😄지불한 사람");
            // await waitForTagifyToLoad();
            // createTag(iid);

            // "정산" 탭을 비활성화(css), "경비" 탭을 활성화(css)
            document.getElementById("adjustmentTab").setAttribute("style", "background-color: #8e8b82; color: #e9dcbe;");
            document.getElementById("expenditureTab").setAttribute("style", "background-color: #ffffff; color: #8e8b82;");
        })
        .catch(error => console.error("Error loading expense-right.html:", error)
        );

}

// Tagify - tag 생성
// async function createTag(itineraryId) {
//     // input 요소 가져오기
//     let payer = document.querySelector('input[name=payer]');
//     let withWhom = document.querySelector('input[name=withWhom]');
//
//     if (!payer || !withWhom) {
//         console.error("Input elements not found.");
//         return;
//     }
//
//     // traveler 조회
//     const travelerList = await callApiAt(`/api/itinerary/${itineraryId}/travelers`, "GET", null);
//     const travelers = travelerList.travelers.map(t => t.name);
//
//     // dropdown 메뉴
//     let payerDropdown = { classname: "payer-dropdown-menu", closeOnSelect: false };
//     let withWhomDropdown = { classname: "withWhom-dropdown-menu", closeOnSelect: false };
//
//
//     // 태그 생성
//     var payerTag = new Tagify(payer, {mode: 'input', whitelist: travelers, maxTags: 1, enforceWhitelist: true});
//     var withWhomTag = new Tagify(withWhom, {mode: 'input', whitelist: travelers, enforceWhitelist: true});
//
//     // 태그 추가 이벤트 처리
//     payerTag.on('add', function() {
//         console.log("PayerTag Value: ", payerTag.value);
//     });
//
//     withWhomTag.on('add', function() {
//         console.log("WithWhomTag Value: ", withWhomTag.value);
//     });
//
//     // payer와 withWhom 값이 중복되지 않도록 처리하는 부분 (필요에 따라 활성화)
//     payerTag.on('add', function() {
//         compareAndRemoveTag(payerTag, withWhomTag);
//     });
//
//     withWhomTag.on('add', function() {
//         compareAndRemoveTag(payerTag, withWhomTag);
//     });
//
//     // payer와 withWhom 값이 중복되면 한쪽에서 제거하는 함수
//     function compareAndRemoveTag(payerTag, withWhomTag) {
//         const withWhomValue = withWhomTag.value.map(item => item.value);
//         const payerValue = payerTag.value.length > 0 ? payerTag.value[0].value : null;
//
//         if (withWhomValue.includes(payerValue)) {
//             // 'remove' 이벤트 트리거로 withWhomTag에서 값 제거
//             withWhomTag.removeTags(payerValue);
//             console.log(`Removed: ${payerValue} from withWhomTag because it matches payerTag value`);
//         }
//     }
//
//
//
//     // // withWhomTag 값을 input 태그의 value로 설정하는 함수
//     // function insertWithWhomTagToInputValue() {
//     //     const withWhomValue = withWhomTag.value.map(item => item.value).join(', ');
//     //     withWhom.value = withWhomValue;
//     //     console.log("Updated withWhom input value: ", withWhom.value);
//     // }
//     //
//     // withWhomTag.on('add', function() {
//     //     // insertWithWhomTagToInputValue();
//     //     console.log("WithWhomTag Value: ", withWhomTag.value);
//     // });
//     //
//     // // payer와 withWhom 값이 중복되지 않도록
//     // function compareAndRemoveTag() {
//     //     const withWhomValue = withWhomTag.value.map(item => item.value);  // withWhomTag에 입력된 값
//     //     const payerValue = payerTag.value.length > 0 ? payerTag.value[0].value : null;  // payerTag에 입력된 값
//     //
//     //     if (withWhomValue.includes(payerValue)) {
//     //         // 'remove' 이벤트 트리거로 withWhomTag에서 값 제거
//     //         withWhomTag.removeTags(payerValue);
//     //         console.log(`Removed: ${payerValue} from withWhomTag because it matches payerTag value`);
//     //     }
//     // }
//     // payerTag.on('add', function() {
//     //     // compareAndRemoveTag();
//     //     console.log("PayerTag Value: ", payerTag.value);
//     // });
//     // withWhomTag.on('add', function() {
//     //     compareAndRemoveTag();
//     //     console.log("WithWhomTag Value: ", withWhomTag.value);
//     // });
//     // payerTag.on('remove', function() {
//     //     compareAndRemoveTag();
//     //     console.log("PayerTag Value After Remove: ", payerTag.value);
//     // });
//
// }
// Tagify 로드가 완료될 때까지 기다리는 함수
// function waitForTagifyToLoad() {
//     return new Promise((resolve) => {
//         let checkTagifyLoaded = setInterval(() => {
//             if (typeof Tagify !== "undefined") {
//                 clearInterval(checkTagifyLoaded);
//                 console.log("✅ Tagify 로드 완료!");
//                 resolve();
//             }
//         }, 100);  // 100ms마다 Tagify가 로드되었는지 확인
//     });
// }


//🎈 오른쪽 패널 - 경비 내역 추가 폼
// html : expense item 추가 폼
function getExpenseItemForm(itineraryId, itineraryEventId) {
    return `<form class="expense-item-creation-form" id="expenseItemCreationForm">
                <input type="text" class="expense-item-creation-content" id="expenseItemCreationContent" name="content" placeholder="📝지출 내용">
                <input type="number" class="expense-item-creation-expenditure" id="expenseItemCreationExpenditure" name="expenditure" required placeholder="💸(원)">
                <select class="expense-item-creation-payer" id="expenseItemCreationPayer" name="payer" required>
                    <option value="" disabled selected>😄지불한 사람</option>
                </select>
                <select class="expense-item-creation-withWhom" id="expenseItemCreationWithWhom"  name="withWhom" multiple> 
                    <option value="" disabled selected>👥함께한 사람</option>
                </select>
                <!-- Expense Item 추가 + 버튼 -->
                <button type="button" class="expense-item-addition-button" id="expenseItemAdditionPlusButton" data-iid='${itineraryId}' data-ieid='${itineraryEventId}'>
                    <i class="fa-solid fa-plus plus-icon"></i>
                </button>
            </form>`;
}

// select option 요소 생성
async function createTravelerOption(itineraryId, selectElement, explainText=null) {
    try {
        const travelerList = await callApiAt(`/api/itinerary/${itineraryId}/travelers`, "GET", null);
        const travelerNameList = travelerList.travelers.map(t => t.name);

        // select 요소
        const select = document.getElementById(selectElement);
        select.innerHTML = "";
        if (explainText) {
            const placeholder = document.createElement("option");
            // placeholder.id = "expenseItemCreationSelectDefaultValue";
            placeholder.value = "";
            placeholder.selected = true;
            placeholder.disabled = false;
            placeholder.textContent = `${explainText}`;
            select.appendChild(placeholder);
        }
        // option
        travelerNameList.forEach(travelerName => {
            const traveler = document.createElement("option");
            // traveler.id = `expenseItemCreationSelectValue-${travelerName}`;
            traveler.value = travelerName;
            traveler.textContent = `@${travelerName}`;
            select.appendChild(traveler);
        });

        return travelerNameList;
    } catch (error) {
        console.error("에러 발생:", error);
    }
}


//🎈 오른쪽 패널 - +버튼 클릭 시 -> 경비 내역(expense item, with whom) 추가
$(document).off("click", ".expense-item-addition-button").on("click", ".expense-item-addition-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID

    // Request Data
    const content = $("#expenseItemCreationContent").val() || null;
    const expenditure = $("#expenseItemCreationExpenditure").val();
    const payer = $("#expenseItemCreationPayer").val();
    const withWhomValues = $("#expenseItemCreationWithWhom").val() || null;
    const withWhomList = [...new Set(withWhomValues)];


    // // 태그 - payer, withWhom 처리 부분은 그대로
    // const payerInput = document.querySelector("#expenseItemCreationPayer");
    // const payerTag = payerInput ? payerInput._tagify : null;
    // const payer = payerTag && payerTag.value.length > 0 ? payerTag.value[0].value : null;
    //
    // const withWhomInput = document.querySelector("#expenseItemCreationWithWhom");
    // const withWhomTag = withWhomInput ? withWhomInput._tagify : null;
    // const withWhomList = withWhomTag ? withWhomTag.value.map(item => item.value) : [];

    // 유효성 검사
    if (!expenditure || !payer) {
        alert("금액과 지출자는 반드시 입력해야 합니다.");
        return;
    }
    if (withWhomList.includes(payer)) {
        alert("Payer는 함께 계산할 수 없습니다!");
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


//💡 오른쪽 패널 - '-'버튼 클릭 시 -> 경비 내역(expense item, with whom) 삭제
$(document).off("click", ".expense-item-delete-button").on("click", ".expense-item-delete-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID
    const emid = $(this).data("emid"); // expense item ID

    await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense/${emid}`, "DELETE", null);

    location.reload();
});

//💡 오른쪽 패널 - 연필 버튼 클릭 시 -> 경비 내역(expense item, with whom) 수정
$(document).off("click", ".expense-item-edit-button").on("click", ".expense-item-edit-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID
    const emid = $(this).data("emid"); // expense item ID

    // 기존 값 가져오기
    const expenseItemBox = document.getElementById(`expenseItemBox-${emid}`);

    const expenseItemContent = expenseItemBox.querySelector(".expense-item-content");
    // const content = expenseItemContent.textContent;
    const expenseItemExpenditure = expenseItemBox.querySelector(".expense-item-expenditure");
    // const expenditure = parseInt(expenseItemExpenditure.textContent);
    const expenseItemPayer = expenseItemBox.querySelector(".expense-item-payer");
    const payer = expenseItemPayer.textContent.replace("@", ""); // "@" 제거
    const expenseItemWithWhom = expenseItemBox.querySelector(".expense-item-with-whom span");
    const withWhom = expenseItemWithWhom.textContent.replace("💡 함께한 사람: ", "").split(", ").map(name => name.replace("@", ""));

    // 수정 시 변경
    expenseItemContent.setAttribute("contenteditable", "true");
    expenseItemContent.setAttribute("style", "height: 35px;");
    expenseItemExpenditure.setAttribute("contenteditable", "true");
    expenseItemExpenditure.setAttribute("style", "height: 35px;");
    expenseItemExpenditure.setAttribute("style", "margin-right: 10px;");

    // select로 변경
    const payerSelect = document.createElement("select");
    payerSelect.setAttribute("class", "expense-item-payer-replace");
    payerSelect.setAttribute("id", `expenseItemPayerReplace-${emid}`);
    payerSelect.setAttribute("style", "height: 35px;");
    expenseItemPayer.parentNode.replaceChild(payerSelect, expenseItemPayer);

    const withWhomSelect = document.createElement("select");
    withWhomSelect.setAttribute("class", "expense-item-with-whom-replace");
    withWhomSelect.setAttribute("id", `expenseItemWithWhomReplace-${emid}`);
    withWhomSelect.setAttribute("multiple", "");
    payerSelect.parentNode.replaceChild(withWhomSelect, expenseItemWithWhom.parentNode);

    // 수정 버튼과 확인 버튼
    expenseItemBox.querySelector(".expense-item-edit-button").setAttribute("style", "display: none");
    expenseItemBox.querySelector(".expense-item-confirm-button").setAttribute("style", "display: flex");

    // payer와 withWhom 목록 나열
    // setTimeout(async () => {
    const newExpenseItemPayer = expenseItemBox.querySelector(".expense-item-payer-replace");
    const newExpenseItemWithWhom = expenseItemBox.querySelector(".expense-item-with-whom-replace");

    await createTravelerOption(iid, `expenseItemPayerReplace-${emid}`, null); //`@${payer}`);
    await createTravelerOption(iid, `expenseItemWithWhomReplace-${emid}`, null); // withWhom.map(name => `@${name}`).join(", "));
    newExpenseItemPayer.querySelector(`option[value="${payer}"]`).selected=true;
    for (const option of newExpenseItemWithWhom.options) {
        if (withWhom.includes(option.value)) {
            option.selected = true;
        }
    }


    // }, 1000);
});


//💡 오른쪽 패널 - 체크 버튼 클릭 시 -> 경비 내역(expense item, with whom) 수정 폼 전송
$(document).off("click", ".expense-item-confirm-button").on("click", ".expense-item-confirm-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID
    const emid = $(this).data("emid"); // expense item ID


    // 수정된 값 가져오기
    const expenseItemBox = document.getElementById(`expenseItemBox-${emid}`);

    const content = expenseItemBox.querySelector(".expense-item-content").textContent.trim();
    const expenditure = parseInt(expenseItemBox.querySelector(".expense-item-expenditure").textContent.trim());
    const payer = expenseItemBox.querySelector(".expense-item-payer-replace").value;
    const withWhomSelect = expenseItemBox.querySelector(".expense-item-with-whom-replace");
    const withWhomList = [...withWhomSelect.selectedOptions].map(option => option.value);

    // 유효성 검사
    if (!expenditure || !payer) {
        alert("금액과 지출자는 반드시 입력해야 합니다.");
        return;
    }
    if (withWhomList.includes(payer)) {
        alert("Payer는 함께 계산할 수 없습니다!");
        return;
    }



    const expenseItemRequestData = {
        content: content,
        payer: payer,
        expense: expenditure
    };

    const withWhomData = {
        withWhomNames: withWhomList
    };

    try {
        await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense/${emid}`, "PUT", expenseItemRequestData); // updateExpenseItem(iid, ieid, emid, expenseItemRequestData);
        await callApiAt(`/api/itineraries/${iid}/expense/${emid}/withWhom`, "DELETE", null); // 절대 경로(맨 앞에 / 붙이기)
        await callApiAt(`/api/itineraries/${iid}/expense/${emid}/withWhom`, "POST", withWhomData);

        // 🎯 페이지 새로고침 (데이터 반영을 위해)
        location.reload();
    } catch (error) {
        console.error("🚨 데이터 수정 중 오류 발생:", error);
        alert("지출 항목을 수정하는 중 오류가 발생했습니다.");
    }


});


// 🎈 ItineraryEvent 별로 ExpenseItem들 조회
async function getExpenseBookForWritingByItineraryEvent(iid, ieid) {
    try {
        // expenseItem 데이터 가져오기
        const expenseItems = await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense`, "GET", null);
        const totalAdjustmentData = await callApiAt(`/api/itineraries/${iid}/adjustment`, "GET", null);
        const travelerData = await callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);
        const placeData = await callApiAt(`/api/itinerary/${iid}/events/${ieid}`, "GET", null);

        const adjustmentBasicInfoPlace = $("#adjustmentBasicInfoPlace");
        const adjustmentBasicInfoTraveler = $("#adjustmentBasicInfoTraveler");
            // 값 가져오기
        // 남은 예산
        const remainedBudget = totalAdjustmentData.totalBalance;
        // 함께하는 traveler
        const numberOfTravelers = travelerData.numberOfTravelers;
        const placeName = placeData.placeDTO.placeName;

            // 렌더링
        // 장소
        adjustmentBasicInfoPlace.html(`현재 위치 : ${placeName}`);
        // 함께하는 traveler
        adjustmentBasicInfoTraveler.html(`${numberOfTravelers} 명과 함께하고 있습니다`);

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
                    <div class="expense-item-expenditure" id="expenseItemExpenditure">${expenseItem.expense} 원</div>
                    <div class="expense-item-payer" id="expenseItemPayer">@${expenseItem.travelerDTO.travelerName}</div>
                    <div class="expense-item-with-whom" id="expenseItemWithWhom-${expenseItem.id}"><span class="with-whom" data-emid="${expenseItem.id}">💡 함께한 사람: 로딩 중...</span></div>
                    <button type="button" class="expense-item-edit-button" id="expenseItemEditButton" data-iid="${iid}", data-ieid="${ieid}" data-emid="${expenseItem.id}">
                        <i class="fa-solid fa-pen edit-icon"></i>
                    </button>
                    <button type="button" class="expense-item-confirm-button" id="expenseItemConfirmButton" data-iid="${iid}", data-ieid="${ieid}" data-emid="${expenseItem.id}">
                        <i class="fa-solid fa-check confirm-icon"></i> <!-- 체크 아이콘 -->
                    </button>
                    <button type="button" class="expense-item-delete-button" id="expenseItemDeleteButton" data-iid="${iid}", data-ieid="${ieid}" data-emid="${expenseItem.id}">
                        <i class="fa fa-minus minus-icon"></i>
                    </button>
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
                    `${withWhomData.map(withWhom => `@${withWhom.travelerDTO.travelerName}`).join(", ")}`
                );
            } catch (whomError) {
                console.error(`Error loading withWhom data for expense ${expenseItem.id}:`, whomError);

            }
        }

    } catch (error) {
        console.error("Error loading expense data:", error);
    }
}


// 🎈왼쪽 패널 - 현재 총 지출액 클릭 : Itinerary Event 별 정산 정보 오른쪽 패널에 로드
$(document).on("click", ".event-total-expense", async function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기
    eventElementForTab = $(this);

    await loadAdjustmentPage(iid, ieid);
});

async function loadAdjustmentPage(itineraryId, itineraryEventId) {
    // adjustment-right.html을 오른쪽 화면`#detailContainer` 영역에 로드
    fetch(`/itinerary/${itineraryId}/events/${itineraryEventId}/adjustment-right`)
        .then(response => {
            // response.text()
            if (!response.ok) throw new Error(`HTTP 오류! 상태 코드: ${response.status}`);
            return response.text();
        })
        .then(html => {
            $("#detailContainer").html(html);
            // "정산" 탭을 활성화(css), "경비" 탭을 비활성화(css)
            document.getElementById("adjustmentTab").setAttribute("style", "background-color: #ffffff; color: #8e8b82;");
            document.getElementById("expenditureTab").setAttribute("style", "background-color: #8e8b82; color: #e9dcbe;");
            if ($("#itineraryEventAdjustmentInfo").length === 0) {
                console.error("❌ itineraryEventAdjustmentInfo 요소를 찾을 수 없습니다.");
                return;
            }
            getAdjustmentByItineraryEvent(itineraryId, itineraryEventId);
        })
        .catch(error => console.error("Error loading adjustment-right.html:", error));
}


// 💡 itinerary Event 별 정산 정보 조회
async function getAdjustmentByItineraryEvent(iid, ieid) {
    try {
        // adjustment 데이터 가져오기
        const adjustmentData = await callApiAt(`/api/itineraries/${iid}/events/${ieid}/adjustment`, "GET", null);
        const totalAdjustmentData = await callApiAt(`/api/itineraries/${iid}/adjustment`, "GET", null);
        const travelerData = await callApiAt(`/api/itinerary/${iid}/travelers`, "GET", null);
        const placeData = await callApiAt(`/api/itinerary/${iid}/events/${ieid}`, "GET", null);

        const individualAdjustmentList = $("#individualAdjustmentList");
        const totalExpenditure = $("#totalExpenditure");
        const adjustmentInfo = $("#itineraryEventAdjustmentInfo");
        const individualExpenditureList = $("#individualExpenditureList");
        const adjustmentBasicInfoPlace = $("#adjustmentBasicInfoPlace");
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
        const placeName = placeData.placeDTO.placeName;


        // 장소
        adjustmentBasicInfoPlace.html(`현재 위치 : ${placeName}`);
        // 지출 렌더링
            // 함께하는 traveler
        adjustmentBasicInfoTraveler.html(`${numberOfTravelers} 명과 함께하고 있습니다`);
            // 총 지출
        let totalExpenditureDetails = `<p class="total-expenditure-money-align"><span class="total-expenditure-money-label">총 지출</span>    <span class="total-expenditure-money">${totalExpense} 원</span></p>`;
        totalExpenditure.html(totalExpenditureDetails);

            // 개인 지출
        let individualExpenditureDetails = "";
        for (const [name, expense] of Object.entries(eachExpenses)) {
            individualExpenditureDetails += `<p class="individual-expenditure"><span class="individual-expenditure-label">@${name}</span><br><!--<span class="individual-expenditure-label">지출</span>--> <span class="total-expenditure-money">${expense.toLocaleString()} 원</span></p>`;
        }
        individualExpenditureList.html(individualExpenditureDetails);

        // 정산 렌더링
        let individualAdjustmentDetails = "";

        // 표
        for (const [name, details] of Object.entries(adjustment)) {
            // 수금(receivedMoney) 항목 처리
            const receivedData = Object.entries(details.receivedMoney || {});
            const receivedRows = receivedData
                .map(([from, amount], index) => `
                    <div class="adjustment-received-row">
                        ${index === 0 ? '<div class="adjustment-received-label">수금</div>' : '<div class="adjustment-received-label"></div>'} <!-- 첫 번째만 "수금" 표시 -->
                        <div class="adjustment-received"><span class="adjustment-received-from">@${from}</span> <span class="adjustment-received-amount">${amount.toLocaleString()} 원</span></div>
                    </div>
        `)
        .join("") || `
                    <div class="adjustment-received-row">
                        <div class="adjustment-received-label">수금</div>
                        <div class="adjustment-received-amount">-</div>
                    </div>
                    `;

            // 송금(sendedMoney) 항목 처리
        const sendedData = Object.entries(details.sendedMoney || {});
        const sendedRows = sendedData
            .map(([to, amount], index) => `
                    <div class="adjustment-send-row">
                        ${index === 0 ? '<div class="adjustment-send-label">송금</div>' : '<div class="adjustment-send-label"></div>'} <!-- 첫 번째만 "송금" 표시 -->
                        <div class="adjustment-send"><span class="adjustment-send-to">@${to}</span> <span class="adjustment-send-amount">${amount.toLocaleString()} 원</span></div>
                    </div>
                    `)
                .join("") || `
        <div class="adjustment-send-row">
            <div class="adjustment-send-label">송금</div>
            <div class="adjustment-send-amount">-</div>
        </div>
    `;

            // 전체 구조
            individualAdjustmentDetails += `
    <div class="adjustment-container">
        <div class="adjustment-subject">@${name}</div>
        ${sendedRows}
        ${receivedRows}
    </div>
`;
        }
        individualAdjustmentList.html(individualAdjustmentDetails);


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
