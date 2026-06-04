(() => {
    const form = document.querySelector("[data-password-form]");
    if (!form) {
        return;
    }

    const message = document.querySelector("[data-password-message]");
    const submitButton = document.querySelector("[data-password-submit]");

    const setMessage = (text, status) => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.hidden = !text;
        message.classList.toggle("is-success", status === "success");
    };

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

    const selectDefaultRoute = async () => {
        const response = await fetch("/api/v1/auth/me", {
            method: "GET",
            credentials: "same-origin"
        });
        const payload = await response.json().catch(() => null);
        if (response.ok && payload && payload.success === true && payload.data && payload.data.defaultRoute) {
            return payload.data.defaultRoute;
        }
        return "/app";
    };

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        setMessage("", "error");

        const formData = new FormData(form);
        const currentPassword = String(formData.get("currentPassword") || "");
        const newPassword = String(formData.get("newPassword") || "");
        const newPasswordConfirm = String(formData.get("newPasswordConfirm") || "");

        if (!currentPassword) {
            setMessage("현재 비밀번호를 입력해 주세요.", "error");
            return;
        }

        if (newPassword.length < 8 || newPassword.length > 16) {
            setMessage("새 비밀번호는 8~16자로 입력해 주세요.", "error");
            return;
        }

        if (newPassword !== newPasswordConfirm) {
            setMessage("새 비밀번호 확인이 일치하지 않습니다.", "error");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "변경 처리 중";
        }

        try {
            const response = await fetch(form.action, {
                method: "PATCH",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });
            const payload = await response.json().catch(() => null);

            if (response.ok && payload && payload.success === true) {
                setMessage("비밀번호가 변경되었습니다. 대시보드로 이동합니다.", "success");
                const defaultRoute = await selectDefaultRoute().catch(() => "/app");
                window.setTimeout(() => {
                    window.location.assign(defaultRoute);
                }, 500);
                return;
            }

            setMessage(selectErrorMessage(payload, "비밀번호 변경에 실패했습니다."), "error");
        } catch (error) {
            setMessage("비밀번호 변경 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "비밀번호 변경";
            }
        }
    });
})();
