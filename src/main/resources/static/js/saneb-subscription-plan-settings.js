(() => {
    const app = document.querySelector("[data-subscription-plan-settings-app]");
    if (!app) {
        return;
    }

    const form = app.querySelector("[data-subscription-plan-form]");
    const priceInput = app.querySelector("[data-subscription-plan-price]");
    const submitButton = app.querySelector("[data-subscription-plan-submit]");
    const message = app.querySelector("[data-subscription-plan-message]");
    const planList = app.querySelector("[data-subscription-plan-list]");

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
    };

    const setBusy = (busy) => {
        if (submitButton) {
            submitButton.disabled = busy;
        }
        app.querySelectorAll("[data-subscription-plan-status]").forEach((button) => {
            button.disabled = busy;
        });
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

    const formatCurrency = (value, currencyCode = "KRW") => {
        const number = Number(value || 0);
        return `${number.toLocaleString("ko-KR")} ${currencyCode}`;
    };

    const createElement = (tagName, className, text) => {
        const element = document.createElement(tagName);
        if (className) {
            element.className = className;
        }
        if (text !== undefined) {
            element.textContent = text;
        }
        return element;
    };

    const renderPlans = (plans) => {
        if (!planList) {
            return;
        }
        planList.replaceChildren();
        const monthlyPlans = plans.filter((plan) => plan.billingCycleCode === "MONTHLY");
        if (monthlyPlans.length === 0) {
            const empty = createElement("p", "field-help", "등록된 월 구독 금액이 없습니다.");
            planList.append(empty);
            return;
        }

        monthlyPlans.forEach((plan) => {
            const card = createElement("article", `mock-plan-card subscription-plan-card ${plan.active ? "is-active" : "is-inactive"}`);
            const header = createElement("div", "subscription-plan-card-head");
            const title = createElement("span", null, plan.planName || "월 구독");
            const status = createElement("small", plan.active ? "status-badge is-active" : "status-badge is-inactive",
                    plan.active ? "사용 중" : "중지");
            header.append(title, status);

            const amount = createElement("strong", null, formatCurrency(plan.priceAmount, plan.currencyCode));
            const code = createElement("small", null, `관리 코드: ${plan.planCode}`);
            const description = createElement("small", null, plan.description || "관리자가 설정한 월 구독 금액");
            const action = createElement("button", "secondary-action", plan.active ? "비활성화" : "활성화");
            action.type = "button";
            action.dataset.subscriptionPlanStatus = plan.planId;
            action.addEventListener("click", () => updatePlanStatus(plan.planId, !plan.active));

            card.append(header, amount, code, description, action);
            planList.append(card);
        });
    };

    const loadPlans = async () => {
        setMessage("등록된 금액을 불러오는 중입니다.");
        setBusy(true);
        try {
            const data = await requestJson("/api/v1/subscription-plans?page=1&size=100");
            renderPlans(data.items || []);
            setMessage("월 구독 금액을 확인했습니다.", "success");
        } catch (error) {
            setMessage(error.message || "구독 금액을 불러오지 못했습니다.", "error");
        } finally {
            setBusy(false);
        }
    };

    const buildPlanCode = () => {
        const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14);
        const suffix = Math.floor(Math.random() * 900 + 100);
        return `MONTHLY_${timestamp}_${suffix}`;
    };

    const insertPlan = async () => {
        const amount = Number(priceInput?.value || 0);
        if (!Number.isFinite(amount) || amount <= 0) {
            setMessage("월 구독 금액을 1원 이상으로 입력하세요.", "error");
            priceInput?.focus();
            return;
        }

        setBusy(true);
        setMessage("월 구독 금액을 저장하는 중입니다.");
        try {
            await requestJson("/api/v1/subscription-plans", {
                method: "POST",
                body: JSON.stringify({
                    planCode: buildPlanCode(),
                    planName: `월 구독 ${amount.toLocaleString("ko-KR")}원`,
                    billingCycleCode: "MONTHLY",
                    priceAmount: amount,
                    currencyCode: "KRW",
                    active: true,
                    sortOrder: 10,
                    description: "관리자 화면에서 설정한 월 구독 금액"
                })
            });
            if (priceInput) {
                priceInput.value = "";
            }
            setMessage("월 구독 금액을 저장했습니다.", "success");
            await loadPlans();
        } catch (error) {
            setMessage(error.message || "월 구독 금액 저장에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    };

    const updatePlanStatus = async (planId, active) => {
        setBusy(true);
        setMessage(active ? "금액을 활성화하는 중입니다." : "금액을 비활성화하는 중입니다.");
        try {
            await requestJson(`/api/v1/subscription-plans/${planId}/status`, {
                method: "PATCH",
                body: JSON.stringify({ active })
            });
            setMessage(active ? "월 구독 금액을 활성화했습니다." : "월 구독 금액을 비활성화했습니다.", "success");
            await loadPlans();
        } catch (error) {
            setMessage(error.message || "상태 변경에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    };

    form?.addEventListener("submit", (event) => {
        event.preventDefault();
        insertPlan();
    });

    loadPlans();
})();
