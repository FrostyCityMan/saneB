# 태백·횡성·영월 BBS 첨부 수집 프로필

## 범위와 완료 기준

Gate 2 구현 증분이다. 기존 9개에 세 개를 추가하여 등록 첨부 프로필은 12개다. 전체 기관 지원 또는 전체 Gate 통과가 아니다. V61/V62 seed의 기관/목록 파서와 2026-09-12 공식 공개 목록·상세 응답을 대조했다. 운영 DB의 현재 매핑/정책은 별도 재확인한다.

- [x] 시스템 기관별 경로와 첨부 영역을 구현한다.
- [x] 발견 누락·잘못된 링크·세션 비전달·파일 상한·비지원 형식·역할 UNKNOWN을 검증한다.
- [x] 기관당 정상 지원사업 공고 최소 3건의 실제 발견·다운로드·signature를 검증한다. 전체 형식/추출 성공은 아니다.
- [ ] 실제 Linux 격리 추출·PostgreSQL·운영 E2E를 검증한다. 다운로드 성공을 추출 성공으로 바꾸지 않는다.

DB-first 기준 기존 V73~V78의 고정 profile/hash·locator·set/file/job 계약을 재사용한다. 새 테이블/컬럼/마이그레이션과 API 의미 변경은 필요하지 않다. v1 계약·화면·역할·제목 필터·본문 분류 정책을 변경하지 않는다. 파일명으로 자동 역할·후보·활성화를 결정하지 않는다.

## 고정 매핑

| 기관 / source code | 공식 목록 host, bbsNo / key | 프로필 | 첨부 요청 파라미터 |
|---|---|---|---|
| 태백 / LGS-000121 | www.taebaek.go.kr, 25 / 352 | LOCAL_TAEBAEK_BBS_V1 | key=352, atchmnflNo |
| 횡성 / LGS-000125 | www.hsg.go.kr, 65 / 821 | LOCAL_HOENGSEONG_BBS_V1 | atchmnflNo |
| 영월 / LGS-000126 | www.yw.go.kr, 17 / 273 | LOCAL_YEONGWOL_BBS_V1 | bbsNo=17, atchmnflNo |

모두 목록 parser code는 SPRING_BBS지만, 같은 목록 파서인 다른 기관을 자동 지원하지 않는다. 목록은 `/www/selectBbsNttList.do`, 상세는 `/www/selectBbsNttView.do`, 파일은 `/www/downloadBbsFile.do`다. 관리자에게 파서·host·selector 선택을 허용하지 않는다.

## 입력과 발견 경계

### 2026-09-14 본문 모델 연결 (로컬 구현)

동일하게 관측된 공식 게시판 표와 제목/내용 표식을 `LocalGovernmentNoticeProviderContentClient`에도 적용한다. 태백25·횡성65·영월17만 해당하며, 첨부 엔진6종/등록 profile12개/형식 추출기3종의 수를 늘리는 변경이 아니다. 본문 전용 모델3개를 기존 본문 클라이언트에 연결한 것이다.

- 성공: 내용 셀의 문장과 신청 링크만 2차 입력으로 사용한다. 게시판 제목·다른 공고·파일 영역·페이지 메뉴/푸터는 내용 셀 밖이므로 섞이지 않는다. 셀 내부의 실제 A/B 문장은 보존한다.
- 구조 변경: 표/제목/내용 셀이 없거나 여러 개면 `BODY_SELECTOR_CHANGED`; 내용 셀이 비면 `BODY_TEXT_EMPTY`다. 기존 FETCH_FAILED로 전달하고 첨부 3차 분석을 계속할 수 있으며, 본문 성공으로 위장하지 않는다.
- 정확한 상세 경로의 게시판 식별자가 누락/중복/잘못된 인코딩이면 공통 본문 선택으로 우회하지 않는다. 미측정 host/게시판은 기존 동작을 유지하며 지원 완료로 집계하지 않는다.
- 기존 파일 발견/다운로드 정책·UNKNOWN 역할·최종 검수·제목 선행 Gate는 불변이다. migration/API 필드/운영 설정 변경은 없다.
- 고정 단위 회귀와 `StandardBbsBodyContentLiveQaTest`의 기관별1개 공개 지원사업 본문(총3개)을 별도 확인한다. 실제 본문 표본 성공은 첨부 추출/분류/운영 E2E 성공을 뜻하지 않는다.

09-14 실행 결과: 본문 클라이언트27·수집 Service21·첨부 분류19·정책 프로세스13의 표적80건이 통과했다. 공식 본문 표본(태백184816·횡성424078·영월157016)3건도18초 실행에 통과했다. 이 표본 실행은 본문 GET만 수행하며 원문을 파일/DB/로그에 남기지 않는다. 전체 로컬 회귀는4분2초 성공: root2318건 중2077통과/241조건부 생략, 독립 패키지16·Node152통과, bootJar/설치 산출물 생성 성공이다. 추출기 task는 변경 없어 UP-TO-DATE였으므로 새25건 실행으로 집계하지 않는다. Linux 정책 부모/전체 Provider/운영 브라우저는 별도 미완료다.

### 기존 첨부 발견 계약

1. provider LOCAL_GOV_NOTICE, 기관 code, SPRING_BBS, 기존 canonical URL hash를 확인한다. 기존 source identity/저장 URL을 변경하지 않는다.
2. 원본 URL의 정확한 host/게시판/menu/notice ID와 알려진 검색·페이지 파라미터만 받는다. 중복 query·제어문자·다른 경로/인증정보/fragment는 거부한다. 네트워크 요청에는 key/bbsNo/nttNo만 전달한다.
3. V61에 HTTP로 저장된 태백 상세는 같은 정확한 host/path를 HTTPS로 승격한다. HTTP 통신은 하지 않는다. 횡성의 관측된 익명 jsessionid 경로는 원본 hash 검증 이후 제거하며 새 요청·locator·로그에 전달하지 않는다. 다른 세션 경로나 기관에는 적용하지 않는다.
4. 태백·영월은 `table.bbs_default.view`, 횡성은 `div.p-wrap.bbs.bbs__view > table.p-table.block`의 제목·본문 표식과 정확한 파일 th/td를 확인한다. 각각 view_attach / p-attach 목록과 이름/직접 다운로드 구조를 사용한다.
5. 미리보기는 관측된 기관별 정확한 경로·파일/공고 연결만 식별하고 요청하지 않는다. JavaScript 실행·임의 URL/폼·재귀 탐색·base URL 추종은 없다.
6. 영역 누락/중복·미해석 링크·잔여 텍스트·버튼·script·다른 속성의 숨은 파일은 FAILED/incomplete다. 이미 찾은 정상 descriptor는 남기되 전체 성공으로 표시하지 않는다. 확인된 빈 영역만 NO_FILES다.
7. 10개 초과는 LIMIT_EXCEEDED이고 부분 목록을 보존한다. 중복 파일은 ID hash로 묶되 이름 충돌은 불완전으로 기록한다. PDF/HWP/HWPX 외 파일도 발견 목록에 남기고 다운로드하지 않는다.
8. 모든 파일 역할은 UNKNOWN이다. ‘공고문’ 파일명은 NOTICE 승인 근거가 아니다. worker의 기존 검수·봉인·제목 제외 선행 Gate·자동 활성화 금지는 유지한다.

## 실제 응답 헤더 호환

최초 전용 QA는 29초에 9/9개 실패했다. 발견은 성공했지만 Content-Type이 기존 허용 목록과 달라 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 종료했고 원본을 삭제했다.

- 태백: `application/x-msdownload`.
- 횡성: `application/octer-stream` 오기.
- 영월: `application/octer-stream; charset=UTF-8` 오기.

이를 전역 generic MIME에 넣지 않는다. 새 시스템 프로필의 불변 허용 값만 worker와 동일한 validator에 전달한다. 예외는 이 두 MIME 값에 한정되고 **명시적 예상 형식 + 실제 8-byte signature + attachment disposition + 같은 확장자의 filename**이 모두 필요하다. HTML/실행 파일 signature, text/html을 허용하는 임의 설정, 예상 형식 부재·불일치, 이름 부재/경로/확장자 불일치는 계속 거부한다. OLE/ZIP 내부의 실제 HWP/HWPX 여부는 Linux 격리 추출기의 별도 판정이다.

두 번째 전용 실행은 37초, 태백3건 통과·횡성/영월6건 `ATTACHMENT_DISPOSITION_INVALID`였다. 두 기관의 공개 HEAD 응답에서 raw C1 제어문자25/29개, 엄격한 UTF-8 복원 후 제어문자0개를 확인했다. 이 두 프로필에만 기존 엄격 UTF-8 octet 복원 옵션을 연결한다. 태백과 기존 기본 프로필에는 적용하지 않는다. 원문 헤더를 보고서에 저장하지 않는다.

공통 interface/validator의 변경은 모든 profile hash에 반영되므로 과거 정책/QA 지문을 그대로 최신으로 인정하지 않는다. 전체 애플리케이션 코드 지문·게시 시 재검증도 유지한다. 정책 게시·ENFORCE·기존 데이터 적용은 이번 증분에서 실행하지 않는다.

## 고정 실제 QA 표본

| 기관 | 공고 ID | 공개 지원사업 범위 | 발견 형식/개수 |
|---|---|---|---|
| 태백 | 185101 | 동물복지형 스마트 축산 지원 | HWPX 2 |
| 태백 | 184816 | 청년농업인 육성지원 | HWPX 2 |
| 태백 | 184827 | 외국인 계절근로자 숙소 지원 | HWPX 1 |
| 횡성 | 424679 | 신혼부부 주거자금 대출이자 | HWPX 1 |
| 횡성 | 424078 | 중소기업 특례보증 | HWPX 2 |
| 횡성 | 424077 | 중소기업육성자금 이차보전 | HWPX 2 |
| 영월 | 157529 | 지역사랑 휴가지원 | PDF 2 |
| 영월 | 157016 | 청년 창업육성 | HWPX 1 |
| 영월 | 156846 | 과수분야 지원 | HWPX 1 |

공식 목록에서 검색은 searchCnd=SJ와 searchKrwd를 사용했다. 합격자·주소 명단·행정 개인자료 대신 공개 지원사업의 공고/양식을 골랐다. 위 표는 다운로드 전 발견 결과이며 최종 검증 결과는 아래 실행 기록으로 구분한다. 제목 분류·지원 자격 통과를 인증하는 표본이 아니다.

전용 QA는 상세 최대1MiB/파일20MiB/표본 총50MiB·직렬 실행이다. production pinned transport/host 검증·validator를 사용하고 상세1회+파일 개수만큼의 직접 GET만 허용한다. 모든 임시 HTML/binary는 finally에서 삭제하고 보고서에는 profile/hash·공개 ID·byte/hash·상태·정리 여부만 남긴다.

HWP 실제 파일, 비지원 혼합/빈 첨부의 실사이트 변형은 이 9건에서 발견하지 못했다. 합성 테스트를 실제 형식 검증으로 표시하지 않는다. Linux 격리 파싱·전체 파일 분류·worker/DB 저장·운영 브라우저는 별도 미완료 Gate다.

## 실행 기록

- 표적 최초 순수 프로필 시험: 19초 성공.
- MIME 및 헤더 실패 2회는 위 원인/범위로 보존한다. 같은 오류를 검증 해제로 우회하지 않는다.
- 헤더 보완 뒤 표적 프로필/validator/worker 62건과 실제 표본9건은 32초 실행에서 모두 통과했다. 실제 파일14개(PDF2/HWPX12), 1,499,273byte, 상세9+파일14=23HTTP, 임시 원본 정리9/9다.
- 위 이후 ‘PDF’/‘HWP’/‘HWPX’처럼 확장자 없이 형식명만 적힌 이름은 비지원으로 기록하도록 보강했다. 이 변경을 포함한 최종 전체 회귀·실사이트 재검증은 별도로 기록한다.

### 최종 코드 결과 — 2026-09-12 08:12 KST

```powershell
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa attachmentProfileDiscoveryQa --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
```

- root1932건 중1715통과/217조건부 생략, extractor25·독립QA13·Node7파일130통과, bootJar/설치 산출물 생성 성공.
- 전체 명령은3분35초 후 **실사이트22건 중 강북1건 HTTP400으로 실패**했다. 태백/횡성/영월의 최종9건은 전부 통과했고 위14파일/1,499,273byte/23HTTP/정리9건과 같다. 다른 profile의 네트워크 실패를 새 profile 성공으로 가리지 않는다.
- `attachmentProfileDiscoveryQa --tests '*LegalBoardAttachmentProfileLiveQaTest'` 및 같은 명령 한정 옵션으로 법정7표본 재실행은24초 성공했다. 강북의 두 번째 파일 BRIDGE_GET에서 발생한 간헐 HTTP400 원인은 미확정이며 해결했다고 주장하지 않는다.
- 새 프로필 단위27건, validator8건, worker30건은 전체 root 실행에 포함된다. 실제 Linux/PG191건은 별도 실행기 목록만 확인했고 통과0이다.
- 코드 목록1442파일과 주요5클래스의 JAR/디스크 일치 확인. 웹 JAR SHA256 `01a9c95c764cd0c6bc47ac99942c3d7ebf92f90858c9910f5917e92cbe277152`다. 현재 source와 산출물 일치이지 동일 SHA 운영 배포 증거가 아니다.
- GitHub 쓰기 권한 없음·AWS 재인증 필요를 다시 확인했다. 기존 Docker/Windows PG 차단은 재시도/보안 완화로 우회하지 않았다. 커밋·푸시·운영 변경·브라우저 실행은 하지 않았다. 목표는 계속 active이며 전체 Gate 최종 통과0이다.
