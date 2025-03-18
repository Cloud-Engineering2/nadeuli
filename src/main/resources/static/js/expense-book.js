/************* 🧳 전역 변수 선언 🧳 *************/

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


/************** 🧳 경비 & 작성 이벤트 핸들링 🧳 **************/

//🎈 왼쪽 패널 - +경비 내역 추가 클릭 시 -> 오른쪽 패널에 경비 내역 로드
$(document).on("click", ".expense-item-list-addition", async function () {
    const iid = $(this).data("iid");   // itinerary ID 가져오기
    const ieid = $(this).data("ieid"); // event ID 가져오기

    // const element = document.getElementById("adjustmentHeaderTabInactive");
    // element.setAttribute("data-iid", iid);  // data-iid 설정
    // element.setAttribute("data-ieid", ieid); // data-ieid 설정

    await loadExpenseItemListAndAddition(iid, ieid);
});


async function loadExpenseItemListAndAddition(iid, ieid) {
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
        })
        .catch(error => console.error("Error loading expense-right.html:", error)
        );

}

// Tagify - tag 생성
async function createTag(itineraryId) {
    // input 요소 가져오기
    let payer = document.querySelector('input[name=payer]');
    let withWhom = document.querySelector('input[name=withWhom]');

    if (!payer || !withWhom) {
        console.error("Input elements not found.");
        return;
    }

    // traveler 조회
    const travelerList = await callApiAt(`/api/itinerary/${itineraryId}/travelers`, "GET", null);
    const travelers = travelerList.travelers.map(t => t.name);

    // dropdown 메뉴
    let payerDropdown = { classname: "payer-dropdown-menu", closeOnSelect: false };
    let withWhomDropdown = { classname: "withWhom-dropdown-menu", closeOnSelect: false };


    // 태그 생성
    var payerTag = new Tagify(payer, {mode: 'input', whitelist: travelers, maxTags: 1, enforceWhitelist: true});
    var withWhomTag = new Tagify(withWhom, {mode: 'input', whitelist: travelers, enforceWhitelist: true});

    // 태그 추가 이벤트 처리
    payerTag.on('add', function() {
        console.log("PayerTag Value: ", payerTag.value);
    });

    withWhomTag.on('add', function() {
        console.log("WithWhomTag Value: ", withWhomTag.value);
    });

    // payer와 withWhom 값이 중복되지 않도록 처리하는 부분 (필요에 따라 활성화)
    payerTag.on('add', function() {
        compareAndRemoveTag(payerTag, withWhomTag);
    });

    withWhomTag.on('add', function() {
        compareAndRemoveTag(payerTag, withWhomTag);
    });

    // payer와 withWhom 값이 중복되면 한쪽에서 제거하는 함수
    function compareAndRemoveTag(payerTag, withWhomTag) {
        const withWhomValue = withWhomTag.value.map(item => item.value);
        const payerValue = payerTag.value.length > 0 ? payerTag.value[0].value : null;

        if (withWhomValue.includes(payerValue)) {
            // 'remove' 이벤트 트리거로 withWhomTag에서 값 제거
            withWhomTag.removeTags(payerValue);
            console.log(`Removed: ${payerValue} from withWhomTag because it matches payerTag value`);
        }
    }



    // // withWhomTag 값을 input 태그의 value로 설정하는 함수
    // function insertWithWhomTagToInputValue() {
    //     const withWhomValue = withWhomTag.value.map(item => item.value).join(', ');
    //     withWhom.value = withWhomValue;
    //     console.log("Updated withWhom input value: ", withWhom.value);
    // }
    //
    // withWhomTag.on('add', function() {
    //     // insertWithWhomTagToInputValue();
    //     console.log("WithWhomTag Value: ", withWhomTag.value);
    // });
    //
    // // payer와 withWhom 값이 중복되지 않도록
    // function compareAndRemoveTag() {
    //     const withWhomValue = withWhomTag.value.map(item => item.value);  // withWhomTag에 입력된 값
    //     const payerValue = payerTag.value.length > 0 ? payerTag.value[0].value : null;  // payerTag에 입력된 값
    //
    //     if (withWhomValue.includes(payerValue)) {
    //         // 'remove' 이벤트 트리거로 withWhomTag에서 값 제거
    //         withWhomTag.removeTags(payerValue);
    //         console.log(`Removed: ${payerValue} from withWhomTag because it matches payerTag value`);
    //     }
    // }
    // payerTag.on('add', function() {
    //     // compareAndRemoveTag();
    //     console.log("PayerTag Value: ", payerTag.value);
    // });
    // withWhomTag.on('add', function() {
    //     compareAndRemoveTag();
    //     console.log("WithWhomTag Value: ", withWhomTag.value);
    // });
    // payerTag.on('remove', function() {
    //     compareAndRemoveTag();
    //     console.log("PayerTag Value After Remove: ", payerTag.value);
    // });

}
// Tagify 로드가 완료될 때까지 기다리는 함수
function waitForTagifyToLoad() {
    return new Promise((resolve) => {
        let checkTagifyLoaded = setInterval(() => {
            if (typeof Tagify !== "undefined") {
                clearInterval(checkTagifyLoaded);
                console.log("✅ Tagify 로드 완료!");
                resolve();
            }
        }, 100);  // 100ms마다 Tagify가 로드되었는지 확인
    });
}


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
            placeholder.id = "expenseItemCreationSelectDefaultValue";
            placeholder.value = "";
            placeholder.selected = true;
            placeholder.disabled = false;
            placeholder.textContent = `${explainText}`;
            select.appendChild(placeholder);
        }
        // option
        travelerNameList.forEach(travelerName => {
            const traveler = document.createElement("option");
            traveler.id = `expenseItemCreationSelectDefaultValue-${travelerName}`;
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


//🎈 오른쪽 패널 - '-'버튼 클릭 시 -> 경비 내역(expense item, with whom) 삭제
$(document).off("click", ".expense-item-delete-button").on("click", ".expense-item-delete-button", async function(event) {
    event.preventDefault(); // 폼 제출 방지

    const iid = $(this).data("iid");   // itinerary ID
    const ieid = $(this).data("ieid"); // event ID
    const emid = $(this).data("emid"); // expense item ID

    await callApiAt(`/api/itineraries/${iid}/events/${ieid}/expense/${emid}`, "DELETE", null);

    location.reload();
});


// 🎈 ItineraryEvent 별로 ExpenseItem들 조회
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
                    <div class="expense-item-expenditure" id="expenseItemExpenditure">${expenseItem.expense} 원</div>
                    <div class="expense-item-payer" id="expenseItemPayer">@${expenseItem.travelerDTO.travelerName}</div>
                    <div class="expense-item-with-whom" id="expenseItemWithWhom-${expenseItem.id}"><span class="with-whom" data-emid="${expenseItem.id}">💡 함께한 사람: 로딩 중...</span></div>
                    <button type="button" class="expense-item-edit-button" id="expenseItemEditButton" data-iid="${iid}", data-ieid="${ieid}" data-emid="${expenseItem.id}">
                        <i class="fa-solid fa-pen edit-icon"></i>
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



// 💡 itinerary Event 별 정산 정보 조회
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
