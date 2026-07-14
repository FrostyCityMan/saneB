(() => {
    "use strict";

    const sidebar = document.querySelector(".saneb-sidebar");
    const toggle = document.querySelector("[data-mobile-nav-toggle]");
    const navigation = document.getElementById("sanebPrimaryNavigation");
    if (!sidebar || !toggle || !navigation) {
        return;
    }

    const groups = Array.from(navigation.querySelectorAll("[data-nav-group]"));
    const storageKey = `saneb.sidebar.open-group.${sidebar.dataset.navRole || "default"}`;

    const setGroupState = (group, expanded) => {
        const groupToggle = group.querySelector("[data-nav-group-toggle]");
        const panelId = groupToggle?.getAttribute("aria-controls");
        const panel = panelId ? document.getElementById(panelId) : null;
        if (!groupToggle || !panel) {
            return;
        }
        group.classList.toggle("is-open", expanded);
        groupToggle.setAttribute("aria-expanded", String(expanded));
        panel.hidden = !expanded;
    };

    const storeOpenGroup = (groupId) => {
        try {
            if (groupId) {
                window.localStorage.setItem(storageKey, groupId);
            } else {
                window.localStorage.removeItem(storageKey);
            }
        } catch (error) {
            // 저장소가 차단된 환경에서도 현재 페이지의 아코디언은 그대로 동작합니다.
        }
    };

    const initializeAccordion = () => {
        let storedGroupId = null;
        try {
            storedGroupId = window.localStorage.getItem(storageKey);
        } catch (error) {
            storedGroupId = null;
        }

        const activeGroup = navigation.querySelector(".side-link.active")?.closest("[data-nav-group]");
        const storedGroup = groups.find((group) => group.dataset.navGroupId === storedGroupId);
        const initialGroup = activeGroup || storedGroup || groups[0] || null;
        groups.forEach((group) => {
            group.classList.toggle("has-active-link", Boolean(group.querySelector(".side-link.active")));
            setGroupState(group, group === initialGroup);
        });
        navigation.classList.add("is-accordion-ready");

        groups.forEach((group) => {
            const groupToggle = group.querySelector("[data-nav-group-toggle]");
            if (!groupToggle) {
                return;
            }
            groupToggle.addEventListener("click", () => {
                const shouldOpen = groupToggle.getAttribute("aria-expanded") !== "true";
                groups.forEach((candidate) => setGroupState(candidate, shouldOpen && candidate === group));
                storeOpenGroup(shouldOpen ? group.dataset.navGroupId : null);
            });
        });
    };

    const closeMenu = () => {
        sidebar.classList.remove("is-menu-open");
        toggle.setAttribute("aria-expanded", "false");
    };

    const openMenu = () => {
        sidebar.classList.add("is-menu-open");
        toggle.setAttribute("aria-expanded", "true");
    };

    initializeAccordion();
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
