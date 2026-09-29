# 전주·전북도 본문/첨부 연결과 HWPX 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 우선 수집하고 발견·전송·형식·파싱·본문 오류는 별도로 기록한다. 개별 지역의 오류나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 전주·전북도 공식 본문·첨부 프로필 2개 연결.
- [x] HWPX 각 1개, 총 413,181byte 다운로드와 signature/MIME/파일명 검증.
- [x] 김제 공식 목록의 HTTP 302 자기 리디렉션을 조사 오류로 분리.
- [x] Java 216통과·조건부 1생략, Node 23통과, bootJar·수집 관측 성공.
- [x] 220영수증/176공고 재현, 기존 218영수증/174공고 보존.
- [x] 조사 원본 10개 1,336,396byte 길이·SHA256 검증 후 개별 삭제.
- [ ] 미등록 91지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 작업은 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 103 | **105/223 (47.1%)** |
| 다운로드 미확인 | 120 | **118** |
| 프로필 등록 지역 | 130 | **132** |
| 미등록 지역 | 93 | **91** |
| 등록 후 다운로드 미확인 | 27 | **27** |
| 현재 프로필 hash에 일치하는 오류 관측 지역 | 32 | **32** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 132+기업마당 1=133프로필, 카탈로그 188참조/131대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 오류 수에는 성공 파일이 공존하는 지역도 포함되므로 미확인 수에 합산하지 않는다. 김제 조사 오류는 프로필 등록 전 관측이며 위 32지역에 가산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 전주 LGS-000164 / 7c37d618236741a28b133aa279d05c63 | 302자, AVAILABLE / ACCEPTED | HWPX 1개 94,425byte 성공 |
| 전북도 LGS-000163 / 670379 | 46자, AVAILABLE / REVIEW_REQUIRED | HWPX 1개 318,756byte 성공 |
| 김제 LGS-000169 | 공식 목록 HTTP 302 자기 리디렉션 | 상세·첨부 미실행, 미등록 유지 |

전북도 본문은 변경 사항만 짧게 기록되어 대상·지원 조합을 충분히 확인하지 못했다. `BODY_COMBINATION_NOT_CONFIRMED`를 유지하며 다운로드 성공을 첨부 텍스트 추출·분류 또는 공고 승인으로 간주하지 않는다. 두 파일의 역할은 UNKNOWN이다.

- 전주 파일 SHA256: `fabba96d0a7d11cf5fef5c6fbfe9089cb6f474676c5fa5f76d3be50c76cce438`
- 전북도 파일 SHA256: `f6d15641e8118b0cab943e850b11a5cf352f0f9ba4df03652a4af6e66594890b`
- 전주 프로필 hash: `0ecfb5a0dd4c9b3b2311d62ea193b3c74067bf9c650cd7f10a01c8effee16dc6`
- 전북도 프로필 hash: `b95df7b69e7081c253c4f67bdc5619c574d3c429d9f4a0faa6872de19fdfa756`
- 관측 producer class hash: `88912baa20debda7dfa12ba00686f907f30be9610738b9a54cf5fe0cd91966d3`

## 구현 계약과 한계

- `JeonbukThirdNoticePage`는 전주의 `board_wrap > view-group` 안에서 제목 라벨, `view-list > view-con` 본문, 첨부파일 라벨을 구분한다. 전북도는 `bbs_skin > bbs_view`의 `bbs_vtop > h4`, `bbs_con`, `p.bbs_filedown`을 사용한다. 메뉴·푸터·담당자·첨부 파일명은 본문에서 제외한다.
- 전주 공식 상세 링크의 빈 `&&` 구간만 사이트 내부에서 정리한다. 중복 파라미터·다른 board/menu·외부 host·HTTP·다른 포트·userinfo·fragment는 거부한다. 공통 query 파서와 기존 기관 지문은 변경하지 않았다.
- 전주는 같은 board의 `download.9is`와 fileUid를 사용한다. 같은 fileUid의 공식 미리보기만 보조 링크로 인식하며 호출하지 않는다. 파일명 뒤 관측 크기 표기만 제거한다.
- 전북도는 같은 board/menu/dataSid의 `download.jeonbuk` 개별 GET만 사용한다. 대응하는 SynapViewer와 같은 공고의 전체 ZIP 링크는 보조 링크로만 인식하며 호출하지 않는다. 연결 가능한 정상 descriptor는 미해석 링크 오류와 함께 보존한다.
- 전북도 파일명 정리에서 `.hwp`가 `.hwpx`의 접두 부분과 겹치는 문제를 회귀 테스트로 발견하고 긴 이름부터 정리하도록 수정했다. 검증을 생략하거나 기대값을 완화하지 않았다.
- 파일 최대 10개·상세 1MiB·파일 20MiB, 미지원 확장자 미다운로드, SSRF/TLS/host 검증을 유지한다. 발견 성공이 모든 공고·모든 미래 첨부 형식의 성공을 보장하지 않는다.
- 이번 공식 상세 HTML의 페이지 이용표시에 비상업적 이용·변경금지 조건이 있었다. 본 작업은 제한된 기술 QA이며 서비스 재배포·상업적 이용 권한을 확인한 것이 아니다. 운영 공개 전 개별 자료의 이용조건 확인이 별도로 필요하다.
- 공통 프로필 인터페이스·fingerprint·validator·기존 migration·DB/API/UI·HWP 추출기·운영 정책·worker 설정은 변경하지 않았다. 신규 카탈로그 참조는 정책 승인·운영 활성화 근거가 아니다.

## 요청량과 정리

조사 GET 총 10회: 전주 목록→공식 리디렉션 대상→제목 검색→상세 4회, 전북도 목록→공식 리디렉션 대상→제목 검색 2회→상세 5회, 김제 목록 1회. 요청당 15초·연결 7초·2MiB·자동 redirect 0·TLS 검증을 유지했다. 김제 자기 리디렉션은 반복하지 않았다.

Java 관측은 각 지역 최대 6요청/23MiB 안에서 1회씩 실행했다. 본문 포함 실제 예약 합계 8회·4,976,125byte, 조사 포함 예약 상한은 18회다. 예약 수와 실제 HTTP 호출 수는 같지 않다. 파일 원본은 관측 finally에서 정리했고 조사 HTML 10개는 길이·hash 대조 후 개별 삭제했다. 비식별 metadata만 보존한다.

## 검증 명령 / 결과 / 미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_JEONBUK_THIRD_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*JeonbukThirdDownloadContractTest' --tests '*ChungcheongSixthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEONJU,JEONBUK' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

환경변수는 실행 전 값을 보관하고 finally에서 복원했다. 최초 실행은 전북도 파일명 정리 테스트 1실패로 중단되어 외부 관측을 실행하지 않았다. 수정 후 동일 명령은 1분 36초에 성공했다. Java JUnit 217건 중 216통과·기존 아산/서산 원본 fixture 컨테이너 1건 조건부 생략, 신규 실제 HTML 구조 검사 통과, 지역 관측 2건 통과, Node 23통과, bootJar 성공. 원본 정리 후 fixture는 더 이상 남아 있지 않으므로 이후 재실행 시 환경변수를 켜지 않는다.

전체 테스트·전국 상시 worker·첨부 텍스트 추출·운영 DB/API/UI·브라우저 E2E는 이번에 검증하지 않았다. 브라우저는 현재 요청의 명시적 실행 지시가 없어 정책상 생략했다. 이번 커밋·푸시는 `[skip deploy]` 범위이며 운영 배포·정책 활성화·재분류는 실행하지 않는다.
