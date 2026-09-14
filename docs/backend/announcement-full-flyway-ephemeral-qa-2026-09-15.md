# 원래 Flyway 통합 시험의 임시 DB 실행

## 목적과 범위

장기 goal Gate6이 명시한 `flywayIntegrationTest`는 기존 첨부 migration17건과 다른 3개 시험이다. 기존 `SANEB_FLYWAY_INTEGRATION=false`인 Linux workflow의 첨부 DB 검증을 이 명령의 통과 근거로 계산하지 않는다.

기존 시험/검증문은 보존하고 `-PsanebFlywayEphemeral=true`를 명시한 task만 별도 loopback PostgreSQL을 생성한다. Spring Boot Flyway·Mapper·분류 golden gate 및 V69 형태→현재 migration의 제외 평문 정리 계약을 실행한다. V70 회귀의 별도 schema도 같은 임시 DB만 사용한다. 외부 DB_URL/DB_USERNAME/DB_PASSWORD를 이 모드에서 읽지 않는다. 미지정한 기존 외부 DB 시험 모드는 변경하지 않으며 운영 DB에서 시험을 실행하는 승인은 아니다.

상시 수집·재분류·첨부 worker·rollback·policy/Provider QA scheduler를 해당 JVM에서 끈다. 원래 Flyway migration 위치를 사용하고 SQL init seed는 실행하지 않는다. context 종료 시 소유한 PostgreSQL을 닫는다. 새로운 의존성·DDL·운영 코드·운영 데이터 변경은 없다.

명시 임시 task는 부모 shell의 DB/Flyway URL·계정 환경변수를 빈 값으로 덮어 fork하며, 자동 설정의 fallback URL은 loopback port1로 지정한다. 실제 연결은 전용 DataSource가 우선해야 한다. 시험에서 `inet_server_port()`와 소유한 PostgreSQL port의 일치를 검증한다. 이 설정은 부모 shell·저장된 운영 설정·기존 외부 DB 모드를 바꾸지 않는다.

최초 임시 모드는 빈 Flyway URL 지정 때문에 bootstrap에서 실패했다. Spring Boot3.4.5 소스의 DataSource 선택 순서를 확인하고 전용 `@FlywayDataSource`를 지정했다. 이후3시험 중2개 통과/1개 실패를 실제 확인했다. 실패는 legacy backfill 행을 요구하는 기존 시험에 빈 DB를 제공한 fixture 차이였다. V62까지 실제 migration → 다섯 기존 카테고리의 숨김 DRAFT 생성 → Spring Boot의 V63~현재 migration 순으로 준비한다. 결과 assignment를 직접 삽입하거나 기존 양성 assertion을 제거하지 않는다. 다섯 primary backfill·지원형태 추정0·현재 migration checksum/pending0 검증도 추가했다.

## 검증 체크리스트

- [x] 원래 요구 명령과 기존 CI 검증 범위의 차이를 확인했다.
- [x] 임시 모드에서 기존3시험을 생략 없이 실행했다. 마지막 실행41초 성공, XML3통과/실패·생략0, assertion 실행6.046초다.
- [x] 실제 실패의 DataSource 선택과 legacy fixture 계약 차이를 해결했다. 기존 assertion을 삭제하거나 약화하지 않았다.
- [x] 마지막 전용 실행 후 해당 시간대의 Java/PostgreSQL/pg_ctl 잔존 없음과 context 정리를 확인했다.
- [~] Linux workflow에 같은 명령과 XML artifact를 추가했다. 필수 보고서 검증은 이3시험의 누락·생략·실패·일부 실행을 거부한다. Node10/10 통과이며 같은 SHA Linux 실행은 남아 있다.
- [x] 전체 회귀+전용 task4분26초 성공: root2556=2299통과/257조건부 생략/실패0, QA 패키지20/20, Flyway3/3. 이후 환경 전달 차단/소유 port 검증을 포함한 마지막 `flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar`도44초에3/3 통과했다. extractor/bootJar/설치 task는 UP-TO-DATE이며 새 실행으로 세지 않는다.
- [x] 최종 Java/PostgreSQL/pg_ctl/Node 잔존 없음과 사용자 Word2개 보존을 확인했다. production JAR는 기존 SHA256 `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`다.

실행 명령:

```powershell
.\gradlew.bat flywayIntegrationTest -PsanebFlywayEphemeral=true --no-daemon --console=plain --max-workers=1
```

이 검증은 운영 migration 적용·전체 Provider·실제 파일·운영 브라우저 Gate를 대체하지 않는다.
