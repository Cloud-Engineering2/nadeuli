document.addEventListener("DOMContentLoaded", function () {
    const isLoggedIn = true; // 로그인 여부
    const userName = "나들이 님";
    const userEmail = "nadeuli@nadeuli.store";
    const userRole = "ROLE_ADMIN"; // 현재 사용자 역할
    const userRoleAdmin = "ROLE_ADMIN";
    const userRoleMember = "ROLE_MEMBER";

    function showLoggedInUI() {
        const header = document.getElementById("right-header");

        let menuItems = "";

        if (userRole === userRoleAdmin) {
            menuItems = `
        <a href="#">지역 관리</a>
        <a href="#">일정 관리</a>
        <a href="#">회원 관리</a>
      `;
        } else if (userRole === userRoleMember) {
            menuItems = `
        <a href="#">일정 생성</a>
        <a href="#">일정 확인</a>
        <a href="#">내 정보</a>
      `;
        }

        header.innerHTML = `
      <div class="balloon" id="balloon-icon-wrapper">
        <svg id="balloon-icon" xmlns="http://www.w3.org/2000/svg" width="30" height="30" color="#343434" class="bi bi-balloon" viewBox="0 0 16 16">
          <path fill-rule="evenodd" d="M8 9.984C10.403 9.506 12 7.48 12 5a4 4 0 0 0-8 0c0 2.48 1.597 4.506 4 4.984M13 5c0 2.837-1.789 5.227-4.52 5.901l.244.487a.25.25 0 1 1-.448.224l-.008-.017c.008.11.02.202.037.29.054.27.161.488.419 1.003.288.578.235 1.15.076 1.629-.157.469-.422.867-.588 1.115l-.004.007a.25.25 0 1 1-.416-.278c.168-.252.4-.6.533-1.003.133-.396.163-.824-.049-1.246l-.013-.028c-.24-.48-.38-.758-.448-1.102a3 3 0 0 1-.052-.45l-.04.08a.25.25 0 1 1-.447-.224l.244-.487C4.789 10.227 3 7.837 3 5a5 5 0 0 1 10 0m-6.938-.495a2 2 0 0 1 1.443-1.443C7.773 2.994 8 2.776 8 2.5s-.226-.504-.498-.459a3 3 0 0 0-2.46 2.461c-.046.272.182.498.458.498s.494-.227.562-.495"/>
        </svg>
      </div>
      <div class="dropdown" id="dropdown">
        <div style="padding: 5px; color: #343434; text-align: center;">${userName}</div>
        <div style="padding: 5px; font-size: 15px; color: #8E8B82; text-align: center;">${userEmail}</div>
        <hr>
        ${menuItems}
        <hr>
        <a href="#">로그아웃</a>
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
        if (!dropdown) return;
        dropdown.style.display = isHovered ? "flex" : "none";
    }

    function showLoggedOutUI() {
        const header = document.getElementById("right-header");
        header.innerHTML = '<a href="/login" style="text-decoration: none; color: #8E8B82; font-size: 25px;">로그인</a>';
    }

    isLoggedIn ? showLoggedInUI() : showLoggedOutUI();
});
