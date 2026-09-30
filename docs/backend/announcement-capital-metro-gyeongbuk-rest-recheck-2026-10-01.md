# 경기·광역권·경북 잔여 수집원 첨부 재검증

## 현재 단계 / Gate

- [x] 고정24공고·24수집원에서 본문과 정상 첨부 확보
- [x] 정상39파일을 보존하고 안성 시간 초과·영주 미지원 항목 분리
- [x] 기존505영수증 보존, 529기록/282공고 재현, Node23개 통과
- [~] 현재 코드 다운로드140/223·확인/재검증83개
- [~] 과거 포함 다운로드204/223(91.5%)·최초 미확인19개 유지
- [ ] 전체 텍스트·상시 worker·DB/API/UI·운영 E2E와 전체 goal 완료

기준 HEAD `ee4bb431ffeb32240e57997f7d6c1e98d1b2fc83`. 이번 회차는 기존 고정 표본과 현재 코드를 사용한 수집 재검증이며 응용/테스트 코드·프로필·카탈로그·migration·정책은 변경하지 않았다. HWP 추출 고도화 보류와 정상 파일 보존·개별 오류 분리 방침을 유지한다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 현재 운영 재조회 또는 전체 개발 완료율이 아니다.

## 실제 결과

| 묶음 | 수집원 | 다운로드 확인 | 정상 파일 / 바이트 | 관측 실행 |
|---|---|---:|---:|---|
| 경기 잔여 | 용인·안산·파주·광명·안성·구리·의왕·여주 | 8/8 | 12개 / 1,345,976 | 8통과, 종료0, 44초 |
| 광역권 잔여 | 광주 동구·서구·북구, 대전 동구, 울산시·중구·북구, 세종 | 8/8 | 10개 / 1,168,809 | 8통과, 종료0, 36초 |
| 경북 잔여 | 포항·경주·안동·영주·상주·문경·경산·고령 | 8/8 | 17개 / 4,934,120 | 8통과, 종료0, 41초 |

합계 **24수집원·39파일·7,448,905바이트**다. 본문24건은 모두 AVAILABLE이다. 관측24개가 통과했지만 부분 실패를 정확히 기록한 경우도 통과하므로 전체 파일 성공이나 운영 승인으로 바꾸어 표현하지 않는다. 과거 보관 공고가 포함되어 있으며 최신 목록의 상시 유입과 전체 텍스트 분석은 별도 검증 사항이다.

### 파일별 오류

| 공고 | 확보 결과 | 남은 문제 | 상태 |
|---|---|---|---|
| ANSEONG-72476 | 정상2개·142,826바이트 | HWPX 힌트 파일1개, FILE_DOWNLOAD/TRANSPORT_TIMEOUT | COLLECTION_ONLY_PARTIAL_NOT_APPROVED |
| YEONGJU-24194 | 정상2개·367,856바이트 | 지원 형식으로 식별되지 않은1개, formatHint=null·downloadAllowed=false | UNSUPPORTED_NOT_DOWNLOADED를 보존한 부분 성공 |

안성은 과거 정상1개에서 이번2개로 관측 결과가 늘었으나 코드 수정 효과라고 단정하지 않는다. 영주 미지원 항목의 실제 포맷을 추측하거나 PDF/HWP/HWPX로 강제 다운로드하지 않았다. 다른22표본은 해당 고정 공고 첨부 집합 수집 완료이며 수집원별3표본·운영 전체 완료는 아니다.

## 범위·예산·원본 정리

세 묶음은 이전 Gradle 종료 확인 후 순차 실행했다. 기존 상한은 영주7요청·23MiB, 나머지 표본6요청·23MiB로 총145요청·552MiB다. 본문 포함 예약은114회·63,359,728바이트이며 실제 wire 요청·전송량과 구분한다. 추가 공개 진단 요청은 없었다.

새 관측24개 모두 원본 정리true·운영쓰기0, 첨부 추출·정책 QA·기대값 승인false다. 보고서는 `build/reports/attachment-regional-collection/`의 `CAPITAL-REST-RECHECK-20261001`, `METRO-REST-RECHECK-20261001`, `GYEONGBUK-REST-RECHECK-20261001` 라벨24개다. 경로·해시는 receipt index, 집계는 regional ledger의 `capitalMetroGyeongbukRemainingRecheckRun`에 기록했다.

기존505영수증은 변경하지 않고24개를 추가했다. 최신 공고282개 중24개만 새 관측으로 갱신하고 나머지258개는 보존했다. 인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA-256 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`는 기존과 같다.

## 실행 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YONGIN,ANSAN,PAJU,GWANGMYEONG,ANSEONG,GURI,UIWANG,YEOJU' -PsanebCollectionReportLabel=CAPITAL-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GWANGJU_DONGGU,GWANGJU_SEOGU,GWANGJU_BUKGU,DAEJEON_DONGGU,ULSAN_CITY,ULSAN_JUNGGU,ULSAN_BUKGU,SEJONG' -PsanebCollectionReportLabel=METRO-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=POHANG,GYEONGJU,ANDONG,YEONGJU,SANGJU,MUNGYEONG,GYEONGSAN,GORYEONG' -PsanebCollectionReportLabel=GYEONGBUK-REST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 관측24개와 Node23개, 529기록/282공고 재현을 통과했다. 코드 변경이 없어 Java 단위 전체·bootJar는 이번에 재실행하지 않았다. 브라우저 검증은 현재 명시 지시가 없어 정책상 미실행이다.

## 남은 범위

현재 코드 다운로드116→140개, 확인·재검증107→83개다. 첨부 오류가 포함된 수집원은18→20개이며 정상 다운로드 수와 중복될 수 있다. 과거 포함204/223·최초 미확인19개는 유지하고, 엄격한 수집원별3표본 전체 집합 Gate는0/223으로 별도 관리한다. 이번 회차에서 모든 기존 오류를 다시 조회한 것은 아니다.

운영 DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 갱신 대기와 과거 공개 CA·HTML 임시 자원 정리 차단도 별도 미해결이다. 이번 다운로드 원본 정리 성공이 과거 자원 정리까지 의미하지 않는다. 전체 goal은 미완료로 유지한다.
