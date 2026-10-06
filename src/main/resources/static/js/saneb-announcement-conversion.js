(() => {
    'use strict';
    const page = document.querySelector('[data-conversion-review]');
    if (!page) return;
    const C = window.SanebAttachmentReview, q = selector => page.querySelector(selector);
    const request = C.client(window.fetch.bind(window));
    const root = `/api/v2/admin/announcement-sources/${encodeURIComponent(page.dataset.sourceId)}`;
    const form = q('[data-form]'), fields = form.elements;
    const confirmation = C.mutation(), draft = C.mutation();
    let source = null, context = null, busy = false, dirty = false, locked = true, epoch = 0, chain = null;
    const text = (parent, tag, value, className) => {
        const el = document.createElement(tag); el.textContent = value || '';
        if (className) el.className = className; parent.append(el); return el;
    };
    const message = (selector, value, focus = false) => {
        const node = q(selector); node.textContent = value || ''; node.hidden = !value;
        if (value && focus) node.focus();
    };
    const checked = name => [...form.querySelectorAll(`input[name="${name}"]:checked`)].map(el => el.value);
    const choices = (selector, name, values) => {
        const container = q(selector); container.replaceChildren();
        Object.entries(values).forEach(([value, label]) => {
            const wrapper = text(container, 'label', '', 'attachment-check'), input = document.createElement('input');
            input.type = 'checkbox'; input.name = name; input.value = value;
            wrapper.append(input, document.createTextNode(label));
        });
    };
    choices('[data-targets]', 'targetCategoryCodes', C.targets);
    choices('[data-supports]', 'supportTypeCodes', C.supports);
    const uncertain = () => confirmation.uncertain || draft.uncertain;
    const saved = () => C.confirmedCurrent(context) && !dirty;
    const gates = () => {
        const ready = !locked && C.matchesContext(source, context) && !context?.linkedAnnouncement;
        q('[data-fields]').disabled = busy || uncertain() || !ready || page.dataset.canManage !== 'true';
        q('[data-convert]').disabled = q('[data-fields]').disabled;
        q('[data-convert]').textContent = busy ? '처리 중…' : saved() ? '저장된 검수로 공고 초안 만들기' : '검수 확인 후 공고 초안 만들기';
        q('[data-refresh]').disabled = busy || uncertain();
        q('[data-retry]').hidden = !uncertain(); q('[data-retry]').disabled = busy;
        // 저장된 검수로 재시도할 때는 확인된 기록을 다시 쓰지 않는다.
        q('[data-review-inputs]').hidden = saved();
        fields.reviewNote.required = !saved(); fields.reviewMethodCode.required = !saved();
    };
    const primaryOptions = () => {
        const prior = fields.primaryTargetCategoryCode.value;
        fields.primaryTargetCategoryCode.replaceChildren();
        const blank = text(fields.primaryTargetCategoryCode, 'option', '대표 지원대상을 선택하세요'); blank.value = '';
        checked('targetCategoryCodes').forEach(code => { const option = text(fields.primaryTargetCategoryCode, 'option', C.label(code)); option.value = code; });
        fields.primaryTargetCategoryCode.value = checked('targetCategoryCodes').includes(prior) ? prior : '';
    };
    const action = (parent, label, callback, disabled = false) => {
        const button = text(parent, 'button', label, 'secondary-action'); button.type = 'button'; button.disabled = disabled;
        button.addEventListener('click', callback);
    };
    const loadBlocks = async (file, number = 1, offset = 0) => {
        const expected = epoch, node = q('[data-blocks]'); node.replaceChildren();
        const token = {}; node.requestToken = token;
        text(node, 'p', '첨부 내용을 불러오는 중입니다.');
        try {
            const data = await request(`${root}/attachment-extractions/${encodeURIComponent(file.extractionId)}/blocks?page=${number}&size=1&textOffset=${offset}&textLimit=2000`);
            if (expected !== epoch || node.requestToken !== token) return;
            node.replaceChildren(); text(node, 'h3', file.displayName || '첨부 내용');
            const block = data.items[0];
            if (!block) { text(node, 'p', '추출한 내용이 없습니다. 외부 원문을 확인하세요.'); return; }
            text(node, 'p', `문단 ${data.page}/${data.totalPages}`, 'attachment-muted'); text(node, 'pre', block.text);
            action(node, '이전 문단', () => loadBlocks(file, number - 1), number <= 1);
            action(node, '다음 문단', () => loadBlocks(file, number + 1), number >= data.totalPages);
            action(node, '문단 처음', () => loadBlocks(file, number), offset === 0);
            action(node, '다음 내용', () => loadBlocks(file, number, block.textEndOffset - block.startOffset), !block.hasMoreText);
            node.focus();
        } catch (error) { if (expected === epoch && node.requestToken === token) { node.replaceChildren(); text(node, 'p', error.message, 'attachment-error'); action(node, '내용 다시 조회', () => loadBlocks(file, number, offset)); } }
    };
    const loadFiles = async (setId, number = 1) => {
        const expected = epoch, node = q('[data-files]'), token = {}; node.requestToken = token;
        node.replaceChildren(); text(node, 'p', '첨부 목록을 불러오는 중입니다.');
        try {
            const data = await request(`${root}/attachment-sets/${encodeURIComponent(setId)}/files?page=${number}&size=10`);
            if (expected !== epoch || node.requestToken !== token) return;
            node.replaceChildren(); text(node, 'p', `첨부 ${data.totalCount}개`, 'attachment-muted');
            if (!data.items.length) text(node, 'p', '이 자료 집합에 파일 기록이 없습니다. 첨부 발견 완료 여부와는 별개입니다.');
            data.items.forEach(file => {
                const row = text(node, 'div', '', 'conversion-file'); text(row, 'span', file.displayName || '첨부파일');
                action(row, '내용 보기', () => loadBlocks(file), !file.extractionId);
                if (!file.extractionId || file.qualityCode !== 'COMPLETE_TEXT' || file.downloadErrorCode || file.extractionErrorCode) text(row, 'p', '전체 내용을 확보하지 못했습니다. 외부 원문 확인이 필요합니다.', 'attachment-muted');
            });
            if (data.totalPages > 1) { action(node, '이전 첨부', () => loadFiles(setId, number - 1), number <= 1); action(node, '다음 첨부', () => loadFiles(setId, number + 1), number >= data.totalPages); }
        } catch (error) { if (expected === epoch && node.requestToken === token) { node.replaceChildren(); text(node, 'p', error.message, 'attachment-error'); action(node, '첨부 다시 조회', () => loadFiles(setId, number)); } }
    };
    const loadLatestFiles = async () => {
        const expected = epoch;
        try {
            // 판정이 아직 없어도 수집된 최신 자료는 열람한다. 과거 이력 목록은 노출하지 않는다.
            const data = await request(`${root}/attachment-sets?page=1&size=1`);
            if (expected !== epoch) return;
            const latest = data.items[0];
            if (!latest) return;
            message('[data-evidence]', latest.discoveryComplete && latest.discoveryStatusCode === 'NO_FILES'
                ? '최신 수집에서 첨부 없음이 확인되었습니다.' : '최근 수집한 자료입니다. 현재 판정에 사용된 근거와는 구분됩니다.');
            await loadFiles(latest.setId);
        } catch (error) {
            if (expected !== epoch) return;
            const node = q('[data-files]'); node.replaceChildren();
            text(node, 'p', '첨부 조회를 완료하지 못했습니다. 첨부 없음으로 판단하지 마세요.', 'attachment-error');
            action(node, '첨부 다시 조회', loadLatestFiles);
        }
    };
    const render = details => {
        message('[data-title]', source.title); message('[data-source-label]', `${source.publicCode} · ${C.label(source.providerCode)}`);
        const body = q('[data-body]'); body.replaceChildren();
        const content = details.content || {};
        message('[data-summary]', content.bodyText ? content.bodyText.slice(0, 180) + (content.bodyText.length > 180 ? '…' : '') : '본문을 확보하지 못했습니다. 원문을 확인하세요.');
        const url = C.safeSourceUrl(content.sourceUrl);
        if (url) { const link = text(body, 'a', '외부 원문 보기 (새 창)'); link.href = url; link.target = '_blank'; link.rel = 'noopener noreferrer'; }
        text(body, 'pre', content.bodyText || '확보한 본문이 없습니다.');
        const evidence = source.effectiveClassification?.setId ? source.effectiveClassification : source.previewClassification;
        message('[data-evidence]', evidence?.setId ? (source.effectiveClassification?.setId === evidence.setId ? '현재 판정에 사용한 첨부입니다.' : '미리보기 자료입니다. 현재 판정에 적용되지 않았습니다.') : '판정에 연결된 첨부가 없습니다. 첨부 없음이 확인된 상태와는 다릅니다.');
        q('[data-files]').replaceChildren(); q('[data-blocks]').replaceChildren();
        if (evidence?.setId) loadFiles(evidence.setId);
        else loadLatestFiles();
        const ready = C.matchesContext(source, context);
        message('[data-blocker]', ready ? (context.linkedAnnouncement ? '이미 공고로 연결되었습니다. 아래 공고 관리에서 이어서 확인하세요.' : '') : C.flowGuidance(source));
        q('[data-result]').replaceChildren(); q('[data-result]').hidden = !context?.linkedAnnouncement;
        if (context?.linkedAnnouncement) {
            text(q('[data-result]'), 'p', `연결된 공고: ${context.linkedAnnouncement.announcementCode}. 공개 상태는 공고 관리에서 확인하세요.`);
            const link = text(q('[data-result]'), 'a', '공고 내용 입력하기', 'primary-action');
            link.href = /^[0-9a-f-]{36}$/i.test(context.linkedAnnouncement.announcementId)
                ? `/app/announcements/input?announcementId=${encodeURIComponent(context.linkedAnnouncement.announcementId)}` : '/app/announcements/input';
        }
        if (!ready) return;
        if (!dirty) ['targetCategoryCodes', 'supportTypeCodes'].forEach(name => form.querySelectorAll(`input[name="${name}"]`).forEach(input => {
            input.checked = (context.confirmedClassification?.[name] || source.effectiveClassification?.[name] || []).includes(input.value);
        }));
        primaryOptions();
        choices('[data-acknowledgements]', 'acknowledgedErrorCodes', Object.fromEntries(context.requiredAcknowledgementCodes.map(code => [code, C.label(code)])));
        q('[data-ack-section]').hidden = !context.requiredAcknowledgementCodes.length;
        q('[data-manual-help]').hidden = !context.manualSourceCheckRequired;
        fields.acknowledged.checked = false;
        if (context.manualSourceCheckRequired) fields.reviewMethodCode.value = 'MANUAL_SOURCE_CHECK';
    };
    const refresh = async () => {
        if (busy || uncertain()) return false;
        busy = true; locked = true; const expected = ++epoch; gates(); message('[data-error]', '');
        message('[data-status]', '최신 자료를 확인하고 있습니다.');
        try {
            const details = await request(root);
            const review = C.canRequestFinalReview(details.source) ? await request(`${root}/attachment-classification/review-context`) : null;
            if (expected !== epoch) return false;
            source = details.source; context = review; locked = !C.matchesContext(source, context);
            render(details); message('[data-status]', saved() ? '검수 저장 완료 · 초안을 만들 수 있습니다.' : '조회 완료 · 공고는 자동 공개되지 않습니다.');
            return !locked;
        } catch (error) {
            source = null; context = null; locked = true;
            ['[data-body]', '[data-files]', '[data-blocks]', '[data-result]'].forEach(selector => q(selector).replaceChildren());
            message('[data-title]', '최신 공고 조회 실패'); message('[data-summary]', '입력은 유지됩니다. 최신 자료 확인을 다시 실행하세요.');
            message('[data-evidence]', '첨부 조회 미완료 · 첨부 없음으로 판단하지 마세요.');
            message('[data-error]', error.message, true); message('[data-status]', '조회 실패 · 전환할 수 없습니다.'); return false;
        } finally { busy = false; gates(); }
    };
    const mutate = async (kind, payload) => {
        const attempt = kind === 'confirm' ? confirmation : draft;
        const prepared = attempt.prepare(payload);
        try {
            const result = await request(`${root}/attachment-classification/${kind === 'confirm' ? 'confirmations' : 'announcements'}`, {method:'POST', body:prepared.body, headers:kind === 'confirm' ? {'Idempotency-Key':prepared.key} : {}});
            if (!/^[0-9a-f-]{36}$/i.test(kind === 'confirm' ? result.confirmationId : result.announcementId)) throw new C.RequestError('저장 결과를 확인하지 못했습니다. 동일 요청으로 다시 확인하세요.');
            attempt.succeed(); return result;
        } catch (error) { attempt.fail(error); throw error; }
    };
    const convert = async (retry = false) => {
        if (busy || page.dataset.canManage !== 'true') return;
        if (!retry && (locked || uncertain() || !C.matchesContext(source, context) || context.linkedAnnouncement)) return;
        message('[data-error]', '');
        if (!retry) {
            if (!form.reportValidity()) return;
            const targets = checked('targetCategoryCodes'), supports = checked('supportTypeCodes'), acknowledgements = checked('acknowledgedErrorCodes');
            const problem = !targets.length || !supports.length ? '지원대상과 지원형태를 각각 한 개 이상 선택하세요.'
                : !targets.includes(fields.primaryTargetCategoryCode.value) ? '선택한 지원대상 중 대표 지원대상을 선택하세요.'
                : !saved() && !fields.reviewNote.value.trim() ? '검수 사유를 1~1,000자로 입력하세요.'
                : !saved() && context.requiredAcknowledgementCodes.some(code => !acknowledgements.includes(code)) ? '추가로 확인할 내용을 모두 확인하고 체크하세요.'
                : !saved() && context.manualSourceCheckRequired && fields.reviewMethodCode.value !== 'MANUAL_SOURCE_CHECK' ? '불완전한 자료가 있습니다. 원문 전체 직접 확인이 필요합니다.' : null;
            if (problem) { message('[data-error]', problem, true); return; }
            chain = {version:{...context.version}, primary:fields.primaryTargetCategoryCode.value, income:fields.incomeJudgementCode.value};
        }
        busy = true; gates();
        try {
            if (retry && draft.uncertain) {
                await mutate('draft', draft.original);
            } else {
                if (!saved() || confirmation.uncertain) {
                    const payload = confirmation.uncertain ? confirmation.original : {version:context.version, targetCategoryCodes:checked('targetCategoryCodes'), supportTypeCodes:checked('supportTypeCodes'), acknowledgedErrorCodes:checked('acknowledgedErrorCodes'), reviewMethodCode:fields.reviewMethodCode.value, reviewNote:fields.reviewNote.value.trim()};
                    const receipt = await mutate('confirm', payload); dirty = false;
                    // 검수 저장 직후 최신 연결을 확인한다. 다른 검수나 버전 변경은 자동 진행하지 않는다.
                    const next = await request(`${root}/attachment-classification/review-context`);
                    if (!C.sameVersion(chain.version, next.version) || !C.confirmedCurrent(next) || next.confirmedClassification.confirmation.confirmationId !== receipt.confirmationId || next.linkedAnnouncement) {
                        locked = true; throw new Error('검수는 저장됐으나 최신 기준이 변경되었습니다. 최신 자료를 확인한 뒤 초안을 만드세요.');
                    }
                    context = next;
                }
                await mutate('draft', {version:context.version, expectedConfirmationId:context.confirmedClassification.confirmation.confirmationId, primaryTargetCategoryCode:chain.primary, incomeJudgementCode:chain.income});
            }
            dirty = false; fields.acknowledged.checked = false; busy = false;
            await refresh();
        } catch (error) {
            if (!uncertain()) locked = true;
            message('[data-error]', `${error.message} ${uncertain() ? '아래 동일 요청 재확인을 사용하세요.' : '입력은 유지했습니다. 최신 자료 확인 후 이어서 진행하세요.'}`, true);
        } finally { busy = false; gates(); }
    };
    form.addEventListener('input', event => {
        if (['targetCategoryCodes', 'supportTypeCodes', 'reviewMethodCode', 'reviewNote', 'acknowledgedErrorCodes'].includes(event.target.name)) dirty = true;
        if (event.target.name !== 'acknowledged') fields.acknowledged.checked = false;
        if (event.target.name === 'targetCategoryCodes') primaryOptions();
        gates();
    });
    form.addEventListener('submit', event => { event.preventDefault(); convert(); });
    q('[data-retry]').addEventListener('click', () => convert(true));
    q('[data-refresh]').addEventListener('click', refresh);
    window.addEventListener('beforeunload', event => { if (dirty || busy || uncertain() || fields.reviewNote.value || fields.incomeJudgementCode.value) { event.preventDefault(); event.returnValue = ''; } });
    refresh();
})();
