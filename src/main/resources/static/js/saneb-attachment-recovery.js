/* 일반 작업 원복의 명시적 승인 UI. 서버의 소유권·권한·불변 승인·CAS를 대체하지 않는다. */
((root) => {
    "use strict";
    const uuid = value => typeof value === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
    const version = value => Number.isInteger(value) && value >= 0 && value <= 2147483647;
    const modes = {APPLIED: "적용한 첨부 판정 원복", FAILED_RESERVATION: "실패로 종료된 예약 해소", UNAVAILABLE: "원복 대상 여부 확인 필요"};
    const reasons = {
        READY: "조회 시점에 원복 가능합니다. 아래 영향과 사유를 확인한 관리자의 승인이 필요합니다.",
        CURRENT_BINDING_CHANGED: "원문·첨부·기관 정보가 변경됐거나 후속 검수·공고 연결·다른 작업이 있습니다. 현재 작업을 덮어쓰지 않습니다.",
        PREVIOUS_BINDING_INVALID: "이전 판정의 본문·규칙·첨부 연결이 유효하지 않습니다. 담당자와 재검수 절차를 확인하세요.",
        JOB_NOT_TERMINAL: "완료된 적용 또는 실패로 종료된 예약만 복구할 수 있습니다. 작업 종료 후 최신 영향을 조회하세요.",
        RECOVERY_EVIDENCE_MISSING: "예약 당시 복구 근거가 없는 작업입니다. 과거 버전을 추정해 복구하지 않습니다.",
        ALREADY_RECOVERED: "이미 원복한 작업입니다. 목록의 원복 영수증을 확인하세요. 다시 복구하지 않습니다."
    };
    const matchesPreview = (source, jobId, p) => !!(source && p && uuid(jobId) && p.sourceId === source.sourceId && p.jobId === jobId
        && version(p.sourceVersion) && version(p.attachmentVersion) && source.sourceVersion === p.sourceVersion && source.attachmentVersion === p.attachmentVersion
        && Object.hasOwn(modes, p.modeCode) && Object.hasOwn(reasons, p.readinessCode)
        && typeof p.previewHash === "string" && /^[0-9a-f]{64}$/.test(p.previewHash)
        && [p.baseReopens, p.confirmationRestores, p.staleConfirmationRemains].every(v => typeof v === "boolean")
        && !(p.baseReopens && p.confirmationRestores) && !(p.staleConfirmationRemains && p.confirmationRestores)
        && p.targetCount === 1 && p.currentHttpRequests === 0);
    const ready = (source, jobId, p) => matchesPreview(source, jobId, p) && p.readinessCode === "READY"
        && p.attachmentVersion < 2147483647 && p.rollbackStatusCode === "NOT_REQUESTED"
        && (p.modeCode === "APPLIED" && p.applicationStatusCode === "APPLIED" && ["SUCCEEDED", "PARTIAL_FAILED"].includes(p.jobStatusCode)
            || p.modeCode === "FAILED_RESERVATION" && p.applicationStatusCode === "PENDING" && ["FAILED", "CONFLICT", "CANCELLED"].includes(p.jobStatusCode));
    const prepare = (source, jobId, p, input) => {
        if (!ready(source, jobId, p)) throw new Error("현재 원문의 버전과 원복 가능 조건이 일치하지 않습니다. 최신 기준과 원복 영향을 다시 조회하세요.");
        return command(p, input);
    };
    const command = (p, input) => {
        const reason = input.reason?.trim();
        if (!reason || reason.length > 1000) throw new Error("원복 사유를 1~1000자로 입력하세요.");
        if (input.acknowledged !== true) throw new Error("작업·버전·기본 경로와 이전 검수의 복구 영향을 직접 확인하세요.");
        return {expectedSourceVersion: p.sourceVersion, expectedAttachmentVersion: p.attachmentVersion, expectedPreviewHash: p.previewHash,
            expectedBaseReopen: p.baseReopens, expectedConfirmationRestore: p.confirmationRestores, acknowledgeBindingRestoration: true, reason};
    };
    const validReceipt = (r, sourceId, jobId, expected = null, actionId = null) => !!(r && r.sourceId === sourceId && r.jobId === jobId
        && uuid(r.actionId) && (!actionId || r.actionId === actionId) && ["APPLIED", "FAILED_RESERVATION"].includes(r.modeCode)
        && r.statusCode === "ROLLED_BACK" && version(r.restoredSourceVersion) && version(r.restoredAttachmentVersion)
        && typeof r.baseReopened === "boolean" && typeof r.confirmationRestored === "boolean" && !(r.baseReopened && r.confirmationRestored)
        && r.targetCount === 1 && r.currentHttpRequests === 0 && typeof r.recordedAt === "string" && Number.isFinite(Date.parse(r.recordedAt))
        && (!expected || r.modeCode === expected.modeCode && r.restoredSourceVersion === expected.payload.expectedSourceVersion
            && r.restoredAttachmentVersion === expected.payload.expectedAttachmentVersion + 1
            && r.baseReopened === expected.payload.expectedBaseReopen && r.confirmationRestored === expected.payload.expectedConfirmationRestore));
    const validJobs = (data, sourceId, page) => !!(data && data.page === page && data.size === 10 && Number.isSafeInteger(data.totalCount) && data.totalCount >= 0
        && data.totalPages === Math.ceil(data.totalCount / 10) && Array.isArray(data.items) && data.items.length === Math.min(10, Math.max(0, data.totalCount - (page - 1) * 10))
        && new Set(data.items.map(j => j?.jobId)).size === data.items.length && data.items.every(j => j && j.sourceId === sourceId && uuid(j.jobId)
            && (j.actionId === null || uuid(j.actionId)) && [j.operationCode, j.jobStatusCode, j.applicationStatusCode, j.rollbackStatusCode].every(v => typeof v === "string")
            && (j.actionId === null || j.rollbackStatusCode === "ROLLED_BACK")));
    const mount = ({page, C, request, apiRoot, canRollback, text, meta, message, date, changed, blocked, refresh, navigation}) => {
        const q = selector => page.querySelector(selector), form = q("[data-recovery-form]"), attempt = C.mutation(() => crypto.randomUUID());
        let source = null, preview = null, selection = null, list = null, number = 1, busy = false, stale = false, dirty = false, generation = 0, sent = null;
        const path = jobId => `${apiRoot}/attachment-jobs/${encodeURIComponent(jobId)}/rollback`;
        const gates = () => {
            const disabled = busy || blocked() || attempt.uncertain;
            const writable = canRollback && !disabled && !stale && ready(source, selection, preview);
            q("[data-recovery-fields]").disabled = !writable; q("[data-recovery-submit]").disabled = !writable;
            q("[data-recovery-load]").disabled = disabled || !source;
            q("[data-recovery-previous]").disabled = disabled || !list || number <= 1;
            q("[data-recovery-next]").disabled = disabled || !list || number >= list.totalPages;
            q("[data-recovery-jobs]").querySelectorAll("button").forEach(button => { button.disabled = disabled; });
            q("[data-recovery-retry]").hidden = !attempt.uncertain;
            q("[data-recovery-retry]").disabled = busy || blocked() || !canRollback;
            q("[data-recovery-submit]").textContent = busy ? "원복 결과 확인 중…" : "확인한 공고 1건의 이전 연결 복구";
        };
        const clearPreview = () => {
            preview = null; form.elements.restorationAcknowledged.checked = false; q("[data-recovery-impact]").replaceChildren();
        };
        const receipt = r => {
            const container = q("[data-recovery-receipt]"); container.replaceChildren();
            meta(container, [["원복 영수증", r.actionId], ["작업", r.jobId], ["복구 유형", modes[r.modeCode]],
                ["복구 당시 원문 / 첨부 버전", `${r.restoredSourceVersion} / ${r.restoredAttachmentVersion}`],
                ["기본 판정 경로 재개", r.baseReopened ? "재개함" : "재개하지 않음"], ["이전 검수 확인", r.confirmationRestored ? "복구함 · 새로운 검수 아님" : "복구하지 않음"],
                ["기록 시각", date(r.recordedAt)], ["외부 요청 / 삭제 / 자동 활성화", "모두 없음"]]);
            message("[data-recovery-result-state]", "해당 작업의 원복 완료 기록을 확인했습니다. 현재 유효 상태는 상단의 최신 기준 조회로 별도 확인하세요. 원래 수집·실패 이력은 보존됩니다.");
        };
        const showReceipt = async (jobId, actionId) => {
            if (!source || busy || blocked() || attempt.uncertain) return;
            const ticket = ++generation; busy = true; changed(); gates();
            message("[data-recovery-error]", "");
            message("[data-recovery-result-state]", "선택한 작업의 원복 영수증을 조회 중입니다.");
            try {
                const r = await request(`${path(jobId)}/actions/${encodeURIComponent(actionId)}`);
                if (ticket !== generation) return;
                if (!validReceipt(r, source.sourceId, jobId, null, actionId)) throw new Error("영수증의 원문·작업·복구 결과가 요청과 일치하지 않습니다. 목록을 다시 조회하세요.");
                receipt(r);
            } catch (error) { if (ticket === generation) { message("[data-recovery-result-state]", "영수증 조회 실패 · 이전에 표시한 영수증이 있다면 해당 작업의 과거 기록입니다."); message("[data-recovery-error]", error.message, true); } }
            finally { if (ticket === generation) { busy = false; changed(); gates(); } }
        };
        const readPreview = async (jobId, ticket) => {
            selection = jobId; clearPreview(); navigation?.write(number, selection);
            message("[data-recovery-state]", "선택한 작업의 현재 원복 영향을 조회 중입니다. 복구는 실행하지 않습니다.");
            const p = await request(`${path(jobId)}/preview`);
            if (ticket !== generation) return;
            if (!matchesPreview(source, jobId, p)) throw new Error("원복 영향과 현재 원문의 버전·식별자·응답 계약이 일치하지 않습니다. 상단에서 최신 기준을 조회하세요.");
            preview = p;
            meta(q("[data-recovery-impact]"), [["원문 / 작업", `${p.sourceId} / ${p.jobId}`], ["유형", modes[p.modeCode]],
                ["원래 수집 상태 / 적용 상태", `${C.label(p.jobStatusCode)} / ${C.label(p.applicationStatusCode)}`], ["원복 상태", C.label(p.rollbackStatusCode)],
                ["현재 원문 / 첨부 버전", `${p.sourceVersion} / ${p.attachmentVersion}`], ["기본 판정 경로 재개", p.baseReopens ? "재개 예정 · 첨부 검수 의무 해제" : "재개하지 않음"],
                ["이전 검수 확인", p.confirmationRestores ? "복구 예정 · 새로운 검수 아님" : p.staleConfirmationRemains ? "오래된 확인 유지 · 다시 검수 필요" : "복구하지 않음"],
                ["범위 / 외부 요청", "공고 1건 / 0회"], ["영향 검증 지문", p.previewHash]]);
            message("[data-recovery-state]", reasons[p.readinessCode] + (!canRollback ? " 조회 전용 권한입니다. 원복 승인은 관리자에게 요청하세요." : ""));
        };
        const choose = async jobId => {
            if (!source || busy || blocked() || attempt.uncertain) return;
            const ticket = ++generation; busy = true; message("[data-recovery-error]", ""); changed(); gates();
            try { await readPreview(jobId, ticket); if (ticket === generation) q("[data-recovery-preview-title]").focus(); }
            catch (error) { if (ticket === generation) { clearPreview(); message("[data-recovery-state]", "원복 영향 조회 미완료 · 승인할 수 없습니다."); message("[data-recovery-error]", error.message, true); } }
            finally { if (ticket === generation) { busy = false; changed(); gates(); } }
        };
        const readJobs = async (target, ticket) => {
            list = null; q("[data-recovery-jobs]").replaceChildren(); message("[data-recovery-list-status]", "일반 작업 이력을 조회 중입니다.");
            const data = await request(`${apiRoot}/attachment-recovery-jobs?page=${target}&size=10`);
            if (ticket !== generation) return;
            if (!validJobs(data, source.sourceId, target)) throw new Error("작업 목록의 원문·전체 건수·페이지 정보가 일치하지 않습니다. 목록을 다시 조회하세요.");
            if (target > 1 && target > data.totalPages) return readJobs(Math.max(1, data.totalPages), ticket);
            list = data; number = target; navigation?.write(number, selection);
            message("[data-recovery-list-status]", `일반 작업 ${data.totalCount}건 · ${data.totalPages ? number : 0}/${data.totalPages}페이지. 배치 작업은 포함하지 않습니다.`);
            if (!data.items.length) text(q("[data-recovery-jobs]"), "p", "이 원문에 일반 작업 이력이 없습니다. 첨부 없음이나 배치 성공을 뜻하지 않습니다.");
            data.items.forEach(job => {
                const item = text(q("[data-recovery-jobs]"), "article", "", "attachment-evidence-item");
                text(item, "h3", `${C.label(job.operationCode)} · ${date(job.createdAt)}`);
                meta(item, [["작업 ID", job.jobId], ["수집 / 적용 / 원복", `${C.label(job.jobStatusCode)} / ${C.label(job.applicationStatusCode)} / ${C.label(job.rollbackStatusCode)}`]]);
                const button = text(item, "button", "이 작업의 원복 영향 조회", "secondary-action"); button.type = "button";
                button.addEventListener("click", () => choose(job.jobId));
                if (job.actionId) {
                    const view = text(item, "button", "이 작업의 원복 영수증 조회", "secondary-action"); view.type = "button";
                    view.addEventListener("click", () => showReceipt(job.jobId, job.actionId));
                }
            });
        };
        const listPage = async target => {
            if (!source || busy || blocked() || attempt.uncertain || !Number.isInteger(target) || target < 1 || target > 1000000) return;
            const ticket = ++generation; busy = true; changed(); gates(); message("[data-recovery-error]", "");
            try { await readJobs(target, ticket); }
            catch (error) { if (ticket === generation) { message("[data-recovery-list-status]", "목록 조회 실패 · 0건으로 판단하지 마세요."); message("[data-recovery-error]", error.message, true); } }
            finally { if (ticket === generation) { busy = false; changed(); gates(); } }
        };
        const load = async nextSource => {
            if (attempt.pending || attempt.uncertain) return;
            const ticket = ++generation; source = nextSource; busy = false; clearPreview(); list = null; stale = false; q("[data-recovery-jobs]").replaceChildren();
            if (!source) { message("[data-recovery-state]", "원문 조회 실패로 복구 승인을 잠갔습니다. 입력 사유는 유지됩니다."); message("[data-recovery-list-status]", "원문 확인 전에는 작업을 조회할 수 없습니다."); gates(); return; }
            const nav = navigation?.read();
            if (nav) { number = Number.isInteger(nav.page) && nav.page >= 1 && nav.page <= 1000000 ? nav.page : 1; selection = uuid(nav.jobId) ? nav.jobId : null; }
            if (selection) q("[data-recovery-panel]").open = true;
            busy = true; changed(); gates(); message("[data-recovery-error]", "");
            try { await readJobs(number, ticket); if (ticket === generation && selection) await readPreview(selection, ticket); }
            catch (error) { if (ticket === generation) { clearPreview(); if (!list) message("[data-recovery-list-status]", "목록 조회 실패 · 0건으로 판단하지 마세요."); message("[data-recovery-state]", "일반 작업 또는 원복 영향 조회 미완료 · 승인할 수 없습니다."); message("[data-recovery-error]", error.message); } }
            finally { if (ticket === generation) { busy = false; changed(); gates(); } }
        };
        const submit = async (replay = false) => {
            if (!source || busy || blocked() || !canRollback || (!replay && (stale || attempt.uncertain)) || (replay && !attempt.uncertain)) return;
            message("[data-recovery-error]", "");
            try {
                if (!replay) {
                    if (!form.reportValidity()) return;
                    const payload = prepare(source, selection, preview, {reason: form.elements.reason.value, acknowledged: form.elements.restorationAcknowledged.checked});
                    sent = {jobId: selection, modeCode: preview.modeCode, payload};
                }
                const prepared = attempt.prepare(sent.payload); busy = true; changed(); gates();
                const r = await request(path(sent.jobId), {method: "POST", body: prepared.body, headers: {"Idempotency-Key": prepared.key}});
                if (!validReceipt(r, source.sourceId, sent.jobId, sent)) throw new C.RequestError("원복 응답의 작업·버전·영향을 확인하지 못했습니다. 동일 요청으로 결과를 재확인하세요.");
                attempt.succeed(); receipt(r); dirty = false; stale = true; busy = false; sent = null;
                form.elements.restorationAcknowledged.checked = false; changed(); await refresh();
            } catch (error) {
                if (attempt.pending) {
                    // 최초 응답 유실 뒤 401/403/409도 최초 요청의 미완료 증거는 아니다. 새 키로 바꾸지 않는다.
                    attempt.fail(replay ? new C.RequestError(error.message) : error); stale = true;
                }
                message("[data-recovery-error]", error.message + (attempt.uncertain ? " 이 탭과 최초 승인 계정을 유지하고 동일 원복 요청으로 재확인하세요." : " 입력 사유를 유지했습니다. 최신 기준을 조회하고 영향을 다시 확인하세요."), true);
            } finally { busy = false; changed(); gates(); }
        };
        form.addEventListener("input", event => { dirty = true; if (event.target.name !== "restorationAcknowledged") form.elements.restorationAcknowledged.checked = false; changed(); });
        form.addEventListener("submit", event => { event.preventDefault(); submit(); });
        q("[data-recovery-retry]").addEventListener("click", () => submit(true));
        q("[data-recovery-load]").addEventListener("click", () => listPage(number));
        q("[data-recovery-previous]").addEventListener("click", () => listPage(number - 1));
        q("[data-recovery-next]").addEventListener("click", () => listPage(number + 1));
        return {load, gates, get busy() { return busy; }, get uncertain() { return attempt.uncertain; }, get stale() { return stale; }, get dirty() { return dirty; }};
    };
    const api = {matchesPreview, ready, prepare, validReceipt, validJobs, mount};
    if (typeof module !== "undefined" && module.exports) module.exports = api; else root.SanebAttachmentRecovery = api;
})(globalThis);
