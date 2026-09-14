# 태백 전체 첨부 고정 기대값 연결

## 범위와 합격 기준

- P3 실제 파일 QA 증분이다. 제목 → 정제 본문 → 전체 첨부 텍스트 → 최종 관리자 검증을 유지한다.
- `TAEBAEK-184816`의 사전 관측을 검토한 뒤 catalog에 파일 2개 모두의 locator/binary/text/blocks/role-assessment hash와 역할 규칙 버전을 고정한다. 실행 중 관측한 값으로 기대값을 자동 덮어쓰지 않는다.
- 혼합 문서의 UNKNOWN은 검수 사유다. 양식 FORM 판정 성공 또는 고정 기대값 일치를 정상 후보·정상 다중 첨부 공고·전체 정책 QA 통과로 계산하지 않는다.
- 기존 실제 세 단계 관측과 별도로 production `AttachmentProviderQaCaseExecutor`의 고정 공고 계약을 재실행한다. 후자는 BODY 분류·worker DB/API·관리자 검수 E2E를 대체하지 않는다.
- 새 검증의 계획 범위는 임시 PostgreSQL의 저장소 seed 전체(비활성 포함, 미삭제)와 두 전국 채널이다. 실제 운영 대상/활성 상태를 나타내지 않는다. 미구현 기관과 기대값 없는 참조를 분모에서 보존한다.
- 외부 실행은 관측 `attachmentBbsOfficialFileObservation`과 고정 계약 `attachmentBbsFixedCaseQa`를 별도 opt-in task로 분리한다. 각각 최대 420초·44요청·80MiB다. 같은 CI에서 둘 다 명시 실행하면 순차 합산 상한은 88요청·160MiB다. 기본값은 둘 다 OFF이며 일반 단위시험은 외부 호출하지 않는다. 원본은 종료 시 정리하고 metadata만 남긴다.
- 운영 DB·정책·수집 플래그·기존 데이터 변경, 자동 ACTIVE, schema/v1 계약 변경은 없다. V1~V83은 보존한다.

## 검증 체크리스트

- [x] Run30의 같은 SHA Linux 결과와 원본 정리를 검토했다. 전체 실행은 실패다.
- [!] 현재 profile/역할 지문 및 파일 2개 전체 기대값 연결은 새 실제 추출 관측 부재로 보류한다. catalog 참조18/기대값0을 보존했다.
- [x] production 실행기에 고정 catalog·전체 seed 범위·임시 저장·현재 runtime을 연결하는 시험을 구현하고 컴파일했다. 실제 파일 실행 통과는 아니다.
- [ ] 정상 수 0·전체 coverage 미완료·7일 만료 및 지문 변경 차단을 검증한다.
- [ ] production 실행기로 실제 전체 파일을 다시 비교하고 결과/예산/정리 metadata를 확인한다.
- [x] 표적116건/생략0/37초 통과. 전체 회귀3분27초 성공: root2556=2299통과/257조건부 생략/실패0, QA 패키지20/20. extractor/bootJar/설치 task는 UP-TO-DATE이며 새 실행으로 합산하지 않는다.
- [ ] 새 SHA의 Linux 관측 결과·원격 SHA·고정 기대값 연결 후 실제 단건 재검증을 확인한다.

검증 명령은 `:test --tests '*AnnouncementAttachmentBbsFixedCaseQaContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest'`와 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa`이며 모두 `gradlew.bat --no-daemon --console=plain --max-workers=1`로 실행했다. production JAR SHA256은 변경 전과 같은 `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`다. Node 일회성 검사로 catalog18참조/기대값0 보존을 확인하고 종료했다. 운영/브라우저 검증은 이번 회차 미실행이다.

## 관측 실패와 후속 조치

[Run30](https://github.com/FrostyCityMan/saneB/actions/runs/34902614936), SHA `72a78c89149267cdacffe2d0ce73c9a5035294b4`의 관측 시각은 `2026-09-14T22:21:10.553004673Z`다. TITLE은 COMBINATION_MATCHED였으나 BODY가 2회 모두 TIMEOUT/FETCH_FAILED였다. 파일 발견/다운로드/추출은 실행되지 않았다. 원본 정리true·운영 쓰기0·본문 상한 예약2요청/2MiB다. 현재 태백 profile hash는 `23c1c1af20689d40c47684f1bf3a36090656181572ff305dc0405d04d5f6a28b`지만 지문 관측만으로 파일 기대값을 만들지 않는다.

본문 실패를 먼저 기록하고 남은 예산 안에서 첨부 발견·추출을 진단하도록 관측 시험을 보완했다. 마지막 BODY 완전성 검증은 유지하므로 파일이 성공해도 본문 실패를 전체 성공으로 승격하지 않는다. 실패/빈 본문/비활성에 대한 음성 시험을 추가했다. 운영 수집기나 timeout/재시도 설정은 바꾸지 않았다.

사전 기대값은 여전히0이므로 `[fixed-bbs-qa]` 또는 해당 수동 입력을 켜지 않는다. 후속 실행은 `[official-bbs-observation]`으로 위 진단 개선만 검증한다. 새 실파일·텍스트·역할 관측을 검토한 뒤 기대값을 별도 변경하고 production 단건 실행기 시험을 켠다. 기대값이 없거나 만료/변경됐다면 실행기는 HTTP 전에 준비 실패를 보고해야 한다.

## 실패 및 잔여 경계

외부 원문/파일/역할 지문 변경, 누락, 미실행, 만료, 다운로드/추출/정리 실패는 실패 또는 미완료다. 기대값을 즉석에서 바꿔 성공으로 만들지 않는다. 고정 계약 PASS는 전체 기관·형식·worker·운영 배포·승인된 기존 데이터 처리·운영 브라우저 Gate 완료가 아니다.

## 2026-09-15 후속 접근 결과

[Run31](https://github.com/FrostyCityMan/saneB/actions/runs/34904550080), SHA `0625b13fb25ddf7001f5e5848cb46087ae01edfc`도 전체 실패다. `2026-09-14T22:45:25.818698755Z`의 TITLE 조합 통과 → BODY 2회 TIMEOUT/FETCH_FAILED → base REVIEW_REQUIRED/BODY_FETCH_FAILED 보존 → DETAIL_DISCOVERY의 TRANSPORT_TIMEOUT을 확인했다. 첨부 발견/다운로드/추출은 미실행이다. 상한 예약3요청/2MiB, 원본 정리true/운영 쓰기0, profile hash는 Run30과 같다.

같은 회차 Windows의 기존 `attachmentProfileDiscoveryQa --tests '*StandardBbsBodyContentLiveQaTest.readsMeasuredOfficialBodyWithoutRequestingFiles'`는 기본 Java 신뢰 설정에서3건 NETWORK_ERROR였다. 저장소의 기존 Windows-ROOT/NUL 인자를 명시한 뒤에는 태백184816·횡성424078·영월157016 모두 AVAILABLE·시도1·redirect0으로3/3 통과(전체18초/HTTP 시험2.088초)했다. TLS 검증을 끄지 않았다. 이것은 로컬 BODY 접근 근거이며 Linux 접근·첨부·운영 성공 근거가 아니다.

두 Linux 실행에서 본문과 별도 pinned 상세 요청이 모두 시간 초과했으므로 단순 parser 문제로 단정하지 않는다. 실행 환경별 네트워크/사이트 접근 차이는 추정이며 세부 원인은 아직 미확인이다. 같은 CI 공식 표본을 즉시 반복하지 않는다. 기존 서울 AWS 검증 환경의 재인증 또는 외부 접근 상태 변경 후 다시 확인한다. 원래 범위의 다른 구현·필수 테스트는 계속 진행하고, 현재 파일 기대값0과 전체 미완료 상태는 보존한다.
