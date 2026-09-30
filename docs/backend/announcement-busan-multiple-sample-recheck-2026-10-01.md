# 부산권 15수집원 다중 표본 재검증

## 현재 단계 / Gate

- [x] 15수집원·45고정 공고 관측, 39공고·정상 첨부65개 확보
- [x] 제목 중단2·첨부 없음1·부분 실패5공고 별도 기록
- [x] 기존556영수증 보존, 601기록/282공고 재현, Node23통과
- [~] 현재 코드 다운로드175/223·확인/재검증48개
- [~] 과거 포함 다운로드204/223·최초 미확인19개 유지
- [ ] 전체 추출·worker·DB/API/UI·운영 E2E 및 전체 goal 완료

기준 HEAD는 `96b27cd545f0eedd29e36aa0c9f64b3e1453a13d`다. 응용/테스트 코드·프로필·카탈로그·migration·운영 설정은 변경하지 않았다. HWP 추출 고도화 보류, 성공 파일 보존·개별 오류 분리 방침을 유지한다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 현재 운영 재조회나 전체 개발 완료율이 아니다.

## 실제 관측 결과

| 묶음 | 수집원 | 공고 | 파일 확보 공고 | 정상 파일 / 바이트 | 테스트 |
|---|---|---:|---:|---:|---|
| FIRST | 부산 중구·서구·동구·영도·부산진·동래·남구 | 23 | 21 | 34 / 3,528,136 | 23통과, 종료0, 62초 |
| SECOND | 북구·해운대·사하·금정·강서·수영·사상·기장 | 22 | 18 | 31 / 5,935,624 | 22통과, 종료0, 47초 |

합계 **15수집원·39공고·65파일·9,463,760바이트**다. 제목 단계에서 중단한2공고 외43공고의 본문은 AVAILABLE이다. 관측 테스트45통과는 오류 기록 계약까지 포함하므로 모든 첨부 성공을 의미하지 않는다. 기존 보고서와 해시를 재확인해 대장에 반영했으며 반영 과정에서는 외부 요청을 반복하지 않았다.

| 공고 | 정상 확보 | 분리한 상태 |
|---|---|---|
| BUSANJIN-47592 / 50698 | 없음 | TITLE_NOT_ELIGIBLE_NOT_FETCHED, 요청0 |
| SAHA-43993 | 없음 | 공식 첨부 없음 확인, 예상0/발견0; 다운로드 성공 아님 |
| BSBUKGU-35116 | 없음 | ATTACHMENT_SIGNATURE_UNSUPPORTED |
| HAEUNDAE-53342 / 53828 | 없음 | ATTACHMENT_FORMAT_MISMATCH |
| SASANG-40426 | 정상2파일 | 미지원1항목은 다운로드하지 않음 |
| YEONGDO-36435 | 정상1파일 | 다른2파일 ATTACHMENT_HTTP_400 |

파일 일부 실패 때문에 같은 공고의 정상 파일을 버리지 않는다. 반대로 수신 바이트가 있다는 이유로 시그니처·형식 실패 파일을 성공으로 바꾸지 않는다. 확장자를 강제하거나 검증 조건을 완화하지 않았다. 이번 부분 실패5공고 모두 발견 경고는 없었다.

현재 지문 기준3개 적격 표본의 첨부 집합 Gate는1→11/223이다. 기존 구로에 부산 중구·서구·동구·부산진·동래·남구·사하·강서·수영·기장10곳이 추가됐다. 사하는 첨부 없음이 확인된 표본과 실제 다운로드 표본을 함께 검증했다. 이는 **첨부 수집 Gate**이며 추출 품질·정책 승인·상시 worker·운영 E2E Gate가 아니다.

## 대장·예산·검증

보고서45개는 `build/reports/attachment-regional-collection/`의 `BUSAN-FIRST-RECHECK-20261001`, `BUSAN-SECOND-RECHECK-20261001` 라벨이다. 보고서 경로·해시는 receipt index, 집계는 regional ledger의 `busanMultipleSampleCurrentCodeRecheckRun`에 기록했다.

기존556영수증을 수정하지 않고45개를 추가했다. 최신282공고 중45개만 갱신하고237개는 보존한다. 전체 상한390요청·1,206MiB, 본문 포함 예약199회·100,663,394바이트다. 예약량과 실제 wire 전송량은 다르다. 추가 공개 진단 요청은0이다. 모두 원본 정리true·운영쓰기0·추출/기대값/운영 E2E 검증false다.

인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA-256 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`를 유지했다.

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=BSJUNGGU,BSSEOGU,BSDONGGU,YEONGDO,BUSANJIN,DONGNAE,NAMGU' -PsanebCollectionReportLabel=BUSAN-FIRST-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=BSBUKGU,HAEUNDAE,SAHA,GEUMJEONG,BSGANGSEO,SUYEONG,SASANG,GIJANG' -PsanebCollectionReportLabel=BUSAN-SECOND-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

관측45통과, Node23통과, 601기록/282공고 재현 통과다. 코드 변경이 없어 Java 단위 전체·bootJar는 재실행하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다.

현재 코드 확인은160→175개, 확인·재검증은63→48개다. 48개는 과거에도 다운로드 미확인19개와 과거 성공했지만 현재 코드 확인이 없거나 실패한29개로 구분한다. 오류 수집원29개는 성공 수집원과 겹칠 수 있으며 별도의 분모로 더하지 않는다. 과거 포함204/223은 최신 상태의 전면 성공을 뜻하지 않는다.

운영 DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 갱신 대기, 과거 공개 CA·HTML 임시 자원 정리 차단은 별도 미해결이다. 이번 원본 정리 확인으로 과거 자원까지 정리됐다고 보고하지 않는다. 전체 goal은 미완료다.
