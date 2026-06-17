(() => {
    const app = document.querySelector("[data-notification-page]");
    if (!app) {
        return;
    }

    const listUrl = app.dataset.notificationListUrl || "/api/v1/notifications/me";
    const list = app.querySelector("[data-notification-list]");
    const refreshButton = app.querySelector("[data-notification-refresh]");
    const unreadFilter = app.querySelector("[data-notification-unread-filter]");

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
                ...(options.headers || {})
            },
            ...options
        });
        const payload = await response.json().catch(() => null);
        if (!response.ok || !payload || payload.success !== true) {
            throw new Error(payload?.message || "요청 처리에 실패했습니다.");
        }
        return payload.data;
    };

    const formatDateTime = (value) => {
        if (!value) {
            return "기록 없음";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return date.toLocaleString("ko-KR", { hour12: false });
    };

    const resourceLabel = (code) => ({
        GENERAL: "일반 안내",
        MATCHING_CASE: "매칭 공고",
        APPLICATION_PROGRESS: "신청 진행",
        CONSULTATION_RESERVATION: "상담",
        PAYMENT_TRANSACTION: "결제",
        DOCUMENT_SUBMISSION: "서류"
    })[code] || "업무 안내";

    const createEmpty = (message) => {
        const empty = document.createElement("p");
        empty.className = "field-help";
        empty.textContent = message;
        return empty;
    };

    const markRead = async (notificationId, card) => {
        if (!notificationId) {
            return;
        }
        await requestJson(`/api/v1/notifications/${encodeURIComponent(notificationId)}/read`, {
            method: "PATCH"
        });
        card.classList.add("is-read");
        const status = card.querySelector("[data-notification-read-state]");
        if (status) {
            status.textContent = "읽음";
        }
        const button = card.querySelector("[data-notification-read-button]");
        if (button) {
            button.disabled = true;
            button.textContent = "확인 완료";
        }
        await window.SanebNotifications?.refreshUnreadCount?.();
    };

    const renderNotifications = (items) => {
        list.replaceChildren();
        if (!items || items.length === 0) {
            list.append(createEmpty(unreadFilter?.checked ? "읽지 않은 알림이 없습니다." : "알림이 없습니다."));
            return;
        }
        items.forEach((item) => {
            const card = document.createElement("article");
            card.className = `notification-card${item.readAt ? " is-read" : ""}`;

            const header = document.createElement("div");
            header.className = "notification-card-header";

            const titleWrap = document.createElement("div");
            const eyebrow = document.createElement("span");
            eyebrow.className = "eyebrow";
            eyebrow.textContent = resourceLabel(item.resourceType);
            const title = document.createElement("h3");
            title.textContent = item.title || "알림";
            titleWrap.append(eyebrow, title);

            const readState = document.createElement("span");
            readState.className = "notification-read-state";
            readState.dataset.notificationReadState = "";
            readState.textContent = item.readAt ? "읽음" : "읽지 않음";
            header.append(titleWrap, readState);

            const body = document.createElement("p");
            body.className = "notification-body";
            body.textContent = item.body || "";

            const meta = document.createElement("dl");
            meta.className = "flow-summary-list notification-meta";
            const created = document.createElement("div");
            const createdDt = document.createElement("dt");
            createdDt.textContent = "발생일";
            const createdDd = document.createElement("dd");
            createdDd.textContent = formatDateTime(item.createdAt);
            created.append(createdDt, createdDd);
            const sent = document.createElement("div");
            const sentDt = document.createElement("dt");
            sentDt.textContent = "처리상태";
            const sentDd = document.createElement("dd");
            sentDd.textContent = item.statusCode === "SENT" ? "전달됨" : item.statusCode || "상태 없음";
            sent.append(sentDt, sentDd);
            meta.append(created, sent);

            const actions = document.createElement("div");
            actions.className = "notification-actions";
            const button = document.createElement("button");
            button.type = "button";
            button.className = "secondary-action";
            button.dataset.notificationReadButton = "";
            button.textContent = item.readAt ? "확인 완료" : "읽음 처리";
            button.disabled = Boolean(item.readAt);
            button.addEventListener("click", async () => {
                button.disabled = true;
                button.textContent = "처리 중";
                try {
                    await markRead(item.notificationId, card);
                } catch (error) {
                    button.disabled = false;
                    button.textContent = "읽음 처리";
                }
            });
            actions.append(button);

            card.append(header, body, meta, actions);
            list.append(card);
        });
    };

    const loadNotifications = async () => {
        list.replaceChildren(createEmpty("알림을 불러오는 중입니다."));
        try {
            const params = new URLSearchParams({
                page: "1",
                size: "50"
            });
            if (unreadFilter?.checked) {
                params.set("unreadOnly", "true");
            }
            const data = await requestJson(`${listUrl}?${params.toString()}`);
            renderNotifications(data.items || []);
        } catch (error) {
            list.replaceChildren(createEmpty(error.message || "알림을 불러오지 못했습니다."));
        }
    };

    refreshButton?.addEventListener("click", loadNotifications);
    unreadFilter?.addEventListener("change", loadNotifications);
    loadNotifications();
})();
