# 인천·강원·대구권 첨부 재검증과 미추홀 상세 오류

## 현재 단계 / Gate

- [x] 25수집원·26고정 공고를 순차 검증
- [x] 24수집원에서 본문 AVAILABLE 및 정상 첨부 33개 확인
- [x] 미추홀 상세 실패와 정선 제목 중단을 서로 다른 상태로 보존
- [x] 기존 479영수증 보존, 505기록/282공고 재현, Node23개 통과
- [~] 현재 코드 다운로드116/223·확인/재검증107개
- [~] 과거 포함 다운로드204/223(91.5%)·최초 미확인19개 유지
- [ ] 전체 텍스트·상시 worker·DB/API/UI·운영 E2E와 전체 goal 완료

기준 HEAD는 `3dd1bd068a7368018b33d7c9626f7711c6af5de9`다. 응용/테스트 코드·프로필·카탈로그·Flyway·정책·운영 설정을 바꾸지 않은 수집 재검증이다. HWP 추출 고도화 보류, 성공 파일 보존·개별 오류 분리 방침을 유지한다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 현재 운영 재조회나 전체 개발 완료율이 아니다.

## 실제 수집 결과

| 묶음 | 대상 | 다운로드 성공 | 정상 파일 / 바이트 | 관측 결과 |
|---|---|---:|---:|---|
| 인천권·대덕 | 인천시·제물포·영종·미추홀·계양·강화·서해·대덕 | 7/8수집원 | 14개 / 6,598,739 | 7통과·미추홀1실패, 종료1, 39초 |
| 강원·대전 서구 | 원주·동해·홍천·화천·양구·인제·고성·정선·대전 서구 | 9/9수집원 | 11개 / 3,484,722 | 제목 중단 포함10통과, 종료0, 43초 |
| 대구권·대전 중구 | 대구시·동구·서구·남구·북구·수성·군위·대전 중구 | 8/8수집원 | 8개 / 633,574 | 8통과, 종료0, 35초 |

합계 **24수집원·33파일·10,717,035바이트**다. 실제 공고 관측 테스트는 **25통과·1실패**이며 제목 중단1건이 통과에 포함된다. 다운로드 성공 수와 테스트 통과 수를 혼동하지 않는다. 확보한 본문은24건 모두 AVAILABLE이나 전체 문서 의미 검증·최종 승인·운영 상시 유입의 증거는 아니다. 보관된 과거 공고도 포함한다.

계양 `GYEYANG-53151`은 이번에 첨부4개 모두 다운로드했다. 과거 표본에서 확인된 파일3개와 나머지 실패 기록은 이전 영수증에 남겨둔다. 정선 `JEONGSEON-37876`은 `TITLE_NOT_ELIGIBLE_NOT_FETCHED`·요청0·첨부0으로 정상 중단했다. 같은 정선의 적격34952는 정상 파일2개를 확인했다.

### 실행 입력 오류도 별도 기록

최초 강원 호출은 미등록 그룹명 `WONJU`로 초기화 오류 `UNKNOWN_OBSERVATION_GROUP`가 발생했다(14초·종료1). 표본 생성 단계에서 중단돼 외부 관측·보고서 생성은 없었다. 새 코드를 추가하거나 실패를 지우지 않고 기존 `EXISTING_SECOND` 그룹을 확인해 원주와 대전 서구를 함께 실행했다. 따라서 최초 안내24수집원·25공고보다 1수집원·1공고가 늘었다. 초기화 실패1건은 위 실제 공고 관측26건과 별도다.

## 미추홀 오류 진단

고정 `MICHUHOL-309943`은 본문 `FETCH_FAILED/BODY_SELECTOR_CHANGED`, 상세 제목 검증 단계 `TITLE_CONFIRMATION/OBSERVATION_FAILED`로 중단됐다. 실제 첨부 목록을 확보하지 못해 파일 요청은 실행하지 않았다. `files=[]`를 첨부 없음으로 해석하지 않는다.

공식 상세와 목록을 Windows HTTP로 각1회 추가 조회했다. TLS 검증, collector User-Agent, redirect 금지, 요청당12초·2MiB 상한을 유지했다. HTML·쿠키·원본 파일은 저장하지 않고 구조 유무만 확인했다.

- 상세309943: HTTP200·5,529바이트. 예상 제목 문구와 `board-view-s1`, `board-title`, `file-crawling`이 모두 없고 오류 관련 문구가 포함됐다.
- 공식 `board_13` 목록: HTTP200·137,577바이트, 페이지 제목은 고시/공고. `board/view.do` 문자열은 없었으며 오류 관련 문구도 포함됐다. 이것만으로 실제 목록 행 정상 여부나 게시판 전체 장애를 확정하지 않는다.

현재 고정 상세에서 예상 공고를 확보하지 못했다는 사실까지만 확정한다. 공고 삭제·주소 계약 변경·일시 응답 오류 중 원인은 미확정이다. HTTP200만으로 성공 처리하거나 selector를 넓히지 않았다. 다음 조사는 현재 공식 목록의 공고 이동 계약과 유효 지원 공고 식별이 필요하며 같은 상세를 무조건 재시도하지 않는다.

## 요청 예산과 증거 보존

관측 보고서26개에 기록된 기존 상한 합계는195요청·655MiB, 실제 예약 합계는109회·67,254,745바이트다. 계양은7요청·23MiB, 화천은 기존44요청·80MiB이고 나머지는6요청·23MiB다. 정선 제목 중단 표본은 상한만 선언되고 실제 예약은0이다. 예약량은 실제 wire 요청·전송량과 다르며 별도 공개 진단2GET은 위 관측 예약 합계에 포함하지 않았다.

새 보고서는 모두 원본 정리true·운영쓰기0, 첨부 추출·정책 QA·기대값 승인false다. 보고서 경로는 `build/reports/attachment-regional-collection/`의 `INCHEON-NEXT-RECHECK-20261001`, `GANGWON-NEXT-RECHECK-20261001`, `DAEGU-NEXT-RECHECK-20261001` 라벨26개이며, 각 해시는 receipt index에 추가했다. 집계는 regional ledger의 `incheonGangwonDaeguCurrentCodeRecheckRun`에 있다.

인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA-256 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`는 그대로다. 기존479영수증을 수정하지 않고26개를 추가했으며 최신282공고 중26개만 갱신했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=INCHEON_CITY,JEMULPO,YEONGJONG,MICHUHOL,GYEYANG,GANGHWA,SEOHAE,DAEDEOK' -PsanebCollectionReportLabel=INCHEON-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EXISTING_SECOND,DONGHAE,HONGCHEON,HWACHEON,YANGGU,INJE,GW_GOSEONG,JEONGSEON' -PsanebCollectionReportLabel=GANGWON-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DAEGU_CITY,DAEGU_DONGGU,DAEGU_SEOGU,DAEGU_NAMGU,DAEGU_BUKGU,SUSEONG,GUNWI,DAEJEON_JUNGGU' -PsanebCollectionReportLabel=DAEGU-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

관측25통과/1실패 외 초기화1실패를 위에 별도 기록했다. Node23개, 505기록/282공고 재현은 통과했다. 코드 변경이 없어 Java 단위 전체·bootJar는 이번에 재실행하지 않았다. 브라우저 검증은 현재 명시 지시가 없어 정책상 미실행이다.

현재 코드 다운로드92→116개, 확인·재검증131→107개다. 첨부 오류 포함 수집원17→18개는 정상 파일 확보와 중복될 수 있다. 엄격한 수집원별3표본 전체 집합 Gate는0/223으로 별도 유지한다. 과거 포함 다운로드204/223·최초 미확인19개는 그대로이며 이번 회차가 모든 기존 오류를 재조회한 것은 아니다.

운영 DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 갱신 대기, 과거 공개 CA·HTML 임시 자원 정리 차단은 별도 미해결이다. 이번 원본 정리 성공으로 과거 자원까지 정리됐다고 보고하지 않는다. 전체 goal은 미완료로 유지한다.
