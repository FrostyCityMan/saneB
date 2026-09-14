# 부산·강북 법정 게시판 첨부 프로필과 실제 다운로드 검증

## 현재 단계와 범위

2026-09-12 Gate 2 구현 증분이다. 전체 Gate 0~8은 아직 최종 통과하지 않았다. 기존 7개에 부산광역시·서울 강북구 시스템 프로필을 추가하여 등록 프로필은 9개다. 실제 전체 지자체/정부24 지원, 정책 전체 QA, Linux 추출·PostgreSQL·운영 E2E 완료를 의미하지 않는다.

기준은 첨부 상세 설계와 QA 계획의 ATT-004/011~014/020~024/028/044/060이다. 기존 Flyway·DB 행·v1 API·Thymeleaf 화면을 변경하지 않는다. 외부 공고 자동 활성화·정책 게시·ENFORCE·기존 데이터 적용은 하지 않았다.

## 시스템 매핑

| 기관 | source code / 목록 파서 | 첨부 프로필 | 확인한 경로 |
|---|---|---|---|
| 부산광역시 | LGS-000027 / SPRING_BBS | LOCAL_BUSAN_LEGAL_GET_V1 | www.busan.go.kr/nbgosi/view → /nbgosi/download |
| 서울 강북구 | LGS-000010 / SPRING_BBS | LOCAL_GANGBUK_LEGAL_GET_V1 | child.gangbuk.go.kr/portal/bbs/B0000245/view.do → eminwon.gangbuk.go.kr의 고정 3단계 파일 전달 |

매핑 근거는 V61/V62 seed와 실제 공개 목록·상세 응답이다. 목록 파서 SPRING_BBS가 같은 다른 기관을 자동 승인하지 않는다. 현재 운영 DB의 source 매핑은 별도 재확인이 필요하다.

- source code·provider·목록 parser·원문 URL hash·HTTPS 443·정확한 host/path/query를 검증한다.
- 부산은 `dl.form-data-info`의 첨부파일 label과 인접 `dd/ul.attfiles`만 사용한다. 같은 파일의 이름 링크와 다운로드 보조 링크는 canonical query와 파일 ID로 중복 제거한다.
- 강북은 `dl.file-lists`의 첨부 label과 `dd.item/a.file`을 사용한다. 별도 새올 파일 서버 한 곳만 허용한다.
- 확인된 빈 영역만 NO_FILES다. 영역 누락/중복·다른 링크/잔여 파일 텍스트·폼 구조 변경은 실패 또는 불완전이다. 10개 초과를 완전 수집으로 표시하지 않는다.
- PDF/HWP/HWPX 외 형식도 발견 목록에 보존하되 다운로드하지 않는다. worker 저장 상태는 BLOCKED / UNSUPPORTED_FORMAT이다.
- 파일명은 표시·형식 확인용이며 역할·분류 정답이 아니다. 모든 일반 첨부 역할은 UNKNOWN이고 수동 검수를 유지한다.

## 강북: 게재기간 확인을 보존한 고정 요청 흐름

단순 FileDown.jsp GET은 binary가 아니라 HTML 중간 페이지였다. 최초 실측은 signature 오류로 실패했으며 이를 성공으로 변경하거나 HTML을 문서로 처리하지 않았다.

1. 해당 파일의 고정 FileDown.jsp GET. 중간 HTML 최대 32 KiB.
2. 정확한 hidden 10필드·action·파일 ID의 원래 파일명 연결을 검증한다. OfrAction.do에 게재기간 조회 POST. 응답 최대 256 byte.
3. 서버가 반환한 게재기간을 서울 날짜와 비교한다. 만료·잘못된 날짜·HTML 오류는 차단한다. 사이트가 명시한 빈 값/EmptyYmd만 무기한 응답으로 처리한다. 유효한 경우 같은 opaque 값과 조회한 기간으로 FDSendNewPbs.jsp POST.

`isHome=Y`로 기간 확인을 우회하지 않는다. JavaScript 실행, 임의 form action 추종, 재귀 다운로드, POST redirect, opaque 값 해독·영구 저장은 없다. 고정 request path와 필드 집합은 매 요청마다 검증한다.

`AttachmentProfileDownloadFlow`를 실제 worker gateway와 실사이트 QA가 함께 사용한다. 단계마다 기존 DB heartbeat/실행 허용·host 자원 lease·누적 byte 예약을 다시 적용한다. 중간 HTML/기간 응답을 삭제한 뒤 같은 작업 임시 경로에 다음 응답을 받는다. 취소/예외도 임시 원문을 정리한다. 기존 출력 파일이 있으면 덮어쓰기·삭제하지 않고 거부한다.

리다이렉트를 포함한 전체 HTTP 예산은 **파일당 최대 4회**로 공유한다. 강북의 기본 3단계에는 GET redirect 여유가 최대 1회다. 기존 단건/배치 승인 상한 `3회 시도 × (상세 4회 + 파일 10개 × 4회) = 132회`를 확대하지 않는다. 각 실제 요청의 30초 제한, 파일 20 MiB, 공고 누적 80 MiB도 유지한다. 새 form은 정확히 10필드이므로 내부 request 객체의 개수 상한만 8→10으로 확장하고 필드별 2048자·인코딩 후 전체 8192 byte 한도는 유지했다.

## 부산: 실측된 다운로드 헤더 호환

부산 응답은 파일 bytes는 HWPX였지만, 한글 filename에 UTF-8/Latin-1 문자열화가 중복되어 제어문자 검사에 실패했다. 실측한 부산 프로필에서만 최대 2회의 엄격한 UTF-8 octet 복원을 허용한다. 기본 프로필은 기존 검증을 유지한다. 잘린 UTF-8·과도한 재인코딩·CR/LF/NUL/DEL·복원 후 제어문자·경로 이탈·확장자 불일치는 계속 거부한다. 헤더 filename을 로컬 파일 경로로 사용하지 않는다.

이는 모든 헤더의 charset을 추측하는 표준 정책이 아니다. [RFC 6266 6절·부록 C.3](https://www.rfc-editor.org/rfc/rfc6266.html#section-6)은 filename* 인코딩과 비표준 인코딩 추측의 상호운용성 위험을 구분한다. 여기서는 확인한 서버의 제한된 예외만 시스템 프로필/지문으로 고정한다.

## 실제 파일 결과

아래는 05시대 전용 QA 결과다. 각 기관 정상 공고 3건 이상과 비지원 혼합 1건을 포함한다. 운영 제목 필터를 통과한 후보 또는 최종 지원 대상으로 해석하지 않는다.

| 기관 / 공고 ID | 발견 | 내려받은 파일 | binary byte 합계 | HTTP 요청 |
|---|---:|---|---:|---:|
| 부산 79571 | 3 | HWPX 3 | 320,596 | 4 |
| 부산 79570 | 3 | HWPX 3 | 321,257 | 4 |
| 부산 79567 | 3 | HWPX 3 | 303,487 | 4 |
| 강북 184761 | 3 | HWPX 3 | 93,193 | 10 |
| 강북 184744 | 2 | PDF 1 / HWPX 1 | 433,657 | 7 |
| 강북 184759 | 1 | HWPX 1 | 53,458 | 4 |
| 강북 184760 | 4 | PDF 1 / HWPX 2 / 비지원 1 미다운로드 | 173,220 | 10 |

새 7사례 합계: 발견 19개, 실제 파일 18개(PDF 2/HWPX 16), 1,698,868 byte, 요청 43회. 비지원 1개를 성공 다운로드 수에 포함하지 않는다. 모든 임시 HTML/binary 정리를 확인했다. HWP 실파일·내부 ZIP/OLE 검증·실제 텍스트 추출·운영 DB 쓰기는 이번 결과에 없다.

기존 새올 6사례를 포함한 전용 QA는 13사례로 확장했다. `build/reports/attachment-profile-discovery-qa`에는 상태·hash·byte·형식·정리 여부만 남긴다. 공개 원본·파일명·opaque 요청 값은 QA 결과 파일/Git에 넣지 않는다. HTML 진단 중 PowerShell 응답 형식 혼동으로 예외가 발생한 초기 시도와, 안전한 metadata 출력으로 수정한 후의 실제 검증은 구분한다.

전체 강제 검증 실행에서는 강북184744의 첫 PDF 다음 요청에서 HTTP400이1회 발생했다. 그 실행의 실사이트 결과는12통과/1실패이며 전체 명령도 실패했다. 안전한 요청 단계 metadata를 보강한 동일13사례 재실행은29초에 전부 통과했다. 원인을 확정하거나 무시하지 않는다. 다음 운영 QA에서 재현 여부를 확인해야 한다.

## 검증과 변경 파일

- `LegalBoardAttachmentDiscoveryProfileTest`: 다중 형식/역할, 빈 영역·부분/중복·한도, source·host·query·GET/POST 범위, 인코딩, Spring 등록.
- `LegalBoardAttachmentTransferTest`: 고정 3단계, 게재기간/만료, 폼/파일 소속 변조, 공유 요청 상한, 기존 파일 보존, 부분 원문 정리, 실제 gateway의 단계별 DB lease/bytes callback과 취소.
- `AttachmentFileTypeValidatorTest`: 기본 strict 동작과 한정된 한글 헤더 복원, 잘못된 인코딩/제어문자/경로·형식 불일치.
- `AnnouncementAttachmentWorkerServiceTest`: 새 profile에서 모든 파일 순회, 비지원 미다운로드, UNKNOWN 역할과 임시 원본 정리. HTTP·추출·DB 응답은 대역이다.
- `AttachmentDownloadBoundaryTest`: 10필드 허용/11필드 거부, 기존 크기·개인정보 비노출·POST redirect 거부 유지.
- `LegalBoardAttachmentProfileLiveQaTest`: 실제 공식 응답과 공통 pinned transport/flow. 전체 profile QA나 운영 worker 성공을 대신하지 않는다.

주요 구현 파일은 `discovery/LegalBoardAttachment*`, `AttachmentDownloadFlowProfile`, `AttachmentProfileDownloadFlow`, `AttachmentProfileFingerprint`, `worker/AttachmentDownloadGateway`, `AttachmentFileTypeValidator`, `AnnouncementAttachmentWorkerServiceImpl`, `AttachmentPinnedDownloadClient`, `build.gradle`이다. 새 의존성은 없다.

공통 request/flow/타입 검사/gateway를 profile 지문에 포함했다. 기존 프로필의 hash도 달라지므로 현재 시스템 manifest로 정책 초안을 다시 고정하고 전체 QA를 재실행해야 한다. 현재 전체 PROVIDER_PROFILES/WORKER_DB_RECOVERY 실행·검증기 연결이 아직 없으므로 이번 표본 성공을 VERIFIED 또는 정책 게시 승인으로 사용할 수 없다.

최종 전체 빌드/회귀 수치와 Git·운영 상태는 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)의 최신 항목에 기록한다.
