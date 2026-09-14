# 제목·본문·첨부 3단계 자동 필터링과 최종 관리자 검증

- 기준일: 2026-09-14. 사용자 확정 방향을 기존 첨부 장기 goal에 통합한다.
- 목적: 시스템이 텍스트를 먼저 선별·분류·대조하여 사람이 읽고 비교해야 하는 양을 줄인다.
- 설계 시작 기준선: master / ae893b87348a9bd1cb0893763f6cad5047093a24, 당시 로컬 V81. 최신 구현·운영 근거는 아래 증분과 장기 진행 기록을 따른다.
- 기존 [첨부 설계](announcement-attachment-collection-design-2026-09-08.md)의 처리 순서·화면 해석을 구체화한다. 기존 제목 제외, A/B 정책, v1, additive migration, 근거 불변·권한·배포 승인 경계는 유지한다.
- 상태: 설계 확정 후 증분 구현 진행. 전체 Gate0~8 / ATT-001~062의 완료 범위를 줄이지 않는다.

## 1. 확정 정책

최신 기관 증분: [보은군 BBS 본문·첨부 프로필](announcement-boeun-bbs-profile-2026-09-15.md)까지 추가했다. 전용 본문14기관, 첨부 엔진6/등록 프로필15/추출 형식3이다. 보은3공고3첨부(HWPX2/PDF1)의 BODY/다운로드/signature를 확인했다. catalog 참조18/실행 기대값0이며 정상 다중첨부/역할·정책 QA는 미완료다. 태백184816의 별도 Linux 관측은 본문431자·HWPX2개 완전 추출 후 UNKNOWN 사유를 보존한 REVIEW_REQUIRED다. 전체 정상 후보·운영 적용으로 계산하지 않는다. 아래 과거 수량은 각 증분 시점의 기록이다.

후속 [형식 적용성 V2](announcement-provider-format-applicability-v2-2026-09-15.md)는 기관별 고정 기대 파일의 제공 형식/미관측·정상 다중 첨부 수와 전체3형식 요구를 분리한다. schema1/기존 API/migration을 보존하며 새 v2 대상 페이지 조회를 추가했다. 제목/본문/첨부 분류 순서, 실패·UNKNOWN·자동 ACTIVE 금지는 변경하지 않는다. 참조15/실제 기대값0과 운영/브라우저 미완료 상태는 유지한다.

현재 [양식 역할 표식 보완](announcement-form-role-markers-2026-09-15.md)은 `document-role-1.0.2`를 명시한 새 정책만 대상으로 한다. 콜론 없는 입력 항목/괄호형 서명을 텍스트의 동일 파일·실제 위치로 확인하며 혼합 문서 UNKNOWN은 유지한다. 아래1.0.1 수치·공식 관측은 과거 버전 근거다. 새 버전의 실제 Linux 효과와 정책 기대값 승인은 별도로 검증한다.

1. 제목 텍스트 1차 → 정제된 본문 텍스트 2차 → 실제 첨부에서 추출한 텍스트 3차 → 관리자 최종 검증이다.
2. 제목 B 또는 대상·지원형태 조합 미충족(제목 A 예외)은 기존 제목 제외 정책으로 종료한다. 원문·첨부 작업을 새로 보관하거나 원문을 복구하지 않는다.
3. 제목 A·본문 부족·본문 A/B는 중간 사람 개입 요청이 아니다. 사유를 보관하고 가능한 첨부 분석까지 진행한다.
4. 본문이 충분해도 첨부가 있으면 3차 분석한다. 본문에 첨부 언급이 없어도 공식 첨부 영역을 확인한다.
5. 본문이 없거나 부족하면 첨부가 이를 보완할 수 있다. 제목 제외와 명시된 A/B 검수 정책은 첨부로 해제하지 않는다.
6. 본문·첨부 B는 현행대로 검수 대상이다. 모든 위치의 단순 B 포함을 자동 제외로 바꾸지 않는다.
7. 파일명·URL·미리보기 링크를 내용 판정에 쓰지 않는다. 같은 문맥 단위의 대상+지원형태 조합을 평가하며 서로 다른 파일의 단어를 합성하지 않는다.
8. 없음/미수집/진행/실패/불완전/정상 추출/관리자 확인은 서로 다른 사실이다. 오류·미지원 파일을 삭제하여 성공률을 높이지 않는다.
9. 관리자에게 전달할 결과는 지원대상·지원형태, 판정 이유, 실제 근거 위치, 미해결 쟁점이다. 후보는 최종 선정이 아니며 공고는 자동 ACTIVE가 되지 않는다.

## 2. 파이프라인과 상태

수집 run의 규칙 release·정책·프로필·추출기 버전을 고정한다. 본문 요청과 첨부 링크 확보를 같은 상세 응답에서 수행할 수 있지만, 판정의 의존 순서는 제목→본문→첨부다. 현재 worker가 상세를 재조회하는 경계도 유지한다.

| 단계 | 입력/산출 | 다음 동작 |
|---|---|---|
| 제목 | 제목 text → 기존 base title stage | 제외 종료, A 또는 조합 통과는 계속 |
| 본문 | 공식 본문 정제 text → base evaluation/근거 | 충분·부족·검수 사유 모두 보존하고 첨부로 계속 |
| 첨부 발견 | 공식 상세 첨부 영역 → 전체 descriptor | 승인 profile·호스트/경로 확인, 정상 0개와 발견 실패 분리 |
| 첨부 추출/판정 | 제한 다운로드 → PDF/HWP/HWPX text·block·품질 → 종합 evaluation | 전 파일 성공/실패 확정 후 SEALED, 동일 규칙으로 종합 |
| 최종 검증 | 현재 종합 근거·미해결 사항 | 준비된 후보는 근거 확인, 예외는 해당 쟁점 검토 |

재시도는 실패한 해당 기술 단계로 복귀한다. 추출 실패를 제목/본문부터 재분류하거나 관리자에게 다운로드 링크를 찾아오도록 요구하지 않는다. 제한된 자동 재시도 종료 후에도 필요한 근거가 없으면 기술 예외이며 정상 후보가 아니다.

### 2.1 DB-first: 계산 상태를 별도 판정 이력으로 저장하지 않음

V66의 base, V72의 set/evaluation/confirmation, V73 이후 job·intake·버전 정보를 source와 같은 조회 snapshot에서 읽는다. 이번 첫 증분은 DDL 없이 조회 projection으로 추가한다. 같은 상태의 중복 저장과 과거 이력 rewrite를 피한다.

v2 `Summary.processingFlow`는 `statusCode`, `isAutomaticAnalysisComplete`, `isFinalReviewAvailable`을 반환한다. 기존 `baseClassification`, `effectiveClassification`, `previewClassification`, `REVIEW_REQUIRED` 의미는 변경하지 않는다.

| 코드 | 의미 | 관리자 동작 |
|---|---|---|
| NOT_APPLIED | 첨부 종합 적용 대상 아님. OFF/COLLECT_ONLY 포함 | 기존 판정·미리보기 구분, 3차 완료로 표시 금지 |
| CLASSIFICATION_PENDING | base 판정 미확보 | 자동 처리 대기 |
| CONFIGURATION_REQUIRED | profile/기본 규칙/정책 결합 보완 필요 | 시스템 설정·검증 담당자가 해결 |
| AUTOMATIC_PROCESSING | 동일 입력 작업 PENDING/RUNNING/RETRY_WAIT 또는 종합 결과 대기 | 최종 검수 입력 요청하지 않음 |
| EVIDENCE_STALE | 현재 입력과 근거 불일치 | 최신 근거를 확보한 후 검증 |
| TECHNICAL_EXCEPTION | 발견/추출/작업 실패·불완전 | 자동 완료 아님. 봉인된 현재 평가가 있으면 기존 명시적 원문 수동 확인 절차만 가능 |
| READY_FOR_FINAL_REVIEW | 적용된 현재 종합 후보, 기술 미완료 없음 | 분류·핵심 근거를 최종 확인 |
| FINAL_REVIEW_EXCEPTION | 자동 분석 후 A/B·역할·문맥 등 쟁점 잔존 | 남은 쟁점 집중 검토 |
| FINAL_REVIEW_CONFIRMED | 현재 버전의 관리자 확인 존재 | 이미 확인된 근거를 다시 입력하지 않음 |

`isFinalReviewAvailable`은 권한·전환 승인이 아니다. 현재 SEALED evaluation과 버전 일치 및 active job 부재가 필요한 조회 힌트이며, 쓰기 시 기존 source lock·CAS·review-context·실패 확인·권한 검증을 그대로 재실행한다. 일반 작업 전체의 SCOPE_READY/PENDING/RUNNING/RETRY_WAIT/PAUSED를 검사한다. batch 미리보기는 기존 검수 차단 대상이 아니다. 기술 예외를 수동 확인해도 `isAutomaticAnalysisComplete`는 true로 바꾸지 않는다.

첫 증분은 목록/상세 API의 projection 및 상세 화면 연결이다. 두 번째 증분은 아래 읽기 전용 대기열 계약으로 구현한다. 클라이언트의 페이지 내 필터링으로 전체 건수를 꾸미지 않는다.

### 2.2 처리 상태별 대기열 계약 (P2)

- 기존 v2 목록에 선택 필터 `processingFlowStatusCode`를 추가한다. 위 9개 코드만 허용하고 공백은 필터 없음으로 처리한다. 미지 코드는 구체적인 400 응답이며 기존 필터·페이지·응답 의미는 보존한다.
- SQL의 공통 `ProcessingFlowStatus` CASE를 목록/count의 동일 `SearchWhere`에서 사용한다. source/base/policy/SEALED set/현재 confirmation/일반 작업 전체를 확인하는 기존 조인을 공유한다. count는 LIMIT 이전 전체 검색 범위이며 목록과 같은 REPEATABLE_READ 조회이다.
- Java projection과 SQL 필터의 우선순위 및 null·미지 사유 차단을 일치시킨다. Mapper 바인딩, 상태 행렬 SQL/Java 비교, 실제 목록/count/page 통합 회귀를 추가한다. PostgreSQL 미실행은 별도로 표시한다.
- `/app/admin/announcement-attachment-queue`를 기존 수집 공고 검수와 연결한다. 기본 필터는 `READY_FOR_FINAL_REVIEW`, 전체 보기와 9개 상태·수집처·검색어·페이지를 URL에 보존한다. APPROVER도 읽을 수 있으나 목록은 모든 역할에서 쓰기 요청을 제공하지 않는다.
- 주 행동은 공고별 `근거와 처리 상태 확인`이다. 오류/세션 만료/시간 초과는 검색 결과 0건으로 바꾸지 않으며, 필터를 유지하고 재조회한다. URL의 미지 필터도 임의로 정상 필터로 바꾸지 않는다.
- 확인한 서버 총건수·현재 페이지·자동 분석 완전성을 구분한다. 관리자 확인 완료이지만 자동 분석 불완전인 경우 두 사실을 함께 표시한다. 정상 후보의 자동 활성화·기존 데이터 재처리·첨부 재다운로드는 실행하지 않는다.

수용 기준: 준비 완료 필터에서 처리 중·기술 예외 0건, 선택 상태와 각 행의 `processingFlow.statusCode` 일치, count/페이지 경계 일치, 필터 변경 시 1페이지, 뒤로 가기 복원, 구체적인 실패 안내 및 XSS 방지. 실제 운영 데이터와 브라우저에서의 성공은 별도 Gate이다.

## 3. 관리자 화면

Design Read: 자동 분석이 끝난 공고의 분류와 남은 쟁점을 한 번에 확인하고, 처리 중·시스템 오류를 사람의 검수 업무와 구분한다.

- 사용자: ADMIN/OPERATOR 반복 검수, APPROVER 읽기 전용. 조회 R0, 검수/초안 전환 R1, 정책 게시·기존 데이터 대량 적용 R2.
- 환경 가정: 한국어/서울 시간, 데스크톱 중심·360px 조회, 키보드/마우스. 기존 Thymeleaf/Bootstrap 5·외부 JS·CSS 유지, 새 의존성 없음.
- 상단은 자동 처리 상태와 다음 행동, 아래는 종합 결과·쟁점·근거, 기본 TITLE/BODY 이력은 중간 근거로 표시한다.
- 자동 처리 중에는 '관리자 검수 필요'를 주 상태로 표시하지 않는다. 읽기·진행 조회는 유지하되 검수 저장/초안 생성은 기존 서버 guard와 일치하게 제한한다.
- TECHNICAL_EXCEPTION은 시스템 처리 실패로 안내한다. 기존 수동 원문 확인 경로는 자동 통과로 위장하지 않는다.
- loading/empty/error/partial/stale/permission/응답 유실·입력 보존·재시도·한글 사유·키보드·모바일 검증을 적용한다. 새로운 모션은 추가하지 않는다.
- 본문 부족이 첨부로 해소되면 base 이력은 보존하되 최종 미해결 사유로 반복 노출하지 않는다. 해결된 사실과 실제 첨부 근거를 함께 보여준다.

## 4. 검수 감소를 막는 구현 과제

1. 지자체 본문 추출은 실측한 태백25·횡성65·영월17 게시판에서 제목 표식과 단일 게시판 표의 `td[title=내용]`만 사용한다. 후속으로 서구·남구·달성·중구·함안·부산시·강북구·화천군의 전용 본문 영역을 추가했으며 상세 경계와 검증은 아래 증분 기록을 따른다. 구조 누락/중복은 `BODY_SELECTOR_CHANGED`이며 페이지 전체로 대체하지 않는다. 내부 결과는 기존 FETCH_FAILED로 전달되어 이후 첨부 분석을 막지 않는다. 나머지 대상은 기존 `main/[role=main]/article/body` 공통 탐색을 유지한다. 명시적인 `nav/[role=navigation]`는 선택 영역 안팎에서 제거하며 일반 본문 문장·실제 제외 조건·신청 링크는 보존한다. 전용11기관 외 본문 영역과 메뉴/푸터 오염의 전수 확인은 여전히 필요하다.
2. 첨부는 공통 구현 6종·등록 profile12개, 형식 추출기3종이다. 목록 파서 개수와 첨부 지원 개수를 혼동하지 않는다. 정부24와 남은 기관의 profile·실제 API 가용성 확인이 필요하다.
3. 현재 발견 profile의 역할은 UNKNOWN이다. 모든 파일을 NOTICE로 바꾸거나 filename만으로 역할을 확정하지 않는다. 공고문/안내문/양식/참고자료의 검증된 영역·문서 텍스트 증거에 대한 시스템 규칙과 공식 QA 기대값을 먼저 정의한다. 조건 미충족은 UNKNOWN을 유지한다.
4. 대상/형태 키워드의 부정·제외 문맥, 기관명 보호, 표 범위·문서 간 상충을 고정 사례로 검증한다. 의미를 임의 추론하는 AI·점수는 추가하지 않는다.
5. 실제 파일 기대값이 없는 catalog를 PASSED로 만들지 않는다. 각 사이트에 실제 제공되는 형식과 미확인 형식을 구분하는 적용성 계약이 필요하다.

## 5. 장기 goal 실행 순서

### P3 문서 역할 자동 식별의 연결 지점 (DB/worker/API/UI 구현, 실제 환경 검증 진행)

현재 코드 확인 결과, 분류기에 정규식만 추가해서는 전체 경로가 연결되지 않는다. 아래 순서로 한 증분을 완성해야 한다.

1. DB-first: V72의 파일 `role_origin_code`는 UNKNOWN/PROFILE/MANUAL만 허용한다. 텍스트 규칙 유래를 PROFILE이나 MANUAL로 위장하지 않고 additive migration으로 구분한다. 규칙 버전/지문, file·extraction·set·source의 같은 소속, 실제 block/문자 위치와 판정 근거 hash를 불변 연결한다. 원문 텍스트는 기존 extraction에만 두며 감사 metadata에 복사하지 않는다. 기존 봉인 집합과 확인 이력을 소급 변경하지 않는다.
2. 판정: COMPLETE_TEXT·유효한 근거 범위가 있을 때만 같은 파일 안의 문서 역할 조건을 평가한다. 파일명·URL·다른 파일의 키워드는 역할 근거가 아니다. 제목/본문의 A/B 의무는 해제하지 않는다. 내용이 부족하거나 역할이 상충하는 혼합 문서는 UNKNOWN으로 남긴다. NOTICE/GUIDE/FORM/REFERENCE의 고정 규칙과 공식 파일 기대값을 먼저 마련하며 모든 UNKNOWN을 NOTICE로 일괄 승격하지 않는다.
3. worker/checkpoint: 현재 `saveFileCheckpoint` 뒤 재개는 descriptor 역할과 저장 역할의 일치를 요구한다. 자동 역할을 도입하면 이 비교와 봉인 manifest에 규칙/근거 버전을 함께 연결해야 한다. 예전 checkpoint를 새 규칙의 결과로 재해석하지 않으며 버전 불일치는 명시적인 재처리 필요 상태다.
4. 재시도/수동 확인: 기존 `saveRetriedAttachmentSet`은 고정한 역할을 보존한다. MANUAL 역할을 자동 판정으로 덮어쓰지 않는다. 같은 추출의 재사용과 새로운 추출을 구분하고, 다른 파일·다른 버전의 역할 근거를 복사하지 않는다. 데이터 적용은 기존 승인·CAS·봉인/복구 경계를 유지한다.
5. API/UI: 기존 v1과 문서 역할5개 코드는 유지하고, v2에 자동 판정 유래·규칙 버전·근거 위치를 추가하여 관리자 최종 검증에서 확인하게 한다. UNKNOWN·기술 미완료·수동 확인·자동 분석 완료를 합치지 않는다. 기존 근거가 없는 행은 null/미확인으로 표시하며 과거 행을 자동 판정된 것으로 채우지 않는다.
6. 검증: 역할별 양성/음성, 신청서가 붙은 혼합 공고문, 파일명만 일치, 불완전/OCR, 서로 다른 파일 간 조합, 다른 source의 근거, 규칙 변경/checkpoint 재개, 수동 역할/재시도 보존을 실제 worker→DB→API까지 검증한다. 현재 공식 첨부 QA 기대값0을 합성 시험으로 대체해 PASSED로 만들지 않는다.

09-15 DB/worker/API/UI 연결을 구현했고 QA 브랜치 d56a7a8의 실제 Linux/PG migration3건·worker11건이 생략 없이 통과했다. 아래 순수 판정기 초기 기록은 그 이전 경계다. 공식 파일 정확도·운영 적용·전체 Gate 완료를 뜻하지 않는다. 엔진·모델 수는 여전히 첨부6/등록12/추출3이며 역할 판정기는 새 사이트 수집 모델이 아니다.

#### 2026-09-15 역할 판정기 v1 증분 — 실제 적용 전

초기 순수 구현 시점의 `AttachmentDocumentRoleClassifier`는 한 파일의 `Extraction`만 받았다. 파일명/URL/본문/다른 첨부의 단어/수동 역할 입력은 없다. 당시에는 HTTP·DB·Spring 자동 연결이 없었고 worker의 UNKNOWN을 실제 NOTICE로 바꾸지 않았다. 이후 아래 V82·명시적 정책 버전 연결을 구현했다.

고정 규칙 v1의 초기 제목 범위는 첫3개 비어 있지 않은 줄·앞600 code point다. 완전 추출과 텍스트 전체를 빠짐없이 덮는 순서/위치 근거를 검증하고, 역할 근거 줄은 신뢰 가능한 단일 block 안에 있어야 한다. NOTICE/GUIDE의 구조 항목 조합은 문서 역할 판별용이며 지원대상+지원형태 키워드 판정의 문맥 제한을 완화하지 않는다.

| 제안 역할 | 텍스트 제목 | 추가 구조 근거 |
|---|---|---|
| NOTICE | 공고/공고문/모집요강으로 끝나는 제목 | 지원대상 또는 신청자격, 지원내용 또는 지원규모, 신청기간 또는 접수기간의 명시 항목 모두 |
| GUIDE | 지원/사업/신청/모집 안내(문/서) 제목 | NOTICE와 같은 세 항목 모두 |
| FORM | 신청서/동의서/확인서/서약서/신고서 제목 | 신청인/사업자등록번호/성명 입력 항목과 서명/인 표시 |
| REFERENCE | 자주 묻는 질문/FAQ 제목 | Q·A의 실제 질문/답변 항목 모두 |
| UNKNOWN | 제목/구조 불충분, 다른 역할 제목이 공존, 추출 불완전, 구조 불명확, 처리 제한 | 해당 고정 사유 유지. 일반 참고자료 전체를 FAQ 규칙으로 확정하지 않음 |

반환은 `ruleVersion`, `rulesHash`, `textHash`, `blocksHash`, `roleCode`, `reasonCode`, `evidence[]`다. 각 evidence는 규칙 코드/block index/code point 시작·끝뿐이며 원문을 복제하지 않는다. 혼합 공고문+신청서는 UNKNOWN을 유지한다. 최대100만자/2만줄/100개 규칙 일치 제한 초과를 성공으로 표시하지 않는다. 원문 누락·겹침·잘못된 block index는 유효하지 않은 입력으로 거부한다.

이 규칙은 합성 양성/음성 회귀22건으로 검증한 초기 구현이며 공식 파일 정확도나 운영 적용 승인이 아니다. 파일에 없는 역할을 추정하지 않는 대신 다양한 제목/서식의 미인식은 남을 수 있다. 실제 공식 파일 기대값을 채우면서 범위를 검증해야 하며 그 전에는 활성화하지 않는다. 기존 종합 분류19건도 그대로 통과했다. 로컬 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1`은3분18초 성공했고 root2342=2101통과/241조건부 생략, 독립 패키지16/16이다. 추출기 시험/설치는 변경 없어 UP-TO-DATE이며 이번 재실행 통과로 세지 않는다. 원격8cb55d1 검증은 이 역할 판정기 추가 전 SHA다.

다음 DB 증분은 V82로 작성했다. V1~V81은 변경하지 않았고 운영 DB에도 반영하지 않았다. 정확한 저장/복사 계약은 DB11.31, API24.34를 따른다.

- 파일의 유래에 TEXT_RULE을 추가하고, extraction/file/set/source 복합 소속으로 연결한 불변 역할 assessment에 위 버전·지문·판정·위치만 저장한다. 추출 원문은 기존 extraction에만 존재한다.
- TEXT_RULE 파일은 같은 추출의 assessment와 역할이 일치해야 봉인할 수 있다. 현재 set이 OPEN인 동안에만 신규 assessment를 넣으며 봉인 뒤 추가/수정은 금지한다. UNKNOWN 제안과 자동 적용된 역할을 구분한다.
- 재사용은 원래 extraction/assessment와 동일 버전·지문·텍스트/위치 근거를 고정하고, 새 다운로드에는 과거 근거를 복사하지 않는다. MANUAL은 덮어쓰지 않는다. 기존 봉인 이력은 소급 작성하지 않는다.
- 실행 snapshot에 역할 규칙 버전/지문을 고정하고 checkpoint·manifest에 포함한다. 기존 snapshot을 새 규칙으로 재해석하지 않는다. v2 파일 조회는 nullable assessment를 추가하고 v1은 변경하지 않는다.
- 역할 제안기 단위 통과 → additive DB 제약/복사 검증 → worker checkpoint/재시도 연결 → v2/UI 근거 → 공식 파일 기대값/전체 QA 순으로 완료한다. 09-15 역할 규칙을 명시한 새 정책만 사용하는 DB/worker/v2/UI 경로를 구현했다. 실제 Linux/PG 실행과 공식 파일 기대값/전체 QA는 별도 잔여다.

09-15 구현은 `document-role-1.0.1`이다. JSON 표현에 의존하던 block hash를 SQL/Java 공통 canonical 형식으로 고정했으므로 버전을 올렸다. 기존 1.0.0은 DB에 적용하지 않은 순수 제안기였다. 기존 정책/실행의 누락 필드를 새 규칙으로 해석하지 않으며, MANUAL/PROFILE 자동 덮어쓰기와 운영 활성화는 하지 않는다. 새 역할 규칙을 적용한 정책이 공식 정확도 검증을 마쳤다는 의미도 아니다.

### 2026-09-15 대전 서구 본문 전용 정제

공식 상세 `www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=51668`의 HTML 구조를 읽기 전용으로 확인했다. `div.card.program--view` 안에 공고번호 `span#notAncmtMgtNo`, 제목 `span#notAncmtSj`, 본문 `span#notAncmtCn`이 각각 한 개 있으며 공고번호가 요청값과 일치했다. 담당자·전화·첨부 영역은 본문과 별도다. 원문·개인정보·다운로드 폼 인자를 문서나 fixture에 복사하지 않았다. 이 구조 표본을 제목 통과/정상 공고/첨부 추출 QA로 세지 않는다.

`LocalGovernmentNoticeProviderContentClient`는 해당 host·path에서만 이 구조를 사용한다. 정확한 공고번호 query, 단일 카드·번호·비어 있지 않은 제목·단일 본문을 요구하고 번호 불일치/중복/selector 변경은 BODY_SELECTOR_CHANGED로 남긴다. 빈 정제 본문은 BODY_TEXT_EMPTY이며 주변 페이지로 대체하지 않는다. 본문 속 A/B 문구와 일반 신청 링크는 유지하되 탐색 메뉴·카드 밖 정보·첨부명은 포함하지 않는다. 다른 서구 게시판과 미실측 기관의 기존 계약은 변경하지 않는다.

신규 회귀5건은 정상 정제, 구조8변형, query6변형, 빈 본문, 다른 게시판을 검증한다. production transport 대신 기존 HTTP 대역을 사용하며 요청1회를 확인한다. 로컬 표적 테스트·bootJar가23초에 통과했다. 현재 본문 전용 정제는 BBS3기관에 서구1기관을 추가한 것이며, 첨부 엔진6/등록 모델12/추출기3의 수를 늘리지 않는다. 실제 운영 수집·전체 모델의 본문 정확도·운영 브라우저 Gate는 여전히 미완료다.

### 새올 본문 정제 증분 — 2026-09-15

부산 남구 `eminwon.bsnamgu.go.kr` 공고46034, 대구 달성군 `eminwon.dalseong.daegu.kr` 공고53932의 공식 상세 HTML을 확인했다. 기존 고정 새올 상세 경로와7개 query만 대상으로 하며 `form[name=form1][method=post]` 안의 표·제목·본문이 유일해야 한다. 남구는 `table.table_03`의 단일 제목 `th[colspan=4]`와 `td[colspan=4] > div.view01_con`, 달성군은 `table.bbsView`의 단일 `제목` label/인접 값과 `td[colspan=4].con.l`을 사용한다.

다른 요청 action·중복/추가 query·누락/중복 form/table/title/body는 BODY_SELECTOR_CHANGED, 정제 후 빈 본문은 BODY_TEXT_EMPTY다. 기관 메타데이터·첨부명·페이지 주변 문구를 본문으로 대체하지 않는다. 두 페이지는 요청 공고번호를 별도 hidden 값으로 반환하지 않으므로 DOM 공고번호 동일성까지 검증했다고 주장하지 않는다. URL·공식 host·요청 action·현재 구조를 확인한 범위다. 대구 중구/함안 등 다른 새올 기관은 이 정제로 처리한다고 추정하지 않는다.

신규 대역 회귀5건(기관별 정상/구조8변형/query7변형/빈 본문/미실측 host)을 추가하여 본문 수집기37+종합 분류19=56건·bootJar가26초에 통과했다. 기존 `attachmentProfileDiscoveryQa`에 `MeasuredBodyContentLiveQaTest`를 등록했고 명시적인 `--tests '*MeasuredBodyContentLiveQaTest'`로 남구·달성·서구3건만 실행했다. 실제 production pinned transport/URL 검증/본문 정제로 모두AVAILABLE·시도1·redirect0을 확인했고14초에 통과했다. 파일 요청·DB 저장·운영 설정 변경은 없다. 표본은 **본문 구조/HTTP smoke**이지 제목 통과 정상 공고·첨부 추출·정책 QA 기대값이 아니다. 원문·담당자·파일명은 fixture/결과에 기록하지 않았다.

현재 전용 본문 정제는 BBS3기관·서구·남구·달성 총6기관이다. 첨부 엔진6/등록 모델12/추출기3은 변하지 않는다. 모든 출처의 BODY→ATTACHMENT→최종 검증 완료와 운영 브라우저 증거는 별도 필수다.

최종 로컬 `test bootJar attachmentContractQaTest --no-daemon --max-workers=1`은3분17초 성공: root2409건=2161통과/248조건부 생략/실패0, 패키징20/20통과. extractor 시험/설치는 UP-TO-DATE로 이번 재실행 성공에 넣지 않는다. Node 보고서 판정기10/10·diff 검사를 통과했다. 실제 본문HTTP3건은 전용 task 결과이며 일반 root에서 생략된 외부/DB/Linux 시험을 성공으로 바꾸지 않는다. 신규 migration·운영 설정/데이터·브라우저 변경은 없고 사용자 Word2개는 보존했다.

### 중구·함안 본문 셀 정제 증분 — 2026-09-15

대구 중구34295·함안43065의 공식 상세 HTML을 읽기 전용으로 확인했다. 두 기관은 기존 고정 새올 상세 경로·7개 query·단일 POST form을 사용한다. 중구는 `table.boardView`, 함안은 `table[width=100%][border=0][cellspacing=1][cellpadding=0]`를 고정하고, 해당 표 소속의 유일한 `제목` label/비어 있지 않은 인접 값과 `td[colspan=4]`의 `word-break:break-all;` 표식을 갖는 유일한 본문 셀만 사용한다. 내부 표의 셀을 바깥 공고의 제목/본문 표식으로 잘못 사용하지 않는다.

style·제목·표·form 구조가 바뀌거나 중복되면 BODY_SELECTOR_CHANGED로 남긴다. 정제 후 빈 셀은 BODY_TEXT_EMPTY이며 담당자·장식 셀·첨부명·메뉴로 대체하지 않는다. CSS 표식 의존성은 의도적인 fail-closed 경계이며 향후 사이트 개편 시 실측 후 수정해야 한다. 요청번호가 DOM 식별값과 일치한다고 추가 주장하지 않는다.

본문 수집기 회귀40건·bootJar는22초에 통과했다. 실제 production pinned transport의 서구·남구·달성·중구·함안5건은15초에 모두 AVAILABLE·시도1·redirect0을 확인했다. 원문·담당자·첨부명은 fixture/보고서에 복제하지 않았다. 표본의 HTTP/본문 구조 확인이지 정상 공고 후보·실파일/DB·정책 QA 성공이 아니다. 전용 본문8기관, 첨부 엔진6/등록 모델12/추출기3이며 미실측 출처를 지원 완료로 계산하지 않는다.

최종 로컬 `:test :attachment-extractor:test attachmentContractQaTest bootJar --no-daemon --max-workers=1`은3분37초 성공했다. root2416건=2168통과/248조건부 생략/실패0, 패키징20/20통과다. 추출기 시험은 UP-TO-DATE이며 이번 실제 재실행으로 세지 않는다. Node 보고서10건·diff 검사 통과, 임시 HTML 진단 소스/프로세스 정리, 사용자 Word2개 보존을 확인했다. 이 증분의 Linux/운영 검증은 별도다.

### 부산시·강북구·화천군 본문 정제 증분 — 2026-09-15

각 기관2개 공식 상세에서 본문과 주변 정보의 DOM 경계를 읽기 전용으로 확인했다. 부산79571/79570은 `div.boardView`의 단일 `h4.form-data-subject`와 `dl.form-data-content`의 `내용` dt/인접 dd를 사용한다. 고정 `/nbgosi/view`, 숫자 sno, 실측 `gosiGbn=A`, 선택적 양수 curPage만 허용한다. 다른 gosiGbn은 현재 정제 범위를 넓혀 추정하지 않으며 BODY_SELECTOR_CHANGED로 남긴다.

강북184761/184744는 `form#board`의 직접 자식 hidden `nttId`를 요청값과 대조하고, 직접 `div.bd-view`의 고유 `h3.bd-view__subject` 및 단일 직접 dl/dd만 사용한다. 고정 법정 공고 게시판/menuNo=200082가 필요하다. `table-dl`의 담당자·첨부 metadata와 `opentype`의 공공누리 안내를 본문으로 사용하지 않는다. 폼 밖/중첩 본문의 같은 이름 입력은 공고번호 근거가 아니다.

화천33897/33895는 기존 새올 경로·7query에 subCheck=N을 요구한다. 단일 form1/post의 `table[width=100%][border=0][cellspacing=1][cellpadding=0]`, 같은 표의 제목 th와 인접 값, `word-break:break-all;`인 단일 colspan4 본문 셀을 사용한다. 첨부의 중첩 표(cellpadding1)와 장식 셀을 제외한다. 부산/화천에서 DOM 공고번호 대조까지 성공했다고 주장하지 않는다.

누락·중복·구조/번호/action 변경은 BODY_SELECTOR_CHANGED, 정제 후 빈 본문은 BODY_TEXT_EMPTY다. 주변 메뉴·담당자·파일명으로 대체하지 않으며 실제 A/B 문구·신청 링크·본문 내부 표/목록은 보존한다. 다른 host/path의 기존 동작과 TITLE 선행 Gate·BODY 실패 후 ATTACHMENT 진행 정책은 바꾸지 않았다. 원문·개인정보·다운로드 폼 값을 fixture/로그/문서에 복제하지 않았다.

본문 회귀49건(새9개 시험 메서드)이 통과했다. 최종 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분40초 성공: root2426건=2178통과/248조건부 생략/실패0, 패키징20/20통과. 추출기 시험은 UP-TO-DATE로 이번 재실행 성공에 합산하지 않는다. JAR SHA256은 `8e814a78acbf11e27033f6669e2a306cf1105e2c301d64759e628c82f153507f`다.

마지막 강북 필드 경계 보완 후 `attachmentProfileDiscoveryQa --tests '*MeasuredBodyContentLiveQaTest'`를 다시 실행하여24초 성공/11사례 전부 AVAILABLE·시도1·redirect0을 확인했다(실제 HTTP 시험4.744초). 기존5사례와 새6사례이며 원문 영구 저장·첨부 다운로드·DB 쓰기·운영 활성화는 없다. 이 표본은 본문 구조/HTTP smoke이지 제목 통과 정상 후보·첨부 추출·정책 QA 기대값이 아니다. Node 설치/보고서 시험22건=20통과/2 Linux 전용 생략, diff 검사 통과다. 이전 SHA `74cc125`의 Linux 설치12/12 통과와 이번 BODY 증분의 아직 미실행인 Linux 결과를 구분한다.

전용 본문은11기관, 첨부 엔진6/등록 profile12/추출 형식3이다. 현재 지자체 결합11개는 운영 snapshot의 기관/목록 parser와 일치하지만212개 활성 지자체에는 첨부 결합이 없다. 모든223개 지원·공식 파일 역할 정확도·같은 SHA 운영 적용/브라우저 완료를 선언하지 않는다.

### 실행 체크리스트

- [x] P0 사용자 정책·기존 계약 충돌 분석과 이 상세 설계 작성.
- [~] P1 현재 DB 이력 기반 처리 흐름 DTO/상세 UI·상태 회귀 구현, 로컬 단위/HTTP/Node 및 QA 브랜치 Linux 실제 PG 통과. 운영 UI 검증 잔여.
- [~] P2 목록의 최종 검증 대기열/기술 예외/자동 처리 필터와 SQL count·pagination 구현, 로컬 회귀 및 9상태/29행 Linux 실제 PG 통과. 운영 적용·브라우저 검증 잔여.
- [~] P3 명시적 탐색 영역 정제·BBS3기관+서구/남구/달성/중구/함안/부산시/강북구/화천군 본문 추출·텍스트 역할 판정기 및 V82/worker/checkpoint/재시도/v2/관리자 근거 연결 구현. V82 통합 경로의 Linux/PG migration3·worker11건 통과. 최신 본문 회귀49건·실제 상세HTTP11건 통과이며 공식 파일 정확도 및 나머지 본문 모델/전체 QA 기대값은 잔여.
- [~] P4 실제 첨부의 역할·사유·텍스트/위치 근거 지문을 catalog→실행→원장 재검증에 연결했다. UNKNOWN/양식만 있는 음성 표본은 정상3공고로 세지 않는다. 전체 profile 매핑·누락 어댑터·실제 파일 기대값·형식 적용성 및 Provider QA 화면은 잔여.
- [~] P5 a314aa9의 Linux 주 검증·독립220/220·직접 부모 연결/취소2건·정리 및 공식3공고/7파일 격리 관측이 통과했다. 실제 품질은 완전5/부분1/OCR필요1, 완전5건도 역할UNKNOWN이다. 역할·기대값/완전PDF·전체 Provider/ATT001~062·새 FLOW 전체 실증은 잔여이며 workflow 성공을 전체 Gate 완료로 대체하지 않는다.
- [ ] P6 검증된 변경의 한글 커밋·푸시·동일 SHA 배포/health/권한 확인.
- [ ] P7 정확한 대상·효과·복구 승인 후 COLLECT_ONLY→ENFORCE 및 기존 데이터 고정 분할 실행.
- [ ] P8 실제 운영 브라우저에서 단계·실패·재시도·최종 확인·DRAFT·권한·반응형 검증 및 전체 집계.

기존 Gate0~8의 분모는 유지한다. Linux/PG·GitHub·AWS 접근 불가를 synthetic 테스트로 대체하지 않는다. 새 인프라·secret·IAM 변경은 별도 승인 대상이다.

## 6. 검증과 완료 기준

- FLOW-001: 제목 제외는 상세/첨부 요청0, source 원문0. 기존 ATT001~002 유지.
- FLOW-002: 본문 부족/A/B는 중간 사람 입력 없이 첨부 분석까지 진행.
- FLOW-003: 충분한 본문이어도 첨부를 확인. 실제 NO_FILES만 생략 사유.
- FLOW-004: pending/재시도/과거 근거는 최종 검증 준비 완료가 아님.
- FLOW-005: 완전 첨부가 부족 본문을 보완하면 최종 사유는 현재 종합 근거이며 base 이력은 불변.
- FLOW-006: 기술 실패/부분 추출/OCR/미지원은 정상 완료로 표시하지 않음. 수동 확인도 자동 완료로 변조하지 않음.
- FLOW-007: UNKNOWN·파일명 B·다른 파일 간 AND·A/B 검수 정책을 우회하지 않음.
- FLOW-008: 현재 확인·원복된 확인·버전 변경·동시 재수집에서 새 단계 표시가 기존 권한/CAS를 우회하지 않음.
- FLOW-009: COLLECT_ONLY preview가 적용된 종합 결과나 전체 검증 완료로 보이지 않음.
- FLOW-010: 목록/count/필터가 같은 서버 조건을 사용. 준비 완료 건수에 진행·기술 오류를 포함하지 않음.
- FLOW-011: 실제 관리자 여정에서 중간 단계의 불필요한 입력 없음, 한글 사유·근거 위치·입력 보존 확인.

단위/Mapper 계약/HTTP/Node 회귀→기본 테스트·bootJar→실제 Linux/PG/Provider→운영 브라우저 순으로 증거를 분리한다. 생략/미실행은 성공이 아니다.

### 6.1 변경된 처리 순서를 지키는 고정 회귀

- `LocalGovernmentNoticeProviderContentClientTest`의 중첩 탐색 영역/본문 대체 경로/메뉴만 있는 응답 3사례: 메뉴 키워드는 본문 근거에서 제외하되 실제 본문의 B·일반 문장·신청 링크는 보존한다. 상세 1회 외 추가 요청이 없어야 한다.
- `AnnouncementSourceServiceImplTest.insertCollectionRunFetchesDetailBodyOnlyAfterTitleGateAllowsIt`: 제목 통과 이후에만 상세 본문을 요청한다.
- `AnnouncementAttachmentClassificationEngineTest.intermediateReviewReasonStillCollectsAttachmentEvidenceBeforeFinalReview`: 제목 A·본문 A/B 각각 첨부 텍스트 근거를 생성한 뒤 최종 검수 사유를 유지한다.
- 같은 클래스의 `sufficientBodyDoesNotSkipAttachmentFilteringOrHideAttachmentExclusionEvidence`와 `sufficientBodyCannotTurnFailedDiscoveryIntoVerifiedNoFiles`: 충분한 본문도 첨부 B·발견 실패를 숨기지 않는다.
- `AnnouncementAttachmentWorkerIntegrationTest.bodyFetchFailureStillCollectsAttachmentsAndPreservesBaseFailureEvidence`: 실제 Linux 추출·임시 PG 경로에서 본문 실패와 첨부 성공 근거를 분리한다. 해당 전용 task의 실행 통과가 필요하며 단위 대역 시험으로 대체하지 않는다.
- `AnnouncementAttachmentJobIntegrationTest.processingQueueMatchesJavaProjectionForAllNineStatesAndFullPageCounts`: 9상태·29행의 SQL/Java·전체 count·페이지를 대조한다. 기술 예외와 정상 후보를 같은 상태로 합치지 않는다.

09-14 로컬 추가 분류 시험은 기존 포함19건 통과했다. [Linux 다섯 번째 실행](https://github.com/FrostyCityMan/saneB/actions/runs/34828914963), SHA `97bf0369d296ebf49661f38edd7518ffd448607c`에서 중첩 메뉴 정제·worker·대기열 실제 PG 및 독립 namespace215건이 통과했다. 정책 부모 연결2건은 `QA_CHILD_FAILED`여서 전체 workflow는 실패다. 운영 수집이나 관리자 브라우저 성공으로 확대하지 않는다.

효과 지표는 동일 기간·대상·규칙 버전 기준의 최종 검증 소요 시간(실사용 관측), 추가 판단 필요 비율, 수동 역할 지정 비율, 기술 실패율, 표본 오분류/누락률이다. 처리 건수와 기간을 함께 기록하며, 개인정보/원문/검수 메모를 분석 이벤트에 복사하지 않는다. 아직 baseline과 개선율을 측정하지 않았으므로 절감률을 약속하지 않는다.

실패 기준: 첨부 다운로드만으로 필터링 완료 선언, 미해결 실패 숨김, 모든 UNKNOWN을 자동 공고문으로 승격, v1 의미 변경, 과거 migration 편집, 승인 없는 운영 적용, 일부 profile 성공을 전체 완료로 확대.
