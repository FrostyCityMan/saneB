# 정부24 공식 목록 API 계약 보완

## 범위와 근거

P3 공통 수집 연결의 증분이다. 제목 → 정제 본문 → 실제 첨부 텍스트 → 최종 관리자 검증 순서는 변경하지 않는다. 정부24 첨부 profile 또는 전체 채널 완료를 추가로 선언하지 않는다.

2026-09-15 확인한 [공공데이터포털의 해당 API](https://www.data.go.kr/data/15113968/openapi.do)와 해당 페이지가 제공하는 [공식 OpenAPI 정의](https://infuser.odcloud.kr/api/stages/44436/api-docs?1684891964110)를 기준으로 한다. 목록 endpoint는 `https://api.odcloud.kr/api/gov24/v3/serviceList`다. 기존 코드의 `type/pageNo/numOfRows/keyword/region`과 달리 공식 목록은 `page/perPage`, 서비스명 LIKE 조건, JSON 기본 응답을 정의한다. 인증 값은 기록하지 않는다.

## 계약과 구현

1. 호스트가 `api.odcloud.kr`이면 공식 목록 모드다. HTTPS/443, 정확한 목록 경로만 허용하며 URL에 사용자 정보·쿼리·fragment를 붙이지 않는다. 다른 구성된 호스트의 기존 중계 API 요청/응답 방식은 유지한다. 잘못된 URL을 예외에 복사하지 않는다.
2. 공식 모드는 `page=1`, `perPage=min(maxCount,500)`이며 null 요청 상한은 100, 0 이하는 거부한다. JSON 기본 응답을 사용한다. 한 query당 HTTP 1회이며 페이지·요청량을 자동 확장하지 않는다. 이 상한은 반환 후보 페이지의 크기이지 전체 정부24를 조회했다는 보장이 아니다.
3. 일반 수동 검색어는 분할하지 않고 서비스명 LIKE 조건으로 전달한다. V2 고정 검색 계획은 `SearchQuery`의 대상·지원형태 대표어를 별도 전달한다. 공식 요청에는 대상 대표어를 보내며 다중 단어 대표어 자체는 분해하지 않는다. 어댑터는 반환 후보를 조합 조건으로 미리 삭제하지 않는다. 지원 대표어가 없더라도 TITLE A 예외일 수 있기 때문이다. **같은 제목**의 대상·지원형태 조합과 A/B 예외는 고정 release의 기존 공통 TITLE 분류기로 전달해 원문 저장 전에 판정한다. 본문·다른 공고의 단어를 합성하지 않는다.
4. 이 단계는 검색 후보 확보이며 분류 완료가 아니다. 기존 공통 TITLE A/B·대상/지원 조건, BODY·첨부 판정·최종 검수·자동 ACTIVE 금지는 그대로다. 대표어의 유효성 확인은 기존 `AnnouncementSourceTextNormalizer`를 사용한다. 후보가 상한보다 적어도 다음 페이지를 자동 요청하지 않는다. 같은 대상의 여러 지원 대표어 조합이 동일 페이지를 중복 조회할 수 있으나 이번에는 검색 계획과 요청 단위/상한을 바꾸거나 캐시하지 않는다. 첫 페이지 밖의 누락 가능성과 실제 LIKE 동작/정렬/호출 한도는 실 API QA에서 확인할 항목이다.
5. 기존 제공자의 새 기본 메서드는 이전과 같이 조합 문자열을 `withSearchKeyword`로 전달한다. 저장한 검색 계획의 canonical JSON/hash, 라운드로빈 중복 제거·최종 요청 상한은 변경하지 않는다. 과거 실행을 재분류하거나 검색 결과를 소급 갱신하지 않는다.
6. 공식 API에 동일 의미가 없는 지역 코드·내부 카테고리·신청기간은 조용히 무시하지 않고 구체적인 오류로 거부한다. 등록/수정 시각이나 소관기관명을 신청기간/지역으로 임의 대체하지 않는다. **새 정부24 자동 배치 요청**은 기존에 임의 생성하던 오늘~3개월 신청기간을 비운다. 기업마당 배치의 기존 값은 유지한다. 이미 승인/대기 중인 기간 포함 정부24 요청은 자동 수정하지 않으며 운영자가 범위를 다시 확인해 새 요청을 만들어야 한다.
7. 공식 응답은 data 배열, 요청과 일치하는 page/perPage, 배열 길이와 같은 정수 currentCount, 페이지 상한, 각 행의 서비스 ID/제목을 검증한다. 오류 객체·다른 배열·잘못된 행을 성공 0건으로 바꾸지 않는다. 원격 오류 본문/URL/인증 값은 예외에 포함하지 않는다. 정상적인 빈 배열은 별도 정상 0건이다.
8. 공식 `전화문의`를 문의 텍스트의 후순위 별칭으로 추가한다. 기존 본문 필드 우선순위와 첨부 metadata 제거/hash 규칙을 유지한다. 목록에 첨부 필드가 없다는 이유로 `NO_FILES`를 생성하지 않는다. 첨부 발견 profile 미등록은 기존 `PROFILE_REQUIRED/PROFILE_MISSING`으로 남긴다.

## DB/API/운영 영향

- DDL/migration, Controller DTO와 응답 wrapper, 출처 코드/별칭, v1/v2 경로·형식 변경 없음. 최근 migration V83 유지.
- 공식 외부 요청 규격과 내부 제공자 인터페이스만 확장한다. 공식 endpoint의 미지원 필터 오류와 새 정부24 자동 배치의 기간 null은 위에 명시한 동작 보완이다.
- 운영 설정·DB·승인된 요청·규칙·정책·ENFORCE·기존 데이터는 변경하지 않았다. 신규 credential이나 운영 API 호출도 하지 않았다.
- 로컬 프로세스의 정부24 URL/key 설정은 모두 없음으로 확인했다. 이는 운영 환경의 현재 설정 확인이 아니다. 운영 AWS 인증 대기와 실제 API 사용권한은 별도 blocker다.
- 첨부 엔진6/profile17/전용 BODY16기관/지원 추출 형식3, catalog 참조24/기대값0은 이 증분으로 증가하지 않는다.

## 검증과 남은 Gate

- [x] 공식 request parameter/인코딩/1회 요청/상한/일반 문자열/대표어 전달/기존 중계 API 계약을 합성 응답으로 검증한다. 커밋 전 검토에서 조합 미충족을 어댑터가 미리 삭제하지 않도록 보완했으며 TITLE A 후보 보존 회귀를 추가했다.
- [x] 잘못된 endpoint·미지원 범위·오류 envelope·잘못된 건수/행을 성공 0건으로 취급하지 않는 회귀를 추가한다.
- [x] 서비스의 typed query 전달·기존 라운드로빈 병합을 검증한다. 첫 표적65건 중1건은 기존 HTML 공백 정제 결과와 기대값 차이로 실패했고 원인을 확인해 기대값을 수정한 뒤65/65 통과했다.
- [x] 커밋 전 TITLE A 보존 보완 후 최종 전체 회귀는4분3초 성공했다. root2622=2361통과/261조건부 생략/실패0, 새 정부24 어댑터40/40, 공통 TITLE gate2/2, 별도 패키지20/20·임시 Flyway3/3이다. 정부24 새 배치 기간 null/기업마당 보존2건도 포함한다. Node22건 중20통과/2 Linux 전용 생략이다. extractor 시험은 UP-TO-DATE이며 새 실행 성공으로 계산하지 않는다. 최종 JAR SHA256은 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`다. 앞선4분13초/root2621 결과를 최종 코드 증거로 사용하지 않는다.
- [!] 실제 API credential 기반 공식 목록 수집·LIKE 의미/첫 페이지 누락·호출 한도·전국/지역 적용성은 미검증이다. 합성 테스트를 정부24 수집 성공으로 세지 않는다.
- [!] 정부24 공식 상세 BODY/첨부 영역·실파일·worker/DB/API/UI 전체 경로와 운영 배포·브라우저는 미완료다. API 목록의 구비서류 텍스트나 지원조건 데이터가 파일 첨부 manifest를 대신하지 않는다.

복구는 미배포 코드 증분의 사용을 보류하는 방식이며, 운영 데이터 rollback은 발생하지 않는다. 실 API 연결 전 기존 기간 포함 승인 요청과 후보 페이지 범위를 다시 확인한다. 전체 Gate/ATT는 `Not ready`, 장기 goal은 ACTIVE다.

실행 명령:

```powershell
.\gradlew.bat :test --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --tests '*AnnouncementSourceProviderAttachmentExclusionTest' --tests '*AnnouncementSourceSearchPlanBuilderTest' --tests '*AnnouncementSourceServiceImplTest' --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-contract-release.test.mjs
```

`sanebFlywayEphemeral=true`는 기존 운영 DB 접속 설정을 사용하지 않고 별도 임시 loopback PostgreSQL에서 검증한다. 사용한 Node/Gradle·자식 JVM/임시 PostgreSQL은 종료했다. 기존 사용자 `output/` Word2개는 수정·커밋하지 않는다.
