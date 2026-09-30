# 전남·혼합 지역·서울 잔여 수집원 재검증

## 현재 단계 / Gate

- [x] 24수집원·27고정 공고 관측 완료, 실패도 결과에 포함
- [x] 20수집원·22공고에서 정상 첨부42개 확보
- [x] 제목 중단, 본문 오류, 발견 경고, 파일 실패를 분리
- [x] 기존529영수증 보존, 556기록/282공고 재현, Node23개 통과
- [~] 현재 코드 다운로드160/223·확인/재검증63개
- [~] 과거 포함 다운로드204/223(91.5%)·최초 미확인19개 유지
- [ ] 전체 텍스트·상시 worker·DB/API/UI·운영 E2E와 전체 goal 완료

기준 HEAD는 `2b819b2f40de5c61fb40a67ceda7a0cf88179dac`다. 응용/테스트 코드·프로필·카탈로그·migration·운영 설정은 변경하지 않았다. HWP 추출 고도화 보류와 성공 파일 보존·개별 오류 분리 방침을 유지한다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 현재 운영 재조회나 전체 개발 완료율이 아니다.

## 실제 수집 결과

| 묶음 | 수집원 | 다운로드 성공 | 정상 파일 / 바이트 | 관측 실행 |
|---|---|---:|---:|---|
| 전남·경남 | 보성·함평·장성·영암·담양·영광·창원·통영 | 7/8 | 7개 / 787,083 | 제목 중단 포함9통과, 종료0, 63초 |
| 혼합 지역 | 경남고성·함안·제주시·서귀포·봉화·울릉·금산·부여 | 6/8 | 8개 / 1,348,432 | 7통과·금산1실패, 종료1, 43초 |
| 서울 잔여 | 중구·용산·강북·도봉·노원·구로·금천·강남 | 7/8 | 27개 / 6,738,281 | 구로3공고 포함9통과·강남1실패, 종료1, 51초 |

합계 **20수집원·22공고·42파일·8,873,796바이트**다. 실제 관측 테스트는25통과·2실패이며, 통과 중에는 오류가 정확히 기록된 부분 결과와 제목 중단이 포함된다. 다운로드 확인, 첨부 집합 완료, 본문 확보, 최종 운영 승인을 같은 지표로 취급하지 않는다. 보관된 과거 공고도 포함하므로 최신 공고의 상시 유입을 증명하지 않는다.

구로35870·39520·49626은 각각1·5·1파일을 확인했다. 현재 지문 기준3개 적격 표본의 전체 첨부 집합이 충족되어 수집원별 엄격한 **첨부 수집 Gate는0→1/223**이다. 해당 수집원은 `LGS-000018`이며 이는 추출·정책 승인·worker/DB/API/UI·운영 E2E Gate가 아니다. 과거 엄격 검증 기록을 최신 코드 결과로 치환하지 않았다.

## 성공과 별도로 보존한 문제

| 공고 | 확보한 결과 | 미완료 / 오류 |
|---|---|---|
| DAMYANG-37086 | 첨부1개54,272바이트 | 본문 FETCH_FAILED/NETWORK_ERROR |
| TONGYEONG-49251 | 본문 확보 | ATTACHMENT_LINK_UNRESOLVED, 파일1개 TRANSPORT_TIMEOUT |
| BONGHWA-32956 | 본문 확보 | 발견 경고, 파일2개 TRANSPORT_TIMEOUT, 미지원1개 미다운로드 |
| GEUMSAN-EA6A4E53C07C7F05A9E9240DBB006D43 | 없음 | BODY_SELECTOR_CHANGED, TITLE_CONFIRMATION/OBSERVATION_FAILED |
| GOSEONG-5733464 | 첨부1개107,520바이트 | ATTACHMENT_LINK_UNRESOLVED |
| GANGNAM-64668 | 없음 | BODY_SELECTOR_CHANGED, TITLE_CONFIRMATION/OBSERVATION_FAILED |
| NOWON-20260824152519260 | 첨부3개602,727바이트 | 본문 BODY_TEXT_EMPTY |

영광29278은 `COMBINATION_NOT_MATCHED/TITLE_COMBINATION_NOT_MATCHED`, 보고서 상태 `TITLE_EXCLUDED_NOT_FETCHED`로 요청0·파일0이다. 제목 중단을 기술 오류나 실제 다운로드 성공으로 집계하지 않는다. 같은 영광29330은 파일1개를 확보했다. 강북은 이번4파일을 확인했지만 과거3파일 관측과 오류 기록은 이전 영수증에 그대로 남겼다.

첨부 수집 전용 보고서의 완료 상태는 본문 오류를 자동 해소하지 않는다. 담양·노원처럼 파일만 확보한 공고를 정상 정밀 후보나 전체 분석 완료로 승격하지 않는다. 오류 수집원25개는 발견/파일 중심 집계여서 본문만 실패한 수집원을 전부 포함하는 전체 장애 수가 아니다.

## 금산·강남 공개 상세 진단

공식 고정 상세를 Windows HTTP로 각1회 추가 조회했다. TLS 검증, collector User-Agent, redirect 금지, 요청당12초·2MiB 상한을 유지했고 HTML·쿠키·원본을 디스크에 저장하지 않았다.

- 금산: HTTP200·1,917,562바이트. 페이지 제목은 고시/공고 보기이지만 예상 공고 문구와 `program--contents`, `bbs--view--tit`, `bbs--view--file`, `bbs--view--content` 모두 없다.
- 강남: HTTP200·439,608바이트. 페이지 제목은 고시공고이고 `post-title`, `post-content` 문자열은 있지만 예상 공고 문구와 `bbs-view-file`은 없다. 오류 관련 문구도 포함됐다.

이는 예상 공고·첨부 영역 미확보의 추가 근거이며 공고 삭제·사이트 개편·일시 응답 오류 중 원인을 확정하는 증거는 아니다. HTTP200이나 페이지 제목만으로 성공 처리하지 않았고 selector를 임의로 넓히지 않았다. 다음 진단에는 현재 공식 목록의 이동 계약과 실제 유효 지원 공고 식별이 필요하다.

## 예산·대장·검증

27표본의 기존 상한 합계는190요청·673MiB, 본문 포함 예약은137회·72,855,462바이트다. 도봉12요청/43MiB, 강북20요청/32MiB, 강남8요청/43MiB, 금산6요청/26MiB, 구로 각8요청/23MiB이며 나머지는6요청/23MiB다. 실제 wire 전송량과 예약량을 구분한다. 별도 공개 진단2GET은 위 예약 합계 밖의 요청이다.

보고서는 `build/reports/attachment-regional-collection/`의 `SOUTH-REST-RECHECK-20261001`, `MIXED-REST-RECHECK-20261001`, `SEOUL-REST-RECHECK-20261001` 라벨27개다. 모두 원본 정리true·운영쓰기0, 추출·정책 QA·기대값 승인false다. 경로·해시는 receipt index, 집계는 regional ledger의 `southMixedSeoulRemainingRecheckRun`에 있다.

기존529영수증을 수정하지 않고27개를 추가했다. 최신282공고 중27개만 갱신하고255개는 보존했다. 인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA-256 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`를 유지한다.

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=BOSEONG,HAMPYEONG,JANGSEONG,YEONGAM,DAMYANG,YEONGGWANG,CHANGWON,TONGYEONG' -PsanebCollectionReportLabel=SOUTH-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GOSEONG,HAMAN,JEJUSI,SEOGWIPO,BONGHWA,ULLEUNG,GEUMSAN,BUYEO' -PsanebCollectionReportLabel=MIXED-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEOUL_JUNGGU,YONGSAN,GANGBUK,DOBONG,NOWON_SUPPORT,GURO,GEUMCHEON,GANGNAM' -PsanebCollectionReportLabel=SEOUL-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 관측25통과/2실패, Node23통과, 556기록/282공고 재현 통과다. 코드 변경이 없어 Java 단위 전체·bootJar는 이번에 재실행하지 않았다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.

현재 코드 다운로드140→160개, 확인·재검증83→63개다. 63개는 과거에도 다운로드 미확인19개와 과거 성공했지만 현재 코드 확인이 없거나 실패한44개를 포함한다. 과거 포함204/223·최초 미확인19개는 그대로이며, 이번에 모든 기존 오류를 다시 조회한 것은 아니다.

운영 DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 갱신 대기, 과거 공개 CA·HTML 임시 자원 정리 차단도 별도 미해결이다. 이번 원본 정리 성공으로 과거 자원까지 정리됐다고 보고하지 않는다. 전체 goal은 미완료로 유지한다.
