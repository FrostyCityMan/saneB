# 통영·합천 첨부 연결과 응답 오류 분리

## 현재 단계 / Gate

정상 파일부터 수집하고 개별 오류는 분리하여 다음 지역을 진행한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기1.0.16 추가 개선 보류를 유지한다.

- [x] 통영·합천 공식 목록·검색·상세 조사, 공유 SCMS 처리기1개·프로필2개 추가.
- [x] 통영의 새올 원문과 홈페이지에서 동일 공고 번호·제목·첨부를 확인하고 기존 identity 연결 계약 추가.
- [x] 합천에서 관측한 구형 MIME·이중 UTF-8 파일명 헤더를 기존 엄격 검증 기능에 연결.
- [!] 통영 파일 형식 실패. 합천 최초 형식 오류·재관측 전송 타임아웃. 자동 수집 성공0지역.
- [!] 합천 기존 새올 identity 연계는 원문 타임아웃으로 미확인. 홈페이지 상세만 지원.
- [!] 경남도·의령·거창 목록 HTTP400은 미등록 후속으로 분리.
- [x] 최종 계약155건·Node23건·bootJar·관측 실행,166영수증/최신125공고 재현.
- [x] 임시 원본15개912,771byte 삭제, 기존 사용자 파일·이전 근거 보존.
- [ ] 미등록138지역 연결, 등록 미관측18지역 후속, 전국 목록→상시 worker 연계 검증.
- [ ] 본문 정제·첨부 추출·DB/API·정책 승인·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 67 | **67/223 (약30.0%)** |
| 다운로드 미관측 지역 | 156 | **156** |
| 로컬 프로필 등록 지역 | 83 | **85** |
| 미등록 지역 | 140 | **138** |
| 등록 후 다운로드 미관측 | 16 | **18** |
| 등록 지역 중 발견·파일 오류 근거 있음 | 20 | **22** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률을 전체 개발·운영 완료율로 표현하지 않는다. 지역85+기업마당1=86프로필, 카탈로그140참조/지역82개다. 신규 expectation은 null이고 기존 승인 기대값1개를 유지한다. 이전162영수증·123공고의 profile hash·기존 실행 metadata를 보존했다.

## 관측 결과

| 지역 / 공고 | 확보 본문 문자열 | 발견 | 최신 파일 결과 |
|---|---:|---|---|
| 통영 LGS-000226 / 49251 | 10,958자 | 1개, 미리보기 script 미해석 | 2,596byte 응답, `FILE_SIGNATURE / ATTACHMENT_SIGNATURE_UNSUPPORTED` |
| 합천 LGS-000241 / 44432 | 4,517자 | 2개, 미리보기 script 미해석 | 2개 모두 `FILE_DOWNLOAD / TRANSPORT_TIMEOUT` |

두 공고 모두 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 최신 결과를 집계하며 성공 파일은 없다. 통영 응답을 HWP로, 합천의 파일명만 보고 HWPX로 집계하지 않는다. 최초/최신 보고서4개는 각각 보존한다.

합천 최초 관측은 첫 파일87,214byte에서 `ATTACHMENT_CONTENT_TYPE_MISMATCH`, 두 번째159,232byte에서 `ATTACHMENT_FORMAT_MISMATCH`였다. 첫 파일의 진단 GET1회에서 실제 PK prefix, `application/x-msdownload;charset=UTF-8`, attachment 파일명 확장자 hwpx를 확인했다. 파일명 헤더는 엄격한 UTF-8 복원2회 후 한글·제어문자 없음이 확인됐다. 기존 검증기의 프로필별 구형 MIME·UTF-8 복원 기능을 **합천에만** 적용했다. synthetic 회귀 테스트는 통과했지만 수정 후 실제 자동 요청은 타임아웃이므로 실제 수집 성공으로 승격하지 않는다. 두 번째 파일의 형식 불일치도 그대로 보류한다.

진단 GET의 파일은 지역 수집 성공 근거로 쓰지 않는다. PK prefix만으로 HWPX 내부 구조·텍스트 추출이 검증된 것도 아니다. 이번 작업은 추출기를 실행하거나 개선하지 않았다.

두 제목은 변경하지 않은 DRAFT seed의 대상·지원 조합을 통과했다. 본문 중간 판정은 통영 `BODY_GROUP_B_MATCHED`, 합천 `BODY_GROUP_A_MATCHED`이며 모두 검수 대상이다. 본문 확보 문자열에 메뉴 등 주변 문구가 포함됐을 수 있어 정제 품질은 후속이다. 이 중간 판정이 첨부 진단을 차단하지 않으며 최종 정책 결과를 의미하지 않는다.

## URL 및 수집 흐름 계약

- `ScmsSaeolAttachmentDiscoveryProfile`은 기관별 공식 호스트·상세 경로·다운로드 경로를 시스템 bean으로 고정한다. `form#saeolGosiVO`의 제목과 `attach1`만 읽고 스크립트를 실행하지 않는다.
- 통영은 공식 홈페이지의 HTTPS `FileDownNewPbs.jsp` 직접 링크를 사용한다. 허용 파일3query·파일명·예상 형식을 대조하며 다른 호스트·경로·redirect는 차단한다.
- 통영의 HTTPS 새올 상세도 실제 조회해 홈페이지 공고49251과 제목·첨부가 일치함을 확인했다. 기존 HTTP/HTTPS 새올 source URL은 원래 hash·identity를 보존하고 동일 공고 번호의 홈페이지 HTTPS 상세로 변환한다. HTTP 원문을 직접 요청하지 않는다. 변형 파라미터·userinfo·fragment·다른 호스트 거부를 테스트했다.
- 합천은 공식 HTTPS `/DownloadEx.do` 프록시만 요청한다. 내부 `eminwon.hc.go.kr`의 `FileDown.jsp`·파일3query·표시명 일치를 검증하고 내부 HTTP를 직접 호출하지 않는다.
- 실제 목록 수집기는 DB의 SAFE_TEMPLATE 프로필을 통해 새올 source URL을 생성할 수 있다(`LocalGovernmentNoticeCollector.selectResolvedLink`, V38/V61/V62). **합천의 그 URL과 홈페이지의 동등성은 타임아웃으로 미확인**이므로 자동 변환하지 않는다. 현재 합천 프로필은 홈페이지 상세 URL만 처리하며, 기존 새올 URL은 `PROFILE_REQUIRED`로 남는다. 코드 등록은 모든 기존 source URL이나 목록→worker 자동 유입 검증을 뜻하지 않는다.
- source/parser/URL hash, HTTPS443, 동일 요청,10파일 상한, 미지원 형식 분리, 역할 UNKNOWN, 형식·disposition 대조를 유지한다. 전역 MIME 허용이나 TLS 완화는 없다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기는 변경하지 않았다.

## 요청량 / 원본 정리

목록·검색·상세 조사 GET13회: 통영6, 합천4(새올 상세 타임아웃1 포함), 경남도223·의령232·거창240 각1. 마지막3지역은 HTTP400이며 기관 전체 장애라고 단정하지 않는다. 조사당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지다.

합천 첫 파일 진단 GET1회는20초/2MiB 상한, 실제87,214byte였다. 관측은 최초 통영4+합천5, 수정 후 각4+5로 예약18회, 조사·진단 포함32회다. 본문 예약 상한을 포함하므로 실제 HTTP 횟수와 동일하다고 표현하지 않는다. 관측당6요청·23MiB, 누적 예약byte9,204,808이다. 더 이상 반복하지 않고 다른 지역을 진행한다.

조사·진단 원본15개 총912,771byte와 관측 임시 원본을 삭제했다. 복구 사본 없이 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하며 기존 사용자 프로세스·`output/`·`scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongnamThirdDownloadContractTest' --tests '*GyeongnamSecondDownloadContractTest' --tests '*GyeongnamFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=TONGYEONG,HAPCHEON' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 `target-inventory-20260928-receipt.txt`를 사용했다. 최초 계약154건/1분45초, 수정 후155건/2분20초에 각각 `BUILD SUCCESSFUL`, 실패·오류·skip0이다. Node23/23,166영수증/125공고 전체 재현 통과. 수집 전용 실행의 빌드 성공은 개별 첨부 수집 성공이나 운영 완료를 의미하지 않는다.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 연결 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다. 다음은 제주권 등 미연결 지역이고, 경남 오류의 완전한 해결을 다음 지역 착수 조건으로 삼지 않는다.
