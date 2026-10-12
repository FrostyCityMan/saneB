(function (root, factory) {
    const api = factory();
    if (typeof module === 'object' && module.exports) module.exports = api;
    else root.SanebPersonIdentity = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';
    function label(name, loginId) {
        const displayName = String(name || '').trim();
        const login = String(loginId || '').trim();
        if (displayName && login) return `${displayName} (${login})`;
        return displayName || login || '계정 정보 확인 필요';
    }
    return { label };
});
