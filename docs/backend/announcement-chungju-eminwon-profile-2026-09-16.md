# 충주시 공식 본문·새올 첨부 모델

## 단계 / 범위

전체 Gate0~8·ATT001~062를 유지하는 수집처 확장이다. 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증 순서를 바꾸지 않는다. `LGS-000137 / SAEOL_GOSI`는 V61/V62의 저장소 계약이며 현재 운영 설정을 새로 확인한 결과는 아니다.

- 공통 첨부 구현7종/등록 모델19개/전용 BODY18기관/추출 형식3종으로 확장한다. 전체 운영 대상223개라는 마지막 확인 수량을 지원 완료 수로 바꾸지 않는다.
- DB migration, v1/v2 API shape, UI, 운영 정책·데이터는 변경하지 않는다. 과거 migration과 사용자 output/Word를 보존한다.
- 충주 공개 참조3건을 schema2 catalog에 추가한다. 총30참조/실행 기대값1/정상 공고0이다. 새3건은 REFERENCE_ONLY이며 추출 품질·역할·정상 다중 첨부·정책 QA 통과를 뜻하지 않는다.

## 실측과 구현 계약

2026-09-16 공식 목록과 상세를9회의 제한된 읽기 전용 GET으로 확인했다. 각 HTML 요청은 연결3초/전체12초/1MiB/redirect0/재시도0이다. 본문·첨부 원문은 저장하지 않았다. 상세 구조 확인용72830은 지원사업/정상 후보로 채택하지 않았고 해당 파일은 요청하지 않았다. 후속 공식 QA의3요청과 별도이며 진단 포함 총12요청이다.

- 상세: `www.chungju.go.kr/www/selectEminwonView.do`, 메뉴510, `ancmt_mgt_no` 양의 공고번호. 저장 URL identity를 검증한 뒤 목록의 검색·표시 인자는 제거하고 정확한 두 인자만 재요청한다.
- BODY: 유일한 `table.bbs_default.view`의 직접 `제목` 표제·비어 있지 않은 값, 직접 `내용` 표제·`td[title=내용].bbs_content`를 요구한다. 본문 중첩 업무표는 보존하고 메뉴·담당부서·첨부명을 본문에 섞지 않는다. 변경/중복 구조는 BODY_SELECTOR_CHANGED이며 페이지 전체 대체는 없다.
- 첨부: 같은 표의 직접 `파일` 셀에서 다운로드 a/img/br 구조만 인정한다. 공식 `eminwon.chungju.go.kr/emwp/jsp/ofr/FileDown.jsp`와 파일명2개/날짜 경로3인자만 허용한다. 파일 소속은 해당 공고에서 발견한 URL과 locator의 공고번호/hash로 고정한다. 다른 파일/기관/메서드 redirect는 거부한다.
- 응답 HTML에 별도 현재 공고번호 hidden 값이 있다는 증거는 없다. 실제 제목은 고정 공개 시험에서 대조하며 DOM 번호 대조까지 구현됐다고 주장하지 않는다.
- 파일명으로 역할을 확정하지 않으며 UNKNOWN을 유지한다. 미지원 파일·미해결 요소를 분모에서 제거하지 않는다. 빈 공식 파일 셀과 발견 실패는 구분한다. TLS·MIME·signature·확장자·Content-Disposition 검사를 완화하지 않는다.
- 기존 기관 공통 코드는 바꾸지 않고 신규 프로필 지문에 전용 페이지 파서 코드까지 포함한다. 기존 태백 고정 기대값을 새 관측 없이 다시 쓰지 않는다.

| 고정 참조 | 공개 제목의 업무 성격 | HTML에서 발견한 첨부 |
|---|---|---|
| CHUNGJU-72625 | 교통약자 차량용 보조기기 추가지원 | HWPX1 |
| CHUNGJU-72039 | 중소기업육성기금 지원계획 변경 | HWPX1 |
| CHUNGJU-70852 | 결혼·출산가정 대출이자 지원 | HWP1 |

## 검증 계획과 결과

### 초기 DRAFT 제목 조건 미충족 진단

V65 seed와 `AnnouncementSourceClassificationEngine`의 실제 일치 근거를 대조했다. 아래는 격리 DB의 `ASCR-000001 / DRAFT` 진단이며 현재 운영 ACTIVE 규칙의 결과가 아니다. B그룹 적중도 아니고 두 지원사업의 업무 적합성 부정도 아니다.

| 참조 | 대상 근거 | 지원형태 근거 | 중단 원인 |
|---|---|---|---|
| CHUNGJU-72625 | 없음. `교통약자`는 초기 대상 목록에 없음 | `지원` / SUPPLEMENTARY | 대상 근거 없음. `지원 사업` 띄어쓰기를 `지원사업`으로 암묵 합치지 않음 |
| CHUNGJU-72039 | `기업` / BUSINESS / SUPPLEMENTARY | `지원` / GENERAL_SUPPORT / SUPPLEMENTARY | 보조 대상+보조 지원형태 조합. `중소기업`·`육성기금`의 별도 강한 규칙은 없음 |

현재 규칙은 한쪽 이상 STRONG인 대상+지원형태 조합 또는 제목 A 예외를 요구한다. 두 표본의 A/B는 모두 없고 TAG 근거는 각각 `지원`, `기업·지원`이다. 후속 요청0의 기존 계약은 유지한다. 회귀 검증은 이 원인까지 확인하며 정책 적합성을 새로 승인하지 않는다.

운영 규칙의 누락 여부는 재인증 후 ACTIVE 버전으로 먼저 확인한다. 이 두 사업을 수집 대상에 포함하려면 별도 DRAFT에서 대상 `교통약자`/`중소기업`, 지원형태 `육성기금`의 분류·강도·유의어를 검토하고 다른 용례의 오탐/누락과 전체 기존 공고 영향도를 비교해야 한다. 기존 `기업`/`지원`을 일괄 STRONG으로 승격하거나 관측 결과만으로 키워드·기대값·운영 정책을 변경하지 않는다. 이번 진단의 규칙/seed/운영 데이터 변경은0이다.

### 실행 결과

- [x] 공식 목록·상세 구조와 지원사업3개의 제목/파일 링크를 읽기 전용으로 확인했다. 파일 다운로드·추출 성공과 구분한다.
- [x] 첫 표적166건 중165통과/1실패는 시험이 유효하지 않은 빈 POST form을 생성한 원인이다. 요청 생성기의 기존 거부는 유지하고 유효한 POST가 프로필에서 거부되는지 검증하도록 시험을 수정했다.
- [x] 후속 표적166건/실패0·공식3건/실패·생략0이54초에 통과했다. 기관/메뉴/query/hash/DOM 경계, 한글 파일명 단일 decoding, 중첩 표·부분 발견·중복·파일10개 한도·미지원 보존·redirect 거부·registry를 검증했다.
- [x] opt-in 공식 QA: 실제 DRAFT seed에서72625·72039는 COMBINATION_NOT_MATCHED/후속 요청0이었다. 70852는 COMBINATION_MATCHED→BODY AVAILABLE/523자→HWP1개170,496bytes/application/octet-stream/signature 통과다. 총3요청/예약2,447,872bytes·원본 정리3/3·운영 쓰기0이다. 2건의 규칙 미충족을 자동 제외 B 적중·지원사업 아님으로 해석하지 않는다. 현재 운영 규칙 판정이 아니라 격리 seed 결과다.
- [x] BODY 실패도 첨부 진단을 중단하지 않고 별도 실패로 남기는 경계, 공고당150초/예약30MiB·파일20MiB·고정3표본을 구현했다. 후속 회귀에서는 이 음성2/양성1 판정이 바뀌면 실패하도록 고정했다.
- [~] 같은3표본의 Linux 격리 추출/텍스트 역할/종합 판정 경로와 수동 workflow 입력을 연결했다. 두 음성은 요청0, 양성의 HWP 전체 파일만 처리하며 실제 품질/역할은 원격 결과로 판정한다. 기존 공통44요청/80MiB·파일20MiB 상한, 관리자 최종 검증·운영 쓰기0을 유지한다. 새 Node 판정기17건+기존 제천32건=49/49 통과다. 아래 실제 실행은 외부 접속 실패이며 추출 성공이 아니다.
- [x] catalog/고정 제목/CI 경로를 포함한 후속 표적·패키징은1분38초 성공했다. 최종 전체 회귀4분36초 성공: root2710=2445통과/265조건부 생략/실패0이다. 패키지20·추출기88·bootJar는 유효한 선행 결과 재사용(UP-TO-DATE)이며 새 실행으로 합산하지 않는다. probe JAR는 재생성했다. 최종 Node 보고서 검증기59/59 및 기존27참조 불변·신규3참조전용을 확인했다.
- [x] 로컬 웹 JAR SHA256은 `c2680a76f97a1e891dc0aa3b83f876c8ba350a2ccdc7346eb12bae2292d80632`다. 운영 설치 지문이 아니다. Java/임시 PostgreSQL·단발 Node 종료를 확인했다.
- [x] `174230a`의 [Linux35047528916](https://github.com/FrostyCityMan/saneB/actions/runs/35047528916)에서 단위·HTTP·실제 임시 DB·격리 패키지·정책 부모 연결/취소/정리 단계는 성공했다. 보관 JUnit root2710=2444통과/266조건부 생략/실패0, 별도 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0이다. 아래 공식 관측 실패 때문에 workflow 전체는 **failure**다.
- [!] 같은 실행의 충주 공식3건은2통과/1실패다. 음성2건은 제목 미충족/본문·첨부 요청0. 양성70852는 BODY2시도 TIMEOUT 후 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT으로 종료했다. 예약3요청/2,097,152bytes, 파일0·추출 미실행·완전성false·원본 정리true·운영 쓰기0이다. Linux에서 충주 HTML 응답을 확보하지 못한 원인은 아직 미확정이며 선택자·파일 MIME·HWP 파싱 오류로 단정하지 않는다. 같은 조건 원격 관측을 재시작하지 않았다.
- [x] 위 제목 미충족의 실제 TAG·강도·대상/유형·A/B 부재를 기존 계약 시험에 추가했다. 관련47건/실패·생략0,1분5초 성공이다. production Java·규칙 seed·migration·API/UI·운영 정책은 변경하지 않았다. 로컬 웹 JAR 지문도c2680a76…로 불변이다.
- [!] 대체 로컬 Linux 확인은 Docker named pipe 부재, WSL 목록은 docker-desktop만 존재한다. Windows 비격리 추출·TLS 해제·새 인프라 생성으로 우회하지 않는다. AWS 인증은 앞선 AWS_AUTH_REFRESH_REQUIRED 이후 사용자 재인증 대기이며 이번에 동일 실패를 재시도하지 않았다.
- [ ] 실제 충주 HWP의 Linux 추출·역할·worker DB/API/정책 QA 및 운영 검증은 여전히 필요하다. 새 모델 추가나 로컬 다운로드 성공만으로 정상 공고 수나 정책을 활성화하지 않는다.

실행 명령과 최종 결과는 장기 진행 기록에 함께 갱신한다. 보고서는 metadata만 보관한다. 브라우저·운영 배포·worker 활성화·기존 데이터 적용은 이번 증분에서 실행하지 않는다.

```powershell
.\gradlew.bat :test --tests '*ChungjuEminwonAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaPlanTest' attachmentProfileDiscoveryQa --tests '*ChungjuEminwonProfileLiveQaTest' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL' bootJar --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-jecheon-observation-report.test.mjs scripts/qa/attachment-chungju-observation-report.test.mjs scripts/qa/attachment-contract-report.test.mjs
```

기존 승인 범위의 읽기 전용 Runtime 확인은 `AWS_AUTH_REFRESH_REQUIRED`로 SSM 전송 전에 중단됐다. 운영 정상 여부를 재확인하지 못했으며 임시 CA는 정리했다. 재로그인 요청을 전달했으며 취소된 배포의 재개 승인과 정책/데이터 범위 승인은 별도다.
