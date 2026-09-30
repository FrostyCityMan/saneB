# 송파 상세 제목 누락 분리와 첨부 수집 복구

## 현재 단계 / Gate

- [x] 공식 목록·상세 공고번호를 검증한 송파33174의 본문85자·HWPX3개 수집
- [x] 상세 제목 누락은 고정 경고와 발견 미완료로 보존
- [x] 공유 구현 변경에 따른 성동·광진 실제 파일 재검증
- [x] 최초 실패를 포함한6개 관측 추가, 기존658개 기록 보존
- [x] 회귀372통과/1생략, 최종 회귀186통과/1생략, 최종 실제 관측3통과, bootJar·Node42통과
- [~] 현재 프로필 기준 파일 확보202/223(90.6%), 잔여21개
- [!] 서대문 공식 파일 GET 시간 초과, 같은 요청 반복 중단
- [ ] 전체 지역 완료·추출·정책 QA·상시 운영·DB/API/UI 운영 E2E

기준 HEAD는 `9a2d28b1b54080edfee9760c93549a577d9a4766`이다. 전 지역 첨부 발견·다운로드 우선, 성공 파일 보존·오류 별도 분리, HWP 추출 고도화 보류를 유지한다. 운영 DB·정책·worker·ENFORCE·재분류·배포를 변경하지 않았다.

## 원인과 변경

송파 공식 목록에는 공고33174의 제목이 있지만 상세 제목 셀과 hidden title은 비어 있다. 새 공고33410에서도 같은 구조를 관측했다. 본문·파일 링크가 존재해도 이전 파서는 제목 확인에서 종료했다. 네트워크 오류와 다른 실패다.

`SongpaNoticeIdentity`는 승인된 HTTPS 호스트·경로·메뉴2776·숫자 공고번호, 단일 gosiFrm, 단일 hidden 공고번호와 요청 URL의 일치를 검증한다. 조건을 충족한 송파 상세만 빈 제목 상태에서도 본문·첨부 영역을 읽는다. 타 기관의 빈 제목 허용이나 임의 제목 주입은 하지 않는다.

수집 QA는 추가로 현재 공식 제목 검색 목록을1회/1MiB 이내에서 읽고, 같은 상세 공고번호와 정확한 제목이 한 번 등장하는지 검증한다. 외부 호스트·추가 query·중복 목록·잘못된 제목/ID·redirect를 거부한다. 검색어 URL은 시스템 코드에서 생성하며 일반 다운로드 프로필의 허용 범위를 넓히지 않는다. 실제 worker는 이미 선택된 source URL/식별 해시와 상세 hidden ID를 검증하고 제목 누락 자체는 미완료로 남긴다. QA의 현재 목록 재조회까지 worker가 수행한다고 표현하지 않는다.

유효 첨부 descriptor는 보존하되 `FAILED`, `complete=false`, `ATTACHMENT_DETAIL_TITLE_UNAVAILABLE`을 반환한다. 따라서 파일 다운로드 확인과 전체 집합 검증 완료는 다르다. 서비스의 고정 경고 허용 목록과 API 문서를 함께 확장했다. DB의 기존 warning JSON과 응답 wrapper를 유지하며 migration·DDL·기존 v1 필드 변경은 없다. 관리자 기존 오류 표시 구조에 “상세 제목 누락 · 공고번호로 첨부 수집, 제목 확인 필요”를 추가했다. UI 구조·동작·디자인 시스템 변경은 없다.

## 실제 관측

| 공고 | 본문 | 최종 첨부 결과 | 집합 상태 |
|---|---:|---|---|
| 송파33174 | 85자 | HWPX3개224,066바이트 | 제목 누락 경고, 발견 미완료 유지 |
| 성동356569 | 104자 | HWP2개65,024바이트 | 수집 관측 완료, 정책 승인 아님 |
| 광진6415244 | 1,930자 | PDF2개·HWPX1개598,852바이트 | 수집 관측 완료, 정책 승인 아님 |

송파 파일 SHA-256:

- 55,380바이트: `363cc52665cbd601bf8daebeede0ea513f0f70dd7eb6c4456c0c73e83ec8a045`
- 99,243바이트: `96f3f5034d0a8d9fd4c0ff016c028ecd3fd3228002f9388545e27fbd7e8ff563`
- 69,443바이트: `9efee047932c2b81dd3731e13c842f733367ca87b17242b3f18c6dcaedeae7f8`

실제 파일 signature·형식 검증 후 원본을 정리했다. 광진은2025년 보관 공고의 기술 검증이며 현재 모집 여부 확인이 아니다. 추출기는 실행하지 않았다.

서대문313956은 본문·첨부4개 링크를 확인했으나 파일 응답이 시간 초과됐다. 최초 .NET 진단의 UTF-8 고정 디코딩은 EUC-KR 원문에 적합하지 않아 전송 비교 근거로 사용하지 않았다. EUC-KR로 파일명을 다시 확인하고 올바른 파일명으로 GET1회를 실행했지만12,036ms에 시간 초과됐다. 이를 파싱 실패·첨부 없음으로 바꾸지 않으며 TLS 우회·UA 위장·동일 요청 반복은 하지 않았다.

## 검증과 실패 기록

1차 로컬 회귀176건 중175통과/조건부1생략, bootJar 성공. 확장 회귀373건 중372통과/조건부1생략. 첫 실제 수집3건은 성동·광진2통과/송파1실패로 Gradle 실패였다. PowerShell finally 이후 셸 종료코드만으로 성공 판단하지 않고 Gradle 출력·JUnit·관측 보고서로 실패를 확인했다.

실패 원인은 공식 검색 결과 href가 검색어 공백을 그대로 포함한 데 있었다. URI로 해석할 때 공백을 인코딩하는 처리와 실제 형태 회귀 테스트를 추가했다. 이전 실패 보고서는 삭제하거나 덮어쓰지 않았다.

최종 회귀187건 중186통과/조건부1생략, 실패0이다. 실제 관측3건과 bootJar를 포함한 명령은1분6초·종료0이다. 송파는 파일3개 성공·집합 미완료이며 관측 태스크 통과를 전체 처리 성공으로 해석하지 않는다. Node42검사와664기록/287최신 표본 재현이 통과했다. `git diff --check`는 통과했다. `core.autocrlf=false`를 강제로 지정한 검사는 기존 CRLF 구간을 whitespace로 보고했으므로 원래 저장소 설정으로 재확인했으며 문서 전체 줄바꿈을 변경하지 않았다.

검증 명령의 핵심 범위:

```powershell
.\gradlew.bat --no-daemon :test --tests '*SongpaNoticeIdentityContractTest' --tests '*SeoulFourthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentDiscoveryEvidenceTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SONGPA,SEONGDONG,GWANGJIN' -PsanebCollectionReportLabel=SONGPA-IDENTITY-FIX-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

인벤토리 테스트는 기존 로컬 영수증 `build/qa-results/target-inventory-20260928-receipt.txt`와 `SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT=true`를 사용했으며 환경변수는 finally에서 복원했다. 별도 확장 회귀에는 catalog106·worker40·processing flow37·pinned download10건도 포함했다. 신규 DB 통합 테스트는 추가·컴파일했으나 실제 PostgreSQL/Docker 실행은 하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. 전체 테스트·실서버 DB/API·운영 배포·운영 E2E 성공을 주장하지 않는다.

## 근거·예산·잔여

공유 상세/프로필 구현 변경으로 성동·광진·송파3개 프로필 지문만 변경됐다. 인벤토리246개 항목 중 나머지243개는 동일하며 카탈로그294참조/223대상·기존 기대값1개도 유지한다. 운영 대상 스냅샷은2026-09-28 15:56:12 KST로 새 운영 조회가 아니다.

- 인벤토리 SHA: `ca5a704917e3d434f97ea44e3e55a40a7d4686500bce11d69bf3b0fb30e03743`
- 관측 클래스 SHA: `9a4d2f0639349d1d81fda75aca4a8d5dd6fe2b1134b2bd966d6927187714f2f0`
- 기존658기록 보존, 신규6기록 추가. 최신287표본 중3개만 갱신, 나머지284개 동일.
- Java 실제 관측2회 합계 최대38요청/138MiB, 본문 포함 예약33회/16,936,394바이트. 관측13파일 다운로드1,551,818바이트는 성동·광진 재검증 중복을 포함하며 지역 수를 부풀리지 않는다.
- 별도 공식 HTTP 진단13회, 각12초/2MiB·연결5초·redirect0·쿠키 저장0·TLS 검증 유지. 디스크 원문 저장0. Java 원본 정리true. 조사 포함 요청 상한51회.

현재 지문 기준201→202/223(90.6%), 잔여22→21이다. 과거 포함 성공205/223, 최초 미확인18개다. 오류 관측 지역은32→33이며 성공 지역과 중복된다. 엄격한3공고·전체 첨부 집합 Gate16/223은 변하지 않는다.

잔여21개는 최초 미확인18개(은평·서대문·검단·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·천안·공주·의성·성주)와 과거 성공 후 최신 미확보3개(아산·봉화·남해)다. AWS 인증 갱신과 기존 임시 자원 정리 차단은 별도 보류다. 다음 회차는 다른 미확보 지역의 공식 수집 경로·환경 차이를 점검하며 동일한 실패 요청을 반복하지 않는다.

이번 검증한 변경만 한국어 커밋과 `[skip deploy]`로 작업 브랜치에 푸시한다. 원본/운영 secret·사용자 output·미추적 캐시를 커밋하지 않는다. 전체 goal은 계속 진행 중이다.
