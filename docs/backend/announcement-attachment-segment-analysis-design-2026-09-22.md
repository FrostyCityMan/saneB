# 첨부 문서 구간별 근거 분석 설계

작성일: 2026-09-22. 상태: 사용자 설계·구현 승인, 구현·검증 진행 중.

## 1. 목적과 승인 경계

공고·안내·신청서가 한 파일에 섞였다는 이유만으로 모든 정보를 검수로 보내지 않고,
문서 안의 역할별 구간과 그 구간 안의 신뢰 가능한 문단·표 근거로 판정한다.
기존 파일 단위 `document-role-1.0.2` 결과 및 봉인된 set/evaluation은 수정하지 않는다.
운영 정책 게시·활성화, 기존 데이터 재분석, 운영 배포는 이 설계 승인만으로 실행하지 않는다.

실제 출발점: e365fd8의 파일 단위 판정은 혼합 역할을 `UNKNOWN/MIXED_DOCUMENT_ROLES`로 반환한다.
PDF 추출기는 페이지 block을 `scopeReliable=false`로 반환한다. 보은 격리 QA의 성공은
수집·추출·임시 DB/API 성공이지, 정상 후보 판정이나 이 설계의 실파일 검증 완료가 아니다.

## 2. 변경하지 않는 정책

- 제목 1차 → 본문 2차 → 전체 첨부 3차 → 관리자 최종 검증 순서를 유지한다.
- 승인된 제목 제외 조건은 원문 저장·사용자 노출·본문/첨부 요청 전에 종료한다.
- 제목 A, 본문 A/B, 첨부 A/B의 기존 최종 검수 정책을 유지한다. 첨부 B를 제목 자동 제외로 승격하지 않는다.
- 파일명·URL·다운로드 이름은 역할 또는 지원대상/지원형태의 내용 근거가 아니다.
- 공고/안내 구간만 긍정 조합 근거로 사용한다. 신청서/FAQ는 참고 근거이며 통과 또는 제외 근거로 사용하지 않는다.
- 미확인 구간·누락·추출 실패·부정 조건·상충을 숨기지 않는다. 준비된 후보도 관리자 확정이나 ACTIVE 상태가 아니다.
- 기존 `/api/v1`, V1~V83, 기존 파일 역할 수동 변경 및 이력의 의미를 보존한다.

## 3. 구간과 조합 범위의 구분

`segment`는 문서 역할을 설명하는 범위이고, `evidenceScopeId`는 키워드 AND를 허용하는 최소 범위다.
구간 전체가 하나의 AND 범위가 되는 것은 아니다.

1. 전체 추출 텍스트의 code point 좌표 `[0, textLength)`를 유지한다.
2. 제목으로 인식되는 **한 줄 전체**가 기존 신뢰 가능한 block 안에 있을 때 구간 경계 후보로 삼는다.
3. 문서 중간의 공고/안내/신청서/FAQ 표제도 경계로 인식한다. 같은 역할의 새 표제도 새 구간이다.
4. 경계 후보 이후 해당 구간 안에서 역할의 필수 구조를 검증한다. 제목 문자열 하나만으로 역할을 확정하지 않는다.
5. 표제 이전 원문, 구조 미확정 구간은 `UNKNOWN`으로 보존한다. 비어 있지 않은 원문을 버려 완전성을 만들지 않는다.
6. 기존 block을 구간 경계에서 잘라 참조할 수 있지만, block 사이 또는 구간 사이를 새 AND 범위로 합치지 않는다.
7. 원본 block index, 원본 code point 시작/끝, 역할 근거 위치를 저장한다. 재배열하거나 UTF-16 위치로 대체하지 않는다.

역할별 필수 근거:

| 역할 | 구간 내 필수 구조 | 분류 적용 |
|---|---|---|
| NOTICE/GUIDE | 구간 초기 표제 + 지원대상/신청자격 + 지원내용/규모 + 신청/접수기간 | 신뢰 가능한 개별 scope에서 조합·A/B·부정 문맥 검사 |
| FORM | 신청서류 표제 + 신청인/등록번호/성명 + 서명/인 | CONTEXT_ONLY |
| REFERENCE | FAQ 표제 + 질문 + 답변 | CONTEXT_ONLY |
| UNKNOWN | 위 조건 미충족 또는 구조 불명확 | 검수 유지, 누락된 이유와 위치 표시 |

분석 한도는 파일당 100만 code point, 2만 block/줄, 200개 구간이다.
한도 초과는 잘린 성공 결과가 아니라 전체 범위를 가진 `UNKNOWN/SEGMENT_ANALYSIS_LIMIT`이다.
분할은 텍스트 완전성을 보증하지 않는다. `PARTIAL_TEXT`는 구간이 잘 나뉘어도 검수가 필요하다.

## 4. PDF 구조 검증

페이지에 텍스트가 있다는 이유로 `scopeReliable=true`를 설정하지 않는다.
새 추출기 버전에서 검증한 문단/표 행만 조합 범위로 제공한다. 우선 검증 대상은 구조 태그가
실제 텍스트 MCID·페이지·읽기 순서와 일대일로 결합된 문단과 표 행이다.
태그 없는 문서, 다단 충돌, 겹침, 누락 MCID, 중복 참조, 그림/Form XObject, OCR 필요,
연결되지 않은 셀/표/페이지는 임의로 연결하지 않고 불확실 근거로 남긴다.
표 전체를 AND 범위로 사용하지 않으며 서로 다른 행의 대상/지원 조건을 합치지 않는다.
새 구조 추출이 기존 추출 결과를 덮어쓰지 않도록 extractorVersion/configHash와 새 extraction을 사용한다.

PDF 문단·표 구조 추출기는 별도 구현/실파일 검증 Gate다. 구간 분석기만 추가한 상태를 PDF 자동 판정 완료로 보고하지 않는다.

## 5. DB 계약 — additive V84

`announcement_attachment_segment_analyses`는 기존 extraction에 결합한 별도 append-only 분석 이력이다.
file/set/source/content/policy는 기존 FK 연결로 추적한다. 원문을 분석 JSON이나 감사 metadata에 복제하지 않는다.

- `id`, `extraction_id`, `file_id`, `set_id`, `source_id`
- `analysis_version`, `rules_hash`, `text_hash`, `blocks_hash`
- `analysis_json`: `analysisVersion`, `rulesHash`, `textHash`, `blocksHash`, `textLength`, `statusCode`, `reasonCode`, `segments`만 저장
- 구간: `index`, `startOffset`, `endOffset`, `roleCode`, `reasonCode`, `evidence`; 근거: `ruleCode`, `blockIndex`, `startOffset`, `endOffset`
- `statusCode=RESOLVED`는 모든 구간의 역할 구조가 확인되었다는 뜻이며, 키워드 분류 통과·공고 확정·운영 반영을 뜻하지 않는다.
- `created_at`
- `(extraction_id, analysis_version, rules_hash)` 유일성: 같은 버전의 재요청은 같은 분석을 반환한다.
- extraction의 textHash/blocksHash와 일치해야 한다. 기존 V82의 canonical block hash를 재사용한다.
- 분석 JSON의 구간은 순서대로 전체 범위를 빠짐없이 덮고, evidence는 해당 구간과 실제 reliable block 안에 있어야 한다.
- 수정/단독 삭제 금지. 기존 원문 삭제에 따른 cascade만 보존한다. 새 분석 버전은 새 행을 만든다. 기존 set/evaluation/role/추출 원문을 변경하지 않는다.

초기 V84 구현은 **독립 분석/SHADOW**다. 분석 행이 있다고 기존 evaluation을 변경하거나 후보를 활성화하지 않는다.
후속 V85는 새 엔진의 evaluation input에 `segment_analysis_id`, match에 `segment_analysis_id/segment_index`를 추가한다.
복합 FK로 동일 source/set/file/extraction/input을 결합하고 정책의 segment 버전·hash와 대조한다.
모든 파일을 입력으로 저장해야 하며 실패/미확인 구간을 제거한 ACCEPTED는 거부한다.
FORM/REFERENCE/UNKNOWN 및 고정 파일 역할 충돌은 CONTEXT_ONLY만 허용한다. 위치는 원본 block과 구간의 교집합 안이어야 한다.
새 결합의 수정·단독 삭제는 금지하며 원문 삭제 cascade는 보존한다. V1~V84는 수정하지 않는다.
평가에 서버가 기록한 `segment_binding_xid`를 사용하여 입력·match의 사후 추가도 거부한다. 새 평가 생성 transaction에서만 근거를 추가할 수 있다.

worker 경로는 실행 snapshot에 `attachment-segment-1.0.0`과 `segmentRuleVersion/segmentRulesHash`가 모두 고정된 경우에만 새 분석을 사용한다.
기존 정책에서 segment 설정이 없으면 기존 JSON/manifest/해시/파일 단위 경로를 유지한다.
신규 manifest schemaVersion은 3이다. 분석은 CPU 단계에서 계산하고 source/job lease fence를 다시 확인한 transaction에서 저장한다.
정책 생성은 아직 기존 엔진을 사용하며 정책 게시 QA는 신규 엔진을 거부한다. 새 golden/provider QA가 연결되기 전 활성화 우회 경로를 추가하지 않는다.

## 6. API 및 UI 계약

새 분석 API는 `/api/v2/admin/announcement-sources/{sourceId}/attachment-extractions/{extractionId}/segment-analysis`다.

- GET: 저장된 현재 분석 버전 조회. 없으면 미분석 상태를 반환한다. GET으로 생성·재분류하지 않는다.
- POST: 서버에 저장된 해당 source의 extraction으로만 분석을 생성한다. 임의 텍스트·역할·구간 좌표를 요청받지 않는다.
- POST 허용 역할 ADMIN/OPERATOR, GET은 ADMIN/OPERATOR/APPROVER. 서버 권한 검증, CSRF 및 no-store 유지.
- 응답은 ApiResponse. file/extraction 식별자, version/hash, 구간 좌표·역할·reason/evidence를 제공한다.
- `applicationMode=SHADOW`는 이 분석 API 호출 자체가 종합 판정을 적용하지 않는다는 뜻이다. 분석 행이 별도 worker evaluation에 참조된 적이 없는지를 뜻하지 않는다.
- 기존 v1과 기존 파일 role/roleAssessment 필드는 의미를 바꾸지 않는다.
- 수동으로 정해진 파일 역할은 새 자동 분석으로 덮어쓰지 않는다. 종합 판정 연결 시 충돌은 검수 사유다.

관리자 화면은 파일 역할과 구간 역할을 나란히 보여야 한다. 구간 전체를 '자동 승인'으로 표시하지 않는다.
공고·안내 근거, 서식 참고, 미확인 범위를 구분하고 원문 위치로 연결한다.
화면 구현은 저장/조회 API 계약 검증 이후 수행한다. 현재 요청에서 브라우저 검증은 별도 지시 전 실행하지 않는다.

## 7. 검증과 성공/실패 기준

- [ ] SEG-001 혼합 공고+신청서가 NOTICE/FORM 구간으로 분리되고 전체 code point를 보존한다.
- [ ] SEG-002 앞부분 미확인, 역할 필수 구조 부족, 신뢰 불가 scope는 검수로 남는다.
- [ ] SEG-003 구간/파일/표 행 사이 AND 금지, FORM A/B는 참고만, NOTICE/GUIDE A/B는 기존 검수 정책 유지.
- [ ] SEG-004 이모지/한글/CRLF·공백, block 경계, 한도, 위조 근거 및 해시 변조 검증.
- [ ] SEG-005 DB FK·유일성·불변성·전체 범위·원문/hash 결합 실 PostgreSQL 검증.
- [ ] SEG-006 API 인증/권한/CSRF/멱등성/source 범위·JSON 원문 미복제 검증.
- [ ] SEG-007 worker/정책/manifest/QA pinning 및 기존 정책 무변경 회귀 검증.
- [ ] SEG-008 PDF 문단·표 구조 추출, 모호한 PDF fail-closed 및 실제 파일 검증.
- [ ] SEG-009 관리자 화면과 최종 검증/초안/자동 활성화 금지 검증.
- [ ] SEG-010 전체 테스트·bootJar·Linux QA. 운영 적용과 브라우저 검증은 승인/실행 증거를 별도 기록.

전체 필수 항목 충족 전 '구간별 분석 구현 완료' 또는 전체 goal 완료를 선언하지 않는다.
UNKNOWN을 제거하거나 기대 결과만 바꿔 테스트를 통과시키는 것은 실패다.
기존 문서의 혼합 파일 UNKNOWN 설명은 과거 버전의 사실로 유지하고 이 문서를 새 버전 설계로 연결한다.

## 8. 구현 연결 계획과 현재 범위

현재 추가한 독립 분석기는 `segment-role-1.0.0`, 구간 종합 엔진은 `attachment-segment-1.0.0`이다.
기존 `attachment-1.0.0`의 공개 진입점은 파일 단위 역할을 그대로 사용한다.
신규 엔진은 모든 파일의 원문·block·분석 재현성을 검사한 후 구간/block 교집합만 분류한다.
FORM/REFERENCE는 참고, UNKNOWN은 검수다. MANUAL/PROFILE 파일 역할과 구간 역할 충돌은
`ATTACHMENT_SEGMENT_ROLE_CONFLICT`로 검수를 유지하며 충돌 구간을 긍정 근거로 사용하지 않는다.

연결별 현재 상태:

1. [~] 정책 `Configuration`/`AttachmentExecutionSnapshot`에 새 segment 버전·hash를 선택적으로 결합했다.
   미설정 정책은 구 버전과 같은 JSON/실행 경로를 유지한다. 오래된 작업을 새 엔진으로 조용히 재실행하지 않는다.
2. [~] 예약·재시도·배치·수집/worker 버전 비교를 확장했다. 새 정책 QA는 별도 연결 전 차단하며 code hash를 이전 승인 결과로 대체하지 않는다.
   후속 연결 대상은 `AnnouncementAttachmentPolicyGoldenGate`, `AnnouncementAttachmentServerQa`, 정책 검증 snapshot/게시 verifier 및 Provider 고정 기대값이다.
   구간 엔진의 혼합 문서·다른 문단/파일 AND 금지·부분 추출·수동 역할 충돌 시험을 고정한 뒤에만 정책 기본 엔진과 게시 검증을 전환한다.
3. [~] worker 봉인 후 동일 extraction의 분석을 생성하고 종합 평가의 입력·판정 지문에 결합했다. 실제 DB/전체 회귀 검증 중이다.
   원문 없는 실패 파일은 분석에서 제거하지 않고 기존 실패 우선순위를 유지한다.
4. [~] V85로 새 evaluation input과 각 match에 segment 분석/구간의 복합 FK·불변 결합을 추가했다. 실제 PostgreSQL 검증 중이다.
   V84 SHADOW 분석 행만 추가했다고 기존 evaluation의 해시/판정을 덮어쓰지 않는다.
5. [ ] 관리자 화면은 파일 역할과 구간 역할을 구분해 보여주고, 단계별 부족한 근거를 표시한다.
6. [ ] PDF 구조 추출 및 합성/실파일 QA, Linux 실제 DB와 전체 회귀를 통과한 뒤 운영 적용 범위를 별도로 확인한다.

전체 goal은 계속 진행 중이다. 이 기반 구현만으로 SEG-007~010이나 기존 ATT/Gate를 완료 처리하지 않는다.

## 9. 이번 구현 검증 기록

- [x] 독립 구간 분석·구간 종합 엔진·SHADOW 저장/조회 API의 로컬 구현을 추가했다.
- [x] 전체 로컬 `:test :attachment-extractor:test :bootJar --no-daemon --max-workers=1`: 8분22초 성공.
  애플리케이션 2819건 중 통과2549/조건부 생략270/실패·오류0, 추출기90/90 통과.
- [x] 마지막 경계/식별자/DB Mapper 보강 후 대상 재검증과 bootJar: 2분25초 성공.
  256건 중 통과252/실제 DB 조건부 생략4/실패·오류0. 전체 XML은 `build/qa-results/segment-local-full-20260922-1617`에 별도 보존했다.
- [x] `git diff --check`; 기존 V1~V83 변경 없음. `output/` 사용자 문서 변경 없음.
- [x] 42f8cb5의 Linux35699149911 일반 PostgreSQL migration suite는 4/4 통과했다. 그러나 독립 namespace 실행은 222건 중 221통과/1실패, 정책 부모도 실패하여 CI 전체는 failure다.
- [~] 실패 case hash를 새 구간 분석 DB 테스트로 특정했다. 계측 attach를 요구하는 Mockito 대신 무호출 시 실패하는 JDK proxy로 수정했으며 독립 실행 재검증 전 해결 완료로 보지 않는다.
- [x] 로컬 `:attachmentMigrationTest :attachmentJobIntegrationTest` 18분2초 성공. 임시 PostgreSQL migration4/4·기존 데이터 배치14/14·작업194/194, 실패/오류/생략0을 XML로 확인했다. V84 저장 분석 행을 유지한 V85 upgrade·fresh schema, 새 구간 worker 평가/근거 변조·사후 추가 거부/실패 파일 유지/cascade도 포함한다. Docker 없이 이 경로를 사용할 수 있음을 직접 확인했다. Linux namespace 실행 증거와는 구분한다.
- [~] 코드6933ef3의 Linux35703442168 실행 중. 운영 DB 적용이나 독립 Linux QA 통과로 미리 간주하지 않는다.
- [x] 연결 보완 후 대상9suite·218건 실패/오류/생략0 및 bootJar 성공(1분9초). 추출기 시험은 UP-TO-DATE 재사용이다. 수정 전 전체 로컬 준비 테스트4실패는 Mapper 등록 누락으로 보완했으며 전체 재실행은 새 Linux CI의 결과를 별도 확인한다.
- [~] worker/manifest/V85 연결을 구현했고 전체 회귀에서 테스트용 Mapper 등록 누락을 발견해 보완했다. 정책 QA·UI 연결, PDF 구조 추출, 실제 파일 재검증, 운영 반영은 미완료다.
- [ ] 브라우저 검증은 현재 요청에 대한 명시 지시가 없어 정책상 미실행이다.

SEG-001~004의 로컬 합성/회귀 증거는 확보했으나 DB·실파일·worker·UI까지의 확장 완료로 간주하지 않는다.
