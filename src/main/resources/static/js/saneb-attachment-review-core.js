/* 첨부 검수의 순수 계약/통신 상태. 외부 원문을 실행하거나 브라우저 저장소에 보관하지 않는다. */
((root) => {
    "use strict";
    const targets = {BUSINESS: "사업자", PERSONAL: "본인(개인)", SPOUSE: "배우자", CHILD: "자녀", PARENT: "부모님"};
    const supports = {GENERAL_SUPPORT: "일반 지원", GRANT_SUBSIDY: "지원금·보조금", POLICY_FINANCE: "정책자금·융자",
        GUARANTEE: "보증", INTEREST_SUPPORT: "이차보전·이자지원", VOUCHER_BENEFIT: "바우처·혜택", REFUND_REDUCTION: "환급·감면"};
    const labels = {...targets, ...supports, ACCEPTED: "유효 후보 · 최종 선정 아님", REVIEW_REQUIRED: "관리자 검수 필요",
        BIZINFO: "기업마당", GOV24_PUBLIC_SERVICE: "정부24", LOCAL_GOV_NOTICE: "지자체",
        CURRENT: "현재 확인", STALE: "이전 확인 · 재검수 필요", NONE: "확인 없음", QUEUED: "작업 예약됨",
        PROTECTED_LINK: "기존 공고 연결 보호", BASE_RECLASSIFICATION_REQUIRED: "기본 판정 갱신 필요", POLICY_BINDING_CHANGED: "적용 정책 변경",
        RECHECK_NOT_DUE: "재확인 주기 미도래", OFF: "새 수집 중지", FROZEN: "실행 정책 고정", NO_POLICY: "정책 없음",
        NOT_REQUESTED: "요청 없음", SCOPE_READY: "실행 승인 대기", PAUSED: "일시정지", PENDING: "대기", RUNNING: "처리 중", RETRY_WAIT: "재시도 대기",
        COLLECT: "전체 첨부 수집", RETRY_FILES: "실패 파일 재시도", ROLE_CHANGE: "문서 역할 변경", APPLIED: "판정 적용됨", ROLLED_BACK: "이전 연결 복구됨",
        SUCCEEDED: "처리 완료", PARTIAL_FAILED: "일부 실패", FAILED: "실패", CANCELLED: "취소", CONFLICT: "버전 충돌",
        FOUND: "첨부 발견", NO_FILES: "첨부 없음 확인", DISCOVERY_FAILED: "첨부 발견 실패", OPEN: "집합 처리 중", SEALED: "집합 처리 종료",
        ATTACHMENT_DETAIL_UNAVAILABLE: "공식 상세 내용 확인 실패", ATTACHMENT_SELECTOR_CHANGED: "첨부 영역 구조 변경 · 원문 확인 필요",
        ATTACHMENT_DOWNLOAD_FORM_CHANGED: "다운로드 폼 변경 · 시스템 수집 방식 재검증 필요",
        ATTACHMENT_LINK_UNRESOLVED: "첨부 링크 확인 실패 · 원문 확인 필요", ATTACHMENT_FILE_LIMIT: "공고별 첨부 파일 수 한도 초과",
        NOTICE: "공고문", GUIDE: "안내문", FORM: "신청 양식 · 문맥 보조", REFERENCE: "참고자료 · 문맥 보조", UNKNOWN: "역할 미확정",
        ROLE_TEXT_STRUCTURE_MATCHED: "문서 제목과 필수 본문 구조 확인", STRUCTURE_UNCERTAIN: "텍스트 문맥 범위 불확실 · 역할 미확정",
        ROLE_ANALYSIS_LIMIT: "역할 분석 한도 초과 · 역할 미확정", MIXED_DOCUMENT_ROLES: "서로 다른 문서 역할 혼재 · 역할 미확정",
        INITIAL_HEADING_REQUIRED: "문서 앞부분의 역할 제목 미확인", ROLE_STRUCTURE_INCOMPLETE: "역할 판정에 필요한 본문 구조 부족",
        NOTICE_HEADING: "공고문 제목", GUIDE_HEADING: "안내문 제목", FORM_HEADING: "신청 양식 제목", REFERENCE_HEADING: "참고자료 제목",
        TARGET_SECTION: "지원대상 항목", SUPPORT_SECTION: "지원내용 항목", APPLICATION_SECTION: "신청 항목",
        APPLICANT_FIELD: "신청인 입력란", SIGNATURE_FIELD: "서명란", QUESTION_ITEM: "질문 항목", ANSWER_ITEM: "답변 항목",
        COMPLETE_TEXT: "텍스트 추출 완료", PARTIAL_TEXT: "일부 텍스트", OCR_REQUIRED: "스캔 문서 · 직접 확인 필요",
        NO_TEXT: "추출 텍스트 없음", ENCRYPTED: "암호화 문서", UNSUPPORTED_FORMAT: "지원하지 않는 형식",
        CURRENT_EFFECTIVE: "현재 적용 판정", CURRENT_PREVIEW: "현재 미리보기 · 적용 안 됨", NOT_CURRENT: "과거 판정 · 검수 기준 아님",
        EXTRACTED_TEXT: "추출 텍스트 검수", MANUAL_SOURCE_CHECK: "원문 전체 직접 확인",
        TITLE_GROUP_A_MATCHED: "제목의 관리자 검수 문구", BODY_GROUP_A_MATCHED: "본문의 관리자 검수 문구",
        BODY_GROUP_B_MATCHED: "본문의 제외 검토 문구", ATTACHMENT_GROUP_A_MATCHED: "첨부의 관리자 검수 문구",
        ATTACHMENT_GROUP_B_MATCHED: "첨부의 제외 검토 문구", BODY_UNAVAILABLE: "본문 미수집", BODY_FETCH_FAILED: "본문 수집 실패",
        BODY_COMBINATION_NOT_CONFIRMED: "본문의 대상·형태 조합 미확인", TARGET_SUPPORT_CONFIRMED: "제목·본문의 대상·형태 확인",
        TARGET_SUPPORT_MATCH: "대상·형태 조합 확인", ATTACHMENT_TARGET_SUPPORT_CONFIRMED: "첨부의 대상·형태 확인",
        ATTACHMENT_INCOMPLETE: "첨부 근거 불완전", ATTACHMENT_DISCOVERY_INCOMPLETE: "첨부 발견 미완료",
        ATTACHMENT_PENDING: "첨부 처리 대기", ATTACHMENT_TEXT_INCOMPLETE: "첨부 텍스트 불완전", ATTACHMENT_CLASSIFICATION_LIMIT: "첨부 분류 처리 한도 초과",
        ATTACHMENT_SCOPE_UNCERTAIN: "동일 문맥 범위 불확실", ATTACHMENT_NEGATIVE_CONTEXT: "부정·제외 문맥 검수 필요", ATTACHMENT_SCOPE_MISSING: "문단 근거 없음",
        ATTACHMENT_CONTEXT_REVIEW: "첨부 문맥 직접 검수 필요", EXTENDED_TARGET_SUPPORT_CONFIRMED: "본문·첨부의 대상·형태 확인",
        EXTENDED_COMBINATION_NOT_CONFIRMED: "본문·첨부의 대상·형태 조합 미확인",
        TAG: "분류 태그 부여", CONTEXT_ONLY: "문맥 참고만 사용", REVIEW_REQUIRED_ACTION: "검수 필요", EXCLUDED_ACTION: "제외 검토", MASK_ONLY: "보호 구간",
        ATTACHMENT_FILE_SET_INCOMPLETE: "파일 집합 미완료", ATTACHMENT_DOWNLOAD_INCOMPLETE: "파일 다운로드 미완료",
        ATTACHMENT_TEXT_NOT_EXTRACTED: "텍스트 추출 미완료", ATTACHMENT_ROLE_UNKNOWN: "문서 역할 미확정",
        NETWORK_TIMEOUT: "외부 요청 시간 초과", NETWORK_UNAVAILABLE: "외부 연결 실패", HTTP_RATE_LIMITED: "외부 요청량 제한",
        HTTP_SERVER_ERROR: "외부 서버 오류", PROFILE_REQUIRED: "시스템 수집 방식 미지원", DOWNLOAD_BLOCKED: "다운로드 차단",
        LIMIT_EXCEEDED: "처리 한도 초과", EXTRACTION_FAILED: "텍스트 추출 실패", ISOLATION_UNAVAILABLE: "격리 실행 환경 사용 불가",
        DISCOVERY_CHANGED: "발견한 첨부 목록 변경", WORKER_PROCESSING_FAILED: "처리 작업 실패"};
    const flowMessages = {
        NOT_APPLIED: ["3단계 종합 판정 미적용", "첨부 미리보기는 기본 판정을 변경하지 않습니다. 자동 분석 완료나 최종 검증 완료가 아닙니다."],
        CLASSIFICATION_PENDING: ["제목·본문 자동 판정 대기", "기본 판정이 아직 없습니다. 최종 검수 입력 없이 처리 상태를 확인하세요."],
        CONFIGURATION_REQUIRED: ["자동 분석 설정 보완 필요", "수집 방식·규칙·정책 결합을 시스템 담당자가 확인해야 합니다. 공고 내용 검수로 해결할 수 없습니다."],
        AUTOMATIC_PROCESSING: ["자동 분석 대기·진행 중", "제목·본문·첨부 근거를 처리 중이거나 접수 대기 중입니다. 최종 검수 입력은 아직 필요하지 않습니다."],
        EVIDENCE_STALE: ["최신 근거 확보 필요", "이전 근거는 현재 입력과 일치하지 않습니다. 최신 작업 결과를 조회한 뒤 최종 검증하세요."],
        TECHNICAL_EXCEPTION: ["자동 분석 미완료 · 기술 예외", "발견·추출·처리 실패 또는 근거 부족을 확인하세요. 수동 원문 확인은 자동 분석 성공을 뜻하지 않습니다."],
        READY_FOR_FINAL_REVIEW: ["최종 관리자 검증 대기", "자동 분석한 분류와 근거를 최종 확인하세요. 유효 후보는 최종 선정이 아니며 자동 공개되지 않습니다."],
        FINAL_REVIEW_EXCEPTION: ["최종 관리자 검증 · 쟁점 확인 필요", "자동 분석 후 남은 A/B 문구·문서 역할·문맥 쟁점을 확인하세요. 중간 판정 단계의 검수 요청은 아닙니다."],
        FINAL_REVIEW_CONFIRMED: ["현재 근거의 관리자 확인 완료", "현재 버전의 확인이 저장되어 있습니다. 자동 분석의 완전성 및 공고 초안·공개 상태와는 별개입니다."]
    };
    const label = code => code == null || code === "" ? "미확인" : labels[code] || flowMessages[code]?.[0] || `확인 필요 (${code})`;
    const flowGuidance = source => flowMessages[source?.processingFlow?.statusCode]?.[1]
        || "자동 처리 상태를 확인하지 못했습니다. 최신 기준을 다시 조회하세요. 검수 저장은 잠겨 있습니다.";
    const canRequestFinalReview = source => {
        const flow = source?.processingFlow;
        return !!(source?.isAttachmentReviewRequired && flow?.isFinalReviewAvailable === true
            && ["TECHNICAL_EXCEPTION", "READY_FOR_FINAL_REVIEW", "FINAL_REVIEW_EXCEPTION", "FINAL_REVIEW_CONFIRMED"].includes(flow.statusCode)
            && !["SCOPE_READY", "PENDING", "RUNNING", "RETRY_WAIT", "PAUSED"].includes(source.attachmentSummary?.jobStatusCode));
    };
    const versionKeys = ["expectedBaseDecisionId", "expectedAttachmentDecisionId", "expectedSourceVersion", "expectedAttachmentVersion", "expectedSetHash"];
    const matchesContext = (source, context) => {
        const v = context?.version, c = source?.effectiveClassification;
        return !!(canRequestFinalReview(source) && v && c && !source.attachmentSummary?.isStale
            && source.sourceId === context.sourceId && v.expectedBaseDecisionId
            && v.expectedAttachmentDecisionId && /^[0-9a-f]{64}$/.test(v.expectedSetHash)
            && source.baseClassification?.decisionId === v.expectedBaseDecisionId && c.decisionId === v.expectedAttachmentDecisionId
            && c.setHash === v.expectedSetHash && source.sourceVersion === v.expectedSourceVersion
            && source.attachmentVersion === v.expectedAttachmentVersion);
    };
    const sameVersion = (a, b) => !!(a && b && versionKeys.every(key => a[key] === b[key]));
    const confirmedCurrent = (context) => {
        const saved = context?.confirmedClassification, c = saved?.confirmation, v = context?.version;
        const hasBinding = !!saved && Object.hasOwn(saved, "binding"), b = hasBinding ? saved.binding : c;
        const validBinding = !hasBinding || (!!b && b.confirmationId === c?.confirmationId && b.sourceId === c?.sourceId
            && Number.isInteger(b.sourceVersion) && Number.isInteger(b.attachmentVersion)
            && (b.restorationId === null ? b.sourceVersion === c.sourceVersion && b.attachmentVersion === c.attachmentVersion
                : typeof b.restorationId === "string" && /^[0-9a-f-]{36}$/i.test(b.restorationId)
                    && b.sourceVersion === c.sourceVersion && b.attachmentVersion > c.attachmentVersion));
        return !!(v && c?.isCurrent && c.sourceId === context.sourceId && c.evaluationId === v.expectedAttachmentDecisionId
            && validBinding && b?.sourceVersion === v.expectedSourceVersion && b.attachmentVersion === v.expectedAttachmentVersion
            && c.setHash === v.expectedSetHash && saved.targetCategoryCodes?.length && saved.supportTypeCodes?.length);
    };
    const safeSourceUrl = value => {
        try {
            const u = new URL(value);
            return ["https:", "http:"].includes(u.protocol) && !u.username && !u.password ? u.href : null;
        } catch { return null; }
    };
    const blockParts = (block, match) => {
        const chars = Array.from(block.text || "");
        if (!match || block.blockIndex !== match.blockIndex || !Number.isInteger(match.startOffset)
            || !Number.isInteger(match.endOffset) || match.startOffset < block.textStartOffset
            || match.endOffset > block.textEndOffset || match.endOffset <= match.startOffset
            || chars.length !== block.textEndOffset - block.textStartOffset) return [{text: chars.join(""), matched: false}];
        const start = match.startOffset - block.textStartOffset, end = match.endOffset - block.textStartOffset;
        return [{text: chars.slice(0, start).join(""), matched: false}, {text: chars.slice(start, end).join(""), matched: true},
            {text: chars.slice(end).join(""), matched: false}];
    };
    class RequestError extends Error {
        constructor(message, status = 0, code = null) { super(message); this.status = status; this.code = code; this.uncertain = status === 0 || status >= 500; }
    }
    const client = (fetcher, timeoutMs = 20000) => async (url, options = {}) => {
        if (!/^\/api\/v2\/admin\/announcement-sources\/[0-9a-f-]{36}(?:\/|$)/i.test(url))
            throw new RequestError("허용되지 않은 요청 경로입니다. 검수 목록에서 다시 진입하세요.", 400);
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        try {
            const response = await fetcher(url, {...options, credentials: "same-origin", cache: "no-store", redirect: "error",
                signal: controller.signal, headers: {"Accept": "application/json", ...(options.body ? {"Content-Type": "application/json"} : {}), ...options.headers}});
            let body;
            try { body = await response.json(); } catch { throw new RequestError("서버 응답을 확인할 수 없습니다. 입력을 유지하고 같은 요청을 다시 확인하세요.", response.ok ? 0 : response.status); }
            if (!body || typeof body !== "object" || (response.ok && body.success === true && (body.data == null || typeof body.data !== "object")))
                throw new RequestError("서버 응답 형식이 일치하지 않습니다. 처리 결과는 미확정이며 동일 요청을 다시 확인해야 합니다.");
            if (!response.ok || body.success !== true) {
                const message = response.status === 401 ? "로그인이 만료됐습니다. 이 탭을 유지하고 다른 탭에서 로그인한 뒤 다시 시도하세요."
                    : response.status === 403 ? "접근 권한 또는 보안 확인이 유효하지 않습니다. 계정 권한과 로그인 상태를 확인하세요."
                    : response.status >= 500 ? "서버 처리 결과를 확인하지 못했습니다. 입력을 바꾸지 말고 동일 요청을 재시도하세요."
                    : body.message || "현재 요청을 처리할 수 없습니다. 최신 기준을 다시 확인하세요.";
                throw new RequestError(message, response.status, body.data?.code);
            }
            return body.data;
        } catch (error) {
            if (error instanceof RequestError) throw error;
            throw new RequestError("연결이 끊겼거나 응답 시간이 초과됐습니다. 변경 요청의 결과는 미확정입니다. 동일 요청으로 재시도하세요.");
        } finally { clearTimeout(timer); }
    };
    // 결과 유실 중에는 다른 payload를 전송하지 않는다. 원문/키는 탭 메모리에서만 유지한다.
    const mutation = (uuid) => {
        let attempt = null, uncertain = false, pending = false;
        return {
            prepare(payload) {
                if (pending) throw new RequestError("이미 처리 중입니다. 응답을 기다려 주세요.", 409);
                const body = JSON.stringify(payload);
                if (uncertain && body !== attempt.body) throw new RequestError("이전 요청의 결과가 미확정입니다. 원래 요청을 먼저 재시도하세요.", 409);
                if (!attempt || attempt.body !== body) attempt = {body, key: uuid()};
                pending = true; return {...attempt};
            },
            succeed() { pending = false; uncertain = false; attempt = null; },
            fail(error) { pending = false; uncertain = !!error.uncertain; if (!uncertain) attempt = null; },
            get pending() { return pending; }, get uncertain() { return uncertain; },
            get original() { return attempt && JSON.parse(attempt.body); }
        };
    };
    // 자동 역할은 최종 승인과 다르다. 관리자 변경 뒤에도 원래 자동 제안과 고정 추출 좌표를 보존한다.
    const roleOrigin = value => ({UNKNOWN:"역할 근거 없음", PROFILE:"시스템 수집 방식 지정", MANUAL:"관리자 지정", TEXT_RULE:"추출 텍스트 규칙 판정"}[value] || "역할 출처 확인 필요");
    const validRoleAssessment = file => {
        const a=file?.roleAssessment, hash=v=>typeof v==="string"&&/^[0-9a-f]{64}$/.test(v);
        const id=v=>typeof v==="string"&&/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
        const rules=["NOTICE_HEADING","GUIDE_HEADING","FORM_HEADING","REFERENCE_HEADING","TARGET_SECTION","SUPPORT_SECTION","APPLICATION_SECTION","APPLICANT_FIELD","SIGNATURE_FIELD","QUESTION_ITEM","ANSWER_ITEM"];
        const reasons=["STRUCTURE_UNCERTAIN","ROLE_ANALYSIS_LIMIT","MIXED_DOCUMENT_ROLES","INITIAL_HEADING_REQUIRED","ROLE_STRUCTURE_INCOMPLETE"];
        return !!(a&&["TEXT_RULE","MANUAL"].includes(file.roleOriginCode)&&id(file.roleExtractionId)&&file.roleExtractionId===file.extractionId
            &&file.qualityCode==="COMPLETE_TEXT"&&typeof a.ruleVersion==="string"&&/^[A-Za-z0-9_.-]{1,40}$/.test(a.ruleVersion)
            &&[a.rulesHash,a.textHash,a.blocksHash].every(hash)&&["NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN"].includes(a.roleCode)
            &&(file.roleOriginCode==="MANUAL"||file.documentRoleCode===a.roleCode)&&Array.isArray(a.evidence)&&a.evidence.length<=100
            &&(a.roleCode==="UNKNOWN"?reasons.includes(a.reasonCode):a.reasonCode==="ROLE_TEXT_STRUCTURE_MATCHED"&&a.evidence.length>=3&&a.evidence.length<=4)
            &&a.evidence.every(e=>e&&rules.includes(e.ruleCode)&&Number.isSafeInteger(e.blockIndex)&&e.blockIndex>=0&&e.blockIndex<20000
                &&Number.isSafeInteger(e.startOffset)&&Number.isSafeInteger(e.endOffset)&&e.startOffset>=0&&e.endOffset>e.startOffset&&e.endOffset<=1000000));
    };
    const api = {targets, supports, label, roleOrigin, validRoleAssessment, flowGuidance, canRequestFinalReview, matchesContext, confirmedCurrent, sameVersion, safeSourceUrl, blockParts, RequestError, client, mutation};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.SanebAttachmentReview = api;
})(globalThis);
