# 고성·창원 첨부 연결 및 경남 차단 응답 분리

## 현재 단계 / Gate

정상 파일부터 수집하고 발견·전송·형식·추출 실패는 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 고성·창원 공식 목록·검색·상세·첨부 영역 조사 및 시스템 프로필2개 추가.
- [x] 고성 HWP1개107,520byte 실제 수집·형식 검증.
- [x] 창원 중첩 URL의 한글 파일명·공백 처리 수정 및 회귀 테스트.
- [!] 창원 첨부 발견 성공 후 응답 형식 검증 실패. 정상 다운로드로 집계하지 않는다.
- [!] 남해·하동·산청·거제 목록 HTTP400 `Request Blocked`. 미등록 상태로 후속 관리한다.
- [x] 최종 계약148건·Node23건·bootJar·실파일 관측 실행,162영수증/최신123공고 재현.
- [x] 조사 원본13개1,073,707byte 정리, 이전 근거·사용자 파일 보존.
- [ ] 미등록140지역 연결 및 등록 미관측16지역 후속.
- [ ] 첨부 추출·본문 정제·상시 worker·DB/API·정책 승인·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 상태를 다시 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 66 | **67/223 (약30.0%)** |
| 다운로드 미관측 지역 | 157 | **156** |
| 로컬 프로필 등록 지역 | 81 | **83** |
| 미등록 지역 | 142 | **140** |
| 등록 후 다운로드 미관측 | 15 | **16** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 18 | **20** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약30.0%는 파일 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 지역83+기업마당1=84프로필, 카탈로그138참조/지역80개다. 신규 참조2개는 expectation=null이며 기존 승인 기대값1개는 그대로다. 이전158영수증·121공고의 profile hash·기존 실행 metadata를 보존했다.

## 실제 관측

| 지역 / 고정 공고 | 본문 확보 문자열 | 첨부 발견 | 파일 결과 |
|---|---:|---|---|
| 고성 LGS-000235 / 5733464 | 1,964자 | 정상 링크1개, 미리보기 등 미해석 항목 분리 | HWP107,520byte 성공 |
| 창원 LGS-000224 / 211423 | 8,104자 | 수정 후1개·발견 완료 | 응답99,491byte, `FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH` |

고성은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 정상 파일1개를 확보해도 미해석 미리보기·스크립트가 있어 전체 첨부 발견 완료로 표현하지 않는다. 최초와 수정 후 재관측의 같은 파일은 중복 집계하지 않는다.

창원은 최초에 중첩 URL의 공백 때문에 descriptor0개였다. 실제 공식 링크의 인코딩을 기준으로 URI 해석 시 공백을 보정했으며, 요청하는 바깥 HTTPS 프록시 URL은 변경하지 않는다. 한글·공백·괄호 파일명의 회귀 테스트를 추가했다. 재관측에서는 발견1개·완료로 복구됐으나 응답 content-type 검증에 실패했다. **파일 확장자나 수신 byte만으로 HWPX 수집 성공을 인정하지 않는다.** 실제 문서 손상·서버 장애·권한 문제라고 단정하지 않으며, 추가 분석은 다음 지역 확대를 막지 않는다.

두 제목은 변경하지 않은 DRAFT seed의 대상·지원 조합을 통과했다. 본문 중간 판정은 `REVIEW_REQUIRED/BODY_GROUP_A_MATCHED`다. 이는 정제 품질이나 최종 정책 판정을 증명하지 않는다. 고성의 실제 게시글 본문은 짧은 안내인데1,964자, 창원은8,104자가 확보되어 메뉴 등 주변 텍스트 혼입 점검을 후속으로 남긴다. 첨부 텍스트 추출은 실행하지 않았다.

최초 결과는 `GOSEONG-5733464-INITIAL.json`, `CHANGWON-211423-INITIAL.json`에 보존했고, 수정 후 같은 공고의 최신 보고서를 별도로 저장했다. 최신 관측만 지역 가용성 집계에 사용한다. 고정 표본 성공은 목록 자동 유입·상시 worker·운영 완료를 의미하지 않는다.

## 구현 범위

- 공유 처리기 `GyeongnamBoardAttachmentDiscoveryProfile`와 시스템 bean2개를 추가한다. 기존 처리기 실행 코드는 수정하지 않았다.
- 고성: `bdvTitWrap > bdvTit`, `bdvFileWrap`의 첨부파일 레이블·`bdvFileBox`만 사용한다. 공식 새올 HTTPS `FileDown.jsp`의 파일3query와 표시 파일명을 대조한다.
- 창원: `form#saeolGosiVO` 안의 제목·`attach1`만 사용한다. 요청은 공식 HTTPS `/cwportal/DownloadEx.do`, 내부 URL은 `eminwon.changwon.go.kr`의 `FileDown.jsp`와 파일3query로 제한한다. 내부 HTTP 주소는 직접 호출하지 않는다.
- source/parser/URL hash, HTTPS443, 공식 호스트·경로, 동일 요청 redirect, 중복 충돌,10파일 상한, 미지원 형식 분리, 역할 UNKNOWN을 유지한다. 미리보기 스크립트는 실행하지 않는다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기는 변경하지 않았다.

## 차단 응답과 요청 예산

남해 LGS-000236, 하동237, 산청238, 거제230의 공식 목록에 각 GET1회를 실행했다. 모두 HTTP400/166byte `Request Blocked`였다. 인증 우회·TLS 완화·반복 호출 없이 미등록·미관측으로 남긴다. 이4지역은 등록 지역 오류 수20에 포함하지 않는다.

조사12회(GET만): 고성5회(이동 경로 확인 포함), 창원3회, 나머지4지역 각1회. 조사당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 고성 이동 응답의 세션 식별자는 요청 URL이나 저장 근거에 포함하지 않았다.

관측은 최초 고성4+창원3, 수정 후 각4회로 예약15회, 조사 포함27회다. 본문 예약 상한을 포함하므로 실제 HTTP 횟수와 동일하다고 표현하지 않는다. 관측 실행당6요청·23MiB 이내, 누적 예약byte9,414,656이다.

조사 원본13개 총1,073,707byte와 관측 임시 원본은 삭제했다. 복구 사본 없이 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하며 기존 사용자 프로세스·`output/`·`scripts/qa/__pycache__/`는 보존한다.

## 실행 명령 / 검증 결과

```powershell
.\gradlew.bat :test --tests '*GyeongnamSecondDownloadContractTest' --tests '*GyeongnamFirstDownloadContractTest' --tests '*GyeongbukFifthDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GOSEONG,CHANGWON' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory 검증은 로컬에 보관된 `target-inventory-20260928-receipt.txt`를 지정했으며 운영 DB를 조회하지 않았다. 최초 계약147건/1분44초, 수정 후148건/1분33초에 각각 `BUILD SUCCESSFUL`, 실패·오류·skip0이다. Node23/23,162영수증/123공고 재현 성공. 근거 갱신 중 지역 행의 잘못된 배치를 재현 검증기가 검출했고, 해당 행을 수정한 후 전체 재현을 다시 통과했다. 지역·공고 수를 임의 보정하거나 검증기를 완화하지 않았다.

수집 전용 실행의 `BUILD SUCCESSFUL`은 개별 파일 실패가 없다는 뜻이 아니다. 고성 발견 부분 오류와 창원 응답 형식 실패를 위와 같이 보존한다. 전체 테스트·Linux 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 미실행이다. 브라우저는 현재 지역 연결 작업에 명시적 요청이 없어 정책상 생략했다. 이번 커밋은 `[skip deploy]`이며 전체 장기 Goal은 진행 중이다.

다음은 경남도·통영·의령·거창·합천 등 미연결 공식 게시판이다. 이미 차단된 지역의 오류 해결을 선행 조건으로 삼지 않는다.
