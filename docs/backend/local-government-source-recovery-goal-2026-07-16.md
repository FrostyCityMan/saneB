# 지자체 공고 수집 잔여 출처 복구 Goal 프롬프트

## 1. Objective

saneB 프로젝트에서 중랑·금천·성남·안양·속초·창원의 현재 공식 공고 목록을 안정적으로 수집하도록 복구하고, 강동·부산 남구·해운대·정읍·임실의 전송 계약을 보정한다. 기존 최근 1년 필터, 운영자 승인, 출처 OFF 정책과 `/api/v1` 계약은 유지한다.

## 2. Context to inspect first

- [ ] `AGENTS.md`를 읽고 준수한다.
- [ ] `docs/backend/db-model-v1.md`, `docs/backend/api-contract-v1.md`를 확인한다.
- [ ] `V29`부터 최신 Flyway 지자체 migration을 확인한다.
- [ ] `LocalGovernmentNoticeCollector`와 `LocalGovernmentNoticeParserFullQaTest`를 확인한다.
- [ ] `FlywayMigrationIntegrationTest`, `MigrationContractTest`를 확인한다.
- [ ] 배포 workflow와 운영 Flyway 설정을 확인한다.

## 3. Constraints

- 기존 `/api/v1` 계약을 깨지 않는다.
- additive Flyway migration만 추가한다.
- 등록일이 아닌 신청기간을 등록일로 저장하지 않는다.
- 최근 1년 필터를 완화하지 않는다.
- 모든 출처는 운영자 승인 전까지 OFF를 유지한다.
- 자동 수집 데이터를 자동 활성화하지 않는다.
- MyBatis `SELECT *`, `${}`와 Thymeleaf `th:utext`를 사용하지 않는다.
- 운영 secret과 개인정보를 코드, 문서, 로그에 기록하지 않는다.

## 4. Step-by-step checklist

- [ ] 중랑구 공식 공고/고시 URL의 최신 행, 등록일 열, 상세 링크를 검증한다.
- [ ] 금천구 공식 고시·공고 URL의 최신 행, 등록일 열, 상세 링크를 검증한다.
- [ ] 성남·안양·속초의 TLS 실패를 공개 수집 endpoint와 요청 방식으로 분리한다.
- [ ] 창원의 현재 공식 고시공고 메뉴 URL을 검증한다.
- [ ] 신규 파서 추가 전 기존 공통 프로필 재사용 가능 여부를 우선 검증한다.
- [ ] V55에서 보정 대상 11곳의 URL, 수집 endpoint, 파서 또는 요청 정책을 보정한다.
- [ ] 구조 변경 6개 출처와 전수 QA에서 확인된 전송 보정 5개 출처를 `VERIFIED/READY/OFF`로 유지한다.
- [ ] 대상 URL이 기존 공통 프로필로 통과하면 불필요한 신규 프로필을 추가하지 않는다.
- [ ] migration 계약 테스트와 PostgreSQL Flyway 기대값을 보강한다.
- [ ] DB/API 문서의 최종 QA 상태를 갱신한다.
- [ ] 대상 11곳 QA를 실행한다.
- [ ] 244곳 전수 QA를 실행한다.
- [ ] 전체 테스트, `bootJar`, Backend Gate를 실행한다.
- [ ] 변경 파일만 한국어 메시지로 커밋하고 `master`에 푸시한다.
- [ ] GitHub Actions/CodeDeploy 성공과 운영 health를 확인한다.
- [ ] 운영 관리자 화면에서 보정 대상 11곳의 `READY/OFF` 상태를 확인한다.

## 5. Success criteria

- [ ] 대상 11곳이 제목, 최근 등록일, 안전한 상세 URL을 추출한다.
- [ ] 전수 QA 결과가 244곳 모두 PASS다.
- [ ] Flyway V55 적용 후 `VERIFIED 244`, `CHECK_REQUIRED 0`, `FAILED 0`, `OFF 244`다.
- [ ] 전체 테스트, `bootJar`, Backend Gate가 성공한다.
- [ ] 운영 배포와 health check가 성공한다.
- [ ] 다른 출처, API, 수집 승인 정책에 회귀가 없다.

## 6. Failure criteria

- [ ] 출처를 migration에서 자동 ON 처리한다.
- [ ] 최근 1년 필터를 제거하거나 완화한다.
- [ ] 신청기간을 등록일로 오인해 저장한다.
- [ ] 범용 휴리스틱을 무제한 확장해 다른 기관 링크를 허용한다.
- [ ] 기존 v1 API를 변경한다.
- [ ] 테스트 또는 실사이트 QA 실패를 남긴 채 배포 완료로 보고한다.

## 7. Progress rules

- `[x]` 완료, `[~]` 진행 중, `[ ]` 대기, `[!]` 차단으로 표시한다.
- 각 Gate가 통과된 뒤에만 다음 단계로 이동한다.
- 실사이트 결과가 설계와 다르면 구현을 강행하지 말고 원인과 대안을 기록한다.
- 기존 사용자 변경 파일은 되돌리거나 커밋하지 않는다.

## 8. Final validation

```powershell
$env:SANEB_LOCAL_GOV_PARSER_QA_CODES='LGS-000008,LGS-000019,LGS-000026,LGS-000034,LGS-000036,LGS-000089,LGS-000094,LGS-000122,LGS-000167,LGS-000174,LGS-000224'
.\gradlew.bat localGovernmentParserQa --rerun-tasks --console=plain --no-problems-report

Remove-Item Env:SANEB_LOCAL_GOV_PARSER_QA_CODES -ErrorAction SilentlyContinue
.\gradlew.bat localGovernmentParserQa --rerun-tasks --console=plain --no-problems-report
.\gradlew.bat test bootJar --console=plain --no-problems-report
powershell -ExecutionPolicy Bypass -File .\scripts\backend-gate.ps1 -SkipSmokeRun
rg -n "SELECT\s+\*|\$\{" src\main\resources\mapper src\main\java
rg -n "th:utext" src\main\resources\templates
git diff --check
```

## 9. Final report format

1. 현재 단계 / Gate 상태
2. 설계 결과
3. 변경 파일
4. DB 및 파서 변경 사항
5. 실행 명령
6. QA/테스트 결과
7. 배포 결과
8. 운영 확인 결과
9. 남은 blocker
