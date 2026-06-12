(() => {
    const app = document.querySelector("[data-mock-billing-app]");
    if (!app) {
        return;
    }

    const planList = app.querySelector("[data-mock-plan-list]");
    const successButton = app.querySelector("[data-mock-payment-success]");
    const failButton = app.querySelector("[data-mock-payment-fail]");
    const message = app.querySelector("[data-mock-billing-message]");
    const result = app.querySelector("[data-mock-billing-result]");
    const subscriptionStatus = app.querySelector("[data-mock-subscription-status]");
    const paymentStatus = app.querySelector("[data-mock-payment-status]");
    const paymentAmount = app.querySelector("[data-mock-payment-amount]");
    const period = app.querySelector("[data-mock-period]");

    let selectedPlanId = null;
    let selectedPlan = null;

    const statusLabels = {
        PENDING: "결제 대기",
        ACTIVE: "이용 중",
        PAST_DUE: "결제 확인 필요",
        CANCELED: "취소",
        EXPIRED: "만료",
        REQUESTED: "요청",
        APPROVED: "승인",
        FAILED: "실패",
        REFUNDED: "환불"
    };

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
    };

    const setButtons = (enabled) => {
        if (successButton) {
            successButton.disabled = !enabled;
        }
        if (failButton) {
            failButton.disabled = !enabled;
        }
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

    const formatDateTime = (value) => {
        if (!value) {
            return "확인 전";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return date.toLocaleString("ko-KR", { hour12: false });
    };

    const renderPlans = (plans) => {
        if (!planList) {
            return;
        }
        planList.replaceChildren();
        if (plans.length === 0) {
            const empty = document.createElement("p");
            empty.className = "field-help";
            empty.textContent = "활성화된 월 구독 요금제가 없습니다.";
            planList.append(empty);
            setButtons(false);
            return;
        }
        plans.forEach((plan, index) => {
            const card = document.createElement("button");
            card.type = "button";
            card.className = "mock-plan-card";
            card.dataset.planId = plan.planId;
            card.innerHTML = `
                <span>${plan.planName}</span>
                <strong>${formatCurrency(plan.priceAmount, plan.currencyCode)}</strong>
                <small>${plan.description || "모의 결제용 월 구독 요금제"}</small>
            `;
            card.addEventListener("click", () => {
                selectedPlanId = plan.planId;
                selectedPlan = plan;
                planList.querySelectorAll(".mock-plan-card").forEach((candidate) => {
                    candidate.classList.toggle("is-selected", candidate === card);
                });
                setButtons(true);
                setMessage("요금제를 선택했습니다.", "success");
            });
            planList.append(card);
            if (index === 0) {
                card.click();
            }
        });
    };

    const renderResult = (data) => {
        const subscription = data.subscription || {};
        const payment = data.payment || {};
        if (subscriptionStatus) {
            subscriptionStatus.textContent = statusLabels[subscription.statusCode] || subscription.statusCode || "확인 전";
        }
        if (paymentStatus) {
            paymentStatus.textContent = statusLabels[payment.statusCode] || payment.statusCode || "확인 전";
        }
        if (paymentAmount) {
            paymentAmount.textContent = formatCurrency(payment.amount, payment.currencyCode);
        }
        if (period) {
            period.textContent = `${formatDateTime(subscription.currentPeriodStart)} ~ ${formatDateTime(subscription.currentPeriodEnd)}`;
        }
        if (result) {
            result.hidden = false;
        }
    };

    const loadPlans = async () => {
        setMessage("요금제를 불러오는 중입니다.");
        try {
            const data = await requestJson("/api/v1/subscription-plans?active=true&page=1&size=20");
            const monthlyPlans = (data.items || []).filter((plan) => plan.billingCycleCode === "MONTHLY");
            renderPlans(monthlyPlans);
            setMessage(monthlyPlans.length > 0 ? "요금제를 선택한 뒤 모의 결제를 진행하세요." : "활성화된 월 구독 요금제가 없습니다.");
        } catch (error) {
            setButtons(false);
            setMessage(error.message || "요금제를 불러오지 못했습니다.", "error");
        }
    };

    const submitMockPayment = async (simulateFailure) => {
        if (!selectedPlanId || !selectedPlan) {
            setMessage("먼저 요금제를 선택하세요.", "error");
            return;
        }
        setButtons(false);
        setMessage(simulateFailure ? "실패 상황을 처리 중입니다." : "모의 결제를 처리 중입니다.");
        try {
            const data = await requestJson("/api/v1/mock-payments/monthly-subscription", {
                method: "POST",
                body: JSON.stringify({
                    planId: selectedPlanId,
                    simulateFailure
                })
            });
            renderResult(data);
            setMessage(data.resultMessage || "모의 결제 처리가 완료되었습니다.", simulateFailure ? "error" : "success");
        } catch (error) {
            setMessage(error.message || "모의 결제 처리에 실패했습니다.", "error");
        } finally {
            setButtons(true);
        }
    };

    successButton?.addEventListener("click", () => submitMockPayment(false));
    failButton?.addEventListener("click", () => submitMockPayment(true));
    loadPlans();
})();
