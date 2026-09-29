# 서대문 본문·첨부 연결과 노원 표본 조사

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 일부 실패는 개별 오류로 분리하고 정상 파일은 보존한다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 서대문 공식 게시판의 제목·본문·첨부 영역 분리 및 고정 다운로드 경로 연결.
- [x] 서대문313956 본문135자 AVAILABLE/ACCEPTED, HWPX 첨부4개 발견.
- [!] 파일4개 모두 FILE_DOWNLOAD / TRANSPORT_TIMEOUT. 다운로드 성공0개이며 성공 지역에 포함하지 않는다.
- [x] Java213통과/조건부1생략·Node23·bootJar·수집 전용 관측 실행 완료.
- [x] 기존225영수증/181공고 보존, 총226영수증/182공고 재현.
- [x] 조사 HTML5개1,099,956byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록85지역 연결, 등록 후 다운로드 미확인29지역 후속.
- [ ] 전국 목록 자동 유입·상시 worker·추출·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번에는 운영 설정을 재조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 첨부 최소1개 다운로드 확인 | 109 | **109/223 (48.9%)** |
| 다운로드 미확인 | 114 | **114** |
| 프로필 등록 지역 | 137 | **138** |
| 미등록 지역 | 86 | **85** |
| 등록 후 다운로드 미확인 | 28 | **29** |
| 첨부 오류 관측 지역 | 33 | **34** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역138+기업마당1=139프로필. 카탈로그194공고/137대상, 신규 expectation=null, 기존 승인 기대값1개를 유지한다. 연결·본문·발견 성공을 다운로드 성공으로 표현하지 않는다. 오류 지역은 성공 지역과 겹칠 수 있어 잔여 수에 합산하지 않는다.

## 실제 관측

서대문 `LGS-000014 / SEODAEMUN-313956`: `2026년 서대문구 소상공인 라이브커머스 지원사업 참여자 모집 재공고`. 제목 조합 통과, 상세 제목 일치, 본문135자 확인 및 첨부4개 발견. 각 파일 다운로드의 전송 호출1회가 시간 초과했다. 원인 변화 없이 재요청하지 않았다. 현재 근거만으로 서버 장애·문자 인코딩·클라이언트 전송 환경 중 원인을 확정할 수 없다.

- 관측 상태: `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, discovered4/downloaded0/failed4.
- 프로필 hash: `a738a794f4ad9bf8c82e0bc5c59a871e580f70c42bc9823be7a4c7f19eecba36`.
- producer class hash: `13fea75abaa3efc2c46ae590be2ba20a9fafa88083ad2a11ff4e54bd8a32c0c9`.
- 파일 역할 UNKNOWN. 파일 byte/hash·텍스트 추출·정책 QA 승인·운영 쓰기 근거는 없다.

노원 공식 목록과 제목검색은200이었으나 ‘소상공인’ 단일 제목 검색에서 데이터 없음이 표시됐다. 이번 표본 미확보일 뿐 지역 전체 지원 공고 부재로 판단하지 않는다. 후속 표본 확보 대상으로 유지한다.

서대문 상세에서 공공누리 제4유형 표시를 확인했다. 이번 기술 QA는 상업적 재사용 허용 여부 확인이 아니다. 운영 재배포·노출 전 원문 및 첨부별 이용 조건 검토가 별도로 필요하다.

## 구현 계약

`SeodaemunNoticePage`는 `table.boardWrite` 아래 제목 `td.subject`, 본문 `td#viewCon.viewCon[colspan=4]`, 정확한 첨부파일 라벨의 다음 셀을 분리한다. 메뉴·담당부서·첨부명을 본문에 넣지 않고 본문 이미지를 다운로드하지 않는다. EUC-KR 공식 HTML을 실제 fixture로 검증했다.

`SeodaemunAttachmentDiscoveryProfile`은 고정 수집원·목록 parser·원문 hash·HTTPS host/path와 공고 query를 검증한다. 공식 첨부 셀 안의 `/downloadFile.do`만 요청한다. 게시판82 경로, UUID 저장 파일명, 원본 파일명과 표시명의 대응을 확인한다. `/htmlView/html.jsp` 미리보기는 같은 파일을 가리키는지만 대조하며 호출하지 않는다. 유효한 descriptor는 다른 링크 실패에도 유지한다.

PDF/HWP/HWPX·미지원 확장자·중복·충돌·부분 실패·10파일 제한·외부 host/HTTP/추가 query 거부·본문 구조 변경·본문과 첨부/이미지 분리를 테스트했다. 기존 공통 프로필 지문·migration·DB/API·UI·HWP 추출기·운영 설정은 변경하지 않았다. 이 연결만으로 목록 자동 유입 또는 상시 수집 완료를 주장하지 않는다.

## 검증·요청량·미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SEODAEMUN_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SeodaemunDownloadContractTest' --tests '*MapoDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEODAEMUN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

Gradle2분35초 성공. Java214건 중213통과·기존 마포 실제 HTML fixture1건 조건부 생략, 신규 서대문 실제 HTML 검사는 통과했다. 수집 관측 태스크 성공은 오류 기록과 정리까지 실행됐다는 뜻이며 **파일 다운로드 성공이 아니다**. Node23통과, bootJar 성공. 환경변수는 이전 값을 보관하고 finally에서 복원했다. 원본 삭제 후 fixture 환경변수는 켜지 않는다.

조사5요청: 서대문3(목록/제목 검색/상세), 노원2(목록/제목 검색). 요청당15초/2MiB, TLS 검증·자동 redirect0 유지. Java 관측7요청/23MiB 상한 중 본문 포함예약7회·2,195,456byte, 조사 포함예약 상한12회다. 예약 수와 실제 HTTP 호출 수는 다르다. 관측 원본 finally 정리 및 조사5개 파일 길이·hash 검증 후 개별 삭제를 확인했다.

전체 테스트·파일 파싱·전국 상시 수집·운영 DB/API/UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 명시적 실행 지시가 없어 사용자 정책상 생략했다. 커밋·푸시는 `[skip deploy]` 범위이며 서대문 전송 오류를 별도 후속으로 남기고 다음 미등록 지역을 진행한다.
