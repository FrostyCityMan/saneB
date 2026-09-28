# 중구 단일 공고 구간 worker·DB·API 격리 QA

## 목표·승인 경계

2026-09-28 사용자 `중구 단일 worker QA 승인`에 따라 JUNGGU-33626의 제목 → 실제 본문 → 공식 첨부 HWP/PDF 전체 → 명시 segment-role-1.0.4 → 실제 worker → 임시 PostgreSQL → API projection을 1회 검증한다.

- 기존 누적27회에 최대5회를 추가한다. 요청 상한30→32회 승인, 누적 byte 상한81,788,928은 유지한다. 이전 사용량14,051,834byte, 신규 상한25,165,824byte이다.
- 서울 서버 CPU1·메모리768MiB·임시공간1GiB·20분 이내. 운영 설치·DB·정책·worker·공고 활성화는 변경하지 않는다.
- 중구 단일 모드 `JUNGGU_SEGMENT`는 명시 실행만 지원하며 CI 외부 호출은 추가하지 않는다. CI의 합성 준비 검증은 실제 공고 성공과 구분한다.

## 성공·실패 기준

- [x] 관측1.0.14에서 확인한 HWP/PDF binary/text 지문, 문자·block 수를 각각 고정한다. HWP127,488byte/3,120자/178block, PDF209,769byte/4,241자/5block이다.
- [x] 추출 원문과 DB 원문, 독립 계산 분석과 저장 분석, 현재 평가 입력FK, segment API 응답, 검수 context를 비교한다. 구버전 GET의 fallback·쓰기·다른 source 조회는 허용하지 않는다.
- [x] HWP 구간1.0.0 관측 hash를1.0.4 기대값으로 재사용하지 않는다. 실제 입력의 연속 coverage와 저장/조회 일치를 검증한다.
- [x] PDF PARTIAL_TEXT는 COMPLETE_TEXT_REQUIRED/UNKNOWN1·근거0으로 유지한다. 전체 공고 REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE를 정상 후보 승인으로 승격하지 않는다.
- [x] 누락 파일·다른 binary·다른 버전·예산 초과·문자열 boolean·거짓 정상 완료는 실패한다. 합성 검증기 fixture는 실제 공고 결과가 아니다.
- [ ] 서버 1회 실행, 원본·lease·unit·전송 객체 정리 및 운영 JAR불변/health를 영수증으로 확인한다.

## 로컬 검증

- Java 중구/함안/기존 probe 표적74개·패키지20개 통과, 실패/생략0. bootJar/probe JAR 성공.
- Node launcher 계약11개, Python 임시 서버 실행기31개: 통과, 외부 요청 없음.
- 최초 패키징 명령은 존재하지 않는 `attachmentContractQaInstallDist` task명으로 실패했다. 실제 `attachmentContractQaTest`의 `installAttachmentContractQa` 의존 경로로 정정하여 통과했다.
- 브라우저는 현재 요청의 명시 실행 지시가 없어 정책상 미실행이다. MockMvc API projection 검증은 운영 인증 브라우저 E2E가 아니다.

## 완료 경계

이 QA의 성공은 고정 공고의 실제 파일→worker→임시 DB/API 연결 증거다. 부분 PDF의 내용 완전성, 정상 기대값 승인, 정책 QA, 운영 적용 및 인증 브라우저 E2E를 대신하지 않는다. 전체9 Gate와 ATT-001~062+구간 분석 범위는 축소하지 않는다.
