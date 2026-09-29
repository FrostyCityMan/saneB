# 검단 파일명 헤더 복원과 광주 서구 검색 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 우선 단계다. 파일별 오류는 분리하고 수집 가능한 파일을 보존한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 개선·운영 설정·DB 변경은 이번 범위가 아니다.

- [x] 검단의 기존 ATTACHMENT_DISPOSITION_INVALID 원인을 실파일·실제 응답 헤더로 재현.
- [x] 검단에만 기존 엄격한 UTF-8 헤더 복원 옵션 연결. 공통 validator·발견 엔진·영종 지문 보존.
- [x] 관련 Java235통과·조건부1생략, bootJar, Node23/23,271영수증/224최신공고 재현.
- [!] Java 수집 재관측은 본문 HTTP_STATUS_ERROR·상세 ATTACHMENT_HTTP_403으로 중단. 다운로드 성공 증가 없음.
- [!] 광주 서구 공식 목록200, 검색은205 오류 응답. 미등록 상태 유지.
- [x] 조사 원본12개349,203byte 및 이번 실행 자원 정리. 기존 실패 보고서 보존.
- [ ] 잔여82개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인39.
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 광주 서구 공식 경로 조사

LGS-000067의 등록 목록 `https://www.seogu.gwangju.kr/menu.es?mid=a10807010000`은 같은 호스트 `/api/eminwon/gosiXmlList.es?mid=a10807010000`으로302였다. 목적지를 확인한 뒤 조회해200 목록과 실제 POST 검색 폼을 확인했다.

제목 검색 `keyField=A`, `keyWord=소상공인`은 HTTP 상태줄 `205 404`,781byte 오류 응답이었다. 폼 스크립트의 `nPage=1`과 나머지 hidden 항목을 포함한 완전한 POST도 같은 응답이었다. 총4요청 후 중단했으며 GET 우회·인증·세션 주입·반복 호출은 하지 않았다. 실패 원인이 사이트 코드인지 접근 정책인지는 확정하지 않는다. 정상 검색 결과 없음이나 첨부 없음으로 기록하지 않는다.

## 검단 응답 진단과 코드 변경

기존 표본 GEOMDAN-235의 공식 상세와 그 안의 실제 `bbsMsgFileDown.do?bcd=notice&msg_seq=235&fileno=1` 링크를 각1회 조회했다. 두 응답 모두200이었다. 파일46,390byte, MIME `application/octet-stream;charset=UTF-8`, ZIP prefix를 확인했다. 파일명 원문이나 헤더 전체는 문서에 복사하지 않는다.

실측 Content-Disposition은 UTF-8 바이트이며 Latin-1 문자열로 읽으면 제어문자가 포함됐다. 엄격한 UTF-8 디코딩 후에는 제어문자가 없었다. 실제 파일·헤더 fixture에서 기존 기본 validator는 ATTACHMENT_DISPOSITION_INVALID, 기존 UTF-8 복원 옵션을 적용하면 HWPX 기본 형식 검증을 통과했다.

- 실측 파일 SHA256: `65dd3e7db2539988ff81734bffc8d8576c63501a3eedd3e67800b57fbdb0820b`.
- `Utf8DispositionAttachmentDiscoveryProfile`은 기존 프로필을 위임하고 UTF-8 옵션만 켠다. 원래 프로필과 wrapper 코드 지문을 결합해 정책 변경을 식별한다.
- 시스템 구성에서 검단에만 적용한다. URL·호스트·공고 소속·GET/redirect·파일 개수·부분 성공·미리보기 제외 계약은 기존 엔진 그대로다.
- 기본 MIME·signature·파일명 확장자·경로·제어문자 검증을 완화하지 않았다. 관리자 설정을 추가하지 않았다.
- 영종 지문 `c2dc7616d6be80e3b9494fed91d321cc227d45a67d7f06356be8f675140d9318`과 기존 카탈로그는 그대로이며 영종을 다시 외부 호출하지 않았다.

이 검사는 ZIP 내부 HWPX 구조·텍스트 추출 성공을 증명하지 않는다. 파일명은 로컬 파일 경로로 사용하지 않는다.

## 수집 경로 재관측 결과

2026-09-30 07:23 KST(UTC `2026-09-29T22:23:18.958924300Z`), 새 검단 지문 `6f86deccb84b4b84153f83d3ee9a440671ab56ff34c354c006d8b9818ddf0f7a`:

- 제목 COMBINATION_MATCHED.
- 본문 FETCH_FAILED / HTTP_STATUS_ERROR,0자.
- 상세 DETAIL_DISCOVERY / ATTACHMENT_HTTP_403. 첨부 발견·다운로드에 도달하지 않음.
- INCOMPLETE, files=[], 원본 정리true, 운영 쓰기0.

실파일 fixture 복원 통과와 현재 Java 네트워크 접근 실패를 구분한다. 앞선 curl 진단의200을 상시 worker 성공으로 승격하지 않는다. files=[]를 첨부 없음으로 해석하지 않는다. 동일 재요청·차단 우회는 하지 않았다.

이전 실패 보고서는 `GEOMDAN-235-BEFORE-UTF8.json`에 해시 일치 복사하고 영수증 경로만 갱신했다. 신규 보고서를 추가해 최신 실패가 집계에 반영되도록 했다. 기존 실패를 삭제하거나 성공 기록으로 덮어쓰지 않았다.

## 검증·집계·정리

```powershell
# SANEB_GEOMDAN_HEADER_FIXTURE=true: 보존된 실제 진단 파일로 1차 테스트
.\gradlew.bat :test --tests '*GeomdanHeaderCompatibilityTest' --tests '*IncheonThirdDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --no-daemon
# 같은 fixture와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*GeomdanHeaderCompatibilityTest' --tests '*IncheonThirdDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GEOMDAN -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

1차23건 중22통과·조건부1생략(26초). 확장236건 중235통과·실패0·조건부1생략, bootJar 통과. 그러나 외부 관측 테스트1건은 상세403으로 실패해 복합 Gradle 명령의 최종 종료코드는1(1분59초)이다. 이 명령 전체를 통과로 보고하지 않는다. 생략은 이미 원본이 정리된 인천권 이전 HTML fixture이며 이번 검단 실파일 fixture는 통과했다. 원본 정리 후 해당 환경변수는 켜지 않는다.

분모는2026-09-28 15:56:12 KST의 활성223지역 수집원 스냅샷이다. 다운로드 확인141/223(63.2%)·잔여82·미등록43·등록 미확인39·첨부 오류46은 유지한다. 지역180+기업마당1=181프로필, 카탈로그236공고/179대상,271영수증/224최신공고다. 이전3표본·전체 파일 Gate는16충족/207잔여다. 이번 운영 재조회는 없다.

조사6요청: 광주 서구4 + 검단 진단2. 각15초, HTML2MiB·진단 파일10MiB 상한이다. 수집 관측1회 최대6요청/23MiB·상세1MiB, 실제 예약3회/2,097,152byte, 조사 포함 예약 상한9회다. 예약과 실제 wire 요청 수는 구분한다. 원본12개는 허용된 절대 경로·크기·SHA256 대조 후 개별 삭제했으며 복구에는 공식 재조회가 필요하다. 보고서·해시는 보존하고 이번 Node·Gradle·Java는 종료했다. 기존 프로세스와 미추적 파일은 건드리지 않았다.

전체 프로젝트 테스트·추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 작업 브랜치 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다. 두 접근 오류에 머물지 않고 다른 수집원 연결을 계속한다.
