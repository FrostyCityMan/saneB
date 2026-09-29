# 용산·금산·창원 첨부 응답 복구와 QA 패키지 보완

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드를 우선하며 개별 오류는 별도로 보존한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 개선은 이번 범위가 아니다.

- [x] 용산 LGS-000004·금산 LGS-000156·창원 LGS-000224의 실제 응답 조사와 기관별 호환 연결.
- [x] 용산 HWP3개·금산 HWPX2개·창원 HWPX1개, 총550,028byte 실제 다운로드 검증.
- [x] 공통 validator·전송·다른 지역 지문 및 이전 실패 보존.
- [x] 관련 Java343통과/4조건부생략·bootJar·실제 수집, Node 수집23검사·278영수증/224공고 재현.
- [x] 이전 커밋 CI 실패 원인 확인, 실제 QA JAR 패키지 수정 및 Java47/Node24검사 통과.
- [x] 조사 원본12개2,642,205byte와 이번 실행 자원 정리.
- [ ] 잔여75개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인32.
- [ ] 새 커밋의 Linux CI 결과, 추출·상시 worker·운영 DB/API/UI·DRAFT·배치·운영 E2E 전체 Gate.

## 응답 조사와 변경 범위

기관별 공식 상세1회·공식 첫 파일1회씩 총6회 진단했다. 모두 HTTP200이었다. 전체 HTML·파일명·응답 헤더는 저장소에 넣지 않았다.

| 기관 | 서버 Content-Type | 진단 첫 파일 | 필요한 처리 |
|---|---|---|---|
| 용산 | application/x-msdownload;charset=UTF-8 | OLE113,152byte | 기존 구형 MIME 옵션·UTF-8 파일명1회 복원 |
| 금산 | hwpx | ZIP131,940byte | HWPX 서명·attachment 파일명 검증 후 내부 MIME 정규화 |
| 창원 | application/x-msdownload;charset=UTF-8 | ZIP99,491byte | 기존 구형 MIME 옵션·UTF-8 파일명2회 복원 |

`LegacyFileResponseAttachmentDiscoveryProfile`을 각 기관 설정에만 연결했다. 기존 `AttachmentFileTypeValidator`와 `AttachmentProfileDownloadFlow`는 수정하지 않았다. 용산·창원은 기존 legacy-binary 및 최대2회 UTF-8 복원 옵션을 사용한다. 금산은 서버 MIME이 정확히 `hwpx`인 응답만 HWPX 기본 서명·예상 형식·파일명 확장자·attachment disposition·경로·제어문자 검증 후 내부 legacy 표현으로 정규화한다. 금산에서 서버가 실제로 `application/x-msdownload`를 보냈다고 기록하지 않는다. 금산의 다른 미허용 MIME은 계속 오류다.

Adapter는 기존 다운로드 flow를 가진 프로필을 덮어쓰지 않으며, 전송은 기존 승인된 요청·호스트·예산 안에서 한 번만 수행한다. 상세 용량도 위임하여 금산2MiB, 용산·창원1MiB를 유지했다. 금산의 약1.99MB 상세 페이지를 기본1MiB로 잘못 축소하지 않는다. 공식 파일 영역·URL·공고 식별·미리보기 제외·부분 성공 정책은 기존 엔진에 위임한다.

서울시·서울 중구·부여·고성의 기존 지문을 회귀 테스트로 비교했고 다른 지역의 최신 표본·집계가 바뀌지 않았음을 이관 과정에서 검증했다. 기존 실패 보고서는 각각 `YONGSAN-766830-BEFORE-MIME.json`, `GEUMSAN-EA6A4E53C07C7F05A9E9240DBB006D43-BEFORE-MIME.json`, `CHANGWON-211423-BEFORE-MIME.json`으로 해시 일치 보존했다. 창원의 더 이른 INITIAL 실패도 유지한다.

## 실제 관측

| 기관·공고 | 관측 UTC / KST | 본문 | 첨부 결과 |
|---|---|---|---|
| 용산766830 | 2026-09-29T23:16:43.889016Z / 09-30 08:16 | AVAILABLE138자·ACCEPTED | HWP3개221,696byte |
| 금산EA6A4E53C07C7F05A9E9240DBB006D43 | 2026-09-29T23:16:58.451529600Z / 09-30 08:16 | AVAILABLE265자·ACCEPTED | HWPX2개228,841byte |
| 창원211423 | 2026-09-29T23:17:00.389069Z / 09-30 08:17 | AVAILABLE8,104자·REVIEW_REQUIRED | HWPX1개99,491byte |

세 관측은 모두 FOUND·complete=true·COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 파일6개 DOWNLOADED, 원본 정리true·운영 쓰기0이다. 창원 본문의 BODY_GROUP_A_MATCHED와 기존 주변 텍스트 혼입 의심은 이번 MIME 수정으로 해소됐다고 주장하지 않는다. 다운로드 성공과 본문 정제·정책 판정·추출 품질은 별개다. 단일 고정 과거 표본을 현재 유효 공고나 모든 공고의 성공으로 간주하지 않는다.

- 용산 프로필 지문: `4945232776a2d587abecd5386844103ac0d9eea391933eac85cc7e109c7e5619`
- 금산 프로필 지문: `a70d4e5ddd9c3c7ae0e368937dbd4519103bcf85fec8502f65ea759ba22efca2`
- 창원 프로필 지문: `204f11de6eb694fa04b837520bd88478465b1fb0f7055b78de80b47aab0cce50`

파일별 크기·SHA256은 영수증 색인에 보존한다. PDF/OLE/ZIP 기본 서명 검증을 문서 내부 구조·HWP/HWPX 텍스트 추출 성공으로 표현하지 않는다.

## 별도 CI 패키지 결함

직전 SHA `0dfd441d01c63ec8d5b9a1bf9a5f2c31cc610456`의 [Linux QA 실행](https://github.com/FrostyCityMan/saneB/actions/runs/36643420503)은3898검사 중1실패·309생략으로 종료됐다. 실패는 `AnnouncementAttachmentOfficialWorkerProbeTest.chungjuCaseDefinitionLoadsWithoutTheUnpackagedLiveQaClass`의 IllegalAccessError이며 필수 DB 검사 Gate도 실패했다. 해당 CI를 성공으로 취급하지 않는다.

원인은 `selectCases`가 지역별 package-private `*DownloadCases`를 참조하지만 별도 worker/BBS probe JAR가 해당 클래스들을 포함하지 않았던 것이다. 기존 classloader 테스트도 본체만 자식 loader로 읽고 지역 정의는 상위 loader에서 가져와 package 접근 경계가 달라졌다.

- 두 probe JAR에 `*DownloadCases*.class`만 추가했다. 운영 클래스·리소스·JUnit 의존성·live QA 테스트를 통째로 넣지 않았다.
- 일반 test가 두 JAR를 먼저 생성하고 실제 JAR에서 공고 정의를 읽도록 했다.
- 테스트는 패키지에 없는 QA 클래스를 상위 테스트 classpath에서 몰래 가져오지 못하게 차단한다.
- 두 JAR 각각에서 충주·태백·용산·금산·창원·안산·인제 정의를 읽고 공고 수를 검증한다. 충주 live QA 클래스가 패키지에 없다는 조건은 유지했다.
- launcher의 허용 그룹·실행 옵션·외부 요청 상한·CI 자동 관측·운영 변경 권한은 확대하지 않았다.

로컬 실제 JAR 검증을 포함한 Java47검사와 launcher/패키지 Node24검사는 통과했다. 새 Linux 실행의 성공은 별도로 확인해야 하며 이 결과를 Linux DB 통합 성공으로 대체하지 않는다.

## 실행 명령과 결과

```powershell
# SANEB_LEGACY_THREE_MIME_FIXTURE=true와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*LegacyThreeMimeCompatibilityTest' --tests '*AnsanInjeMimeCompatibilityTest' --tests '*SeoulSeventhDownloadContractTest' --tests '*GyeongnamSecondDownloadContractTest' --tests '*ChungcheongFifthDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCaseExecutorTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YONGSAN,GEUMSAN,CHANGWON' -PsanebCollectionWindowsTrust=true --no-daemon
# 외부 조회 없이 실제 생성한 두 probe JAR 검사
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AttachmentContractWorkflowTest' --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node --test scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-bbs-observation-probe.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 로컬 컴파일은 지문 함수 인자 개수 오류로 실패했고 기존2인자 계약에 맞춰 수정했다. 외부 관측은 그 실패 중 실행되지 않았다. 다음 집중 실행52건 중49통과·3조건부생략(43초), 확장 실행347건 중343통과·4조건부생략 및 bootJar·실제 관측 성공(2분32초)이다. 생략은 정리된 이전 조사 fixture 조건이며 이번 세 기관의 실파일 fixture는 실행했다. 이후 패키지 수정 검증47건은 생략 없이 통과(27초)했다. 원본 정리 후에는 fixture 환경변수를 켜지 않는다.

현재 집계는148/223수집원(66.4%) 최소1첨부 다운로드 확인, 잔여75(미등록43+등록 미확인32), 오류 기록40수집원이다. 오류는 성공 수집원과 겹칠 수 있다. 분모는2026-09-28 15:56:12 KST 읽기 전용 스냅샷이다. 지역180+기업마당1=181프로필·카탈로그236공고/179대상 유지,278영수증/224최신공고 재현 통과다. 기존3표본·전체 파일 Gate는16충족/207잔여이며 전체 목표 완료율이 아니다.

진단6요청은 각15초·연결7초, 상세는 금산3MiB/나머지2MiB, 파일은10MiB 한도였다. 실제 관측 기관별1회, 각각6요청·금산26MiB/나머지23MiB 이내다. 본문 포함 실제 예약15회/11,255,785byte, 조사 포함 예약 상한21회다. 예약과 실제 wire 요청 수는 구분한다. 조사 원본12개는 승인된 정확한 임시 경로·크기·SHA256 대조 후 개별 삭제했고 복구에는 공식 재조회가 필요하다. 보고서·해시는 유지했다. 이번 Node·Gradle·Java는 종료했고 기존 프로세스·무관한 미추적 파일은 보존했다.

운영 설치·DB/migration·API/UI·정책 게시·ENFORCE·기존 데이터·추출기·운영 배포는 변경하지 않았다. 전체 로컬 테스트와 현재 SHA의 Linux DB 통합은 이번 로컬 검증 범위가 아니다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 미실행이다. 작업 브랜치 `[skip deploy]` 범위를 유지하며 장기 goal은 진행 중이다.
