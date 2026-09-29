# 청도·칠곡·봉화 첨부 연결과 부분 수집 결과

## 현재 단계 / Gate

정상 파일 수집·개별 오류 분리 방식으로 전 지역 첨부 연결을 확대한다. 한 지역의 오류 해결이나 3표본 확보는 다음 지역 착수 조건이 아니다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 청도·칠곡·봉화 공식 목록·검색·상세·첨부 방식 확인.
- [x] 기존 포털 처리기 재사용, 시스템 프로필3개·미승인 참조3개 추가.
- [x] 청도 HWPX2개·칠곡 HWPX1개·봉화 HWPX/HWP2개 다운로드 검증.
- [x] 봉화 미확인 미리보기·JPG 미지원과 정상 파일 보존을 함께 확인.
- [x] 계약163건·Node23건·bootJar·실제 관측3공고 실행 완료.
- [x] 150영수증/최신114공고 재현, 기존 근거 보존, 임시 원본 정리.
- [ ] 경북 잔여 지역과 전국 미등록149지역 연결.
- [ ] 본문 정제·첨부 추출·상시 worker·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업은 현재 운영 설정 재조회나 운영 DB·정책·worker 변경을 포함하지 않는다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 58 | **61/223 (약27.4%)** |
| 다운로드 미관측 지역 | 165 | **162** |
| 로컬 프로필 등록 지역 | 71 | **74** |
| 미등록 지역 | 152 | **149** |
| 등록 후 다운로드 미관측 | 13 | **13** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 13 | **14** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약27.4%는 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 지역74+기업마당1=75프로필, 카탈로그129참조/지역71개다. 신규 기대값은 승인하지 않았으며 기존 expectation1개를 유지한다. 새 엔진은 추가하지 않고 `GyeongbukPortalAttachmentDiscoveryProfile`에 기관별 설정3개만 추가했다. 기존 처리기 실행 코드와 기존 profile hash는 바뀌지 않았다.

## 실제 수집 결과

| 지역 / 공고 | 본문 확보 길이 | 발견 | 실제 다운로드 |
|---|---:|---|---|
| 청도 LGS-000215 / 24118 | 1,602자 | 2개·완료 | HWPX66,923 + 64,193byte |
| 칠곡 LGS-000218 / 34056 | 1,897자 | 1개·완료 | HWPX97,762byte |
| 봉화 LGS-000220 / 32956 | 1,334자 | 3개·부분 오류 | HWPX1,482,436 + HWP168,448byte, JPG 미지원 |

검증된 파일5개 총 **1,879,762byte**다. 세 공고는 임시 DB DRAFT seed 제목 조합을 통과했고 상세 제목 식별을 완료했다. 청도·칠곡은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 봉화는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다.

청도 본문 중간 결과는 `REVIEW_REQUIRED/BODY_GROUP_A_MATCHED`, 칠곡은 `REVIEW_REQUIRED/BODY_GROUP_B_MATCHED`, 봉화는 `ACCEPTED/TARGET_SUPPORT_CONFIRMED`다. **본문 정제 품질·전체 본문 확보·첨부 텍스트 추출·관리자 승인·운영 활성화는 검증하지 않았다.** 본문 길이와 중간 분류만으로 정책 정확성이나 최종 후보 승인을 단정하지 않는다. 과거 표본의 파일 수집 검증은 현재 모집 중인 공고라는 뜻이 아니다.

### 봉화 부분 오류의 처리

봉화 공식 첨부 영역은 다운로드 옆에 `fn_egov_gosi_preview_external` 미리보기 링크를 사용한다. 기존 처리기는 해당 변형을 인식하지 않으므로 `ATTACHMENT_LINK_UNRESOLVED`, 발견 `FAILED/complete=false`로 기록한다. 미리보기는 호출하지 않는다. 반면 별도로 검증된 `goDownload` 직접 파일3개는 보존되어 HWPX·HWP2개를 정상 다운로드하고 JPG1개는 미지원으로 분리한다.

미리보기 변형까지 해석하기 위해 기존 공통 처리기를 변경하거나 다른 지역의 근거를 무효화하지 않았다. 이 오류는 후속 보완 항목이며 정상 파일 수집 또는 다음 지역 진행을 차단하지 않는다. 발견 오류가 남아 있으므로 정상 전체 집합·엄격 Gate 통과로 보고하지 않는다.

## 구현과 확인 범위

- 세 기관의 공식 `form#detailForm`과 `bod_view`/`view_file` 영역을 사용한다. 청도·봉화는 `div.subject`, 칠곡은 `h4` 제목 구조다.
- 다운로드는 각 기관의 `eminwon` HTTPS 호스트와 고정 `/emwp/jsp/ofr/FileDown.jsp`, 공개 파일3query로 제한한다. 원격 script는 읽기만 하며 실행하지 않는다.
- 세 기관의 대표 고시 목록 주소는 공식 포털 목록으로302 이동했다. 청도·봉화의 공개 일회성 query는 조사 중에만 사용하고 카탈로그·근거 대장에는 저장하지 않았다.
- 칠곡 HEAD는403이었으나 GET은302/200으로 정상 연결됐다. HEAD 결과만으로 GET 수집을 불가능하다고 판정하지 않았다.
- 목록은 공식 검색 폼으로 POST 조회했다. 상세는 공식 data-action 주소에 GET 요청해 실제 제목과 첨부를 확인했다. 상시 목록 스케줄러부터의 유입 E2E는 검증하지 않았다.
- 제목 검증·경로/호스트 제한·동일 요청 결합·파일명/형식 검증·중복 충돌·10파일 상한은 유지했다. 문서 역할 UNKNOWN이며 파일명으로 최종 정책 판정을 승인하지 않는다.
- migration·DB/API·화면·운영 정책·worker 설정·추출기는 변경하지 않았다.

## 요청 예산과 정리

조사17회(GET10·POST4·HEAD3), 관측 예약14회다. 합계31회는 본문 예약 상한을 포함하므로 실제 HTTP 횟수와 동일하다고 표현하지 않는다.

| 지역 | 조사 | 관측 예약 | 이번 합계 |
|---|---:|---:|---:|
| 청도 | 5 | 5 | 10 |
| 칠곡 | 7 | 4 | 11 |
| 봉화 | 5 | 5 | 10 |

조사당15초/연결7초/최대1MiB/자동 redirect0/TLS 검증 유지. 관측당 최대6요청·23MiB, 예약 byte 합계9,178,834. 임시 원본18개 총3,574,349byte와 관측 임시 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하며 사용자 프로세스와 미추적 `output/`, `scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukFourthDownloadContractTest' --tests '*GyeongbukThirdDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHEONGDO,CHILGOK,BONGHWA' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

계약163건 실패·오류·skip0, bootJar·실제 관측3공고 실행 완료(1분36초). Node23/23. 150영수증/114공고 재현 통과. 이전147영수증·기존 실행 metadata·기존 profile hash를 보존했다.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 미실행이다. 브라우저는 현재 지역 연결 작업에서 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
