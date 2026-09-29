# 울산 중구·북구 첨부 연결 및 실파일 수집

## 현재 단계 / Gate

성공 파일을 보존하고 발견·다운로드·파싱 실패는 별도 기록한다. 모든 오류 해결이나3표본 확보를 다른 지역 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기1.0.16 개선 보류를 유지한다.

- [x] 중구 고정4필드 POST 및 북구 평문 GET 프로필2개 연결.
- [x] 중구 HWP147,456byte, 북구 HWPX64,750byte 및 본문 확보.
- [!] 남구 공식 목록15초 타임아웃·0byte. 미등록 후속으로 분리.
- [!] 중구36951 상세HTTP400·0byte. 해당 공고 오류로 분리하며 기관 전체 장애로 단정하지 않음.
- [x] 계약152건·Node23건·bootJar,178영수증/136공고 재현.
- [x] 조사 원본9개155,463byte 및 관측 임시 원본 정리.
- [ ] 미등록127지역 연결, 등록 후 다운로드 미관측19지역 후속.
- [ ] 전국 목록→상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 75 | **77/223 (약34.5%)** |
| 다운로드 미관측 지역 | 148 | **146** |
| 로컬 프로필 등록 지역 | 94 | **96** |
| 미등록 지역 | 129 | **127** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 최신 등록 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역96+기업마당1=97프로필, 카탈로그151참조/지역93개다. 신규 expectation은 null, 기존 승인 기대값1개는 유지한다. 기존175영수증·134공고와 실행 metadata를 보존한다. 남구 목록 오류와 중구36951 상세 오류는 조사 metadata에 별도로 보존하며 위 최신 표본 오류23지역 집계에 합산되지 않는다.

## 실제 결과 / 계약

| 지역 / 표본 | 확보 본문 | 발견 / 다운로드 | 결과 |
|---|---:|---|---|
| 울산 중구 LGS-000078 / 21392 | 238자 | 최종1 / 1 | HWP147,456byte |
| 울산 북구 LGS-000081 / 44394 | 496자 | 1 / 1 | HWPX64,750byte |

합계2개212,206byte. 두 최종 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. HWP/HWPX 텍스트 추출·구간 분석·DB/API 저장·운영 정책 승인은 이번에 검증하지 않았다.

중구21392는 **2019년 보관 공고**의 전송 구조 검증이다. 현재 모집으로 간주하지 않으며 운영 공고를 생성하지 않았다. 북구는 기본 검색에서2015년17056이 반환됐으나, 공식 폼의 전체 기간 조건 `list_gubun=A`로2026년 하반기44394를 선택했다.17056 파일은 요청하지 않았다.

- 중구 `LOCAL_ULSAN_JUNGGU_POST_V1`: `eminwon.junggu.ulsan.kr`·SAFE_SAEOL_EMINWON 바인딩. 기존 대구 북구 프로토콜을 바탕으로 별도 클래스를 추가했다. `form1`의 public_view·중첩 td 첨부 라벨, `nnn`의 hidden4개를 검증한다. FileDownNew.jsp POST는 불투명3인자와 고정 `isHome=Y`만 허용한다. 고정값 변경·필드 누락/추가·다른 요청 redirect는 거부한다. 암호화 인자 원문은 로그·문서·카탈로그에 저장하지 않는다.
- 최초 중구 관측은 고정 isHome 필드 미반영으로 `ATTACHMENT_DOWNLOAD_FORM_CHANGED`였으며 다운로드하지 않았다. 최초 실패 보고서와 당시 profile hash를 보존했다. 공식 폼에 맞춘 수정·회귀 테스트 후 중구만 재관측하여 성공했다. 북구 성공 파일은 재요청하지 않았다.
- 북구 `LOCAL_ULSAN_BUKGU_GET_V1`: `eminwon.bukgu.ulsan.kr`·SAFE_SAEOL_EMINWON 바인딩. 기존 새올 GET의 파일명·경로·조합 검증을 재사용하면서 실측 FileDownNew.jsp 경로로만 매핑한다. 이 기관에서는 이전 FileDown.jsp·POST·인코딩 경로·다른 요청 redirect를 허용하지 않는다.
- 스크립트를 실행하지 않고 고정 함수3인자를 읽는다. 고정 HTTPS443 출처·source/parser·원문 URL hash·파일10개 상한·signature/MIME 검증·역할 UNKNOWN을 유지한다. TLS/MIME 완화는 없다. 기존 공유 프로필 클래스와 hash는 변경하지 않았다.
- 고정 상세 표본 관측이다. 목록 수집기→상시 worker 자동 유입이나 운영 성공으로 확대 해석하지 않는다. migration·DB/API·화면·운영 정책·worker·추출기 변경 없음.

최종 profile hash: 중구 `f94bb8b19b3bd6533e49e9e47c1b0eb7ff9ee3b62f0883d12bdcb81816f3597a`, 북구 `1bda70ba677e0fd94c23281085745fd01dce1da1fe63ce43a5fafbadfca886d1`. 중구 최초 실패 hash는 `f7061922d190ffc718f198a74342779fb3f549a0c4931b1bde21e0b06f1d7886`, producer class는 `985d8721c84ac2debcfebc7a45cc7d26ef1799f2f757b45cca70ef8a12afb3a9`다.

## 요청량 / 정리

공식 조사 GET10회: 중구4·남구1·북구5. 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 관측 예약 상한은 최초 중구3+북구4+수정 중구4=11회, 본문 예약 포함6,530,955byte. 조사 포함21회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 관측당 최대6요청·23MiB를 유지했다.

조사 원본9개155,463byte는 길이·SHA256 대조 후 삭제했다. 중구 첫 HTTP400의0byte 파일도 포함한다. 관측 임시 원본도 정리했고 비식별 hash·결과 metadata만 보존한다. 사용자 output·__pycache__와 기존 프로세스는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*UlsanFirstDownloadContractTest' --tests '*MetroSecondDownloadContractTest' --tests '*MetroSaeolFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=ULSAN_JUNGGU,ULSAN_BUKGU' -PsanebCollectionWindowsTrust=true --no-daemon
# 수정 후 같은 검증 명령에서 관측 그룹만 ULSAN_JUNGGU로 제한하여 재실행
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. 첫 계약152건 통과·bootJar 성공(1분46초)이었지만 중구 발견은 실패했다. 수정 후152건 모두 통과·실패/오류/생략0, 중구 관측과 bootJar 포함1분39초 BUILD SUCCESSFUL. Node23건 통과. 영수증 이관 중 최초 실패 파일명의 소문자 접미사가 경로 규칙에 맞지 않아 재현 검사가 중단됐으며, 대문자 파일명으로 정정한 후178영수증/136공고 재현을 통과했다. 검증 규칙은 완화하지 않았다.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 계속 진행 중이다.
