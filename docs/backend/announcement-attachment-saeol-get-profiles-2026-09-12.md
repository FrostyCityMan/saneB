# 새올 GET 첨부 프로필 확장·실측 기록

## 현재 단계와 범위

Gate 2·6 부분 진척, 전체 **Not ready**. 시스템 첨부 프로필을 기존 2개에서 6개로 확장한다. 목록 파서 전체나 모든 지자체 첨부 지원 완료가 아니다. 운영 DB·정책·기존 원문·공고 공개 상태를 변경하지 않는다.

후속 09-12 화천 POST 증분으로 현재 등록은 7개이며 전용 실사이트 task는 6사례/10파일로 확장됐다. 아래 03:04/03:11 수치는 GET4기관 당시 기록이다. 최신 계약·공통 호출 해석 수정·화천 검증은 `announcement-attachment-hwacheon-post-profile-2026-09-12.md`를 따른다.

기준: `announcement-attachment-collection-design-2026-09-08.md` 4.3·5.3·6.1, ATT-004/011~014/020~024/028. URL·영역·요청 템플릿은 시스템 코드에 고정하며 관리자에게 선택/편집 기능을 추가하지 않는다.

## 추가된 시스템 매핑

| 기관 / source code | 첨부 profile code | 검증한 exact host | 기존 목록 profile | 첨부 영역 |
|---|---|---|---|---|
| 부산 남구 / LGS-000034 | LOCAL_BUSAN_NAMGU_GET_V1 | eminwon.bsnamgu.go.kr | SAFE_SAEOL_EMINWON_LEGACY | form1 내 첨부파일 th의 인접 td |
| 대구 달성군 / LGS-000052 | LOCAL_DAEGU_DALSEONG_GET_V1 | eminwon.dalseong.daegu.kr | SAFE_SAEOL_EMINWON | form1 내 첨부파일 th의 인접 td |
| 대구 중구 / LGS-000045 | LOCAL_DAEGU_JUNGGU_GET_V1 | eminwon.jung.daegu.kr | SAFE_SAEOL_EMINWON_LEGACY | form1 내 첨부파일 div.tal |
| 함안군 / LGS-000233 | LOCAL_HAMAN_GET_V1 | eminwon.haman.go.kr | SAFE_SAEOL_EMINWON_CELL | form1 내 첨부파일 td의 인접 td |

목록 매핑 근거는 V37·V40·V43·V54·V55다. 실제 운영 설정이 변경됐으면 source code·목록 profile·원문 URL hash 검증에서 거부한다. 과거 seed만으로 현재 운영 매핑이 같다고 단정하지 않는다.

- 공통 상세 경로: `/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do`. 고정된 context/homepage/jndinm/method/methodnm/subCheck와 숫자 공고 ID, 정확히 7개 query만 허용한다.
- 공통 다운로드 경로: `/emwp/jsp/ofr/FileDown.jsp`, 같은 exact host의 HTTPS 443 GET만 허용한다. user_file_nm/sys_file_nm/file_path 이외 query, 다른 host/port/path, 이중 인코딩된 경로 이탈, script 추가 명령, POST는 거부한다.
- `goDownLoad`의 고정 세 문자열 인자만 해석한다. 사이트 JavaScript를 실행하거나 form action·base 태그·임의 다운로드 링크를 따라가지 않는다. 요청 값은 메모리에서만 사용하고 locator에는 공고 ID와 디렉터리+시스템 파일명의 SHA-256만 저장한다.
- 부산 남구의 V55 저장 URL은 HTTP일 수 있다. 확인한 동일 host/path/query만 HTTPS 요청으로 변환하며 원래 source URL/identity hash는 보존한다. 다른 기관·다른 경로의 HTTP 승격 또는 HTTPS 실패 시 HTTP fallback은 없다.
- 확인된 영역이 비어 있을 때만 NO_FILES다. 영역/폼 중복·누락, 해석하지 못한 링크/추가 파일 텍스트/버튼은 FAILED와 기존 발견 파일을 함께 반환한다. 11개 이상은 최대 10개를 유지하면서 LIMIT_EXCEEDED/불완전으로 반환한다. 비지원 형식도 발견 목록에 남기되 다운로드하지 않는다.

## 문서 역할 계약 수정

설계 6.1은 파일명만으로 NOTICE 등을 확정하지 않도록 규정한다. 이번에 확인한 네 기관과 기존 기업마당/대전 서구의 일반 첨부 영역은 파일 역할을 명시하지 않는다. 따라서 **모두 UNKNOWN으로 발견**하며, 기존 두 프로필의 파일명 기반 NOTICE/GUIDE/FORM/REFERENCE 추정을 제거했다. 파일명은 표시·형식 식별용이고 분류 본문에 섞지 않는다.

관리자가 실제 내용을 보고 확정한 역할은 기존 역할 변경 API/새 판정 경로를 사용한다. 실패 파일 재시도 시 저장된 MANUAL GUIDE가 재발견 UNKNOWN으로 덮어써지지 않는 테스트를 유지한다. 추후 명시적 문서 역할을 제공하는 출처가 있으면 별도 DOM·QA 근거를 갖춘 프로필로만 자동 역할을 지정한다.

두 기존 프로필의 실행 hash도 변경된다. 과거 고정 hash를 현재 작업에 재사용하지 않는다. 새 profile 목록/hash에 맞춰 초안을 저장하고 전체 QA를 다시 거쳐야 하며, 이 변경 자체가 정책 게시나 ENFORCE 활성화는 아니다. 기존 DB 행·과거 판정·v1 응답·migration은 수정하지 않았다.

## 실제 외부 검증

2026-09-12 03:04 KST 첫 전용 실행에서 기존 `AttachmentPinnedDownloadClient`와 `AttachmentFileTypeValidator`를 사용했다. 고정 공식 상세 4건의 모든 첨부 7개를 실제 내려받았다. 원문이나 파일명은 QA 보고서에 기록하지 않고 상태·hash·byte·형식·정리 여부만 저장한다.

| 기관 | 공고 ID | 파일 수 | signature 식별 | 파일 실제 byte |
|---|---:|---:|---|---:|
| 부산 남구 | 46034 | 4 | HWPX 4 | 383,297 |
| 대구 달성군 | 53932 | 1 | HWPX 1 | 89,664 |
| 대구 중구 | 34295 | 1 | PDF 1 | 47,368 |
| 함안군 | 43065 | 1 | HWPX 1 | 44,879 |

상세 4회+첨부 7회, 파일 합계 565,208 byte. 모든 임시 HTML/binary 삭제 확인. 원본은 영구 보관하거나 Git에 추가하지 않는다. 이 표본들은 출처/다운로드 구조 검증용이며, 운영 제목 필터를 통과한 수집 후보나 최종 수혜 대상이라고 의미를 바꾸지 않는다.

**파일 signature 식별은 격리 파서의 문서 내용 검증이 아니다.** HWPX의 ZIP 내부·실제 PDF 텍스트는 이 QA에서 파싱하지 않는다. HWP 실파일, 전체 출처, Linux 추출·worker DB 저장/분류·운영 화면은 미검증이다. 일반 root 테스트는 외부 요청을 하지 않으며 아래 전용 task만 명시적으로 실행한다.

```powershell
.\gradlew.bat attachmentProfileDiscoveryQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
```

전용 task는 `SaeolAttachmentProfileLiveQaTest` 4사례, 단일 JVM·사례 180초·각 요청 30초·파일 20MiB·사례 누적 80MiB 상한을 사용한다. 고정 표본의 파일 수/구조가 바뀌면 실패한다. 실패를 임의 기대값 변경으로 통과시키지 말고 공식 응답을 재검토해야 한다. metadata는 ignored `build/reports/attachment-profile-discovery-qa/*.json`, JUnit은 `build/test-results/attachmentProfileDiscoveryQa`에 생성된다.

03:11 KST 최종 전체 재실행에서도 4사례/7파일·동일 byte·원본 정리를 재확인했다. `:test :attachment-extractor:test bootJar :attachment-extractor:installDist attachmentProfileDiscoveryQa --rerun-tasks`는 2분52초 성공, root 1,519통과/196조건부 생략·추출기25통과·실사이트4통과다. Node 회귀7파일 116통과. 일반 root의 전용 실사이트 QA 1개 생략 표시는 별도 task의 실제 4사례 실행과 구분한다. 기존 조건부 DB/Linux 검증을 성공으로 바꾸지 않는다.

## 남은 차이와 Gate

- [x] 4개 exact-host 시스템 프로필·다중 파일 발견·기존 worker 등록 경로 연결.
- [x] 실제 공개 상세/다운로드 4사례·7파일의 식별 및 임시 원본 정리.
- [~] 단위 회귀는 4가지 HTML 구조·변조 URL·인코딩·부분/빈 영역·수량·역할·Spring 등록을 검증한다. worker 연결 시험의 HTTP/추출/DB 응답은 대역이며 실제 worker E2E가 아니다.
- [~] 화천군 LGS-000130은 후속 전용 `FileDownNew.jsp` POST 프로필을 구현했다. 인자를 해독/영구 저장하지 않으며 실제 두 공고·세 파일 다운로드를 확인했다. 격리 추출/worker DB 검증은 남는다. 같은 GET 프로필로 등록한 것이 아니다.
- [!] Docker Linux 엔진 named pipe 부재 재확인. WSL 목록에는 docker-desktop만 있었다. 실제 격리 추출·최신 PostgreSQL 통합 검증은 미완료다.
- [ ] 나머지 지자체/정부24 및 기존 두 프로필의 전체 실파일·Linux worker/DB·실패 복구 QA와 정책 게시 검증.
- [ ] 정확한 SHA 커밋/푸시·운영 배포, 승인된 정책/ENFORCE·기존 데이터 적용, 운영 역할별 브라우저 E2E.

실제 출처가 필요한 곳을 합성 성공으로 채우지 않는다. 이 4개 프로필 추가로 전체 41종 목록 파서나 223개 활성 수집원의 첨부 처리를 완료했다고 보고하지 않는다. 과거 운영 수집원 수는 현재값 재확인이 필요한 별도 snapshot이다.
