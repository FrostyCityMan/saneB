import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const source = readFileSync(new URL('../../src/main/resources/static/js/saneb-csrf.js', import.meta.url), 'utf8');
function harness() {
    const calls = [];
    const document = { cookie: 'XSRF-TOKEN=test-raw-token', querySelector: () => null };
    const window = {
        location: { href: 'http://localhost/app/admin/announcement-attachment-policies', origin: 'http://localhost' },
        fetch: (input, init) => { calls.push({ input, init }); return Promise.resolve({ ok: true }); }
    };
    runInNewContext(source, { window, document, Request, Headers, URL });
    return { window, document, calls };
}

test('same-origin policy POST uses raw cookie and preserves body and idempotency key', async () => {
    const { window, calls } = harness();
    await window.fetch('/api/v2/admin/announcement-attachment-policies', {
        method: 'POST', headers: { 'Idempotency-Key': 'synthetic-key', 'Content-Type': 'application/json' }, body: '{}'
    });
    assert.equal(calls[0].init.headers.get('X-XSRF-TOKEN'), 'test-raw-token');
    assert.equal(calls[0].init.headers.get('Idempotency-Key'), 'synthetic-key');
    assert.equal(calls[0].init.body, '{}');
});

test('safe methods and other origins do not receive a CSRF header', async () => {
    const { window, calls } = harness();
    for (const method of ['GET', 'HEAD', 'OPTIONS', 'TRACE']) await window.fetch('/api/v2/test', { method });
    await window.fetch('https://example.org/test', { method: 'POST' });
    for (const call of calls) assert.equal(call.init.headers, undefined);
});

test('cookie is reread on each mutation after refresh', async () => {
    const { window, document, calls } = harness();
    await window.fetch('/api/v2/test', { method: 'POST' });
    document.cookie = 'XSRF-TOKEN=test-refreshed-token';
    await window.fetch('/api/v2/test', { method: 'POST' });
    assert.equal(calls[0].init.headers.get('X-XSRF-TOKEN'), 'test-raw-token');
    assert.equal(calls[1].init.headers.get('X-XSRF-TOKEN'), 'test-refreshed-token');
});

test('Request headers survive wrapping and an explicit CSRF value is not silently replaced', async () => {
    const { window, calls } = harness();
    await window.fetch(new Request('http://localhost/api/v2/test', {
        method: 'PUT', headers: { 'X-XSRF-TOKEN': 'explicit-value', 'Idempotency-Key': 'synthetic-key' }
    }));
    assert.equal(calls[0].init.headers.get('X-XSRF-TOKEN'), 'explicit-value');
    assert.equal(calls[0].init.headers.get('Idempotency-Key'), 'synthetic-key');
});

test('cookie takes priority over masked page metadata', async () => {
    const { window, document, calls } = harness();
    document.querySelector = selector => ({ content: selector.includes('_csrf_header') ? 'X-XSRF-TOKEN' : 'masked-form-token' });
    await window.fetch('/api/v2/test', { method: 'DELETE' });
    assert.equal(calls[0].init.headers.get('X-XSRF-TOKEN'), 'test-raw-token');
});

test('missing token is not fabricated and remains rejectable by the server', async () => {
    const { window, document, calls } = harness();
    document.cookie = '';
    await window.fetch('/api/v2/test', { method: 'POST' });
    assert.equal(calls[0].init.headers, undefined);
});
