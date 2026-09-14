/* 읽기 전용 대기열 계약. 필터는 서버 전체 범위에 적용하며 원문을 브라우저 저장소에 기록하지 않는다. */
((root) => {
    "use strict";
    const flowCodes = Object.freeze(["NOT_APPLIED", "CLASSIFICATION_PENDING", "CONFIGURATION_REQUIRED",
        "AUTOMATIC_PROCESSING", "EVIDENCE_STALE", "TECHNICAL_EXCEPTION", "READY_FOR_FINAL_REVIEW",
        "FINAL_REVIEW_EXCEPTION", "FINAL_REVIEW_CONFIRMED"]);
    const providers = ["BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE"];
    const pageSize = 20;
    const stateFromSearch = search => {
        const query = new URLSearchParams(search);
        return {processingFlowStatusCode: query.get("processingFlowStatusCode") ?? "READY_FOR_FINAL_REVIEW",
            providerCode: query.get("providerCode") || "", keyword: query.get("keyword") || "", page: query.get("page") || "1"};
    };
    const validatedState = input => {
        const flow = String(input.processingFlowStatusCode ?? "").trim();
        const provider = String(input.providerCode ?? "").trim();
        const keyword = String(input.keyword ?? "").trim();
        if (flow && !flowCodes.includes(flow)) throw new Error("처리 단계가 지원하지 않는 값입니다. 목록에서 처리 단계를 선택한 뒤 조건으로 조회하세요.");
        if (provider && !providers.includes(provider)) throw new Error("수집처는 기업마당·정부24·지자체 중 선택하거나 전체로 바꾸세요.");
        if (keyword.length > 100 || /[\u0000-\u001f\u007f-\u009f]/.test(keyword)) throw new Error("검색어는 제어문자 없이 100자 이하여야 합니다.");
        if (!/^[0-9]+$/.test(String(input.page)) || Number(input.page) < 1 || Number(input.page) > 1000000)
            throw new Error("페이지는 1~1000000 범위의 정수여야 합니다. 조건으로 조회하면 첫 페이지부터 확인할 수 있습니다.");
        return {processingFlowStatusCode: flow, providerCode: provider, keyword, page: Number(input.page)};
    };
    const searchFromState = state => {
        const value = validatedState(state);
        // 빈 처리 단계도 URL에 유지해야 새로고침 시 기본 '검증 대기'로 바뀌지 않는다.
        const query = new URLSearchParams({processingFlowStatusCode: value.processingFlowStatusCode, page: String(value.page)});
        if (value.providerCode) query.set("providerCode", value.providerCode);
        if (value.keyword) query.set("keyword", value.keyword);
        return query.toString();
    };
    const validatePage = (data, state) => {
        const request = validatedState(state);
        if (!data || !Array.isArray(data.items) || data.page !== request.page || data.size !== pageSize
            || !Number.isSafeInteger(data.totalCount) || data.totalCount < 0
            || data.totalPages !== Math.ceil(data.totalCount / pageSize)
            || data.items.length !== Math.min(pageSize, Math.max(0, data.totalCount - (data.page - 1) * pageSize)))
            throw new Error("목록과 전체 건수의 응답이 일치하지 않습니다. 완료로 판단하지 말고 같은 조건으로 다시 조회하세요.");
        const ids = new Set();
        for (const source of data.items) {
            const flow = source?.processingFlow;
            if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(source?.sourceId || "")
                || ids.has(source.sourceId) || !flowCodes.includes(flow?.statusCode)
                || typeof flow?.isAutomaticAnalysisComplete !== "boolean" || typeof flow?.isFinalReviewAvailable !== "boolean"
                || (request.processingFlowStatusCode && request.processingFlowStatusCode !== flow.statusCode)
                || (request.providerCode && request.providerCode !== source.providerCode))
                throw new Error("공고의 처리 상태가 현재 검색 조건과 일치하지 않습니다. 최신 목록을 다시 조회하세요.");
            ids.add(source.sourceId);
        }
        return data;
    };
    const client = (fetcher, timeoutMs = 15000) => async state => {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        try {
            const response = await fetcher(`/api/v2/admin/announcement-sources?${searchFromState(state)}&size=${pageSize}`,
                {method: "GET", credentials: "same-origin", cache: "no-store", redirect: "error", signal: controller.signal,
                    headers: {Accept: "application/json"}});
            if (response.status === 401) throw new Error("로그인이 만료됐습니다. 다른 탭에서 로그인한 뒤 같은 조건으로 다시 조회하세요.");
            if (response.status === 403) throw new Error("조회 권한이 없습니다. 관리자·운영자·승인자 계정의 권한을 확인하세요.");
            let body;
            try { body = await response.json(); } catch { throw new Error("서버 응답을 읽을 수 없습니다. 같은 조건으로 다시 조회하세요."); }
            if (!response.ok || body?.success !== true) throw new Error(response.status >= 500
                ? "서버에서 목록을 조회하지 못했습니다. 검색 조건을 유지하고 다시 조회하세요."
                : (body?.message || "검색 조건을 처리하지 못했습니다. 조건을 확인하고 다시 조회하세요."));
            return validatePage(body.data, state);
        } catch (error) {
            if (error?.name === "AbortError" || error instanceof TypeError)
                throw new Error("연결이 끊겼거나 조회 시간이 초과됐습니다. 네트워크를 확인하고 같은 조건으로 다시 조회하세요.");
            throw error;
        } finally { clearTimeout(timer); }
    };
    // 이전 요청이 늦게 도착해 새 필터의 결과/오류를 덮어쓰지 않도록 보장한다.
    const latestRequest = () => {
        let revision = 0;
        return {begin: () => ++revision, isCurrent: value => value === revision};
    };
    const api = {flowCodes, pageSize, stateFromSearch, validatedState, searchFromState, validatePage, client, latestRequest};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.SanebAttachmentQueue = api;
})(globalThis);
