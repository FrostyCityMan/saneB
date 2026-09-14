# 정부24 첨부 출처 코드 정합성

## 범위와 근거

- 기존 수집 Provider·원문·V70 제외 tombstone의 정부24 코드는 `GOV24_PUBLIC_SERVICE`다.
- 첨부 배치/전체 목록 API의 기존 필터 별칭은 `GOV24`다. 기존 Mapper는 이를 원문 코드와 그대로 비교하여 정부24 후보를 누락했다.
- V73 작업 `frozen_provider_code`, V74 전체 목록 항목 `provider_code` 제약에 실제 코드가 없어 정부24 고정 항목 저장도 실패할 수 있다.
- 정책 registry 검증·QA 대상 코드도 실제 Provider 코드와 일치시켜야 한다. 이것은 정부24 첨부 profile 신규 지원이나 실제 API 수집 성공을 뜻하지 않는다.

## DB/API 경계

1. V1~V77을 수정하지 않는다. V78에서 두 CHECK 제약에 `GOV24_PUBLIC_SERVICE`를 추가한다. 기존 `GOV24` 값은 이전 이력 호환을 위해 허용하되 데이터를 일괄 변경하지 않는다.
2. 기존 배치/전체 목록 `providerCodes` 입력·정규화·필터 응답·멱등 요청 지문은 `GOV24`를 유지한다. 새 API 버전이나 UI 값 변경은 없다.
3. Mapper의 바인딩된 필터 값만 `GOV24` → `GOV24_PUBLIC_SERVICE`로 변환한다. 원문 컬럼에는 함수를 씌우지 않는다. 다른 출처·시간 범위·보호 조건은 유지한다.
4. DB 후보·고정 항목·작업·profile·QA 대상은 실제 출처 코드를 유지한다. 외부 범위 집계의 providerCode만 기존 별칭으로 표시한다. 고정 분할 소속 검증도 같은 별칭 경계를 사용한다.
5. 정부24 profile이 없으면 후보를 0건으로 숨기지 않고 `PROFILE_REQUIRED`를 반환한다. 수집·게시·ENFORCE·운영 공고 활성화를 우회하지 않는다.
6. 기존 멱등 요청은 최초 결과로 재조회한다. 수정 전 정부24 0건 미리보기는 새 후보/집계 지문과 달라져 재확인이 필요하다. 고정된 이력에 대상을 자동 추가하지 않는다.

## 검증 계획과 완료 조건

Design Read: 기존 Thymeleaf/Bootstrap 관리자 화면에서 정부24의 내부 코드 차이를 사용자가 해석하지 않도록 같은 한글 이름과 정확한 준비 상태를 표시한다.

- 사용자/과업: ADMIN·OPERATOR·APPROVER의 범위 조회, 정책 상세 확인, 준비 실패 사유 확인. 기존 역할별 쓰기 권한은 유지한다.
- 위험: 이번 표기/응답 검증 변경은 R0, 연결된 대량 수집·정책 게시는 R2로 취급한다. 후보 누락과 잘못된 준비 성공 표시를 방지한다.
- 가정: 기존 PC/모바일·키보드 입력·한국어/KST 환경을 유지한다. 레이아웃·라우팅·모션·포커스·동의 방식은 변경하지 않는다.
- 적용 상태: 실제 정부24 코드의 정상 응답 수용, 과거 별칭 이력 읽기, 알 수 없는 코드 거부, `PROFILE_REQUIRED` 비활성/안내 유지.
- 검증: 기존 Node 계약/DOM 이벤트 대역 회귀와 한글 이름 검사. 이번 증분의 실제 운영 브라우저·접근성 검증은 미완료 Gate에 남긴다. 새 UI 의존성은 추가하지 않는다.

- [x] SQL 바인딩: 배치 집계/후보 및 전체 목록 digest/materialize의 동일 코드 변환·바인딩 검사 통과. SQL 실제 실행은 아래 PG Gate다.
- [x] 서비스 단위 검증: 별칭 집계, 실제 코드 후보, 미지원 profile 안내, 고정 분할 소속 검증, 기존 멱등성 보존.
- [x] 정책 단위 검증: 실제 정부24 profile 코드 수용, QA 대상 코드 일치. 가짜 profile은 테스트 안에서만 사용한다.
- [~] migration: 새 제약 정적 계약 통과. V77→V78 업그레이드·이전 checksum·fresh schema 시험은 작성했으나 실제 PostgreSQL에서 미실행.
- [!] 실제 PostgreSQL: 정부24 원문 전체 목록 고정과 실제 코드 보존, 미지원 profile의 배치 예약 차단 사례는 조건부 생략.
- [x] 로컬 회귀·bootJar·독립 QA 패키징 검사. 조건부 생략은 통과로 계산하지 않는다.

## 2026-09-12 07:36 KST 검증 결과

- 첫 표적 Gradle은 30초·213건 중 새 Mapper 검사 1건이 실패했다. SQL 주석의 `LIMIT` 단어를 실행 절로 잘못 판정한 테스트였다. 주석을 제거한 SQL을 검사하도록 수정했고 같은 6개 테스트 클래스 재실행은 25초·213건 전부 통과했다.
- 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1` 및 명령 한정 Windows-ROOT truststore 설정: 3분11초·22작업 실행 성공. root203suite/1900건=1684통과/216조건부 생략/실패0, extractor25통과, 독립 QA 실행기·패키징13통과.
- Node7개 파일 전체130통과. 정부24 실제 응답 코드 수용·과거 이력 읽기·한글 표기와 기존 오류/권한/동의/응답 유실 회귀를 검사했다. 브라우저는 실행하지 않았으며 실제 운영 UI 성공을 주장하지 않는다.
- 독립 QA 목록은191건(job168/migration2/backfill13/worker8), `INVENTORY_ONLY`·passed0이다. 이 중 정부24 DB 사례와 V78 upgrade 제약 검증은 Linux 실행 대상이지 성공 증거가 아니다.
- 웹 JAR SHA256 `adbcf938672b2be359486f7b01430417c01d6587bd8f11bc7d49844e41b1f925`. 코드 목록1440파일/8,670,543byte의 웹/QA 바이트 일치 검사는 통과했다. 실제 배포 지문은 아니다.
- V1~V77의 기존74개 migration 파일 묶음 해시 `6decbc8fdf132a52f92a5bc1f5f3ce8a99e0bcf5918f130eaafa499a086ecc8a`가 작업 전후 같다. V78 외 기존 migration 수정 없음.

이번 변경은 코드/계약 정합성 수정이다. 정부24 API 설정·실제 첨부 제공 방식·Linux worker·운영 배포·브라우저 검증은 별도 필수 Gate로 유지한다.
