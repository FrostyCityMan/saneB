# 동두천 PDF 비파일 응답 검증과 장기 goal 재개

## 현재 단계 / Gate

- [x] 장기 goal active 상태·루트 AGENTS·Git 기준선 재확인
- [x] 공식 목록의 새 적격 공고44176을 별도 참조로 추가
- [x] 제목 조합·본문71자·공식 PDF 링크1개 검증
- [!] PDF 대신 HTML2,596바이트 응답, 파일 수집 실패로 분리
- [x] Java146개·관측1개·Node23개 통과, bootJar 성공
- [~] 현재 프로필 기준 파일 확인203/223, 미확인20개 유지
- [!] 서울 서버 조회는 AWS TLS 인증서 검증 오류로 미확인
- [ ] 상시 유입·전체 지역 완료·추출·운영 DB/API/UI·운영 E2E

기준 HEAD는 `335c5ff007cc474d683241c7f44e5242a84c516e`다. 사용자의 재개 요청에 따라 `long-goal-operating-protocol`을 적용했다. 성공 파일 보존·개별 오류 분리·HWP 추출 고도화 보류를 유지한다. 분모223은2026-09-28 15:56:12 KST 활성 수집원 스냅샷이며 현재 운영 조회나 전체 goal 완료율이 아니다.

## 조사와 변경 범위

동두천 공식 소상공인 검색 목록에서44176 `2026년 소상공인 도로점용료 감면 신청 안내 공고`를 확인했다. 상세의 공식 첨부 영역에는 PDF1개가 있었고 기존 프로필이 허용하는 `eminwon.ddc.go.kr/emwp/jsp/ofr/FileDownNewPbs.jsp` 링크였다. 다른 링크·미리보기·내부 경로로 우회하지 않았다.

새 `DONGDUCHEON_SUPPORT` 표본과 참조 카탈로그만 추가했다. 기존44784 제목 중단과45339의 HWPX 실패 기록을 보존했다. 기존 제목 규칙에서 `소상공인`과 `감면` 조합이 통과하는지 계약 테스트로 검증했다. 카탈로그295→296개, 참조 전용295개·기존 기대값1개다. 새 expectation은 null이다.

응용 Java·수집 프로필·분류 규칙·DB/API·화면·Flyway V85·운영 설정을 변경하지 않았다. 동두천 참조 수만2→3이며 전체246개 inventory 대상의 프로필 지문은 불변이다.

## 실제 관측

2026-10-01 05:11 KST 보고서: `build/reports/attachment-regional-collection/DONGDUCHEON-44176-DONGDUCHEON-SUPPORT-20261001.json`.

| 단계 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE71자, ACCEPTED/TARGET_SUPPORT_CONFIRMED |
| 상세·첨부 발견 | 식별 확인, FOUND, complete=true, PDF1개 |
| 다운로드 | FAILED, FILE_SIGNATURE/ATTACHMENT_SIGNATURE_UNSUPPORTED |
| 응답 | HTTP200, HTML2,596바이트; 정상 PDF 아님 |
| 종합 | COLLECTION_ONLY_PARTIAL_NOT_APPROVED |
| 원본 정리·운영쓰기 | true·0 |

실패 응답 해시 `ef64e65be14c93b36e395b543b1b9a904447723f4b29ccf2751a5aea552c1d12`는 유효 파일 해시가 아니다. .NET 직접 GET과 실제 Java 수집 경로의 응답 해시가 같았다. 기존45339의 파일 실패가 특정 HWPX에만 한정되지 않는 추가 근거지만, 전체 공고가 실패하거나 서버 권한·만료가 원인이라고 확정하지 않는다. 화면의 일반적인 게재기간 안내만으로 원인을 단정하지 않는다.

본문 ACCEPTED는 로컬 규칙 판정이며 최종 진행 공고 확정이나 자동 활성화가 아니다. 실패 파일을 정상 후보 근거로 승격하지 않는다. 동일 파일의 무제한 재시도 대신 공식 정상 응답·새 적격 표본·다른 승인 환경이라는 새 근거가 있을 때 재개한다.

## 명령·검증·보존

```powershell
# inventory는 기존 읽기 전용 영수증과 현재 코드만 비교한다.
# 실제 실행은 아래 두 환경변수의 이전 값을 finally에서 복원했다.
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='build/qa-results/target-inventory-20260928-receipt.txt'
.\gradlew.bat --no-daemon :test --tests '*RecoveredSupportDownloadContractTest' --tests '*DongducheonYouthCollectionContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DONGDUCHEON_SUPPORT' -PsanebCollectionReportLabel=DONGDUCHEON-SUPPORT-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Java146개(106+21+2+14+3) 통과·실패/생략0, 관측1개 통과·bootJar 성공, 실행3분35초·종료0이다. 관측 통과는 실패 분리 및 원본 정리가 검증됐다는 의미이며 다운로드 성공이 아니다. Node23개 통과와666기록/289최신 표본 재현을 확인했다. 기존665영수증과288최신 표본 내용은 그대로 보존했다.

- 현재 inventory SHA: `31d6fabfa89aca60fc7d141bf32740b9453135b038d2bc7b70ab0f9e4a4749b0`
- 동두천 프로필 SHA: `adedefd94a81f19069c7c47c018ec671077887847643601a26a05561aacf1207`
- 관측 클래스 SHA: `9a4d2f0639349d1d81fda75aca4a8d5dd6fe2b1134b2bd966d6927187714f2f0`

직접 진단3GET은 각각12초/2MiB, 연결5초, redirect0·쿠키0·TLS검증 유지, 원문 디스크 저장0이다. Java 관측 상한6요청/23MiB 중 본문 포함 예약4회/2,673,188바이트다. 예약값은 실제 wire 요청·전송량이 아니다. 직접 진단 포함 상한9요청이며 AWS 조회1회는 이 공개 수집 예산과 별개다.

현재 파일 확인203/223(91.0%), 과거 포함206/223, 최초 미확인17개, 최신 미확보20개, 오류 포함33개, 엄격3표본 첨부 집합 Gate16/223을 유지한다. 오류 수집원은 성공 수집원과 중복된다.

잔여20개: 은평·서대문·검단·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·공주·의성·성주, 과거 성공 후 최신 미확보 아산·봉화·남해.

## 외부 차단·미실행 범위

서울 리전 STS 읽기 전용 조회1회는 기존 공개 CA bundle을 해당 명령에만 지정했지만 TLS 인증서 검증에서 실패했다. 현재 계정 권한·로그인 유효성·서울 서버 상태는 확인하지 못했다. TLS 검증 해제·전역 신뢰 변경은 하지 않았고 로그인 갱신·인증서 경로 점검 진행 여부를 사용자에게 요청했다. 응답 대기 중 운영 조작은 하지 않는다.

전체 Java suite·DB 통합·운영 QA·배포는 미실행이며 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 사용자 output·Python 캐시와 이전 정리 차단 자원은 건드리지 않는다. 작업 브랜치 `[skip deploy]` 커밋·푸시 범위만 유지하며 장기 goal은 미완료다.
