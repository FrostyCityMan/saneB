(() => {
    const app = document.querySelector("[data-consultation-app]");
    if (!app) {
        return;
    }

    const operating = app.dataset.operating === "true";
    const requestForm = app.querySelector("[data-consultation-request-form]");
    const statusForm = app.querySelector("[data-consultation-status-form]");
    const requestMessage = app.querySelector("[data-consultation-message]");
    const statusMessage = app.querySelector("[data-consultation-status-message]");
    const requestButton = app.querySelector("[data-consultation-request-submit]");
    const statusButton = app.querySelector("[data-consultation-status-submit]");
    const refreshButton = app.querySelector("[data-consultation-refresh]");
    const list = app.querySelector("[data-consultation-list]");

    const statusLabels = {
        REQUESTED: "요청 접수",
        ASSIGNED: "담당자 배정",
        CONFIRMED: "상담 확정",
        CANCELED: "취소",
        COMPLETED: "상담 완료",
        NO_SHOW: "미참석"
    };

    const setMessage = (target, text, status = "info") => {
        if (!target) {
            return;
        }
        target.textContent = text || "";
        target.classList.toggle("is-success", status === "success");
        target.classList.toggle("is-error", status === "error");
    };

    const setBusy = (button, busy, text) => {
        if (!button) {
            return;
        }
        if (!button.dataset.defaultText) {
            button.dataset.defaultText = button.textContent;
        }
        button.disabled = busy;
        button.textContent = busy ? text : button.dataset.defaultText;
    };

    const valueOf = (form, name) => String(form.querySelector(`[name='${name}']`)?.value || "").trim();

    const textOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

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

    const selectReservation = (reservation) => {
        if (!statusForm) {
            return;
        }
        statusForm.querySelector("[name='reservationId']").value = reservation.reservationId || "";
        statusForm.querySelector("[name='partnerUserId']").value = reservation.partnerUserId || "";
        statusForm.querySelector("[name='note']").value = reservation.statusNote || "";
        setMessage(statusMessage, "선택한 상담 요청을 처리할 수 있습니다.", "success");
    };

    const renderReservations = (reservations) => {
        if (!list) {
            return;
        }
        list.replaceChildren();
        if (reservations.length === 0) {
            const empty = document.createElement("p");
            empty.className = "field-help";
            empty.textContent = "조회된 상담 요청이 없습니다.";
            list.append(empty);
            return;
        }
        reservations.forEach((reservation) => {
            const card = document.createElement("article");
            card.className = "consultation-card";
            card.innerHTML = `
                <div>
                    <span class="eyebrow">${statusLabels[reservation.statusCode] || reservation.statusCode}</span>
                    <h3>${reservation.requestNote || "상담 요청"}</h3>
                </div>
                <dl class="flow-summary-list">
                    <div><dt>상담 요청 ID</dt><dd>${reservation.reservationId}</dd></div>
                    <div><dt>회원 ID</dt><dd>${reservation.memberUserId}</dd></div>
                    <div><dt>담당자 ID</dt><dd>${reservation.partnerUserId || "미배정"}</dd></div>
                    <div><dt>접수일</dt><dd>${formatDateTime(reservation.createdAt)}</dd></div>
                </dl>
            `;
            if (operating) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "secondary-action";
                button.textContent = "이 요청 처리";
                button.addEventListener("click", () => selectReservation(reservation));
                card.append(button);
            }
            list.append(card);
        });
    };

    const loadReservations = async () => {
        try {
            const data = await requestJson("/api/v1/consultation-reservations?page=1&size=20");
            renderReservations(data.items || []);
        } catch (error) {
            if (list) {
                list.replaceChildren();
                const message = document.createElement("p");
                message.className = "field-help";
                message.textContent = error.message || "상담 요청 목록을 불러오지 못했습니다.";
                list.append(message);
            }
        }
    };

    requestForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const requestNote = textOrNull(valueOf(requestForm, "requestNote"));
        if (!requestNote) {
            setMessage(requestMessage, "상담 요청 내용을 입력하세요.", "error");
            return;
        }
        const memberUserId = operating ? textOrNull(valueOf(requestForm, "memberUserId")) : null;
        setBusy(requestButton, true, "접수 중");
        setMessage(requestMessage, "");
        try {
            await requestJson("/api/v1/consultation-reservations", {
                method: "POST",
                body: JSON.stringify({
                    memberUserId,
                    requestNote
                })
            });
            requestForm.reset();
            setMessage(requestMessage, "상담 요청을 접수했습니다.", "success");
            await loadReservations();
        } catch (error) {
            setMessage(requestMessage, error.message || "상담 요청 접수에 실패했습니다.", "error");
        } finally {
            setBusy(requestButton, false, "접수 중");
        }
    });

    statusForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const reservationId = textOrNull(valueOf(statusForm, "reservationId"));
        if (!reservationId) {
            setMessage(statusMessage, "처리할 상담 요청을 선택하세요.", "error");
            return;
        }
        setBusy(statusButton, true, "저장 중");
        setMessage(statusMessage, "");
        try {
            await requestJson(`/api/v1/consultation-reservations/${encodeURIComponent(reservationId)}/status`, {
                method: "PATCH",
                body: JSON.stringify({
                    statusCode: valueOf(statusForm, "statusCode"),
                    partnerUserId: textOrNull(valueOf(statusForm, "partnerUserId")),
                    note: textOrNull(valueOf(statusForm, "note"))
                })
            });
            setMessage(statusMessage, "상담 상태를 저장했습니다.", "success");
            await loadReservations();
        } catch (error) {
            setMessage(statusMessage, error.message || "상담 상태 저장에 실패했습니다.", "error");
        } finally {
            setBusy(statusButton, false, "저장 중");
        }
    });

    refreshButton?.addEventListener("click", loadReservations);
    loadReservations();
})();
