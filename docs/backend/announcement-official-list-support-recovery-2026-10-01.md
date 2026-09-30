# 공식 목록의 추가 지원 공고로 강남·거창 첨부 재확보

## 현재 단계 / Gate

- [x] 공식 목록의 검색 필드·상세 제목·첨부 영역 확인
- [x] 기존 실패 표본을 보존하고 강남·금산·거창 참조 3건 추가
- [x] 강남 HWPX 2개, 거창 HWPX 1개 다운로드·형식 검증
- [!] 금산 HWP 2개는 전송 후 Content-Type 불일치로 별도 오류 유지
- [x] 회귀 186개 중 183통과·3조건부 생략, 실제 관측 3통과, bootJar 성공
- [x] Node 23통과·655영수증/285최신 표본 재현
- [~] 현재 프로필 기준 첨부 확보 199/223(89.2%), 미확보 24개 수집원
- [ ] 전체 첨부 추출·상시 worker·DB/API/UI·운영 E2E와 전체 goal 완료

기준 HEAD `d5649e904b8e90d8dd1488707656b453347f4e15`. 전 지역 첨부 확보 우선, 파일별 오류 분리, HWP 추출기 1.0.16 이후 고도화 보류를 유지한다. 이번 변경은 QA 표본·참조 카탈로그·검증·증거 대장이다. 응용 Java·수집 프로필·migration·운영 설정은 변경하지 않았다.

## 공식 목록과 새 표본

기존 강남64668·금산EA6A4E53C07C7F05A9E9240DBB006D43·거창47689·아산80727의 실패를 삭제하거나 다른 공고의 성공으로 덮어쓰지 않았다. 현재 공개된 다른 지원 공고를 추가 표본으로 고정했다. 기존 공고가 삭제됐는지, 접근 실패가 영구적인지는 확정하지 않았다.

| 수집원 | 공식 목록에서 확인한 검색 필드 | 추가 공고 / 표시 첨부 |
|---|---|---|
| 강남 LGS-000024 | keyfield=BNI_MAIN_TITLE, keyword=청년 | 61922 / HWPX 2개 |
| 금산 LGS-000156 | skey=title, sval=소상공인 | 4df078b6fceb5d17b3162ce865ecbf3b / HWP 2개 |
| 거창 LGS-000240 | stype=title, sstring=청년 | 45659 / HWPX 1개 |

강남은 미취업 청년 어학·자격증 응시료 지원, 금산은 소상공인 사회보험료 2026년 1분기분 지원사업 변경 공고, 거창은 청년 구직자 자격증 취득 응시료 지원사업이다. 아산은 sltOption=1, txtKeyword 검색 형식을 확인했으나 이번 조사에서는 사용할 새 지원 공고를 확보하지 못했다. 메뉴의 ‘소상공인 화재보험료 지원’ 링크를 공고 상세로 오인하지 않았다.

초기 keyword만 전달한 검색은 제목 필터 적용을 입증하지 못해 후속으로 공식 select 값을 확인했다. 이 요청도 요청 수에서 제외하지 않았다. 브라우저·스크립트 실행 없이 공식 HTML을 메모리에서만 조회했다. 검색 필드 조사는 운영 목록 파서 수정이나 상시 수집 검증을 뜻하지 않는다.

## 실제 수집 결과와 금산 오류

| 공고 | 본문 | 발견 | 파일 결과 | 최종 수집 상태 |
|---|---|---|---|---|
| GANGNAM-61922 | AVAILABLE | FOUND, complete=true | HWPX 81,546 + 120,612바이트 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| GEOCHANG-45659 | AVAILABLE | FOUND, complete=true | HWPX 76,066바이트 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| GEUMSAN-4DF078B6FCEB5D17B3162CE865ECBF3B | AVAILABLE, 411자 | FOUND, complete=true | 190,976 + 74,752바이트 전송, 모두 FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH | COLLECTION_ONLY_PARTIAL_NOT_APPROVED |

성공 파일은 3개·278,224바이트다. 금산의 전송 바이트·해시를 정상 다운로드 근거로 승격하지 않았다. 별도 Windows HTTP 진단에서 첫 금산 파일은 HTTP200, 원시 Content-Type **`hwp`**, OLE signature `D0CF11E0A1B11AE1`, SHA-256 `b80c7a09c256e4a8781956c7a8f26b40c5afba71e445c18b6954663ecf810b8f`였다. Java 실패 보고서와 같은 해시다. .NET의 파싱된 ContentType은 빈 값이었으므로 이를 헤더 부재라고 단정하지 않고 원시 Content-Type 필드만 추가 확인했다.

현재 금산 프로필의 호환 모드는 `BARE_HWPX`이며 bare `hwpx`만 보정한다. bare `hwp`가 일반 MIME 검증에 남아 거절된 것이 첫 파일의 확인된 원인이다. 두 번째 파일은 동일 오류 코드이나 원시 헤더까지 별도 확인하지 않았다. OLE signature는 내부 HWP 구조·텍스트 추출 성공 증거가 아니다. 이번 회차에 공통 MIME 검증을 완화하지 않았다. 다음 구현은 금산에 한정한 형식·파일명·signature 결합 검증과 프로필 지문 갱신 후 재관측이다.

## 구현 및 회귀 검증

`RecoveredSupportDownloadCases`에서 기존 source/parser/프로필/목록 주소를 재사용하고 새 상세 식별자·제목·표시 파일 수만 고정했다. 그룹은 GANGNAM_SUPPORT, GEUMSAN_SUPPORT, GEOCHANG_SUPPORT다. 기존 그룹은 기존 실패 표본을 계속 반환한다. 9개 계약 검증으로 독립 식별자·동일 프로필·기존 제목 정책·요청 한도·카탈로그 미승인을 확인한다.

카탈로그 289→292공고이며 대상은 223개 유지다. 참조 전용 291개, 기존 기대값 1개이며 새 실행 기대값 승인은 없다. 기존 회귀의 전체·지역별 참조 개수와 정책 snapshot 개수만 변경했다. executableCount=0, REFERENCE_ONLY, 기대값 미승인 검증은 보존했다.

초기 실행은 170개 중 4실패·1조건부 생략이었다. 모두 추가 참조 수를 반영하지 않은 기존 개수 검증 실패였다. 수정 후 정책 snapshot 회귀를 추가하여 최종 186개 중 183통과·3조건부 생략, 실패0이다. 생략은 로컬 조사 원본을 요구하는 ChungcheongFifth actualOfficialBoundaries, SeoulEighth actualDobongUtf8DispositionPreservesFormat 및 actualPageContract다. 신규 공식 표본 관측 3개는 별도로 실행·통과했다. 명령 전체는 3분43초, 종료0이다. bootJar는 첫 실행에서 생성됐고 최종 실행에서 UP-TO-DATE 확인됐다.

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='build/qa-results/target-inventory-20260928-receipt.txt'
.\gradlew.bat --no-daemon :test --tests '*RecoveredSupportDownloadContractTest' --tests '*SeoulEighthDownloadContractTest' --tests '*ChungcheongFifthDownloadContractTest' --tests '*GeochangDownloadContractTest' --tests '*RegionalObservationSourceContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GANGNAM_SUPPORT,GEUMSAN_SUPPORT,GEOCHANG_SUPPORT' -PsanebCollectionReportLabel=RECOVERED-SUPPORT-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

실제 명령에서는 환경변수를 finally로 해제했다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 전체 Java 테스트·운영 테스트 통과로 확대 해석하지 않는다.

## 증거·예산·남은 범위

기존 652영수증·282최신 표본을 모두 보존하고 3영수증·3표본만 추가했다. 대장의 `officialListSupportRecoveryRun`에 결과와 예산을 기록했다. 이전 관측의 producerClassHash는 당시 값을 유지한다. 최신 QA 실행 클래스 지문은 `caeef0d8ae1727e6b4ceb96c6ab68fa1440d7dfd7549c87b297b906e60e2b148`다.

기존 읽기 전용 운영 수집원 영수증을 현재 코드와 다시 대조했다. 새 운영 조회는 아니다. 2026-09-28 15:56:12 KST 스냅샷 시각, 활성223개와 모든 프로필 지문은 유지하고 세 지역의 catalogReferenceCount만 1→2로 증가했다. 인벤토리 SHA-256은 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`에서 `508f7f12028326fd0cd6b2b32c00ba84390c74fa02cefccdf0465bc26b2cd62e`로 바뀌었다.

별도 공개 진단은 GET26회, 요청당2MiB·연결5초·요청12초, TLS 검증·redirect0·cookie 저장0이다. 원문은 메모리에서만 처리하여 디스크 기록0이다. 실제 Java 관측은 최대20요청·92MiB, 예약14회·11,422,928바이트였고 임시 원본 정리true다. 두 경로 합산 요청/예약 상한은46회다. 진단 실패나 파일 형식 실패를 정상 수집으로 세지 않는다.

현재 첨부 확보199/223, 미확보24개(최초 미확인19 + 과거 성공 후 최신 실패5: 미추홀·아산·금산·봉화·남해)다. 과거 포함204/223, 오류 수집원33, 엄격한 3공고 전체 첨부 집합 Gate16/223은 유지한다. 오류 수집원과 성공 수집원은 중복될 수 있다. 이 수치는 전체 개발 진행률·운영 완료율이 아니다.

운영 DB·worker·정책·ENFORCE·재분류·배포 변경0, 추출기 실행0. AWS 인증 갱신 대기와 기존 CA/HTML 임시 자원 정리 차단은 별도 보류 상태다. 이번 원본 정리를 과거 자원 정리 완료로 간주하지 않는다. 다음 우선순위는 금산 bare HWP 응답 처리, 나머지 공식 목록/상세 연결, 환경 의존 연결 실패 진단이다.
