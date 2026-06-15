(() => {
    if (window.AppLoading) {
        return;
    }

    const DEFAULT_OPTIONS = {
        title: "처리 중입니다",
        message: "잠시만 기다려 주세요.",
        steps: [],
        preset: "default-api",
        mode: "indeterminate",
        minVisibleMs: 420,
        delayMs: 150,
        autoAdvance: true,
        autoAdvanceMs: 1500,
        autoAdvanceStopBeforeLast: true,
        cancelable: false,
        onCancel: null
    };

    const presetStore = new Map();
    const stateStore = new Map();
    let activeToken = null;
    let tokenSequence = 0;
    let overlay = null;

    const registerPreset = (name, preset) => {
        if (!name || !preset) {
            return;
        }
        const normalizedPreset = { ...preset };
        if (Object.prototype.hasOwnProperty.call(preset, "steps")) {
            normalizedPreset.steps = normalizeSteps(preset.steps || []);
        }
        presetStore.set(name, normalizedPreset);
    };

    const normalizeSteps = (steps) => steps.map((step, index) => {
        if (typeof step === "string") {
            return {
                key: `step-${index}`,
                label: step,
                afterResponse: false
            };
        }
        return {
            key: step.key || `step-${index}`,
            label: step.label || step.title || `단계 ${index + 1}`,
            afterResponse: Boolean(step.afterResponse)
        };
    });

    const defaultSteps = [
        { key: "prepare", label: "요청 준비 중" },
        { key: "waiting", label: "서버 응답 대기 중" },
        { key: "response", label: "응답 데이터 확인 중", afterResponse: true },
        { key: "render", label: "결과를 화면에 반영하는 중", afterResponse: true },
        { key: "done", label: "완료 준비 중", afterResponse: true }
    ];

    registerPreset("default-api", {
        title: "요청 처리 중",
        message: "서버 응답을 기다리고 있습니다.",
        steps: defaultSteps
    });
    registerPreset("save", {
        title: "저장 중",
        message: "입력한 내용을 서버에 저장하고 있습니다.",
        steps: [
            { key: "prepare", label: "저장 내용 확인 중" },
            { key: "waiting", label: "서버에 저장 요청 중" },
            { key: "response", label: "저장 결과 확인 중", afterResponse: true },
            { key: "render", label: "저장 결과를 화면에 반영하는 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });
    registerPreset("delete", {
        title: "삭제 중",
        message: "선택한 항목을 삭제하고 있습니다.",
        steps: [
            { key: "prepare", label: "삭제 요청 준비 중" },
            { key: "waiting", label: "서버에 삭제 요청 중" },
            { key: "response", label: "삭제 결과 확인 중", afterResponse: true },
            { key: "render", label: "목록을 다시 정리하는 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });
    registerPreset("search", {
        title: "검색 중",
        message: "조건에 맞는 결과를 찾고 있습니다.",
        steps: [
            { key: "prepare", label: "검색 조건 확인 중" },
            { key: "waiting", label: "서버에서 검색 중" },
            { key: "response", label: "검색 결과 확인 중", afterResponse: true },
            { key: "render", label: "검색 결과를 화면에 반영하는 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });
    registerPreset("upload", {
        title: "파일 업로드 중",
        message: "파일을 서버로 전송하고 있습니다.",
        mode: "determinate",
        autoAdvance: false,
        steps: [
            { key: "prepare", label: "파일 준비 중" },
            { key: "uploading", label: "파일 전송 중" },
            { key: "response", label: "업로드 결과 확인 중", afterResponse: true },
            { key: "render", label: "첨부 정보를 화면에 반영하는 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });
    registerPreset("ai-long-running", {
        title: "AI 작업 실행 중",
        message: "AI 작업은 시간이 걸릴 수 있습니다.",
        autoAdvanceMs: 2400,
        steps: [
            { key: "prepare", label: "입력 데이터 준비 중" },
            { key: "request", label: "AI 처리 요청 중" },
            { key: "generating", label: "결과 생성 중" },
            { key: "waiting", label: "서버 응답 대기 중" },
            { key: "response", label: "응답 데이터 확인 중", afterResponse: true },
            { key: "render", label: "결과를 화면에 반영하는 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });
    registerPreset("ai-transcription", {
        title: "전사 작업 실행 중",
        message: "음성 또는 영상 내용을 분석하고 있습니다.",
        preset: "ai-long-running"
    });
    registerPreset("ai-document", {
        title: "문서 생성 중",
        message: "문서 내용을 생성하고 있습니다.",
        preset: "ai-long-running"
    });
    registerPreset("export", {
        title: "파일 내보내기 중",
        message: "내보낼 파일을 준비하고 있습니다.",
        steps: [
            { key: "prepare", label: "내보내기 조건 확인 중" },
            { key: "waiting", label: "파일 생성 요청 중" },
            { key: "response", label: "생성 결과 확인 중", afterResponse: true },
            { key: "render", label: "다운로드 준비 중", afterResponse: true },
            { key: "done", label: "완료 준비 중", afterResponse: true }
        ]
    });

    const resolveOptions = (options = {}) => {
        const presetName = options.preset || DEFAULT_OPTIONS.preset;
        const preset = selectPreset(presetName);
        const merged = {
            ...DEFAULT_OPTIONS,
            ...preset,
            ...options
        };
        const sourceSteps = Object.prototype.hasOwnProperty.call(options, "steps")
            ? options.steps
            : Object.prototype.hasOwnProperty.call(preset, "steps")
                ? preset.steps
                : defaultSteps;
        merged.steps = normalizeSteps(sourceSteps);
        return merged;
    };

    const selectPreset = (presetName) => {
        const preset = presetStore.get(presetName) || {};
        if (preset.preset && preset.preset !== presetName) {
            return {
                ...selectPreset(preset.preset),
                ...preset
            };
        }
        return preset;
    };

    const ensureOverlay = () => {
        if (overlay) {
            return overlay;
        }

        const root = document.createElement("section");
        root.className = "app-loading-overlay";
        root.hidden = true;
        root.setAttribute("aria-live", "polite");
        root.setAttribute("aria-label", "처리 상태");

        const card = document.createElement("article");
        card.className = "app-loading-card";
        card.setAttribute("role", "status");

        const head = document.createElement("div");
        head.className = "app-loading-head";
        const indicator = document.createElement("div");
        indicator.className = "app-loading-spinner";
        indicator.setAttribute("aria-hidden", "true");
        const titleGroup = document.createElement("div");
        const title = document.createElement("strong");
        title.className = "app-loading-title";
        title.dataset.loadingTitle = "true";
        const message = document.createElement("p");
        message.className = "app-loading-message";
        message.dataset.loadingMessage = "true";
        titleGroup.append(title, message);
        head.append(indicator, titleGroup);

        const progressWrap = document.createElement("div");
        progressWrap.className = "app-loading-progress";
        progressWrap.hidden = true;
        progressWrap.setAttribute("aria-hidden", "true");
        const progressBar = document.createElement("div");
        progressBar.className = "app-loading-progress-bar";
        progressBar.dataset.loadingProgressBar = "true";
        progressWrap.append(progressBar);
        const progressText = document.createElement("small");
        progressText.className = "app-loading-progress-text";
        progressText.dataset.loadingProgressText = "true";

        const steps = document.createElement("ol");
        steps.className = "app-loading-steps";
        steps.dataset.loadingSteps = "true";

        const error = document.createElement("p");
        error.className = "app-loading-error";
        error.dataset.loadingError = "true";
        error.hidden = true;

        const actions = document.createElement("div");
        actions.className = "app-loading-actions";
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "secondary-action app-loading-cancel";
        cancel.dataset.loadingCancel = "true";
        cancel.textContent = "취소";
        cancel.hidden = true;
        actions.append(cancel);

        card.append(head, progressWrap, progressText, steps, error, actions);
        root.append(card);
        document.body.append(root);
        overlay = root;

        cancel.addEventListener("click", () => cancelActive());
        window.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                cancelActive();
            }
        });
        return overlay;
    };

    const currentState = () => activeToken == null ? null : stateStore.get(activeToken);

    const activateState = (token) => {
        activeToken = token;
        const state = stateStore.get(token);
        if (!state) {
            render();
            return;
        }
        if (state.delayTimer) {
            return;
        }
        state.visible = true;
        state.shownAt = Date.now();
        if (state.options.autoAdvance) {
            startAutoAdvance(state);
        }
        render();
    };

    const show = (options = {}) => {
        const resolved = resolveOptions(options);
        const token = `loading-${++tokenSequence}`;
        const state = {
            token,
            options: resolved,
            stepIndex: 0,
            progress: null,
            visible: false,
            shownAt: 0,
            failed: false,
            errorMessage: "",
            responseArrived: false,
            delayTimer: null,
            autoTimer: null
        };
        stateStore.set(token, state);
        activeToken = token;

        if (resolved.delayMs > 0) {
            state.delayTimer = window.setTimeout(() => {
                state.delayTimer = null;
                if (stateStore.has(token)) {
                    activateState(token);
                }
            }, resolved.delayMs);
        } else {
            activateState(token);
        }
        return token;
    };

    const startAutoAdvance = (state) => {
        clearAutoTimer(state);
        state.autoTimer = window.setInterval(() => {
            const maxIndex = selectAutoMaxIndex(state);
            if (state.stepIndex < maxIndex) {
                state.stepIndex += 1;
                render();
            }
        }, state.options.autoAdvanceMs);
    };

    const selectAutoMaxIndex = (state) => {
        if (!state.options.autoAdvanceStopBeforeLast) {
            return state.options.steps.length - 1;
        }
        const firstAfterResponse = state.options.steps.findIndex((step) => step.afterResponse);
        if (firstAfterResponse < 0) {
            return Math.max(0, state.options.steps.length - 2);
        }
        return Math.max(0, firstAfterResponse - 1);
    };

    const clearAutoTimer = (state) => {
        if (state?.autoTimer) {
            window.clearInterval(state.autoTimer);
            state.autoTimer = null;
        }
    };

    const setStep = (token, stepKeyOrIndex) => {
        const state = stateStore.get(token);
        if (!state) {
            return;
        }
        const index = typeof stepKeyOrIndex === "number"
            ? stepKeyOrIndex
            : state.options.steps.findIndex((step) => step.key === stepKeyOrIndex);
        if (index < 0) {
            return;
        }
        state.stepIndex = Math.min(index, state.options.steps.length - 1);
        if (state.options.steps[state.stepIndex]?.afterResponse) {
            state.responseArrived = true;
        }
        render();
    };

    const setProgress = (token, progress) => {
        const state = stateStore.get(token);
        if (!state) {
            return;
        }
        const loaded = Number(progress?.loaded);
        const total = Number(progress?.total);
        if (!Number.isFinite(loaded) || !Number.isFinite(total) || total <= 0) {
            state.progress = null;
            render();
            return;
        }
        state.options.mode = "determinate";
        state.progress = {
            loaded: Math.max(0, loaded),
            total,
            ratio: Math.max(0, Math.min(1, loaded / total))
        };
        render();
    };

    const markResponse = (state) => {
        const firstAfterResponse = state.options.steps.findIndex((step) => step.afterResponse);
        state.responseArrived = true;
        clearAutoTimer(state);
        if (firstAfterResponse >= 0 && state.stepIndex < firstAfterResponse) {
            state.stepIndex = firstAfterResponse;
        }
        render();
    };

    const hide = (token) => {
        const state = stateStore.get(token);
        if (!state) {
            return Promise.resolve();
        }
        clearTimers(state);
        if (!state.visible) {
            stateStore.delete(token);
            selectNextActive();
            return Promise.resolve();
        }
        if (!state.failed) {
            markResponse(state);
        }
        return walkFinishSteps(state).then(() => {
            const remaining = Math.max(0, state.options.minVisibleMs - (Date.now() - state.shownAt));
            return wait(remaining);
        }).then(() => {
            stateStore.delete(token);
            selectNextActive();
        });
    };

    const walkFinishSteps = async (state) => {
        if (state.failed) {
            return;
        }
        const steps = state.options.steps;
        const firstAfterResponse = steps.findIndex((step) => step.afterResponse);
        if (firstAfterResponse < 0) {
            return;
        }
        for (let index = Math.max(state.stepIndex, firstAfterResponse); index < steps.length; index += 1) {
            state.stepIndex = index;
            render();
            await wait(index === steps.length - 1 ? 120 : 180);
        }
    };

    const fail = (token, error) => {
        const state = stateStore.get(token);
        if (!state) {
            return;
        }
        clearTimers(state);
        state.failed = true;
        state.visible = true;
        state.errorMessage = selectErrorMessage(error);
        activeToken = token;
        render();
    };

    const selectErrorMessage = (error) => {
        if (!error) {
            return "요청 처리에 실패했습니다.";
        }
        if (typeof error === "string") {
            return error;
        }
        return error.message || "요청 처리에 실패했습니다.";
    };

    const withLoading = async (task, options = {}) => {
        const token = show(options);
        try {
            const result = await task(token);
            const state = stateStore.get(token);
            if (state) {
                markResponse(state);
            }
            await hide(token);
            return result;
        } catch (error) {
            fail(token, error);
            throw error;
        }
    };

    const loadingFetch = (input, init = {}, options = {}) => withLoading(async (token) => {
        const response = await window.fetch(input, init);
        const state = stateStore.get(token);
        if (state) {
            markResponse(state);
        }
        return response;
    }, options);

    const upload = (url, formData, options = {}) => {
        const token = show({
            preset: "upload",
            ...options
        });
        setStep(token, "uploading");
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open(options.method || "POST", url);
            xhr.withCredentials = options.withCredentials !== false;
            Object.entries(options.headers || {}).forEach(([name, value]) => {
                xhr.setRequestHeader(name, value);
            });
            xhr.upload.onprogress = (event) => {
                if (event.lengthComputable) {
                    setProgress(token, {
                        loaded: event.loaded,
                        total: event.total
                    });
                }
            };
            xhr.onload = () => {
                const state = stateStore.get(token);
                if (state) {
                    markResponse(state);
                }
                hide(token).then(() => resolve(xhr));
            };
            xhr.onerror = () => {
                const error = new Error("파일 업로드에 실패했습니다.");
                fail(token, error);
                reject(error);
            };
            xhr.onabort = () => {
                const error = new Error("파일 업로드가 취소되었습니다.");
                fail(token, error);
                reject(error);
            };
            const state = stateStore.get(token);
            if (state) {
                state.xhr = xhr;
            }
            xhr.send(formData);
        });
    };

    const cancelActive = () => {
        const state = currentState();
        if (!state || !state.options.cancelable) {
            return;
        }
        if (state.xhr) {
            state.xhr.abort();
        }
        if (typeof state.options.onCancel === "function") {
            state.options.onCancel(state.token);
        }
        fail(state.token, new Error("요청이 취소되었습니다."));
    };

    const clearTimers = (state) => {
        if (state.delayTimer) {
            window.clearTimeout(state.delayTimer);
            state.delayTimer = null;
        }
        clearAutoTimer(state);
    };

    const selectNextActive = () => {
        const tokens = Array.from(stateStore.keys());
        activeToken = tokens.length > 0 ? tokens[tokens.length - 1] : null;
        const state = currentState();
        if (state && !state.visible && !state.delayTimer) {
            activateState(state.token);
        } else {
            render();
        }
    };

    const render = () => {
        const root = ensureOverlay();
        const state = currentState();
        if (!state || !state.visible) {
            root.hidden = true;
            document.body.classList.remove("app-loading-active");
            return;
        }
        root.hidden = false;
        document.body.classList.add("app-loading-active");
        root.classList.toggle("is-failed", state.failed);
        root.querySelector("[data-loading-title]").textContent = state.failed ? "처리 실패" : state.options.title;
        root.querySelector("[data-loading-message]").textContent = state.failed
            ? "아래 내용을 확인한 뒤 다시 시도하세요."
            : state.options.message;
        renderProgress(root, state);
        renderSteps(root, state);
        const error = root.querySelector("[data-loading-error]");
        error.hidden = !state.failed;
        error.textContent = state.failed ? state.errorMessage : "";
        const cancel = root.querySelector("[data-loading-cancel]");
        cancel.hidden = !state.options.cancelable || state.failed;
    };

    const renderProgress = (root, state) => {
        const progressWrap = root.querySelector(".app-loading-progress");
        const progressBar = root.querySelector("[data-loading-progress-bar]");
        const progressText = root.querySelector("[data-loading-progress-text]");
        const determinate = state.options.mode === "determinate" && state.progress;
        progressWrap.hidden = !determinate;
        progressText.hidden = !determinate;
        if (!determinate) {
            progressBar.style.width = "0%";
            progressText.textContent = "";
            return;
        }
        const percent = Math.round(state.progress.ratio * 100);
        progressBar.style.width = `${percent}%`;
        progressText.textContent = `${formatBytes(state.progress.loaded)} / ${formatBytes(state.progress.total)} (${percent}%)`;
    };

    const renderSteps = (root, state) => {
        const list = root.querySelector("[data-loading-steps]");
        list.replaceChildren();
        state.options.steps.forEach((step, index) => {
            const item = document.createElement("li");
            item.classList.toggle("is-done", index < state.stepIndex);
            item.classList.toggle("is-active", index === state.stepIndex);
            item.classList.toggle("is-pending", index > state.stepIndex);
            const marker = document.createElement("span");
            marker.className = "app-loading-step-marker";
            marker.textContent = index < state.stepIndex ? "✓" : String(index + 1);
            const label = document.createElement("span");
            label.textContent = step.label;
            item.append(marker, label);
            list.append(item);
        });
    };

    const formatBytes = (value) => {
        if (value >= 1024 * 1024) {
            return `${(value / 1024 / 1024).toFixed(1)}MB`;
        }
        if (value >= 1024) {
            return `${(value / 1024).toFixed(1)}KB`;
        }
        return `${Math.round(value)}B`;
    };

    const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));

    window.AppLoading = {
        show,
        hide,
        withLoading,
        setStep,
        setProgress,
        fail,
        registerPreset,
        fetch: loadingFetch,
        upload
    };
})();
