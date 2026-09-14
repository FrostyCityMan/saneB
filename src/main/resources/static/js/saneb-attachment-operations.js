/* 현재 공고 한 건의 복구 계약. 과거 근거 탐색과 별도이며 서버의 권한/버전 검증을 대체하지 않는다. */
((root) => {
    "use strict";
    const uuid = value => typeof value === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
    const hash = value => typeof value === "string" && /^[0-9a-f]{64}$/.test(value);
    const roles = ["NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN"];
    const classification = source => source?.isAttachmentReviewRequired ? source.effectiveClassification : source?.previewClassification;
    const version = source => ({expectedBaseDecisionId: source?.baseClassification?.decisionId,
        expectedAttachmentDecisionId: classification(source)?.decisionId ?? null,
        expectedSourceVersion: source?.sourceVersion, expectedAttachmentVersion: source?.attachmentVersion});
    const validVersion = v => uuid(v.expectedBaseDecisionId) && (v.expectedAttachmentDecisionId === null || uuid(v.expectedAttachmentDecisionId))
        && Number.isInteger(v.expectedSourceVersion) && v.expectedSourceVersion >= 0
        && Number.isInteger(v.expectedAttachmentVersion) && v.expectedAttachmentVersion >= 0 && v.expectedAttachmentVersion < 2147483647;
    const matchesCollection = (source, context) => {
        const v = version(source);
        return !!(source && context && validVersion(v) && context.version && Object.keys(v).every(k => v[k] === context.version[k])
            && uuid(context.policyId) && hash(context.policyHash) && hash(context.executionHash)
            && ["COLLECT_ONLY", "ENFORCE"].includes(context.modeCode)
            && context.isAttachmentReviewRequired === source.isAttachmentReviewRequired
            && context.effectCode === (source.isAttachmentReviewRequired ? "PRESERVE_ENFORCE_AND_STALE_CONFIRMATION" : "COLLECT_PREVIEW_ONLY")
            && Number.isInteger(context.maximumDownloadBytes) && context.maximumDownloadBytes > 0 && context.maximumDownloadBytes <= 83886080
            && context.maximumFileCount === 10 && context.maximumAttempts === 3 && context.maximumHttpRequests === 132);
    };
    const currentFiles = (source, data) => {
        const c = classification(source), a = source?.attachmentSummary, v = version(source);
        return !!(validVersion(v) && uuid(c?.decisionId) && uuid(c.setId) && hash(c.setHash) && a && !a.isStale
            && !["PENDING", "RUNNING", "RETRY_WAIT"].includes(a.jobStatusCode)
            && data?.page === 1 && data.totalPages === 1 && Array.isArray(data.items) && data.items.length > 0 && data.items.length <= 10
            && data.totalCount === data.items.length && a.totalCount === data.items.length && a.processedCount === data.items.length
            && new Set(data.items.map(f => f.fileId)).size === data.items.length
            && data.items.every(f => uuid(f.fileId) && f.setId === c.setId && roles.includes(f.documentRoleCode)));
    };
    const retryable = file => ["FAILED", "CANCELLED"].includes(file.downloadStatusCode)
        || (file.downloadStatusCode === "SUCCEEDED" && ["PARTIAL_TEXT", "CORRUPT", "LIMIT_EXCEEDED", "TIMEOUT", "FAILED", "ISOLATION_UNAVAILABLE"].includes(file.qualityCode));
    const prepare = (source, context, data, input) => {
        const fail = message => { throw new Error(message); };
        const reason = input.reason?.trim();
        if (!reason || reason.length > 1000) fail("복구·변경 사유를 1~1000자로 입력하세요.");
        if (!input.acknowledged) fail("대상·요청 상한과 재검수 필요를 직접 확인하세요.");
        if (!["COLLECT", "RETRY_FILES", "ROLE_CHANGE"].includes(input.operation)) fail("실행할 첨부 작업을 선택하세요.");
        const network = input.operation !== "ROLE_CHANGE";
        if (network && !matchesCollection(source, context)) fail("현재 원문과 게시 수집 조건이 일치하지 않습니다. 최신 기준을 다시 조회하세요.");
        if (network && (!Number.isInteger(input.maximumDownloadBytes) || input.maximumDownloadBytes < 1 || input.maximumDownloadBytes > context.maximumDownloadBytes))
            fail(`다운로드 상한은 1~${context.maximumDownloadBytes}바이트 정수로 입력하세요.`);
        if (input.operation === "COLLECT") return {operation: input.operation, path: "/attachment-jobs/collection", method: "POST", maximumHttpRequests: 132,
            payload: {version: context.version, expectedPolicyId: context.policyId, expectedPolicyHash: context.policyHash,
                expectedExecutionHash: context.executionHash, maximumDownloadBytes: input.maximumDownloadBytes, reason}};
        if (!currentFiles(source, data)) fail("현재 집합 전체의 파일·판정·처리 건수를 확인하지 못했습니다. 최신 기준을 조회하세요.");
        const c = classification(source), base = {version: {...version(source), expectedSetHash: c.setHash}, expectedSetId: c.setId, reason};
        if (input.operation === "RETRY_FILES") {
            const ids = input.fileIds;
            if (!source.attachmentSummary.isDiscoveryComplete || source.attachmentSummary.discoveryStatusCode !== "FOUND")
                fail("첨부 전체 발견이 완료되지 않았습니다. 실패 파일 재시도 대신 전체 재수집 조건을 확인하세요.");
            if (!Array.isArray(ids) || !ids.length || ids.length > 10 || new Set(ids).size !== ids.length
                || ids.some(id => !data.items.some(file => file.fileId === id && retryable(file))))
                fail("현재 집합에서 재시도 가능한 실패 파일을 1~10개 선택하세요. 성공·OCR·차단 파일은 선택할 수 없습니다.");
            return {operation: input.operation, path: "/attachment-jobs", method: "POST", maximumHttpRequests: 12 * (1 + ids.length),
                payload: {...base, fileIds: [...ids].sort(), maximumDownloadBytes: input.maximumDownloadBytes}};
        }
        const values = input.fileRoles;
        if (!Array.isArray(values) || values.length !== data.items.length || new Set(values.map(f => f?.fileId)).size !== data.items.length
            || values.some(f => !f || !roles.includes(f.documentRoleCode) || !data.items.some(old => old.fileId === f.fileId)))
            fail("현재 집합 전체 파일의 역할을 각각 한 번씩 선택하세요.");
        if (!values.some(f => data.items.some(old => old.fileId === f.fileId && old.documentRoleCode !== f.documentRoleCode)))
            fail("현재와 다른 문서 역할을 1개 이상 선택하세요.");
        return {operation: input.operation, path: "/attachment-roles", method: "PUT", maximumHttpRequests: 0,
            payload: {...base, fileRoles: [...values].sort((a, b) => a.fileId.localeCompare(b.fileId))}};
    };
    const mount = ({page, C, request, apiRoot, canManage, text, meta, message, changed, blocked, refresh}) => {
        const q = selector => page.querySelector(selector), form = q("[data-operation-form]");
        const attempt = C.mutation(() => crypto.randomUUID());
        let source = null, context = null, data = null, generation = 0, busy = false, locked = true, dirty = false, stale = false, jobId = null, sent = null;
        const selectedFiles = () => [...form.querySelectorAll("[data-retry-file]:checked")].map(n => n.value);
        const gates = () => {
            const disabled = !canManage || busy || blocked() || locked || attempt.uncertain;
            q("[data-operation-fields]").disabled = disabled;
            q("[data-operation-submit]").disabled = disabled;
            q("[data-operation-retry]").hidden = !attempt.uncertain;
            q("[data-operation-retry]").disabled = busy || blocked();
            q("[data-operation-job]").disabled = !jobId || busy || blocked() || attempt.uncertain;
            q("[data-operation-submit]").textContent = busy ? "첨부 작업 확인 중…" : "선택한 첨부 작업 예약";
        };
        const impact = () => {
            const op = form.elements.operation.value, count = selectedFiles().length;
            const isRole = op === "ROLE_CHANGE";
            q("[data-operation-budget]").hidden = isRole;
            form.elements.maximumDownloadBytes.required = !isRole;
            form.querySelectorAll("[data-file-retry-label]").forEach(n => { n.hidden = op !== "RETRY_FILES"; });
            form.querySelectorAll("[data-file-role-label]").forEach(n => { n.hidden = !isRole; });
            const max = op === "COLLECT" ? 132 : op === "RETRY_FILES" ? 12 * (1 + count) : 0;
            const scope = op === "COLLECT" ? "전체 첨부 (발견 파일 최대 10개)" : op === "RETRY_FILES" ? `선택 실패 파일 ${count}개` : `현재 전체 파일 ${data?.items.length ?? 0}개의 역할`;
            message("[data-operation-impact]", !op ? "작업을 선택하면 대상과 요청 상한을 표시합니다." :
                `공고 1건 · ${scope}. 외부 HTTP 최대 ${max}회${isRole ? " · 다운로드 없음" : ` · 전체 다운로드 상한 ${form.elements.maximumDownloadBytes.value || "미입력"}바이트`}. 이전 확인은 STALE이며 새 결과의 재검수가 필요합니다. 자동 활성화 없음.`);
        };
        const load = async (nextSource, linked = false) => {
            const ticket = ++generation; source = nextSource; context = null; data = null; locked = true; stale = false;
            const container = q("[data-operation-files]"); container.replaceChildren(); q("[data-operation-limits]").replaceChildren();
            form.elements.operationAcknowledged.checked = false;
            if (!source) { jobId = null; message("[data-operation-state]", "원문 조회 실패로 첨부 복구를 잠갔습니다."); gates(); return; }
            jobId = source.attachmentSummary?.jobId || jobId;
            let contextError = null, filesError = null;
            try { context = await request(`${apiRoot}/attachment-collection-context`); } catch (error) { contextError = error; }
            if (ticket !== generation) return;
            const c = classification(source);
            if (uuid(c?.setId)) {
                try { data = await request(`${apiRoot}/attachment-sets/${encodeURIComponent(c.setId)}/files?page=1&size=10`); } catch (error) { filesError = error; }
            }
            if (ticket !== generation) return;
            const hasFiles = currentFiles(source, data), collect = matchesCollection(source, context);
            const terminal = ["PENDING", "RUNNING", "RETRY_WAIT"].includes(source.attachmentSummary?.jobStatusCode);
            locked = linked || terminal || (!hasFiles && !collect);
            [...form.elements.operation.options].forEach(option => {
                option.disabled = option.value === "COLLECT" ? !collect : option.value === "RETRY_FILES" ? !collect || !hasFiles
                    || !source.attachmentSummary?.isDiscoveryComplete || source.attachmentSummary?.discoveryStatusCode !== "FOUND"
                    || !data.items.some(retryable) : option.value === "ROLE_CHANGE" ? !hasFiles : false;
            });
            // 조회 이전의 파일 선택은 새 기준에 재사용하지 않는다. 입력 사유는 보존한다.
            form.elements.operation.value = "";
            if (collect) {
                form.elements.maximumDownloadBytes.max = context.maximumDownloadBytes;
                if (!form.elements.maximumDownloadBytes.value) form.elements.maximumDownloadBytes.value = context.maximumDownloadBytes;
                meta(q("[data-operation-limits]"), [["수집 정책", context.policyId], ["정책 모드", context.modeCode === "ENFORCE" ? "첨부 검수 적용 정책" : "미리보기 수집 정책"],
                    ["이 공고에 대한 효과", source.isAttachmentReviewRequired ? "기존 검수 의무 유지 · 이전 확인 STALE" : "미리보기만 수집 · 검수 의무 신규 적용 안 함"],
                    ["전체 수집 상한", `파일 ${context.maximumFileCount}개 · 시도 ${context.maximumAttempts}회 · HTTP ${context.maximumHttpRequests}회 · ${context.maximumDownloadBytes}바이트`]]);
            }
            if (hasFiles) data.items.forEach(file => {
                const item = text(container, "div", "", "attachment-evidence-item");
                text(item, "p", `${file.displayName || "이름 미확인"} · ${C.label(file.documentRoleCode)} · ${C.label(file.downloadStatusCode)} · ${C.label(file.qualityCode)}`);
                const retryLabel = text(item, "label", "", "attachment-check"); retryLabel.dataset.fileRetryLabel = "";
                const input = document.createElement("input"); input.type = "checkbox"; input.dataset.retryFile = ""; input.value = file.fileId; input.disabled = !retryable(file);
                retryLabel.append(input, document.createTextNode(retryable(file) ? `재시도 선택: ${file.displayName || file.fileId}` : "재시도 불가 · 원문 직접 확인 또는 전체 수집 조건 확인"));
                const roleLabel = text(item, "label", `변경할 문서 역할: ${file.displayName || file.fileId}`, "field-block"); roleLabel.dataset.fileRoleLabel = "";
                const select = document.createElement("select"); select.dataset.fileRole = file.fileId;
                roles.forEach(role => { const option = text(select, "option", C.label(role)); option.value = role; }); select.value = file.documentRoleCode; roleLabel.append(select);
            });
            if (!hasFiles) text(container, "p", filesError?.message || "변경 가능한 현재 파일 전체를 확인하지 못했습니다. 과거 파일은 변경 기준이 아닙니다.");
            message("[data-operation-state]", !canManage ? "조회 전용 권한입니다. 첨부 작업 예약은 관리자 또는 운영자가 담당합니다."
                : linked ? "이미 공고에 연결되어 첨부 복구로 덮어쓸 수 없습니다."
                    : terminal ? "진행 중인 첨부 작업이 있습니다. 접수한 작업 상태를 확인하세요."
                        : contextError ? `${contextError.message} 저장된 근거의 역할 변경은 별도 조건으로 서버에서 확인합니다.`
                            : "조회한 조건은 예약 승인이 아닙니다. 작업·요청 한도·영향을 선택하고 확인하세요.");
            if (!locked) message("[data-operation-error]", "");
            impact(); gates();
        };
        const submit = async (replay = false) => {
            if (busy || blocked() || !canManage || (!replay && (locked || attempt.uncertain))) return;
            message("[data-operation-error]", "");
            let command = sent;
            try {
                if (!replay) {
                    if (!form.reportValidity()) return;
                    command = prepare(source, context, data, {operation: form.elements.operation.value, reason: form.elements.reason.value,
                        maximumDownloadBytes: Number(form.elements.maximumDownloadBytes.value), acknowledged: form.elements.operationAcknowledged.checked,
                        fileIds: selectedFiles(), fileRoles: [...form.querySelectorAll("[data-file-role]")].map(n => ({fileId: n.dataset.fileRole, documentRoleCode: n.value}))});
                }
                const prepared = attempt.prepare(command.payload); sent = command; busy = true; changed(); gates();
                const result = await request(`${apiRoot}${command.path}`, {method: command.method, body: prepared.body, headers: {"Idempotency-Key": prepared.key}});
                if (!uuid(result.jobId) || result.sourceId !== source.sourceId || result.operationCode !== command.operation)
                    throw new C.RequestError("작업 접수 식별자가 일치하지 않습니다. 동일 요청으로 결과를 다시 확인하세요.");
                jobId = result.jobId; attempt.succeed(); dirty = false; busy = false; locked = true; sent = null;
                message("[data-operation-result]", `작업 ${jobId} · ${C.label(result.jobStatusCode)}. 예약 응답은 파일 처리 성공이 아닙니다. 상태 조회와 최신 근거 확인 후 재검수하세요.`);
                changed(); await refresh();
            } catch (error) {
                // 입력 오류는 요청하지 않았으므로 현재 근거를 잠글 필요가 없다.
                if (attempt.pending) { attempt.fail(error); if (!error.uncertain) { locked = true; stale = true; } }
                message("[data-operation-error]", error.message + (attempt.uncertain ? " 동일 첨부 작업 재시도로 확인하세요." : " 입력 사유를 유지했습니다. 충돌이면 최신 기준을 조회하세요."), true);
            } finally { busy = false; changed(); gates(); }
        };
        q("[data-operation-job]").addEventListener("click", async () => {
            if (busy || blocked() || attempt.uncertain || !jobId) return;
            busy = true; changed(); gates();
            try {
                const result = await request(`${apiRoot}/attachment-jobs/${encodeURIComponent(jobId)}`);
                if (result.jobId !== jobId || result.sourceId !== source.sourceId) throw new Error("조회한 작업이 현재 공고와 일치하지 않습니다.");
                message("[data-operation-result]", `작업 ${jobId} · ${C.label(result.jobStatusCode)} · ${result.errorCode ? C.label(result.errorCode) : "실패 코드 없음"}. 최신 기준 조회로 파일 전체 품질과 검수 상태를 확인하세요.`);
            } catch (error) { message("[data-operation-error]", error.message, true); }
            finally { busy = false; changed(); gates(); }
        });
        form.addEventListener("input", event => { dirty = true; if (event.target.name !== "operationAcknowledged") form.elements.operationAcknowledged.checked = false; impact(); changed(); });
        form.addEventListener("submit", event => { event.preventDefault(); submit(); });
        q("[data-operation-retry]").addEventListener("click", () => submit(true));
        return {load, gates, get busy() { return busy; }, get stale() { return stale; }, get uncertain() { return attempt.uncertain; }, get dirty() { return dirty; }};
    };
    const api = {version, matchesCollection, currentFiles, retryable, prepare, mount};
    if (typeof module !== "undefined" && module.exports) module.exports = api; else root.SanebAttachmentOperations = api;
})(globalThis);
