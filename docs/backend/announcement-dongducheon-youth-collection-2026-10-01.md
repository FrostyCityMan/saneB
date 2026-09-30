# 동두천 청년 지원 공고 수집과 비파일 응답 분리

## 현재 단계 / Gate

- [x] 공식 목록·상세에서 확인한45339를 기존 프로필의 별도 참조 표본으로 추가
- [x] 새 표본 제목 조건 통과·기존44784 제목 중단 보존 테스트
- [x] 본문29자·공식 첨부HWPX 링크1개 발견
- [!] 파일 응답2,596byte는 HTML, `ATTACHMENT_SIGNATURE_UNSUPPORTED`로 다운로드 성공 제외
- [x] 관련 Java154통과/조건부1생략·bootJar·Node23통과·406영수증/280공고 재현
- [~] 과거 다운로드 경험201/223수집원(90.1%), 미확인22개 유지
- [~] 최신 코드 실파일 관측27/223, 신규 확인·재검증196개
- [ ] 상시 목록 유입·전체 첨부 집합·추출·DB/API/UI·운영 E2E

분모223은2026-09-28T15:56:12.116778+09:00 활성 지역 수집원 스냅샷이다. 고유 지자체 수·현재 운영 조회·전체 개발 완료율이 아니다. HWP 추출 고도화1.0.16 보류와 정상 파일 보존·개별 오류 분리 원칙을 유지한다.

## 구현 범위와 검증된 계약

기준 HEAD `8aca250f14ea0e0e9cf68402c9efe44af2e99015`. 동두천 기존 수집기·본문 처리기·프로필·공통 전송 코드·키워드 규칙은 변경하지 않았다. 이전 공식 목록 조사에서 확인한 `2026년 동두천시 청년구직비용 패키지 지원사업 공고`45339와HWPX 링크1개를 선택했다.

`CapitalFourthDownloadCases.selectDongducheonYouthCase()`와 전용 관측 그룹 `DONGDUCHEON_YOUTH`를 추가했다. 기존 `DONGDUCHEON` 그룹은44784 한 건과 제목 조합 미충족 조건을 유지한다. 신규 테스트는 두 제목 판정, source identity, HTTPS 상세 경로, 참조 전용 catalog, 요청6회/23MiB 상한을 검증한다. 파일이 성공하도록 제목이나 키워드를 바꾸지 않았다.

카탈로그는 기존286공고·승인 기대값1개를 그대로 두고 기대값null인 신규 참조1개를 추가하여287공고/222대상이다. reference-only286개, 기존 기대값1개다. 이에 따른 catalog·policy snapshot 테스트의 정확한 건수만 갱신했다. 운영 정책 게시·기대값 승인·외부 공고 활성화는 없다.

## 실제 관측

2026-10-01 00:15:00 KST, `DONGDUCHEON-45339-DONGDUCHEON-YOUTH-20261001.json`:

| 단계 | 결과 |
| --- | --- |
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE29자, 분류는 REVIEW_REQUIRED/BODY_UNAVAILABLE |
| 공식 첨부 | FOUND,1개, 공고 식별·전체 발견 완료 |
| 파일 | 전송2,596byte, FILE_SIGNATURE/ATTACHMENT_SIGNATURE_UNSUPPORTED |
| 수집 결과 | COLLECTION_ONLY_PARTIAL_NOT_APPROVED, 다운로드0 |
| 원본·운영 | 원본 정리true, 운영쓰기0, 추출·정책·기대값 승인false |

본문을 가져왔다는 사실과 충분한 분류 근거가 있다는 판단은 별개다. 실패 응답의 SHA-256 `548267d87c240b526e9394f004d52f6c96fd242e75a5af29f4360bef4044b490`는 오류 근거이며 유효 첨부 파일 해시가 아니다.

실수집 상한6요청·23MiB 중 본문 포함 요청예약4회·바이트예약2,673,188이다. 예약값은 실제 wire 요청 수·전송량과 다르다. Gradle 종료0은 실패 기록을 정상 생성했다는 뜻이지 파일 다운로드 성공이 아니다.

추가 공개 진단은 공식 상세1회와 그 응답에 있는 정확한 다운로드 링크1회, 각12초/2MiB 상한이었다. 둘 다HTTP200이고 파일 URL은2,596byte HTML을 반환했다. 기간 관련 문구의 존재만으로 실제 만료·권한 부족을 확정하지 않는다. 구체적인 서버 원인은 미확정이다. 원문은 저장하지 않았고 TLS검증·collector UA·쿠키 미사용·자동 redirect 금지를 유지했다. 미리보기나 대체 내부 파일 경로로 우회하지 않았다.

## 실행 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :test --tests '*DongducheonYouthCollectionContractTest' --tests '*CapitalFourthDownloadContractTest' :bootJar
.\gradlew.bat --no-daemon :test --tests '*DongducheonYouthCollectionContractTest' --tests '*CapitalFourthDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentCollectionOnlySummaryTest' --tests '*RegionalCollectionReportArchiveTest' --tests '*AnnouncementAttachmentObservationTransferContractTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DONGDUCHEON_YOUTH' -PsanebCollectionReportLabel=DONGDUCHEON-YOUTH-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

집중11개 중10통과/이전 원본 fixture1조건부 생략. 확대155개 중154통과/동일1생략, 실패0·bootJar 통과. inventory 비교는 이전 읽기 전용 영수증으로만 수행했으며 환경변수는 복원했다. 전체 Java suite를 실행한 것은 아니다.

Node23개 통과. 영수증 추가 시 배열 삽입 위치 오류를 재현 검증이 탐지하여, 신규 공고를 정렬된 위치로 옮긴 뒤406영수증/280최신공고·27개 현재 다운로드·오류 포함8수집원·엄격3표본 Gate0을 재현했다. 기존405영수증과279공고 내용은 유지한다. 실패·오류 수집원 수와 성공 수는 중복 가능하므로 더하지 않는다.

- 현재 inventory SHA-256: `7f6d6881f8b2f70781a955d5ab9e31174735bd1455400c43b6742bf7e89b419c`
- 신규 관측 producer SHA-256: `1df3a2ac93ee2d0e73aa1894eb42df316589e7a3209c1d9fa4e920415ae2a7cc`
- 동두천 프로필 SHA-256: `adedefd94a81f19069c7c47c018ec671077887847643601a26a05561aacf1207`

관측 클래스에 그룹을 추가했으므로 신규 producer 지문이 바뀌었다. 과거 영수증의 producer hash를 새 값으로 덮지 않는다. 응용 Java·프로필 구현은 불변이다.

## 남은 업무와 범위

다운로드 미확인22개는 이전 목록 그대로이며 동두천도 포함된다. 새 표본은 제목 단계 문제를 넘어 파일 응답 오류까지 원인을 좁힌 증거이지 성공 지역 추가가 아니다. 해당 파일은 서버의 정상 응답 또는 다른 공식 적격 표본을 확인한 뒤 재개한다. 다른 지역의 가능한 파일 수집은 계속한다.

천안 공식 새올 도메인의 소상공인·청년 지원 공고 검색2질의는 결과가 없어 새 표본을 확보하지 못했다. 검색 실패를 공식 사이트 장애로 해석하지 않는다.

운영 DB·설정·worker·정책·ENFORCE·재분류·배포·Flyway V85·HWP 추출기에는 변경이 없다. AWS·운영 조회는 미실행이며 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 기존 미추적 output 및 Python 캐시는 보존하고, 기존 연제·구례 임시 폴더 정리 차단은 별도 미해결로 유지한다. 작업 브랜치 `[skip deploy]` 커밋·푸시 범위이며 전체 goal은 미완료다.
