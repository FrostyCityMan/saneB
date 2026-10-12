const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '../../src/main/resources');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const common = read('static/css/saneb-dashboard.css');
const consultation = read('static/css/saneb-consultation-management.css');
const attachment = read('static/css/saneb-announcement-attachment-review.css');
// CSS 선언 계약 검사이며 실제 브라우저의 레이아웃 계산/반응형 검증을 대체하지 않는다.
const rules = css => [...css.replace(/\/\*[\s\S]*?\*\//g, '').matchAll(/([^{}]+)\{([^{}]*)\}/g)]
    .map(([, selector, body]) => ({selectors: selector.trim().split(',').map(s => s.trim()), body}));
const exact = (css, selector) => rules(css).find(rule => rule.selectors.includes(selector));
const pages = fs.readdirSync(path.join(root, 'templates/app')).filter(file => file.endsWith('.html'))
    .map(file => ({file, html: read('templates/app/' + file)}));
const paddedRoots = ['consultation-workspace', 'attachment-review-page', 'notification-page',
    'operation-task-page', 'billing-page', 'consultation-page'];

test('여백이 없었던 업무 화면군은 하나의 공통 너비·여백 계약을 사용한다', () => {
    for (const name of paddedRoots) {
        const rule = exact(common, '.' + name);
        assert.ok(rule, name);
        for (const property of ['width: 100%', 'max-width: var(--saneb-page-width)',
            'margin: 0 auto', 'padding: var(--saneb-page-padding)', 'gap: var(--saneb-page-gap)']) {
            assert.ok(rule.body.includes(property), name + ': ' + property);
        }
    }
    assert.match(common, /--saneb-page-width:\s*1320px/);
    assert.match(common, /@media\s*\(max-width: 760px\)\s*\{\s*:root\s*\{\s*--saneb-page-padding: 18px 14px/);
});

test('전체 app 템플릿의 최상위 main에는 명시적인 바깥 여백 규칙이 존재한다', () => {
    assert.ok(pages.length >= 36, '전체 app 화면을 검사해야 합니다.');
    for (const {file, html} of pages) {
        const classes = html.match(/<main\b[^>]*class="([^"]+)"/s)?.[1].split(/\s+/);
        assert.ok(classes?.length, file);
        assert.ok(classes.some(name => exact(common, '.' + name)?.body.match(/\bpadding\s*:/)), file);
    }
});

test('전용 화면 CSS는 공통 select 화살표와 오른쪽 여백을 덮어쓰지 않는다', () => {
    const select = exact(common, '.saneb-main select').body;
    assert.match(select, /background-image:/);
    assert.match(select, /padding: 10px 34px 10px 12px/);
    for (const css of [consultation, attachment]) {
        for (const rule of rules(css).filter(rule => rule.selectors.some(s => /\bselect\b/.test(s)))) {
            assert.doesNotMatch(rule.body, /(?:^|;)\s*(?:background|background-image|padding|padding-right|appearance)\s*:/);
        }
    }
});

test('공통 스타일의 변수 참조는 모두 선언되며 레거시 이름은 사내비 토큰에 연결된다', () => {
    const css = common + consultation + attachment;
    const defined = new Set([...css.matchAll(/(--[\w-]+)\s*:/g)].map(match => match[1]));
    for (const [, name] of css.matchAll(/var\((--[\w-]+)/g)) assert.ok(defined.has(name), name);
    for (const [old, current] of Object.entries({line:'border', 'line-strong':'border-strong', ink:'text', muted:'muted', accent:'teal'})) {
        assert.ok(common.includes(`--${old}: var(--saneb-${current});`));
    }
});

test('공통 상단 H1 아래 app 화면은 중복 H1을 만들지 않는다', () => {
    assert.equal((read('templates/fragments/layout.html').match(/<h1\b/g) || []).length, 1);
    for (const {file, html} of pages) assert.doesNotMatch(html, /<h1\b/, file);
    for (const name of ['list', 'detail', 'new']) {
        assert.match(read(`templates/app/consultation-management-${name}.html`), /summary-panel summary-header cm-heading/);
    }
});

test('선택·입력·포커스와 좁은 본문 전환 화면의 별도 너비를 보존한다', () => {
    assert.match(consultation, /:focus-visible\s*\{[^}]*outline:/);
    assert.match(attachment, /forced-colors: active/);
    assert.match(attachment, /\.conversion-review\s*\{\s*max-width:1120px/);
    assert.match(attachment, /\.attachment-review-page select\s*\{[^}]*min-height: 44px/);
    assert.match(attachment, /\.attachment-check input\s*\{[^}]*width: 20px/);
    assert.doesNotMatch(consultation, /\.consultation-workspace input,/);
});

// 새 참고안은 이전 트리 위계 대신 작은 섹션 라벨과 평면 아이콘 목록을 사용한다.
test('사이드바는 작은 섹션 제목과 일정한 아이콘 메뉴 간격을 사용한다', () => {
    assert.match(common, /--saneb-nav-section-size: 13px/);
    assert.match(common, /--saneb-nav-item-size: 14px/);
    assert.match(exact(common, '.nav-group-toggle').body, /font-size: var\(--saneb-nav-section-size\)/);
    assert.match(exact(common, '.side-link').body, /font-size: var\(--saneb-nav-item-size\)/);
    assert.match(exact(common, '.side-nav').body, /gap: 16px/);
    assert.match(exact(common, '.nav-group-items').body, /padding-left: 0/);
    assert.match(exact(common, '.side-link').body, /padding: 10px;/);
    for (const selector of ['.nav-group-toggle', '.side-link']) {
        assert.match(exact(common, selector).body, /min-height: 44px/);
    }
    assert.doesNotMatch(common, /\.nav-group-items > \.side-link::(?:before|after)/);
});

test('선택 메뉴는 사내비 청록 배경·밝은 글자이며 각 폭의 제목 아이콘 처리를 보존한다', () => {
    const active = exact(common, '.side-link.active').body;
    assert.match(active, /background: var\(--saneb-teal\)/);
    assert.match(active, /color: var\(--saneb-surface\)/);
    assert.match(active, /font-weight: 750/);
    assert.match(exact(common, '.saneb-sidebar').body, /background: var\(--saneb-panel\)/);
    const desktop = common.slice(0, common.indexOf('@media (max-width: 1180px)'));
    const groupIcon = rules(desktop).filter(rule => rule.selectors.includes('.nav-group-icon')).at(-1);
    assert.match(groupIcon.body, /display: none/);
    const responsive = common.slice(common.indexOf('@media (max-width: 1180px)'));
    const mobile = responsive.slice(responsive.indexOf('@media (max-width: 760px)'));
    assert.match(responsive, /\.nav-group-icon\s*\{\s*display: inline-flex/);
    assert.match(mobile, /\.nav-group-icon\s*\{\s*display: none/);
    assert.match(mobile, /\.nav-group-items\s*\{\s*padding-left: 0/);
    assert.match(exact(common, '.side-link:focus-visible').body, /outline:/);
    assert.match(exact(common, '.side-link.active:focus-visible').body, /outline-color: var\(--saneb-border-strong\)/);
});

test('사이드바 텍스트 색상 쌍은 4.5:1 이상 대비를 유지한다 — 정적 색상 검증', () => {
    const color = name => common.match(new RegExp(`--saneb-${name}: #([0-9a-f]{6});`))[1];
    const luminance = hex => hex.match(/../g).map(value => parseInt(value, 16) / 255)
        .map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
        .reduce((total, value, i) => total + value * [.2126, .7152, .0722][i], 0);
    for (const [foreground, background] of [['muted', 'panel'], ['text', 'panel'], ['surface', 'teal'], ['surface', 'teal-light']]) {
        const values = [luminance(color(foreground)), luminance(color(background))].sort((a, b) => a - b);
        assert.ok((values[1] + .05) / (values[0] + .05) >= 4.5, foreground + '/' + background);
    }
});

test('모든 사이드바 링크는 축소 화면 아이콘을 가지며 기존 그룹·접기 계약을 보존한다', () => {
    const layout = read('templates/fragments/layout.html');
    const nav = layout.match(/<nav class="side-nav"[\s\S]*?<\/nav>/)[0];
    const links = [...nav.matchAll(/<a class="side-link"[\s\S]*?<\/a>/g)];
    assert.ok(links.length >= 20);
    for (const [link] of links) assert.match(link, /class="nav-icon" aria-hidden="true"/);
    assert.equal((nav.match(/data-nav-group-id=/g) || []).length, 6);
    assert.equal((nav.match(/class="nav-group-items"/g) || []).length, 6);
    assert.match(common, /\.nav-group-items\[hidden\]\s*\{\s*display: none/);
    assert.match(read('static/js/saneb-layout.js'), /setAttribute\("aria-expanded", String\(expanded\)\)/);
    assert.match(read('static/js/saneb-layout.js'), /event.key === "Escape"/);
});
