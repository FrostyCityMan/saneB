/* 고정 기대값의 검증 범위만 읽는다. 실제 QA 성공·수집·변경 요청은 이 화면의 계약이 아니다. */
((root) => {
    "use strict";
    const route = "/app/admin/announcement-attachment-provider-coverage", size = 20;
    const formats = ["HWP", "HWPX", "PDF"];
    const bindings = {
        SYSTEM_BINDING_MATCHED: "시스템 연결 일치 · 실제 수집 성공 아님",
        PROFILE_MISSING: "시스템 첨부 모델 없음 · 구현 필요",
        LIST_PARSER_MISMATCH: "목록 파서 불일치 · 시스템 연결 확인 필요",
        PROFILE_AMBIGUOUS: "첨부 모델 중복 연결 · 시스템 등록 정리 필요"
    };
    const sameSet = (a, b) => a.length === b.length && a.every(v => b.includes(v));
    const validFormats = a => Array.isArray(a) && a.length <= 3 && new Set(a).size === a.length && a.every(v => formats.includes(v));
    const targetKey = v => typeof v === "string" && /^(BIZINFO|GOV24_PUBLIC_SERVICE|LOCAL_GOV_NOTICE:[A-Za-z0-9_-]{1,100})$/.test(v);
    function target(t, P) {
        const a = t?.formatApplicability;
        if (!t || !targetKey(t.targetKey) || !Object.hasOwn(bindings, t.bindingStatusCode)
            || ![t.referenceCount, t.executableCount, t.normalNoticeCount].every(P.integer)
            || t.executableCount > t.referenceCount || t.normalNoticeCount > t.executableCount
            || t.requiredNormalNoticeCount !== 3 || typeof t.isExpectationCoverageComplete !== "boolean"
            || !validFormats(t.missingFormats) || !a || !validFormats(a.expectedProvidedFormats) || !validFormats(a.unobservedFormats)
            || !P.integer(a.normalMultiFileNoticeCount) || a.normalMultiFileNoticeCount > t.normalNoticeCount
            || !sameSet(a.unobservedFormats, formats.filter(f => !a.expectedProvidedFormats.includes(f)))
            || !t.missingFormats.every(f => a.expectedProvidedFormats.includes(f))) return false;
        if (a.statusCode !== (a.expectedProvidedFormats.length ? "FIXED_SAMPLE_EXPECTATIONS" : "EXPECTATIONS_UNKNOWN")) return false;
        if (!a.expectedProvidedFormats.length && (t.normalNoticeCount > 0 || a.normalMultiFileNoticeCount > 0)) return false;
        const complete = t.bindingStatusCode === "SYSTEM_BINDING_MATCHED" && t.referenceCount > 0
            && t.executableCount === t.referenceCount && t.normalNoticeCount >= 3 && !t.missingFormats.length && a.normalMultiFileNoticeCount >= 1;
        return t.isExpectationCoverageComplete === complete;
    }
    function coverage(d, policyId, n, P) {
        const f = d?.formatCoverage, items = d?.targets?.items;
        return !!(d && d.policyId === policyId && P.version(d.policyVersion)
            && [d.snapshotHash, d.catalogHash, d.planHash].every(P.hash) && d.isQaPassed === false
            && typeof d.isExpectationCoverageComplete === "boolean" && f?.modeCode === "FIXED_SAMPLE_FORMATS_V2"
            && validFormats(f.requiredFormats) && sameSet(f.requiredFormats, formats) && validFormats(f.missingFormats)
            && P.page(d.targets, n, size, t => target(t, P)) && new Set(items.map(t => t.targetKey)).size === items.length
            && (!d.isExpectationCoverageComplete || (!f.missingFormats.length && items.every(t => t.isExpectationCoverageComplete))));
    }
    function parameters(search, P) {
        const q = new URLSearchParams(search), id = q.get("policyId"), n = q.get("page") || "1";
        P.requireValue([...q.keys()].every(k => ["policyId", "page"].includes(k)) && q.getAll("policyId").length === 1
            && q.getAll("page").length <= 1 && P.uuid(id) && /^[1-9][0-9]*$/.test(n)
            && Number.isSafeInteger(Number(n)) && (Number(n) - 1) * size <= 2147483647,
        "정책 ID 또는 페이지가 올바르지 않습니다. 공고 첨부 정책 관리에서 정책을 선택한 뒤 검증 범위를 다시 여세요.");
        return {policyId: id.toLowerCase(), page: Number(n)};
    }
    const fingerprint = d => JSON.stringify([d.policyId, d.policyVersion, d.snapshotHash, d.catalogHash, d.planHash,
        d.targets.totalCount, d.isExpectationCoverageComplete, d.formatCoverage]);
    function mount({page, P, request, doc, navigation}) {
        const q = s => page.querySelector(s);
        const state = {policyId: null, page: 1, data: null, busy: false, failed: false, baseline: null};
        const el = (box, tag, value) => {const e = doc.createElement(tag); if (value != null) e.textContent = String(value); box.append(e); return e;};
        const meta = (box, pairs) => {const dl = el(box, "dl"); dl.className = "attachment-meta";
            for (const [key, value] of pairs) {el(dl, "dt", key); el(dl, "dd", value);}};
        const fmt = (a, empty) => a.length ? a.join(" · ") : empty;
        function gates() {
            page.setAttribute("aria-busy", String(state.busy));
            q("[data-refresh]").disabled = state.busy || !state.policyId;
            q("[data-prev]").disabled = state.busy || state.failed || !state.data || state.page <= 1;
            q("[data-next]").disabled = state.busy || state.failed || !state.data || state.page >= state.data.targets.totalPages;
        }
        function clear() {
            state.data = null;
            q("[data-summary]").replaceChildren(); q("[data-targets]").replaceChildren(); q("[data-page-info]").textContent = "";
        }
        function render(d) {
            const box = q("[data-summary]"); box.replaceChildren();
            meta(box, [["전체 대상", `${d.targets.totalCount.toLocaleString("ko-KR")}곳 · 모든 페이지 포함`],
                ["정책 조회 버전", d.policyVersion], ["전체 기대값 범위", d.isExpectationCoverageComplete ? "기대값 준비됨 · 실제 검증은 별도" : "기대값 준비 미완료"],
                ["전체 필수 형식", fmt(d.formatCoverage.requiredFormats)],
                ["전체 완전 추출 기대값 부족 형식", fmt(d.formatCoverage.missingFormats, "없음 · 실제 추출 통과 아님")],
                ["실제 QA 통과 여부", "이 조회는 통과 근거가 아닙니다"]]);
            const evidence = el(box, "details"); el(evidence, "summary", "조회 대상과 계획 지문 확인");
            meta(evidence, [["정책 ID", d.policyId], ["입력 지문", d.snapshotHash], ["표본 지문", d.catalogHash], ["계획 지문", d.planHash]]);
            const list = q("[data-targets]"); list.replaceChildren();
            for (const t of d.targets.items) {
                const card = el(list, "article"); card.className = "attachment-evidence-item";
                el(card, "h3", t.targetKey === "BIZINFO" ? "기업마당" : t.targetKey === "GOV24_PUBLIC_SERVICE" ? "정부24" : `지자체 · ${t.targetKey.split(":")[1]}`);
                const a = t.formatApplicability;
                meta(card, [["시스템 연결", bindings[t.bindingStatusCode]],
                    ["기대값 범위", t.isExpectationCoverageComplete ? "기대값 준비됨 · 실제 QA 전" : "기대값 준비 미완료"],
                    ["공고 참조 / 실행 기대값", `${t.referenceCount}건 / ${t.executableCount}건`],
                    ["정상 공고 기대값", `${t.normalNoticeCount}건 / 최소 ${t.requiredNormalNoticeCount}건`],
                    ["정상 다중 첨부 기대값", `${a.normalMultiFileNoticeCount}건 / 최소 1건`],
                    ["관측 기대 형식", fmt(a.expectedProvidedFormats, "관측 기대값 없음 · 확인 필요")],
                    ["미관측 형식", fmt(a.unobservedFormats, "없음") + " · 기관 미지원이나 검증 면제가 아님"],
                    ["완전 추출 기대값 부족 형식", fmt(t.missingFormats, a.expectedProvidedFormats.length ? "없음 · 실제 QA 통과 아님" : "요구 형식 미확정 · 부족 없음으로 판단하지 않음")]]);
            }
            if (!d.targets.items.length) el(list, "p", d.targets.totalCount ? "이 페이지에는 대상이 없습니다. 이전 페이지로 이동하거나 최신 계획 첫 페이지를 조회하세요." : "조회된 검증 대상이 없습니다. 시스템 대상 설정을 확인하세요. 전체 QA 통과를 뜻하지 않습니다.");
            q("[data-page-info]").textContent = `${state.page}페이지 / ${d.targets.totalPages}페이지 · 현재 ${d.targets.items.length}곳 표시 · 전체 ${d.targets.totalCount}곳`;
        }
        async function load(n, reset = false, moveFocus = false) {
            if (state.busy || !state.policyId) return;
            state.busy = true; state.failed = false; state.page = n;
            if (reset) state.baseline = null;
            clear(); gates(); q("[data-error]").hidden = true;
            q("[data-status]").textContent = `${n}페이지 검증 범위를 조회 중입니다. 수집이나 데이터 변경은 실행하지 않습니다.`;
            navigation.replace({policyId: state.policyId, page: n});
            try {
                const d = await request(`${P.base}/${state.policyId}/provider-qa-runs/execution-plan/targets?page=${n}&size=${size}`, {method: "GET"});
                P.requireValue(coverage(d, state.policyId, n, P), "CONTRACT");
                P.requireValue(!state.baseline || state.baseline === fingerprint(d), "CHANGED");
                state.baseline = fingerprint(d); state.data = d; render(d);
                q("[data-status]").textContent = "검증 범위 조회 완료. 실제 파일 QA 성공이나 정책 활성화를 뜻하지 않습니다.";
                if (moveFocus) q("#coverage-targets").focus();
            } catch (e) {
                clear(); state.failed = true;
                const message = e.message === "CHANGED" ? "페이지 이동 중 정책·표본·대상 계획이 변경됐습니다. 이전 계획과 섞지 않습니다. 최신 계획 첫 페이지를 다시 조회하세요."
                    : e.message === "CONTRACT" ? "검증 범위 응답의 대상·형식·건수가 현재 계약과 다릅니다. 서버 버전을 확인한 뒤 최신 계획 첫 페이지를 조회하세요."
                    : e.status === 401 ? "로그인이 만료됐습니다. 다른 탭에서 로그인한 뒤 최신 계획 첫 페이지를 조회하세요. 현재 정책 주소는 유지됩니다."
                    : e.status === 403 ? "검증 범위를 조회할 권한이 없습니다. 활성 관리자·운영자·승인자 계정과 비밀번호 변경 완료 여부를 확인하세요."
                    : e.status === 409 ? "현재 설치·정책·규칙의 검증 계획을 확정할 수 없습니다. 담당자가 Linux 격리 추출기 설치와 정책·규칙 상태를 확인한 뒤 다시 조회하세요."
                    : e.status === 404 ? "선택한 정책을 찾을 수 없습니다. 공고 첨부 정책 관리에서 정책을 다시 선택하세요."
                    : "서버 연결 또는 응답을 확인하지 못했습니다. 연결 상태를 확인한 뒤 최신 계획 첫 페이지를 조회하세요. 데이터 변경은 요청하지 않았습니다.";
                q("[data-status]").textContent = "조회 미완료 · 검증 범위 및 QA 통과 여부를 판단하지 않습니다.";
                q("[data-error]").textContent = message; q("[data-error]").hidden = false; q("[data-error]").focus();
            } finally {state.busy = false; gates();}
        }
        async function start() {
            try {
                const p = parameters(navigation.search(), P); state.policyId = p.policyId;
                q("[data-policy-link]").href = "/app/admin/announcement-attachment-policies?policyId=" + p.policyId;
                await load(p.page);
            } catch (e) {
                state.failed = true; clear(); gates();
                q("[data-status]").textContent = "유효한 정책을 선택해야 조회할 수 있습니다.";
                q("[data-error]").textContent = e.message; q("[data-error]").hidden = false; q("[data-error]").focus();
            }
        }
        q("[data-refresh]").addEventListener("click", () => load(1, true));
        q("[data-prev]").addEventListener("click", () => {if (!q("[data-prev]").disabled) return load(state.page - 1, false, true);});
        q("[data-next]").addEventListener("click", () => {if (!q("[data-next]").disabled) return load(state.page + 1, false, true);});
        return {state, start};
    }
    const api = {target, coverage, parameters, mount, route, size};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else {
        root.SanebAttachmentProviderCoverage = api;
        const page = document.querySelector("[data-provider-coverage]"), P = root.SanebAttachmentPolicy;
        if (page && P) mount({page, P, request: P.client(root.fetch.bind(root)), doc: document,
            navigation: {search: () => root.location.search, replace: params => root.history.replaceState(null, "", route + "?" + new URLSearchParams(params))}}).start();
    }
})(globalThis);
