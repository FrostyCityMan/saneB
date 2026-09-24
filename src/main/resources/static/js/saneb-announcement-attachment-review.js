(() => {
    "use strict";
    const page = document.querySelector("[data-attachment-review-page]");
    if (!page) return;
    const C = window.SanebAttachmentReview;
    const sourceId = page.dataset.sourceId;
    const root = `/api/v2/admin/announcement-sources/${encodeURIComponent(sourceId)}`;
    const request = C.client(window.fetch.bind(window));
    const canManage = page.dataset.canManage === "true";
    const q = selector => page.querySelector(selector);
    const reviewForm = q("[data-review-form]"), draftForm = q("[data-draft-form]");
    const confirmAttempt = C.mutation(() => crypto.randomUUID()), draftAttempt = C.mutation(() => crypto.randomUUID());
    let source = null, context = null, epoch = 0, busy = false, locked = true, reviewDirty = false, draftDirty = false;
    let operations = null, recovery = null;
    let mutationStale = false;
    const panels = new WeakMap();
    const text = (parent, tag, value, className) => {
        const node = document.createElement(tag); node.textContent = value == null ? "미확인" : String(value);
        if (className) node.className = className; parent.append(node); return node;
    };
    const clear = element => { panels.set(element, Symbol()); element.replaceChildren(); };
    const action = (parent, title, callback, disabled = false) => {
        const button = text(parent, "button", title, "secondary-action"); button.type = "button"; button.disabled = disabled;
        button.addEventListener("click", callback); return button;
    };
    const message = (selector, value, focus = false) => {
        const element = q(selector); element.textContent = value || ""; element.hidden = !value;
        if (value && focus) element.focus();
    };
    const meta = (parent, pairs) => {
        const dl = document.createElement("dl"); dl.className = "attachment-meta";
        pairs.forEach(([name, value]) => { text(dl, "dt", name); text(dl, "dd", value); }); parent.append(dl);
    };
    const date = value => {
        if (!value) return "미확인";
        const d = new Date(value); return Number.isNaN(d.getTime()) ? "날짜 확인 필요" : `${new Intl.DateTimeFormat("ko-KR", {
            dateStyle: "medium", timeStyle: "medium", timeZone: "Asia/Seoul"}).format(d)} (서울)`;
    };
    const listLabels = values => values?.length ? values.map(C.label).join(", ") : "없음";
    const choices = (element, name, values) => Object.entries(values).forEach(([code, label]) => {
        const wrapper = document.createElement("label"); wrapper.className = "attachment-check";
        const input = document.createElement("input"); input.type = "checkbox"; input.name = name; input.value = code;
        wrapper.append(input, document.createTextNode(label)); element.append(wrapper);
    });
    choices(q("[data-target-choices]"), "targetCategoryCodes", C.targets);
    choices(q("[data-support-choices]"), "supportTypeCodes", C.supports);
    const selected = name => Array.from(reviewForm.querySelectorAll(`input[name="${name}"]:checked`)).map(node => node.value);
    const uncertain = () => confirmAttempt.uncertain || draftAttempt.uncertain || !!operations?.uncertain || !!recovery?.uncertain;
    const gates = () => {
        const ready = !busy && !operations?.busy && !operations?.stale && !recovery?.busy && !recovery?.stale && !locked && C.matchesContext(source, context) && !context.linkedAnnouncement;
        q("[data-review-fields]").disabled = !canManage || !ready || uncertain();
        q("[data-confirm]").disabled = !canManage || !ready || uncertain();
        const convertible = canManage && ready && !uncertain() && !reviewDirty && C.confirmedCurrent(context);
        q("[data-draft-fields]").disabled = !convertible;
        q("[data-create-draft]").disabled = !convertible;
        q("[data-retry-confirm]").hidden = !confirmAttempt.uncertain;
        q("[data-retry-draft]").hidden = !draftAttempt.uncertain;
        q("[data-retry-confirm]").disabled = busy;
        q("[data-retry-draft]").disabled = busy;
        q("[data-refresh]").disabled = busy || !!operations?.busy || !!recovery?.busy || uncertain();
        q("[data-confirm]").textContent = confirmAttempt.pending ? "검수 확인 저장 중…" : "검수 확인 저장";
        q("[data-create-draft]").textContent = draftAttempt.pending ? "초안 생성 중…" : "비활성 공고 초안 1건 생성";
        operations?.gates();
        recovery?.gates();
    };
    // 각각의 페이지/조회 응답은 소유 패널과 전체 기준 세대에 묶어 늦은 응답이 새 근거를 덮지 못하게 한다.
    const paged = (container, path, render, empty, size = 10, onLoaded = null) => {
        const expectedEpoch = epoch;
        const load = async number => {
            const token = Symbol(); panels.set(container, token); container.replaceChildren();
            text(container, "p", "근거를 불러오는 중입니다.").setAttribute("role", "status");
            const current = () => epoch === expectedEpoch && panels.get(container) === token;
            try {
                const data = await request(`${path}${path.includes("?") ? "&" : "?"}page=${number}&size=${size}`);
                if (!current()) return;
                container.replaceChildren();
                text(container, "p", `${data.totalCount}건 · ${data.totalPages ? data.page : 0}/${data.totalPages}페이지`).setAttribute("role", "status");
                if (!data.items.length) text(container, "p", empty);
                data.items.forEach(item => render(container, item));
                if (onLoaded) onLoaded(data);
                const nav = document.createElement("div"); nav.className = "attachment-actions"; container.append(nav);
                action(nav, "이전 페이지", () => load(number - 1), number <= 1);
                action(nav, "다음 페이지", () => load(number + 1), number >= data.totalPages);
            } catch (error) {
                if (!current()) return; container.replaceChildren();
                text(container, "p", error.message, "attachment-error").setAttribute("role", "alert");
                action(container, "이 근거 조회 재시도", () => load(number));
            }
        };
        return load(1);
    };
    const showBlocks = (file, match = null, blockPage = 1, offset = 0) => {
        const container = q("[data-blocks]"), token = Symbol(), expectedEpoch = epoch;
        panels.set(container, token); container.replaceChildren();
        text(container, "h3", `추출 문단 · ${file.displayName || file.fileId}`);
        text(container, "p", "고정 추출 이력을 조회합니다. 원본 파일을 내려받거나 외부 주소를 요청하지 않습니다.");
        const area = document.createElement("div"); container.append(area);
        text(area, "p", "텍스트를 조회 중입니다.").setAttribute("role", "status");
        request(`${root}/attachment-extractions/${encodeURIComponent(file.extractionId)}/blocks?page=${blockPage}&size=1&textOffset=${offset}&textLimit=2000`)
            .then(data => {
                if (epoch !== expectedEpoch || panels.get(container) !== token) return;
                area.replaceChildren();
                text(area, "p", `문단 ${data.totalPages ? data.page : 0}/${data.totalPages} · 한 번에 최대 2,000자`);
                const block = data.items[0];
                if (!block) { text(area, "p", "조회 가능한 추출 문단이 없습니다. 원문을 직접 확인하세요."); return; }
                if (match && (block.blockIndex !== match.blockIndex || file.extractionId !== match.extractionId)) {
                    text(area, "p", "일치 좌표와 문단 순서가 다릅니다. 자동 강조하지 않습니다. 문단과 좌표를 직접 비교하세요.", "attachment-error");
                    match = null;
                }
                if (match && offset === 0 && match.startOffset >= block.textEndOffset && match.startOffset < block.endOffset) {
                    showBlocks(file, match, blockPage, match.startOffset - block.startOffset); return;
                }
                meta(area, [["원문 위치", block.locator], ["문단 범위", `${block.startOffset}~${block.endOffset} (끝 제외)`],
                    ["현재 텍스트 범위", `${block.textStartOffset}~${block.textEndOffset}`], ["동일 문맥 확인", block.scopeReliable ? "신뢰 범위 확인됨" : "범위 불확실 · 직접 검수 필요"]]);
                const pre = document.createElement("pre");
                C.blockParts(block, match).forEach(part => pre.append(part.matched ? text(document.createDocumentFragment(), "mark", part.text) : document.createTextNode(part.text)));
                area.append(pre);
                const nav = document.createElement("div"); nav.className = "attachment-actions"; area.append(nav);
                action(nav, "이전 문단", () => showBlocks(file, null, blockPage - 1, 0), blockPage <= 1);
                action(nav, "다음 문단", () => showBlocks(file, null, blockPage + 1, 0), blockPage >= data.totalPages);
                action(nav, "문단 처음부터", () => showBlocks(file, null, blockPage, 0), offset === 0);
                action(nav, "이 문단의 다음 텍스트", () => showBlocks(file, null, blockPage, block.textEndOffset - block.startOffset), !block.hasMoreText);
            }).catch(error => {
                if (epoch !== expectedEpoch || panels.get(container) !== token) return;
                area.replaceChildren(); text(area, "p", error.message, "attachment-error").setAttribute("role", "alert");
                action(area, "텍스트 조회 재시도", () => showBlocks(file, match, blockPage, offset));
            });
    };
    const segments = window.SanebAttachmentSegments.createPanel({container: q("[data-segments]"), request, sourceId,
        readEpoch: () => epoch, text, meta, action, date, core: C,
        showBlocks: (file, match, blockPage) => { showBlocks(file, match, blockPage); q("[data-blocks]").focus(); }});
    const showFiles = (setId, title, evaluation = null) => {
        segments.reset();
        clear(q("[data-blocks]"));
        return paged(q("[data-files]"), `${root}/attachment-sets/${encodeURIComponent(setId)}/files`, (parent, file) => {
            const item = document.createElement("article"); item.className = "attachment-evidence-item"; parent.append(item);
            text(item, "h3", file.displayName || "이름 미확인 파일");
            meta(item, [["집합", title], ["파일 ID", file.fileId], ["형식", file.detectedTypeCode || "미확인"],
                ["문서 역할", C.label(file.documentRoleCode)], ["역할 출처", C.roleOrigin(file.roleOriginCode)], ["다운로드", C.label(file.downloadStatusCode)],
                ["다운로드 실패", file.downloadErrorCode ? C.label(file.downloadErrorCode) : "기록 없음"],
                ["추출 품질", C.label(file.qualityCode)], ["추출 실패", file.extractionErrorCode ? C.label(file.extractionErrorCode) : "기록 없음"],
                ["추출 글자 수", file.characterCount == null ? "미집계" : `${file.characterCount}자`], ["추출 시각", date(file.extractedAt)],
                ["이전 추출 재사용", file.reusedFromExtractionId || "재사용 기록 없음"]]);
            action(item, "이 파일의 추출 텍스트 확인", () => showBlocks(file), !file.extractionId);
            if (evaluation) action(item, "선택한 판정에 연결된 구간 근거 확인", () => segments.show(file, setId, evaluation), !file.extractionId);
            action(item, "독립 구간 분석 확인 (기존 1.0.0)", () => segments.show(file, setId), !file.extractionId);
            if (!file.extractionId) text(item, "p", "추출 이력이 없어 텍스트를 조회할 수 없습니다.");
            if (file.roleAssessment == null) {
                text(item, "p", "텍스트 역할 판정 근거가 없습니다. 기존 정책·수동 지정 또는 미완료 추출일 수 있으며, 역할 자동 판정 완료로 간주하지 않습니다.");
            } else if (!C.validRoleAssessment(file)) {
                text(item, "p", "역할 근거와 추출 이력이 일치하지 않습니다. 자동 강조하지 않습니다. 최신 근거를 다시 조회하고 담당자에게 확인하세요.", "attachment-error").setAttribute("role", "alert");
            } else {
                const a=file.roleAssessment;
                meta(item, [["텍스트 규칙 제안", C.label(a.roleCode)], ["역할 판정 사유", C.label(a.reasonCode)], ["역할 규칙 버전", a.ruleVersion]]);
                text(item, "p", file.roleOriginCode === "MANUAL"
                    ? "현재 역할은 관리자 지정값입니다. 아래 자동 제안은 변경 전 근거이며 관리자 지정값을 덮어쓰지 않습니다."
                    : "문서 역할 판정입니다. 공고의 지원대상 확정이나 관리자 최종 검증 완료를 의미하지 않습니다.");
                const details=document.createElement("details"); item.append(details); text(details, "summary", `역할 판정 근거 ${a.evidence.length}개와 지문 확인`);
                meta(details, [["역할 근거 추출 ID", file.roleExtractionId], ["규칙 지문", a.rulesHash], ["텍스트 지문", a.textHash], ["문단 지문", a.blocksHash]]);
                if (!a.evidence.length) text(details, "p", "확정할 수 있는 역할 구조 근거가 없습니다. 추출 품질과 판정 사유를 함께 확인하세요.");
                a.evidence.forEach(e => {
                    text(details, "p", `${C.label(e.ruleCode)} · 문단 ${e.blockIndex + 1} · ${e.startOffset}~${e.endOffset} (코드포인트·끝 제외)`);
                    action(details, `${C.label(e.ruleCode)}의 추출 텍스트 확인`, () => showBlocks(file, {...e, extractionId:file.roleExtractionId}, e.blockIndex + 1));
                });
            }
        }, `파일 기록이 없습니다. 집합 ${title}의 발견 상태를 함께 확인하세요.`);
    };
    const showSets = () => paged(q("[data-sets]"), `${root}/attachment-sets`, (parent, set) => {
        const item = document.createElement("article"); item.className = "attachment-evidence-item"; parent.append(item);
        const active = source?.effectiveClassification?.setId === set.setId;
        const preview = source?.previewClassification?.setId === set.setId;
        const usage = active ? "현재 적용 판정의 집합" : preview ? "미리보기 집합 · 적용 안 됨" : "별도 집합 · 현재 검수 기준 아님";
        text(item, "h3", `${usage} · ${date(set.createdAt)}`);
        meta(item, [["집합 ID", set.setId], ["집합 처리", C.label(set.setStatusCode)], ["첨부 발견", C.label(set.discoveryStatusCode)],
            ["발견 완료", set.discoveryComplete ? "완료" : "미완료"], ["파일 처리", `${set.processedCount}/${set.discoveredCount}개`],
            ["경고", listLabels(set.warningCodes)], ["집합 해시", set.manifestHash || "아직 확정되지 않음"]]);
        const classification = active ? source.effectiveClassification : preview ? source.previewClassification : null;
        action(item, "이 집합의 파일 확인", () => showFiles(set.setId, usage,
            classification?.decisionId ? {evaluationId: classification.decisionId} : null));
    }, "첨부 집합 기록이 없습니다. 첨부 없음으로 단정할 수 없습니다.");
    const showMatches = evaluation => {
        const container = q("[data-matches]");
        return paged(container, `${root}/attachment-classification/${encodeURIComponent(evaluation.evaluationId)}/matches`, (parent, match) => {
            const item = document.createElement("article"); item.className = "attachment-evidence-item"; parent.append(item);
            text(item, "h3", `일치 문구: ${match.termText}`);
            meta(item, [["판정 용도", C.label(evaluation.usageCode)], ["판정 ID", evaluation.evaluationId], ["파일 ID", match.fileId],
                ["추출 ID", match.extractionId], ["규칙", `${match.groupCode} / ${match.ruleCode}`],
                ["적용 동작", C.label(match.appliedActionCode)], ["문단 / 문자 좌표", `${match.blockIndex} / ${match.startOffset}~${match.endOffset} (코드포인트·끝 제외)`]]);
            action(item, "이 일치 위치의 텍스트 확인", () => showBlocks({fileId: match.fileId, extractionId: match.extractionId}, match, match.blockIndex + 1));
        }, "첨부 일치 근거가 없습니다. 제목·본문 기본 판정이나 발견·추출 실패 사유를 함께 확인하세요.");
    };
    const showHistory = () => paged(q("[data-history]"), `${root}/attachment-classification/history`, (parent, evaluation) => {
        const item = document.createElement("article"); item.className = "attachment-evidence-item"; parent.append(item);
        text(item, "h3", `${C.label(evaluation.usageCode)} · ${C.label(evaluation.semanticStatusCode)}`);
        meta(item, [["판정 시각", date(evaluation.evaluatedAt)], ["사유", C.label(evaluation.reasonCode)], ["판정 ID", evaluation.evaluationId],
            ["정책 ID", evaluation.policyId], ["규칙 ID", evaluation.ruleReleaseId], ["고정 입력 해시", evaluation.inputHash], ["경고", listLabels(evaluation.warningCodes)]]);
        action(item, "이 판정의 일치 근거 조회", () => showMatches(evaluation));
        action(item, "이 판정의 파일 집합 조회", () => showFiles(evaluation.setId, C.label(evaluation.usageCode), evaluation));
    }, "첨부 판정 이력이 없습니다. 처리 중 여부와 정책 적용 여부를 확인하세요.");
    const renderCurrent = content => {
        const node = q("[data-current-summary]"); clear(node); text(node, "h3", source.title);
        const a = source.attachmentSummary || {};
        text(node, "h3", C.label(source.processingFlow?.statusCode));
        text(node, "p", C.flowGuidance(source));
        text(node, "p", source.processingFlow?.isAutomaticAnalysisComplete === true
            ? "자동 분석 완료 · 관리자 최종 검증/공고 공개 여부와 별개"
            : "자동 분석 완료로 확인되지 않음 · 수동 확인으로 이 상태를 성공 처리하지 않음");
        meta(node, [["공고", `${source.publicCode} · ${C.label(source.providerCode)}`], ["원문 버전 / 첨부 버전", `${source.sourceVersion} / ${source.attachmentVersion}`],
            ["첨부 검수 정책", source.isAttachmentReviewRequired ? "현재 적용 · 자동 분석 후 최종 검증" : "미적용 · 미리보기로 기본 판정을 변경하지 않음"],
            ["작업", C.label(a.jobStatusCode)], ["실패 코드", a.errorCode ? C.label(a.errorCode) : "기록 없음"],
            ["접수 상태", a.intakeStatusCode ? C.label(a.intakeStatusCode) : "접수 기록 없음"], ["첨부 발견", C.label(a.discoveryStatusCode)],
            ["발견 완료", a.isDiscoveryComplete == null ? "미확인" : a.isDiscoveryComplete ? "완료" : "미완료"],
            ["파일 진행", a.totalCount == null ? "미집계" : `${a.processedCount ?? "미집계"}/${a.totalCount}개`],
            ["판정 최신성", a.isStale ? "이전 근거 · 다시 검수 필요" : "조회 시점 기준"], ["검수 확인", C.label(source.confirmationStatusCode)]]);
        [["1·2차 제목·본문 판정 이력 · 중간 근거", source.baseClassification], ["현재 적용 판정", source.effectiveClassification], ["첨부 미리보기 · 적용 안 됨", source.previewClassification]].forEach(([title, c]) => {
            text(node, "h3", title);
            if (!c) { text(node, "p", "판정 기록 없음"); return; }
            meta(node, [["판정", c.decisionId ? C.label(c.semanticStatusCode) : "종합 판정 미생성 · 상단 자동 처리 상태 확인"], ["사유", C.label(c.reasonCode)], ["판정 ID", c.decisionId || "아직 생성되지 않음"],
                ["지원대상", listLabels(c.targetCategoryCodes)], ["지원형태", listLabels(c.supportTypeCodes)]]);
        });
        if (source.confirmationStatusCode === "CURRENT") text(node, "p", "현재 분류 항목에는 저장된 관리자 확정 분류가 반영됩니다. 자동 판정 당시의 근거는 아래 분류 이력에서 확인하세요.");
        const body = q("[data-source-content]"); clear(body);
        const url = C.safeSourceUrl(content.sourceUrl);
        if (url) { const link = text(body, "a", "외부 원문 전체 확인 (새 창)", "secondary-action"); link.href = url; link.target = "_blank"; link.rel = "noopener noreferrer"; }
        else text(body, "p", "안전하게 열 수 있는 원문 주소가 없습니다. 담당자에게 원문 위치를 확인하세요.");
        text(body, "pre", content.bodyText || "본문이 확보되지 않았습니다. 첨부 분석 결과와 자동 처리 상태를 확인하세요.");
    };
    const renderReview = error => {
        const ready = C.matchesContext(source, context);
        message("[data-review-state]", error?.message || (!C.canRequestFinalReview(source) ? C.flowGuidance(source)
            : !ready ? "현재 요약과 검수 기준이 일치하지 않습니다. 최신 기준을 다시 조회하세요."
            : context.linkedAnnouncement ? "이미 공고에 연결된 원문입니다. 새 검수 확인과 중복 초안 생성은 사용할 수 없습니다."
                : "최신 검수 기준을 읽었습니다. 아래 분류와 필수 사유를 직접 확인하세요."));
        if (!ready) return;
        const ack = q("[data-acknowledgements]"); clear(ack);
        if (context.requiredAcknowledgementCodes.length) choices(ack, "acknowledgedErrorCodes", Object.fromEntries(context.requiredAcknowledgementCodes.map(code => [code, C.label(code)])));
        else text(ack, "p", "현재 별도 확인이 필요한 실패·검수 사유가 없습니다.");
        reviewForm.elements.confirmationAcknowledged.checked = false;
        draftForm.elements.draftAcknowledged.checked = false;
        const saved = context.confirmedClassification;
        if (!reviewDirty) {
            ["targetCategoryCodes", "supportTypeCodes"].forEach(name => reviewForm.querySelectorAll(`input[name="${name}"]`).forEach(input => {
                input.checked = (saved?.[name] || source.effectiveClassification?.[name] || []).includes(input.value);
            }));
        }
        const primary = draftForm.elements.primaryTargetCategoryCode, prior = primary.value; primary.replaceChildren();
        const blank = text(primary, "option", "대표 지원대상을 선택하세요"); blank.value = "";
        if (C.confirmedCurrent(context)) saved.targetCategoryCodes.forEach(code => { const option = text(primary, "option", C.label(code)); option.value = code; });
        primary.value = [...primary.options].some(option => option.value === prior) ? prior : "";
        message("[data-confirmed-summary]", context.linkedAnnouncement ? "이미 공고에 연결된 원문입니다. 추가 검수 확인이나 새 초안 생성은 필요하지 않습니다. 연결된 공고를 확인하세요." : C.confirmedCurrent(context)
            ? `저장된 확인 ${saved.confirmation.confirmationId} · ${date(saved.confirmation.confirmedAt)} · 대상: ${listLabels(saved.targetCategoryCodes)} · 형태: ${listLabels(saved.supportTypeCodes)}.${saved.binding?.restorationId ? ` 원복으로 이전 확인이 복구됐습니다(검수 당시 첨부 버전 ${saved.confirmation.attachmentVersion} → 현재 유효 버전 ${saved.binding.attachmentVersion}). 새로 검수한 기록은 아닙니다.` : ""} 수정 중인 검수 입력이 있으면 먼저 확인을 저장하세요.`
            : "현재 버전에 일치하는 저장된 확인이 없습니다. 위에서 검수 확인을 완료하세요.");
        if (context.linkedAnnouncement) message("[data-draft-result]", `연결된 공고: ${context.linkedAnnouncement.announcementCode}. 현재 승인·활성 상태는 공고 관리에서 확인하세요.`);
    };
    const refresh = async () => {
        if (busy || operations?.busy || recovery?.busy || uncertain()) return;
        const currentEpoch = ++epoch; busy = true; locked = true; mutationStale = false; context = null; gates();
        segments.reset();
        message("[data-page-error]", ""); message("[data-page-status]", "입력을 유지하고 현재 원문·검수 기준을 조회 중입니다.");
        try {
            const details = await request(root);
            let review = null, reviewError = null;
            if (C.canRequestFinalReview(details.source)) {
                try { review = await request(`${root}/attachment-classification/review-context`); } catch (error) { reviewError = error; }
            }
            if (currentEpoch !== epoch) return;
            source = details.source; context = review; locked = !C.matchesContext(source, context);
            if (!locked) { message("[data-review-error]", ""); message("[data-draft-error]", ""); }
            renderCurrent(details.content); renderReview(reviewError);
            await operations.load(source, !!context?.linkedAnnouncement);
            await recovery.load(source);
            ["[data-files]", "[data-blocks]", "[data-history]", "[data-matches]"].forEach(selector => clear(q(selector)));
            showSets();
            message("[data-page-status]", `조회 시각: ${date(new Date().toISOString())}. 외부 수집을 실행하지 않았습니다.`);
        } catch (error) {
            source = null; context = null; locked = true;
            await operations.load(null);
            await recovery.load(null);
            ["[data-current-summary]", "[data-source-content]", "[data-sets]", "[data-files]", "[data-blocks]", "[data-history]", "[data-matches]", "[data-acknowledgements]"].forEach(selector => clear(q(selector)));
            message("[data-page-error]", error.message, true);
            message("[data-review-state]", "원문 조회 실패로 검수·초안 생성을 잠갔습니다. 입력은 유지됩니다.");
            message("[data-confirmed-summary]", "최신 확인을 조회하지 못했습니다."); message("[data-page-status]", "조회 미완료");
        } finally { if (currentEpoch === epoch) { busy = false; gates(); } }
    };
    const submit = async (kind, original = null) => {
        if (busy || operations?.busy || operations?.uncertain || operations?.stale || recovery?.busy || recovery?.uncertain || recovery?.stale || !canManage) return;
        const confirming = kind === "confirm", form = confirming ? reviewForm : draftForm;
        const attempt = confirming ? confirmAttempt : draftAttempt;
        const errorSelector = confirming ? "[data-review-error]" : "[data-draft-error]";
        message(errorSelector, "");
        if (!original && (locked || !C.matchesContext(source, context) || context.linkedAnnouncement)) return;
        let payload = original;
        if (!payload) {
            if (!form.reportValidity()) return;
            if (confirming) {
                const targetCategoryCodes = selected("targetCategoryCodes"), supportTypeCodes = selected("supportTypeCodes"), acknowledgedErrorCodes = selected("acknowledgedErrorCodes");
                const note = form.elements.reviewNote.value.trim();
                const problem = !targetCategoryCodes.length || !supportTypeCodes.length ? "지원대상과 지원형태를 각각 1개 이상 선택하세요."
                    : !note ? "직접 확인한 내용과 검수 사유를 1~1000자로 입력하세요."
                    : context.requiredAcknowledgementCodes.some(code => !acknowledgedErrorCodes.includes(code)) ? "현재 실패·검수 사유를 각각 확인해야 저장할 수 있습니다."
                    : context.manualSourceCheckRequired && form.elements.reviewMethodCode.value !== "MANUAL_SOURCE_CHECK" ? "불완전한 첨부 근거가 있습니다. 원문 전체를 직접 확인하고 검수 방법을 변경하세요." : null;
                if (problem) { message(errorSelector, problem, true); return; }
                payload = {version: context.version, targetCategoryCodes, supportTypeCodes, acknowledgedErrorCodes,
                    reviewMethodCode: form.elements.reviewMethodCode.value, reviewNote: note};
            } else {
                if (reviewDirty || !C.confirmedCurrent(context)) { message(errorSelector, "수정한 검수 입력을 저장하고 최신 확인을 다시 조회하세요.", true); return; }
                payload = {version: context.version, expectedConfirmationId: context.confirmedClassification.confirmation.confirmationId,
                    primaryTargetCategoryCode: form.elements.primaryTargetCategoryCode.value, incomeJudgementCode: form.elements.incomeJudgementCode.value};
            }
        }
        try {
            const prepared = attempt.prepare(payload); busy = true; gates();
            const result = await request(`${root}/attachment-classification/${confirming ? "confirmations" : "announcements"}`, {
                method: "POST", body: prepared.body, headers: confirming ? {"Idempotency-Key": prepared.key} : {}});
            if (!/^[0-9a-f-]{36}$/i.test(confirming ? result.confirmationId : result.announcementId))
                throw new C.RequestError("저장 결과 식별자를 확인하지 못했습니다. 동일 요청으로 다시 확인하세요.");
            attempt.succeed(); reviewDirty = false; draftDirty = false; busy = false;
            message("[data-page-status]", confirming ? `검수 확인 응답: ${result.confirmationId}. 현재 연결을 다시 조회합니다.` : `비활성 초안 생성 확인: ${result.announcementCode}. 자동 활성화하지 않았습니다.`);
            if (!confirming) message("[data-draft-result]", `생성된 공고 초안: ${result.announcementCode}. 공고 관리에서 조건 입력과 후속 승인을 진행하세요.`);
            await refresh();
        } catch (error) {
            attempt.fail(error); if (!error.uncertain) { locked = true; mutationStale = true; }
            message(errorSelector, `${error.message}${error.uncertain ? " 아래 동일 요청 재시도 버튼을 사용하세요." : " 입력을 보존했습니다. 최신 기준을 조회한 뒤 다시 확인하세요."}`, true);
        } finally { busy = false; gates(); }
    };
    reviewForm.addEventListener("input", event => {
        reviewDirty = true;
        if (event.target.name !== "confirmationAcknowledged") reviewForm.elements.confirmationAcknowledged.checked = false;
        gates();
    });
    draftForm.addEventListener("input", event => {
        draftDirty = true;
        if (event.target.name !== "draftAcknowledged") draftForm.elements.draftAcknowledged.checked = false;
    });
    reviewForm.addEventListener("submit", event => { event.preventDefault(); submit("confirm"); });
    draftForm.addEventListener("submit", event => { event.preventDefault(); submit("draft"); });
    q("[data-retry-confirm]").addEventListener("click", () => submit("confirm", confirmAttempt.original));
    q("[data-retry-draft]").addEventListener("click", () => submit("draft", draftAttempt.original));
    q("[data-refresh]").addEventListener("click", refresh);
    q("[data-load-history]").addEventListener("click", () => { if (source) showHistory(); });
    operations = window.SanebAttachmentOperations.mount({page, C, request, apiRoot: root, canManage, text, meta, message,
        changed: gates, blocked: () => busy || mutationStale || confirmAttempt.uncertain || draftAttempt.uncertain || !!recovery?.busy || !!recovery?.uncertain || !!recovery?.stale, refresh});
    recovery = window.SanebAttachmentRecovery.mount({page, C, request, apiRoot: root, canRollback: page.dataset.canRollback === "true", text, meta, message, date,
        changed: gates, blocked: () => busy || mutationStale || confirmAttempt.uncertain || draftAttempt.uncertain || !!operations?.busy || !!operations?.uncertain || !!operations?.stale, refresh,
        navigation: {
            read: () => { const u = new URL(window.location.href); return {page: Number(u.searchParams.get("recoveryPage") || 1), jobId: u.searchParams.get("recoveryJob")}; },
            write: (number, jobId) => { const u = new URL(window.location.href); u.searchParams.set("recoveryPage", String(number));
                if (jobId) u.searchParams.set("recoveryJob", jobId); else u.searchParams.delete("recoveryJob");
                window.history.replaceState(window.history.state, "", u); }
        }});
    window.addEventListener("beforeunload", event => { if (reviewDirty || draftDirty || operations.dirty || operations.busy || recovery.dirty || recovery.busy || busy || uncertain()) { event.preventDefault(); event.returnValue = ""; } });
    refresh();
})();
