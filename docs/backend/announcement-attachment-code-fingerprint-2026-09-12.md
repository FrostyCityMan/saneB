# 첨부 정책 QA 코드 지문과 게시 전 검증 경계

## 발견한 문제와 수정 범위

이전 `AttachmentPolicyValidationSnapshotFactory.selectCodeHash()`는 수동으로 나열한 일부 클래스/Mapper/migration만 읽었다. 목록 밖 worker·저장 서비스·다른 Mapper가 바뀌면 QA 코드 지문에 그 변경이 반영되지 않을 수 있었다. 또한 정책 게시의 마지막 설치 지문/fixture 확인이 DB 쓰기 잠금 안에서 수행됐다.

현재 수정은 **현재 빌드의 전체 애플리케이션 class/resource 목록 및 실행 바이트 대조**, **파일/QA 검증과 게시 DB transaction 분리**다. Provider/worker DB QA 실행기를 추가하거나 `MISSING`을 `VERIFIED`로 바꾸는 수정이 아니다. 기존 `/api/v1`·v2 요청/응답과 V1~V77 migration은 변경하지 않는다.

## 빌드와 실행 계약

1. Gradle `generateAttachmentQaCodeCatalog`는 `compileJava`·`processResources` 이후 현재 두 출력 디렉터리의 모든 파일을 열거한다. 이름순으로 classpath 상대 경로·크기·SHA-256을 저장하고 중복/빈 목록/10000파일 초과를 거부한다.
2. `attachment-qa-code/catalog.json`은 별도 생성 디렉터리의 main output으로 포함한다. 자기 자신은 목록에 넣지 않아 순환 hash를 만들지 않는다. 웹 `bootJar`와 독립 QA JAR은 같은 목록을 사용한다. 이 파일은 관리자/외부 제출 입력이 아니다.
3. `AttachmentApplicationCodeFingerprint`는 고정 classpath 목록을 읽고 모든 실제 class/resource의 길이와 SHA-256을 대조한 뒤 목록 바이트의 SHA-256을 반환한다. 누락·변경·중복·상한·상대 경로 이탈·URL·미정의 JSON 필드·중복 JSON key·후행 JSON을 거부한다. 파일시스템 경로나 네트워크 URL을 입력받지 않는다.
4. 상한은 목록2 MiB, 개별 파일64 MiB, 합계256 MiB,10000파일이다. stream으로 읽으며 원문을 로그/응답에 출력하지 않는다. DB transaction 중 호출하면 `QA_CODE_TRANSACTION_ACTIVE`로 거부한다.
5. `selectRuntime()`가 기존 Linux/추출기/suite 지문과 새 executionCodeHash를 transaction 밖에서 검증한다. `selectSnapshot()`은 읽은 지문과 DB 입력만 결합한다. QA 내부 입력의 schemaVersion은2이며 `installed.executionCodeHash`와 상위 `executionCodeHash`를 동일하게 고정한다. 과거 schema1 QA를 현재 게시 성공으로 재사용하지 않는다.

이 지문은 애플리케이션 클래스/리소스의 현재 빌드·classpath 일치를 검사한다. 외부 프레임워크/DB driver JAR 전체의 실행 바이트나 JVM 모든 파일을 검사하는 도구는 아니다. 추출기/JDK 일부·parser 라이브러리는 기존 runtime identity가 담당하고, 전체 배포 artifact·의존성 및 동일 SHA 운영 확인은 별도 필수 Gate다. 지문 존재를 실제 QA 성공으로 간주하지 않는다.

## 게시 순서

- 기존 멱등 요청은 원래 영수증만 반환한다.
- 설치/코드 지문을 읽고 짧은 REPEATABLE READ에서 정책·scope·최신 QA/단계·snapshot을 준비한다.
- transaction 밖에서 실제 QA 근거를 검증하고 설치/코드 지문을 다시 대조한다.
- EXCLUSIVE NOWAIT 경계 안에서 같은 요청의 선행 영수증을 먼저 확인하고, 현재 정책·scope·최신 QA/단계 및 snapshot hash/JSON이 검증 당시와 같은지 재확인한다.
- 동일할 때만 기존 V77 DB 계약으로 영수증·이전 정책 퇴역·새 정책 게시·감사 metadata를 원자적으로 저장한다. DB 입력이 바뀌면 검증 지문을 재사용하지 않는다.
- 잠금 밖 검증 중 같은 요청이 먼저 성공한 경합은 actor/정책/요청 hash를 확인하여 원래 영수증을 반환한다. 다른 요청의 성공으로 대체하지 않는다.

설치 artifact를 실행 중 제자리 교체하지 않는 기존 배포 운영 전제는 유지한다. 파일시스템과 DB를 하나의 원자 transaction으로 묶었다고 주장하지 않는다. 실제 Linux·PostgreSQL의 지연/경합·실제 정책 게시 검증은 미완료다.

## 검증 범위

- `AttachmentApplicationCodeFingerprintTest`: 실제 현재 classpath 목록 대조와 worker/Mapper 변경, 누락·형식·경로·크기·transaction 거부.
- `AttachmentPolicyValidationSnapshotFactoryTest`: 코드 변경 시 snapshot 무효화, 지문 없는 입력 거부, 실제 빌드 목록 조회.
- `AnnouncementAttachmentPolicyPublicationServiceTest`: transaction 밖 runtime/verifier 호출, 잠금 뒤 DB 단계/snapshot 변경 차단, 설치/코드 변경 차단, 선행 동일 요청 영수증 복구. transaction 표식은 테스트용 대역이며 실제 PostgreSQL 검증이 아니다.
- `AttachmentContractQaPackageTest`: 현재 main class/resource 전체 목록과 웹/독립 QA JAR의 동일 목록·각 파일 크기·SHA 대조.
- 실제 실행 명령·집계·초기 실패/수정 내역은 `announcement-attachment-end-to-end-progress-2026-09-09.md` 최신 기록을 따른다. 전체 Gate는 미완료다.
