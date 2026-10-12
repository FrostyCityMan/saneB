/* 상담 운영 화면의 명시적 사람 선택. 서버 권한·저장 검증을 대체하지 않습니다. */
(() => {
    'use strict';
    window.SanebPersonPicker = function (root, requestJson) {
        if (!root) return null;
        const query = root.querySelector('[data-person-query]');
        const results = root.querySelector('[data-person-results]');
        const hidden = root.querySelector('input[type="hidden"]');
        const selected = root.querySelector('[data-person-selected]');
        const message = root.querySelector('[data-person-message]');
        const search = root.querySelector('[data-person-search]');
        const prev = root.querySelector('[data-person-prev]');
        const next = root.querySelector('[data-person-next]');
        const label = root.dataset.personPicker === 'member' ? '회원' : '담당자';
        const objectLabel = label === '회원' ? '회원을' : '담당자를';
        const subjectLabel = label === '회원' ? '회원이' : '담당자가';
        let page = 1, sequence = 0, items = [], pending = false;
        const emptyResults = () => {
            items = []; results.replaceChildren(new Option('검색 후 ' + objectLabel + ' 선택하세요', ''));
            prev.disabled = true; next.disabled = true;
        };
        const clearSelection = () => {
            hidden.value = ''; selected.textContent = '선택된 ' + subjectLabel + ' 없습니다.';
        };
        const set = (id, name, loginId) => {
            sequence++; pending = false; query.value = ''; clearSelection(); emptyResults();
            hidden.value = id || '';
            selected.textContent = id ? '선택: ' + window.SanebPersonIdentity.label(name, loginId) : '선택된 ' + subjectLabel + ' 없습니다.';
            message.textContent = '변경하려면 성함 또는 아이디로 검색한 뒤 선택하세요.';
            search.disabled = false; results.disabled = false;
        };
        const load = async (targetPage) => {
            const own = ++sequence; page = targetPage; pending = true;
            search.disabled = true; results.disabled = true; emptyResults();
            message.textContent = label + ' 검색 중입니다.';
            try {
                const params = new URLSearchParams({keyword: query.value.trim(), page: String(page), size: '10'});
                const data = await requestJson(root.dataset.lookupUrl + '?' + params);
                if (own !== sequence) return;
                items = data.items || [];
                results.replaceChildren(new Option(items.length ? objectLabel + ' 선택하세요' : '검색 결과가 없습니다', ''));
                items.forEach(person => results.append(new Option(window.SanebPersonIdentity.label(person.name, person.loginId), person.userId)));
                prev.disabled = page <= 1; next.disabled = page >= data.totalPages;
                message.textContent = items.length ? `총 ${data.totalCount}명 · ${page}/${data.totalPages}페이지 · 성함과 아이디를 확인해 선택하세요.` : '검색 결과가 없습니다. 성함 또는 아이디를 확인하거나 검색어를 지우고 다시 검색하세요.';
            } catch (error) {
                if (own !== sequence) return;
                message.textContent = error.message || '검색하지 못했습니다. 검색 버튼을 눌러 다시 시도하세요.';
            } finally {
                if (own === sequence) { pending = false; search.disabled = false; results.disabled = false; }
            }
        };
        query.addEventListener('input', () => {
            sequence++; pending = false; page = 1; search.disabled = false; results.disabled = false;
            clearSelection(); emptyResults(); message.textContent = '검색어가 바뀌었습니다. 검색 후 ' + objectLabel + ' 다시 선택하세요.';
        });
        query.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); load(1); } });
        search.addEventListener('click', () => load(1));
        prev.addEventListener('click', () => load(page - 1));
        next.addEventListener('click', () => load(page + 1));
        results.addEventListener('change', () => {
            const person = items.find(item => item.userId === results.value);
            clearSelection();
            if (person) { hidden.value = person.userId; selected.textContent = '선택: ' + window.SanebPersonIdentity.label(person.name, person.loginId); }
        });
        return {set, unresolved: () => pending || (query.value.trim() !== '' && !hidden.value)};
    };
})();
