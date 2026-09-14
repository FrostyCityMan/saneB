# 수집원 실파일 QA 예약·이력·취소 화면

## 설계 기준

Design Read: 기존 Thymeleaf·Bootstrap·첨부 업무 CSS를 재사용해 고정된 QA 분할의 범위·외부 요청 예산을 검토하고, 별도 확인 후 예약·취소하는 업무 화면을 만든다. 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증 흐름은 변경하지 않는다.

- 사용자: 활성 ADMIN은 예약/취소, OPERATOR/APPROVER는 조회만 한다. 서버의 비밀번호 변경 완료·CSRF 검증을 유지한다.
- 주요 과업: 정책에서 진입→전체 계획/선택 분할의 공고 코드·요청/바이트/최대 시간 확인→사유/개별 동의→예약 응답과 실행/항목 이력 확인→필요시 최신 버전으로 취소.
- 위험 R2: 외부 파일 요청을 만드는 QA 예약이다. 게시/ENFORCE/기존 데이터 APPLY/공고 활성화 권한과 분리한다. 브라우저 QA에서는 합성 fixture만 사용하며 운영 예약은 실행하지 않는다.
- 화면: 새 `/app/admin/announcement-attachment-provider-qa`, 기존 정책 상세에서 진입. 읽기 전용 coverage 화면의 무변경 계약을 보존한다.
- 재사용: 실제 layout·첨부 공통 CSS·정책 요청 client·전역 CSRF/로딩 처리. 새 라이브러리·DB/migration·공개 API shape 변경은 없다.
- 기기/언어: 한국어 업무 용어, 서울 시각, 320px부터 반응형 카드/dl·44px 컨트롤·키보드·status/alert. 고정 예산을 직접 입력하거나 파서/URL/파일/성공 값을 선택하는 폼은 금지한다.

## 안전 계약

1. 계획·이력·항목은 정확한 페이지 분모/정책 소속/버전/지문을 검증한다. 기대 coverage와 COMPLETED/PASSED를 정책 전체 QA 통과·정상 공고로 표시하지 않는다.
2. 계획 조회가409여도 기존 이력·취소는 별도로 동작한다. OFF에서는 새 예약을 막고 조회/취소는 유지한다. 전체 기대값 미완료 인지는 일부 QA 분할 실행 동의일 뿐 전체 Gate 면제가 아니다.
3. 예약은 현재 페이지의 고정 분할을 선택하고 범위·네트워크 예산·미완료 인지(필요시)를 개별 확인한다. 사유1~1000자. POST 전에 입력을 문자열과 UUID 키로 고정한다. 응답은 정책/계획/분할/예산에 정확히 결합한다.
4. 응답 유실·5xx·잘못된 성공 응답은 미확정이다. 새 예약/입력 변경을 막고 원래 키·본문으로만 명시적으로 재확인한다. 이후401/403/409도 이전 미확정을 지우지 않는다. 새로고침/이탈 시 경고하며 사유/요청 키를 URL·로그·영구 브라우저 저장소에 쓰지 않는다.
5. 취소는 READY/RUNNING만 최신 run rowVersion과 사유로 PUT한다. CANCEL_REQUESTED는 소유자 정리 중이지 취소 완료가 아니다. 응답 유실 후 최신 이력을 조회하고 별도 인지 후 현재 상태를 새 검토 기준으로 채택할 수 있으나 원래 취소 성공을 추정하지 않는다.
6. 입력/동의는 오류 때 보존하고 다른 분할/행동 선택 때 동의를 초기화한다. 중복 제출·동시 선택을 막고 자동 예약/자동 재시도/자동 다음 분할은 없다. 페이지별 조회는 현재 선택과 URL을 보존한다.
7. 범위/네트워크 예산/요청 결과는 지속 표시한다. 내부 case 코드와 metadata만 사용하고 임의 오류 원문·원문 URL·파일/본문·lease/키를 표시하지 않는다.

## 검증 계획과 완료 조건

- [x] DTO/상태/예산/페이지/legacy 이력·멱등 재확인·취소 복구 Node 계약.
- [x] 실제 화면 연결, 역할 위조·중복 클릭·입력 보존·OFF·계획 실패와 이력 분리·명시적 확인 Node 상호작용.
- [x] SSR·권한·CSRF·기존 v1/API 불변·서버 표적79건 통과·bootJar 성공. 전체 회귀는2498건 중2246통과/252조건부 생략/실패0, QA 패키지20/20이다. 별도 실제 Linux/DB 경계는 아래와 구분한다.
- [~] 합성 브라우저 예약→이력→항목→취소·응답 유실 복구·조회 전용 역할·320~1440px 확인. 전체 키보드/보조기술·네이티브 확대는 미완료이며 실제 운영 성공으로 계산하지 않는다.
- [x] 같은 SHA `ad0e1307dd0ed1c8cbadb33468b2297ff5ece326`의 [Linux 실행27](https://github.com/FrostyCityMan/saneB/actions/runs/34898209145) 전체 성공. root2498=2245통과/253생략, 별도 job192·migration17·worker11·runtime1·부모2·독립220/220·정리 통과, Node241/241·설치12/12다. 운영 배포/전체 Provider/정책 게시/기존 배치/운영 브라우저는 전체 장기 목표의 별도 필수 잔여다.

측정은 시나리오별 계약·무중복 요청·실제 변경 응답 일치로 수행하고 새로운 분석 SDK를 도입하지 않는다. 현재 전체 Gate/ATT는 Not ready다.

## 2026-09-15 로컬·합성 검증 근거

- `node --test scripts/qa/attachment-provider-qa-ui.test.mjs`: 최종41/41 통과. 추가 페이지 시험의 초기 실패는 fixture의 기대 공고21건보다 실행 가능 분모2건이 작아서 응답 계약에 거부된 결과였고, 시험의 분모를21로 정정했다. 운영 계약을 완화하지 않았다.
- workflow와 같은 UI/판정기10개 파일의 Node 회귀241/241·실패/생략0. 별도 `attachment-contract-release.test.mjs`는12건 중 Windows10통과/2 Linux 전용 생략이다. 생략을 통과에 합산하지 않는다.
- `:test --tests '*AnnouncementAttachmentProviderQaViewSmokeTest' --tests '*AnnouncementAttachmentProviderQaManagementControllerSmokeTest' --tests '*AnnouncementAttachmentPolicyViewControllerSmokeTest' --tests '*AnnouncementAttachmentProviderQaManagementServiceTest' bootJar --no-daemon --max-workers=1`:79건(7+27+8+37) 통과·40초 성공. 전용 SSR은 실제 렌더링의 역할, 초기 비활성/미동의, CSRF 연결, 외부 역할403/익명401을 검사한다.
- `SANEB_ATTACHMENT_PROVIDER_QA_VIEW_EXPORT=true` 시험 산출물에서 실제 토큰·외부 자산을 제거한 SSR을 `attachment-provider-fixture-server.mjs`로 loopback에서 제공했다. 운영 DB/인증/파일 요청을 연결하지 않았다. 가짜 CSRF cookie/header만 사용하고 실제 전역 CSRF JS를 그대로 경유했다.
- 합성 정상 시나리오: 예약1회→전체2항목 조회→취소1회. fixture 쓰기2, CSRF 거부0, 외부 요청0, 운영 쓰기0. 실제 화면 console 오류0/경고0, 예약 영수증 focus 확인. 성공 화면이 전체 Provider QA 통과를 주장하지 않음을 확인했다.
- 합성 예약 유실: 서버에 최초 예약 저장 후503→재확인403→동일 키/본문의 정상 응답. 총3회 요청/실제 예약1건이다. 합성 취소 유실은 최신 CANCELLED 이력 GET 후 별도 동의로 현재 상태만 채택했다. 추가 PUT 없이 원래 취소 성공 미확정 안내를 유지했다. 주입한 HTTP 실패의 console 오류는 정상 시나리오 오류0과 구분한다.
- 조회 전용·실행 OFF·계획409·빈 목록을 브라우저에서 확인했다. 계획409/OFF여도 기존 READY 취소는 가능하고 새 예약만 막는다. 1440/1024/768/375/360/320px에서 가로 넘침0, desktop/mobile 스크린샷을 검토했다. 스크린샷은 ignored `build/attachment-provider-ui-qa`에 둔다.
- 네이티브 확대 검증을 위해 Chrome 설정 탭을 열 때 제어 context가 종료되어200/400% 확대는 확인하지 못했다. CSS 확대를 네이티브 확대 증거로 대신하지 않는다. 전체 Tab/Shift+Tab·보조기술 검증도 별도 잔여다. Chrome 세션 종료를 확인했고 전용 Node 서버를 중지했다. 생성한 CLI 기록은 ignored build 아래로 이동했으며 사용자 Word2개는 변경하지 않았다.
- 직전 SHA `1d4be1b5c4e4fbe86bb3b520f93d9f2f2c9ee328`의 Linux 실행26은 통과했지만 이 새 예약 화면의 같은 SHA 검증이 아니다.
- 최종 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분37초 성공이다. root2498=2246통과/252조건부 생략/실패0, QA 패키지20/20. extractor25건과 bootJar는 UP-TO-DATE이므로 새 시험/빌드 실행으로 세지 않는다. 앞선 표적 실행에서 생성한 현재 bootJar SHA256은 `fd57b761ceb2dcd7cab381ae3e3db9cb2fb46aced6c927f59ed3a01b0b2402e1`이며 운영 artifact가 아니다.
