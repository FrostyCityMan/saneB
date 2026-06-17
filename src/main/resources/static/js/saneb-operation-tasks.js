(() => {
    const app = document.querySelector("[data-operation-task-page]");
    if (!app) {
        return;
    }

    const baseUrl = app.dataset.operationTaskUrl || "/api/v1/operation-tasks";
    const form = app.querySelector("[data-operation-task-filter-form]");
    const list = app.querySelector("[data-operation-task-list]");
    const refreshButton = app.querySelector("[data-operation-task-refresh]");

    const statusLabels = {
        OPEN: "대기",
        IN_PROGRESS: "처리중",
        WAITING: "보류",
        DONE: "완료",
        CANCELED: "취소"
    };

    const priorityLabels = {
        URGENT: "긴급",
        HIGH: "높음",
        NORMAL: "보통",
        LOW: "낮음"
    };

    const typeLabels = {
        DELAYED_PROGRESS: "장기 미진행",
        SUPPLEMENT_REQUEST: "보완 요청",
        RECONTACT: "TM 재접촉",
        PAYMENT_FAILED: "결제 확인",
        CONSULTATION_PENDING: "상담 대기",
        GENERAL: "일반 업무"
    };

    const resourceLabels = {
        APPLICATION_PROGRESS: "신청 진행",
        MATCHING_CASE: "매칭 공고",
        CONSULTATION_RESERVATION: "상담",
        PAYMENT_TRANSACTION: "결제",
        DOCUMENT_SUBMISSION: "서류",
        GENERAL: "일반"
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
            return "기한 없음";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return date.toLocaleString("ko-KR", { hour12: false });
    };

    const createMessage = (message) => {
        const empty = document.createElement("p");
        empty.className = "field-help";
        empty.textContent = message;
        return empty;
    };

    const selectValue = (name) => String(form?.querySelector(`[name='${name}']`)?.value || "").trim();

    const buildUrl = () => {
        const params = new URLSearchParams({ page: "1", size: "50" });
        ["taskTypeCode", "statusCode", "priorityCode"].forEach((name) => {
            const value = selectValue(name);
            if (value) {
                params.set(name, value);
            }
        });
        return `${baseUrl}?${params.toString()}`;
    };

    const changeStatus = async (taskId, statusCode) => {
        await requestJson(`${baseUrl}/${encodeURIComponent(taskId)}/status`, {
            method: "PATCH",
            body: JSON.stringify({ statusCode })
        });
        await loadTasks();
    };

    const addComment = async (taskId, textarea) => {
        const commentText = String(textarea.value || "").trim();
        if (!commentText) {
            textarea.focus();
            return;
        }
        await requestJson(`${baseUrl}/${encodeURIComponent(taskId)}/comments`, {
            method: "POST",
            body: JSON.stringify({ commentText })
        });
        textarea.value = "";
    };

    const renderTasks = (items) => {
        list.replaceChildren();
        if (!items || items.length === 0) {
            list.append(createMessage("조회된 운영 업무가 없습니다."));
            return;
        }
        items.forEach((item) => {
            const card = document.createElement("article");
            card.className = `operation-task-card priority-${String(item.priorityCode || "NORMAL").toLowerCase()}`;

            const header = document.createElement("div");
            header.className = "operation-task-header";
            const titleWrap = document.createElement("div");
            const eyebrow = document.createElement("span");
            eyebrow.className = "eyebrow";
            eyebrow.textContent = typeLabels[item.taskTypeCode] || item.taskTypeCode || "운영 업무";
            const title = document.createElement("h3");
            title.textContent = item.title || "운영 업무";
            titleWrap.append(eyebrow, title);

            const status = document.createElement("span");
            status.className = "soft-status";
            status.textContent = statusLabels[item.statusCode] || item.statusCode || "상태 없음";
            header.append(titleWrap, status);

            const description = document.createElement("p");
            description.className = "operation-task-description";
            description.textContent = item.description || "상세 설명이 없습니다.";

            const meta = document.createElement("dl");
            meta.className = "flow-summary-list operation-task-meta";
            [
                ["중요도", priorityLabels[item.priorityCode] || item.priorityCode || "보통"],
                ["업무 대상", resourceLabels[item.resourceType] || item.resourceType || "일반"],
                ["처리 기한", formatDateTime(item.dueAt)],
                ["생성일", formatDateTime(item.createdAt)]
            ].forEach(([label, value]) => {
                const row = document.createElement("div");
                const dt = document.createElement("dt");
                dt.textContent = label;
                const dd = document.createElement("dd");
                dd.textContent = value;
                row.append(dt, dd);
                meta.append(row);
            });

            const actions = document.createElement("div");
            actions.className = "operation-task-actions";
            [
                ["IN_PROGRESS", "처리 시작"],
                ["WAITING", "보류"],
                ["DONE", "완료"],
                ["CANCELED", "취소"]
            ].forEach(([statusCode, label]) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = statusCode === "DONE" ? "primary-action button-action" : "secondary-action";
                button.textContent = label;
                button.disabled = item.statusCode === statusCode || item.statusCode === "DONE" || item.statusCode === "CANCELED";
                button.addEventListener("click", async () => {
                    button.disabled = true;
                    button.textContent = "저장 중";
                    try {
                        await changeStatus(item.taskId, statusCode);
                    } catch (error) {
                        button.disabled = false;
                        button.textContent = label;
                    }
                });
                actions.append(button);
            });

            const comment = document.createElement("div");
            comment.className = "operation-task-comment";
            const textarea = document.createElement("textarea");
            textarea.rows = 2;
            textarea.maxLength = 1000;
            textarea.placeholder = "처리 메모를 입력하세요.";
            const commentButton = document.createElement("button");
            commentButton.type = "button";
            commentButton.className = "secondary-action";
            commentButton.textContent = "메모 저장";
            commentButton.addEventListener("click", async () => {
                commentButton.disabled = true;
                commentButton.textContent = "저장 중";
                try {
                    await addComment(item.taskId, textarea);
                    commentButton.textContent = "저장 완료";
                    window.setTimeout(() => {
                        commentButton.textContent = "메모 저장";
                        commentButton.disabled = false;
                    }, 900);
                } catch (error) {
                    commentButton.textContent = "메모 저장";
                    commentButton.disabled = false;
                }
            });
            comment.append(textarea, commentButton);

            card.append(header, description, meta, actions, comment);
            list.append(card);
        });
    };

    async function loadTasks() {
        list.replaceChildren(createMessage("운영 업무를 불러오는 중입니다."));
        try {
            const data = await requestJson(buildUrl());
            renderTasks(data.items || []);
        } catch (error) {
            list.replaceChildren(createMessage(error.message || "운영 업무를 불러오지 못했습니다."));
        }
    }

    form?.addEventListener("submit", (event) => {
        event.preventDefault();
        loadTasks();
    });
    refreshButton?.addEventListener("click", loadTasks);
    loadTasks();
})();
