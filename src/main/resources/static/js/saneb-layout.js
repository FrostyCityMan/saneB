(() => {
    "use strict";

    const sidebar = document.querySelector(".saneb-sidebar");
    const toggle = document.querySelector("[data-mobile-nav-toggle]");
    const navigation = document.getElementById("sanebPrimaryNavigation");
    if (!sidebar || !toggle || !navigation) {
        return;
    }

    const closeMenu = () => {
        sidebar.classList.remove("is-menu-open");
        toggle.setAttribute("aria-expanded", "false");
    };

    const openMenu = () => {
        sidebar.classList.add("is-menu-open");
        toggle.setAttribute("aria-expanded", "true");
    };

    sidebar.classList.add("is-mobile-nav-ready");

    toggle.addEventListener("click", () => {
        if (sidebar.classList.contains("is-menu-open")) {
            closeMenu();
            return;
        }
        openMenu();
    });

    navigation.addEventListener("click", (event) => {
        if (event.target.closest("a") && window.matchMedia("(max-width: 760px)").matches) {
            closeMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && sidebar.classList.contains("is-menu-open")) {
            closeMenu();
            toggle.focus();
        }
    });

    window.addEventListener("resize", () => {
        if (!window.matchMedia("(max-width: 760px)").matches) {
            closeMenu();
        }
    });
})();
