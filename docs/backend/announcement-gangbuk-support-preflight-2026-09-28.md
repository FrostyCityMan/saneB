# 강북 지원사업 상세 사전 확인 및 공통 관측 전달 경로 보완

## 목표와 완료 경계

등록된 강북 프로필에 지원사업 고정 참조가 없는 공백을 줄인다. 전체 ATT001~062+구간 분석/9 Gate와 운영 대상225개 범위를 유지한다. 이번 결과는 **상세 식별·첨부 발견**이며 본문 판정·실제 파일 다운로드/추출·worker DB/API·정상 기대값·운영 E2E 성공이 아니다.

- [x] 실제 worker가 사용하는 다단계 파일 전달을 공통 실파일 관측 테스트에도 연결했다.
- [x] 고정 지원사업179490의 DRAFT 제목 판정·공식 상세 식별·전체 첨부 영역 발견을 확인했다.
- [ ] 실제 본문 판정 → 발견한4개 전체 다운로드 → Linux 격리 추출·구간 분석 → worker/임시 DB/API.
- [ ] 추가 지원사업2건·형식별/정상 다중 첨부 근거 및 기대값 검토.
- [ ] 정책 QA·정확한 영향 승인·같은 SHA 운영 적용·역할별 운영 브라우저 검증.

catalog40참조/14프로필·저장 기대값1/정상0은 변경하지 않았다. 발견만으로 참조나 정상 기대값을 추가하지 않는다. 전체 Gate는8부분/1차단이다.

## 발견한 검증 경로 차이와 수정

`AnnouncementAttachmentBbsOfficialObservationTest`는 직접 pinned download를 호출하여 강북의 bridge GET → 게재기간 POST → 최종 파일 POST를 처리하지 못했다. 운영 `AttachmentDownloadGateway`는 이미 `AttachmentProfileDownloadFlow`를 사용하고 있다.

관측 파일 다운로드도 같은 flow로 연결했다. 공고 전체 누적 요청·byte 예산과 파일당 최대4 HTTP 요청을 함께 적용하며, bridge·기간 응답의byte도 합산한다. 게재기간 만료/예산 부족/부분 다운로드는 실패로 남기고 임시 파일을 정리한다. `isHome=Y` 우회·만료기간 무시·파일명 역할 확정은 없다. 기존 직접 GET 프로필과 제목/본문/첨부 판정 순서는 유지한다. 제품 코드·DB/API·migration·정책은 변경하지 않았다.

## 강북179490 실제 관측

후보는 [강북 공식 새소식](https://child.gangbuk.go.kr/portal/bbs/B0000145/view.do?menuNo=200081&nttId=182614)이 연결한 공고번호다. 새소식에 나온 파일 수를 실제 법정 게시판 검증 결과로 간주하지 않고 등록된 정확한host/path/menuNo의 [공식 상세](https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=179490)를 직접 확인했다.

- 고정 제목: `2026년 청년 어학・자격시험 응시료 지원 사업 모집 공고`.
- 2026-09-28 16:10:03~16:10:05 KST 로컬 상세 확인. 서울 서버 실행이나 운영 상태 조회는 아니다.
- 현재 migration을 적용한 별도 loopback 임시 PostgreSQL의 DRAFT seed에서 `COMBINATION_MATCHED` 확인 후 요청했다. 운영 ACTIVE 규칙 판정이라고 주장하지 않는다.
- 규칙 hash `609cea985b8f547292340539ab68293e6148ee76fd8636d270680aa19d7227ae`.
- `LOCAL_GANGBUK_LEGAL_GET_V1`, profile hash `6aa8ef570fdaf1dcc94e6e66b85d326a197de1057b6564368e97dc87ae781158`.
- 단일 `form#board`의 직접 hidden `nttId`와 직접 제목을 대조했다. 상세 hash `d41253cf9e689591d741b949cf1a9bc13c0179629ce70fdfeea00dcd5ec64b1c`.
- 발견 `FOUND/complete=true`, 전체4개, 형식 힌트 순서 HWPX/HWP/HWPX/HWPX, 미해석 경고0. 힌트는 binary signature 검증이 아니다.
- 첨부 다운로드0·본문 판정 미실행·운영 쓰기0·기대값 승인false·정책 QAfalse. 상세 임시 원본 삭제 확인.
- 결과 `build/qa-results/gangbuk-support-detail-179490-result.json`, SHA256 `fef3c6fef563c6c79cfcdc3d728aacc5d7cab56656bd9f9f22bb9b0f7cabcac9`.

## 예산과 중복 방지

이번 상세 사전 확인은 최대1요청·1MiB·전송30초/전체 시험90초, redirect/자동 재시도/파일 다운로드0이다. 실제 요청 예약1/1·278,528/1,048,576byte다. `gangbuk-support-detail-179490-reservation.json`을 네트워크 전에 CREATE_NEW로 확보하고 실패/중단 시에도 보존한다. 같은 경로를 재실행하면 예약 파일 때문에 중단하며 예약을 지워 우회하지 않는다.

09-12 강북184761/184744/184759/184760의 과거 구조 QA와09-15 본문 smoke는 별도 이력이다. 그 요청량을 없애거나 새 표본의 성공으로 이전하지 않는다. 후속179490 전체파일 QA에서는 이번1회/278,528byte를 누적 기준선에 더하고, 실행 전에 전체 파일/bridge/기간/본문 요청 상한과 기존 승인 범위를 대조한다. 중구32/32 등 다른 기관의 한도를 빌리지 않는다.

## 실행 검증

- `:test --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*LegalBoardAttachmentTransferTest'`: 신규8+기존9=17통과.
- `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*LegalBoardAttachment*Test'`: 55통과/외부 시험1조건부 생략·실패0. 실제 임시 seed의 기존 제목 stop/본문 검수 후 첨부 진행 계약을 보존했다.
- 기본 `:test --tests '*GangbukSupportDetailPreflightTest'`: 제목/식별 회귀1통과, 실제 조회1조건부 생략. 일반 CI에서 새 외부 요청은 실행하지 않는다.
- 명시 `gangbukSupportDetailPreflight`:2통과/생략0, 실제 상세 조회1회. 시험용 원본/임시 PostgreSQL/Gradle 종료. TLS 검증을 유지했다.
- 모든 Gradle 명령은 로컬 wrapper, `--no-daemon --max-workers=1`을 사용했다. 상세 실행에는 Windows ROOT trust store를 전달했으며 TLS 검증 비활성화는 없다.
- `attachmentBbsObservationProbeJar attachmentContractQaTest bootJar`: 관측 JAR 재생성·패키징20통과, bootJar는 제품 코드 불변으로 UP-TO-DATE 성공. 전체 root suite는 이번 증분에서 다시 실행하지 않았고 표적 회귀와 구분한다.
- `node --test scripts/qa/attachment-bbs-observation-probe.test.mjs`:9통과/생략0. 새 명시 task가 기존 지역 task/CI 외부 호출에 섞이지 않음을 확인했다. 실행한 Node는 종료했다.
- `git diff --check`: 오류0. 직전 e753374 Linux run36389360033은 조회 당시 진행 중이므로 이번 코드 또는 해당 실행의 통과로 보고하지 않는다.

브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 정책/기존 데이터/배포/운영 설정은 변경하지 않는다.
