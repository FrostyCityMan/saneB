# 공고 첨부파일 수집·텍스트 추출 상세 설계

- 작성일: 2026-09-08
- 상태: 상세 설계 초안 작성·자체 검토 완료. 구현·외부 파일 실증·운영 적용 전
- 저장소 기준선: `master`, `61223aef8db0118cf32d01e4fac16d41b0041868`, 최신 Flyway `V71`
- 적용 제품: 사내비 / 내부 프로젝트: saneB
- 요청 범위: PDF·HWP 등 첨부파일 수집·다운로드·텍스트 추출의 설계. 이번 작업은 Markdown 문서만 변경한다.
- 관련 기준: [기존 분류 V2 설계](announcement-classification-v2-design-2026-08-11.md), [DB 계약](db-model-v1.md), [API 계약](api-contract-v1.md), [검증 계획](announcement-attachment-qa-plan-2026-09-08.md)
- 2026-09-14 후속 확정: [제목·본문·첨부 3단계 자동 필터링과 최종 관리자 검증](announcement-three-stage-filtering-workflow-2026-09-14.md). 이 문서 상단의 날짜·승인/Gate는 최초 설계 시점 기록이다. 현재 구현/검증 상태는 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다.

## 1. 단계와 Gate

| Gate | 상태 | 완료 조건 |
|---|---|---|
| D0 현재 계약 확인 | [x] | AGENTS, Flyway, Provider, 분류 엔진, 전환 Service, 화면·테스트 확인 |
| D1 상세 설계 | [x] | 수집·추출·DB·API·분류·운영·검증 계약 및 자체 검토 완료 |
| I0 구현 착수 | [ ] | 사용자의 다음 구현 지시. 미정 항목은 아래 설계 기본값으로 구현 가능 |
| I1 로컬 구현 검증 | [ ] | 단위·계약·보안·실제 PostgreSQL 검증과 빌드 |
| Q1 출처별 격리 QA | [ ] | 사이트별 링크 발견·다운로드·PDF/HWP/HWPX 추출 실증 |
| R1 운영 수집 | [ ] | 배포·출처별 활성화 범위 확정, 런타임 자원·보안 Gate 통과 |
| R2 판정 적용 | [ ] | 확장 API·전환 차단·관리자 화면 준비, 미리보기 비교 통과 |
| R3 기존 원문 적용 | [ ] | 대상 고정 → 첨부 수집 → 판정 미리보기 → 명시적 적용 |

이 문서는 기존의 첨부파일 제외 정책을 **향후 확장 범위에 한하여 변경하는 제안**이다. 기존 모드의 실행 계약은 유지하며, 설계 작성 자체로 다운로드·규칙 활성화·재분류를 시작하지 않는다. 과거 문서의 운영 수치는 현재 운영 증거로 사용하지 않았다.

## 2. 목표·범위·설계 기본값

### 2.1 이번 설계의 핵심 결정

1. 기존 제목 사전 판정을 통과한 공고만 상세·첨부를 요청한다. 제목 B 또는 대상+지원유형 미충족으로 제외된 공고는 신규 첨부 행·작업도 만들지 않는다.
2. 사이트별 **발견·다운로드 어댑터**와 파일 형식별 **공통 추출기**를 분리한다. 기관 수만큼 PDF/HWP 추출기를 복제하지 않는다.
3. 1차 구현 대상은 텍스트형 PDF, HWP 5.x, HWPX다. 스캔·이미지·혼합 문서는 OCR 필요 상태를 식별한다. OCR 실행은 후속 단계다.
4. 기본 수집 대상은 해당 공고에 직접 연결된 지원 형식 첨부 전체다. 본문이 있다는 이유로 첨부 수집을 생략하지 않는다. 파일 수·용량 제한 초과는 부분 처리로 표시한다.
5. 파일명과 URL은 다운로드 식별용이다. 공고 제목이나 분류용 본문에 섞지 않는다.
6. 본문·첨부에서만 발견된 A/B는 검수 근거다. 첨부 B만으로 자동 제외하지 않는다. 제목 B 우선과 제목 제외 원문 비저장을 유지한다.
7. 파일 원본은 제한된 임시 작업공간에만 보관하고 추출 후 제거한다. 관리자 검수에 필요한 추출 텍스트·근거는 별도 접근제어 데이터로 저장한다.
8. 기존 제목·본문 판정과 첨부를 포함한 종합 판정을 별도 이력으로 둔다. 기존 v1 응답의 본문·사유·위치를 첨부로 위장하지 않는다.

### 2.2 비목표

- 모든 사이트·모든 문서의 100% 다운로드 또는 정확한 레이아웃 복원 보장
- ZIP 첨부 풀기, 첨부 내부의 또 다른 문서·링크 재귀 수집, 실행파일·매크로 실행
- 로그인·CAPTCHA·유료 접근·암호·문서 추출 제한 우회
- 일반 사용자 파일 다운로드 서비스, PDF/HWP 원본 미리보기, 장기 원본 아카이브
- 네이버 전자증명 자동수집, 개인 제출서류 저장소와 외부 공고 첨부의 혼용
- AI 자동 승인·탈락, 선정확률·추천점수, 운영 공고 자동 활성화, 매칭 조건 자동 입력
- 이번 턴의 코드·migration 생성, 운영 조회·수정, 커밋·푸시·배포

첨부 자체는 다운로드하되 내부 그림을 별도 수집하거나 OCR하지 않는 것이 1차 범위다. 실제 스캔 문서가 많으면 후속 OCR 없이는 검수 건수가 줄지 않을 수 있다.

## 3. 현재 저장소와 확장 지점

아래 경로는 모두 저장소 루트 기준이며 직접 확인한 코드다.

| 근거 파일 | 현재 동작 | 확장 방향 |
|---|---|---|
| `build.gradle` | Java 21, Spring Boot 3.4.5, HttpClient5, Jsoup. 문서 추출 의존성 없음 | 별도 추출 산출물에 PDFBox·hwplib·hwpxlib 버전 고정 |
| `src/main/java/com/saneb/domain/announcementsource/provider/AbstractJsonAnnouncementSourceProviderClient.java` | JSON의 첨부 필드 제거 | 기존 직렬화 유지, 별도 발견 어댑터에서 최소 descriptor 확보 |
| `src/main/java/com/saneb/domain/announcementsource/provider/content/LocalGovernmentNoticeProviderContentClient.java` | HTML만 허용, 첨부 링크 제거 후 본문 추출 | 원본 DOM에서 링크를 먼저 별도 식별하고 본문 사본에서는 계속 제거 |
| `src/main/java/com/saneb/domain/announcementsource/provider/content/PinnedProviderContentHttpTransport.java` | 검증한 공개 IP로 연결, 자동 redirect·쿠키 비활성 | 제한된 바이너리 스트리밍 전용 transport 추가 |
| `src/main/java/com/saneb/domain/announcementsource/classification/AnnouncementSourceClassificationEngine.java` | 제목 B는 제외, 본문 B는 검수 | 기존 엔진 보존, 첨부 종합 판정기로 합성 |
| `src/main/java/com/saneb/domain/announcementsource/classification/AnnouncementSourceContentHasher.java` | 첨부를 제외한 내용 hash | 기존 hash 유지, 별도 첨부 manifest/input hash 추가 |
| `src/main/resources/db/migration/V26__create_announcement_source_collection.sql` | `announcement_source_attachments`: `source_id`, `file_name`, `file_url`, `file_type_code` | 과거 링크 조회용으로 유지, 새 추출 이력과 혼용 금지 |
| `src/main/resources/db/migration/V64__create_versioned_announcement_classification_rules.sql` | `attachment_analysis_enabled=false` CHECK | 기존 규칙 모드는 그대로. 별도 첨부 정책 버전 추가 |
| `src/main/resources/db/migration/V66__create_announcement_classification_evidence.sql` | 판정 위치 TITLE/BODY, source당 current 1건 | 기존 enum·current 유지, 첨부 종합 판정·근거 별도 저장 |
| `src/main/resources/db/migration/V69__create_announcement_source_reclassification_runs.sql` | 고정된 본문 내용으로 미리보기·적용·원복 | 네트워크가 필요한 첨부 재수집은 신규 배치로 분리 |
| `src/main/resources/db/migration/V70__stop_persisting_title_excluded_sources.sql` | 제목 제외 원문 삭제 및 tombstone | 신규 하위 데이터도 비저장·삭제 경로에 포함 |
| `src/main/java/com/saneb/domain/announcementsource/service/impl/AnnouncementSourceV2ConversionServiceImpl.java` | 기존 판정 ID·version·확정 태그·검수 조건 검증 | 첨부 종합 판정과 검수의 버전 조건 추가 경로 |
| `src/main/resources/templates/app/collected-announcements.html` | v1 조회, v2 전환, 첨부 제외 안내 | 종합 조회용 v2 연결 및 파일별 상태 패널 |

### 3.1 발견한 문서·화면 불일치

- DB 문서의 5.17 첨부 표는 `source_snapshot_id/attachment_name/attachment_url`로 적혀 있으나 실제 V26은 `source_id/file_name/file_url`이다. 구현 시 Flyway를 따른다.
- 기존 분류 설계 9절 하단과 14.5 일부 예제에 자동 제외 원문 보관 문구가 남아 있다. V70과 같은 문서 7.6절의 비저장 정책이 우선한다.
- 수집 검수 화면에도 `자동 제외도 원문 ... 보존`이라는 과거 설명이 남아 있다. 첨부 UI 구현 시 `자동 제외: 건수·사유만 보관`으로 함께 정정한다.
- 기존 문서 상단의 V62/V68·운영 보류 문구는 과거 기록이다. 이번 설계의 실제 코드 기준은 V71이며 현재 운영 상태는 미조회다.

이 불일치 때문에 기존 표·문구를 그대로 복사하여 구현하지 않는다. 본 문서 연결 안내 외의 기존 구현·화면은 이번 작업에서 수정하지 않는다.

## 4. 정책 버전과 실행 흐름

### 4.1 세 가지 실행 모드

| 모드 | 첨부 네트워크 | 종합 판정 | 운영 전환 영향 |
|---|---|---|---|
| `OFF` | 새 요청 없음 | 과거 이력 조회만 | 기존 첨부 적용 대상의 검수 요구를 자동 해제하지 않음 |
| `COLLECT_ONLY` | 승인된 출처·범위만 | 저장·미리보기만 | 기존 제목·본문 판정과 전환 유지 |
| `ENFORCE` | 승인된 출처·범위만 | 신규 공고는 종합 판정을 current로 반영 | 첨부 처리·종합 검수 Gate 적용 |

출시 초기값은 OFF다. 첨부 정책 버전은 기존 키워드 release를 참조하며 `DRAFT → ACTIVE → RETIRED`로 관리한다. 신규 수집은 실행 시작 시 **키워드 release, 첨부 정책, 출처 프로필 hash, 엔진·추출기 버전**을 고정한다. 이후 규칙·정책 변경을 진행 중인 작업에 끼워 넣지 않는다.

기존 `attachment_analysis_enabled`는 제목·본문 엔진의 과거 계약으로 false를 유지한다. 새 정책의 모드와 UI 라벨은 별도이며, 기존 DB CHECK를 해제하거나 과거 release를 수정하지 않는다. 키워드 release가 교체되면 새 release에 맞는 첨부 정책 검증이 필요하며, 일치하는 정책이 없으면 신규 ENFORCE 판정을 유효 후보로 완료하지 않는다.

이 증분의 종합 판정은 **base evaluation과 같은 keyword release**만 사용한다. 기존 source의 base release가 요청한 첨부 정책과 다르면 `BASE_RECLASSIFICATION_REQUIRED`로 범위 확정 단계에서 차단하며, 첨부가 새로운 규칙의 제목 판정을 우회하지 못하게 한다. 먼저 해당 source의 제목·본문을 기존 재분류 절차로 갱신하고, 여전히 비제외인 source만 새 batch에 넣는다. 이미 첨부 ENFORCE가 적용된 source의 keyword release 변경은 12.3절의 검증된 binding 원복 → 제목·본문 재분류 → 새 첨부 정책 preview·적용 순서로 수행한다. 각 단계는 명시적 범위와 충돌 검증이 필요하며 자동 연쇄 실행하지 않는다.

### 4.2 신규 수집 흐름

```text
수집 run 및 정책 snapshot 고정
  → 제목 사전 판정
     → EXCLUDED: tombstone·건수·규칙 참조만 기록, 종료
     → 제목 A 또는 대상+지원유형 통과
        → 공식 본문 확보 + 첨부 descriptor 발견
        → 기존 source/content/base evaluation 저장 + 첨부 job 예약
        → 목록 수집 run 종료 가능 (첨부 job 완료와 구분)
        → 백그라운드 다운로드 → 격리 추출 → 파일별 결과 저장
        → 첨부 set 봉인 → 같은 규칙으로 종합 판정
        → 버전 조건 확인 → current 반영 또는 CONFLICT
        → 관리자 근거 확인·태그 확정 → 운영 공고 DRAFT 전환
```

기존 provider item의 raw JSON·본문·hash에는 첨부를 다시 넣지 않는다. 발견 결과는 별도 DTO로 전달한다. Provider 원문을 두 번 요청해야 하는 경우에는 새 job이 공식 상세/API를 재조회하며, 수집 시 API 인증정보가 들어 있던 원본 payload를 그대로 저장하지 않는다.

본문 fetch 실패여도 검증된 독립 API 첨부 descriptor가 있으면 파일 수집을 계속할 수 있다. 상세 링크 발견까지 실패하면 `DISCOVERY_FAILED`이며 `첨부 없음`으로 처리하지 않는다. 본문에 `첨부 참조`만 있는 경우 첨부의 완전한 공고문이 2차 근거를 대신할 수 있다.

### 4.3 출처별 발견 계약

- 공통 입력: `sourceId`, `contentVersionId`, `providerCode`, 시스템의 `profileCode/profileHash`, 검증된 공식 상세 경로.
- 공통 출력: 발견 상태, 발견 시각, descriptor 목록, 목록 완전성, 누락·차단 사유.
- descriptor: 임시 fetch URL, 파일 표시명, 예상 형식, 순서, 안정 file locator, 문서 역할 힌트. 인증 header/cookie는 포함하지 않는다.
- 기업마당·정부24: 제공된 API 링크 또는 공식 상세의 첨부 영역을 사용한다. 해당 채널의 key·응답 가용 여부는 별도 운영 Gate다.
- 지자체: 기존 목록 파서와 독립된 시스템 첨부 프로필을 두며, 검증한 DOM 영역과 다운로드 요청 템플릿만 사용한다.
- GET을 기본 지원한다. POST·세션 쿠키·일시 서명 링크가 필요한 출처는 전용 프로필 QA 후 허용하며 임의 JS 실행으로 해결하지 않는다.
- 파일 서버 host는 기관 host와 다를 수 있다. 시스템 allowlist에 검증된 exact host·port·path만 등록한다. 전체 `*.go.kr` 신뢰는 금지한다.
- 관리자에게 파서·CSS selector·임의 URL·HTTP header 편집 기능을 제공하지 않는다. 출처별 QA 상태만 보여준다.

파일 확장자가 없는 다운로드 URL도 대상이다. 실제 응답의 Content-Disposition, Content-Type, 파일 signature를 함께 확인하며 오류 HTML을 PDF로 저장하지 않는다. 인증 query·cookie는 메모리 내 요청 수명으로 제한하고 DB·로그·에러에 남기지 않는다. 재시도 시 안정 locator로 새 URL을 발급받는다. 안전한 locator를 만들 수 없는 출처는 `PROFILE_REQUIRED`다.

## 5. 다운로드·추출 계약

### 5.1 초기 자원 한도

다음은 설계 기본값이다. 격리 QA 결과로 정책 DRAFT에서 조정하고 version/hash를 남긴다. 값을 초과하면 조용히 자르지 않고 명시적 제한 상태를 저장한다.

관리자 정책값은 배포 환경의 hard cap보다 클 수 없다. source/host budget과 semaphore는 DB lease로 공유하여 worker 수를 늘려도 전체 동시성 한도를 우회하지 못하게 한다. 두 번째 worker를 추가하기 전에도 전역 한도 테스트가 필요하다.

| 항목 | 기본값 |
|---|---|
| 공고별 파일 수 | 10개, 초과 시 `LIMIT_EXCEEDED/PARTIAL` |
| 파일/공고별 다운로드 예산 | 20 MiB / 80 MiB. 재시도 bytes도 예산에 합산 |
| 연결 / 무응답 read / 요청 전체 시간 | 3초 / 10초 / 30초 |
| redirect | 최대 3회, 매 hop URL·DNS·목적 host 재검증 |
| 전체 / host별 다운로드 동시성 | 2 / 1, 전체 worker에 공유되는 lease·예산 |
| 추출 동시성 | 최초 전체 1개, 웹 JVM 밖 |
| 파일 추출 시간 / 프로세스 메모리 | 30초 / OS 한도 512 MiB, Java heap 256 MiB 이내 |
| PDF 페이지 / 출력 code point 수 | 200페이지 / 1,000,000자 |
| HWPX 내부 ZIP | 2,000 entry / 해제 후 합계 128 MiB / 압축비 100배 |
| 재시도 | 일시 네트워크 오류 2회 추가, 총 3회. 1분·5분 backoff와 jitter |
| 임시 공간 | 전체 1 GiB, 부족하면 신규 claim 중단 |
| 임시파일 보관 | 정상·실패 완료 시 즉시 제거, 비정상 종료 잔여물 최대 24시간 |

HWP의 record·stream 압축 해제에도 별도 해제 byte 한도를 적용한다. HWP/HWPX의 페이지 번호는 실제 레이아웃 엔진 없이 정확히 알 수 없으므로 추정 페이지를 만들지 않는다.

### 5.2 추출기

| 형식 | 후보 | 출력·한계 |
|---|---|---|
| PDF | Apache PDFBox 3.x | 페이지 순 텍스트와 page locator. 다단·표 읽기 순서 QA 필요 |
| HWP 5.x | hwplib | 문단·표 셀의 텍스트와 section/paragraph/cell locator. 암호화 파일 미지원 |
| HWPX | hwpxlib | XML의 문단·표 텍스트와 구조 locator. HWP parser에 넘기지 않음 |
| 스캔/이미지 | 후속 OCR adapter | 1차에서는 `OCR_REQUIRED`, 자동 외부 전송 없음 |

구현 시 Java 21에서의 동작, 라이선스 고지, 선택 버전의 취약점·전이 의존성을 확인하여 정확한 버전을 고정한다. 여기의 라이브러리 이름은 실파일 호환성 통과를 뜻하지 않는다.

추출 출력은 HTML이 아닌 UTF-8 plain text다. `text`, `textHash`, `blocks`, `qualityCode`, `warnings`, 처리량, extractor/version/config hash를 반환한다. 각 block에는 순서·원문 code point `[start,end)`·locator·구조 신뢰 상태만 둔다. 정규화는 기존 NFKC 정책을 재사용하되 오탈자·표 셀 의미·금액을 추측해 고치지 않는다.

`COMPLETE_TEXT`는 지원되는 텍스트 구조를 한도 내에서 처리했다는 의미다. 자격조건을 정확히 해석했다는 의미가 아니다. 일부 페이지만 텍스트가 있고 다른 페이지가 스캔인 문서, 본문 이미지의 글자 여부를 확인하지 못한 문서는 `PARTIAL_TEXT` 또는 `OCR_REQUIRED`다. 장식 이미지 제외는 검증된 프로필에 한해서만 허용한다. 빈 결과를 정상 완료로 기록하지 않는다.

### 5.3 격리·보안

- 다운로드는 기존 공개 IP pinning 원칙과 TLS hostname 검증을 유지한다. loopback/private/link-local/IPv6 우회, redirect와 DNS rebinding을 모두 검증한다.
- Content-Length만 신뢰하지 않고 streaming 실제 byte 수를 제한한다. HTTP 압축과 문서 내부 압축의 제한을 각각 적용한다.
- 추출 프로세스는 네트워크·DB·운영 secret·다른 사용자 파일 접근이 없는 전용 권한으로 실행한다. plain Java CLI 산출물을 쓰며 Spring 전체 application context를 띄우지 않는다.
- 부모의 전체 환경변수를 자식에게 상속하지 않는다. 작업 파일·형식·제한값만 검증된 IPC로 전달한다. 문자열 shell 명령을 조합하지 않는다.
- XML DTD·외부 entity를 차단하고 ZIP 경로 이탈·symlink·중첩 파일 추출을 차단한다. PDF action/JavaScript, HWP macro·외부 링크는 실행하지 않는다.
- timeout이면 작업 프로세스 트리를 종료한 뒤 임시파일을 정리한다. thread interrupt 또는 Java heap 제한만으로 격리 완료를 주장하지 않는다.
- 임시파일 이름은 서버 UUID이고 원본 filename을 경로로 사용하지 않는다. 웹 공개 디렉터리 밖에 둔다. 재시작 정리는 검증된 전용 root 내부의 만료 job 디렉터리만 대상으로 한다.
- 파서 오류·stdout·stderr·추적 로그에 문서 내용·URL query·인증정보가 섞이지 않도록 고정 에러 코드로 매핑한다. 상세 원문은 일반 감사 로그와 분리한다.
- 원본 binary 영구 보관과 사용자 다운로드 endpoint는 1차에 없다. 필요하면 보관·삭제·권한 계약을 별도 추가한다.

## 6. 분류와 문서 역할

### 6.1 역할

`NOTICE`(공고문), `GUIDE`(지원 안내), `FORM`(신청 양식), `REFERENCE`(참고자료), `UNKNOWN`으로 구분한다. 파일명만으로 NOTICE를 확정하지 않는다. 시스템의 검증된 첨부 영역·명시된 문서 구분으로 결정할 수 없으면 UNKNOWN이다. 관리자는 문서 역할을 지정할 수 있으며 변경 사유·버전·처리자를 남긴 새 판정을 생성한다. 이는 지자체 파서 선택 기능이 아니다.

- NOTICE/GUIDE: 대상·지원형태 조합과 A/B 근거를 평가한다.
- FORM/REFERENCE: 기본은 참고 근거만. 이 문서만으로 자동 ACCEPTED를 만들거나 A/B 때문에 자동 제외하지 않는다.
- UNKNOWN: 추출·키워드 근거를 보여주되 자동 유효 후보 확정에는 사용하지 않는다. 문서 역할 확인이 필요하다.
- `지원 제외 업종`, 여러 사업을 나열한 안내, 표 읽기 순서가 불명확한 내용: 부정문 의미를 자동 추측하지 않고 검수로 남긴다.

### 6.2 종합 판정 규칙

아래 규칙은 ENFORCE 대상의 종합 결과다. 기존 base evaluation은 수정하지 않는다. 주 사유는 아래 표 순서로 결정하고 나머지 경고·근거도 함께 반환한다.

| 순서 | 조건 | 종합 결과 |
|---|---|---|
| 1 | 제목 EXCLUDED | 첨부 작업 생성 금지. 기존 tombstone 경로 |
| 2 | 첨부 발견·다운로드·추출 진행 중 | `REVIEW_REQUIRED / ATTACHMENT_PENDING` |
| 3 | 발견 실패, 제한 초과, 필요한 파일 실패·부분 추출·OCR 필요 | `REVIEW_REQUIRED / ATTACHMENT_INCOMPLETE` |
| 4 | UNKNOWN 문서·모호한 구조/문서 간 충돌 | `REVIEW_REQUIRED / ATTACHMENT_CONTEXT_REVIEW` |
| 5 | 제목 A 또는 본문 A/B | `REVIEW_REQUIRED`, 기존 base 사유와 근거 보존 |
| 6 | NOTICE/GUIDE의 첨부 A/B | `REVIEW_REQUIRED / ATTACHMENT_GROUP_B_MATCHED` 또는 `ATTACHMENT_GROUP_A_MATCHED` |
| 7 | 완전한 발견 결과가 NO_FILES | 기존 본문 판정 유지 |
| 8 | 완전한 본문 또는 하나의 완전한 NOTICE/GUIDE 안에서 조합 확인, 검수 조건 없음 | `ACCEPTED / EXTENDED_TARGET_SUPPORT_CONFIRMED` |
| 9 | 그 외 | `REVIEW_REQUIRED / EXTENDED_COMBINATION_NOT_CONFIRMED` |

파일 A의 대상과 파일 B의 지원형태를 합쳐 조합을 충족시키지 않는다. 첨부의 자동 조합 단위는 같은 문단 또는 구조가 보존된 하나의 표 행이다. `blocks_json`에 `evidenceScopeId`를 두고 이 단위 안에서 기존 강약 AND를 적용한다. 문단을 가로질러야 하거나 PDF 표의 행·셀 관계를 확정할 수 없으면 검수로 남긴다. heading 추정으로 서로 떨어진 구간을 임의 합치지 않는다. 태그는 확인된 문서별 근거의 합집합으로 제공하되 실제 조건이나 가족관계를 자동 확정하지 않는다.

첨부 일치 방식은 기존 TOKEN/NORMALIZED_PHRASE와 기관명 보호 정책을 재사용한다. EXACT_TITLE은 공고 제목 전용이며 filename·첨부 본문에 적용하지 않는다. 여러 사업이 섞인 안내 문서를 한 사업으로 자동 해석하지 않는다.

첨부만으로 본문의 부족을 보완할 수는 있지만 제목 미충족·제목 B를 복구할 수 없다. 제목 A는 첨부가 정상이어도 검수다. 중요 파일 한 개가 실패했는데 다른 파일 한 개가 성공했다고 전체 성공으로 처리하지 않는다. ROLE이 FORM/REFERENCE로 확정된 파일의 실패는 참고 경고로 둘 수 있으나 발견 목록의 완전성이 확인되어야 한다.

### 6.3 사람의 검수

OPERATOR/ADMIN은 전체 원문을 직접 확인하고 실패·불확실한 항목을 특정하여 수동 검수 완료할 수 있다. 이때 자동 판정·실패 상태를 ACCEPTED/성공으로 덮어쓰지 않는다. 확인 방법·검수 사유·판정 ID·set hash·version을 고정한 별도 confirmation을 저장한다. 제목 EXCLUDED는 이 경로의 대상이 아니다.

종합 판정 또는 파일 역할·추출 결과가 변경되면 이전 confirmation은 STALE다. 이미 연결된 운영 공고의 공개 상태는 자동 변경하지 않고 재검수 필요를 표시한다.

## 7. 상태와 동시성

### 7.1 상태 축

| 축 | 코드 |
|---|---|
| 발견 | `PENDING`, `RUNNING`, `FOUND`, `NO_FILES`, `FAILED`, `UNSUPPORTED`, `PARTIAL` |
| 파일 다운로드 | `PENDING`, `RUNNING`, `DOWNLOADED`, `RETRY_WAIT`, `FAILED`, `BLOCKED`, `LIMIT_EXCEEDED` |
| 추출 결과 | `COMPLETE_TEXT`, `PARTIAL_TEXT`, `EMPTY_TEXT`, `OCR_REQUIRED`, `ENCRYPTED`, `UNSUPPORTED`, `FAILED`, `TIMEOUT` |
| set | `OPEN`, `SEALED`, `CANCELLED` |
| job | `PENDING`, `RUNNING`, `RETRY_WAIT`, `COMPLETED`, `PARTIAL_FAILED`, `FAILED`, `CANCELLED`, `CONFLICT` |
| 검수 | 기존 source 검수 상태 + 종합 confirmation `CURRENT/STALE` |

source의 `QA_BLOCKED`, 최근 목록 `LATEST_RUN_FAILURE`, 첨부 `DISCOVERY_FAILED`, 파일 추출 실패, 종합 `REVIEW_REQUIRED`는 다른 정보다. 첨부 실패로 목록 수집의 과거 성공을 실패로 바꾸지 않는다.

종합 evaluation은 SEALED set에만 생성한다. 그 전 ENFORCE 조회의 `effectiveClassification.decisionId`는 null이고 `REVIEW_REQUIRED/ATTACHMENT_PENDING`은 job에서 계산한 진행 projection이다. 이전 종합 이력이 있더라도 새 첨부 처리 중에는 `isStale=true`와 pending을 함께 반환하고 전환을 막는다. 봉인 시 `is_discovery_complete=false`인 실패 집합도 종합 INCOMPLETE 판정을 생성할 수 있다.

### 7.2 예약·lease·멱등성

- source 저장과 최초 job 예약을 같은 짧은 DB transaction에서 처리한다. 외부 HTTP/추출 중 transaction을 유지하지 않는다.
- PostgreSQL queue를 사용한다. DAO/Mapper에서 명시 컬럼 + `FOR UPDATE SKIP LOCKED`로 job을 claim하고 lease token·만료·attempt를 저장한다. Redis/Kafka는 추가하지 않는다.
- source/content/policy/generation의 UNIQUE로 중복 예약을 막는다. worker lease가 만료되어도 파일 결과 확정은 동일 lease token·상태·version CAS가 성공한 worker만 수행한다.
- 실제 외부 요청은 재시도로 중복될 수 있다. exactly-once 다운로드를 약속하지 않고, DB 결과·판정 반영을 멱등으로 보장한다.
- 완료 시 source 행을 잠그고 content ID, base evaluation ID, rule release ID, attachment version, 적용 mode binding을 재확인한다. 하나라도 다르면 `CONFLICT`이며 현재 판정·수동 검수를 덮어쓰지 않는다.
- 같은 URL의 파일이 바뀌면 binary SHA-256이 달라 새 extraction/set 이력이 된다. ETag·Last-Modified만으로 같은 내용임을 확정하지 않는다.
- 기존 내용 hash에는 첨부가 없으므로 목록 중복 조기 반환보다 첨부 갱신 예약 판단을 먼저 수행해야 한다. 동일 source의 첨부 재확인은 정책상 최소 24시간 간격이며 명시적 재시도는 별도 제한을 적용한다.
- set은 모든 파일의 성공/실패/차단이 정해졌을 때만 봉인한다. 마지막 성공 하나가 도착했다고 봉인하지 않는다. 중지 요청은 새 파일 요청을 멈추며 in-flight 결과는 저장 후 종료한다.
- set 봉인 후 파일·역할·extraction 참조를 변경하지 않는다. 재시도·역할 수정은 새 generation과 새 set을 만들고 과거 판정 입력을 보존한다.

## 8. DB 확장 계약

### 8.1 공통 원칙

아래는 설계 계약이다. 후속 구현에서 V72 DB 확장과 일부 처리 기반을 추가했으며, 전체 구현·운영 반영은 아직 완료되지 않았다. 실제 검증과 남은 항목은 [첨부 구현·출시 진행 기록](announcement-attachment-implementation-2026-09-08.md)을 따른다. 기존 V1~V71 파일·checksum·기존 API용 enum·hash는 변경하지 않는다.

신규 테이블 공통: `id uuid` PK, `created_at timestamptz NOT NULL DEFAULT now()`. 가변 작업/정책에는 `updated_at`과 `row_version integer NOT NULL DEFAULT 0 CHECK >= 0`를 둔다. hash는 소문자 SHA-256 64자리 CHECK, 건수/byte/offset은 0 이상, 종료 시각은 시작 시각 이상으로 제한한다. 아래 별도 표시가 없는 업무 필드는 NOT NULL, `?`는 nullable이다. FK 컬럼과 삭제 경로에는 인덱스를 둔다. PostgreSQL 식별자는 63byte 이내의 짧은 이름을 명시한다.

### 8.2 정책·작업

| 신규 테이블 | 핵심 컬럼 | 제약·인덱스 |
|---|---|---|
| `announcement_attachment_policies` | `policy_code varchar(40)`, `version_no int`, `policy_status_code varchar(20)`, `mode_code varchar(20)`, `rule_release_id uuid`, `policy_hash varchar(64)?`, `settings_json jsonb`, `profile_manifest_json jsonb`, `created_by uuid`, `published_at timestamptz?` | 코드·version UNIQUE, rule FK, rule별 ACTIVE partial UNIQUE. DRAFT 외 수정 금지, ACTIVE hash 필수, settings 스키마 서버 검증 |
| `announcement_attachment_batches` | `batch_type_code varchar(20)`, `policy_id uuid`, `batch_status_code varchar(30)`, `scope_json jsonb`, `scope_hash varchar(64)`, `maximum_count int`, `requested_by uuid`, `approved_by uuid?`, `scope_fixed_at timestamptz`, `preview_hash varchar(64)?`, `applied_at timestamptz?`, `reason_hash varchar(64)` | 유형 `BACKFILL/RETRY`, 정책·user FK. status/created 인덱스. 범위 고정 뒤 수정 금지 |
| `announcement_attachment_jobs` | `source_id uuid`, `content_version_id uuid`, `base_evaluation_id uuid`, `policy_id uuid`, `batch_id uuid?`, `set_id uuid?`, `generation int`, `expected_source_version int`, `expected_attachment_version int`, `job_status_code varchar(30)`, `attempt_count int`, `next_attempt_at timestamptz?`, `lease_token uuid?`, `lease_expires_at timestamptz?`, `heartbeat_at timestamptz?`, `error_code varchar(80)?`, `idempotency_key uuid`, `request_hash varchar(64)` | source/content/base의 composite FK, `(source_id,content_version_id,policy_id,generation)` UNIQUE, source별 active job partial UNIQUE, `(job_status_code,next_attempt_at,id)` 인덱스, batch/source 인덱스, idempotency key UNIQUE |

`settings_json`은 mode, 제한값, 허용 형식, 보관 기간, 추출기 식별자·정확한 버전만 허용한다. `profile_manifest_json`은 코드와 hash 목록이다. URL query, 인증정보, 임의 실행 코드, regex는 허용하지 않는다. 시스템 프로필은 코드/seed로 검증하며 정책 게시가 프로필 코드를 수정하지 않는다.

배치의 대상 목록은 생성 시점에 jobs로 materialize한다. `scope_json` 필터를 나중에 다시 실행하여 대상을 늘리지 않는다. 처음은 최대 100 source, API 상한은 1,000으로 한다. count는 job 상태 집계가 기준이며 캐시 count를 사용한다면 같은 transaction에서 갱신하고 재계산 검증을 둔다. 중복 job이 이미 있으면 해당 batch 생성은 충돌을 반환하고 암묵적으로 범위를 줄이지 않는다.

### 8.3 파일 집합과 추출

| 신규 테이블 | 핵심 컬럼 | 제약·인덱스 |
|---|---|---|
| `announcement_source_attachment_sets` | `source_id uuid`, `content_version_id uuid`, `policy_id uuid`, `data_purpose_code varchar(20)`, `discovery_status_code varchar(20)`, `set_status_code varchar(20)`, `manifest_hash varchar(64)?`, `profile_hash varchar(64)`, `discovered_count int`, `processed_count int`, `is_discovery_complete boolean`, `discovered_at timestamptz?`, `sealed_at timestamptz?` | `(id,source_id)` UNIQUE, content/source composite FK, source/time 인덱스. SEALED에 hash·sealed 시각 필수 |
| `announcement_source_attachment_files` | `set_id uuid`, `source_id uuid`, `stable_locator_hash varchar(64)`, `safe_locator_json jsonb`, `display_name varchar(500)?`, `detected_type_code varchar(20)?`, `document_role_code varchar(20)`, `role_origin_code varchar(20)`, `download_status_code varchar(20)`, `downloaded_bytes bigint`, `binary_hash varchar(64)?`, `sort_order int`, `error_code varchar(80)?` | `(set_id,stable_locator_hash)` UNIQUE, `(id,set_id,source_id)` UNIQUE, set/source composite FK, set/status 인덱스 |
| `announcement_source_attachment_extractions` | `file_id uuid`, `set_id uuid`, `source_id uuid`, `attempt_no int`, `extractor_code varchar(40)`, `extractor_version varchar(40)`, `extractor_config_hash varchar(64)`, `quality_code varchar(30)`, `extracted_text text?`, `text_hash varchar(64)?`, `blocks_json jsonb`, `character_count int`, `page_count int?`, `duration_ms int`, `error_code varchar(80)?` | file/set/source composite FK, `(id,set_id,source_id)` UNIQUE, `(file_id,attempt_no)` UNIQUE. COMPLETE_TEXT이면 비어 있지 않은 text/hash 필수, 결과는 append-only |

`safe_locator_json`은 허용된 상대 경로·비기밀 file ID·시스템 profile code만 포함한다. 모든 사용자 메시지는 한국어다.

추출 `blocks_json`은 텍스트를 중복 복사하지 않는 locator/offset 구조다. shape·code point offset 범위·block 순서를 서버에서 검증한다. 개인정보 가능성이 있는 filename·추출 텍스트는 접근제어 테이블에만 두며 감사 metadata에는 복사하지 않는다.

`data_purpose_code`는 연결된 source와 같아야 한다. 일반 API에서 받지 않으며 service 검증과 constraint trigger로 불일치를 거부한다. QA DB와 운영 DB는 별도 실행환경으로 분리한다. 단순히 purpose 컬럼만 달리해 격리 QA가 됐다고 보지 않는다.

### 8.4 종합 판정·근거·확정

| 신규 테이블 | 핵심 컬럼 | 제약·인덱스 |
|---|---|---|
| `announcement_source_attachment_evaluations` | `source_id uuid`, `content_version_id uuid`, `base_evaluation_id uuid`, `set_id uuid`, `policy_id uuid`, `rule_release_id uuid`, `engine_version varchar(40)`, `input_hash varchar(64)`, `decision_hash varchar(64)`, `decision_status_code varchar(30)`, `reason_code varchar(80)`, `warning_codes_json jsonb`, `is_current boolean`, `evaluated_at timestamptz` | source/content/base/set composite FK, `(id,source_id)` UNIQUE, source당 current partial UNIQUE, `(source_id,input_hash,engine_version)` UNIQUE. 결과 ACCEPTED/REVIEW_REQUIRED만 허용 |
| `announcement_source_attachment_evaluation_inputs` | `evaluation_id uuid`, `source_id uuid`, `set_id uuid`, `file_id uuid`, `extraction_id uuid?`, `document_role_code varchar(20)`, `input_status_code varchar(30)` | `(evaluation_id,file_id)` UNIQUE. evaluation/set/source와 file/set/source·extraction/file의 연결 일치 composite FK. 실패·차단 파일도 행 생성 |
| `announcement_source_attachment_matches` | `evaluation_id uuid`, `source_id uuid`, `set_id uuid`, `file_id uuid`, `extraction_id uuid`, `rule_release_id uuid`, `keyword_group_id uuid`, `keyword_rule_id uuid`, `keyword_term_id uuid`, `block_index int`, `start_offset int`, `end_offset int`, `applied_action_code varchar(30)` | evaluation input FK, term/rule/group/release FK, evaluation/file/term/offset UNIQUE. offset은 추출 원문 code point, action TAG/REVIEW_REQUIRED/CONTEXT_ONLY/MASK_ONLY |
| `announcement_source_attachment_confirmations` | `source_id uuid`, `evaluation_id uuid`, `set_hash varchar(64)`, `confirmed_by uuid`, `confirmed_at timestamptz`, `review_method_code varchar(30)`, `acknowledged_error_codes_json jsonb`, `review_note varchar(1000)`, `is_current boolean` | evaluation/source composite FK, source당 current partial UNIQUE. user FK. 과거 확인은 삭제하지 않음 |
| `announcement_source_attachment_tags` | `evaluation_id uuid`, `confirmation_id uuid?`, `target_category_id uuid?`, `support_type_id uuid?`, `origin_code varchar(20)` | 대상/형태 중 정확히 하나만 NOT NULL CHECK와 각 카탈로그 FK. AUTO면 confirmation NULL, CONFIRMED면 NOT NULL. 자동/확정별 중복 partial UNIQUE |

FK를 만들기 위해 각 참조 대상에 필요한 composite UNIQUE를 추가한다. 예: evaluations에 `(id,set_id,source_id)`와 `(id,rule_release_id)`, files에 `(id,set_id,source_id)`, extractions에 `(id,file_id,set_id,source_id)`, confirmations에 `(id,evaluation_id)`와 `(id,source_id)`를 제공한다. evaluation_inputs에 `(evaluation_id,file_id,extraction_id,set_id,source_id)` UNIQUE를 두고 matches에서 참조한다.

V64의 실제 연결은 `keyword_terms.keyword_rule_id → keyword_rules.group_id → rule_groups.release_id`다. rules에 존재하지 않는 release 컬럼을 가정하지 않는다. groups에 `(id,release_id)` UNIQUE를 additive하게 추가하고 matches의 `(keyword_group_id,rule_release_id)`가 이를 참조하게 한다. rules의 기존 `(id,group_id)` UNIQUE와 terms의 `(id,keyword_rule_id)` UNIQUE도 각각 참조한다. 이 관계로 다른 release의 term 혼입을 막는다. 다른 공고의 파일·태그가 연결되는 것도 DB에서 거부한다.

평가 입력에 포함된 set/content/base/policy/rule 조합은 단순 source 일치만으로 충분하지 않다. policies `(id,rule_release_id)`, sets `(id,source_id,content_version_id,policy_id)`, 기존 base evaluations `(id,source_id,content_version_id,rule_release_id)`의 composite UNIQUE/FK로 정확한 버전 연결을 보장한다. 다운로드 완료 파일의 hash 필수, COMPLETE_TEXT의 유효 scope/offset, 태그 카탈로그 활성 상태는 SQL CHECK가 가능한 부분과 service 검증을 나누어 테스트한다.

기존 `announcement_source_confirmed_target_categories/support_types`는 기존 판정의 확정값으로 유지한다. 종합 확정값은 위 tags로 분리하여 v1의 `based_on_evaluation_id` 의미를 바꾸지 않는다. 운영 공고 DRAFT를 만들 때 선택 경로에 해당하는 확정 태그를 기존 운영 공고 assignment 테이블에 저장한다.

### 8.5 기존 source의 additive 필드

- `attachment_policy_id uuid NULL`: 적용 대상으로 고정한 정책 FK.
- `is_attachment_review_required boolean NOT NULL DEFAULT false`: 종합 검수 요구. 전역 flag OFF로 자동 해제하지 않는다.
- `attachment_row_version integer NOT NULL DEFAULT 0`: 예약·새 결과·역할·확정 변경 시 증가.
- `current_attachment_evaluation_id uuid NULL`: source/evaluation composite FK, 기존 base evaluation과 별도.

pointer와 `is_current`는 source lock 아래 같은 transaction에서 갱신하며 deferred constraint trigger로 일치 여부를 검증한다. source 삭제 시 하위 FK cascade와 순환 pointer가 충돌하지 않도록 pointer FK는 deferrable NO ACTION으로 정의한다. 삽입 순서는 source → set/files → evaluation → pointer다.

### 8.6 내용 hash·보관·삭제

1. manifest hash: schema version, 순서가 고정된 안정 locator hash, 파일 hash, 역할, 발견 완전성, 실패 코드, 선택 extraction ID/text hash/버전의 canonical JSON SHA-256.
2. input hash: base content/evaluation hash + keyword release snapshot hash + policy/profile hash + manifest hash + 종합 engine version. 임시 URL·경로·처리 시각·lease token은 제외한다.
3. binary hash가 같아도 다른 source의 검수·PII 데이터에 자동 교차 링크하지 않는다. 재사용은 같은 source·purpose의 검증된 동일 binary와 동일 extractor 설정에 한정한다.
4. 추출 텍스트 기본 보관은 source 근거와 동일한 수명이다. 성공 즉시 binary를 제거하므로 나중의 byte-for-byte 원본 재추출은 보장할 수 없다. 새로 다운로드한 hash가 다르면 새 버전이다.
5. 개인정보 삭제나 제목 제외 원문 정리 시 새 하위 테이블도 CASCADE 대상이다. jobs·batches에는 source/text/URL을 복원할 수 있는 payload를 두지 않는다. 삭제된 job의 건수·비식별 사유는 batch 집계로 남긴다.
6. worker 완료 저장 전에 source의 존재·현재 버전을 검증한다. 삭제된 source를 worker가 새로 만들지 않는다. 생성 초기 source identity 조회를 제외하고 모든 쓰기는 FK로 기존 source에 귀속한다.
7. 기존 V70 cleanup을 수정하지 않는다. 향후 새 제외 정리 서비스는 기존 연결 공고/진행 작업 보호와 원문 제거 정책을 그대로 이어받고, worker lease 무효화·임시파일 정리까지 수행한다.

## 9. API·호환성

### 9.1 버전 경계

종합 판정은 본문이 없어도 첨부로 근거를 확보할 수 있어 기존 판정의 의미가 달라진다. 따라서 신규 조회·확정·전환은 **v2 별도 리소스**를 사용한다. 기존 v1의 `bodyText`, `reasonCode`, `matches.locationCode`에 첨부 내용을 억지로 투영하지 않는다.

- v1 source/classification 조회는 기존 TITLE/BODY 판정을 계속 반환한다. 기존 첨부 이력 배열도 기존 계약 그대로다.
- 새 v2 source 목록/상세는 `baseClassification`과 `effectiveClassification`, 첨부 상태를 구분하여 반환한다. ENFORCE 관리자 화면의 목록·건수·필터는 모두 같은 effective 기준을 쓴다.
- 첨부 검수 요구가 없는 source는 기존 v1/v2 전환 경로가 그대로 동작한다.
- `is_attachment_review_required=false`인 OFF/COLLECT_ONLY source의 `effectiveClassification`은 base와 같고 첨부 미리보기는 별도 `previewClassification`으로 반환한다. 첨부 검수가 요구되는 source에서는 current 종합 판정 또는 pending/stale projection을 사용하며 전역 worker OFF에도 이 기준을 유지한다. 따라서 단순 수집 canary가 관리자 기본 목록의 의미를 바꾸지 않는다.
- `is_attachment_review_required=true`인 source의 기존 v1/v2 전환·기존 태그 확정 경로는 종합 검수를 우회하지 못하도록 409를 반환한다. 기존 오류 wrapper와 기존 전환 오류 코드를 유지하고, 새 화면에서 검수하도록 안내한다.
- 이는 성공 응답 구조 변경은 아니지만 기존 클라이언트의 업무 동작에 영향을 주는 추가 제약이다. R2 활성화 전 소비자 목록과 전환 경로를 확인하고 공식 관리자 화면을 새 리소스로 전환해야 한다.
- 기존 V69 재분류·원복도 첨부 적용 source는 기본적으로 차단하고 새 배치 경로를 안내한다. 다른 경로에서 base가 바뀌면 종합 결과/확정은 즉시 STALE 처리하며 전환을 막는다.
- 이미 연결된 source에 대한 동일 전환 재요청은 기존 link를 멱등 반환할 수 있다. 이때 새 운영 공고나 새 분류를 적용한 것처럼 표시하지 않는다.

### 9.2 신규 endpoint

모든 경로는 제안이며 아직 구현되지 않았다. 세션 인증·서버 권한·CSRF 검증을 유지한다. 목록은 기본 20/최대 100의 `PageResponse`, 단건/변경은 `ApiResponse`다. 아래 `READ`는 ADMIN/OPERATOR/APPROVER, `WRITE`는 ADMIN/OPERATOR다. USER/PARTNER/REVIEWER는 접근 불가다.

| Method | `/api/v2/admin` 하위 경로 | 권한 | 계약 |
|---|---|---|---|
| GET | `/announcement-sources` | READ | effective 상태·첨부 상태·provider·기간·태그로 pagination |
| GET | `/announcement-sources/{sourceId}` | READ | 기존 필수 원문 + base/effective 요약, version |
| GET | `/announcement-sources/{sourceId}/attachment-sets` | READ | 집합·발견 상태·처리 건수·갱신 시각 |
| GET | `/announcement-sources/{sourceId}/attachment-sets/{setId}/files` | READ | 파일별 역할·다운로드·추출 상태, 파일명 |
| GET | `/announcement-sources/{sourceId}/attachment-extractions/{extractionId}/blocks` | READ | block 단위 페이지 응답, 원문 offset·locator. binary/외부 인증 URL 없음 |
| GET | `/announcement-sources/{sourceId}/attachment-classification` | READ | current 종합 판정과 입력 집합·base·규칙 참조 |
| GET | `/announcement-sources/{sourceId}/attachment-classification/{evaluationId}/matches` | READ | 파일별 근거 pagination, 필요 구간만 반환 |
| POST | `/announcement-sources/{sourceId}/attachment-jobs` | WRITE | 단건 승인 범위 내 수집/재시도 job 예약, 202 |
| PUT | `/announcement-sources/{sourceId}/attachment-roles` | WRITE | expected set/version과 파일 역할 배열을 받아 새 set/preview 생성, 원본 set 미수정 |
| POST | `/announcement-sources/{sourceId}/attachment-classification/confirmations` | WRITE | 종합 판정·실패 확인·다중 태그 확정 |
| POST | `/announcement-sources/{sourceId}/attachment-classification/announcements` | WRITE | current 종합 확인 기준으로 DRAFT 생성 |
| POST/GET | `/announcement-attachment-batches` | ADMIN/READ | 기존 원문 범위 고정 예약(POST)/목록(GET) |
| GET | `/announcement-attachment-batches/{batchId}` | READ | 범위·수집/preview/적용 단계·건수·version 조회 |
| GET | `/announcement-attachment-batches/{batchId}/items` | READ | 개별 성공/실패/충돌·preview 차이 pagination |
| POST | `/announcement-attachment-batches/{batchId}/collection` | ADMIN | 고정 대상의 외부 요청 시작 |
| POST | `/announcement-attachment-batches/{batchId}/preview` | ADMIN | 저장된 파일만으로 판정. 추가 외부 호출 금지 |
| POST | `/announcement-attachment-batches/{batchId}/application` | ADMIN | 고정 preview만 적용 |
| POST | `/announcement-attachment-batches/{batchId}/pause` 또는 `/resume` | ADMIN | 예약 중지/재개. 적용 단계와 수집 단계 구분 |
| POST | `/announcement-attachment-batches/{batchId}/rollback` | ADMIN | 충돌 없는 적용분 pointer·검수 binding 복원 |
| GET/POST | `/announcement-attachment-policies` | READ/ADMIN | 정책 조회/초안 생성 |
| PUT | `/announcement-attachment-policies/{policyId}` | ADMIN | DRAFT 설정 수정, expectedVersion 필수 |
| POST | `/announcement-attachment-policies/{policyId}/validation` 또는 `/publication` | ADMIN | QA 증거 검증/게시. 키워드 release 자동 게시 없음 |

WRITE의 단건 수집은 이미 승인된 ACTIVE 정책·출처 allowlist·한도 안에서만 가능하다. 권한이 있는 사람이 요청해도 임의 URL이나 범위 밖 provider를 지정할 수 없다. batch를 생성하는 것과 외부 다운로드를 시작하는 것은 별개다.

### 9.3 핵심 DTO와 충돌

종합 조회 예시(식별자는 설명용 placeholder):

```json
{
  "success": true,
  "data": {
    "sourceId": "<source-uuid>",
    "baseClassification": {
      "decisionId": "<base-evaluation-uuid>",
      "semanticStatusCode": "REVIEW_REQUIRED",
      "reasonCode": "BODY_UNAVAILABLE"
    },
    "effectiveClassification": {
      "decisionId": "<attachment-evaluation-uuid>",
      "semanticStatusCode": "ACCEPTED",
      "reasonCode": "EXTENDED_TARGET_SUPPORT_CONFIRMED",
      "setId": "<set-uuid>",
      "inputHash": "<sha256>",
      "targetCategoryCodes": ["BUSINESS"],
      "supportTypeCodes": ["POLICY_FINANCE"]
    },
    "attachmentSummary": {"discoveryStatusCode": "FOUND", "totalCount": 2, "completedCount": 2},
    "sourceVersion": 4,
    "attachmentVersion": 3,
    "confirmationStatusCode": "STALE"
  },
  "message": ""
}
```

확정/전환에는 `expectedBaseDecisionId`, `expectedAttachmentDecisionId`, `expectedSourceVersion`, `expectedAttachmentVersion`, `expectedSetHash`가 필수다. 확정 요청은 `targetCategoryCodes[]`, `supportTypeCodes[]`, `reviewMethodCode`, `acknowledgedErrorCodes[]`, `reviewNote`를 받는다. 전환은 `expectedConfirmationId`, 대표 대상과 기존 운영 공고 입력 필드를 추가로 받으며 대표 대상은 확정 태그에 포함되어야 한다.

원문 없는 제목 제외 건은 sourceId를 만들지 않으므로 위 API 접근 대상이 아니다. 파일/집합/추출/evaluation ID가 요청 source와 다르면 404를 반환한다. job·batch 생성과 confirmation에는 `Idempotency-Key` UUID를 적용하며 동일 key/동일 요청은 기존 응답, 동일 key/다른 요청은 409다. 해당 테이블에 `idempotency_key uuid UNIQUE`, `request_hash varchar(64)`를 둔다. 상태 전이는 expectedVersion CAS로 중복 실행을 거부한다. 전환은 기존 source link UNIQUE와 동일 요청 판별로 중복 DRAFT 생성을 막는다.

| 상태 | 오류 코드 제안 | 사용자 메시지 |
|---|---|---|
| 409 | `ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT` | 첨부파일 또는 판정이 변경되었습니다. 최신 근거를 확인한 뒤 다시 저장하세요. |
| 409 | `ANNOUNCEMENT_ATTACHMENT_NOT_READY` | 첨부파일 처리가 끝나지 않았습니다. 파일별 상태를 확인한 뒤 검수하세요. |
| 409 | `ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED` | 현재 첨부 판정을 기준으로 지원대상과 지원형태를 확정해야 합니다. |
| 409 | `ANNOUNCEMENT_ATTACHMENT_PREVIEW_STALE` | 미리보기 이후 원문·규칙·첨부가 변경되었습니다. 새 미리보기를 생성하세요. |
| 400 | `ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID` | 문서 역할은 공고문, 지원 안내, 신청 양식, 참고자료, 미확인 중 하나여야 합니다. |
| 403 | `ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN` | 이 작업은 공고 운영자 또는 관리자만 실행할 수 있습니다. |

외부 fetch 404/403/429 등은 API의 원시 예외로 전달하지 않고 파일 상태·고정 사유로 반환한다. 실패를 사용자 입력 오류로 표현하지 않는다. 추출 원문 응답은 `Cache-Control: no-store`이며 전량 다운로드 API를 만들지 않는다.

## 10. 구현 계층과 파일 영향

기존 `Controller → Service → ServiceImpl → DAO → Mapper XML → PostgreSQL`을 유지한다. 아래 이름은 신규 후보이며 기존 같은 이름 존재 여부를 구현 직전 확인한다.

| 구성요소 | 책임 |
|---|---|
| `AnnouncementAttachmentController/Service/ServiceImpl` | 상태 조회, 수집 job 예약, 문서 역할·검수 계약 |
| `AnnouncementAttachmentPolicyController/Service/ServiceImpl` | 정책 DRAFT·검증·게시와 변경 감사 |
| `AnnouncementAttachmentBatchController/Service/ServiceImpl` | 범위 고정, 수집, preview, 적용·중지·원복 |
| `AnnouncementAttachmentDao`, `AnnouncementAttachmentMapper.xml` | 작업·집합·추출·종합 판정·확정의 명시 SQL |
| `AnnouncementAttachmentDiscoveryClient` | provider별 첨부 descriptor 발견 |
| `AnnouncementAttachmentDownloadClient` | allowlist, 공개 IP 고정, 스트리밍, bytes·시간 예산 |
| `AnnouncementAttachmentWorker` | PG job lease, 프로세스 실행, 결과 검증·저장 |
| `DocumentTextExtractor` 구현체 3개 | PDF/HWP/HWPX plain text·locator 생성. DB/HTTP 없음 |
| `AnnouncementAttachmentClassificationEngine` | 고정 base+문서별 결과로 종합 판정, I/O 없음 |
| `AnnouncementAttachmentConversionServiceImpl` | 새 종합 확정 검증 후 기존 DRAFT 생성 로직 재사용 |

조회 메서드는 `selectAttachmentSetList`, `selectAttachmentDetails`; 저장은 `saveAttachmentExtraction`, `saveAttachmentEvaluation`; 예약은 `insertAttachmentJob`; 변경은 `updateAttachmentJobLease` 등 기존 접두사를 따른다. API Controller에 파일 다운로드·판정 로직을 두지 않는다.

외부 문서를 다루는 추출 CLI는 별도 main과 별도 Gradle 산출물로 구성한다. 웹 bootJar에 문서 파서를 직접 로드해 요청 thread에서 실행하지 않는다. Windows 개발환경과 Ubuntu 운영환경의 프로세스 종료·자원 제한을 각각 검증한다. 신규 메시지 broker, 상용 OCR, 외부 AI는 도입하지 않는다.

## 11. 관리자 화면 설계

### 11.1 Design Read

> 수집 공고에서 누락된 첨부 근거와 처리 실패를 빠르게 확인하고, 현재 파일 버전을 기준으로 검수를 확정한다.

- 사용자: OPERATOR/ADMIN의 반복 검수, APPROVER의 읽기 전용 확인.
- 주요 과업: 실패 파일 확인·재시도, 첨부 근거 읽기, 현재 종합 판정의 태그 확정.
- 위험: 조회 R0, 역할 지정 R1, 기존 데이터 대량 적용·정책 게시 R2. 공고 누락·잘못된 전환 방지가 우선이다.
- 스택: 기존 Thymeleaf·Bootstrap 5·`saneb-dashboard.css`·페이지별 외부 JS 유지. 신규 UI/모션 라이브러리 없음.
- 기기: 관리자 데스크톱 우선, 모바일 360px 조회·검수 가능. 표는 핵심 정보 카드로 재배치한다.
- 언어/시간: 한국어·Asia/Seoul. 상태에 텍스트 라벨, 키보드 포커스, 실패 복구 경로 제공.

### 11.2 정보 구조

기존 `/app/admin/collected-announcements`의 상세 흐름 안에 다음을 추가한다. 목록은 새 v2 effective 기준으로 전환한다.

1. 종합 상태·현재 처리 단계·마지막 갱신 시각. `유효 후보`는 최종 확정이 아님을 유지.
2. 제목/본문 판정과 첨부 처리 상태를 나란히 요약. 원문 출처 상태는 별도 표시.
3. 파일 목록: 파일명, 형식, 문서 역할, 다운로드, 텍스트 추출, 검수 사유, 재시도 가능 여부.
4. 파일 선택 시 plain text 근거 패널. PDF는 페이지, HWP/HWPX는 문단·표 셀 위치로 이동.
5. 역할 미확인·실패·부분 추출 확인과 지원대상·지원형태 태그 확정.
6. 현재 종합 판정 확인 후에만 `운영 공고 초안 만들기`를 제공.

파일 패널의 주 행동은 `첨부 근거 확인`, 확인 폼의 주 행동은 `현재 판정으로 검수 확정`이다. 추출 완료 전에 검수 완료를 낙관적으로 표시하지 않는다. 상세 조회는 큰 text 전량을 목록 응답에 포함하지 않고 block 페이지를 필요할 때 요청한다.

### 11.3 상태·복구

| 상태 | 표시·행동 |
|---|---|
| OFF/미수집 | `첨부 수집 미적용` 및 현재 적용 범위. 첨부 0개로 표현 금지 |
| 발견 정상 0개 | `확인된 첨부파일 없음`, 발견 시각 표시 |
| 처리 중 | 전체/완료/실패 건수·현재 단계. 다른 공고 검수 가능 |
| 부분 실패 | 성공 파일은 읽을 수 있고 실패 항목만 재시도 가능. 자동 완료 금지 |
| OCR 필요 | `이미지 문서로 텍스트 확인이 필요합니다`, 운영자가 수동 확인 가능 |
| 제한 초과 | `파일이 20 MiB 제한을 초과했습니다` 등 실제 적용 한도 명시 |
| 역할 미확인 | `공고문인지 신청 양식인지 확인해 주세요`와 역할 선택 |
| 정책/출처 QA 미통과 | 출처별 미지원 사유 표시, 사용자가 파서를 선택하게 하지 않음 |
| 충돌/STALE | 최신 판정 확인 링크와 입력 유지. 강제 덮어쓰기 버튼 없음 |
| 오프라인/세션 만료 | 서버 처리 상태를 성공으로 바꾸지 않음. 재접속 시 job 조회, 작성 중 검수 메모 자동 로컬 저장 금지 |
| 권한 없음 | 읽기 권한과 실행 권한을 구분하고 불가 사유 표시 |
| 수집/적용 중지 | 이미 받은 데이터와 진행 중 파일 처리 여부 명시. 중지가 삭제/원복을 뜻하지 않음 |

HTML escape된 텍스트와 검증한 code point 구간으로 강조한다. 추출 HTML·스크립트, `th:utext`, 임의 innerHTML은 사용하지 않는다. 필터·페이지·파일 선택은 URL 또는 비민감 상태로 복원하며 문서 원문·검수 메모를 localStorage에 두지 않는다.

목록·파일 선택·검수 폼은 키보드로 사용 가능해야 한다. 상태는 색상에 의존하지 않고 `aria-live` 갱신은 건수·단계 변경으로 제한한다. 긴 원문은 모달 대신 패널/상세 페이지를 사용한다. 320px stress, 360px 기본, 확대·focus-visible 수용 기준을 QA 계획에 둔다. 브라우저 검증은 사용자의 명시적 지시가 있을 때 실행한다.

## 12. 기존 데이터 처리·전환·원복

### 12.1 대상과 두 단계 미리보기

1. `PRODUCTION`, 저장된 비제외 source, 지정 provider/수집일/마감일/최대 건수로 범위를 고정한다. QA source·삭제된 제목 제외 source·기본적으로 연결된 운영 공고는 제외한다.
2. 최초 범위 미리보기는 외부 호출이 없으며 대상 건수·provider·최대 bytes·기존 정책만 보여준다. 아직 모르는 파일 수·예상 판정 변경 건수를 꾸며내지 않는다.
3. 명시적인 수집 실행으로 첨부를 받아 별도 set/extraction을 저장한다. source 기본 판정·검수는 아직 변경하지 않는다.
4. 수집 완료 후 봉인한 set으로 종합 판정 미리보기를 만든다. 상태 변화·파일 실패·추출 불완전·역할 미확인·태그 변화·규칙 버전을 보여준다.
5. 적용 승인 시 preview의 source/base/set/policy/input hash가 같을 때만 반영한다. 변경됐으면 해당 항목 CONFLICT이며 새 preview가 필요하다.
6. 이전 정책 binding·current attachment evaluation·confirmation ID·versions를 jobs의 적용 이력 필드에 저장하여 조건부 원복을 지원한다. 이전 원문 텍스트를 복사하지 않는다.

jobs의 추가 필드: `preview_evaluation_id uuid?`, `preview_hash varchar(64)?`, `previous_attachment_evaluation_id uuid?`, `previous_confirmation_id uuid?`, `previous_policy_id uuid?`, `previous_is_review_required boolean?`, `applied_evaluation_id uuid?`, `applied_attachment_version int?`, `application_status_code varchar(30)`, `rollback_status_code varchar(30)`. evaluation/source와 confirmation/source composite FK, 비음수 version CHECK를 둔다. `application_status_code`는 `NOT_REQUESTED/PENDING/APPLIED/CONFLICT/FAILED`, rollback은 `NOT_REQUESTED/ROLLED_BACK/CONFLICT/FAILED`다.

batch 상태 전이는 `SCOPE_READY → COLLECTION_PENDING → COLLECTING → COLLECTED 또는 COLLECTION_PARTIAL_FAILED → PREVIEW_RUNNING → PREVIEW_READY 또는 PREVIEW_PARTIAL_FAILED → APPLYING → APPLIED 또는 APPLY_PARTIAL_FAILED`다. 수집·적용 중지는 각각 `COLLECTION_PAUSED/APPLY_PAUSED`, 원복은 `ROLLING_BACK → ROLLED_BACK 또는 ROLLBACK_PARTIAL_FAILED`다. 부분 실패 preview는 적용 가능한 고정 성공 항목과 실패 항목을 모두 보여주고 명시적으로 선택된 성공 항목에만 적용한다. 실패 항목을 암묵적으로 제외하여 전체 성공으로 표시하지 않는다.

jobs에는 배치 범위 내 항목 선택용 `is_selected_for_application boolean DEFAULT false`를 둔다. 선택 변경은 PREVIEW_READY 계열에서만 가능하며 batch preview hash·row_version에 반영한다. collection 이후의 preview는 sealed set만 읽고 네트워크 요청을 생성하지 않는다. apply 시 현재 ACTIVE policy/rule과 승인한 preview가 달라졌으면 충돌로 처리한다. 신규 자동 수집 job은 실행 시작 때 고정한 정책을 따르므로 이 배치 적용 조건과 구분한다.

### 12.2 기존 검수 보호

- 신규 ENFORCE source는 수집 직후부터 첨부 검수 요구를 설정하며 pending 상태에서 전환을 막는다.
- 기존 source는 COLLECT_ONLY 동안 기존 검수를 유지한다. 종합 적용 시 이전 확인을 STALE로 만들고 새 종합 확인이 필요하다.
- COLLECT_ONLY에서 ENFORCE로 정책을 바꾸어도 기존 모든 source를 즉시 자동 적용하지 않는다. 기존 source 적용은 위 batch를 사용한다.
- 이미 연결된 운영 공고는 기본 backfill에서 제외한다. 별도 범위에 포함하면 첨부 근거·재검수 경고만 갱신하고 운영 공고·조건·지원 진행은 변경하지 않는다.
- V70에서 제거된 원문의 URL·첨부 목록은 되살릴 수 없다. tombstone으로 역조회하거나 원문을 추측해 생성하지 않는다. 향후 공식 목록에 재발견되면 당시 제목 정책을 먼저 적용한다.

### 12.3 원복

원복은 기존 schema 삭제나 원문 삭제가 아니다. 적용한 종합 evaluation이 여전히 current이고 그 이후의 검수·역할 변경·전환·content 갱신이 없을 때 이전 pointer/binding/confirmation 상태를 source lock 아래 복원한다. 버전은 과거 숫자로 되돌리지 않고 증가시킨다. 새 이력은 보존한다.

원복 후 기존 base 판정 경로가 다시 열리는 source는 영향 요약에 포함해야 한다. runtime OFF는 다운로드만 멈추며 이 복원 작업을 대신하지 않는다. 활성화 후의 긴급 대응은 worker 중지와 current evidence 유지가 기본이다. 구버전 JAR에는 새 전환 차단이 없으므로 ENFORCE 상태를 남긴 채 구버전 앱으로 되돌리는 것은 금지하며, 유지보수 전환 또는 검증된 binding 원복을 먼저 수행한다.

신규 자동 ENFORCE job도 이전 binding/current/confirmation과 적용 후 version을 기록해야 같은 복구 검증을 할 수 있다. 이미 이후 검수·운영 전환이 있어 안전하게 binding을 원복할 수 없는 source의 keyword release 교체는 이 증분에서 강제 지원하지 않는다. 새 release로의 base/첨부 통합 전환 절차를 별도 검증하기 전까지 현재 근거를 유지하고 `재검수 절차 확인 필요`로 남긴다. 이러한 항목은 기존 데이터 batch의 적용 가능 건수에서 구분한다.

## 13. migration·구현·출시 순서

| 증분 | 구현 범위 | 통과 조건 |
|---|---|---|
| A | 새 테이블·source additive 컬럼, DTO·Mapper 계약 | 기존 migration checksum 동일, 빈 DB와 V71 upgrade 성공 |
| B | 프로필별 발견·안전한 다운로드 + PG job | 제목 제외 요청 0, SSRF/bytes/lease 계약 통과 |
| C | 격리 추출 CLI, PDF/HWP/HWPX | 정상·암호·손상·혼합·한도·프로세스 종료 fixture 통과 |
| D | 불변 set·종합 판정·근거·확정 | 기존 Golden 유지 + 첨부 Golden·충돌·FK 테스트 통과 |
| E | v2 조회·확정·전환 + 기존 경로 guard + 관리자 UI | 권한·CSRF·409·중복 전환·입력 보존 검증 |
| F | backfill 수집·preview·apply·pause·rollback | 범위 고정·부분 실패·원복 CAS·기존 운영 공고 보존 |
| G | 출처별 격리 QA와 COLLECT_ONLY canary | 실제 파일 확보·품질·자원 보고, 오류/본문 가용 상태 분리 |
| H | 신규 ENFORCE → 기존 데이터 명시 적용 | 종합 UI 사용 확인, before/after 근거·모니터링 준비 |

의존성이 없는 단위 검증은 병렬 수행할 수 있으나 빌드·문서 추출·브라우저 등 고부하 작업을 동시에 실행하지 않는다. 이번 설계 작업에서는 하위 에이전트를 사용하지 않았다.

운영 flag 제안: `SANEB_ANNOUNCEMENT_ATTACHMENT_WORKER_ENABLED=false`, `SANEB_ANNOUNCEMENT_ATTACHMENT_MODE=OFF`. DB 정책 ACTIVE 및 provider/profile allowlist와 함께 충족되어야 요청할 수 있다. 정책 게시만으로 worker를 시작하지 않고, worker ON만으로 출처를 신뢰하지 않는다. OCR 설정은 1차 구현에 노출하지 않는다.

릴리스에는 웹 JAR 외 추출 산출물, 정확한 parser dependency manifest, 전용 작업 root·OS 권한·memory/time 제한·네트워크 차단·정리 작업이 포함된다. `bootJar` 성공만으로 운영 첨부 준비 완료를 판정하지 않는다. 운영 배포 직전 release-readiness 검증에서 DB·앱·CLI·환경설정을 함께 확인한다.

## 14. 검증·성공·실패 기준

세부 fixture와 시나리오는 [첨부 QA 계획](announcement-attachment-qa-plan-2026-09-08.md)에 둔다.

### 14.1 설계 완료 기준

- [x] 기존 본문 B=검수, 제목 제외 비저장 및 원문 복구 불가 정책을 설계에 반영.
- [x] 기존 v1·v2 경로의 호환 동작과 추가 전환 제약을 명시.
- [x] DB FK/unique/index, 원문 보관, 실패 상태, 버전·해시·lease·원복을 정의.
- [x] PDF/HWP/HWPX와 OCR의 범위, 실제 사이트 QA 미실행을 구분.
- [x] UI의 미수집/없음/실패/부분 성공/STALE과 다음 행동을 정의.
- [x] 문서 링크·Git diff·변경 범위 검증 완료. 런타임 무결성은 구현 테스트 Gate에 남김.

### 14.2 구현 성공 기준

- 제목 제외 공고에서 상세·첨부 HTTP 요청과 원문성 신규 행 모두 0.
- 일반 PDF/HWP/HWPX fixture의 지정 핵심 문구·표 셀·근거 locator를 재현.
- 스캔·암호·손상·부분 추출은 명확한 미완료 상태이며 자동 제외/완전 추출로 표현하지 않음.
- 파일명만의 B 키워드와 다른 파일 간 AND로 판정하지 않음.
- 기관별 링크 탐색 성공, 파일 bytes 확보, 추출 성공을 각각 증명.
- 동일 요청/worker 중복·lease 만료·동시 검수에서 중복 적용 또는 덮어쓰기 없음.
- v1 Golden·Controller·기존 DRAFT 전환 회귀, 확장 경로 권한·CSRF·DB 제약 통과.
- 정책 OFF, 제한 초과, 추출기 장애에서 웹 서비스·다른 수집 흐름이 동작.
- 새로운 결과는 유효 후보 또는 검수 대상이며 운영 공고 자동 활성화 없음.

### 14.3 실패 기준

- 확장자가 PDF라는 이유만으로 내려받은 HTML을 성공 파일로 취급.
- 첨부 하나 성공으로 전체 실패를 숨기거나 OCR 필요를 첨부 없음으로 표시.
- 첨부 텍스트를 기존 `body_text`에 합쳐 출처와 기존 API 의미를 잃음.
- 기존 migration·ACTIVE 규칙을 수정하거나 모드 OFF로 검수 guard를 우회.
- 바뀐 파일·본문·규칙으로 preview 승인 없이 기존 데이터를 적용.
- filename, 원문, 인증 query·cookie·비밀번호를 로그·Git·감사 metadata에 기록.
- 웹 JVM에서 무제한 파싱하거나 timeout 이후 자식 프로세스·임시파일을 방치.
- 실파일 검증 없이 ‘15개 전체 지원’, 운영 확인 없이 ‘적용 완료’라고 보고.

## 15. 남은 선택·근거·이번 검증 기록

### 15.1 설계 기본값과 추후 확인

| 항목 | 이번 기본값 | 다음 확인 시점 |
|---|---|---|
| 형식 | 일반 PDF/HWP 5.x/HWPX, OCR 후속 | 구현 초기 fixture |
| keyword release 불일치 | 기존 base 재분류 선행, 동일 release끼리만 종합 | 기존 데이터 범위 고정 시 |
| 서버 파일 원본 | 임시 보관 후 제거, 텍스트는 source 근거 수명 | 원본 다운로드/장기보관 요청이 있을 때 재설계 |
| 본문·첨부 B | 관리자 검수, 자동 제외 없음 | 제품 정책 변경 요청 시에만 재검토 |
| 알려진 양식/참고자료 | 참고 근거, 자동 후보 생성 제외 | 출처별 QA로 역할 판별 검증 |
| 미지원 다운로드 방식 | PROFILE_REQUIRED, 관리자에게 실패 사유 표시 | POST/쿠키/서명 URL 출처별 QA |
| 운영 자원·수집량 | 보수적 한도, 전역 추출 동시성 1 | Q1 실증으로 CPU/메모리/queue 측정 |
| 과거 데이터 | 최대 100건의 제한 배치부터, 연결된 공고 제외 | 운영 대상·기간 결정 후 실행 |

이 기본값은 작업을 진행하기 위한 제안이며 클라이언트가 이미 세부값까지 확정했다고 표현하지 않는다. 전체 사이트 지원 범위와 일정은 출처별 표본 검증 이후 확정한다.

### 15.2 외부 기술 근거

2026-09-08 공식 프로젝트 문서를 확인했다. 외부 문서는 라이브러리 기능 근거이며 saneB의 구현/운영 성공 근거가 아니다.

- [Apache PDFBox FAQ](https://pdfbox.apache.org/3.0/faq.html): PDF 텍스트 순서, 스캔·인코딩·추출 권한의 한계.
- [hwplib](https://github.com/neolord0/hwplib): HWP 텍스트 추출과 암호화 파일 제한.
- [hwpxlib](https://github.com/neolord0/hwpxlib): 별도 HWPX 읽기·텍스트 추출 지원.
- [Tesseract 문서](https://tesseract-ocr.github.io/tessdoc/): 후속 OCR 후보. 이번 범위에서 설치·실행하지 않음.

### 15.3 이번 실행 기록

- 실행: `Get-Location`, `Get-Content`, `rg/rg --files`, 명령 범위의 `git -c safe.directory=C:/PersonalProject/saneB status --short --branch`, `rev-parse HEAD`.
- 결과: 작업 경로 saneB, 시작 시 master/61223aef·clean, AGENTS·기존 계약·코드 확인. 문서-only 설계 작업.
- 진행 중 경로 glob/가정한 클래스명이 맞지 않은 조회는 `rg --files`와 실제 경로로 재확인했다. 해당 조회 실패를 구현 결함으로 간주하지 않았다.
- 최종 문서 검증: Node.js 일회성 검사로 신규 문서의 상대 링크 6개·실제 source 참조 12개·QA ID 연속성·code fence·공백·문서 언어 점검 통과. `git diff --check` 통과. 변경은 신규 문서 2개와 기존 문서 3개의 연결 안내뿐이다.
- 미실행: Gradle/DB migration/실파일 다운로드·추출/운영·AWS/GitHub 조회. 이번에는 구현/배포를 하지 않았으므로 런타임 통과를 주장하지 않는다.
- 브라우저: 사용자 정책상 미실행. Node.js는 문서 검증용 일회성 프로세스만 사용하고 종료 여부를 확인한다.
