# 강동구 공식 포털 본문·첨부 다운로드 연결

## 현재 단계와 Gate

- [x] 공식 포털 제목·본문·첨부 영역 분리 및 다운로드 연결
- [x] 고정 공고 37327에서 HWP 2개 다운로드·파일 형식 검증
- [x] 최초 HTML 응답 실패와 수정 후 성공 기록을 모두 보존
- [x] 정상 파일 수집과 미해결 미리보기 발견 경고를 분리
- [~] 활성 지역 수집원 223개 중 다운로드 관측 193개(86.5%), 잔여 30개
- [ ] 미연결 4개: 울산 남구(079), 괴산(144), 천안(148), 서천(158)
- [ ] 등록됐으나 정상 다운로드 미확인 26개
- [ ] 상시 목록 유입·전체 파일 집합·추출·DB/API·운영 E2E 별도 검증

분모는 2026-09-28T15:56:12.116778+09:00 운영 인벤토리의 활성 **수집원** 수다. 고유 지자체 수 또는 최신 운영 조회 결과로 바꾸어 표현하지 않는다. 193개는 적어도 한 파일의 실제 다운로드가 관측된 수집원이며 전체 공고 수집률이 아니다. 엄격한 전체 집합 Gate는 16/223으로 유지한다. 오류가 있는 수집원 46개는 다운로드 성공 집계와 중복될 수 있다.

## 관측 대상과 구현

- 공고: `GANGDONG-37327`, `LGS-000026`
- 공식 상세: `https://www.gangdong.go.kr/web/newportal/notice/01/37327`
- 제목: 중소기업 및 소상공인 특별추천 신용대출 확대 및 1년간 무이자 융자지원
- **2021년 보관 공고**다. 현재 접근 가능한 다운로드 경로 검증이며 현재 신청 가능한 공고나 운영 수집 성공의 근거가 아니다.
- 프로필: `LOCAL_GANGDONG_POST_V1`, 시스템 바인딩 `SAFE_SAEOL_EMINWON_HREF`
- 프로필 SHA-256: `e225f458c3b792a34fc8b1c51737f6a7b0ff385843a0b1279f19c22c7d48405b`

`GangdongNoticePage`는 공식 `frmNotice` 표의 제목, 본문, 첨부 셀만 선택한다. 메뉴·담당자 정보·첨부 링크를 본문에 섞지 않으며 구조 변경은 오류로 기록한다. 본문 1,163자와 지원대상·지원유형 조합을 확인했다.

`GangdongAttachmentDiscoveryProfile`은 첨부 셀의 공식 `goDownLoad` 호출에서 파일명·저장 파일명·경로 3개 값을 읽고, 실제 공식 함수와 동일한 공개 POST 4개 필드를 구성한다. JavaScript를 실행하지 않는다. 파일 역할은 `UNKNOWN`을 유지한다.

`GangdongPeriodDownloadFlow`는 실제 응답을 따라 다음 순서로 처리한다.

1. `FileDownNewPbs.jsp` POST → 공식 중간 HTML 폼 확인
2. `OfrAction.do` POST → 게시기간 확인
3. `FDSendNewPbs.jsp` POST → 파일 다운로드

호스트·경로·폼 필드·파일 식별자·게시기간을 검증한다. 공식 `isHome=N`을 유지하고 만료일을 우회하지 않는다. 기존 전송기의 요청·바이트 제한, 취소, lease/heartbeat, 임시 파일 정리를 사용한다. 다른 파일·호스트로의 변경 및 미리 채운 게시기간은 차단한다.

미리보기 `goViewer`·`fnPreChk`는 실행하지 않아 `ATTACHMENT_LINK_UNRESOLVED`가 남는다. 따라서 `discoveryComplete=false`, `collectionStageComplete=false`다. 이 경고가 정상 HWP 2개 다운로드를 막지는 않으며 전체 발견 완료로도 바꾸지 않는다.

## 실파일 증거

최종 관측: 2026-09-30 21:42:35 KST. HWP 2개, 총 **50,688byte**, 파일 다운로드 실패 0개.

| 크기 | 파일 SHA-256 |
| --- | --- |
| 17,408byte | `54db9a04b29009784f850052613c29626b1035ab763281a2ee47b3cba0d28f55` |
| 33,280byte | `bf61982f9d39b993c7f38ba0eb2839ec4a8d2ad289e4e4a34e5e7255b5849785` |

본문 SHA-256: `b281de7e9459c5c33b4f9ddb883c2056039916b7337c491ada616300f02ea11f`.

| 실행 | 보고서 | 결과 / 보고서 SHA-256 |
| --- | --- | --- |
| 최초 21:34:51 KST | `build/reports/attachment-regional-collection/GANGDONG-37327.json` | 중간 HTML 2건을 파일로 통과시키지 않음, `ATTACHMENT_SIGNATURE_UNSUPPORTED`; `9eb6e0ba0fe49d9249e597c98d196390ad3291eb3840a189308177555f5b8810` |
| 3단계 연결 후 | `build/reports/attachment-regional-collection/GANGDONG-37327-FLOW-20260930.json` | HWP 2개 성공, 발견 경고 별도; `9fc79d0c870c5865ce3f6628f020aea71e6a332a50fdca3d9929a67345aa4f9c` |

최초 실행 producer hash는 `88836aed736902d59fb702197c8b9988cc8603c62c179d002cef72ea9307dd75`다. 이후 컴파일 직후 Gradle compile transaction이 보존한 이전 `AnnouncementAttachmentBbsOfficialObservationTest.class`(150,639byte, 수정 시각 21:30:22 KST)를 읽어 해시를 확인했다. 새 클래스 해시를 과거 실행에 소급하지 않았다. 최종 실행 producer hash는 `cc62b0598d8b09ddbeb65699e2e5273a2a2535d54218941ca392ad0a0850034d`다.

두 실행의 요청 상한은 각각 6·9회, 바이트 상한은 각각 24,117,248byte였다. 실제 보고서의 요청 예약 합계는 14회, 본문 포함 예약 바이트 합계는 4,583,004byte다. 원본 임시 파일 정리 확인, 운영 쓰기 0회. 텍스트 추출·기대값 승인·운영 E2E는 실행하지 않았다.

## 다른 미연결 지역 조사

공식 공개 페이지 구조 조사 20요청(GET 18, POST 2)을 별도로 실행했다. 요청별 12초·2MiB, TLS 검증 유지, 자동 redirect 비활성, collector UA 사용. 원문 파일은 저장하지 않았다.

| 지역 | 요청 수 | 결과 |
| --- | ---: | --- |
| 강동 | 10 | 공식 목록·검색·상세와 중간 폼 확인. 2회 POST는 다운로드 중간 응답 진단이며 게시기간 우회 없음 |
| 괴산 | 4 | 공식 포털 및 내장 새올 목록 200, 후속 목록 400. 상세·첨부 연결 미완료 |
| 울산 남구 | 4 | 공식 포털 및 내장 새올 목록 200, 후속 목록 400. 상세·첨부 연결 미완료 |
| 천안 | 1 | 공식 목록 403 |
| 서천 | 1 | 공식 목록 502 |

26개 등록 미확인 수집원은 은평, 서대문, 송파, 부산, 연제, 검단, 유성, 성남, 평택, 포천, 이천, 동두천, 강릉, 속초, 평창, 철원, 충북, 충주, 영동, 공주, 순창, 진도, 의성, 영덕, 성주, 합천이다. 각 오류의 이전 관측 기록을 유지했으며 이번에 모두 재접속한 것은 아니다. TLS·HTTP·빈 제목·파일 형식·권한 문제를 서로 다른 오류로 취급한다.

## 검증과 기록 보존

Gradle은 프로젝트 wrapper `.\gradlew.bat --no-daemon`으로 순차 실행했다.

- 최초 계약·본문·worker·intake 선택 테스트: 212개 통과.
- 3단계 연결 전 확장 회귀·인벤토리·카탈로그·bootJar·관측: 386개 통과. 관측 태스크 종료 0은 최초 실파일 실패를 성공으로 바꾸지 않는다.
- 3단계 연결 후 강동·통영 전송·worker·intake·파일 형식: 96개 중 95개 통과, 기존 통영 외부 fixture 조건부 1개 생략.
- 최종 `:test` 선택: `GangdongDownloadContractTest`, `GangdongPeriodAttachmentTransferTest`, `AttachmentProviderInventoryAuditTest`, `LocalGovernmentNoticeProviderContentClientTest`, `AttachmentFileTypeValidatorTest`, `AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage` → **182개 통과**, 실패·생략 0.
- 같은 최종 실행의 `:bootJar` 통과, `:attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GANGDONG -PsanebCollectionReportLabel=FLOW-20260930 -PsanebCollectionWindowsTrust=true` 실파일 관측 완료.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`: **349기록 / 274최신 공고** 재현.
- `node scripts/qa/report-collection-availability.mjs`: **193/223·잔여30·오류46·엄격16** 재현.

기존 347개 실행 기록 및 273개 최신 공고는 변경하지 않고 강동 실패·성공 2기록과 최신 1공고를 추가했다. 카탈로그 기존 282개 사례를 보존하고 reference-only 1개를 추가하여 283사례·219대상이 됐다. 기대값 승인은 추가하지 않았다. 프로필 총 220개(지역 219개+기업마당 1개). 인벤토리 산출물 해시는 `c23026eb0fdac8afd069de7e3846e83d37400befe79ab2e7fce618a492755417`이며 입력 운영 인벤토리는 9월 28일 기록을 재사용했다.

## 범위와 남은 위험

- 운영의 기존 새올 목록 endpoint는 변경하지 않았다. 새 공식 포털 상세를 고정 표본으로 연결한 상태이며 기존 URL 식별자를 조용히 바꾸지 않는다. 새 목록의 상시 유입과 목록·상세 계약은 별도 후속이다.
- DB/API 계약·Flyway V85·운영 정책·worker·ENFORCE·재분류·배포 변경 없음.
- HWP 추출기 1.0.16 고도화 보류 유지. 이번 파일 다운로드 성공을 HWP 텍스트 해석 성공으로 표현하지 않는다.
- 전체 테스트 스위트·운영 DB/API·AWS·운영 E2E 미실행. 브라우저는 현재 사용자 지시가 없어 정책상 미실행.
- 이번 관측의 원본 정리는 확인했으나, 과거 `build/qa-yeonje-gurye-20260930` 정리 차단은 해결되지 않았다. 모든 과거 임시 자원까지 정리됐다고 보고하지 않는다.
- 커밋·푸시는 작업 브랜치의 `[skip deploy]` 범위로만 진행하며 전체 goal은 계속 진행 상태다.
