(() => {
    const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const DEFAULT_HEADER_NAME = "X-XSRF-TOKEN";
    const originalFetch = window.fetch.bind(window);

    const readMeta = (name) => {
        const meta = document.querySelector(`meta[name='${name}']`);
        return meta ? String(meta.content || "").trim() : "";
    };

    const readCookie = (name) => {
        const prefix = `${name}=`;
        const cookie = document.cookie
                .split(";")
                .map((value) => value.trim())
                .find((value) => value.startsWith(prefix));
        if (!cookie) {
            return "";
        }
        return decodeURIComponent(cookie.slice(prefix.length));
    };

    const isSameOrigin = (input) => {
        const rawUrl = input instanceof Request ? input.url : input;
        const url = new URL(String(rawUrl), window.location.href);
        return url.origin === window.location.origin;
    };

    window.fetch = (input, init = {}) => {
        const request = input instanceof Request ? input : null;
        const method = String(init.method || (request ? request.method : "GET")).toUpperCase();
        if (SAFE_METHODS.has(method) || !isSameOrigin(input)) {
            return originalFetch(input, init);
        }

        const token = readMeta("_csrf") || readCookie(CSRF_COOKIE_NAME);
        if (!token) {
            return originalFetch(input, init);
        }

        const headerName = readMeta("_csrf_header") || DEFAULT_HEADER_NAME;
        const headers = new Headers(init.headers || (request ? request.headers : undefined));
        if (!headers.has(headerName)) {
            headers.set(headerName, token);
        }

        return originalFetch(input, {
            ...init,
            headers
        });
    };
})();
