# 공통 전송 재검증과 충주·서천 첨부 다운로드 관측

## 현재 단계 / Gate

- [x] 현재 공통 전송 코드에서 추가16개 수집원 실제 파일 확인
- [x] 충주·서천의 정규 수집 최초 성공과 앞선 HTTP400 실패를 모두 보존
- [x] Java61개·Node23개·bootJar·399영수증/279최신공고 재현 통과
- [~] 과거 관측 포함 다운로드 경험199/223수집원(89.2%), 미확인24개
- [~] 최신 코드 다운로드 관측24/223, 신규 확인 또는 재검증199개
- [ ] 최신 코드 3표본 전체 첨부 집합 Gate0/223, 상시 운영·추출·정책·운영 E2E

분모223은 2026-09-28T15:56:12.116778+09:00 인벤토리의 활성 지역 수집원이다. 고유 지자체 수나 현재 운영 재조회 결과가 아니다. 과거 성공과 현재 코드 성공을 합쳐 현재 코드 검증률로 표시하지 않는다. HWP 추출 고도화는1.0.16에서 보류하며, 가능한 파일 수집과 개별 오류 분리에 집중한다.

## 기준선과 변경 범위

시작 HEAD는 `bf623a00eeaa6d2d80ea12fe53965382bc1c2c90`이다. 이 작업에서는 응용 코드·프로필·DB·API·UI·Flyway V85·정책·운영 설정을 변경하지 않았다. 공통 전송 지문은 이전 영덕 수정 이후 값 그대로다. 추가한 코드는 명시적 환경변수로만 실행되는 `AttachmentHttp400DiagnosticTest`이며, 진단 성공을 수집 또는 정책 승인으로 간주하지 않는다.

기존377영수증을 순서·해시 그대로 보존하고22개를 추가했다. 기존16공고의 최신 관측을 갱신하고 제목 단계 중단 공고2개를 추가하여399영수증/279최신공고다. 기존 카탈로그와 태백의 과거 기대값은 변경하지 않는다.

- 인벤토리 SHA-256: `8186842d80c326c7d7c215e59724c469c07ace7a66ab1ba9fd4e4b14403c6a28`
- 관측 producer SHA-256: `99427d305bb8148d1b4e3c41306905a1985b2de2cc733c8ce976d7a05e86fcf1`
- 과거 공통 코드의196개 성공과 현재 코드 성공 수집원의 합집합은199개다. 단순 합산하지 않는다.

## 실제 수집 결과

| 순차 배치 | 대상·결과 | 보고서 | 성공 파일 | 성공 바이트 |
| --- | --- | ---: | ---: | ---: |
| A | 성동·광진·화순·태안·양양·울진·태백·나주 | 8 | 12 | 1,399,580 |
| B | 부평·춘천·동작·강원도·경북도·삼척 성공, 충주·서천 HTTP400, 충주2공고 제목 중단 | 10 | 6 | 653,418 |
| C | 충주·서천 성공, 충주2공고 제목 중단 유지 | 4 | 2 | 192,512 |

22보고서에서 성공 파일 관측20건·2,245,510byte다. 배치별 허용 상한은 A48요청/184MiB, B60요청/230MiB, C24요청/92MiB다. 실제 요청 예약량은 각각36/32/8, 바이트 예약량19,106,437/18,536,381/4,573,399다. 예약값은 본문 시도 상한을 포함하며 실제 wire 요청 수와 같다고 단정하지 않는다. 모든 보고서에서 원본 정리true·운영쓰기0·추출 검증false다.

### 성공과 별도로 남긴 오류

- 화순39230: HWPX134,544byte 성공. 본문은 `FETCH_FAILED / DETAIL_HOST_NOT_ALLOWED`로 별도 미완료다.
- 울진35041: HWP84,480byte 성공. `ATTACHMENT_LINK_UNRESOLVED` 발견 경고를 유지하며 전체 첨부 발견 완료로 표시하지 않는다.
- 충주72039·72625: 제목 조합 미충족, 수집 요청0, 파일0. 다운로드 성공 수에 포함하지 않는다.
- 송파33174: 공식 상세 제목 칸과 숨겨진 title 값이 비어 있다. 본문과 첨부 링크 존재만으로 제목 단계를 우회하지 않았다.
- 초기 배치 옵션 `BUPYEONG`은 존재하지 않는 그룹으로 실행 전에 실패했다. `METRO_REMAINDER`로 바로잡았으며 해당 설정 실패의 공식 요청은0이다.

## HTTP400 조사와 한계

충주70852와 서천27490은 B배치에서 파일HTTP400, 동일 응용 코드의 C배치에서 정상 다운로드였다. 최종 HWP는 각각170,496byte와22,016byte이며 실제 형식 검증을 통과했다.

- 충주 SHA-256: `d0ae488f79028cb8e78987e355cfcc3aa2547ce9c05b07f139c3b0042da0695f`
- 서천 SHA-256: `f3efffe1f10d86859c61413f93a155303bcae2e1089b4e85638e5ec7c8d8ae47`

별도 공개 HTTP 조사9요청(각12초·2MiB), Java Shell 조사4요청으로 응답을 비교했다. 직접 HTTP 성공만으로 정규 수집 성공을 선언하지 않았으며 최종 정규 보고서를 근거로 집계했다. 정확한 HTTP400 발생 원인은 미확정이다. 전송 버그를 수정했다고 보고하지 않는다.

선택 진단 테스트는 고정 공개 충주 공고에 직접 파일 요청과 본문→상세→프로필 파일 요청을 비교한다. 한 실행 최대5요청·7MiB, Windows에서는 시스템 ROOT 신뢰 저장소를 사용하고 설정을 복원한다. TLS 검증 해제·인증 쿠키·사용자 세션 사용은 없다. 초기 Java 테스트의 신뢰 저장소 설정 누락은 TLS 실패로 기록했고 수정 후 진단2개를 통과했다. 응답 원문·URL·헤더는 출력하지 않고 제한된 오류 분류와 바이트·해시만 출력한다. 진단 테스트가 통과해도 다운로드 성공 여부는 별도 출력과 정규 수집 영수증으로 확인한다.

## 실행 명령 / 검증

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEONGDONG,GWANGJIN,HWASUN,TAEAN,YANGYANG,ULJIN,TAEBAEK,NAJU' -PsanebCollectionReportLabel=TRANSPORT-RECHECK-A-20260930 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHUNGJU,SEOCHEON,CHUNCHEON,SAMCHEOK,GANGWON_PROVINCE,GYEONGBUK_PROVINCE,METRO_REMAINDER' -PsanebCollectionReportLabel=TRANSPORT-RECHECK-B-20260930 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHUNGJU,SEOCHEON' -PsanebCollectionReportLabel=TRANSPORT-RECHECK-C-20260930 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :test --tests '*AttachmentHttp400DiagnosticTest' --tests '*RegionalCollectionReportArchiveTest' --tests '*AttachmentCollectionOnlySummaryTest' --tests '*ChungjuEminwonAttachmentDiscoveryProfileTest' --tests '*SeocheonDownloadContractTest' --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*AttachmentPinnedDownloadClientTest' :bootJar
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

진단 실행에만 `SANEB_ATTACHMENT_HTTP400_DIAGNOSTIC=true`를 설정하고 실행 후 이전 값으로 복원했다. 최종 선택 Java61개 통과·실패0·생략0, Node23개 통과, bootJar 통과다. 전체 Java 회귀를 이번 작업에서 다시 실행했다고 표현하지 않는다. 영수증399개/최신공고279개 재현·현재 코드24다운로드/오류 포함5수집원/엄격 Gate0을 확인했다. 화순 본문 오류는 첨부 오류5개와 별도다.

## 남은 수집원과 다음 순서

과거 관측까지 포함해 다운로드 미확인24개는 미연결 울산남구1개와 등록된23개(은평·서대문·송파·부산광역시·연제·검단·성남·평택·포천·이천·동두천·강릉·속초·평창·철원·충북도·영동·천안·공주·순창·진도·의성·성주)다. 모든 기관의 현재 장애를 재조회한 목록은 아니다.

1. 미확인24개에서 제한된 공개 표본을 확보하고 수집 가능한 파일부터 기록한다.
2. 현재 코드 기준 나머지199개는 과거 성공175개 재검증과 미확인24개로 나눠 진행한다.
3. 사이트 응답·본문·발견·다운로드·형식 오류를 각각 보존한다. 실패 파일 때문에 다른 정상 파일을 버리지 않는다.
4. 상시 worker·운영 설치·DB·정책·재분류·운영 E2E는 별도 Gate다. 이번 작업에서 실행하지 않았다.

브라우저 검증은 현재 명시 요청이 없어 사용자 정책에 따라 미실행이다. 이번 진단의 임시 원본은 제거했다. 이전 `build/qa-yeonje-gurye-20260930` 정리 차단은 해소되지 않았으며 새 원본 정리 결과와 혼동하지 않는다. 기존 미추적 `output/`, `scripts/qa/__pycache__/`는 보존한다.
