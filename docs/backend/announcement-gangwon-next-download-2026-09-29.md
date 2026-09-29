# 동해·정선·강원 고성·양양 첨부 연결 및 실파일 수집

## 현재 단계 / Gate

성공 파일을 보존하고 수집·파싱 실패는 별도로 관리한다. 모든 오류 해결이나3표본 확보를 다른 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지 및 HWP 추출기1.0.16 개선 보류를 유지한다.

- [x] 동해·정선·강원 고성 GET 및 양양 POST 프로필4개 연결.
- [x] 4지역 HWP4개·HWPX2개, 총2,152,605byte 다운로드.
- [x] 정선37876 제목 조합 미충족 표본은 관측 단계 요청0회로 중단·별도 기록.
- [!] 정선36195 상세HTTP400·0byte는 해당 공고 오류로 별도 후속.
- [x] 계약162건·Node23건·bootJar,183영수증/141공고 재현.
- [x] 조사 원본14개225,837byte 및 관측 임시 원본 정리.
- [ ] 미등록123지역 연결, 등록 후 다운로드 미관측19지역 후속.
- [ ] 전국 목록→상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 77 | **81/223 (약36.3%)** |
| 다운로드 미관측 지역 | 146 | **142** |
| 로컬 프로필 등록 지역 | 96 | **100** |
| 미등록 지역 | 127 | **123** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 최신 등록 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 확인율은 전체 개발·운영 완료율이 아니다. 지역100+기업마당1=101프로필, 카탈로그156참조/지역97개다. 신규 expectation은 null, 기존 승인 기대값1개는 유지한다. 이전178영수증·136공고와 실행 metadata를 보존한다. 정선36195 조사 오류는 위 최신 표본 오류23지역 집계와 별도로 기록한다.

## 실제 결과

| 지역 / 표본 | 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 동해 LGS-000120 / 36513 | 130자 | 1 / 1 | HWP90,112byte |
| 정선 LGS-000128 / 34952 | 348자 | 2 / 2 | HWPX102,946+HWP1,720,320byte |
| 강원 고성 LGS-000133 / 32775 | 444자 | 2 / 2 | HWP51,200+117,248byte |
| 양양 LGS-000134 / 37521 | 203자 | 1 / 1 | HWPX70,779byte |
| 정선 LGS-000128 / 37876 | 미실행 | 미실행 | 제목 단계 중단·관측 요청0회 |

다운로드4표본은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 추출·구간 분석·DB/API 저장·최종 정책 승인은 이번에 검증하지 않았다. 동해와 정선34952는2025년 보관 공고, 강원 고성32775는2026년1월 재공고다. 현재 모집 여부를 증명하거나 운영 공고로 생성한 결과가 아니다.

정선37876의 제목은 `2026년도 정선군 소상공인 경영안정 자금 시행 공고`다. 현재 로컬 DRAFT 규칙은 `COMBINATION_NOT_MATCHED`로 판정한다. 제목에서 지원유형 표현을 놓칠 가능성을 후속 정책 검토 항목으로 남긴다. 운영 활성 규칙도 동일한 판정이라고 단정하지 않는다. 규칙·seed를 변경하지 않았으며 해당 표본을 삭제하거나 성공 수에 넣지 않았다.

37876 상세는 제목 계약 검사 전 구조 조사에서 한 차례 조회했다. 이후 관측 실행에서는 `TITLE_NOT_ELIGIBLE_NOT_FETCHED`, 본문·첨부 요청0회·파일0개를 검증했다. 조사 원문은 정리했고 운영 저장은 없다. 대체 적격 후보36195는 상세HTTP400이었으며 정상 응답한34952로 동일 기관의 전송 구조를 검증했다. 실패를 성공으로 치환하지 않고 조사 결과와 제목 중단 표본을 함께 보존한다.

## 구현 계약

- 동해 `LOCAL_DONGHAE_GET_V1`: `eminwon.dh.go.kr`·SAFE_SAEOL_EMINWON_CELL. 공식 form/post와98% 표 구조를 확인한 후 기존 새올 form1 검증기로 전달한다. 원문 identity는 변경하지 않는다.
- 정선 `LOCAL_JEONGSEON_GET_V1`: `eminwon.jeongseon.go.kr`·SAFE_SAEOL_EMINWON. 공식 skinTb.eminwon 본문 칸에서 직접 텍스트 노드 `첨부파일`이 단 한 번 있고 앞뒤 br이 확인될 때 그 이후 모든 노드를 전달한다. 본문 안의 다른 링크는 첨부로 해석하지 않는다. 표식 누락·중복·중첩은 발견 오류다.
- 강원 고성 `LOCAL_GW_GOSEONG_GET_V1`: `eminwon.gwgs.go.kr`·SAFE_SAEOL_EMINWON. 공식 tb_style1의 첨부파일1·2 등 번호 행을 모두 수집한다. 번호 누락·구조 불일치는 오류로 남기고 정상 descriptor는 보존한다.
- 위3지역은 별도 경계 wrapper가 기존 새올 GET 검증기를 재사용한다. 기존 공유 코드·hash를 변경하지 않았다.
- 양양 `LOCAL_YANGYANG_POST_V1`: `eminwon.yangyang.go.kr`·SAFE_SAEOL_EMINWON. 기존 대구 북구 POST 구현을 바탕으로 별도 클래스를 추가했다. 공식 skinTb 첨부 행과 nnn hidden3필드 폼을 검증하고 FileDownNew.jsp로 불투명 인자를 전달한다. isHome 등 추가 필드, 다른 요청 redirect를 거부한다. 암호화 인자는 해독·영구 저장하지 않는다.
- 고정 함수3인자 파싱만 수행하며 스크립트는 실행하지 않는다. 고정 HTTPS443 출처·source/parser·원문 URL hash·10파일 상한·signature/MIME 검증·역할 UNKNOWN을 유지한다. TLS/MIME 정책을 완화하지 않았다.
- 고정 상세 표본의 수집 확인이다. 목록 수집기→상시 worker 자동 유입이나 운영 성공으로 확대 해석하지 않는다. migration·DB/API·화면·운영 정책·worker·추출기 변경 없음.

Profile hash: 동해 `6e67a9d93366993034148948aefb14e651f6a8acc486aa590fb98254befc46f7`, 정선 `3a7d1251818a585ecf39f9a731ddd021decd2040303faf87d55b0a314aba82d0`, 강원 고성 `bbbece4d6f5e6cf7002f46b354f1f956c00bba0131f3b32f60c18066b7505656`, 양양 `7eb82fe8fae8813fc1be7c54cfccd3483dd16fb3acbac6b23b615eb51efce7fd`. Producer class는 `1a2f016a6bd56f4cef153186576fc55fa3c7eb9cba99ee77ea081c5113e0b532`다.

## 요청량 / 정리

공식 조사 GET14회: 동해3·정선5·강원 고성3·양양3. 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 관측 예약 상한18회·본문 예약 포함10,570,901byte. 조사 포함32회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 각 관측 최대6요청·23MiB다.

조사 원본14개225,837byte를 길이·SHA256 대조 후 삭제했다. 정선36195의0byte 응답 파일도 포함한다. 관측 임시 원본은 정리했고 비식별 hash·결과 metadata만 보존한다. 사용자 output·__pycache__ 및 기존 프로세스는 보존한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GangwonNextDownloadContractTest' --tests '*UlsanFirstDownloadContractTest' --tests '*MetroSecondDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=DONGHAE,JEONGSEON,GW_GOSEONG,YANGYANG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. 첫162건 중1건이 정선37876 제목을 적격이라고 가정한 검사에서 실패(1분32초)하여 외부 관측 전에 중단됐다. 제목 규칙은 유지하고 해당 표본을 명시적 음성 사례로 보존한 뒤 적격 표본을 추가했다. 최종162건 모두 통과·실패/오류/생략0, 실파일 관측·bootJar 포함2분3초 BUILD SUCCESSFUL. Node23건 및183영수증/141공고 재현 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
