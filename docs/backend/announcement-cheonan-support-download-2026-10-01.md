# 천안 제목 적합 공고의 첫 첨부 수집 확인

## 현재 단계 / Gate

- [x] 기존 천안107953의 제목 조합 미충족·요청0 기록 보존
- [x] 공식 공고113864 추가 참조 및 기존 제목 규칙 검증
- [x] 본문88자·HWPX1개69,648바이트 실제 수집·형식 검증
- [x] Java146통과·실제 관측1통과·bootJar·Node23통과
- [~] 현재 프로필 기준 파일 확보203/223(91.0%), 잔여20개
- [ ] 목록 상시 유입·전체 지역 완료·추출·정책 QA·운영 DB/API/UI·운영 E2E

기준 HEAD `6311ccf6b53d9d4447738324e6c73ae2a70e21ac`. 직전 송파 첨부 확보는 진전으로 분류했다. 이번에도 전 지역 첨부 발견·다운로드 우선, 파일별 오류 분리, HWP 추출기 고도화 보류를 유지한다. 실제 운영 조회·쓰기·배포는 없다.

## 공식 근거와 구현 범위

천안의 기존 표본은 전송 실패가 아니라 제목 조합 미충족이었다. 이를 성공률을 높이기 위해 우회하지 않고 새 공식 표본을 찾았다. 검색 결과에서 찾은 [공식 공고113864](https://eminwon.cheonan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=113864&subCheck=Y)는 `(재공고)청소년 인성교육 및 고3 수능이후 생활지도 사업 지방보조금 지원계획`이다. 검색 결과만 사용하지 않고 기존 프로필이 허용한7개 query의 공식 상세를 직접 확인했다. 응답200·5,914바이트, 공식 goDownLoad의 파일3인자와 HWPX 링크1개를 확인했다. 다른 기관 이미지·목록 버튼은 요청하지 않았다.

`RecoveredSupportDownloadCases`와 계약 테스트에 `CHEONAN_SUPPORT`를 추가했다. source148·SPRING_BBS·기존 profile·목록 주소·URL 정규화·제목 규칙은 그대로다. `청소년`과 지원 문구가 기존 DRAFT seed에서 `COMBINATION_MATCHED`로 판정됨을 검증했다. 이 공고가 실제 서비스 사용자의 최종 진행 대상이라는 의미는 아니다.

카탈로그는294→295참조/223대상이다. 새 참조의 expectation은 null이며 참조 전용294개와 기존 기대값1개를 유지한다. 기존107953·그 제목 중단 기록은 변경하지 않았다. 응용 Java·수집 엔진·API·DB·화면·Flyway V85·프로필 지문·키워드·운영 설정은 변경하지 않았다. 상시 목록이 새 공고를 자동 유입한다는 증거로 사용하지 않는다.

## 실제 수집 결과

관측 보고서는 `build/reports/attachment-regional-collection/CHEONAN-113864-CHEONAN-SUPPORT-20261001.json`이다.

| 항목 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE, 88자, ACCEPTED |
| 상세 식별 | 확인 |
| 발견 | FOUND, complete=true, HWPX1개 |
| 파일 | DOWNLOADED, 69,648바이트 |
| 파일 SHA-256 | `35ceb853aea803f60154029b6a78286aa87d665b7819f4b6a797eee2108ee021` |
| 관측 상태 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| 원본 정리 / 운영쓰기 | true / 0 |

파일 signature·기대 형식 검증은 통과했으나 HWPX 내부 텍스트 추출·구간 분석·정책 기대값 승인은 실행하지 않았다. BODY ACCEPTED는 로컬 규칙 결과이며 운영 공고 활성화나 관리자 확정이 아니다.

## 검증

```powershell
# 기존 읽기 전용 영수증과 현재 로컬 코드 비교용 환경변수만 한정 설정
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='build/qa-results/target-inventory-20260928-receipt.txt'
.\gradlew.bat --no-daemon :test --tests '*RecoveredSupportDownloadContractTest' --tests '*CheonanDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHEONAN_SUPPORT' -PsanebCollectionReportLabel=CHEONAN-SUPPORT-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 실행에서는 두 환경변수의 이전 값을 보관하고 finally에서 복원했으며 Gradle 종료코드를 그대로 반환했다. Java는 catalog106·천안5·추가표본18·정책 snapshot14·inventory3, 합계146개 통과·실패/생략0이다. 실제 관측1개와 bootJar까지3분33초·종료0이다. Node23개와665기록/288최신 표본 재현을 검증했다. 전체 Java/DB 통합·AWS·운영 E2E는 미실행이며 브라우저는 현재 명시 지시가 없어 정책상 미실행했다.

## 보존·집계·예산

기존664기록과287최신 표본을 그대로 보존하고1기록/1표본을 추가했다. 모든 프로필 지문은 그대로이며 천안 catalogReferenceCount만1→2다. 대상 스냅샷은2026-09-28 15:56:12 KST이며 운영의 현재 전체 수집원 재조회가 아니다.

- 인벤토리 SHA: `f28945ff0a1164dde2940d5268853d2d553edb339687ab8d46474c5d5aa6e3a5`
- 천안 현재 프로필 SHA: `6c4e76f4fc5a3134e82ecc083893fcf78dc845959fd9ee02a3967c82514d9179`
- 관측 클래스 SHA: `9a4d2f0639349d1d81fda75aca4a8d5dd6fe2b1134b2bd966d6927187714f2f0`
- 현재 파일 확보202→203/223, 잔여21→20. 과거 포함206/223, 최초 미확인17개.
- 오류 수집원33, 엄격3공고·전체 집합 Gate16/223은 유지한다. 오류 수집원은 성공 지역과 겹친다.

공식 도메인 검색11질의·열기2회는 직접 요청 예산과 구분한다. 열기 중 공고103516 조회 실패는 선택한113864의 성공으로 덮지 않는다. 직접 구조 진단은3GET, 각12초/2MiB·연결5초·redirect0·쿠키 저장0·TLS 검증 유지이며 원문 디스크 저장0이다. 실제 Java 관측은 최대6요청/23MiB, 본문 포함 예약4회/2,172,714바이트다. 진단 포함 요청 상한9회이며 검색 도구의 서버 내부 요청 수는 알 수 없다. Java 임시 원본은 정리했다.

잔여20개: 은평·서대문·검단·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·공주·의성·성주, 과거 성공 후 최신 미확보 아산·봉화·남해. 전체 goal은 미완료이며 동일한 접근 실패 재시도 대신 다음 공식 경로·표본·환경 차이를 확인한다. 운영 정책·worker·ENFORCE·기존 데이터·배포 변경은 없고 작업 브랜치의 `[skip deploy]` 커밋·푸시만 진행한다. 사용자 output·미추적 캐시와 과거 정리 차단 자원은 건드리지 않는다.
