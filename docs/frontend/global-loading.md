# 전역 로딩 오버레이 사용 지침

`/js/saneb-loading.js`는 화면 어디서나 사용할 수 있는 전역 처리 상태 컴포넌트입니다.

## 기본 사용

```javascript
await window.AppLoading.withLoading(
    () => requestJson("/api/v1/example", { method: "GET" }),
    {
        preset: "default-api",
        title: "정보 불러오는 중",
        message: "서버 응답을 기다리고 있습니다."
    }
);
```

## 저장 요청

```javascript
await window.AppLoading.withLoading(
    () => requestJson("/api/v1/example", {
        method: "PUT",
        body: JSON.stringify(payload)
    }),
    {
        preset: "save",
        title: "정보 저장 중",
        message: "입력한 내용을 서버 검증 후 저장하고 있습니다."
    }
);
```

## 파일 업로드

```javascript
const formData = new FormData(form);
const xhr = await window.AppLoading.upload("/api/v1/files", formData, {
    cancelable: true
});
```

업로드 진행률은 브라우저가 `lengthComputable=true`를 제공할 때만 표시합니다.
일반 조회, 저장, AI 장시간 작업에는 임의 퍼센트나 가짜 진행률을 표시하지 않습니다.

## 제공 프리셋

- `default-api`
- `save`
- `delete`
- `search`
- `upload`
- `ai-long-running`
- `ai-transcription`
- `ai-document`
- `export`

## 상태 단계 정책

서버 응답 전에는 `응답 데이터 확인 중`, `결과를 화면에 반영하는 중`, `완료 준비 중` 단계를 표시하지 않습니다.
해당 단계는 실제 서버 응답이 도착한 뒤에만 진행합니다.
