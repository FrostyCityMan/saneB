(() => {
    'use strict';
    const app = document.querySelector('[data-consultation-management]');
    if (!app) return;
    const M = window.SanebConsultationManagement;
    const person = window.SanebPersonIdentity.label;
    const q = selector => app.querySelector(selector);
    const feedback = q('[data-feedback]'), errorBox = q('[data-error]');
    const mode = app.dataset.consultationManagement;
    let state = M.state(window.location.search), busy = false, dirty = false, current = null, latest = null;
    const say = (text, error = false) => {
        const target = error ? errorBox : feedback;
        target.textContent = text; target.hidden = !text;
        if (error && text) target.focus();
    };
    const clear = () => { say(''); say('', true); };
    const request = async (url, options = {}) => {
        const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 20000);
        try {
            const response = await fetch(url, {credentials: 'same-origin', cache: 'no-store', ...options,
                headers: {Accept: 'application/json', 'Content-Type': 'application/json'}, signal: controller.signal});
            const payload = await response.json().catch(() => null);
            if (!response.ok || payload?.success !== true) {
                const messages = {401: '로그인이 만료되었습니다. 다시 로그인 후 상담을 확인하세요.',
                    403: '관리자 또는 운영자만 상담관리 기능을 이용할 수 있습니다.',
                    404: '상담을 찾을 수 없습니다. 목록에서 다시 확인하세요.'};
                const error = new Error(messages[response.status] || payload?.message || '상담 조회·저장에 실패했습니다. 다시 조회해 확인하세요.');
                error.status = response.status; throw error;
            }
            return payload.data;
        } catch (error) {
            if (error.status) throw error;
            throw new Error(options.method ? '서버 응답을 확인하지 못했습니다. 입력은 유지됩니다. 중복 접수·저장 전에 목록 또는 최신 내용에서 반영 여부를 확인하세요.' : '상담을 불러오지 못했습니다. 연결 상태를 확인한 뒤 다시 조회하세요.');
        } finally { clearTimeout(timer); }
    };
    const element = (tag, text, cls) => {
        const node = document.createElement(tag); node.textContent = text ?? '';
        if (cls) node.className = cls; return node;
    };
    const link = (text, url) => { const node = element('a', text); node.href = url; return node; };
    const updateLinks = () => {
        app.querySelectorAll('[data-list-link]').forEach(a => { a.href = M.href('', state); });
        if (q('[data-new-link]')) q('[data-new-link]').href = M.href('new', state);
    };
    updateLinks();
    app.addEventListener('click', event => {
        const anchor = event.target.closest('a');
        if (!anchor) return;
        if ((dirty || busy) && !window.confirm('저장하지 않은 내용이 사라질 수 있습니다. 이동하시겠습니까?')) { event.preventDefault(); return; }
        if (mode === 'list') history.replaceState({...history.state, consultationScroll: window.scrollY}, '', window.location.href);
    });
    window.addEventListener('beforeunload', event => { if (dirty || busy) { event.preventDefault(); event.returnValue = ''; } });

    if (mode === 'list') {
        const filter = q('[data-filter]'), rows = q('[data-rows]'), pagination = q('[data-pagination]');
        filter.elements.statusCode.value = state.statusCode; filter.elements.size.value = String(state.size);
        const load = async () => {
            if (busy) return;
            busy = true; clear(); app.setAttribute('aria-busy', 'true');
            rows.replaceChildren(); pagination.replaceChildren(); q('[data-table]').hidden = true; q('[data-empty]').hidden = true;
            q('[data-count]').textContent = '상담을 불러오는 중입니다.';
            filter.querySelectorAll('button').forEach(b => { b.disabled = true; });
            try {
                let data = await request('/api/v1/consultation-reservations?' + M.query(state));
                if (state.page > Math.max(1, data.totalPages)) {
                    state.page = Math.max(1, data.totalPages); history.replaceState(null, '', M.href('', state)); updateLinks();
                    data = await request('/api/v1/consultation-reservations?' + M.query(state));
                }
                q('[data-count]').textContent = `총 ${data.totalCount}건 · ${data.items.length ? (state.page - 1) * state.size + 1 : 0}–${Math.min(state.page * state.size, data.totalCount)}건 표시`;
                q('[data-table]').hidden = data.items.length === 0;
                q('[data-empty]').hidden = data.items.length !== 0;
                q('[data-empty]').textContent = state.statusCode ? '선택한 상태의 상담이 없습니다. 필터를 초기화해 확인하세요.' : '접수된 상담이 없습니다. 상담 접수 버튼으로 시작하세요.';
                data.items.forEach(row => {
                    const tr = element('tr');
                    const member = element('td', row.reservationCode || '요청 코드 확인 필요');
                    member.append(element('small', person(row.memberName, row.memberLoginId))); tr.append(member);
                    const note = element('td'); note.append(element('span', row.requestNote || '요청 내용 없음', 'cm-row-note')); tr.append(note);
                    const values = [['담당자', row.partnerUserId ? person(row.partnerName, row.partnerLoginId) : '미배정'], ['상태', M.labels[row.statusCode] || '상태 확인 필요'], ['접수일', M.date(row.createdAt)]];
                    values.forEach(([label, text]) => { const td = element('td', text); td.dataset.label = label; tr.append(td); });
                    const td = element('td'), a = link('상세 보기', M.href(row.reservationId, state));
                    a.setAttribute('aria-label', `${row.reservationCode || '상담'} 상세 보기`); td.append(a); tr.append(td); rows.append(tr);
                });
                const addPage = (text, page) => pagination.append(link(text, M.href('', {...state, page})));
                if (state.page > 1) addPage('이전', state.page - 1);
                M.pages(state.page, data.totalPages).forEach(page => {
                    const a = link(String(page), M.href('', {...state, page}));
                    if (page === state.page) a.setAttribute('aria-current', 'page'); pagination.append(a);
                });
                if (state.page < data.totalPages) addPage('다음', state.page + 1);
                if (history.state?.consultationScroll) window.scrollTo(0, history.state.consultationScroll);
            } catch (error) { q('[data-count]').textContent = '조회 실패 · 건수를 확인하지 못했습니다.'; say(error.message, true); }
            finally { busy = false; app.setAttribute('aria-busy', 'false'); filter.querySelectorAll('button').forEach(b => { b.disabled = false; }); }
        };
        filter.addEventListener('submit', event => { event.preventDefault(); window.location.assign(M.href('', {statusCode: filter.elements.statusCode.value, size: Number(filter.elements.size.value), page: 1})); });
        q('[data-reload]').addEventListener('click', load); load(); return;
    }

    const form = q('form'), fields = q('[data-fields]'), save = q('[data-save]');
    const picker = window.SanebPersonPicker(q('[data-person-picker]'), request);
    const value = name => form.elements.namedItem(name).value.trim();
    const summary = () => {
        if (!current || mode !== 'detail') return;
        const r = current.reservation;
        const selected = q('[data-person-selected]').textContent;
        q('[data-summary]').textContent = `담당자: ${r.partnerUserId ? person(r.partnerName, r.partnerLoginId) : '미배정'} → ${value('partnerUserId') ? selected.replace(/^선택: /, '') : '기존 담당자 유지'}\n상태: ${M.labels[r.statusCode]} → ${M.labels[value('statusCode')] || '처리할 상태를 선택하세요'}`;
    };
    form.addEventListener('input', () => { dirty = true; summary(); });
    form.addEventListener('change', () => { dirty = true; summary(); });
    const setBusy = flag => { busy = flag; fields.disabled = flag; save.textContent = flag ? '저장 중…' : mode === 'new' ? '상담 접수' : '상태 저장'; };

    if (mode === 'new') {
        form.addEventListener('submit', async event => {
            event.preventDefault(); if (busy) return; clear();
            if (!value('memberUserId') || picker.unresolved()) { say('성함·아이디로 회원을 검색한 뒤 검색 결과에서 선택하세요.', true); return; }
            if (!value('requestNote')) { say('상담 요청 내용을 공백이 아닌 문자로 입력하세요.', true); return; }
            setBusy(true);
            try {
                const r = await request('/api/v1/consultation-reservations', {method: 'POST', body: JSON.stringify({memberUserId: value('memberUserId'), requestNote: value('requestNote')})});
                dirty = false; busy = false; window.location.assign(M.href(r.reservationId, state));
            } catch (error) { say(error.message, true); setBusy(false); }
        });
        return;
    }

    const endpoint = '/api/v2/admin/consultation-reservations/' + encodeURIComponent(app.dataset.reservationId);
    const render = data => {
        current = data; latest = null; const r = data.reservation;
        q('[data-detail]').hidden = false; q('[data-code]').textContent = r.reservationCode;
        q('[data-status]').textContent = M.labels[r.statusCode] || '상태 확인 필요';
        const info = q('[data-info]'); info.replaceChildren();
        [['회원', person(r.memberName, r.memberLoginId)], ['담당자', r.partnerUserId ? person(r.partnerName, r.partnerLoginId) : '미배정'], ['접수일', M.date(r.createdAt)], ['최근 수정일', M.date(r.updatedAt)], ['상담 시작', M.date(r.startAt)], ['상담 종료', M.date(r.endAt)]].forEach(([label, text]) => {
            const group = element('div'); group.append(element('dt', label), element('dd', text)); info.append(group);
        });
        q('[data-request-note]').textContent = r.requestNote || '요청 내용 없음';
        q('[data-saved-note]').textContent = r.statusNote || '저장된 메모 없음';
        picker.set(r.partnerUserId, r.partnerName, r.partnerLoginId);
        form.elements.note.value = r.statusNote || '';
        form.elements.statusCode.replaceChildren(new Option('처리할 상태를 선택하세요', ''));
        data.allowedNextStatusCodes.forEach(code => form.elements.statusCode.append(new Option(M.labels[code], code)));
        fields.disabled = data.allowedNextStatusCodes.length === 0;
        q('[data-terminal]').hidden = data.allowedNextStatusCodes.length !== 0;
        q('[data-conflict]').hidden = true; dirty = false; summary();
    };
    const reload = async () => {
        if (busy) return; clear(); busy = true; q('[data-reload]').disabled = true; say('최신 상담 내용을 불러오는 중입니다.');
        try {
            const data = await request(endpoint);
            if (dirty) {
                latest = data; const r = data.reservation;
                q('[data-latest]').textContent = `상태: ${M.labels[r.statusCode]}\n담당자: ${r.partnerUserId ? person(r.partnerName, r.partnerLoginId) : '미배정'}\n상담 메모: ${r.statusNote || '없음'}\n수정일: ${M.date(r.updatedAt)}`;
                q('[data-conflict]').hidden = false; say('최신 내용을 별도로 표시했습니다. 작성 내용은 유지됩니다.');
            } else { render(data); say(''); }
        } catch (error) { say(''); say(error.message, true); }
        finally { busy = false; q('[data-reload]').disabled = false; }
    };
    q('[data-use-latest]').addEventListener('click', () => {
        if (latest && !busy && window.confirm('작성 중인 내용을 최신 저장 내용으로 교체하시겠습니까?')) { render(latest); clear(); }
    });
    q('[data-reload]').addEventListener('click', reload);
    form.addEventListener('submit', async event => {
        event.preventDefault(); if (busy || !current || fields.disabled) return; clear();
        if (!current.allowedNextStatusCodes.includes(value('statusCode'))) { say('현재 상태에서 처리할 다음 상태를 선택하세요.', true); return; }
        if (picker.unresolved()) { say('담당자 검색 결과에서 한 명을 선택하거나 검색어를 지워 기존 담당자를 유지하세요.', true); return; }
        const r = current.reservation, partner = value('partnerUserId') || null;
        if (['ASSIGNED', 'CONFIRMED'].includes(value('statusCode')) && !partner && !r.partnerUserId) { say('배정·확정하려면 담당자를 검색한 뒤 선택하세요.', true); return; }
        if ((partner && partner !== r.partnerUserId || value('statusCode') === 'CANCELED') && !window.confirm(`${r.reservationCode}\n${q('[data-summary]').textContent}\n이 내용으로 저장하시겠습니까?`)) return;
        const body = {statusCode: value('statusCode'), partnerUserId: partner === r.partnerUserId ? null : partner, note: value('note'), expectedUpdatedAt: r.updatedAt};
        setBusy(true);
        try { const data = await request(endpoint + '/status', {method: 'PATCH', body: JSON.stringify(body)}); render(data); say('상담 상태를 저장했습니다.'); }
        catch (error) { say(error.message, true); }
        finally { busy = false; fields.disabled = current.allowedNextStatusCodes.length === 0; save.textContent = '상태 저장'; }
    });
    reload();
})();
