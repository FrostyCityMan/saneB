# 미추홀 첨부 재확보와 아산 파일 전송 단계 분리

## 현재 단계 / Gate

- [x] 직전 금산 호환·실파일 확보는 진전으로 분류
- [x] 공식 목록에서 미추홀·아산 추가 지원 공고 확인
- [x] 기존 실패를 보존하고 참조2건·제목 정책 계약 검증 추가
- [x] 미추홀 HWP1개56,832바이트 다운로드·형식 검증
- [!] 아산 본문·첨부 발견 성공, 파일2개 전송 시간 초과
- [x] 회귀155통과·2조건부 생략, 관측2통과, bootJar·Node23·658기록 재현 통과
- [~] 현재 프로필 기준 첨부 확보201/223(90.1%), 미확보22개 수집원
- [ ] 추출·정책 QA·상시 운영·DB/API/UI·운영 브라우저 E2E와 전체 goal 완료

기준 HEAD `b5e7cbacdb0370396b60a9ff19953a2e6b31d321`. 전 지역 첨부 발견·다운로드 우선, HWP 추출기 고도화 보류, 파일별 오류 분리를 유지한다. 수집 프로필·응용 Java·migration·운영 DB/설정은 변경하지 않았다.

## 공식 목록과 표본

미추홀 `board_code=board_13` 목록에서 실제 제목 검색 필드는 srchKey=A, 검색어 필드는 srchValue다. 상세 링크는 `/main/board/` 기준 상대경로 `view.do?sq=...`였다. 초기 넓은 sq 패턴에 동 행정복지센터 링크도 잡혔으므로 이를 공고 수로 세지 않고 게시판 상세 경로에 한정했다. 청년 검색의 창업공간 결과 대신 소상공인 검색에서 공고315063을 선택했다.

아산 `no=257` 목록에서 sltOption=1, txtKeyword=지원으로 검색하고 공식 PageNo=2 링크를 따라 공고76469를 확인했다. 첫 페이지의 전통시장/상점가 공고78033도 www·bare host에서 상세가 열리는 것은 확인했지만, 해당 제목을 기존 강한 대상 키워드 조합 충족으로 임의 인정하지 않고 수집 표본에서 제외했다. 공고76469는 기존 `인증비 지원` A그룹으로 진입할 수 있다. 키워드·우선순위는 수정하지 않았다.

| 수집원 | 새 공고 | 제목 | 표시 첨부 | 제목 경로 |
|---|---|---|---:|---|
| 미추홀 LGS-000057 | 315063 | 2026년도 중소기업 육성 및 소상공인 지원사업 융자계획 공고 | HWP1 | COMBINATION_MATCHED |
| 아산 LGS-000151 | 76469 | 2026년 친환경농산물 인증비 지원사업 신청 공고 | HWPX2 | GROUP_A_MATCHED |

기존 미추홀309943·아산80727은 그대로 유지한다. 새 표본은 MICHUHOL_SUPPORT/ASAN_SUPPORT 그룹이며 같은 source/parser/프로필과 등록 목록 주소를 사용한다. 아산은 기존 www 호스트에서 실제 상세를 확인했으므로 호스트 허용 범위를 넓히지 않았다. 외부 URL·본문·첨부 내용은 실행 지시로 취급하지 않는다.

## 실제 관측과 실패 단계

| 항목 | 미추홀315063 | 아산76469 |
|---|---|---|
| 본문 | AVAILABLE, 96자 | AVAILABLE, 701자 |
| 본문 판정 | ACCEPTED | REVIEW_REQUIRED |
| 상세 식별 / 발견 | true / FOUND, complete=true | true / FOUND, complete=true |
| 파일 | HWP56,832바이트 성공 | 2개 모두 FILE_DOWNLOAD / TRANSPORT_TIMEOUT |
| 최종 관측 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED | COLLECTION_ONLY_PARTIAL_NOT_APPROVED |

미추홀 파일 SHA-256은 `a2115646b4b8cb09ee3874f1ba4a453f38a1a8d128a3cc2c00ebf75ef474e673`다. 아산 파일은 원문/해시를 확보하지 못했으므로 성공 수에 포함하지 않는다. 제목 A그룹과 본문 REVIEW_REQUIRED는 QA 판정이며 운영 관리자 상태를 변경한 것이 아니다.

추가 연결 진단1회에서 eminwon.asan.go.kr의 TCP443과 인증서 검증을 포함한 TLS1.2 handshake는202ms 내 성공했다. HTTP 요청은0이다. 이후 공식 상세에 실린 첫 파일의3개 POST 필드를 읽어 같은 공식 FileDown.jsp로 별도 .NET HttpClient 요청1회를 보냈으나12,023ms에 시간 초과됐다. 본문·발견·TCP/TLS 성공과 파일 POST 응답 실패를 구분한다. 영구 서버 장애·방화벽·차단 원인을 단정하지 않았으며 TLS 우회, 쿠키 이식, User-Agent 위장, 타임아웃 확대 또는 반복 동일 재시도는 하지 않았다.

## 구현 및 검증

`RecoveredSupportDownloadCases`와 계약 테스트에2개 그룹을 추가했다. 기존 식별자·실패 이력 독립성, 프로필/목록 주소 유지, 새 상세의 canonical identity, 제목 조합/A그룹, 유한 예산, expectation=null을 확인한다. 카탈로그는292→294참조, 대상223개 유지다. 참조 전용293개와 기존 기대값1개이며 신규 정책 기대값 승인은 없다.

첫 회귀에서 새 아산 참조의 providerNoticeId를 잘못 계산해2개 테스트가 실패했다. 기존 정규화는 `/main/cms/`의 마지막 slash를 제거하고 query를 정렬해 해시를 계산한다. URL은 그대로 두고 카탈로그 식별 해시를 기존 Java 정규화 결과에 맞춰 수정했다. 실패한 실행에서는 관측 작업이 시작되지 않았다.

최종 회귀 XML은157개 중155통과·2조건부 생략·실패0이다. 공식 관측2개와 bootJar를 포함한 최종 명령은3분53초·종료0이다. 관측 작업 성공은 오류 분리 보고서 생성까지 포함하며 아산 다운로드 성공을 뜻하지 않는다. 로컬 조사 fixture를 요구하는 조건부 테스트는 통과로 계수하지 않았다. 전체 Java/DB 통합/운영 E2E는 재실행하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다.

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='build/qa-results/target-inventory-20260928-receipt.txt'
.\gradlew.bat --no-daemon :test --tests '*RecoveredSupportDownloadContractTest' --tests '*IncheonSecondDownloadContractTest' --tests '*ChungcheongSixthDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=MICHUHOL_SUPPORT,ASAN_SUPPORT' -PsanebCollectionReportLabel=MICHUHOL-ASAN-SUPPORT-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

실제 실행은 inventory 환경변수를 finally에서 해제했다. 최종 Node23개와658영수증/287최신 표본 재현이 통과했다.

## 지문·예산·남은 범위

기존656영수증과285최신 표본은 수정하지 않고 새2영수증·2표본을 추가했다. 결과는 대장 `michuholAsanSupportRecoveryRun`이다. 기존 읽기 전용 운영 대상 영수증을 현재 코드와 대조했으며 새 운영 조회는 아니다. 모든 프로필 지문·활성223개·스냅샷 시각2026-09-28 15:56:12 KST는 유지되고 미추홀·아산의 catalogReferenceCount만1→2로 증가했다.

인벤토리 SHA-256은 `f4b1e5ab5c1ed61ee1072546b1809343546555cd6497b489047f537aef07d60e` → `a9cc5dfa95a7c07662f9235a27cde47847f6519dd7f6495902fb5ca9fb418726`이다. 관측 클래스 지문 `caeef0d8ae1727e6b4ceb96c6ab68fa1440d7dfd7549c87b297b906e60e2b148`은 유지한다.

별도 공식 HTTP 진단은14회(GET13/POST1), 요청당2MiB·연결5초·요청12초·redirect0·cookie 저장0·TLS 검증 유지다. 원문은 메모리에서만 처리해 디스크 기록0이다. TCP/TLS 진단1회는 별도이며 HTTP0이다. 실제 Java 관측은 최대12요청·46MiB, 본문 포함 예약9회·4,546,048바이트다. HTTP 진단과 Java 예약 상한 합계26회, 원본 정리true·운영쓰기0이다.

현재 지문 첨부 확보200→201/223(90.1%), 미확보23→22다. 남은22개는 최초 미확인19개와 과거 성공 후 현재 미확보3개(아산·봉화·남해)다. 오류 수집원32, 과거 포함204/223, 엄격3공고 전체 집합 Gate16/223은 유지한다. 미추홀의 이전 실패도 남아 있어 지역 전체 Gate 완료로 승격하지 않는다.

운영 DB·정책·worker·ENFORCE·재분류·배포 변경0, 추출기 실행0. AWS 인증 갱신 대기와 과거 임시 CA/HTML 정리 차단도 별도 보류다. 아산은 이후 다른 실행 환경 또는 공식 다운로드 정상 흐름의 차이를 확인해야 하며, 같은 POST를 계속 반복하지 않는다. 다른 미확보 수집원 점검은 계속할 수 있으므로 전체 goal을 차단 또는 완료로 처리하지 않는다.
