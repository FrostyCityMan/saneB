# 청양군 SRC-017843 본문 수집 복구

## 원인과 범위

- 대상: `SRC-017843`, 청양군청, 공식 상세 공고번호37959.
- 운영 설치 코드 `45d39ae1f0f478a5378af19486e98166e7dbeade`를 서울 서버에서 읽기 전용 재현했다. HTTP200/HTML8174바이트지만 `BODY_SELECTOR_CHANGED`로 본문을 추출하지 못했다. 게시판은 main/article 대신 구형 새올 form/table 본문 셀을 사용한다.
- 정확한 최초 수집 오류는 기존 run 항목에 남지 않았다. 위 원인은 같은 URL과 설치 코드를 이용한 재현 결과이며 과거 로그를 복원한 것이 아니다.
- 첨부 작업 상태와 본문 수집 상태는 독립적이다. 첨부 job 성공만으로 본문 또는 모든 첨부 추출 성공을 단정하지 않는다.

## 변경

1. 청양 공식 HTTPS 호스트·경로·상세 query를 검증하는 전용 본문 파서를 추가한다. 유일한 제목/본문 셀만 허용하며 첨부 표·메뉴·담당 정보는 제외한다. 모호한 구조나 빈 본문은 기존 실패 코드로 반환한다.
2. 정상 수집 실행에서 본문 오류 코드·HTTP 상태·시도/리다이렉트 횟수를 기존 run 항목 `error_message`에 저장한다. 원문·URL·예외 메시지는 추가 저장하지 않는다. 기존 오류의 소급 보정이나 복구 미리보기 실패 이력 저장은 이 범위가 아니다.
3. 기존 V1 목록에 선택 필터 `bodyFetchFailed`를 추가한다. 생략 시 기존 조회 범위, false는 오류 제외, true는 오류만 조회한다. 목록과 count에 동일 조건을 적용한다.
4. 일반 검수 탭/요약에서 본문 실패를 제외하고 ‘본문 수집 오류’ 탭에 보존한다. ‘전체(오류 포함)’도 유지한다. 현재 판정을 기준으로 하여 복구된 공고를 과거 오류 이력 때문에 숨기지 않는다.

DB/API 원칙: 신규 migration 없음, 기존 원문·분류 이력·첨부·운영 연결 삭제 없음, 기존 V1 응답/wrapper 유지, 정책·상시 worker 설정·자동 활성화 변경 없음.

## 검증 영수증

| 검증 | 결과 |
|---|---|
| 관련 Java 파서/서비스/API/화면 smoke/Mapper/migration 정적 계약 | 351건 통과, 실패·생략0 |
| Node 실제 화면 스크립트 DOM/HTTP 대역 | 11건 통과, 실패·생략0. 브라우저 QA가 아님 |
| `:bootJar` | 생성 성공 |
| 서울 실제 HTML + 새 청양 파서 | 본문718자 추출, 중첩 표0. 운영 설치 클라이언트는 동일 응답에서 기존 오류 재현 |
| 로컬 PostgreSQL/Flyway 통합 테스트 | 실행했으나 Windows Application Control이 pgcrypto.dll을 차단하여 migration 단계 실패. 테스트 본문 실행 전 중단 |

서울 임시 진단 SSM 영수증: `41013460-f3f4-4293-8660-5df37e12d382`. 운영 JAR 고정 확인 후 임시 디렉터리에 진단 클래스와 새 파서 클래스만 적재하고 종료 시 정리했다. 운영 DB 쓰기0, 서비스 재시작0. 새 클라이언트 전체의 운영 설치 검증 또는 DB 반영 성공을 뜻하지 않는다.

실행 명령:

```powershell
node --check src/main/resources/static/js/saneb-collected-announcements.js
node --test scripts/qa/collected-navigation.test.mjs scripts/qa/collected-attachment-overview.test.mjs
.\gradlew.bat --no-daemon --max-workers=1 :test --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementSourceServiceImplTest' --tests '*AnnouncementSourceControllerSmokeTest' --tests '*AnnouncementSourceBodyRefreshServiceImplTest' --tests '*AnnouncementSourceViewControllerSmokeTest' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*MigrationContractTest' :flywayIntegrationTest -PsanebFlywayEphemeral=true --tests '*FlywayMigrationIntegrationTest.bodyFailureListPartitionPreservesLegacyAndRecoveredSources' :bootJar
```

위 Gradle 결합 명령의 최종 상태는 DB 환경 오류로 실패다. 통과한 개별 task와 전체 명령 성공을 혼동하지 않는다.

첫 Linux CI `37426176730`에서는 migration이 실행됐으나 신규 합성 fixture가 `semantic_reason_code`의 NOT NULL 제약을 위반해 실패했다. 실제 V56 기본값과 동일한 `PROVIDER_TRUSTED`를 사용하도록 테스트를 정정했다. 운영 schema/제약을 완화하지 않으며 수정 SHA의 전체 재검증을 요구한다.

## 운영 Gate

- [x] 사용자가 코드 배포·서비스 재시작·SRC-017843 단건 본문/분류 버전 갱신을 승인했다.
- [ ] 고정 SHA Linux 전체 CI와 실제 PostgreSQL 통합 테스트 통과.
- [ ] 같은 SHA 코드 배포, 설치 JAR/health/기존 정책 유지 확인.
- [ ] 인증된 관리자 복구 API로 미리보기 생성·동일 미리보기 적용·결과 확인.
- [ ] 브라우저 조작은 별도 명시 승인 후에만 수행한다.

전체 재수집·다른 공고 재분류·정책 게시·자동 활성화는 하지 않는다. 복구 적용은 기존 본문 복구 API의 버전/소유자/활성 작업/운영 연결 보호를 유지하며 직접 SQL로 우회하지 않는다. 코드 롤백은 직전 정상 JAR로 가능하지만 이미 생성된 분류 이력을 삭제하는 rollback은 수행하지 않는다.
