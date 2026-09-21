# 첨부 수집 ATT-001~062 구현·검증 추적표

## 2026-09-22 서울 태백 재관측·고정 비교 근거

[서울 임시 QA](announcement-seoul-temporary-bbs-qa-2026-09-21.md)에서 최신 BBS 코드의 BODY431자와 전체HWPX2파일(2041/1994자,62/117블록)을 관측하고, 새 사전 기대값을 production CaseExecutor로 별도 재다운로드·재추출해 비교했다. 두 실행 각 JUnit1/1·실패/생략0, 전체 파일/역할 지문 일치·원본/임시 자원 정리·운영 DB쓰기0이다. UNKNOWN/MIXED_DOCUMENT_ROLES와 FORM·REVIEW_REQUIRED를 유지해 ATT-011의 해당 표본 분류 근거, ATT-017의 HWPX 해당 표본 추출/locator 근거를 보강한다. 실제 UI 표시/후속 행동·나머지 모든 profile/형식 요구는 남는다.

합계8요청/2,658,726bytes가 승인44요청/80MiB 이내인 점은 예산 사용 근거이며 ATT-024의 초과 경계 시험이나 ATT-060의 다중 worker 동시성/정책 게시를 대체하지 않는다. 관측 성공을 제목 제외·실패/재시도·운영 DB/API/브라우저 검증으로 확장하지 않으며 기존62행 상태/분모를 유지한다.

## 2026-09-16 검증 기준 갱신

아래 날짜별 증분은 당시 기록이다. 과거의 “최신 PG 미실행/환경 차단”은 현재의 Linux 실행 여부를 뜻하지 않는다. `85c7f65` Linux35050057694의 보관 XML에서 migration3·전체 분할14·job192·worker12·Flyway3·runtime1·정책 부모2건의 실패·생략0을 확인했다. [DB/API 계약 근거](announcement-attachment-contract-evidence-2026-09-16.md)에 실제 testcase와 증명 범위를 연결했다. ATT037/053/058의 DB 실행 차단 표기는 갱신하되 전체 운영 요구 통과로 승격하지 않는다.

실제 공식 파일·운영 설정/데이터·인증 브라우저는 별도 근거가 필요하다. 표의 나머지 “최신 PG” 요구는 위 실행된 해당 assertion으로 충족한 부분과 운영/실파일 잔여를 구분해 읽는다. 후속 `9fe892c` Linux35051444985도 전체 계약 success이며 제천 별도3/3을 확인했다. 제목 음성1건 요청0, 양성2건 전체HWPX3개 완전 추출·worker/DB/API 대조·UNKNOWN 검수 유지·원본/lease 정리/운영쓰기0이다. ATT002/011/017/036의 해당 표본 근거이며 전체62행·Gate0~8과 정상 표본·전체 대상 분모를 줄이지 않는다.

- 09-14 17시 3단계 대기열: P1 처리 흐름 projection/상세 안내와 P2 v2 선택 필터·공통 SQL count/page·읽기 전용 화면을 구현했다. FLOW004~010 및 ATT043~045의 부분 구현/로컬 회귀 근거이며 기존62행의 실제 통과 상태를 변경하지 않는다. root2059통과/239생략·Node152·독립 QA14·bootJar 성공, 실제 PG 대기열 시험은 initdb/Code Integrity 초기화 실패1로 assertion 미실행이다. Linux 독립213건은INVENTORY_ONLY/통과0. P3/P4·실제 Provider/Linux/운영 E2E와 전체 Gate 최종 통과0을 유지한다. 상세는09-14 3단계 설계/최신 진행 기록/API24.33/DB11.30이다.

- 09-12 11:15 Provider QA 관리 연결: V80 승인 계획/분할 원자 봉인, 정확한 범위·예산 확인/멱등 예약, 역할별 계획·원장·항목 페이지/취소, 기본 OFF 스케줄러의 현재 입력/전체 항목 대조·단일 case 실행을 연결했다. ATT-029~034/037/044/045/052/058/060/061의 부분 구현 근거다. Service34/HTTP23/스케줄러2와 전체 root1919통과234생략·추출기25(기존 결과 재사용)·QA14·Node130통과. 실제 PG3사례/V80 migration은 미실행이며 독립208건도 발견만 했고 통과0이다. 공식 참조9/기대값0, 전체 catalog/UI/verifier·Linux/운영 E2E 잔여와62행/Gate 최종 통과0을 유지한다. 상세는 `announcement-attachment-provider-qa-management-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 10:40 시스템 catalog/계획: 참조 표본과 실행 기대값·전체 target/case 분모·정상3공고/형식별 기대 coverage·중복 상세/제목 판정·유효기간·분할 상한을 구현해 정책 snapshot6에 연결했다. ATT-001/002/012~014/029/034/044/052/060/061의 부분 계획 근거다. 공식 참조9/실행 기대값0이며 실제 Provider 성공으로 바꾸지 않는다. catalog27건 포함 root1858통과231생략·extractor25·QA14·Node130통과, 독립205건은 실행0이다. 실제 기대값/전체 catalog·예약 API/정책 verifier·Linux/PG·운영 E2E가 남으며 기존62행과 Gate 최종 통과0을 유지한다. 상세는 `announcement-attachment-provider-qa-catalog-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 10:15 Provider QA 원장: V79의 불변 전체 요구 범위/run/case와 내부 ExecutionService를 연결했다. 기존 job/정책 QA와 전역 자원을 공유하고 소유권/만료/취소·원자적 누적 예산·정리 후 결과 지문/건수 대조를 구현했다. ATT-029~034/037/044/052/058/060/061의 부분 구현 근거다. 내부 Service35/기존 실행기42와 로컬 전체 root1831통과231생략·extractor25·QA14·Node130통과. 새 PG14사례를 포함한 독립205건은 발견만 했고 통과0이다. 전체 catalog/예약 API/정책 verifier·실제 Linux/PG/Provider/운영 브라우저는 미완료이며 PROVIDER_PROFILES MISSING/전체 Gate 최종 통과0과 기존62행을 유지한다. 상세는 `announcement-attachment-provider-qa-ledger-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 고정 공고 실행 단위: 제목 제외 요청0·전체 파일 목록/형식/binary 지문 검증·기존 flow/transport/signature/Linux extractor 호출·부분 실패/취소 분모·공유 자원 callback·420초/요청/byte 제한·원본 정리·안전한 결과 metadata를 구현했다. ATT-001/002/012~014/017/020~024/027/032/044/052/060의 부분 실행 단위 근거다. HTTP/격리 추출은 대역으로 검증했으며 실제 Provider/DB 원장/운영 증거가 아니다. PROVIDER_PROFILES MISSING/전체 Gate 최종 통과0을 유지한다. 최종 실행 단위42건·root1794통과217생략·extractor25·QA14·Node130통과이며 구체적 실행/캐시/미실행 경계는 최신 진행 기록을 따른다.

- 09-12 09:13 전체 Provider 요구 범위: 시스템12개 프로필에 기관/목록 parser 결합을 선언하고 모든 대상의 미구현·불일치·중복·단순 결합을 읽기 전용 provider-qa-plan API와 snapshot schema5에 연결했다. ATT-044/045/052/060/061의 부분 범위·권한·입력 고정 근거다. 전체 Provider 실파일/분할 원장은 미구현이며 이 목록/최소 요구량은 실제 성공이 아니다. 표적145·최종 root1752통과217생략·extractor25·QA14·Node130통과, 독립191건은 실행0이다. 기존62행과 운영/실파일 Gate를 유지하며 상세는 `announcement-attachment-provider-qa-scope-2026-09-12.md`를 따른다.

- 09-12 정책 DB QA 연결: WORKER_DB_RECOVERY를 부모 소유 namespace/임시 PG 실행·취소/정리·전체 사례/지문 검증과 게시 verifier에 연결했다. snapshot schema4는 독립 QA artifact/전체 suite/case를 고정한다. JSONB 필드 순서와 증거 저장 시각도 검증한다. ATT-029~031/034/037/044/045/053/055/058/060/061의 부분 구현 근거이며 실제 Linux 연결2사례·독립191건·전체 Provider·운영 E2E는 미완료다. 표적72/QA14 및 첫 전체 root1738통과217생략·extractor25·Node130통과, 최종 후속 결과는 최신 진행 기록을 따른다. 기존62개 조건과 Gate0~8을 축소하지 않는다.
- 08:57 최종 후속 회귀는3분13초 성공, root1740통과/217생략·extractor25·QA14·Node130통과다. JSONB/기록 시각 회귀와 시간 의존적인 Node fixture 수정을 포함한다. 독립191건은 INVENTORY_ONLY·통과0이며 부모 Linux2사례도 아직 미실행이다. 실제 운영·전체 Provider 증거로 대체하지 않는다.

- 09-12 BBS 증분: 태백·횡성·영월 고정 profile3개를 추가해 registry12개다. 기관별 exact URL/파일 영역·세션 비전달·빈 영역/누락 구분·10개 상한·UNKNOWN 역할·비지원 파일 보존과 제한된 MIME/파일명 헤더 호환을 구현했다. 각 기관3건/총9공고14파일(PDF2/HWPX12)1,499,273byte·23HTTP·원본 정리를 실제로 확인했다. ATT-004/011~014/020~024/028/044/060의 부분 근거이며 HWP 실파일·빈/비지원 혼합 실사이트·Linux/PG·전체 Provider/정책 QA·운영 E2E는 미완료다. MIME/헤더 초기 실패와 최종 회귀는 `announcement-attachment-standard-bbs-profiles-2026-09-12.md` 및 최신 진행 기록을 따른다. 기존62행의 완료 조건을 축소하지 않는다.
- 08:12 최종 회귀는 root1715통과/217생략·extractor25·QA13·Node130통과다. 실사이트22건 중 강북1건 BRIDGE_GET HTTP400으로 전체 명령은 실패했고, 동일 법정7표본 재검증은24초 통과했다. 원인 미확정/간헐 실패를 보존하며 전체 실사이트/운영 Gate 완료로 표시하지 않는다. 새 BBS9건은 최종 코드로 통과했다.

- 09-12 07:36 정부24 계약 증분: 실제 출처 `GOV24_PUBLIC_SERVICE`와 배치 필터 별칭 `GOV24`의 경계를 수정하고 V78에서 첨부 작업/전체 목록 CHECK를 확장했다. 정책 registry·QA 대상·UI 응답/한글 표기, 고정 분할 소속·멱등 재요청·미지원 profile 차단 회귀를 포함한다. root1684통과/216생략·extractor25·QA13·Node130통과. ATT-053 및 전체 Provider/기존 데이터 범위 계약의 부분 근거이며 실제 정부24 수집·PG·운영 검증은 미완료다. 새 PG1사례·V78 upgrade를 포함한 독립 목록191건은 발견만 했고 통과0이다. 상세는 `announcement-attachment-gov24-provider-contract-2026-09-12.md`를 따른다.

- 09-12 07:08 코드 지문/게시 경계 증분: 수동 코드 목록을 전체 main class/resource 1438파일과 실제 바이트 대조로 교체했다. 잠금 밖 설치/QA 검증 후 게시 잠금 안에서 최신 DB 근거를 재대조하며 변경·동일 요청 경합을 검사한다. root1678통과/215생략·extractor25·QA13·Node129통과다. ATT-034/044/045/061의 부분 코드·로컬 회귀 근거이며 실제 PG·추가 Provider/worker QA 및 운영 게시/E2E는 미완료다. 상세는 `announcement-attachment-code-fingerprint-2026-09-12.md`를 따른다.

- 09-12 06:48 Gate6 증분: 실제 worker/격리 추출/DB/검수/DRAFT 연결8사례를 작성하고 전용 task·독립 산출물·CI 필수 XML에 연결했다. HTTP는 고정 합성 입력이다. root1658통과/215생략·extractor25·Node129·QA12통과이며, worker8은 Linux/PG 미실행이다. 독립 목록190건 발견/passed0은 전체 통과가 아니다. 상세 범위와 마지막 조건부 테스트 정리 assertion 보강 후 재컴파일/패키징 증거는 최신 진행 기록과 `announcement-attachment-contract-runtime-2026-09-12.md`를 따른다.

- 09-12 06:20 Gate6 검증 경로 증분: 독립 Linux DB QA 산출물/실행 판정/CI 연결 및 정책 Node 테스트 누락을 보완했다. root1656통과/207생략·extractor25·Node129·별도 실행기/패키징11통과. 고정 suite 목록182건(job168/migration2/backfill12)은 INVENTORY_ONLY/실행0이며 실제 SQL·worker·운영 성공이 아니다. ATT-029~031/037/053/055/058/060의 실제 DB 증거를 확보할 실행 경로를 추가한 것으로 모든 행의 잔여 Gate를 유지한다. 상세는 `announcement-attachment-contract-runtime-2026-09-12.md`와 최신 진행 기록을 따른다.

기준: `announcement-attachment-qa-plan-2026-09-08.md`. 2026-09-11 현재 작업 트리의 연결표이며 전체 완료 증명서가 아니다.
원래 테스트 메서드의 일부 ATT 번호는 QA 문서 의미와 다르므로 **메서드 이름의 번호가 아니라 검증한 조건**으로 연결했다.

## 상태와 증거 범위

- 09-12 05:54 부산·강북 후속: root1862건 중1655통과/207생략, extractor25·Node129통과. 신규프로필14/고정3단계flow9/worker2/header2 및 form10/11 경계를 검증했다. ATT-004/011~014/020~024/028/044/060의 부분 근거다. 실사이트13사례 재실행 통과, 새7사례18파일(PDF2/HWPX16)1,698,868byte·비지원1개 미다운로드/원본 정리다. 전체 강제 실행 중 강북 HTTP4001회로 명령이 실패했고 동일표본29초 재검증은 통과했다. 원인은 미확정으로 보존한다. Linux/PG·전체 Provider/정책 QA·운영/브라우저는 여전히 미완료이며 `announcement-attachment-legal-board-profiles-2026-09-12.md`와 최신 진행 기록에 경계를 적었다.

- 09-12 05시 최종 Node 회귀는7파일129건 통과(정책34 포함), 실패/생략0이다. 아래 V77 UI의 마지막 모드3종·고정 항목 페이지/분모 검증까지 포함하며 실제 서버/DB/운영 증거는 아니다.

- 09-12 05:11 V77 게시 UI 후속: root1834건 중1628통과/206생략, extractor25, 정책 Node34통과. V77 서버/API/QA verifier 회귀와 준비·개별 영향 확인·요청/영수증 결합 UI를 포함한다. 실제 로컬 합성 브라우저 정상 게시/응답 유실 동일 key/body/만료/조회 전용·INCOMPLETE 차단, 키보드 확인·제출,320~1440px 가로 넘침0을 확인했다. ATT-044/045/052/053·정책 수명주기의 부분 근거이며 전체 요구 완료는 아니다. 전용 PG는 initdb 초기화3실패로 SQL 미도달, 후속 job task 미실행이다. 전체 Provider/worker QA 실행·검증기와 실제 Linux/DB·운영 E2E는 남는다. 상세는 `announcement-attachment-policy-publication-ui-2026-09-12.md`와 최신 진행 기록을 따른다.

- 09-12 04:17 게시 준비 범위 V76/API 회귀: root1788건 중1586통과/202생략, extractor25·Node117통과. Service19/HTTP21, Mapper/Migration 정적 계약과 PG6사례 작성을 추가했다. 전체 ID/상태 고정과 권한/CSRF·사유 비노출은 ATT-045/052/053·정책 수명주기의 부분 근거다. job PG165/전체 분할PG12/migration2는 생략이다. 실제 정책 게시·전체 QA·Linux/DB·운영 E2E를 통과한 결과가 아니다. 상세는 `announcement-attachment-policy-publication-scope-2026-09-12.md`와 최신 장기 진행 기록을 따른다.

- 09-12 03:41 전체 회귀: root1740건 중 **1544통과/196조건부 생략**, extractor25·Node117통과, 별도 공식 다운로드6사례/10파일통과. 화천 POST·공통 고정 호출 선형 해석·폼 변경 실패 근거 저장/한글 표시를 포함한다. job PG159/전체 분할PG12/migration2는 생략이며 새 parameterized PG 입력별 실행을 의미하지 않는다. 상세는 `announcement-attachment-hwacheon-post-profile-2026-09-12.md`다. 실파일 텍스트 추출·전체 Provider·운영 worker/DB/브라우저 증거는 아니다.

- 09-11 18:37 최신 회귀: root1517건 중 **1340통과/177조건부 생략**, extractor25·Node48통과, 실패/오류0. 일반 작업 목록/영향/ADMIN 원복 승인/영수증 UI를 연결하고 Node mount/계약16건, Service5/HTTP7/Mapper1건을 추가했다. SSR9건도 권한·잠긴 승인·필수 동의·영수증 제목을 검증했다. 일반 이력 PG1건을 추가한 최신 job PG153건은 전부 생략이다. 최종 전체 rerun/build/installDist는3분56초에 성공했다. 실제 SQL/trigger·전체 배치 UI·운영/렌더링 브라우저 성공 증거는 아니다.
- 09-11 18:09 최신 회귀: root1503건 중 **1327통과/176조건부 생략**, extractor25·Node32통과, 실패/오류0. 일반 원복 Service21/HTTP15와 Mapper26/MigrationContract84가 통과했다. 일반 APPLIED 및 실패 예약의 영향 조회·ADMIN 승인·원자적 복구·멱등 응답·원래 실패 보존을 구현하고 PG12건을 추가했다. 최신 job PG152건은 모두 생략이다. 최종 전체 rerun/build/installDist는3분23초에 성공했지만 실제 SQL/trigger·관리자 복구 UI·운영/브라우저 성공 증거는 아니다.
- 09-11 17:46 최신 회귀: root1453건 중 **1289통과/164조건부 생략**, extractor25·Node32통과, 실패/오류0. 일반 작업 예약 전 버전/확인 binding 고정과 적용 후 지문 저장, 근거 변경 방지의 migration 정적2건·Mapper1건을 추가했다. PG8건을 추가해 job PG140건은 모두 생략이다. 최종 코드로 전체 rerun/build/installDist가3분21초에 성공했지만 실제 SQL/trigger·일반 원복 승인/실행·운영 및 브라우저 성공 증거는 아니다.
- 09-11 17:24 최신 회귀: root1442건 중 **1286통과/156조건부 생략**, extractor25·Node32통과, 실패/오류0. BatchRollbackService20/HTTP16/Scheduler2, Mapper/MigrationContract와 승인·전체 입력 CAS·이전 binding 복구·중복/충돌·무효 확인 보호·분모 보존을 검증했다. job PG132건은 모두 생략이다. 기존 확인 복원 PG fixture도 실제 승인/worker 경로에 연결했고 PG8건을 추가했지만 실제 DB/운영 성공 증거는 아니다. 첫 전체 실행은 512 MiB 힙 압박을 확인해 소유 worker를 종료했으며, 컨텍스트 캐시4 제한 후 전체 회귀가 3분32초에 성공했다. 테스트/검증 조건은 축소하지 않았다.
- 09-11 16:52 최신 회귀: root1394건 중 **1246통과/148조건부 생략**, extractor25·Node32통과, 실패/오류0. ReviewService41/MapperBinding23과 이전 확인의 불변 원본·복구 유효 버전 분리, 잘못된/오래된 복구 근거 차단, 최신 버전 DRAFT 조건을 검증했다. 새 PG6건을 포함한 job PG124건은 모두 생략이다. DB 복구 근거 생성은 테스트 fixture에만 있으며 전체 원복 승인 API/worker·배치 UI·운영 E2E 완료를 뜻하지 않는다.
- 09-11 16:23 최신 회귀: root1376건 중 **1234통과/142조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchApplicationService23/HTTP18/Scheduler2와 승인 접수·중지/재개·항목별 CAS·불변 승인·실패/backoff·수집/적용 결과 분리를 포함한다. 추가 PG8건을 포함한 job PG118건은 생략되어 실제 SQL/trigger·운영 성공을 뜻하지 않는다. 조건부 rollback/검수 복원 계약·배치 UI·전체 Provider/QA/E2E는 미완료다.
- 09-11 15:46 회귀: root1322건 중 **1188통과/134조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchPreviewService16/HTTP18과 SEALED 근거·불변 이력·명시적 선택·0HTTP·입력 변경 충돌·삭제 수 구분의 당시 증거다. 최신 변경과 실제 운영 검증은 별도다.
- 09-11 15:09 회귀: root 1279건 중 **1151통과/128조건부 생략**, extractor25·Node30통과, 실패/오류0. BatchService26/BatchController24/BatchClaim5와 수집 승인·중지/재개·전체 집계·고정 입력 충돌의 당시 증거다. 최신 변경의 검증은 위 실행과 구분한다.

- [x] 해당 요구의 전체 필수 계층을 직접 검증. 현재는 운영/브라우저 Gate가 남아 전체 요구 [x]는 없다.
- [~] 코드·부분 로컬 테스트는 있으나 실제 파일/DB/운영/API/UI 중 필수 증거가 남음.
- [ ] 구현 또는 직접 assertion을 추가해야 함.
- [!] 해당 DB 검증이 현재 환경에서 차단. Windows Code Integrity 3077의 libpq.dll 로드 거부, Docker 시작 timeout. 기존 통과 기록을 최신 변경의 증거로 대체하지 않는다.
- 10:23 root 703건(1실패)은 caller fixture 수정 후 해소. 이후 root 726건(659통과/67생략) 성공은 **24시간 간격 수정 전** 증거다.
- 24시간 간격 추가 후 Current/Intake/MigrationContract 선택 검증 78건 통과. 최종 전체 회귀 결과는 진행 기록에서 별도 갱신한다.
- 공개 파일 4개 CLI의 과거 기록은 실제 worker/DB/API/운영 ACTIVE의 성공이 아니다.
- 09-11 10:47 로컬 회귀: root 992건 중 898통과/94조건부 생략, extractor 13통과, 실패/오류 0. 규칙/정책 불일치 준비 차단과 workflow 구조 검증을 포함한다. Node 보고서 판정 9건도 통과했다. 배포·운영·브라우저 또는 생략 PG 성공 증거가 아니다.
- 09-11 11:31 최신 로컬 회귀: root 1053건 중 **952통과/101조건부 생략**, extractor 13통과, 실패/오류 0. 정책 service 26/HTTP 25건, Mapper/identity migration 정적 검증을 포함한다. 초안 API 통과는 정책 검증·게시나 DB 실제 실행 성공이 아니다.
- 09-11 11:59 최신 로컬 회귀: root 1104건 중 **1000통과/104조건부 생략**, extractor 13통과, 실패/오류 0. GoldenGate 10/CheckService 18/CheckController 18건과 추가 Mapper/MigrationContract를 포함한다. 서버 분류 사례 AG-001~030은 실파일·전체 QA/게시·운영 검증을 대신하지 않는다.
- 09-11 14:08 최신 로컬 회귀: root 1207건 중 **1090통과/117조건부 생략**, extractor 25통과, Node 30통과, 실패/오류 0. 정책 QA 예약/취소·snapshot·부분 증거 INCOMPLETE·lease guard를 포함한다. 실제 Linux/PG/운영 E2E 완료 증거는 아니다.
- 09-11 14:35 최신 로컬 회귀: root 1246건 중 **1124통과/122조건부 생략**, extractor25/Node30 통과·실패0. 배치 범위 service14/HTTP17과 Mapper/MigrationContract를 포함한다. 현재 수집 시작·분류 preview·적용·실행 중지/원복·전체 처리 UI는 미완료다.
- PG 접두 테스트는 `attachmentJobIntegrationTest`의 이전 40건 통과 기록과 현재 추가·변경한 153건을 구분한다. 최신153건은 Linux 실행이 필요하다. XML/parameter 오프라인 검증을 SQL/trigger 통과로 대체하지 않는다. 수동 Linux workflow와 skip/오래된 보고서 차단기를 추가했지만 원격 실행은 아직 없다.

## 파일 키

2026-09-12 화천 POST 증분: `discovery/HwacheonPostAttachmentDiscoveryProfileTest` 13건, `AttachmentDownloadInvocationTest` 4건이 exact source·POST4필드·불투명 값 비저장·긴 입력/표현식 거부·빈 영역/부분/한도·등록을 검증한다. 실제 다운로드 task는 GET4사례+화천2사례/총10파일이다. `AnnouncementAttachmentDiscoveryEvidenceTest` 6건과 worker26건에 폼 실패 전달/저장 대역을 포함하고, `AnnouncementAttachmentJobIntegrationTest.att013DiscoveryWarningIsBoundToImmutableManifestAndReadApi`에 새 코드 PG 입력을 추가했다(미실행). Node UI 회귀는 실패 사유 한글 표시·NO_FILES와 구분을 검증한다. ATT-013의 발견 실패를 저장 단계에서 잃지 않도록 연결했지만 전체 요구 완료로 판정하지 않는다.

2026-09-12 새올 GET 증분: `discovery/SaeolGetAttachmentDiscoveryProfileTest`는 4기관 구조·출처/URL/인코딩·NO_FILES/FAILED·한도·UNKNOWN 역할·등록 계약을, `SaeolAttachmentProfileLiveQaTest`는 실제 공식 상세4/첨부7개 다운로드·signature·임시 정리를 검증한다. ATT-004/011~014/020~024/028의 **부분 증거**이며 실파일 텍스트 추출/전체 profile/worker DB 성공은 아니다. 기존 기업마당/대전 서구의 파일명 기반 역할 추정도 제거했고 MANUAL 역할 재시도 보존 회귀를 유지했다. 상세는 `announcement-attachment-saeol-get-profiles-2026-09-12.md`를 따른다.

2026-09-11 추가: `AttachmentRuntimeGate`와 12개 빌드 합성 입력 AR-001~012, parser/CLI fixture 단위 테스트 및 `AttachmentRuntimeGateIntegrationTest`를 연결했다. 품질·한글/이모지·정확한 근거 좌표·원본 정리·전후 지문을 검사한다. **실제 Linux 실행은 미확인**이며 root 테스트에서의 조건부 생략이나 mock 실행은 ATT 완료 증거가 아니다. 상세 범위는 `announcement-attachment-policy-validation-2026-09-11.md`를 따른다.

`src/test/java/com/saneb/domain/announcementattachment/` 아래:

- E: `classification/AnnouncementAttachmentClassificationEngineTest.java`
- W: `worker/AnnouncementAttachmentWorkerServiceTest.java`
- F: `worker/AttachmentFileTypeValidatorTest.java`
- B/S: `discovery/BizInfoAttachmentDiscoveryProfileTest.java`, `discovery/SeoguSaeolAttachmentDiscoveryProfileTest.java`
- T/I: `extraction/AttachmentTemporaryStorageTest.java`, `extraction/IsolatedAttachmentExtractorTest.java`
- Runtime: `extraction/AttachmentRuntimeGateTest.java`, `extraction/AttachmentRuntimeGateIntegrationTest.java`; 12개 입력의 parser/CLI 검증은 `attachment-extractor/src/test/java/com/saneb/extractor/AttachmentRuntimeFixtureTest.java`
- PolicyValidation: `service/impl/AnnouncementAttachmentPolicyValidationServiceTest.java`, `service/impl/AttachmentPolicyValidationSnapshotFactoryTest.java`, `controller/AnnouncementAttachmentPolicyValidationControllerSmokeTest.java`. ATT-029/030/031/045/052/060/061에 연결되는 예약 멱등성·권한·변경/취소·원문 비노출·입력 고정/공유 슬롯 계약의 부분 증거다. 대응 PG 12건은 미실행이며 전체 policy QA/게시 검증은 아니다.
- HTTP: `controller/AnnouncementAttachmentControllerSmokeTest.java`
- CurrentController/CurrentService/IntakeServiceTest: 같은 패키지의 `controller/AnnouncementAttachmentCurrentControllerSmokeTest.java`, `service/AnnouncementAttachmentCurrentServiceTest.java`, `service/AnnouncementAttachmentIntakeServiceTest.java`
- PG: `src/test/java/com/saneb/db/AnnouncementAttachmentJobIntegrationTest.java`
- WorkerDB: `src/test/java/com/saneb/db/AnnouncementAttachmentWorkerIntegrationTest.java` — 실제 worker/DB/추출 8사례, HTTP만 합성. Linux 미실행이다. `AttachmentWorkerFixtureContractTest` 2건은 그 합성 입력·실패 계약만 로컬 검사한다.
- D: `src/test/java/com/saneb/domain/announcementsource/provider/content/AttachmentDownloadBoundaryTest.java`
- X: `attachment-extractor/src/test/java/com/saneb/extractor/AttachmentExtractorTest.java`
- SourceServiceImplTest: `src/test/java/com/saneb/domain/announcementsource/service/impl/AnnouncementSourceServiceImplTest.java`
- RoleService/RoleController: `service/AnnouncementAttachmentRoleServiceTest.java`, `controller/AnnouncementAttachmentRoleControllerSmokeTest.java`
- Checkpoint: `service/AnnouncementAttachmentCheckpointServiceTest.java`; mapper binding은 `src/test/java/com/saneb/db/AnnouncementAttachmentMapperBindingTest.java`
- RetryService/RetryEvidence/RetryController: `service/AnnouncementAttachmentRetryServiceTest.java`, `service/AnnouncementAttachmentRetryEvidenceTest.java`, `controller/AnnouncementAttachmentRetryControllerSmokeTest.java`
- HistoryService/HistoryController/ReadService: `service/AnnouncementAttachmentHistoryServiceTest.java`, `controller/AnnouncementAttachmentHistoryControllerSmokeTest.java`, `service/AnnouncementAttachmentReadServiceTest.java`
- CollectionService/CollectionController: `service/AnnouncementAttachmentCollectionServiceTest.java`, `controller/AnnouncementAttachmentCollectionControllerSmokeTest.java`

구현은 `src/main/java/com/saneb/domain/announcementattachment/{classification,discovery,worker,extraction,service,dao,controller}`,
`src/main/resources/mapper/announcementattachment`, V72/V73 migration과 연결된다. 기존 경로 guard는 announcementsource service 구현에 있다.

## 62개 요구 추적

| ID | 요구 | 현재 구현/테스트 근거 | 상태 | 남은 필수 증거 |
|---|---|---|---|---|
| ATT-001 | 제목 B: 요청/원문 0 | E.excludedOrUnmatchedTitleCannotEnterAttachmentEngine; PG.att001And002…; W.att001… | [~] | 실제 목록→상세/첨부 요청 0, 사용자 비노출 E2E |
| ATT-002 | 제목 조합 미충족 복구 금지 | E.excludedOrUnmatchedTitleCannotEnterAttachmentEngine; PG.att001And002… | [~] | 실제 worker 요청 0 |
| ATT-003 | 제목 A는 첨부 정상이어도 검수 | 분류 엔진 정책, PolicyGoldenGate AG-019 제목 A+정상 NOTICE 직접 검증 | [~] | 최신 실제 DB·브라우저 회귀 |
| ATT-004 | 파일명 B는 분류 입력 아님 | B.discoversAllFiveFilesAndKeepsFilenameOutOfClassificationInputs | [~] | 실제 filename-only B 표본의 DB 판정 |
| ATT-005 | NOTICE 첨부 B는 검수 | E.attachmentGroupBRequiresReviewNeverTitleExclusion | [~] | 운영 ACTIVE 근거·검수 표시 |
| ATT-006 | 첨부 A/B 동시: B 우선/A 보존 | E.attachmentGroupBRequiresReviewNeverTitleExclusion; PolicyGoldenGate AG-007 B 주사유·A/B 양쪽 근거 직접 assertion | [~] | 최신 실제 DB/API·운영 확인 |
| ATT-007 | 본문 부족을 같은 첨부가 보완 | E.attachmentCanSupplementMissingBodyWithoutChangingBase; PG.att016And036… | [~] | 실파일 worker→DB→API |
| ATT-008 | 본문 실패와 첨부 처리는 분리 | 별도 detail/descriptor 경로; WorkerDB.bodyFetchFailureStillCollectsAttachmentsAndPreservesBaseFailureEvidence 작성 | [~] | 실제 Linux/PG 실행 및 본문 수집기 실패+유효 첨부 실사이트 표본 |
| ATT-009 | 파일 간 AND 금지 | E.separateFilesCannotSatisfyAnd | [~] | 실제 다중 첨부 판정/locator |
| ATT-010 | FORM은 참고 근거 | E.formIsContextOnlyEvenWhenItContainsFullCombination | [~] | 관리자 역할 수정·재검수 흐름 |
| ATT-011 | UNKNOWN/불명확 scope는 검수 | E.unknownRoleAndUnreliablePdfScopeRequireReview | [~] | 실제 UNKNOWN 역할 표시/후속 행동 |
| ATT-012 | 정상 영역 0건은 NO_FILES | B.verifiedEmptyContainerIsDifferentFromMissingOrBrokenSelector; W.att012…; PG.att012And057… | [~] | 대상 profile별 실사이트 0건 |
| ATT-013 | selector 실패는 NO_FILES 아님 | B.verifiedEmptyContainerIsDifferentFromMissingOrBrokenSelector; S.changedLayoutAndUnresolvedAdditionalDownloadAreNotNoFiles; W.att013… | [~] | profile별 발견 실패·운영 표시 |
| ATT-014 | 일부 NOTICE 실패도 전체 근거 보존 | E.partialFileCannotBeHiddenByAnotherSuccessfulFile; W.att014EveryFile…; RetryEvidence 비선택 성공/실패 보존; W.manualRetryMultipleFailuresReusesNewSuccessOnItsNextLease | [~] | 최신 PG·다중 실파일 실패/재시도·성공 파일 재사용 |
| ATT-015 | 제외 문맥은 긍정 후보 아님 | E.negativeEligibilityIsNotPositiveCandidate | [~] | 본문/첨부 상충 표본·운영 검수 |
| ATT-016 | 혼합/스캔 PDF 품질 | X.blankPdfRequiresOcrAndDoesNotBecomeNoFiles; W.att020Ocr… | [~] | 텍스트+스캔 혼합 PDF 실파일 전체 페이지 검증 |
| ATT-017 | 한글 PDF/HWP/HWPX locator | X.pdfTextIsExtractedWithoutInventingReliableTableScopes; X.hwpxKeepsKoreanParagraphAndCellLocationsSeparate; X.hwp5KoreanRecordsHaveParagraphNotFakePageNumbers | [~] | 합성 fixture 외 모든 대상 profile/형식 실파일; 한글 PDF |
| ATT-018 | HWP 추정 page 번호 금지 | X.hwp5KoreanRecordsHaveParagraphNotFakePageNumbers | [~] | 실제 HWP section/paragraph/cell API 표시 |
| ATT-019 | 암호/손상/0byte 상태 | X.encryptedPdfIsExplicitlyBlocked; X.hwpEncryptedFlagIsNotIgnored; X.hwpTruncatedRecordIsNotCompleteText; Runtime AR-004/005/012 고정 입력 검사 | [~] | 실제 Linux 실행·0byte/손상 실파일의 worker 상태/로그 |
| ATT-020 | HTML 위장 파일 추출 금지 | F.errorHtmlIsNeverAcceptedBecauseItsNameSaysPdf; X.htmlNamedPdfIsNotAPdf; W.att020Ocr… | [~] | 공식 다운로드 endpoint 실패 응답 실증 |
| ATT-021 | 확장자 없는 binary 검증 | F.signatureAndMimeAcceptSupportedFormatsWithoutUrlExtensions | [~] | 실제 Content-Disposition/MIME/격리 parser |
| ATT-022 | redirect allowlist 재검증 | D.redirectRevalidatesPathBeforeDnsOrSecondHttp; D.postRedirectNeverForwardsFileNamesOrParametersToAnotherEndpoint | [~] | 기관별 GET redirect 실제 연결·host lease 교체 |
| ATT-023 | SSRF/DNS 재바인딩 방어 | AttachmentPinnedDownloadClientTest; D.stalledDnsTimesOutWithoutStartingHttpAndResolverExits | [~] | Linux 실제 pinned transport/사설망 canary |
| ATT-024 | 파일/스트리밍/누적 예산 | D.reservesBytesBeforeReadingAndKeepsActualBytesSeparate; D.deniedBudgetReadsNoBytesAndDeletesOnlyNewPartialFile; PG.att024And060…; T.att024… | [~] | 최신 PG + 실제 20/80 MiB 경계 |
| ATT-025 | 압축 폭탄과 앱 생존 | D.compressedOrOverLimitResponsesAreRejectedBeforeReading; X.archiveBombIsRejected | [~] | HWP record bomb·Linux OOM/주 JVM 생존 |
| ATT-026 | XXE/ZIP 경로 이탈 | X.hwpxDtdAndExternalEntityAreRejected; X.zipTraversalIsRejectedWithoutCreatingFiles; Runtime AR-008/009 CORRUPT·원본 정리 검사 | [~] | 실제 Linux 실행·canary 읽기/외부 요청 0 |
| ATT-027 | timeout/OOM/프로세스 종료 정리 | I.processCommandRequiresNetworkPidFilesystemAndMemoryIsolation; W.failedSaveNeverLeavesOriginalFiles; T.att038… | [~] | 실제 Linux timeout/OOM/프로세스 트리 종료 |
| ATT-028 | 매크로/embedded/JS 비실행 | S.arbitraryJavascriptAndTraversalAreNeverExecutedOrRequested; I.processCommandRequiresNetworkPidFilesystemAndMemoryIsolation | [~] | 매크로/embedded 파일 실표본 |
| ATT-029 | 동일 키 멱등 | PG.att029And030…; PG.att029SimultaneousSameKeyReturnsExactlyOneJob; PG.att029CompletedEvaluation…; Role/Retry/CollectionService 동일 actor/key/정규화 요청 | [~] | 최신 PG 재실행·정책/배치 API |
| ATT-030 | 같은 키 다른 요청 409 | PG.att029And030ReserveIdempotentlyWithoutChangingBaseOrAllowingDifferentRequests; CollectionService 다른 actor/body/key 충돌 및 HTTP 409 | [~] | 최신 PG·정책/배치 API |
| ATT-031 | lease 동시 claim/늦은 응답 | PG.att031TwoWorkers…; PG.att031ExpiredOwner…; W.att031SealedCrashRecovery…; W.restartRetriesFailedFileOnly…/Checkpoint; RetryEvidence 최종 fence | [~] | 최신 PG checkpoint 복구·실제 두 worker/중간 장애·sealed 복구 |
| ATT-032 | 같은 URL binary 교체 | set binary/manifest hash 계약; PG.att014And032…; W.newJobGenerationDoesNotReusePriorJobEvenWithSameLocator | [~] | 같은 URL 다른 binary 실수집 2세대 |
| ATT-033 | 같은 본문 첨부 갱신 예약 | SourceServiceImplTest.attachmentPlanIsFrozen…; PG.sameContentOnALaterRunWaits24HoursThenCreatesANewGeneration; IntakeServiceTest | [~] | 최신 PG 24시간 경계·실제 바이너리 교체 |
| ATT-034 | 내용/검수/version 변경 충돌 | PG.att034ChangedSource…; PG.att034And044BaseChange… | [~] | 검수 변경 API 동시성/늦은 worker |
| ATT-035 | 원문 삭제 후 되살림 금지 | PG.att035And038…; W.att001MissingEligibleSource…; WorkerDB.sourceDeletedDuringRealExtractionCannotBeRecreatedByLateWorkerResult 작성 | [~] | 실제 Linux 실행 중 source 삭제·임시 자원 정리 |
| ATT-036 | source 불일치 접근 404 | HTTP.sourceMismatchReturns404Wrapper; HistoryService/HistoryController 다른 source/evaluation/file 404; ReadService 제목 제외·QA·기본 판정 없는 조회 차단 | [~] | 최신 PG·운영 각 READ 역할의 교차 source 요청 |
| ATT-037 | 다른 source/release DB 근거 거부 | Linux35050057694 Migration3/3·PG.att061PolicyProfileAndRuleMustMatchWithoutUpdatingSourceOnFailure 통과 | [~] | 운영 API·최종 설치에서 교차 근거 거부 재확인 |
| ATT-038 | 제목 제외 cascade/임시 정리 | PG.att035And038…; T.att038SuccessAndExceptionScopes…; T.att038ExpiredOrphan… | [~] | Linux 실제 OS crash 후 원본 정리 |
| ATT-039 | 역할 변경은 새 set/STALE | RoleService/RoleController: 새 SEALED set·추출 provenance/시각 보존·버전/STALE·OFF 무HTTP; PG 역할/동시 요청/재사용 무결성 4건; 복구 UI·Node·합성 브라우저 OFF에서 역할 변경 예약/검수 잠금 | [~] | 최신 PG 실행·역할 변경 후 재검수 운영 E2E |
| ATT-040 | 미완료/오래된 확인 전환 거부 | ReviewServiceTest stale/setHash/OPEN/처리중/cross-source; ReviewControllerSmokeTest 409; RetryService/새 PG 선택 재시도 후 확인 STALE; 단건 복구 UI·Node 및 합성 브라우저 409 공유 쓰기 잠금/입력 보존/재확인 검증 | [~] | 최신 PG 실행·실제 운영 두 세션 충돌/재검수 |
| ATT-041 | 기존 V1/V2 검수 우회 차단 | 기존 전환/분류/재분류 service guard 테스트; PG.att041LegacyGuard… | [~] | 운영 소비자 경로·브라우저 확인 |
| ATT-042 | 첨부 미적용 기존 전환 유지 | AnnouncementSourceV2ConversionServiceImplTest 등 기존 회귀 | [~] | 운영 미적용 source 정상 DRAFT 흐름 |
| ATT-043 | 정상 확인→DRAFT 멱등 | ReviewServiceTest 같은 요청/다른 요청/자동 활성화 차단; ReviewControllerSmokeTest; PG 확인·동시 DRAFT/actor-source key race; 합성 브라우저 확인→별도 초안/응답 유실 재시도, Node 동일 payload/key | [~] | 최신 PG 실행·운영 중복 클릭 E2E |
| ATT-044 | OFF는 새 요청만 중지/검수 유지 | W.att040OffBeforeDetail…; PG.att040OffImmediately…; CurrentServiceTest; Current PG projection; PublicationImpact의 해당/전체 규칙 고정 작업·검수/적용/원복 관측·OFF 중지조건 해제 영향(읽기 전용) | [~] | 정책 게시/교체 transaction·실제 OFF·sealed 복구·최신 PG |
| ATT-045 | 역할/CSRF | HTTP/Current/Review/Role/Retry/History/CollectionControllerSmokeTest: READ 3역할, WRITE 2역할; PolicyControllerSmokeTest·PolicyServiceTest: READ 3역할/ADMIN 변경·금지 필드400/CSRF/직접 service 방어; 정책 UI8 SSR·22 Node·합성 브라우저 읽기 역할 변경0/명시 동의 | [~] | 정책 전체 QA/게시 API 및 실제 운영 세션·CSRF 확인 |
| ATT-046 | 배치 범위 고정 | V74 전체 목록·V75 고정 분할→batch 연결/전체 집계; BackfillIntegrationTest 1001건 두 분할 예약·신규 미편입·삭제/경합·불변 소속 시험12건 작성(PG 미실행); 전체 분할 UI17 Node·8 SSR | [~] | 최신 PG 전체 범위/경합·실제 승인 범위 운영 실행/최종 대조·운영 브라우저 |
| ATT-047 | scope/분류 preview 네트워크 0 | Batch/BackfillService 순수 전체/단일 범위 조회; BatchPreviewService exact 봉인 metadata·불변 이력/선택·HTTP0; 배치 UI 전체 페이지 개수/중복/최종 지문 대조·선택 보존 | [~] | 최신 PG/worker·운영 실제 HTTP0 및 실제 브라우저 검증 |
| ATT-048 | preview 뒤 버전 변경 충돌 | BatchPreview/Application CAS; BackfillService 전체 지문409, V75 분할 예약/시작/claim 입력 확인 및 기관 변경 차단 계약; 배치/분할 UI 응답 유실 동일키 유지·401/403/409 Node | [~] | 최신 PG·분할 예약/실행 실제 경합·운영 적용/충돌·실제 관리자 브라우저 |
| ATT-049 | 추가 검수/전환 뒤 rollback 거부 | 배치 원복 영향/ADMIN·CSRF 승인 API와 전체 inputHash 재검증 worker; 일반 원복 UI의 버전·효과/충돌/유실 동일 요청·잠금; 배치 승인 이력/영수증 대조·다른 배치 결과 해제 Node/SSR; 새 검수·DRAFT·기관 변경 차단 PG | [~] | 실제 PostgreSQL·일반/배치 복구 브라우저·운영 검증 |
| ATT-050 | 변경 없는 binding 원복 | 배치 고정 승인/조건부 CAS; 일반 APPLIED/실패 예약 원자적 복구·확인/실패 보존·무효 확인 STALE; 일반/배치/전체 분할 UI; 승인 목록 당시 영향/현재 영수증 분리, PG 페이지/삭제/혼합 이력3건 추가(미실행) | [~] | 실제 PostgreSQL/운영 되돌리기·일반/전체 배치 브라우저·승인 범위 분할 실행/대조 |
| ATT-051 | 연결된 운영 공고 보호 | Intake.selectProtectedLinkExists; 기존 link 멱등/guard | [~] | 명시 포함 batch의 보호/경고 정책·후속 신청 불변 |
| ATT-052 | 원문/secret/XSS 분리 | D.formIsBoundedImmutableAndDoesNotAppearInDiagnosticStrings; 근거/History HTTP no-store·좌표만 반환; PG.att036And052…; 합성 브라우저 HTML 문자 비실행·코드포인트 강조, 새 SSR/Node 테스트 | [~] | 운영 실제 로그·HAR·화면 검증; 수집 오류 로그 점검 |
| ATT-053 | 새 DB/업그레이드/checksum | Linux35050057694 freshSchemaAndV71UpgradePreservePriorChecksums·Flyway3/3, V83까지 순차 적용/빈 DB 검증 | [~] | 최종 배포의 실제 DB·설치 migration 재확인; 마지막 운영 확인09-15 V83과 구분 |
| ATT-054 | OFF/COLLECT_ONLY 기존 Golden 유지 | 기존 분류/수집 회귀와 E/CurrentServiceTest | [~] | 정책별 통합 Golden + 운영 ACTIVE 동일 release |
| ATT-055 | 동시 확인/전환 | source 잠금·평가 current unique; PG 동시 확인/동시 DRAFT·다른 source 멱등 키 race 테스트 추가 | [~] | 최신 PG 동시 실행 및 운영 E2E 검증 |
| ATT-056 | 문단/불명확 셀 AND 금지 | E.separateParagraphsCannotSatisfyAnd; E.unknownRoleAndUnreliablePdfScopeRequireReview | [~] | 실제 PDF/HWP/HWPX 표/문단 worker evidence |
| ATT-057 | OPEN/pending 판정 ID null | PG.att031UnsealedOrExpiredWorker…; CurrentServiceTest.enforcePending…; HistoryService pending의 과거 판정은 NOT_CURRENT | [~] | 최신 PG 및 운영 화면 pending→sealed |
| ATT-058 | source 같아도 다른 입력 조합 거부 | Linux35050057694 immutableEvidenceRejectsMismatchedBindingsAndCascadesWithSource·역할 근거 소속 시험 통과 | [~] | 최신 운영 API에서 교차 set/policy/base 거부 검증 |
| ATT-059 | 추출기 비밀·망·파일 격리 | I.processCommandRequiresNetworkPidFilesystemAndMemoryIsolation; Windows fail closed | [~] | Linux 실제 subprocess 환경/망/파일 canary |
| ATT-060 | 전역 동시성/한도 상향 금지 | PG.att060DownloadAndHostAndExtractionCaps…; PG.att024And060…; D/T 한도 검사; CollectionService 공유 수동 한도·132 HTTP 상한; PolicyService/HTTP 초안 1~80 MiB·임의 실행 설정 거부 | [~] | 최신 PG + 두 실제 worker·실제 정책 검증/게시 상한 |
| ATT-061 | base/정책 release 불일치 | E.mismatchedRuleReleaseIsRejectedBeforeEvaluation; PG.att061PolicyProfileAndRule…; Intake/SourceService/MapperBinding 미일치 ENFORCE·퇴역 규칙 준비 차단; PolicyService 초안 DRAFT/ACTIVE 규칙 허용; PublicationImpact 최신 QA/필수 재검증 및 정책 UI의 조회 버전·QA 누락/불일치·퇴역 선택 차단 | [~] | 최신 PG 실행·정책 전체 QA/게시/교체·운영 검증 |
| ATT-062 | COLLECT_ONLY는 preview만 변경 | CurrentServiceTest.collectOnlyKeepsBaseEffective…; HistoryService CURRENT_PREVIEW와 AUTO 태그 구분; Current/History PG projection | [~] | 최신 PG 목록/count/태그와 운영 브라우저 |
