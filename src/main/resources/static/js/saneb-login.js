(() => {
    const form = document.querySelector("[data-login-form]");
    if (!form) {
        return;
    }

    const message = document.querySelector("[data-login-message]");
    const submitButton = document.querySelector("[data-login-submit]");
    const passwordInput = document.querySelector("#password");
    const passwordToggle = document.querySelector("[data-password-toggle]");

    const setMessage = (text, status) => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.hidden = !text;
        message.classList.toggle("is-success", status === "success");
    };

    const selectErrorMessage = (payload, fallback) => {
        if (payload && typeof payload.message === "string" && payload.message.trim() !== "") {
            return payload.message;
        }
        const fieldErrors = payload && payload.data && Array.isArray(payload.data.fieldErrors)
                ? payload.data.fieldErrors
                : [];
        if (fieldErrors.length > 0 && fieldErrors[0].message) {
            return fieldErrors[0].message;
        }
        return fallback;
    };

    if (passwordToggle && passwordInput) {
        passwordToggle.addEventListener("click", () => {
            const shouldShow = passwordInput.type === "password";
            passwordInput.type = shouldShow ? "text" : "password";
            passwordToggle.setAttribute("aria-pressed", String(shouldShow));
            passwordToggle.setAttribute("aria-label", shouldShow ? "비밀번호 숨기기" : "비밀번호 보기");
        });
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        setMessage("", "error");

        const formData = new FormData(form);
        const loginId = String(formData.get("loginId") || "").trim();
        const password = String(formData.get("password") || "");

        if (!loginId || !password) {
            setMessage("아이디와 비밀번호를 모두 입력해 주세요.", "error");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "확인 중";
        }

        try {
            const response = await fetch(form.action, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ loginId, password })
            });
            const payload = await response.json().catch(() => null);

            if (response.ok && payload && payload.success === true && payload.data && payload.data.defaultRoute) {
                setMessage("로그인되었습니다. 이동합니다.", "success");
                window.location.assign(payload.data.defaultRoute);
                return;
            }

            setMessage(selectErrorMessage(payload, "로그인 정보가 올바르지 않습니다."), "error");
        } catch (error) {
            setMessage("로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "로그인하기";
            }
        }
    });
})();
