# 첨부 정책 관리 화면 — 초안·QA·게시 영향 관측

현재 Gate 4는 부분 완료이며 전체 출시 판정은 **Not ready**다. 정책 관리 화면을 추가했지만 전체 QA 실행·게시 transaction·전체 Provider/profile·실제 PostgreSQL/Linux·운영 브라우저 완료를 의미하지 않는다.

09-12 후속: V76 준비/V77 실제 게시·영수증 화면을 연결했다. 아래 초안/QA 최초 구현 기록과 구분하며 최신 범위는 `announcement-attachment-policy-publication-ui-2026-09-12.md`를 따른다. 실제 전체 QA·운영 게시 검증은 아직 미완료다.

## 목표·설계 기준

- 사용자 목표: 저장할 초안과 현재 운영 정책을 구별하고, QA 누락·실패 원인과 다음 조치를 확인한다.
- Design Read: 기존 Thymeleaf/Bootstrap 업무 화면에서 `초안 → QA → 게시 영향`을 순서대로 읽되, 저장과 운영 활성화를 섞지 않는다.
- 주요 과업: 초안 생성·수정·개정 / QA 예약·취소·단계 이력 / 해당 규칙·전체 규칙의 게시 영향 확인.
- 위험: 읽기는 R0, 초안 편집은 R1, 설치 QA 예약·취소와 향후 게시 판단은 R2로 관리한다. 사유·명시적 확인·서버 버전 검증·결과 재확인을 사용한다.
- 가정: 한국어 내부 업무 사용자, 데스크톱 중심이지만 360px 모바일·키보드 접근을 지원한다. 새 UI/상태/모션 의존성·차트는 추가하지 않는다.
- 스킬 적용: long-goal-operating-protocol의 검증 경계, ui-ux-operating-principles의 오류 복구·입력 보존, frontend-ui-engineering의 역할별 제어·포커스·상태 분리. browser-qa/playwright-visual-qa/playwright는 합성 로컬 화면 확인에만 사용했다.

## 서버 계약과 연결

화면 `/app/admin/announcement-attachment-policies`는 ADMIN/OPERATOR/APPROVER만 접근하며 `Cache-Control: no-store`다. 공고·수집 메뉴에서 진입하고 키워드 관리·배치·전체 분할 화면으로 연결한다. 실제 변경 권한은 기존 API와 ServiceImpl에서 다시 검증한다.

| 화면 작업 | 기존 서버 계약 | 결과/영향 |
|---|---|---|
| 정책 목록·상세 | v2 policies GET, page/size/status | 상태·개정·조회 버전·설치 설정·시스템 profile 읽기 |
| 키워드 규칙 목록 | v1 announcement-source-rule-releases GET | 페이지별 DRAFT/ACTIVE 선택, 퇴역 선택 제거. 규칙 자동 게시 없음 |
| 새 초안 | policies POST + UUID Idempotency-Key | DRAFT 생성, 원문/운영 정책 미변경 |
| 초안 저장 | policies/{id} PUT + expectedVersion | 저장한 초안만 변경, 서버 시스템 설정 갱신, 과거 QA 현재성 재검증 필요 |
| 새 개정 | policies/{id}/revisions POST + UUID 키 | 원본 보존, 새 초안. 게시/QA 성공 복사 안 함 |
| QA 예약 | validation-runs POST + UUID 키·정책 조회 버전 | 서버가 허용할 때 예약. 완료/게시 아님 |
| QA 목록·상세 | validation-runs GET, page/size/runId | 4단계 상태·증거 지문·오류 코드, 입력 DB 버전과 전체 실행 최신성 구분 |
| QA 취소 | validation-runs/{runId}/cancellation PUT + 실행 조회 버전 | 대기 취소 또는 현재 파일 정리 후 취소. 수집 배치 취소 아님 |
| 게시 영향 | publication-impact GET | 해당/전체 규칙 집계·OFF 영향·최신 QA·차단 사유. 승인 토큰이나 scope 고정 아님 |

이 최초 화면 증분에서는 기존 v1/v2 응답, DAO/Mapper/DB schema, migration, QA worker 설정을 변경하지 않았다. 이후 V76/V77 연결 화면에는 명시적 개별 확인을 요구하는 게시 요청이 추가됐으며 기존 데이터 처리·worker 설정 변경은 여전히 호출하지 않는다.

## 상태와 오류 회복

1. 화면 진입은 읽기만 한다. 입력에 parser/URL/설치 경로/임의 성공값을 받지 않는다. 저장·QA 전에 대상·버전·설정·효과와 미리 체크되지 않은 동의를 확인한다.
2. 초안 편집과 작업 사유는 탭 메모리에 유지한다. 저장 실패·409·401·403·네트워크 오류로 입력을 지우지 않으며 페이지 이탈 시 미저장 경고를 제공한다. 로그·localStorage·sessionStorage에 보관하지 않는다. 다른 탭 로그인 링크를 제공한다.
3. 같은 요청의 응답 유실 후 401/403/409는 최초 실패의 증거가 아니다. 생성·개정·QA 예약은 원래 키·본문을 유지한다. 새 입력·다른 요청으로 교체하지 않는다. 재시도 응답의 정책은 현재 상태일 수 있어 최초 버전과 동일하다고 주장하지 않는다.
4. 수정·취소는 키 없는 버전 기반 요청이다. 유실 뒤 재전송이 409여도 성공/실패를 단정하지 않는다. 정확한 정책/실행의 현재 상태를 조회하고 별도 동의 후 새 검토 기준만 채택한다. 원래 요청의 성공 여부는 미확정으로 남긴다. 입력을 유지하고 다시 조회·검토하도록 한다.
5. 후속 조회 실패가 이미 받은 저장 응답을 지우지 않는다. 정책 상세를 다시 읽을 때 이전 QA/영향은 먼저 해제하며, 일부 조회 실패는 표시하고 변경을 차단한다. QA의 `MISSING`·`NOT_RUN`·`FAILED`·`INCOMPLETE`를 구별한다. `VERIFIED`도 게시 승인이 아니다.
6. 원문/evidence JSON 전체를 렌더링하지 않는다. 상태·고정 메타데이터와 제한된 reasonCode만 `textContent`로 표시한다. HTML 삽입을 사용하지 않는다.
7. URL에는 상태 필터·페이지·정책·QA ID만 둔다. 사유/키/입력은 넣지 않는다. 링크는 네이티브 이동을 유지하며 미확정 요청 중 이동을 막는다.
8. 동의 영역은 명명한 inline section이다. 확인 취소 시 호출 버튼으로, 성공 시 받은 응답으로, 미확정 상태 채택 시 다시 조회 버튼으로 포커스를 이동한다. 자동 polling으로 입력이나 포커스를 덮어쓰지 않는다.

## 관측 수치 해석

`matchingRule`은 `allRules`의 부분집합이므로 더하지 않는다. 원문·job·plan도 다른 차원이다. `frozenCollectionPlanCount`는 누적 고정 계획이며 현재 실행 수가 아니다. 관측 시각·건수·공고별 byte 상한과 현재 외부 요청 0회를 명시한다. 동일 건수의 대상 교체까지 감지하는 승인 지문으로 사용하지 않는다.

## 검증 계획과 수용 기준

- [x] 실제 화면 이벤트와 API/DOM 대역으로 새 초안·수정·개정·QA 예약/취소·유실 재확인·현재 상태 채택·입력 보존·부분 실패·읽기 역할·반환 계약·필드 제한·포커스 검증.
- [x] Spring/Thymeleaf 실제 렌더링과 Security 3개 내부 역할/3개 외부 역할/미인증, 초기 disabled·동의·라벨·no-store·외부 script 연결 검증.
- [~] 로컬 실제 브라우저에서 합성 SSR/API 업무 흐름·반응형 확인. 실제 인증/DB/운영 검증이 아님. 자세한 실행 결과는 진행 기록에 남긴다.
- [ ] 실제 서버/DB의 정책 생성·동시 편집·QA 실행·취소·이력과 운영 역할별 브라우저 검증.
- [ ] 실제 전체 QA 증거와 게시 준비·승인·교체 transaction 및 관리자 게시 화면 연결.
- [ ] 운영 환경의 스크린리더·200/400% 실제 확대·저사양 성능 검증. CSS 리플로우를 이 결과로 대체하지 않는다.

실패 기준: 미확정 저장을 성공으로 처리, DRAFT ENFORCE 저장을 운영 적용으로 표시, 미실행·증거 누락을 QA 통과로 표시, 읽기 역할의 변경 요청, 입력/키 유실, 과거 영향 재사용, 관측 수치 합산, 실제 운영 미검증을 완료로 보고하는 경우다.

합성 브라우저 harness는 `SANEB_ATTACHMENT_POLICY_VIEW_EXPORT=true`일 때만 `AnnouncementAttachmentPolicyViewControllerSmokeTest`가 생성한 비밀정보 제거 SSR을 읽는다. `scripts/qa/attachment-policy-fixture-server.mjs`는 loopback의 임의 포트, 명시적 자산 allowlist, 메모리 API만 사용한다. 실제 인증·CSRF 검증 대체물이 아니며 서버/브라우저는 검증 후 종료한다.

운영 판단 지표는 조회/편집/QA 상태의 정확성, 중복 mutation 0, 입력 유실 0, 무승인 게시 0이다. 새 사용자 추적 분석은 추가하지 않았다. 예외: 사유·키의 브라우저 지속 저장을 하지 않아 탭 강제 종료 시 복구할 수 없다. 이탈 경고·동일 탭 유지·서버 목록/이력 재확인으로 대체하며 실제 세션 복구 E2E는 남아 있다.
