# 광주 동구·북구 및 대전 동구 첨부 수집

## 현재 단계 / Gate

성공 파일을 보존하고 실패를 별도 기록하여 다음 지역을 진행한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기1.0.16 추가 개선 보류를 유지한다.

- [x] 광주 동구·북구 및 대전 동구 프로필3개 연결.
- [x] 세 지역 본문 및 PDF2개·HWP3개, 총642,167byte 실제 다운로드.
- [x] 계약152건·Node23건·bootJar,175영수증/134공고 재현 검증.
- [x] 조사 원본10개226,130byte 및 관측 임시 원본 정리.
- [ ] 미등록129지역 연결, 등록 후 다운로드 미관측19지역 후속.
- [ ] 전국 목록→상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업에서 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 72 | **75/223 (약33.6%)** |
| 다운로드 미관측 지역 | 151 | **148** |
| 로컬 프로필 등록 지역 | 91 | **94** |
| 미등록 지역 | 132 | **129** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역94+기업마당1=95프로필, 카탈로그149참조/지역91개다. 신규 expectation은 null이고 기존 승인 기대값1개를 유지한다. 이전172영수증·131공고와 실행 metadata를 보존한다.

## 실제 결과 및 구현 계약

| 지역 / 표본 | 확보 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 광주 동구 LGS-000066 / 43109 | 512자 | 2 / 2, 발견 완료 | PDF379,775+71,928byte |
| 광주 북구 LGS-000069 / 56519 | 600자 | 2 / 2, 발견 완료 | HWP71,168+91,648byte |
| 대전 동구 LGS-000072 / 36677 | 195자 | 1 / 1, 발견 완료 | HWP27,648byte |

세 표본 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 변경하지 않은 DRAFT 제목 조합을 통과했고 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 텍스트 추출·구간 분석·DB/API 저장·운영 정책 승인은 이번에 검증하지 않았다.

광주 두 공고의 제목 중2025년은 연매출 기준 연도이며2026년 게시 공고다. 대전 동구 표본은2021년 청년일자리 사업 보관 공고다. 전송 구조 검증에 사용했으며 현재 모집으로 간주하거나 운영 공고를 생성하지 않았다.

- 광주 동구 `LOCAL_GWANGJU_DONGGU_GET_V1`: `eminwon.donggu.gwangju.kr`·SAFE_SAEOL_EMINWON_LIST 바인딩. 공식 form1, tstyle_view 제목, add_file의 첨부파일 라벨과 콘텐츠만 정규화한다.
- 광주 북구 `LOCAL_GWANGJU_BUKGU_GET_V1`: `eminwon.bukgu.gwangju.kr`·SAFE_SAEOL_EMINWON 바인딩. 공식 board_read 제목과 info_basic의 첨부파일 라벨 및 모든 dd를 보존한다. 알려진 정적 아이콘은 정확한 src와 다음 링크 파일명이 일치하고 이벤트 속성이 없을 때만 인정한다. 아이콘 URL을 다운로드하지 않는다.
- 광주 두 레이아웃은 별도 wrapper에서 기존 새올 GET 검증기로 전달한다. 알 수 없는 링크·태그는 발견 오류로 남기면서 정상 descriptor를 유지한다. 기존 공유 클래스와 hash는 변경하지 않았다.
- 대전 동구 `LOCAL_DAEJEON_DONGGU_GET_V1`: `eminwon.donggu.go.kr`·SAFE_SAEOL_EMINWON 바인딩. 기존 새올 td 첨부 라벨과 FileDown.jsp GET 프로토콜을 재사용한다.
- 고정 함수3인자만 파싱하고 스크립트를 실행하지 않는다. source/parser·원문 URL hash·HTTPS443·동일 요청 redirect·고정 다운로드 경로·10파일 상한·signature/MIME 검증을 유지한다. 파일 역할은 UNKNOWN이며 TLS/MIME 정책 완화는 없다.
- 고정 상세 표본의 수집 확인이다. 목록 수집기→상시 worker 자동 유입이나 운영 성공으로 확대 해석하지 않는다. migration·DB/API·화면·운영 정책·worker·HWP 추출기 변경 없음.

프로필 hash: 광주 동구 `68c59733a0f6f155423901083ba8c467767efb07cbfc9f5d70d5e3d7995c6a11`, 광주 북구 `000df0197d91c9911b9ac41dfd743ae6519e188db22978c655e098ce8e4fb642`, 대전 동구 `ae354b28b89d6906e6a1deb55e37cbb76ef9031517b1d5cbb62fd68b0084aea2`. 관측 producer class는 `5fff78a2744028aeafd5b1504407255b83749ece77ccd985fe7cefe092629fcb`다.

## 요청량과 정리

공식 조사 GET10회: 광주 동구3·북구3·대전 동구4. 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 대전 동구 소상공인 검색 무결과 후 지원 검색에서 표본을 선택했다.

실파일 관측14요청 예약 상한, 본문 예약 포함6,962,544byte다. 조사 포함24회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 관측당 최대6요청·23MiB 제한을 유지했다. 조사 원본10개226,130byte와 관측 임시 원본은 삭제하고 hash·비식별 metadata만 보존한다. 사용자 output 및 __pycache__, 기존 프로세스는 건드리지 않는다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*MetroSecondDownloadContractTest' --tests '*MetroSaeolFirstDownloadContractTest' --tests '*SeoulFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GWANGJU_DONGGU,GWANGJU_BUKGU,DAEJEON_DONGGU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. 계약152건 모두 통과, 실패·오류·생략0. 관측·bootJar 포함 첫 실행1분45초 BUILD SUCCESSFUL. Node23건 및175영수증/134공고 재현 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 계속 진행 중이다.
