# 수집 공고 본문·첨부 상태 정합성 개선

## 범위와 기준

- 기준 HEAD: `4c27b0cc7f00c5fb12de2f742ffd29d45c6cc3f3`.
- 기존 Thymeleaf/Bootstrap 화면과 V2 첨부 조회 계약을 재사용한다.
- 목적: 과거 원문 링크 부재를 첨부 없음으로 오해하지 않게 하고, 메뉴·푸터가 섞인 기존 본문을 관리자 확인 후 단건 복구한다.
- 운영 DB 변경, 정책 게시, 첨부 재수집, ENFORCE, 커밋·푸시·배포는 이번 구현 실행 범위가 아니다.

## 화면과 상태 계약

1. 수집 공고 상세에서 현재 첨부 상태를 `attachment-classification` V2 응답으로 별도 조회한다.
2. 발견 완료 + 파일 0개인 경우에만 `첨부 없음 확인`으로 표시한다. 미시작·조회 실패·발견 실패·부분 처리·구버전 근거는 구분한다.
3. 처리 파일 수는 추출 성공 건수가 아니다. 원문 링크 목록은 `원문 첨부 링크 · 과거 수집 정보`로 분리한다.
4. COLLECT_ONLY 미리보기와 실제 판정 적용은 구분한다. 비활성 검수 버튼의 후속 절차를 설명하고 자동 활성화하지 않는다.
5. 목록·공고 선택 후 늦게 도착한 다른 요청의 응답이 현재 화면을 덮어쓰지 않게 한다.

## 신규 본문 정제

- 기존 지역별 검증된 본문 선택자를 우선 유지한다.
- 일반 페이지에서는 유일한 main/role=main/article 영역만 허용한다. 복수 영역 또는 식별 실패는 `BODY_SELECTOR_CHANGED`로 분리한다.
- 전체 body를 공고 본문으로 대신 저장하지 않는다. 메뉴·전역 배너·푸터를 제거하되 공고 안의 표·본문 내용은 보존한다.
- 실패 공고 때문에 전체 수집을 실패시키지 않는 기존 구조를 유지한다.
- 새로운 미지원 사이트의 본문은 정확한 선택자가 확보되기 전까지 실패로 표시될 수 있다. 첨부 없음으로 해석하지 않는다.

## 기존 본문 단건 복구

`공고 선택 → 공식 본문 조회·미리보기 → 변경 내용 확인 → 영향 확인 체크 → 같은 미리보기 적용 → 최신 첨부 근거 재확인`

### DB

신규 additive Flyway `V91__add_source_body_refresh_previews.sql`만 추가한다. 기존 migration은 수정하지 않는다.

`announcement_source_body_refresh_previews`에 요청자, 공고/기본 판정/본문/규칙 버전, 첨부 버전, 고정 본문과 SHA-256, 정제 버전, 만료 시각, 적용 영수증을 저장한다. 본문은 원문 영역에만 저장하고 감사 metadata에는 미리보기 식별자만 남긴다. DB trigger는 고정 근거 수정을 차단하고 적용 영수증은 한 번만 기록한다.

### API

기본 경로: `/api/v2/admin/announcement-sources/{sourceId}/body-refresh-previews`

| 요청 | 동작 | 원문·판정 변경 |
| --- | --- | --- |
| POST 기본 경로 | 등록된 공식 상세 주소 조회, 정제·규칙 판정, 고정 미리보기 저장 | 없음 |
| POST `/{previewId}/apply` | 현재 버전과 비교하고 고정 본문으로 새 본문·판정 버전 저장 | 해당 공고 1건 |

- 기존 수집 변경 권한과 동일한 ADMIN/OPERATOR 서버 권한 검사, 세션 CSRF, ApiResponse, no-store를 적용한다. APPROVER는 본문 복구 변경 권한에 포함하지 않는다.
- 적용은 외부 페이지나 파일을 다시 요청하지 않는다. 같은 미리보기 재시도는 저장된 영수증을 반환한다.
- 미리보기 유효기간은 30분. 다른 요청자·공고·규칙·본문·첨부 버전, 정제 버전 또는 해시 불일치는 적용하지 않는다.
- 적용 시 기존 본문 이력을 보존하고 새 기본 판정을 검수 대기로 저장한다. 이전 첨부 근거와 확정 태그의 유효성을 이어 붙이지 않는다.
- 미리보기 후에는 현재 본문·판정·정책이 바뀌지 않는다. 미리보기 행 및 감사 기록은 생성된다.

### 차단 조건

- 기본 판정이 없는 공고, 제목 제외 공고, 정부24 및 비지자체 수집처.
- 비활성/삭제 수집처, 진행 중 첨부 작업, 운영 공고 연결.
- 첨부 판정 적용이 연결된 공고: 먼저 기존 승인된 원복 절차가 필요하다.
- 본문 실패·공백·2,000,000자 초과, 적용할 본문 변경 없음.

### 제한과 후속 운영

- 미리보기 만료는 적용 금지이며 자동 삭제는 아니다. 미리보기 보관·정리 정책은 후속 과제다.
- 운영 공고 연결 또는 ENFORCE 적용 공고를 이 단건 도구로 우회 복구하지 않는다.
- 일괄 데이터 복구 및 첨부 재수집은 별도 승인·대상 확정 후 진행한다.
- 기존 저장 본문은 코드 배포만으로 바뀌지 않는다.

## 검증 계획

- 공유 첨부 요약·기존 검수/구간/복구 UI Node 회귀 테스트.
- 지역 본문 선택자/모호한 영역/메뉴 제거/통신·인코딩 회귀 테스트.
- 본문 복구 service: 외부 조회 실패 시 보존, 고정 근거 적용, 중복 적용, 버전 충돌, 다른 요청자 차단.
- Controller: 권한, CSRF, 익명 접근, wrapper/no-store, 잘못된 UUID 안내.
- Mapper 바인딩과 임시 PostgreSQL Flyway V91, 불변 근거 trigger, 새 본문 이력 보존.
- Gradle root 전체 테스트 및 bootJar. 실제 결과는 작업 최종 보고를 기준으로 한다.
- 브라우저 검증은 현재 요청에서 명시되지 않아 정책상 실행하지 않는다. 운영 UI 통과로 보고하지 않는다.

## 실제 실행 결과 (2026-10-05)

```powershell
node --test scripts/qa/collected-attachment-overview.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-segments-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs
node --check src/main/resources/static/js/saneb-collected-announcements.js
.\gradlew.bat :test --tests '*AnnouncementSourceBodyRefresh*' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*MigrationContractTest' flywayIntegrationTest -PsanebFlywayEphemeral=true :bootJar --no-daemon --max-workers=1
git diff --check
```

- Node 65개 통과, JavaScript 구문 검사 통과.
- 마지막 대상 Java 테스트 316개 통과, 실패/오류/생략 0.
- 실제 loopback 임시 PostgreSQL Flyway 검증 6개 통과, 실패/오류/생략 0. V91과 Mapper 저장·버전 비교·불변 trigger·이력 보존 포함.
- 최종 Gradle BUILD SUCCESSFUL (1분 43초), bootJar 성공. 운영 DB는 사용하지 않았다.
- 앞선 관련 Java 대상 시험은 931개 통과했으나, 이 숫자는 마지막 대상 316개와 중복되므로 합산하지 않는다.
- 별도 전체 root 회귀 `:test flywayIntegrationTest -PsanebFlywayEphemeral=true :bootJar`는 약 24분 실행 후 중단했다. 전체 회귀 통과 증거는 확보하지 못했다. 마지막 성공 명령은 전체 회귀가 아닌 위 대상 검증이다.
- 브라우저/운영 적용/기존 운영 데이터 복구는 실행하지 않았다. 커밋·푸시하지 않았다.

## 후속 운영 반영 승인

위 결과는 로컬 구현 종료 시점의 기록이다. 이후 사용자의 운영 반영 요청으로 `2815c3c`를 커밋·푸시하고 고정 태그를 배포했다. Linux 전체 테스트·빌드 성공, CodeDeploy 성공, 운영 DB V91 및 health UP을 확인했다. 기존 데이터 본문 복구·첨부 재수집·정책 게시·브라우저 QA는 실행하지 않았다. 상세 사전/사후 증거는 [운영 반영 기록](../deployment/announcement-body-refresh-release-2026-10-05.md)을 따른다.
