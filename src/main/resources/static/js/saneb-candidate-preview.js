(() => {
    const app = document.querySelector("[data-candidate-preview-app]");
    if (!app) {
        return;
    }

    const form = app.querySelector("[data-candidate-preview-form]");
    const submitButton = app.querySelector("[data-candidate-preview-submit]");
    const message = app.querySelector("[data-candidate-preview-message]");
    const result = app.querySelector("[data-candidate-preview-result]");
    const count = app.querySelector("[data-candidate-count]");
    const amount = app.querySelector("[data-candidate-amount]");
    const notice = app.querySelector("[data-candidate-notice]");

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-error", status === "error");
        message.classList.toggle("is-success", status === "success");
    };

    const setBusy = (busy) => {
        if (!submitButton) {
            return;
        }
        if (!submitButton.dataset.defaultText) {
            submitButton.dataset.defaultText = submitButton.textContent;
        }
        submitButton.disabled = busy;
        submitButton.textContent = busy ? "확인 중" : submitButton.dataset.defaultText;
    };

    const valueOf = (name) => String(form.querySelector(`[name='${name}']`)?.value || "").trim();

    const numberOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : Number(text);
    };

    const booleanOrNull = (value) => {
        if (value === "true") {
            return true;
        }
        if (value === "false") {
            return false;
        }
        return null;
    };

    const textOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

    const formatCurrency = (value) => {
        if (value == null) {
            return null;
        }
        return `${Number(value).toLocaleString("ko-KR")}원`;
    };

    const renderResult = (data) => {
        if (count) {
            count.textContent = Number(data.possibleCandidateCount || 0).toLocaleString("ko-KR");
        }
        const minAmount = formatCurrency(data.minSupportAmount);
        const maxAmount = formatCurrency(data.maxSupportAmount);
        if (amount) {
            amount.textContent = minAmount && maxAmount ? `${minAmount} ~ ${maxAmount}` : "가입 후 상세 확인";
        }
        if (notice) {
            notice.textContent = data.criteriaNotice || "회원가입 전 임시 확인 결과입니다.";
        }
        if (result) {
            result.hidden = false;
        }
    };

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const annualRevenue = numberOrNull(valueOf("annualRevenue"));
        if (annualRevenue != null && Number.isNaN(annualRevenue)) {
            setMessage("연매출은 숫자로 입력하세요.", "error");
            return;
        }
        setBusy(true);
        setMessage("");
        try {
            const response = await fetch("/api/v1/pre-signup/candidate-preview", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json",
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    regionCode: textOrNull(valueOf("regionCode")),
                    annualRevenue,
                    openingDate: textOrNull(valueOf("openingDate")),
                    hasSpouse: booleanOrNull(valueOf("hasSpouse")),
                    hasChild: booleanOrNull(valueOf("hasChild"))
                })
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || !payload || payload.success !== true) {
                throw new Error(payload?.message || "임시 후보 확인에 실패했습니다.");
            }
            renderResult(payload.data || {});
            setMessage("임시 후보 확인이 완료되었습니다.", "success");
        } catch (error) {
            setMessage(error.message || "임시 후보 확인에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    });
})();
