(() => {
    const form = document.querySelector("[data-signup-form]");
    if (!form) {
        return;
    }

    const message = document.querySelector("[data-signup-message]");
    const submitButton = document.querySelector("[data-signup-submit]");

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

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        setMessage("", "error");

        const formData = new FormData(form);
        const password = String(formData.get("password") || "");
        const passwordConfirm = String(formData.get("passwordConfirm") || "");

        if (password !== passwordConfirm) {
            setMessage("비밀번호 확인이 일치하지 않습니다.", "error");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "가입 처리 중";
        }

        try {
            const response = await fetch(form.action, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    loginId: String(formData.get("loginId") || "").trim(),
                    password,
                    passwordConfirm,
                    name: String(formData.get("name") || "").trim(),
                    phone: String(formData.get("phone") || "").trim() || null,
                    email: String(formData.get("email") || "").trim() || null,
                    termsAgreed: formData.get("termsAgreed") === "on",
                    privacyAgreed: formData.get("privacyAgreed") === "on"
                })
            });
            const payload = await response.json().catch(() => null);

            if (response.ok && payload && payload.success === true && payload.data && payload.data.defaultRoute) {
                setMessage("회원가입이 완료되었습니다. 이동합니다.", "success");
                window.location.assign(payload.data.defaultRoute);
                return;
            }

            setMessage(selectErrorMessage(payload, "회원가입 처리에 실패했습니다."), "error");
        } catch (error) {
            setMessage("회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "회원가입";
            }
        }
    });
})();
