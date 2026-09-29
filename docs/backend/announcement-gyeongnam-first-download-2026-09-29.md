# 고령 첨부 수집·진주 연결 및 경남 첫 조사

## 현재 단계 / Gate

정상 파일은 수집하고 개별 발견·다운로드·파싱 실패를 분리하여 다음 지역으로 진행한다. 오류 해결이나 3표본 확보를 다음 지역의 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 경북 잔여 고령과 경남 진주 공식 목록·검색·상세 구조 확인.
- [x] 공유 처리기1개·시스템 프로필2개·미승인 참조2개 추가.
- [x] 고령 HWP3개 실제 다운로드·파일 형식 검증.
- [!] 진주 상세 발견 요청 타임아웃2회. 최초·재시도 근거 보존, 다운로드 미확인.
- [!] 사천·김해·창녕 목록 HTTP400. 이번에는 미등록으로 남기고 다음 지역 진행.
- [x] 계약150건·Node23건, bootJar 작업 완료, 158영수증/최신121공고 재현.
- [x] 임시 원본 정리, 기존 근거·사용자 파일 보존.
- [ ] 경남 잔여와 전국 미등록142지역 연결, 등록 미관측15지역 후속.
- [ ] 본문 정제·첨부 추출·상시 worker·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업은 현재 운영 설정 재조회나 운영 DB·정책·worker 변경을 포함하지 않는다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 65 | **66/223 (약29.6%)** |
| 다운로드 미관측 지역 | 158 | **157** |
| 로컬 프로필 등록 지역 | 79 | **81** |
| 미등록 지역 | 144 | **142** |
| 등록 후 다운로드 미관측 | 14 | **15** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 17 | **18** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약29.6%는 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 지역81+기업마당1=82프로필, 카탈로그136참조/지역78개다. 신규 expectation은 null이며 기존 승인 기대값1개를 유지한다. 기존 처리기 실행 코드·119공고의 profile hash·이전155영수증·이전 실행 metadata를 보존했다.

## 실제 수집 / 실패

| 지역 / 공고 | 본문 확보 문자열 | 첨부 발견 | 실제 파일 |
|---|---:|---|---|
| 고령 LGS-000216 / 42015 | 4,935자 | 3개·완료 | HWP133,632 + 81,920 + 1,507,328byte |
| 진주 LGS-000225 / 64420 | 12,471자 | 타임아웃, 식별 미완료 | 발견·다운로드0개, 첨부 없음으로 간주하지 않음 |

고령은 총3개 **1,722,880byte**, `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 진주는 `DETAIL_DISCOVERY/TRANSPORT_TIMEOUT`, `INCOMPLETE`이며 `JINJU-64420-INITIAL.json`과 최종 재시도 보고서를 모두 보존한다. 코드 계약 테스트와 사전 curl 상세 조회는 성공했지만, 실제 관측 실행기의 상세 요청은2회 실패했다. 진주 프로필 등록을 실제 수집 성공으로 표현하지 않는다.

두 제목은 DRAFT seed 조합을 통과했다. 본문 중간 판정은 `REVIEW_REQUIRED/BODY_GROUP_B_MATCHED`지만 **본문 정제 정확성·전체 본문 확보·최종 정책 판정을 검증한 것은 아니다.** 진주의 실제 상세 본문은 짧은 문단인데 확보 문자열이12,471자여서 주변 텍스트 혼입을 점검해야 한다. 고령도4,935자의 본문 경계 정제를 후속으로 남긴다. 첨부 텍스트 추출은 실행하지 않았다. 고정 표본 수집은 현재 모집 중임을 의미하지 않는다.

## 구현 계약

- 고령: `table.boardView_table`의 제목과 `첨부` 레이블 셀만 읽는다. 동일 `IDX_FI`·`BRD_ID=1023`인 `/front/viewFile.do` 파일명 링크와 `/front/downFile.do` 다운로드 링크를 결합한다. 파일명 뒤 형식·용량 표시만 제거하며 미리보기는 호출하지 않는다.
- 고령 운영 seed의 HTTP 원문 identity는 보존한다. 조사에서 동일 HTTPS 목록·상세 접근을 확인했고 프로필은 HTTPS만 요청한다. 고정 `IDX=154`, `BRD_ID=1023`, 공고 `BOARD_IDX`를 대조한다. 운영 seed는 변경하지 않았다.
- 진주: `div.bbs1view1 > h1.h1`과 공식 `attach1` 영역을 읽는다. 요청은 `https://www.jinju.go.kr/DownloadEx.do`만 사용한다. 중첩 URL은 `eminwon.jinju.go.kr/emwp/jsp/ofr/FileDown.jsp`와 공개 파일3query로 제한하고 외부 name과 내부 사용자 파일명을 대조한다. 내부 HTTP 주소를 직접 호출하지 않는다.
- 진주 첨부 영역의 미리보기 script는 실행하지 않으며 미해석 발견 오류를 남기는 계약이다. 실제 관측에서는 그 단계에 도달하지 못했다.
- source/parser/URL hash 결합, HTTPS443, 공식 호스트·고정 경로, 동일 요청 redirect 제한, 중복 충돌·10파일 상한, 형식별 분리, 문서 역할 UNKNOWN을 유지한다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기는 변경하지 않았다.

## 사천·김해·창녕 조사

각 공식 목록에 GET1회로 HTTP400/166byte 응답이 관측됐다. 인증 차단·사이트 전체 장애라고 단정하지 않으며 Linux 환경 등 후속 확인 대상으로 남긴다. 첨부 계약을 확인하지 못했으므로 미등록·다운로드 미관측으로 유지한다. 이3지역은 등록 지역 오류 수18에 포함되지 않는다.

- 사천 LGS-000227: `https://www.sacheon.go.kr/news/00009/00014.web`
- 김해 LGS-000228: `https://www.gimhae.go.kr/03360/00023/00029.web`
- 창녕 LGS-000234: `https://www.cng.go.kr/03517/01553.web`

다음은 아직 연결하지 않은 경남 공식 게시판(경남도·창원·통영·거제·의령·고성·남해·하동·산청 등)이다. 이번 오류를 먼저 모두 해결해야만 진행하는 방식은 사용하지 않는다.

## 요청 예산 / 정리

조사10회(GET9·검색 POST1), 관측 예약12회로 합계22회다. 본문 예약 상한 포함이며 실제 HTTP 횟수와 동일하다고 표현하지 않는다. 고령 조사4+관측6, 진주 조사3+첫 관측3+재시도3, 사천·김해·창녕 각 조사1이다.

조사당15초/연결7초/최대1MiB/자동 redirect0/TLS 검증 유지. 관측 실행당 최대6요청·23MiB, 예약 byte 합계8,087,635. 진주는2회 이후 추가 재시도하지 않았다. 임시 조사 원본16개 총977,629byte와 관측 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하고 사용자 프로세스·미추적 `output/`, `scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongnamFirstDownloadContractTest' --tests '*GyeongbukSixthDownloadContractTest' --tests '*GyeongbukFifthDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GORYEONG,JINJU' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JINJU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

계약150건 실패·오류·skip0, bootJar 작업 완료, Node23/23. **첫 복합 Gradle 명령은 고령 관측 성공/진주 관측 실패로 exit1(2분1초), 진주 재시도도 exit1(45초)**다. 전체 명령을 성공으로 보고하지 않는다. 158영수증/121공고 재현은 통과했다. 외부 관측 실패는 코드 계약 테스트 실패와 구분하며 다음 지역 진행을 막지 않는다.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 미실행이다. 브라우저는 현재 지역 연결 작업에 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
