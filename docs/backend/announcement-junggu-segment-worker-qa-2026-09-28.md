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
- [x] 서버 1회 실행 성공. 원본·lease·unit·전송 객체 정리와 운영 JAR불변/healthUP을 영수증으로 확인했다.

## 로컬 검증

- Java 중구/함안/기존 probe 표적74개·패키지20개 통과, 실패/생략0. bootJar/probe JAR 성공.
- 후속 로컬 `:test` 전체3,084개 중2,790개 통과/294개 조건부 생략, 실패·오류0,7분46초 성공. 조건부 생략을 실제 외부/DB 검증으로 세지 않는다.
- Node launcher 계약11개, Python 임시 서버 실행기31개: 통과, 외부 요청 없음.
- 최초 패키징 명령은 존재하지 않는 `attachmentContractQaInstallDist` task명으로 실패했다. 실제 `attachmentContractQaTest`의 `installAttachmentContractQa` 의존 경로로 정정하여 통과했다.
- 브라우저는 현재 요청의 명시 실행 지시가 없어 정책상 미실행이다. MockMvc API projection 검증은 운영 인증 브라우저 E2E가 아니다.

## 완료 경계

이 QA의 성공은 고정 공고의 실제 파일→worker→임시 DB/API 연결 증거다. 부분 PDF의 내용 완전성, 정상 기대값 승인, 정책 QA, 운영 적용 및 인증 브라우저 E2E를 대신하지 않는다. 전체9 Gate와 ATT-001~062+구간 분석 범위는 축소하지 않는다.

## 실행 추적

- 소스72ec2b2904151950fd69ee662eb220734fda1879, QA 브랜치 원격 일치. 운영 배포 없음.
- 실행b174a010edd941e483173d6e48c17ad0,135파일/88,408,816byte. archiveSha256=`ac97938444faf60c37c6ddf2cae3a0763e20018baed533e6471d6ecc542d8a40`, codeHash=`a15049147cd15f81eb45d07615e5b20c87c3785659b850d41641f39c19c8d549`.
- `build/qa-results/junggu-segment-reservation-20260928.json`의 단일 예약을 보존한다. 중복 CheckOnly조차 `JUNGGU_SEGMENT_ALREADY_RESERVED_DO_NOT_RETRY`로 거부됨을 확인했다.
- 미사용 사전 패키지c9eb7928의ZIP만 제거하고plan은 보존했다.

## 서울 실제 실행 결과

- SSM `b3e351ec-574f-4971-ba01-8cb73f13a348`: Success/exit0,50.796초, 검사1개 통과·실패/생략/취소/컨테이너 실패0.
- 영수증 `build/temporary-bbs-qa-b174a010edd941e483173d6e48c17ad0/result-utf8.json`, SHA256 `22a14695905a658ee36baef274769d93584ce68685887534eaf652234a28935b`.
- 제목 조합 통과, 실제 본문 AVAILABLE/1attempt/ACCEPTED, 공식 첨부2개 발견·2개 처리·격리 추출2회. 본문 ACCEPTED를 전체 최종 승인으로 사용하지 않았다.
- HWP COMPLETE_TEXT/3,120자/178block·파일 역할FORM. 명시1.0.4의4구간 중UNKNOWN3/NOTICE0이며 분석 hash=`7c0482bddb7815456ba5c0402708003789f47770208fd679feebd449d568e972`이다. 구간 분석이 저장됐다는 사실과 역할 판정이 충분하다는 주장은 구분한다.
- PDF PARTIAL_TEXT/4,241자/5block, Do1·신뢰구조0페이지.1.0.4 UNKNOWN1/COMPLETE_TEXT_REQUIRED·NOTICE0, 분석 hash=`61decb57e9caab7101810f9223eb2077fa14030b45cd3a18727abd24bddc9f99`이다.
- 실제 worker EVALUATED이나 job PARTIAL_FAILED/화면 처리흐름TECHNICAL_EXCEPTION/최종 REVIEW_REQUIRED·ATTACHMENT_INCOMPLETE다. 부분 품질을 정상 후보로 표시하지 않는 계약이 검증됐다.
- 양쪽 원문/DB·분석/DB·평가 입력FK·구간 API·평가결합 API·검수 context 일치, 구버전 GET 무변경·다른 source404·no-store·자동확정/운영공고 링크0 확인. 브라우저와 운영 인증은 미실행이다.
- 신규 예약5요청/2,439,945byte, 중구 누적 **32/32회·16,491,779/81,788,928byte**. 남은 요청0이므로 추가 실파일 실행은 새로운 범위 승인이 필요하다.
- 원본 제거/임시 unit inactive/잔여 lease0/서버 전송 임시 파일 제거 확인. 운영 DB미사용·쓰기0, JAR불변·healthUP이다. 정책 QA·기대값 승인·전체 분석 완료·운영 브라우저 E2E는 모두false다.
- 해당 execution 소유 S3 객체 삭제/부재 확인, plan.cleaned=true. 지문·절대 경로를 확인한 로컬ZIP도 제거했다. plan/영수증/예약은 보존했다. 실행한 Gradle/시험·AWS 세션은 종료했으며 기존 사용자 Java 프로세스는 보존했다.
