/* header.js
 * nadeuli Service - 여행
 * header.html 에서 사용할 js 함수 정리
 * 작성자 : 박한철
 * 최종 수정 날짜 : 2025.03.14
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.14     header.html 에 있던 script 를 header.js 로 분리
 *                         일정 생성, 내 일정 링크 설정
 * 박한철    2025.03.19     로그인 확인 / 로그아웃 처리
 * 이홍비    2025.03.20     글자 수정
 * ========================================================
 */

const userRoleAdmin = "ADMIN";
const userRoleMember = "MEMBER";


function logout() {
    fetch("/auth/logout", {
        method: "POST",
        credentials: "include"
    }).then(res => res.json())
        .then(data => {
            if (data.success) {
                alert("로그아웃 완료");
                location.href = "/login"; // 또는 메인 페이지로
            }
        });
}


document.addEventListener("DOMContentLoaded", function () {
    console.log('User Name:', userName);
    console.log('User Email:', userEmail);
    console.log('Profile Image:', profileImage);
    console.log('Role:', userRole);
    isLoggedIn ? showLoggedInUI() : showLoggedOutUI();
});


function showLoggedInUI() {
    const header = document.getElementById("right-header");
    console.log("로그인 상태입니다.");
    let menuItems = "";

    if (userRole === userRoleAdmin) {
        console.log("관리자입니다.");
        menuItems = `
        <a href="/admin/region">지역 관리</a>
        <a href="/itinerary/create">일정 생성</a>
        <a href="/itinerary/mylist">내 일정</a>
        <a href="/mypage">내 정보</a>
      `;
    } else if (userRole === userRoleMember) {
        console.log("회원입니다.");
        menuItems = `
        <a href="/itinerary/create">일정 생성</a>
        <a href="/itinerary/mylist">내 일정</a>
        <a href="/mypage">내 정보</a>
      `;
    }

    header.innerHTML = `
            <div class="balloon" id="balloon-icon-wrapper">
      
            <!-- 기본 아이콘 (비어있는 버전) -->
              <svg id="balloon-icon-default" xmlns="http://www.w3.org/2000/svg" width="30" height="30" class="bi bi-balloon" viewBox="0 0 16 16">
                 <path fill-rule="evenodd" d="M8 9.984C10.403 9.506 12 7.48 12 5a4 4 0 0 0-8 0c0 2.48 1.597 4.506 4 4.984M13 5c0 2.837-1.789 5.227-4.52 5.901l.244.487a.25.25 0 1 1-.448.224l-.008-.017c.008.11.02.202.037.29.054.27.161.488.419 1.003.288.578.235 1.15.076 1.629-.157.469-.422.867-.588 1.115l-.004.007a.25.25 0 1 1-.416-.278c.168-.252.4-.6.533-1.003.133-.396.163-.824-.049-1.246l-.013-.028c-.24-.48-.38-.758-.448-1.102a3 3 0 0 1-.052-.45l-.04.08a.25.25 0 1 1-.447-.224l.244-.487C4.789 10.227 3 7.837 3 5a5 5 0 0 1 10 0m-6.938-.495a2 2 0 0 1 1.443-1.443C7.773 2.994 8 2.776 8 2.5s-.226-.504-.498-.459a3 3 0 0 0-2.46 2.461c-.046.272.182.498.458.498s.494-.227.562-.495"/>
              </svg>
            
              <!-- 호버 시 보여줄 아이콘 (채워진 버전) -->
              <svg id="balloon-icon-filled" xmlns="http://www.w3.org/2000/svg" width="30" height="30" class="bi bi-balloon" viewBox="0 0 16 16" style="display: none;">
                <path fill-rule="evenodd" d="M8.48 10.901C11.211 10.227 13 7.837 13 5A5 5 0 0 0 3 5c0 2.837 1.789 5.227 4.52 5.901l-.244.487a.25.25 0 1 0 .448.224l.04-.08c.009.17.024.315.051.45.068.344.208.622.448 1.102l.013.028c.212.422.182.85.05 1.246-.135.402-.366.751-.534 1.003a.25.25 0 0 0 .416.278l.004-.007c.166-.248.431-.646.588-1.115.16-.479.212-1.051-.076-1.629-.258-.515-.365-.732-.419-1.004a2 2 0 0 1-.037-.289l.008.017a.25.25 0 1 0 .448-.224zM4.352 3.356a4 4 0 0 1 3.15-2.325C7.774.997 8 1.224 8 1.5s-.226.496-.498.542c-.95.162-1.749.78-2.173 1.617a.6.6 0 0 1-.52.341c-.346 0-.599-.329-.457-.644"/>
              </svg>
    
         
          </div>
          <div class="dropdown" id="dropdown">
            <div class="main-header-user-name">${userName}</div>
            <div class="main-header-user-email">${userEmail}</div>
            <hr>
            ${menuItems}
            <hr>
            <button class="logout-btn" onclick="logout()">로그아웃</button>
           </div>
    `;

    addDropdownEventListeners();
}

function addDropdownEventListeners() {
    const dropdown = document.getElementById("dropdown");
    const balloonWrapper = document.getElementById("balloon-icon-wrapper");

    if (!dropdown || !balloonWrapper) return;

    balloonWrapper.addEventListener("mouseenter", () => {
        toggleBalloon(true);
    });

    balloonWrapper.addEventListener("mouseleave", () => {
        toggleBalloon(false);
    });

    dropdown.addEventListener("mouseenter", () => {
        toggleBalloon(true);
    });

    dropdown.addEventListener("mouseleave", () => {
        toggleBalloon(false);
    });
}

function toggleBalloon(isHovered) {
    const dropdown = document.getElementById("dropdown");
    const iconDefault = document.getElementById("balloon-icon-default");
    const iconFilled = document.getElementById("balloon-icon-filled");
    if (!dropdown || !iconDefault || !iconFilled) return;
    if (isHovered) {
        iconDefault.style.display = "none";
        iconFilled.style.display = "inline";
        dropdown.style.display = "flex";
    } else {
        iconDefault.style.display = "inline";
        iconFilled.style.display = "none";
        dropdown.style.display = "none";
    }
}

function showLoggedOutUI() {
    console.log("비로그인 상태입니다.");
    const header = document.getElementById("right-header");
    header.innerHTML = '<a href="/login" class="main-header-login-link">로그인</a>';
}