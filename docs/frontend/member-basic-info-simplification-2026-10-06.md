# 기본정보 입력 간소화 구현

## 범위와 완료 기준

승인된 `docs/mockups/member-basic-info-simple/index.html`의 사용자 입력 개선을 실제 `/app/member/basic-info`에 연결한다. 기존 Thymeleaf·공통 CSS·v1 API·DB를 유지한다. 사용자 목적은 아는 값만 입력하고 서류별 중복 질문 없이 여러 가족의 정보를 저장하는 것이다. 프로필 편집 R1 위험으로 분류하며 값 유실 방지를 우선한다.

- [x] 국세 체납/완납 단일 선택, 선택 해제로 미입력 유지
- [x] 건강보험 직장가입자/지역가입자/피부양자 단일 선택
- [x] 가족관계증명서 안의 가족별 반복 입력, 제거·저장 전 실행 취소
- [x] API가 반환한 모든 서류 사전 표시, 접기/펼치기
- [x] 부가세 과세기간·공급가액 입력 숨김 및 기존 값 보존
- [x] 기존 사업자 상세·주소 검색·인터뷰 항목 유지
- [x] 조회 실패 시 저장 금지·재시도, 저장 중 편집 잠금
- [x] JavaScript 문법·단위 시험
- [x] 루트 Gradle 화면/API 테스트 10개·bootJar 통과
- [ ] 실제 DB 왕복 저장 검증 — 이번 로컬 비DB 검증에서 미실행
- [ ] 브라우저 QA — 현재 요청에서 지시하지 않아 정책상 미실행
- [ ] 운영 반영 — 이번 범위 아님

## 재사용 소스 / 영향

- `templates/app/member-basic-info.html`: 기존 필드와 주소 검색 모달을 재사용. 사업자·인터뷰는 접기 영역으로 전환한다.
- `static/css/saneb-dashboard.css`: 기존 색·패널·폼 토큰을 재사용하고 새 규칙은 `[data-basic-info-simple]` 범위에 제한한다.
- `static/js/saneb-member-basic-info.js`: 기존 GET/PUT, 가족 배열, 주소 검색, 오류 응답 흐름을 유지한다.
- `static/js/saneb-member-basic-info-core.js`: v1 값 보존과 단일 선택 변환을 담당하는 순수 함수.
- `static/js/saneb-member-basic-info-simple.js`: 일반 사용자 전용 서류 렌더링과 이벤트.

관리자 기본정보 화면은 기존 선택·추가 방식을 유지한다. 공유 JS의 라벨 연결 및 알 수 없는 기존 select 값 보존은 관리자에도 적용한다. 새 패키지·UI 라이브러리·migration은 추가하지 않는다.

## v1 / DB 호환

`MemberBasicInfoServiceImpl.saveDocumentInputValues`는 `documentInputs`가 있으면 서류 값을 전체 교체한다. 따라서 새 UI는 조회 카탈로그의 기존 값을 복사하고 표시된 입력만 덮어써서 전송한다. 숨겨진 과세기간·공급가액·발급일·과거 가족 서류 요약 등은 삭제하거나 임의 변환하지 않는다. 명시적으로 비운 입력만 null로 보낸다. 기존 API의 전체 교체 의미는 변경하지 않는다.

| UI 입력 | 기존 저장 필드 |
|---|---|
| 국세 완납 | `TAX_PAID_STATUS=true`, `NATIONAL_TAX_DELINQUENT=false` |
| 국세 체납 | `TAX_PAID_STATUS=false`, `NATIONAL_TAX_DELINQUENT=true` |
| 국세 선택 해제 | 위 두 필드 null |
| 건강보험 한 자격 | `HEALTH_INSURANCE_BASIS_CODE`와 해당 boolean 하나만 true, 나머지는 false; 본인 기본정보 자격도 동기화 |
| 건강보험 선택 해제 | 위 코드/boolean 및 본인 자격을 null |
| 가족별 정보 | 기존 `families[]` → `family_members` |

라디오를 사용자가 변경하지 않았다면 기존 서류 필드를 자동 생성하거나 정규화하지 않는다. 기존 false를 참으로 뒤집어 추측하지 않는다. 기존 값이 충돌하면 자동 완납·가입자 판단을 하지 않으며 해당 서류를 다시 선택하거나 선택 해제로 해결한 뒤 저장한다.

가족 배열과 과거의 단일 서류 요약은 서로 다른 계약이다. 과거 단일 출생연도·가족 수·자유문을 여러 명의 가족으로 추정하지 않고 접힌 읽기 전용 영역에서 제공한다. 가족별 정보를 바꿔도 과거 요약을 자동 갱신하지 않는다. 차이가 있으면 운영자 정정이 필요하다. 구조화된 가족 정보와 문서 증빙을 자동 통합하는 migration/API 변경은 별도 정책 작업이다.

## 매칭 경계

기본정보 저장 시 `matchingService.insertBasicMatchingCandidates`가 호출되는 기존 동작만 유지한다. 신규 공고·조건·상태 변경에 따른 자동 재매칭, 정밀 후보 자동 재계산은 이번 UI 변경에 추가하지 않는다. 화면은 후보를 선정·승인 결과로 표현하지 않는다.

## 상태 / 검증

빈 값, 입력 있음, 이전 값 보존, 충돌 값 확인, 가족 빈 목록·삭제 취소, 조회 중·실패·재시도, 저장 중·실패·성공, 미저장 이탈 경고를 제공한다. 값은 브라우저 영구 저장소에 기록하지 않는다. 모바일 한 열, 44px 컨트롤, 포커스 표시, label 연결, 네이티브 details/라디오를 적용한다. 실제 렌더링·스크린리더 검증 완료를 의미하지 않는다.

실행 명령:

```powershell
node --check src/main/resources/static/js/saneb-member-basic-info-core.js
node --check src/main/resources/static/js/saneb-member-basic-info-simple.js
node --check src/main/resources/static/js/saneb-member-basic-info.js
node --test scripts/qa/member-basic-info-simple.test.cjs scripts/qa/member-basic-info-view.test.cjs
.\gradlew.bat --no-daemon --max-workers=1 :test --tests '*MemberBasicInfoViewControllerSmokeTest' --tests '*MemberBasicInfoControllerSmokeTest' :bootJar
```

Gradle 검증에서는 `SPRING_PROFILES_ACTIVE=test`, `DB_URL=jdbc:postgresql://127.0.0.1:1/disabled`, Flyway·SQL 초기화·수집 worker를 비활성화했다. 실제 DB 스모크 테스트는 실행하지 않는다.

현재 결과: JS 문법 통과, Node 22개 통과(순수 함수·정적 계약 15개, 가짜 DOM 이벤트 단위 시험 7개), Spring 사용자/관리자 화면·API 테스트 10개 통과, bootJar 성공. 가짜 DOM 시험은 실제 브라우저 QA가 아니다. 최초 `test` 명령은 하위 모듈에도 필터가 적용되어 해당 모듈에 테스트가 없다는 오류가 났으며, 루트 `:test`로 재실행하여 성공했다. 전체 Java 테스트와 실제 DB 저장 시험은 실행하지 않았다.

최종 보강 후 `:bootJar`를 다시 실행해 성공했다. `build/libs/saneB-0.0.1-SNAPSHOT.jar` 내부의 JS 3개·HTML·공통 CSS 총 5개 리소스를 현재 소스와 SHA-256으로 비교해 모두 일치했다. Node 시험과 단일 실행 Gradle 프로세스는 종료됐으며 서버·브라우저를 띄우지 않았다. HEAD는 `547739e52b2df2e72a842bdf38af641d9563eeb8`로 유지했고 커밋·푸시·배포는 하지 않았다.

사용한 스킬은 long-goal-operating-protocol, ui-ux-operating-principles, frontend-design-core, frontend-ui-engineering이다. 기존 필드 보존, 범위 제한, 오류 회복 및 테스트 근거를 우선했다.
