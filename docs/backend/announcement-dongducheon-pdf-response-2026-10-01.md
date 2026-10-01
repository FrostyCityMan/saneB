# 동두천 PDF 비파일 응답 검증과 장기 goal 재개

## 2026-10-01 최종 파일 수집 확인: PDF·HWPX 2개 성공

- [x] 수정본 `cc9678b8bf87b67bee777365e6b9bf8a9ef3c7b7`의 서울 격리 실행에서 공식 파일 2개, 299,468byte 확보.
- [x] 710영수증/289표본 재집계. 과거 한 번 이상 및 최신 표본 기준 210/223수집원, 최초 미확인 **13개**.
- [x] 원본·임시 unit·S3 전송물·로컬 전송 ZIP 정리. 운영 JAR 불변·healthUp 확인, 운영 쓰기 0.
- [~] 현재 프로필 지문 파일 근거 21/223. 엄격한 현재 3표본·전체 파일 집합 Gate 0/223과 운영 E2E는 별도 미완료.
- [ ] 텍스트 추출·상시 worker·운영 DB/API/UI 검증. 이번 실행은 수집 전용이며 HWP 고도화는 계속 보류.

| 고정 공고 | 본문 / 첨부 결과 | 파일 SHA256 |
|---|---|---|
| 44176 | 본문 71자 AVAILABLE·ACCEPTED, PDF 216,452byte | `70e243e70dafa6d6a926e88b0436e9481ddf028f31d7c24fdd7bfd1d6d4665c6` |
| 45339 | 본문 29자 AVAILABLE·REVIEW_REQUIRED, HWPX 83,016byte | `c89ea4e6bc0257be696559623d33062237514f4d2cb6e85526bfd436d1a2ddff` |

45339는 본문 전송에는 성공했으나 판정에 필요한 정보가 부족하여 `BODY_UNAVAILABLE` 사유의 검수를 유지한다. 첨부 다운로드 성공을 본문·첨부 분석 또는 최종 승인으로 바꾸지 않았다. 두 파일 모두 공식 중간 폼→게재기간 조회→파일의 3회 전송이 완료됐고 발견된 파일 집합 1/1개를 확보했다. 기존 44784의 제목 단계 중단은 그대로다.

실행 ID `576b69374ef64024bc883068a24c9f43`, SSM `dd8647a5-062f-4c4e-9604-1ada3500469e`, 실행 모드 `SEOUL_COLLECTION_03`, 종료 Success/0·29.085초. 실행 코드 hash `3159b5ab4a452617e65f88d0b17a465f70b838d3844cda4cff3154aa3f0d1a98`, 프로필 hash `c6b180dff41b41dd815ca04adf9fe14f63b5ad9b0e941448f6a6a7557e9dbc4d`, 결과 영수증 SHA256 `7fc03a11eb798d74b19cd99922f4a7a221af7398b0503f21c65a837746693dab`다. 기존 실패 실행과 별개의 변경 코드 실행이며 실패 이력은 보존했다.

이번 상한은 12요청·46MiB·20분, CPU 1개·메모리 512MiB·임시공간 1GiB다. 영수증 예약은 12회·5,645,876byte이며 본문 예약 상한을 포함하므로 실제 wire 사용량과 구분한다. 아래 이전 후속 단계의 상한 30요청과 합쳐 이번 수정 진단 단계의 상한은 42요청이다. 더 오래된 수집 이력도 대장에 남는다. 패키지는 135파일·89,890,355byte, SHA256 `c5c3532384fae18cee148060308e0075a04a8b1728cc50e9536ac57a74b0dfd8`이며 삭제 전 정확한 경로·hash와 원격 정리 완료를 검증했다. 결과 metadata와 실행 계획은 보존했다.

남은 최초 미확인 수집원은 은평·서대문·울산 남구·성남·평택·이천·포천·강릉·속초·철원·영동·공주·의성 13개다. 분모 223은 2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수나 현재 운영 완료율이 아니다. 현재 지문 재검증 대기 202개와 최초 미확인 13개를 혼동하지 않는다.

검증 명령: `installAttachmentContractQa attachmentBbsObservationProbeJar` 패키지 재빌드 성공. `node build/qa-tools/import-dongducheon-period-fixed.mjs`는 모든 입력 hash·종료·정리·코드 지문을 검증하고 다른 수집원 표본 불변을 확인하여 대장을 재계산했다. `node --test scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-collection-receipts.test.mjs` 30개 통과·실패/생략 0, `node scripts/qa/verify-collection-receipt-index.mjs` 710영수증/289표본 재현, `git diff --check` 통과. 애플리케이션 코드·DB/API·Flyway·정책은 이번 결과 기록에서 변경하지 않았다. `cc9678b`의 CI [36821916064](https://github.com/FrostyCityMan/saneB/actions/runs/36821916064)는 최종 completed/success를 확인했다. 이 회차에는 CI XML 세부 개수를 회수하지 않았으며 과거 테스트 개수를 현재 값으로 쓰지 않는다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.

아래 재검증 대기·파일 0·미확인 14 표시는 이번 성공 이전의 이력이다.

## 2026-10-01 후속: 중간 폼 처리 누락 수정, 실제 파일 재검증 대기

- [x] 서울에서44176의 HTML2,596byte가 오류 문구만 있는 페이지가 아니라 공식 다운로드 중간 폼임을 확인.
- [x] 동두천 프로필에 `GET 중간 폼 → POST 게재기간 조회 → POST 파일` 처리를 연결.
- [x] 폼의 파일 ID가 원래 저장 파일명과 일치하지만 ASCII 전용 조건에 걸리는 문제를 후속 확인하고 수정.
- [x] 최종 수정본 Java24개 통과·bootJar 성공, Python40개·Node30개 통과,709영수증/289표본 재현.
- [!] 실제 파일 확보0. 최종 파일 ID 수정본의 외부 실행은 아직 미검증. 수정 전 서울 실행에서44176은 폼 검증 중단,45339는 중간 폼 요청 timeout.
- [~] 파일 확보 이력209/223·최초 미확인14 유지. 현재 프로필 지문 파일 근거20/223, 전체 집합 및 운영 E2E 미완료.

### 원인과 구현 범위

이전의 HTML 응답을 곧바로 파일 서명 실패로 처리하던 경로에 공식 다단계 다운로드 처리가 빠져 있었다. 서울에서 동일 응답 SHA256 `ef64e65be14c93b36e395b543b1b9a904447723f4b29ccf2751a5aea552c1d12`를 재현했고, `form#form[name=form][method=post]`의 action `FDSendNewPbs.jsp`,10개 hidden 필드,공식 `OfrAction.do` 조회를 확인했다. 원문·폼의 불투명 값·파일명·쿠키는 보관하거나 출력하지 않았다.

`DongducheonPeriodAttachmentDiscoveryProfile`은 기존 발견 프로필을 감싼다. 호스트·경로·필드 집합·동일 파일 식별·본문/파일 한도·취소/lease/byte 예약·원본 정리를 유지한다. `isHome`은 공식 빈 값 그대로이며 `Y`로 바꾸지 않는다. 기간을 미리 채우거나 만료일을 연장하지 않는다. 잘못된 폼·잘못된 기간·만료 기간은 파일 POST 전에 중단한다. 사이트 JavaScript는 실행하지 않는다. 기존 통영의 허용 범위를 다른 사이트에 일괄 적용하지 않는다.

첫 서울 실행에서44176은 폼 처리 중 `ATTACHMENT_DOWNLOAD_BLOCKED`였다. 원인 분리 진단 결과 파일 ID가 원래 파일명에 정확히 포함됨은true,ASCII 정규식 일치는false,하이픈0개,그 밖의 문자2개였다. 문자 원문을 추측하여 특정 문자열을 하드코딩하지 않는다. 수정본은 길이10~200·경로 구분자/상위 경로/퍼센트/제어문자 금지와 `Pattern.quote` 기반 원래 저장 파일명 대조를 함께 적용한다. 유니코드·대괄호 합성 표본과 다른 파일 ID 거부를 테스트했으며 이 합성 표본은 실제 파일 검증이 아니다.

운영 프로필·정책은 게시하지 않았다. DB/API·화면·Flyway V85·A/B 분류·HWP1.0.16은 변경하지 않았다. inventory246대상 중 동두천 LGS-000112의 프로필 지문만 바뀌었으며 최종 지문은 `c6b180dff41b41dd815ca04adf9fe14f63b5ad9b0e941448f6a6a7557e9dbc4d`다.

### 실행 근거

| 환경 / 실행 | 결과 |
|---|---|
| 서울 구조 진단 SSM `0b799375-0426-4c2e-bf84-2110fe1a84a0` | 상세200·568,155byte,중간 폼200·2,596byte |
| 로컬 실제 파이프라인2공고 | 본문 AVAILABLE,상세 재조회 TRANSPORT_TIMEOUT,새 파일 절차 도달 전 중단. 원본 정리true·수집 task2실패 |
| 서울 격리 SSM `352ec3b8-ef49-4138-bbe3-1754772a897b` | 종료Success/0·35.421초. **영수증 보존 성공일 뿐 파일 성공이 아님** |
| 서울44176 PDF 표본 | 본문71자·발견1·중간 폼1회 완료 후 프로필 검증 중단. 파일0 |
| 서울45339 HWPX 표본 | 본문29자·발견1·중간 폼1회 요청 timeout. 파일0 |
| 폼 조건 진단 SSM `d70faacd-36e4-444e-ae65-f7a1a5b66ac0` | 같은 응답 hash·폼 구조. 원래 파일명과 ID 일치true,ASCII ID 조건false |
| 문자 범위 진단 SSM `2bccc306-be4a-46f9-b1ca-273c3d42a007` | 같은 응답 hash,문자 범위의 boolean/count만 기록. 원문 값 비출력 |

격리 실행 ID `af2b9d8e90f54a49860fe6fb9bb8ccdb`,패키지135파일/89,890,233byte,패키지 SHA256 `6b20974c16427be91f4d6a9246cd2250704e2ed42818a8404e1b691868d9c027`,당시 실행 코드 hash `cf1dacf7e1904d547804beede3b307141071cfc6f58bffb1b70996e207d51662`다. 당시 프로필 지문 `09d9358ea4faac2125584a6ee6c081a8d9adac200d557b5342c1cef9421d5b13`은 최종 수정본과 다르다. 원격 결과 SHA256 `e5b565c7cfa2eec304b1e54bda884af8bdb5906628a239fa2e0605efa24e984a`를 보존한다. 최종 수정으로 이 실패를 삭제하거나 성공으로 치환하지 않았다.

서울 수집 상한12요청/46MiB,실제 영수증 예약8회/5,343,780byte다. 로컬 상한12요청/46MiB,예약6회/4,194,304byte. 별도 구조 진단은3회×2GET=6요청으로 각2MiB 상세+32KiB 중간 폼 상한이다. 합계 상한30요청이며 예약값은 실제 wire 요청 수가 아니다. 이전 동두천 관측 이력도 대장에 남기며 이번 숫자로 누적 이력을 초기화하지 않는다.

CPU1개·메모리512MiB·임시공간1GiB·최대20분의 임시 unit에서 실행했다. 원본/임시 자원/원격 전송물 정리true,unitInactive·운영JAR불변·healthUp true. S3 패키지와 정확한 hash를 확인한 로컬 전송 ZIP만 삭제했고 계획·metadata는 남겼다. 운영 DB·설치·정책·worker 변경0. 사용자의 output·미추적 캐시는 보존했다.

### 검증과 다음 조치

1. 최초 경계 테스트: 동두천11개 포함54통과/실파일 fixture 조건2생략. 기존 통영과 수도권 프로필 회귀 포함.
2. 등록/카탈로그/정책 snapshot 테스트·bootJar 성공. 이어 수행한 실제 로컬 수집은2실패이므로 해당 전체 Gradle 명령의 종료는실패다.
3. 서울 실행기: Java54개·Python40개·쉘/PowerShell 문법 검사·격리 패키지 빌드 통과.
4. 최종 파일 ID 수정본: 동두천12+inventory3+실행기9=24통과·실패/생략0,bootJar 성공.
5. Node30개 및709영수증/289표본 재현 통과. 현재 inventory SHA256 `5a15c0f7ffcbedf5103ca02851ffcfd969b32c84d8202bb87edab5f6da488858`.
6. 다음은 **최종 수정본의 서울 고정 표본 재검증**이다. 기존03실행은 종료·정리 완료이며 이를 재시작하지 않는다. 변경 코드와 새로운 한정 실행의 사용량을 별도 기록한다. 파일 성공·추출·운영 E2E 완료를 선행 선언하지 않는다.

직전 `5b8cd43`의 [CI36819969333](https://github.com/FrostyCityMan/saneB/actions/runs/36819969333)는 최종success다. 이번 동두천 변경 전 코드의 결과이며 이번 수정본 CI나 실제 파일 수집 증거가 아니다. 브라우저는 현재 명시 지시가 없어 정책상 미실행. 아래 수치는 이전 시점 이력이다.

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
