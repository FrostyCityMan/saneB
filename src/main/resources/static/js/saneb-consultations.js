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
    let selectedReservation = null;
    const personLabel = window.SanebPersonIdentity.label;

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

    const memberPicker = window.SanebPersonPicker(app.querySelector('[data-person-picker="member"]'), requestJson);
    const partnerPicker = window.SanebPersonPicker(app.querySelector('[data-person-picker="partner"]'), requestJson);

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
        const reservationCodeField = statusForm.querySelector("[data-consultation-reservation-code]");
        if (reservationCodeField) {
            reservationCodeField.value = reservation.reservationCode || "";
        }
        selectedReservation = reservation;
        statusForm.querySelector("[name='statusCode']").value = Object.hasOwn(statusLabels, reservation.statusCode) ? reservation.statusCode : "";
        app.querySelector('[data-consultation-current-partner]').textContent = '현재 담당자: ' + (reservation.partnerUserId ? personLabel(reservation.partnerName, reservation.partnerLoginId) : '미배정');
        partnerPicker.set(reservation.partnerUserId, reservation.partnerName, reservation.partnerLoginId);
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
            const appendText = (parent, tag, value, className) => {
                const node = document.createElement(tag); node.textContent = value;
                if (className) node.className = className;
                parent.append(node); return node;
            };
            const heading = appendText(card, 'div', '');
            appendText(heading, 'span', statusLabels[reservation.statusCode] || reservation.statusCode, 'eyebrow');
            appendText(heading, 'h3', reservation.requestNote || '상담 요청');
            const details = appendText(card, 'dl', '', 'flow-summary-list');
            for (const [label, value] of [
                ['상담 요청 코드', reservation.reservationCode || '-'],
                ['회원', personLabel(reservation.memberName, reservation.memberLoginId)],
                ['담당자', reservation.partnerUserId ? personLabel(reservation.partnerName, reservation.partnerLoginId) : '미배정'],
                ['접수일', formatDateTime(reservation.createdAt)]
            ]) {
                const pair = appendText(details, 'div', '');
                appendText(pair, 'dt', label); appendText(pair, 'dd', value);
            }
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
        if (requestButton.disabled) return;
        const memberUserId = operating ? textOrNull(valueOf(requestForm, "memberUserId")) : null;
        if (operating && (!memberUserId || memberPicker.unresolved())) {
            setMessage(requestMessage, "성함 또는 아이디로 검색한 뒤 접수할 회원을 선택하세요.", "error");
            return;
        }
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
            memberPicker?.set(null);
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
        if (statusButton.disabled) return;
        const reservationId = textOrNull(valueOf(statusForm, "reservationId"));
        if (!reservationId) {
            setMessage(statusMessage, "처리할 상담 요청을 선택하세요.", "error");
            return;
        }
        const nextStatus = valueOf(statusForm, "statusCode");
        if (!nextStatus || nextStatus === 'REQUESTED' || nextStatus === selectedReservation?.statusCode) {
            const terminal = ['COMPLETED', 'NO_SHOW', 'CANCELED'].includes(selectedReservation?.statusCode);
            setMessage(statusMessage, terminal
                ? "상담 완료·미참석·취소된 요청은 상태를 변경할 수 없습니다."
                : "현재 상태와 다른 변경할 상태를 선택하세요. 요청 접수 상태로는 저장할 수 없습니다.", "error");
            return;
        }
        if (partnerPicker.unresolved()) {
            setMessage(statusMessage, "검색 결과에서 담당자를 선택하거나 검색어를 비워 기존 담당자를 유지하세요.", "error");
            return;
        }
        const partnerUserId = textOrNull(valueOf(statusForm, "partnerUserId"));
        const nextPartner = partnerUserId || selectedReservation?.partnerUserId;
        if (['ASSIGNED', 'CONFIRMED'].includes(valueOf(statusForm, 'statusCode')) && !nextPartner) {
            setMessage(statusMessage, "담당자 배정 또는 상담 확정에는 담당자 선택이 필요합니다.", "error");
            return;
        }
        if (partnerUserId && partnerUserId !== selectedReservation?.partnerUserId && !window.confirm(
            `${selectedReservation?.reservationCode || '선택한 상담'}의 담당자를 변경합니다.\n${app.querySelector('[data-person-picker="partner"] [data-person-selected]').textContent}\n저장하시겠습니까?`
        )) return;
        setBusy(statusButton, true, "저장 중");
        setMessage(statusMessage, "");
        try {
            const updated = await requestJson(`/api/v1/consultation-reservations/${encodeURIComponent(reservationId)}/status`, {
                method: "PATCH",
                body: JSON.stringify({
                    statusCode: valueOf(statusForm, "statusCode"),
                    partnerUserId,
                    note: textOrNull(valueOf(statusForm, "note"))
                })
            });
            selectReservation(updated);
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
