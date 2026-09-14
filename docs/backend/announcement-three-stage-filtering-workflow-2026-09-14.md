# 제목·본문·첨부 3단계 자동 필터링과 최종 관리자 검증

- 기준일: 2026-09-14. 사용자 확정 방향을 기존 첨부 장기 goal에 통합한다.
- 목적: 시스템이 텍스트를 먼저 선별·분류·대조하여 사람이 읽고 비교해야 하는 양을 줄인다.
- 기준선: master / ae893b87348a9bd1cb0893763f6cad5047093a24, 로컬 최신 V81. 운영 적용 증거가 아니다.
- 기존 [첨부 설계](announcement-attachment-collection-design-2026-09-08.md)의 처리 순서·화면 해석을 구체화한다. 기존 제목 제외, A/B 정책, v1, additive migration, 근거 불변·권한·배포 승인 경계는 유지한다.
- 상태: 설계 확정 후 증분 구현 진행. 전체 Gate0~8 / ATT-001~062의 완료 범위를 줄이지 않는다.

## 1. 확정 정책

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

1. 지자체 본문 추출은 실측한 태백25·횡성65·영월17 게시판에서 제목 표식과 단일 게시판 표의 `td[title=내용]`만 사용한다. 구조 누락/중복은 `BODY_SELECTOR_CHANGED`이며 페이지 전체로 대체하지 않는다. 내부 결과는 기존 FETCH_FAILED로 전달되어 이후 첨부 분석을 막지 않는다. 나머지 대상은 기존 `main/[role=main]/article/body` 공통 탐색을 유지한다. 명시적인 `nav/[role=navigation]`는 선택 영역 안팎에서 제거하며 일반 본문 문장·실제 제외 조건·신청 링크는 보존한다. 세 모델 외 본문 영역과 메뉴/푸터 오염의 전수 확인은 여전히 필요하다.
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

이 목록의 전체 연결은 아직 미완료다. 아래 순수 판정기 구현만으로 DB/worker/API/운영 적용을 완료 처리하지 않는다. 엔진·모델 수는 여전히 첨부6/등록12/추출3이며 역할 판정기는 새 사이트 수집 모델이 아니다.

#### 2026-09-15 역할 판정기 v1 증분 — 실제 적용 전

`AttachmentDocumentRoleClassifier`는 한 파일의 `Extraction`만 받는다. 파일명/URL/본문/다른 첨부의 단어/수동 역할 입력은 없다. HTTP·DB·Spring 자동 연결도 없다. 현재 `AnnouncementAttachmentWorkerServiceImpl`과 종합 분류기의 역할 계약은 그대로이며 UNKNOWN을 실제 NOTICE로 바꾸지 않는다.

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

### 실행 체크리스트

- [x] P0 사용자 정책·기존 계약 충돌 분석과 이 상세 설계 작성.
- [~] P1 현재 DB 이력 기반 처리 흐름 DTO/상세 UI·상태 회귀 구현, 로컬 단위/HTTP/Node 및 QA 브랜치 Linux 실제 PG 통과. 운영 UI 검증 잔여.
- [~] P2 목록의 최종 검증 대기열/기술 예외/자동 처리 필터와 SQL count·pagination 구현, 로컬 회귀 및 9상태/29행 Linux 실제 PG 통과. 운영 적용·브라우저 검증 잔여.
- [~] P3 명시적 탐색 영역 정제·BBS3모델 본문 추출·텍스트 역할 판정기 및 V82/worker/checkpoint/재시도/v2/관리자 근거 연결 구현. 새 통합 경로의 Linux/PG 실행·공식 파일 정확도 및 나머지 본문 모델/전체 QA 기대값은 잔여.
- [ ] P4 전체 대상의 profile 매핑·누락 어댑터·실제 파일 기대값 및 Provider QA 화면 연결.
- [~] P5 이전66d499c 정상 부모 연결은 QA_CHILD_PROCESS_LIMIT 실패(같은UID97/부모JVM9)다. V82/새 역할 경로 d56a7a8의 Linux 주 검증은 통과했고 독립/부모는 진행 중이다. 후속 CI 전용 직접 JUnit 실행기를 구현해 준비 Gradle 종료 뒤 부모2사례를 같은 한도에서 실행하도록 변경했다. 실제 자원 오류 해결 확인과 전체 Provider/ATT001~062·새 FLOW 전체 실증은 잔여.
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
