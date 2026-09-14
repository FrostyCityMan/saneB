(() => {
    "use strict";
    const page = document.querySelector("[data-attachment-queue-page]");
    if (!page) return;
    const Q = window.SanebAttachmentQueue, C = window.SanebAttachmentReview;
    const one = selector => page.querySelector(selector);
    const form = one("[data-queue-filter]"), items = one("[data-queue-items]"), error = one("[data-queue-error]");
    const status = one("[data-queue-status]"), count = one("[data-queue-count]"), pageText = one("[data-queue-page]");
    const results = one("#attachment-queue-results"), first = one("[data-queue-first]"), prev = one("[data-queue-prev]"), next = one("[data-queue-next]");
    if (!Q || !C) {
        error.hidden = false;
        error.textContent = "대기열 스크립트를 불러오지 못했습니다. 페이지를 새로고침하세요.";
        results.setAttribute("aria-busy", "false");
        return;
    }
    const request = Q.client(window.fetch.bind(window)), revisions = Q.latestRequest();
    let current = Q.stateFromSearch(window.location.search), lastPage = null;
    const text = (tag, value, parent) => {
        const element = document.createElement(tag);
        element.textContent = value;
        parent.appendChild(element);
        return element;
    };
    const syncForm = state => {
        for (const key of ["processingFlowStatusCode", "providerCode", "keyword"]) {
            const field = form.elements.namedItem(key);
            field.querySelectorAll?.("option[data-unsupported]").forEach(option => option.remove());
            if (field.tagName === "SELECT" && !Array.from(field.options).some(option => option.value === state[key])) {
                const option = text("option", `지원하지 않는 값 (${state[key]})`, field);
                option.value = state[key]; option.dataset.unsupported = "true";
            }
            field.value = state[key];
        }
    };
    const showSummary = state => {
        one("[data-queue-filter-summary]").textContent = `적용 조건: ${state.processingFlowStatusCode ? C.label(state.processingFlowStatusCode) : "전체 처리 단계"} / ${state.providerCode ? C.label(state.providerCode) : "전체 수집처"} / ${state.keyword ? `검색어: ${state.keyword}` : "검색어 없음"}`;
        one("[data-queue-guidance]").textContent = state.processingFlowStatusCode
            ? C.flowGuidance({processingFlow: {statusCode: state.processingFlowStatusCode}})
            : "전체 단계에는 미적용·자동 처리 중·기술 예외가 포함됩니다. 전체 검색 건수는 검수 준비 완료 건수가 아닙니다.";
    };
    const setPaging = () => {
        first.disabled = !lastPage || current.page <= 1;
        prev.disabled = !lastPage || current.page <= 1;
        next.disabled = !lastPage || current.page >= lastPage.totalPages || current.page >= 1000000;
    };
    const render = data => {
        items.replaceChildren();
        count.textContent = `전체 검색 ${data.totalCount.toLocaleString("ko-KR")}건 · 현재 페이지 ${data.items.length}건 · 수집일 최신순`;
        pageText.textContent = `${data.page.toLocaleString("ko-KR")}페이지 / 전체 ${data.totalPages.toLocaleString("ko-KR")}페이지`;
        if (!data.items.length) text("p", data.totalCount
            ? "현재 페이지에 공고가 없습니다. 첫 페이지로 이동하거나 검색 조건을 변경하세요."
            : "선택한 조건의 공고가 없습니다. 전체 상태로 초기화하거나 수집처·검색어를 변경하세요.", items);
        for (const source of data.items) {
            const card = document.createElement("article"); card.className = "attachment-evidence-item";
            const heading = text("h3", "", card);
            const link = text("a", source.title || "제목 미확인", heading);
            link.href = `/app/admin/collected-announcements/${encodeURIComponent(source.sourceId)}/attachments`;
            text("p", `${C.label(source.providerCode)} · ${source.agencyName || "기관 미확인"} · 접수 마감 ${source.applicationEndDate || "미확인"}`, card);
            text("p", C.label(source.processingFlow.statusCode), card);
            text("p", `자동 분석: ${source.processingFlow.isAutomaticAnalysisComplete ? "완료" : "미완료 또는 미적용"} · 최종 검증 절차: ${source.processingFlow.isFinalReviewAvailable ? "현재 근거 확인 가능" : "현재 근거 준비 안 됨"}`, card);
            text("p", C.flowGuidance(source), card);
            const action = text("a", "근거와 처리 상태 확인", card);
            action.className = "secondary-action"; action.href = link.href;
            action.setAttribute("aria-label", `${source.title || "공고"} 근거와 처리 상태 확인`);
            items.appendChild(card);
        }
    };
    const load = async (input, historyMode = "replace") => {
        const revision = revisions.begin();
        error.hidden = true; error.textContent = ""; lastPage = null; setPaging();
        items.replaceChildren(); count.textContent = "전체 검색 건수 미확인"; pageText.textContent = "페이지 미확인";
        results.setAttribute("aria-busy", "true"); status.textContent = "서버 전체 범위에서 현재 처리 상태를 조회하고 있습니다.";
        try {
            const selected = Q.validatedState(input);
            current = selected; syncForm(selected); showSummary(selected);
            if (historyMode !== "none") window.history[historyMode === "push" ? "pushState" : "replaceState"](
                null, "", `${window.location.pathname}?${Q.searchFromState(selected)}`);
            const data = await request(selected);
            if (!revisions.isCurrent(revision)) return;
            lastPage = data; render(data); setPaging();
            status.textContent = `현재 조건의 조회가 완료됐습니다. 전체 검색 ${data.totalCount.toLocaleString("ko-KR")}건입니다. 공고 최종 선정·공개 완료를 뜻하지 않습니다.`;
        } catch (failure) {
            if (!revisions.isCurrent(revision)) return;
            error.textContent = failure.message; error.hidden = false;
            status.textContent = "조회하지 못했습니다. 결과는 0건이 아니라 미확인입니다. 검색 조건을 유지하고 다시 조회하세요.";
        } finally { if (revisions.isCurrent(revision)) results.setAttribute("aria-busy", "false"); }
    };
    form.addEventListener("submit", event => {
        event.preventDefault();
        load({processingFlowStatusCode: form.elements.namedItem("processingFlowStatusCode").value,
            providerCode: form.elements.namedItem("providerCode").value, keyword: form.elements.namedItem("keyword").value, page: 1}, "push");
    });
    one("[data-queue-clear]").addEventListener("click", () => load({processingFlowStatusCode: "", providerCode: "", keyword: "", page: 1}, "push"));
    one("[data-queue-refresh]").addEventListener("click", () => load(current));
    first.addEventListener("click", () => load({...current, page: 1}, "push"));
    prev.addEventListener("click", () => load({...current, page: current.page - 1}, "push"));
    next.addEventListener("click", () => load({...current, page: current.page + 1}, "push"));
    window.addEventListener("popstate", () => { current = Q.stateFromSearch(window.location.search); syncForm(current); showSummary(current); load(current, "none"); });
    syncForm(current); showSummary(current); load(current);
})();
