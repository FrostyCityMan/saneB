# 수집원 QA의 형식 적용성 V2

## 목표와 변경 범위

제목 → 정제 본문 → 실제 PDF/HWP/HWPX 텍스트 → 관리자 최종 검증을 유지한다. 이 문서는 사이트별 수집 엔진이나 분류 규칙을 변경하는 것이 아니라, 고정한 공식 공고 표본의 **파일 형식 검증 범위**를 정확히 나타내기 위한 계약이다.

사용자 Gate6은 대상 profile별 실제 공고에서 발견되는 지원 형식과 다중 첨부 검증을 요구한다. 모든 기관에 세 형식이 반드시 존재한다고 가정하지 않는다. 반대로 한 번 발견하지 못했다고 미지원/첨부 없음/QA 통과로 판단하지 않는다. 전체 활성 지자체와 기업마당·정부24의 분모는 유지한다.

## 계약

1. 기존 catalog schema1은 각 대상의 PDF/HWP/HWPX 세 형식 요구와 직렬화 형태를 보존한다. 신규 schema2 리소스를 별도로 사용하며 과거 리소스·정책 snapshot·migration·v1 API를 수정하지 않는다.
2. schema2의 출처별 요구 형식은 현재 유효한 고정 실행 기대값의 **모든 다운로드 가능 파일**에서 계산한다. COMPLETE_TEXT만 보고 요구 형식을 정하지 않는다. PDF가 OCR_REQUIRED/PARTIAL_TEXT/암호/손상이어도 PDF는 검증 범위에 남고, 같은 대상의 완전 추출 PDF 기대값이 없으면 missingFormats에 남는다.
3. 표본에서 등장하지 않은 지원 형식은 unobservedFormats다. 뜻은 '이 고정 표본에 관측 기대값이 없음'이지 '이 사이트는 제공하지 않음'이 아니다. 참조만 있거나 유효한 파일 기대값이 없으면 EXPECTATIONS_UNKNOWN이다. 관리자가 형식을 임의 제외하는 필드는 없다.
4. 각 대상은 기존 정상 공고3건을 유지하고, 그중 전체 파일이 검증되는 정상 다중 첨부 공고를 최소1건 요구한다. UNKNOWN/양식만 존재/부분/OCR/발견 불완전/제목 차단/첨부 없음은 정상 공고 수와 정상 다중 첨부 수를 채우지 않는다. 음성 표본 자체는 기존 계약대로 실행할 수 있으며 실패 파일/전체 파일 분모를 삭제하지 않는다.
5. 전체 catalog에서는 PDF/HWP/HWPX 세 형식의 COMPLETE_TEXT 기대 coverage를 모두 요구한다. 기관별로 실제 발견되는 형식을 합산하되 한 기관의 완전 PDF가 다른 기관에서 발견된 실패 PDF를 상쇄하지 못한다.
6. 이 단계의 coverage는 **계획 기대값**이다. isQaPassed=false를 유지한다. 실제 최종 Gate는 기존 catalog/profile/runtime/규칙/scope/plan hash·원장·모든 분할/공고/파일 결과 재검증을 그대로 수행한다. 실제 목록/파일/버전 변경·미실행·실패를 계획상 성공으로 대체하지 않는다.

## DB / API 영향

- 신규 DDL이나 운영 데이터 수정은 없다. 기존 불변 snapshot/plan JSON과 hash에 신규 metadata를 포함하며 설치 코드·리소스 지문이 바뀐다. 과거 성공 근거를 새 catalog의 성공으로 재사용하지 않는다.
- schema2 catalog의 TargetPlan에 formatApplicability(상태, 기대 제공 형식, 미관측 형식, 정상 다중 첨부 수)를, 전체 Plan에 formatCoverage(고정 모드, 전체 필수/누락 형식)를 추가한다. URL/파일명/텍스트/개인정보는 포함하지 않는다.
- 구조적 provider-qa-plan API의 requiredFormats는 기존 schema1 계약을 보존한다. 실제 적용성 조회는 새 GET `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-runs/execution-plan/targets`로 분리한다. 기존 execution-plan·v1 API와 화면은 변경하지 않는다.
- 새 조회는 ADMIN/OPERATOR/APPROVER의 활성·비밀번호 변경 완료 계정만 허용하며 page>=1/size1~100, ApiResponse/내부 PageResponse/no-store를 유지한다. 전체 catalog/plan 지문과 전체 대상 수를 페이지와 분리한다. 현재 설치/snapshot 확인 불가는409이고 DB 쓰기·HTTP·파일 요청·예약·정책 게시를 실행하지 않는다. 현재 metadata는 화면에 연결하기 전 API 검증 단계다.
- schema1 직렬화에는 신규 null 필드를 추가하지 않아 과거 형식/계획 hash 표현을 보존한다. 새 서버가 현재 schema2 계획과 과거 snapshot을 비교하면 불일치로 차단한다.

## 검증 / 성공·실패 기준

- [x] schema1의 기존 정상/형식 요구·직렬화 유지.
- [x] 합성 파일 기대값으로 서로 다른 기관의 제공 형식, 한 형식 기관의 정상 다중 첨부, 전체3형식 coverage 검증.
- [x] 미관측·참조만 존재·만료·구조 불완전·UNKNOWN/부분/OCR·추가 미지원 파일이 정상 coverage를 부풀리지 않음.
- [x] 같은 기관 실패 형식을 다른 기관 성공으로 상쇄하지 않음.
- [x] 전체 대상/정상3건/모든 파일/분할/시간·요청·바이트 한도 유지.
- [x] 실제 근거 재검증 코드에 합성 원장 연결·metadata 변조 차단, resource15건 기대값0 유지. 실제 운영 원장 검증 아님.
- [x] catalog·근거 verifier·Service·MockMvc 권한/페이지·snapshot 표적156건 및 bootJar 통과(53초).
- [x] 전체 로컬 단위/계약·bootJar/QA 설치 패키지 검증. root2484=2232통과/252조건부 생략/실패0, 별도 QA 패키지20/20,3분18초 성공. 추출기25건은 UP-TO-DATE이며 이번 재실행 통과로 세지 않는다.
- [ ] 같은 SHA의 Linux 검증·실제 Provider/운영 검증. 직전 be45ddf 실행24의 성공을 이번 증분에 재사용하지 않는다.

성공은 위 계약 구현·검증 완료를 뜻한다. 실제 공식 기대값·운영 QA·정책 게시 성공을 뜻하지 않는다. 형식을 임의 N/A로 바꾸거나 실패/UNKNOWN을 정상 후보로 계산하면 실패다. 현재 전체 Gate/ATT는 Not ready이며 운영 활성화는 실행하지 않는다.

표적 명령:

```powershell
.\gradlew.bat :test --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderQaEvidenceGateTest' --tests '*AnnouncementAttachmentProviderQaManagementServiceTest' --tests '*AnnouncementAttachmentProviderQaManagementControllerSmokeTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' bootJar --no-daemon --max-workers=1
```

MockMvc는 서버 HTTP 계약 시험이며 실제 브라우저가 아니다. 이번 증분은 공식 파일 요청·DB migration 실행·운영 배포/정책/데이터 쓰기 없이 검증한다. 기존 운영 인증 만료와 전체 공식 표본/역할·내용 기대값 부족은 이 변경으로 해소되지 않는다.

최종 로컬 명령은 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`이다. bootJar SHA256은 `b1bdf80619b037c9678594204cffd371912db3ce8ea2644e7815a7dbc764c41a`다. Node 일회성 JSON 대조는 v1/v2의 공개 참조15개 동일·기대값0을 확인하고 종료했다. 기존 v1 리소스/migration diff는 없으며 사용자 output/Word2개를 보존한다.
