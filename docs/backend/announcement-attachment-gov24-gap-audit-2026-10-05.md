# 정부24 본문·첨부 경로 미완료 감사

## 기준과 판정

2026-10-05, 저장소 `23f8eae05fd42dae12c0f57a12f548d41601f5f0` 기준이다. 정부24 목록 수집 구현은 존재하지만 공식 상세 본문 보완과 첨부 발견 프로필은 미완료다. API key 설정만으로 해결되는 운영 설정 문제와 혼동하지 않는다. 전체 goal/Gate는 미완료이며 이 문서는 구현 완료 증거가 아니다.

## 현재 확인한 근거

| 항목 | 근거 | 판정 |
| --- | --- | --- |
| 공식 목록 | `Gov24PublicServiceAnnouncementSourceProviderClient`의 `/api/gov24/v3/serviceList`, 조합 검색 전달, 응답 envelope 검사 | 구현 존재; 이번 실제 인증 수집은 미실행 |
| 현재 본문 | 같은 클래스 `selectProviderItem`이 목록의 지원내용/요약을 선택 | 공식 상세 본문 전체 확보와 다름 |
| 공식 상세 API | `src/main/java`, `scripts`의 Java/Node/Python에서 `serviceDetail` 연결 없음 | 추가 구현 필요 |
| 첨부 metadata | 같은 클래스가 첨부를 제거한 raw payload와 빈 `attachments`를 반환 | 목록으로부터 첨부 수집하지 않음 |
| 첨부 발견 | `announcementattachment/discovery`에 정부24 프로필 없음 | 공식 상세·파일 경로 관측 후 구현 필요 |
| 누락 표시 | `AttachmentProviderQaPlan`이 정부24를 전체 대상에 포함하고 `PROFILE_MISSING` 반환 | 미지원 대상을 분모에서 숨기지 않음 |
| DB 코드 | V78이 실제 코드 `GOV24_PUBLIC_SERVICE`를 허용; 입력 별칭 `GOV24` 유지 | 코드 정합성이 첨부 지원을 의미하지 않음 |

검토한 테스트는 `Gov24PublicServiceAnnouncementSourceProviderClientTest`, `AttachmentProviderQaPlanTest`다. 후자의 등록 컨텍스트는 일부 설정만 포함하므로 해당 테스트의 프로필 수를 전체 지역 수로 보고하지 않는다.

## 공식 명세 재확인

- [공공데이터포털](https://www.data.go.kr/data/15113968/openapi.do)
- [공식 OpenAPI JSON](https://infuser.odcloud.kr/api/stages/44436/api-docs?1684891964110)

인증 없는 공개 명세 GET으로 확인했다. 실제 서비스 데이터 API와 파일 다운로드는 호출하지 않았다.

명세는 `serviceList`, `serviceDetail`, `supportConditions`를 정의한다. 목록에는 상세조회URL이 있고, 상세에는 서비스ID·지원내용·구비서류·온라인신청사이트URL 등의 문자열이 있다. 이번에 확인한 목록/상세 schema에는 파일 manifest가 정의되어 있지 않다. 이는 실제 공식 페이지에 첨부가 없다는 증거가 아니다. 문자열 내부 링크 포함 여부, 실제 파일 제공 경로도 아직 확인하지 않았다.

따라서 구비서류 안내를 추출 파일로 저장하거나, 빈 목록으로 `NO_FILES`를 만들거나, 온라인 신청 링크를 다운로드 링크로 추측해서는 안 된다.

## 다음 구현 및 검증 순서

1. 공식 상세 표본 확보: 서비스ID가 같은 목록·상세·공식 페이지를 대조한다. 허용 호스트/경로와 파일 소속을 확정한다. key와 원본 개인정보는 문서·로그에 남기지 않는다.
2. 제목 통과 후 상세 본문 보완: 공식 상세 API 연결 시 기존 호출 상한에 추가 요청을 산입하고, 서비스ID 불일치·빈 응답·오류를 구분한다. 본문 snapshot/hash 및 재시도 계약을 보존한다. 목록 단계에서 모든 상세를 선제 호출하지 않는다.
3. 첨부 프로필: 실제 관측된 공식 영역과 다운로드 규칙으로 구현한다. 메뉴·관련 링크·신청 사이트를 첨부로 추정하지 않는다. 확인된 첨부 없음과 발견 실패를 분리한다.
4. 실패 독립 처리: 가능한 파일은 수집·추출·저장하고 실패 파일은 사유를 별도로 남긴다. 일부 성공을 전체 첨부 검증 완료로 승격하지 않는다.
5. 격리 worker/DB/API 검증 후 정확한 운영 단건 범위를 승인받아 관리자 화면까지 확인한다. COLLECT_ONLY 결과를 판정 적용·DRAFT 생성 성공으로 보고하지 않는다.

성공 기준: 동일 서비스의 제목→본문→발견한 첨부→파일별 추출/오류→DB/API/관리자 화면 근거가 이어지고, 첨부 없음·미지원·부분 실패가 구별된다. 실패 기준: 임의 링크 탐색, 합성 응답을 실수집으로 보고, 전체 대상에서 정부24 제외, 정상 후보 자동 활성화, 운영 설정·정책 무승인 변경.

## 범위와 미실행

실행 명령:

```powershell
.\gradlew.bat :test --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --tests '*AttachmentProviderQaPlanTest' --offline --no-daemon --console=plain --max-workers=1
git diff --check
```

결과: Gradle 45초 성공. XML 기준 목록 제공자40건·QA 계획6건, 총46건 통과/실패0/오류0/생략0. 합성 입력과 등록 계약 검증이며 실제 정부24 수집 증거가 아니다. Node 단회 실행으로 XML 건수를 확인했고 프로세스는 종료했다. 단회 Gradle 실행도 종료했다. 앱 코드 변경이 없으므로 bootJar/전체 회귀/운영 배포는 실행하지 않았다.

이번 작업은 공개 명세·로컬 코드 감사와 오프라인 회귀에 한정한다. 운영 DB/정책/worker 설정, 양평 단건 수집 예약, 배포, 브라우저 조작은 하지 않았다. 양평 SRC-017546 수집 승인 대기는 정부24 구현 누락과 별개다.

## 후속 증분: 공식 상세 응답 검증 모듈

`Gov24ServiceDetailResponse`를 추가했다. 공식 상세의 단건 응답(page=1/perPage=1)을 검사하고 요청 서비스ID와 응답 서비스ID의 일치를 요구한다. 빈 결과, 잘못된 건수/형식, 다른 서비스, 빈 제목을 성공 본문으로 바꾸지 않는다. 구비서류 안내·온라인 신청 링크·지원내용은 별도 필드이며 파일 descriptor를 만들지 않는다. 오류와 `toString`에는 원격 텍스트/URL을 복사하지 않는다.

이 모듈만 추가한 커밋 `21451cd`에서는 HTTP 호출과 서비스 흐름에 연결하지 않았다. 아래 후속 연결 증분과 구분한다. 운영 활성화는 별도다.

검증 명령:

```powershell
.\gradlew.bat :test --tests '*Gov24ServiceDetailResponseTest' --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --offline --no-daemon --console=plain --max-workers=1
```

26초 성공. XML 기준 상세 응답13건·기존 목록40건, 총53건 통과/실패0/오류0/생략0이다. 합성 입력 검증이며 Node와 단회 Gradle은 종료했다. bootJar/전체 테스트/실제 API/브라우저/운영 배포는 이번 증분에서 실행하지 않았다.

## 후속 증분: 제목 gate 이후 상세 연결

- 내부 제공자 인터페이스의 상세 본문 기능은 기본 비활성이다. 정부24만 `GOV24_PUBLIC_SERVICE_DETAIL_BODY_ENABLED` 설정을 읽으며 기본값은 false다. 운영 설정은 변경하지 않는다.
- `AnnouncementSourceServiceImpl.selectProviderContent`는 정부24/기능 활성/분류 run 활성/제목 A 또는 대상·지원 조합 통과를 모두 만족할 때 상세를 요청한다. 가짜 지자체 ID를 만들거나 지자체 전용 DTO 검증을 완화하지 않는다.
- 공식 HTTPS 목록 endpoint가 설정된 경우에만 동일 공식 호스트의 고정 상세 경로를 호출한다. 서비스ID EQ/page1/perPage1이며 자동 재시도·다음 페이지·임의 URL 탐색은 없다. 중계 API endpoint는 미지원으로 남긴다.
- 중복 병합 후 처리 공고당 최대1개 상세 요청이 추가된다. 기능 활성화 시 승인 수집 건수만큼 추가 요청이 발생할 수 있으므로 목록 검색 요청량에 상세 요청 상한을 더해 운영 범위를 승인해야 한다. 기존 승인 범위를 자동 확대하지 않는다.
- 공식 본문 필드의 값만 문단 단위로 조합하며 시스템이 생성한 필드명, 서비스명, 신청 URL, 문의처는 본문에 섞지 않는다. 구비서류 안내는 본문 문자열이며 실제 첨부를 뜻하지 않는다. 빈 본문/100,000자 초과는 실패로 남긴다. 현재 HTTP 계층은 기존 JSON 제공자 transport를 공유하므로 이 문자 제한은 다운로드 바이트 상한을 의미하지 않는다. 운영 활성화 전 응답 바이트/시간 자원 제한 검증을 추가해야 한다.
- 성공 시 `PROVIDER_FULL_TEXT/AVAILABLE`, 실패 시 기존 요약은 보존하고 상세 availability 실패/미지원 상태를 전달한다. 기존 raw payload/hash에 상세나 첨부를 덧붙이지 않는다. content snapshot/version 처리는 기존 분류 저장 경로를 사용하나 실제 DB 상세 증분의 통합 검증은 아직 별도다.
- 실제 공고의 본문이 달라지므로 이 flag 활성화는 첨부 COLLECT_ONLY와 독립된 판정 입력 변경이다. 과거 데이터의 재처리·정책 게시·ENFORCE·DRAFT 전환은 자동 실행하지 않는다.

신규 `Gov24DetailBodyGateTest`는 본문 준비 단계의 flag/run/title gate와 실패 보존을 검사한다. private 단계 단위 검증이며 DB/E2E 증거가 아니다. 정부24 제공자 테스트는 고정 상세 경로/단건 요청/비활성 네트워크0/다른 서비스 거부/중계 endpoint 미호출을 합성 transport로 검증한다. 실제 정부24 API/파일 다운로드와 운영 브라우저는 미실행이다.

### 연결 증분 검증 결과

명령: `.\gradlew.bat :test bootJar --offline --no-daemon --console=plain --max-workers=1`

2026-10-05 12:36 KST경 14분59초로 정상 종료했다. root `:test` XML 전체4,662건 중4,263통과/399조건부 생략/실패0/오류0이다. 정부24 목록·상세 제공자43건, 상세 응답13건, 본문 준비 gate6건은 합계62건 모두 통과했다. 별도 extractor/독립 worker DB/Flyway task를 이번 명령으로 실행한 것은 아니다. bootJar 생성과 `git diff --check`는 통과했다.

긴 실행 중 같은 세션을 유지했고 thread stack에서 카탈로그→첨부 서비스→Spring 클래스 경로 파일 탐색 진행을 확인했다. 네트워크 대기나 실패로 단정하여 테스트를 중복 시작하지 않았다. 종료 후 이번 Gradle 관련 JVM 3개가 남지 않았으며 XML 집계 Node도 종료했다. 운영 배포·실제 인증 API·브라우저 검증은 미실행이다. 응답 전송 자원 제한과 실제 상세 DB 저장 검증은 다음 필수 작업이며, 현재 코드의 로컬 테스트 성공만으로 운영 활성화를 승인하지 않는다.

## 후속 증분: 상세 응답 전송 상한

앞선 `22ddcc9`의 전송 자원 제한 미완료 항목을 보완한다. 상세 조회만 `selectBoundedDetailJson` 경로를 사용하며 기존 목록 transport의 동작은 변경하지 않는다.

- 최대 다운로드 2MiB. `BoundedJsonBodySubscriber`가 수신 chunk를 복사하기 전에 남은 예산을 검사하며 초과 시 subscription을 취소한다. 초과 응답 전체를 저장한 뒤 잘라서 정상 JSON으로 취급하지 않는다.
- 전체 헤더·본문 수신 deadline은 기존 timeout 설정(최소1초), 최대10초다. headers 후 본문이 멈춰도 제한 시간을 넘겨 기다리지 않으며 pending 요청을 취소한다. 이 deadline은 JSON 파싱 CPU 시간까지 보장하는 값은 아니다.
- HTTP 자동 redirect/재시도는 추가하지 않는다. 고정 공식 endpoint 검사와 API key 비출력은 유지한다. `Accept-Encoding: identity`를 요청하며 압축 해제 경로를 새로 만들지 않는다.
- 원격 오류 메시지/URL을 예외의 원인으로 노출하지 않는다. 실패는 기존 상세 실패 경로로 전달하며 다른 서비스·이전 응답·첨부를 대신 사용하지 않는다.
- unit subscriber 검증은 정확한 경계/여러 chunk/초과 chunk 미소비·취소/실패 후 성공 전환 금지/원격 오류 비노출/중복 subscription 거부를 포함한다.
- 실제 HTTP 검증은 loopback 가짜 서버만 사용한다. 정상 JSON, 정상 헤더 후 정지하는 본문, 유효 JSON이지만 2MiB보다 큰 chunked 응답을 포함한다. 서버와 executor는 finally에서 정리한다. 정부24 운영 실요청 증거가 아니다.

실제 상세 DB snapshot/version 연결, 활성화 승인, 공식 표본·첨부 발견·운영 브라우저 E2E는 계속 미완료다. 정책 게시·ENFORCE·기존 데이터 재처리·자동 활성화는 하지 않았다.

검증 명령: `.\gradlew.bat :test --tests '*Gov24*Test' --tests '*BoundedJsonBodySubscriberTest' --tests '*AnnouncementSourceServiceImplTest' bootJar --offline --no-daemon --console=plain --max-workers=1`

첫35초 실행 후 용량 초과 fixture를 파싱 가능한 큰 JSON으로 강화하고 재실행했다. 최종28초 실행에서92건 통과/실패0/오류0/생략0이며 subscriber4건·로컬 HTTP3건을 포함한다. bootJar는 첫 실행에서 생성했고 테스트 fixture만 수정한 최종 실행에서는 UP-TO-DATE다. 전체 root 회귀는 직전 `22ddcc9`의 결과이며 이번 전송 변경 뒤 전체 재실행으로 과장하지 않는다. 독립 DB/Flyway/실제 정부24/운영 브라우저는 미실행이다. Node·단회 Gradle·가짜 HTTP 서버·executor를 종료했다.

## 후속 증분: 임시 PostgreSQL content version 저장 검증

`FlywayMigrationIntegrationTest.gov24DetailContentVersionsKeepSummaryProvenanceAndDeduplicateIdenticalBody`를 추가했다. 기존 소유 임시 PostgreSQL이 존재할 때만 실행하며 외부 DB 실행 모드에서는 이 사례를 생략한다. 실제 Flyway schema·Spring persistence service·DAO·Mapper로 다음을 검증한다.

- 동일 목록 원문 hash에서 목록 요약과 정부24 상세 본문이 다른 content version으로 저장된다.
- 상세 본문에 `PROVIDER_FULL_TEXT/AVAILABLE` 출처·상태가 함께 저장된다.
- 같은 상세를 다시 저장해도 content version 2개를 유지한다.
- 상세 조회 실패와 요약 fallback은 별도의 세 번째 버전이며 현재 evaluation은 `FETCH_FAILED` 한 개다.
- 목록 원문 hash가 보존되고 `announcements` 행 수는 증가하지 않는다.

합성 Provider item과 합성 분류 결과를 저장 계층에 전달하는 검증이다. 실제 정부24 API·제목 분류 엔진→수집 run→snapshot refresh 전체 흐름·관리자 UI를 실행한 것은 아니다. 특히 persistence service는 본문 content version과 판정 projection을 담당하며 목록 snapshot 본문 갱신은 상위 수집 service의 별도 책임이다. 따라서 정부24 전체 DB/E2E 완료로 승격하지 않는다.

실행 명령: `.\gradlew.bat flywayIntegrationTest -PsanebFlywayEphemeral=true --offline --no-daemon --console=plain --max-workers=1`

첫 실행50초 성공, XML4건 통과/실패0/오류0/생략0이며 정부24 신규 사례가 실제 실행됐음을 확인했다. 해당 task는 부모 DB 환경변수를 비우고 소유 loopback DB를 사용하며 운영 worker/배치를 비활성화한다. Node 집계 후 Java/PostgreSQL 프로세스가 남지 않았다. 운영 변경·실제 정부24 요청은 없다.

SQL 주석 정리 후 최종 재실행도51초 성공했다. XML4건 통과/실패·오류·생략0을 재확인했고 Node·Java·임시 PostgreSQL을 정리했다. 이번 변경은 테스트·문서뿐이며 앱 JAR·migration·API·운영 설정 변경은 없다.

## 후속 증분: 수집 service에서 snapshot까지 연결

`gov24CollectionRunRefreshesSnapshotAndRetainsPreviousContentVersion`은 소유 임시 PostgreSQL에서 실제 수집 service·분류 coordinator/engine·활성 규칙 조회·DAO·Mapper·transaction manager를 연결한다. 외부 제공자만 합성 목록/상세 응답이며 네트워크를 호출하지 않는다. 규칙 seed는 임시 DB 안에서 기존 Golden Gate/게시 service로 활성화한다. 이 동작은 운영 정책 게시가 아니다. 첨부 intake는 이 사례의 연결 대상이 아니므로 상시 worker E2E로 계산하지 않는다.

신규 수집→목록 hash는 같고 상세 본문만 변경→동일 상세 재수신의 세 실행을 검증한다. snapshot은 최신 상세로 바뀌고 원문은 한 개, content version은 두 개, 마지막 실행은 중복이며 상세 요청은 실행당1개다. 목록 raw hash는 보존한다.

첫 컴파일에서 DAO 패키지명 오기를 수정했다. 다음 실제 DB 실행에서는 `selectExactSourceAcrossProviders`의 null 게시일 조건에서 PostgreSQL이 매개변수 자료형을 추론하지 못하는 오류를 발견했다. fixture에 임의 날짜를 채우지 않고 독립 null 검사 매개변수에 text/date CAST를 추가했다. URL·게시일이 모두 없는 경우도 직접 Mapper 호출로 검증한다. 중복 판정 의미·기존 컬럼·migration·API 계약은 변경하지 않는다. 이 오류는 기존 공통 중복 검사 SQL에 있으며 정부24 전용 SQL이 아니다.

운영의 실제 발생 여부는 이번 임시 검증으로 단정하지 않는다. 실제 정부24 API/첨부 발견/상시 worker/운영 관리자 화면·정책 적용/공고 DRAFT 전환은 여전히 미완료다.

최종 검증:

- `flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar --offline --no-daemon --console=plain --max-workers=1`: 55초 성공, 임시 PostgreSQL5건 모두 통과/생략0. 신규 실제 수집 service 경로 사례도 실행됐으며 bootJar를 생성했다.
- `:test --tests '*MigrationContractTest' --tests '*AnnouncementSourceServiceImplTest' --tests '*Gov24*Test' --offline --no-daemon --console=plain --max-workers=1`: 25초 성공,188건 통과/실패0/오류0/생략0.
- `git diff --check` 통과. Node·Gradle JVM·임시 PostgreSQL 종료. 이번 공통 SQL 수정 후 전체 root 회귀/실제 운영 DB/브라우저는 재실행하지 않았다. 최종 운영 반영 전 Linux CI·배포 검증은 별도로 필요하다.
