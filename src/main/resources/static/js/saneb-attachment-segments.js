/* 고정 추출 이력의 구간 근거 조회. 분석 실행·재분류·검수 확정 API는 호출하지 않는다. */
((root) => {
    "use strict";
    const roles = ["NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN"];
    const reasons = ["ROLE_TEXT_STRUCTURE_MATCHED", "STRUCTURE_UNCERTAIN", "ROLE_ANALYSIS_LIMIT", "MIXED_DOCUMENT_ROLES",
        "INITIAL_HEADING_REQUIRED", "ROLE_STRUCTURE_INCOMPLETE", "COMPLETE_TEXT_REQUIRED", "SEGMENT_ANALYSIS_LIMIT"];
    const rules = ["NOTICE_HEADING", "GUIDE_HEADING", "FORM_HEADING", "REFERENCE_HEADING", "TARGET_SECTION", "SUPPORT_SECTION",
        "APPLICATION_SECTION", "APPLICANT_FIELD", "SIGNATURE_FIELD", "QUESTION_ITEM", "ANSWER_ITEM"];
    const labels = {RESOLVED: "구간 역할 분석 완료 · 최종 검증 아님", SEGMENTS_RESOLVED: "모든 구간의 역할 근거 확인",
        SEGMENT_CONTEXT_REQUIRED: "역할 미확정 구간이 남아 있음", COMPLETE_TEXT_REQUIRED: "전체 텍스트 확보 필요",
        SEGMENT_ANALYSIS_LIMIT: "구간 분석 한도 초과 · 직접 확인 필요"};
    const id = v => typeof v === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
    const hash = v => typeof v === "string" && /^[0-9a-f]{64}$/.test(v);
    const versions = {"segment-role-1.0.0": "fb807a5fcf11c102badcc35cc4b60c6abe7fa36672e2aa431e3b5f2dc16bcdde",
        "segment-role-1.0.2": "2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82"};
    const integer = (v, min, max) => Number.isSafeInteger(v) && v >= min && v <= max;
    // 서버는 원문으로 분석을 재현한다. 여기서는 잘못 연결된 응답/좌표를 표시하지 않는 방어 검증만 수행한다.
    const valid = (data, sourceId, setId, file, bound = false) => {
        if (!data || !file || ![sourceId, setId, file.fileId, file.extractionId].every(id)
            || data.sourceId !== sourceId || data.setId !== setId || file.setId !== setId
            || data.fileId !== file.fileId || data.extractionId !== file.extractionId
            || !roles.includes(data.fileRoleCode) || !["UNKNOWN", "PROFILE", "MANUAL", "TEXT_RULE"].includes(data.fileRoleOriginCode)
            || data.applicationMode !== "SHADOW") return false;
        if (data.analysisState === "NOT_ANALYZED") return data.analysis === null && data.analysisId === null && data.analyzedAt === null;
        const a = data.analysis;
        if (data.analysisState !== "ANALYZED" || !id(data.analysisId) || typeof data.analyzedAt !== "string"
            || !Number.isFinite(Date.parse(data.analyzedAt)) || !a || !Object.hasOwn(versions, a.analysisVersion)
            || (!bound && a.analysisVersion !== "segment-role-1.0.0") || a.rulesHash !== versions[a.analysisVersion]
            || ![a.rulesHash, a.textHash, a.blocksHash].every(hash) || !integer(a.textLength, 1, 1000000)
            || file.characterCount !== a.textLength || !Array.isArray(a.segments) || !integer(a.segments.length, 1, 200)) return false;
        let end = 0;
        for (const [index, s] of a.segments.entries()) {
            if (!s || s.index !== index || s.startOffset !== end || !integer(s.endOffset, end + 1, a.textLength)
                || !roles.includes(s.roleCode) || !reasons.includes(s.reasonCode) || !Array.isArray(s.evidence) || s.evidence.length > 100
                || (s.roleCode !== "UNKNOWN" && (s.reasonCode !== "ROLE_TEXT_STRUCTURE_MATCHED" || s.evidence.length < 3 || s.evidence.length > 4))
                || (s.roleCode === "UNKNOWN" && s.reasonCode === "ROLE_TEXT_STRUCTURE_MATCHED")
                || !s.evidence.every(e => e && rules.includes(e.ruleCode) && integer(e.blockIndex, 0, 19999)
                    && integer(e.startOffset, s.startOffset, s.endOffset - 1) && integer(e.endOffset, e.startOffset + 1, s.endOffset))) return false;
            end = s.endOffset;
        }
        const resolved = a.segments.every(s => s.roleCode !== "UNKNOWN");
        return end === a.textLength && (resolved
            ? a.statusCode === "RESOLVED" && a.reasonCode === "SEGMENTS_RESOLVED" && file.qualityCode === "COMPLETE_TEXT"
            : a.statusCode === "REVIEW_REQUIRED" && ["SEGMENT_CONTEXT_REQUIRED", "COMPLETE_TEXT_REQUIRED", "STRUCTURE_UNCERTAIN", "SEGMENT_ANALYSIS_LIMIT"].includes(a.reasonCode));
    };
    const validBinding = (data, evaluationId, sourceId, setId, file) => !!data && id(evaluationId)
        && data.evaluationId === evaluationId && id(data.policyId) && typeof data.evaluationCurrent === "boolean"
        && roles.includes(data.evaluatedFileRoleCode) && data.segmentAnalysis?.analysisState === "ANALYZED"
        && valid(data.segmentAnalysis, sourceId, setId, file, true);
    const createPanel = ({container, request, sourceId, readEpoch, text, meta, action, showBlocks, date, core}) => {
        let generation = 0;
        const label = code => labels[code] || core.label(code);
        const reset = () => { generation++; container.replaceChildren(); container.setAttribute("aria-busy", "false"); };
        const show = async (file, setId, evaluation = null) => {
            reset(); const ownGeneration = generation, ownEpoch = readEpoch();
            const current = () => ownGeneration === generation && ownEpoch === readEpoch();
            text(container, "h3", `문서 구간 근거 · ${file.displayName || file.fileId}`);
            text(container, "p", "고정 추출 이력의 저장된 구간 분석을 조회합니다. 조회만으로 분석·재분류·검수 확정·공고 공개가 실행되지 않습니다.");
            text(container, "p", evaluation ? "선택한 판정의 입력에 연결된 구간 분석입니다. 현재 운영 적용·최종 검수 완료 여부는 상단 현재 판정에서 확인하세요."
                : "독립 분석 조회 · 기존 1.0.0 규칙의 결과입니다. 현재 판정에 사용된 근거라는 뜻이 아닙니다.");
            const body = text(container, "div", "");
            text(body, "p", "구간 근거를 조회 중입니다.").setAttribute("role", "status");
            container.setAttribute("aria-busy", "true"); container.focus();
            try {
                if (evaluation && !id(evaluation.evaluationId)) throw new Error("판정 ID를 확인할 수 없습니다. 최신 기준 또는 분류 이력을 다시 조회하세요.");
                const result = await request(`/api/v2/admin/announcement-sources/${encodeURIComponent(sourceId)}/attachment-extractions/${encodeURIComponent(file.extractionId)}/segment-analysis${evaluation ? `/evaluations/${encodeURIComponent(evaluation.evaluationId)}` : ""}`);
                if (!current()) return;
                if (evaluation ? !validBinding(result, evaluation.evaluationId, sourceId, setId, file) : !valid(result, sourceId, setId, file))
                    throw new Error("구간 근거의 판정·원문·집합·파일·추출 식별자 또는 좌표가 일치하지 않습니다. 최신 기준을 조회한 뒤 다시 확인하세요.");
                const data = evaluation ? result.segmentAnalysis : result;
                body.replaceChildren();
                if (evaluation) meta(body, [["선택한 판정 ID", result.evaluationId], ["판정 정책 ID", result.policyId],
                    ["판정 이력 상태", result.evaluationCurrent ? "조회 시점의 현재 이력 · 운영 적용 여부와 별개" : "과거 판정 이력 · 현재 검수 기준 아님"],
                    ["판정 당시 파일 역할", core.label(result.evaluatedFileRoleCode)]]);
                meta(body, [["파일 전체 역할", core.label(data.fileRoleCode)], ["파일 역할 출처", core.roleOrigin(data.fileRoleOriginCode)]]);
                if (data.analysisState === "NOT_ANALYZED") {
                    text(body, "p", "현재 분석 규칙에 맞는 저장된 구간 근거가 없습니다. 분석 미완료이며, 첨부 없음이나 정상 후보를 뜻하지 않습니다. 자동 처리 상태와 정책 버전을 확인하세요.").setAttribute("role", "status");
                    return;
                }
                const a = data.analysis;
                text(body, "p", label(a.statusCode)).setAttribute("role", "status");
                text(body, "p", "구간 역할은 파일 전체 역할을 덮어쓰지 않습니다. 종합 분류 적용 여부는 현재 적용 판정에서 별도로 확인하세요. 신청 양식·참고자료는 문맥 보조이며 지원대상 확정 근거로 단독 사용하지 않습니다.");
                if (["MANUAL", "PROFILE"].includes(data.fileRoleOriginCode) && a.segments.some(s => s.roleCode !== data.fileRoleCode))
                    text(body, "p", "수동·시스템 지정 파일 역할과 구간 제안이 다릅니다. 파일 역할은 유지하며, 종합 판정의 역할 충돌 사유를 확인하세요.", "attachment-error");
                meta(body, [["분석 사유", label(a.reasonCode)], ["분석 시각", date(data.analyzedAt)], ["구간 수", `${a.segments.length}개`]]);
                const fingerprints = text(body, "details", ""); text(fingerprints, "summary", "분석 버전·식별자·지문 확인");
                meta(fingerprints, [["분석 ID", data.analysisId], ["추출 ID", data.extractionId], ["집합 ID", data.setId],
                    ["분석 버전", a.analysisVersion], ["규칙 지문", a.rulesHash], ["텍스트 지문", a.textHash], ["문단 지문", a.blocksHash]]);
                for (const s of a.segments) {
                    const item = text(body, "details", "", "attachment-evidence-item");
                    text(item, "summary", `${s.index + 1}번 구간 · ${core.label(s.roleCode)} · ${s.startOffset}~${s.endOffset}`);
                    meta(item, [["구간 판정 사유", label(s.reasonCode)], ["원문 좌표", `${s.startOffset}~${s.endOffset} (유니코드 문자 기준 · 끝 제외)`]]);
                    if (!s.evidence.length) text(item, "p", "확정할 수 있는 구간 역할 근거가 없습니다. 전체 추출 텍스트와 분석 사유를 확인하세요.");
                    let expanded = false;
                    item.addEventListener("toggle", () => {
                        if (!item.open || expanded || !current()) return;
                        expanded = true;
                        for (const e of s.evidence) action(item, `${core.label(e.ruleCode)} · 문단 ${e.blockIndex + 1} · ${e.startOffset}~${e.endOffset} 확인`,
                            () => { if (current()) showBlocks(file, {...e, extractionId: data.extractionId}, e.blockIndex + 1); });
                    });
                }
            } catch (error) {
                if (!current()) return;
                body.replaceChildren(); text(body, "p", error.message, "attachment-error").setAttribute("role", "alert");
                action(body, "구간 근거 조회 재시도", () => show(file, setId, evaluation));
            } finally { if (current()) container.setAttribute("aria-busy", "false"); }
        };
        return {show, reset};
    };
    const api = {valid, validBinding, createPanel};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.SanebAttachmentSegments = api;
})(globalThis);
