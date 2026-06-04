(() => {
    const app = document.querySelector("[data-admin-users-app]");
    if (!app) {
        return;
    }

    const baseUrl = app.dataset.baseUrl;
    const message = app.querySelector("[data-admin-users-message]");

    const selectErrorMessage = (payload, fallback) => {
        const fieldErrors = payload && payload.data && Array.isArray(payload.data.fieldErrors)
                ? payload.data.fieldErrors
                : [];
        if (fieldErrors.length > 0 && fieldErrors[0].message) {
            return fieldErrors[0].message;
        }
        if (payload && typeof payload.message === "string" && payload.message.trim() !== "") {
            return payload.message;
        }
        return fallback;
    };

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
    };

    const setBusy = (button, busy) => {
        if (!button) {
            return;
        }
        if (!button.dataset.defaultText) {
            button.dataset.defaultText = button.textContent;
        }
        button.disabled = busy;
        button.textContent = busy ? "저장 중" : button.dataset.defaultText;
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
            throw new Error(selectErrorMessage(payload, "요청 처리에 실패했습니다."));
        }
        return payload.data;
    };

    const selectedRoleCodes = (form) => Array.from(form.querySelectorAll("input[name='roleCodes']:checked"))
            .map((field) => field.value)
            .filter(Boolean);

    const applyUserState = (card, user) => {
        if (!card || !user) {
            return;
        }
        const statusField = card.querySelector("[name='statusCode']");
        if (statusField) {
            statusField.value = user.statusCode || "ACTIVE";
        }
        const roles = new Set(user.roles || []);
        card.querySelectorAll("input[name='roleCodes']").forEach((field) => {
            field.checked = roles.has(field.value);
        });
    };

    app.addEventListener("submit", async (event) => {
        const form = event.target.closest("[data-user-update-form]");
        if (!form) {
            return;
        }
        event.preventDefault();

        const card = form.closest("[data-user-id]");
        const userId = card ? card.dataset.userId : "";
        const button = form.querySelector("button[type='submit']");
        if (!userId) {
            setMessage("회원 식별자를 확인할 수 없습니다.", "error");
            return;
        }

        const roleCodes = selectedRoleCodes(form);
        if (roleCodes.length === 0) {
            setMessage("권한을 하나 이상 선택하세요.", "error");
            return;
        }

        try {
            setBusy(button, true);
            const statusCode = form.querySelector("[name='statusCode']")?.value || "ACTIVE";
            await requestJson(`${baseUrl}/${encodeURIComponent(userId)}/status`, {
                method: "PATCH",
                body: JSON.stringify({ statusCode })
            });
            const updatedUser = await requestJson(`${baseUrl}/${encodeURIComponent(userId)}/roles`, {
                method: "PUT",
                body: JSON.stringify({ roleCodes })
            });
            applyUserState(card, updatedUser);
            setMessage("회원 상태와 권한이 저장되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });
})();
