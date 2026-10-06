# 수집 공고 전환 중심 화면 적용

## 범위와 기준

사용자가 전환 중심 HTML 목업을 승인하고 구현·적용을 요청했다. 기준 HEAD: `3ab07e7baed2c391e1e6a21a208ce981960316d0`.

- 기존 Thymeleaf·공통 사내비 CSS 사용. 새 라이브러리 없음.
- 기본 `/app/admin/collected-announcements/{sourceId}/attachments`는 간략한 자료·분류·검수 사유·초안 설정을 표시한다.
- 제목·본문 180자 발췌, 선택 열람 본문/첨부만 노출한다. 발췌는 생성 요약이 아니다.
- 문서 역할·실패 이력·복구 도구는 기본 화면에서 제거한다. 기존 기능과 회귀 테스트는 같은 경로의 `?tools=true` 유지보수 보기에 보존한다. 일반 화면에는 도구 진입 링크를 두지 않는다.
- 한 번 확인하면 기존 검수 확인 API → 최신 확인 ID·버전 검증 → 기존 비활성 초안 API를 호출한다. 원자적 단일 API가 아니다.
- 초안 실패 후 검수는 유지하고, 현재 저장된 확인이 있으면 초안만 재시도한다. 응답 유실 시 원래 payload와 확인 멱등키를 유지한다.
- 실패·불확실 자료에 대한 서버 필수 확인 항목과 수동 원문 확인은 필요한 경우에만 노출하며 생략하지 않는다.
- 공고 입력 링크는 생성된 announcementId로 기존 입력 화면의 상세 GET을 호출한다. 자동 저장·공개 없음.
- DB/API 계약·migration·정책·worker·수집 설정 변경 없음. 기존 사용자 미추적 파일은 포함하지 않는다.

## 수용 기준

- [x] 승인된 기존 디자인을 사용하고 전환 행동을 중심에 배치
- [x] 버전/역할/필수 근거/불확실 응답/중복 클릭 방어의 Node 테스트
- [x] SSR 권한·템플릿 15건 실패·생략 0, bootJar 성공
- [ ] 동일 SHA Linux 전체 CI 및 임시 DB 계약
- [ ] 운영 사전 진단·배포·health·자산 동일성 확인
- [ ] 브라우저 QA: 사용자 현재 요청에 명시되지 않아 정책상 미실행

## 검증 및 복구 계획

Node `attachment-conversion.test.mjs`와 기존 첨부 UI 회귀 테스트를 실행한다. SSR 테스트는 기본 화면과 별도 유지보수 보기 모두 실행한다. Linux CI와 배포 workflow에도 새 Node 테스트를 필수로 연결한다.

배포는 고정 SHA 태그에서 `attachment_qa=false`로 수행한다. 업무 데이터 변경·테스트 초안 생성·재수집·정책 활성화는 하지 않는다. 직전 운영 코드 `37063cc5cce357606eb9d758e2df03245c89ed48` 및 기존 backup/rollback hook을 복구 기준으로 사용한다.

운영 health와 공개 정적 자산 비교는 HTTP 읽기 전용 검증이며, 실제 관리자 클릭이나 반응형 검증을 대체하지 않는다. 최초 3차 판정이 미적용인 공고는 기존 서버 정책에 따라 계속 차단된다.

## 사전 검증 결과

- Node 신규 전환 13건, 기존 첨부 UI 75건 통과. 생성/유실 재시도/부분 저장/버전 변경/권한/중복 클릭/공고 입력 딥링크를 포함한다.
- `gradlew.bat :test --tests '*AnnouncementAttachmentViewControllerSmokeTest' --tests '*AnnouncementConversionViewSmokeTest' :bootJar --no-daemon --console=plain --max-workers=1`: 성공, HTTP 15건.
- 사전 진단 GitHub run `37403793751`: 성공, V91·migration 실패 0, active/waiting 첨부 작업 0, COLLECT_ONLY 개정2 유지, 업무 쓰기 0. 9월 미종결 일정 1건은 연결 run 없음인 기존 상태이며 정상 종료로 표시하지 않는다.
- 설치 JAR과 직전 배포 원본 SHA-256 `d67c4830cf60efa999c7499c1af716a71c3bfbdf4a517ec8b2a61e1691056681` 일치.
