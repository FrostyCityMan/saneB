# 성주 공개 세션 첨부 수집

## 현재 단계 / Gate

- [x] 고정 공고586507의 공개 상세·공식 파일 링크 비교 진단
- [x] 익명 세션 수명·호스트·파일 범위 설계 및 구현
- [x] 쿠키 경계·pinned 전송·성주 파서 집중 테스트
- [x] 상시 worker·정책 QA·수집 관측기의 동일 다운로드 요청 연결
- [x] 실제 Java 수집기로 고정 공고 첨부2개 다운로드·형식 검사
- [x] 최신 실행 지문의 수집 대장 재구성 및 타 지역 대표 전송 회귀
- [ ] 운영 반영·worker/DB/API/UI E2E

기준 HEAD는 `aea0b12f88d54250581fa68843cfe9df1b477d7e`다. 기존 대장의204/223·잔여19개는 변경 전 코드의 증거다. 이번 공통 전송 변경은 모든 프로필 실행 지문에 영향을 주므로 과거 파일 근거를 최신 코드 성공으로 치환하지 않는다. 고유 지자체 수·전체 goal 완료율과도 구분한다. HWP 추출기1.0.16·운영 DB·규칙·설정·배포는 변경하지 않는다.

## 실측 근거

후속 갱신: [공개 세션 회귀와 집계 정정](announcement-public-session-regression-2026-10-01.md)에서704영수증/289표본 재현, 현재 코드16/223 실파일 확인을 완료했다. 아래1/223·대장 재구성 대기는 최초 구현 시점의 이력이다. 또한 아래 “최초 다운로드 미확인18개”는 **최초 미확인15개와 과거 성공 후 최신 미확보3개**로 정정한다. 운영·전체 goal 완료는 아니다.

고정 상세는 `https://www.sj.go.kr/page.do?mnu_uid=1044&bod_uid=586507&cmd=258`이다. 실제 응답의 `form#frm div.bod_view > dl.view_file > dd`에서 확인한 첫 번째 공식 `/programs/board/board_download.do?file_uid=…` 링크만 비교했다. 다른 공고·경로·뷰어를 탐색하지 않았다.

| 비교 | UTC 관측 시각 | 파일 응답 |
| --- | --- | --- |
| 익명 쿠키와 상세 Referer | 2026-09-30T22:59:00.1451409Z | 200, application/octet-stream,114688byte, OLE 서명 |
| Referer만 사용 | 2026-09-30T23:00:26.9865287Z | 200, text/html,374byte, 파일 서명 아님 |
| 익명 쿠키만 사용 | 2026-09-30T23:02:20.9714449Z | 200, application/octet-stream,114688byte, 동일 OLE 서명 |

두 바이너리 응답 SHA-256은 `d04fb09b15a9e97a2bd334fcb34589b9ee4b9a986b697107ea513c34d412578f`다. 상세는 매번133324byte이며, 총6HTTP 요청이다. 공개 페이지가 발급한 쿠키 이름은 JSESSIONID·LENA-UID·L-VISITOR다. 값은 기록·출력하지 않았다. 어느 쿠키 하나가 필수인지는 미확인이다. 원문·파일 저장0, 운영쓰기0이다.

이는 .NET 비교 진단으로, Java 수집기·모든 첨부·HWP 내부 구조·텍스트 추출 성공을 입증하지 않는다. 상세 주소만 전달하는 변경은 충분하지 않다는 근거이며, 다른 지역에 같은 원인을 적용하지 않는다.

## 구현 계약

1. 제목 판정·본문·첨부 발견 이후, 성주 프로필만 발견한 descriptor의 noticeId와 attachmentId를 확인해 공개 세션 계획을 만든다. DB/API 입력으로 쿠키·임의 헤더·URL을 받지 않는다.
2. 파일 한 건마다 새 메모리 세션으로 고정 공개 상세GET→파일GET 최대2요청을 수행한다. 두 요청 모두 기존 HTTPS·DNS pinning·프로필 승인·host lease·heartbeat·요청 및 DB 바이트 예산을 거친다.
3. 상세 응답은200·HTML·엄격 UTF-8·최대2MiB다. 원문은 파일에 저장하지 않는다. 기존 공식 영역 파서로 같은 locator·파일 URI·표시 이름이 다시 발견될 때만 파일 요청을 허용한다. 다른 첨부의 오류는 해당 정상 파일을 버리는 이유로 사용하지 않는다.
4. 같은 공개 상세의 승인된 쿠키 이름만 메모리에 보관한다. 정확한 HTTPS host 및 파일 URI로 한 번만 전송하며 Domain·Path·만료를 검사한다. 전역 jar·ThreadLocal·로그인·브라우저 쿠키·DB/로그 보관은 사용하지 않는다.
5. 상세와 파일을 합쳐30초, 원래 파일 바이트 한도 이내다. 상세 바이트도 예산에 포함한다. redirect·자동 재시도·다른 파일·preview·HTTP 강등·TLS 예외는 허용하지 않는다.
6. 성공 여부와 무관하게 쿠키 참조를 해제하고 실패 부분파일을 정리한다. 파일의 MIME·disposition·signature 검증은 기존 검사기를 그대로 사용한다. 수집 실패는 개별 파일 오류로 보존하며 다른 파일·지역을 계속 처리한다.
7. worker, 정책 QA 실행기, 고정 공고 관측기가 같은 `selectDownloadRequest` 계약을 사용한다. 다른 프로필은 기존 요청 생성 기본값을 유지한다. 세션 클래스 및 관련 내부 클래스도 실행 지문에 포함한다.

## 검증 기준

성공 기준: 익명 세션 누출·교차 호스트 전송 없이 실제 Java 경로에서 공식 파일을 받고 기존 파일 형식 검사 통과. 상세 및 파일 모두 승인·요청·바이트 callback을 통과. 부분 실패와 성공을 분리하고 원본·자원을 정리한다.

실패 기준: HTML200을 파일 성공으로 집계, 진단을 상시 성공으로 표현, 쿠키 값 출력/저장, 기존 파일 근거를 새 지문으로 치환, 운영 정책 자동 활성화, 실패 검사를 삭제해 테스트 통과 처리.

집중 테스트에는 쿠키 이름/Domain/Path/30초/단일 사용/분리/헤더 한도, 본문 링크 변경, 양 단계 승인 거부·redirect 차단·예산 차단·기존 파일 보존을 포함한다. 브라우저는 사용자 정책상 실행하지 않는다.

기존 Linux 전체 run36787477542는4420개 중4099통과·1실패·320생략으로 종료됐다. 실패는 화천 고정 worker 계약이며 이번 성주 구현 전 기준이다. 이를 새 구현의 전체 테스트 성공으로 사용하지 않는다.

## 실제 Java 수집 결과

2026-09-30T23:24:39.191641600Z, `attachmentRegionalCollectionObservation`의 `SEONGJU` 고정 공고·`PUBLIC-SESSION-02` 관측에서 본문1842자, 공식 첨부2개 발견·다운로드2개·실패0을 확인했다. 기존 형식 검사기에서 HWP MIME·파일명 헤더·OLE 서명을 모두 통과했다.

| 파일 | 바이트 | SHA-256 |
| --- | ---: | --- |
| 공식 첨부1 | 114688 | d04fb09b15a9e97a2bd334fcb34589b9ee4b9a986b697107ea513c34d412578f |
| 공식 첨부2 | 59904 | b4bad426b1ead0e4ffc0e94bad4d808663a9d296c00679ab38df849a2ec87d2a |

보고서: `build/reports/attachment-regional-collection/SEONGJU-586507-PUBLIC-SESSION-02.json`, SHA `ce0728d4af8cba91fb74037b006276de7faed61044af15f19152ee5e4abdf4e7`. 프로필 실행 지문은 `7c82bf7fae6b862c8937a092e15fe6524ea652ed17ad95781a3ead9f9aaee574`다. 원본 정리true·운영쓰기0·추출 미실행·규칙 승인 없음이다. 본문 A그룹 검수 판단을 유지한다.

첫 관측 `PUBLIC-SESSION-01`도 보존한다. 첨부1개 성공 후 기존6예약 상한 때문에 다른 파일이 승인 callback에서 중단됐다. 사이트의 경로 오류로 단정하지 않는다. 본문 예약2 + 상세1 + 두 파일 각각 상세/파일2 =7예약으로 성주 수집 전용 상한을 변경했다. 바이트23MiB·시간 제한은 유지하며 다른 지역 상한은 바꾸지 않았다. 이에 따라 과거12지역 묶음의 향후 상한 표기도74→75로 변경했으며 과거74예약 실행 기록은 수정하지 않았다.

두 번째 관측은7예약·2703360예약byte 이내다. 예약값은 실제 wire 요청·전송량과 같다고 주장하지 않는다. 정상 파일과 실패의 병행 보존 및 파일 단위 작업 계속 수행은 worker 회귀로 검증한다.

과거 파일 근거 포함 최초 다운로드 확인 수집원은 **204→205/223**, 최초 다운로드 미확인은 **19→18개**다. 새 공통 코드의 실제 재검증은 **성주1/223**이며, 나머지222개를 새 코드 성공으로 표시하지 않는다. 정식 inventory/영수증/대장 재구성은 다음 단계다. 기존 봉인 대장686영수증/289표본은 변경 전 코드 이력으로 보존한다.

남은 최초 다운로드 미확인18개: 은평·서대문·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·공주·아산·의성·봉화·남해.

## 최종 로컬 검증 / 후속

```powershell
.\gradlew.bat --no-daemon :test --tests '*AttachmentPublicSession*Test' --tests '*AttachmentPinnedDownloadClientTest' --tests '*AttachmentDownloadBoundaryTest' --tests '*AttachmentRefererDownloadTest' --tests '*GyeongbukThirdDownloadContractTest' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*RegionalTransportLinuxContractTest' --tests '*AttachmentContractWorkflowTest' bootJar --rerun
.\gradlew.bat --no-daemon attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SEONGJU -PsanebCollectionReportLabel=PUBLIC-SESSION-02 -PsanebCollectionWindowsTrust=true --rerun
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs scripts/qa/attachment-github-collection-receipts.test.mjs scripts/qa/attachment-regional-linux-workflow.test.mjs scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs scripts/qa/attachment-ulsan-namgu-survey.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

최종 선택 Java228개 전부 통과·생략0, Node64개 통과, bootJar 성공이다. 처음 확대 테스트의54실패는 신규 default 메서드를 호출하지 않은 Mockito 대역이 null 요청을 돌려준 문제였다. 일반·다단계 프로필 대역에 실제 default 메서드 호출을 연결했고 검증 조건을 삭제하지 않은 채 재검증했다. 공개 세션의 링크 변경·본문 오류·만료·쿠키 차단·한도 초과도 worker의 기존 고정 오류 코드로 구분하며 다른 성공 파일을 유지하는5개 회귀를 포함한다.

관측 생산 클래스 SHA는 `020fb8bc2256fab329fa07060218181a9fedb5d536b39a27d1a53d43e0c74e2b`다. 보관 대장686/289 재현은 과거 inventory의 무결성 검증이지 새 코드223지역 재검증이 아니다. 신규 공통 지문과 두 성주 관측을 다음 대장 갱신에서 별도로 반영한다. 상시 처리/정책/운영 DB·API 검증은 하지 않았고 운영 배포 Gate는 미완료다.
