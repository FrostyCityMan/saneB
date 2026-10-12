/* 상담 목록 URL과 표시 정책. 브라우저 상태에 개인정보 원문을 보관하지 않습니다. */
((root) => {
    'use strict';
    const labels = Object.freeze({REQUESTED: '요청 접수', ASSIGNED: '담당자 배정', CONFIRMED: '상담 확정', COMPLETED: '상담 완료', NO_SHOW: '미참석', CANCELED: '취소'});
    const state = (search) => {
        const p = new URLSearchParams(search);
        const page = Number(p.get('page'));
        return {statusCode: Object.hasOwn(labels, p.get('statusCode')) ? p.get('statusCode') : '',
            page: Number.isSafeInteger(page) && page > 0 && page <= 10000 ? page : 1,
            size: p.get('size') === '50' ? 50 : 20};
    };
    const query = (s) => new URLSearchParams({statusCode: s.statusCode, page: String(s.page), size: String(s.size)}).toString();
    const href = (id, s) => '/app/admin/consultations' + (id ? '/' + encodeURIComponent(id) : '') + '?' + query(s);
    const pages = (current, total) => {
        const start = Math.max(1, Math.min(current - 2, total - 4));
        return Array.from({length: Math.min(5, Math.max(0, total))}, (_, i) => start + i);
    };
    const date = (value) => value ? new Date(value).toLocaleString('ko-KR', {timeZone: 'Asia/Seoul', hour12: false}) : '미지정';
    const api = {labels, state, query, href, pages, date};
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    else root.SanebConsultationManagement = api;
})(typeof window === 'undefined' ? globalThis : window);
