# 천안 공식 새올 연결과 제목 중단 검증

## 현재 단계 / Gate

- [x] 공식 상세의 제목·본문·첨부 영역 확인 및 천안 프로필 추가
- [x] 본문 분리·전체 첨부 발견·파일별 오류 분리 계약 테스트
- [x] 고정 공고의 제목 조합 미충족 시 본문·첨부 요청0 검증
- [~] **196/223 수집원(87.9%) 실제 다운로드 확인, 잔여27 유지**
- [ ] 미연결1: 울산 남구(079)
- [ ] 연결 후 다운로드 미확인26: 기존25개+천안(148)
- [ ] 천안 제목 적합 표본, 상시 목록 유입, 실제 파일, 운영 E2E

분모는 2026-09-28T15:56:12.116778+09:00 인벤토리의 활성 수집원이며 현재 운영 재조회나 고유 지자체 수가 아니다. 최소1파일 다운로드 비율과 전체 프로젝트 완료율을 구분한다. 엄격한 전체 첨부 집합 Gate는16/223, 오류 수집원47개 유지다. 제목 정책 중단은 전송 오류로 집계하지 않는다.

## 공식 경로 조사

기존 대표 홈페이지 목록의 실패와 별개로 공식 `eminwon.cheonan.go.kr` 상세가 응답함을 확인했다.

- 공고115419의 공식 상세에서 구분값 `01%2C04%2C05`는 HTTP400, 쉼표 표기 `01,04,05`는 HTTP200이었다. 인코딩 형태에 따른 관측 차이이며 서버 내부 원인을 확정한 것은 아니다.
- `epcCheck`·`not_ancmt_se_code`가 없는 기존 새올 7개 query 형식도 같은 공고에서 HTTP200이었다. 새 프로필은 이 실측 형식만 허용한다. 기존 저장 URL이나 query를 임의 변환하지 않는다.
- [공식 공고107953](https://eminwon.cheonan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=107953&subCheck=Y)의 제목은 `26년 1인가구 급식(밀키트) 지원사업 지원 대상자 모집공고`다. 직접 GET200, 공식 본문 셀과 HWP 링크1개를 확인했다. 링크 발견은 파일 다운로드 성공이 아니다.
- 공개 목록 검색 POST는20초 시간 초과, GET은 HTTP400 또는12초 시간 초과였다. 목록의 상시 자동 유입은 미해결이며 상세 접근 성공으로 덮지 않는다.

직접 구조 조사9요청(GET8/POST1), 요청별2MiB, GET12초/POST20초 상한으로 실행했다. TLS 검증·collector UA를 유지하고 redirect는 자동 추적하지 않았다. 검색 도구9질의·공식 페이지열기1회는 이 직접 요청 예산과 별도다. 조사 원문 HTML과 첨부 원본 파일은 저장하지 않았다. 구조 조사와 아래 제목 선행 수집 태스크를 구분한다.

## 구현 범위

- `CheonanNoticePage`: 정확한 HTTPS host·path·7개 query, 중복 query 거부, 단일 `form[name=form][method=post]`·98% 표·제목·본문 셀 검증.
- `CheonanAttachmentDiscoveryProfile`: 검증된 메모리 내 form 이름만 바꿔 기존 `SaeolGetAttachmentDiscoveryProfile`에 공식 첨부 전체 영역을 전달한다. 파일 GET·host/path 제한·10파일 상한·중복 제거·UNKNOWN 역할·미지원 형식·부분 오류 분리를 재사용한다.
- 본문은 중첩 첨부 표·담당 부서·메뉴·푸터와 분리한다. 기존 지역 프로필과 공통 다운로드 엔진은 수정하지 않았다.
- 바인딩은 `LOCAL_CHEONAN_GET_V1 / LGS-000148 / SPRING_BBS`. 바인딩은 지역과 parser 코드의 일치일 뿐 현재 목록 URL이 새 상세를 자동 제공한다는 증거가 아니다.
- 카탈로그 고정 표본은 reference-only이며 기대값 승인·규칙 변경·외부 공고 자동 활성화는 없다.

## 실제 수집 태스크 결과

2026-09-30 22:43:32 KST에 `CHEONAN-107953`을 기존 임시 DB DRAFT seed로 판정했다.

- `titleStage=COMBINATION_NOT_MATCHED`
- `titleReason=TITLE_COMBINATION_NOT_MATCHED`
- `status=TITLE_EXCLUDED_NOT_FETCHED`
- 본문·첨부 요청 예약0, 예약 바이트0, 파일목록0, 운영쓰기0
- `originalFilesRemoved=true`, 추출·정책 QA·기대값 승인false

현재 seed에서 이 제목이 대상·지원 조합을 충족하지 못했으며 성공률을 높이기 위한 키워드 수정이나 제목 우회는 하지 않았다. 이 결과는 태스크가 정상적으로 중단된 증거이지 본문·첨부 다운로드 성공이 아니다. 파일 해시는 없다.

보고서: `build/reports/attachment-regional-collection/CHEONAN-107953.json`. SHA-256은 영수증 인덱스에 기록했다.

| 항목 | SHA-256 |
| --- | --- |
| 천안 프로필 | `91d5cba445384cddb1ea93e2279bdc7d93cab93882fbd45ba96b2735a9fb0a4e` |
| 이번 producer 클래스 | `99427d305bb8148d1b4e3c41306905a1985b2de2cc733c8ce976d7a05e86fcf1` |
| 갱신한 로컬 인벤토리 산출물 | `2d62e8d666e46afab8969eb823bbc8202f11231a4d6f70abe683b8d044163635` |

기존360기록·276공고·285카탈로그사례를 보존하고 **361기록/277공고**, **286사례·222대상(reference-only285+기존기대값1)**으로 확장했다. 시스템 프로필223개(지역222+기업마당1), 미연결1개다. 과거 보고서에는 당시 producer hash를 유지한다.

## 실행 명령 / 검증

1. `.\gradlew.bat --no-daemon :test --tests '*CheonanDownloadContractTest' --tests '*SeocheonDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage' --tests '*AttachmentProviderQaCatalogTest.reviewedExceptionFixtureKeepsAllReferencesAndCannotFillNormalCoverage' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AnnouncementAttachmentIntakeServiceTest' :bootJar`
   - 251통과·1조건부생략·실패0, bootJar 통과. 생략은 인벤토리 export 입력 미설정이었다.
2. 프로세스 한정 인벤토리 환경변수를 설정하고 `:test --tests '*CheonanDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=CHEONAN -PsanebCollectionWindowsTrust=true` 실행.
   - 로컬8개 전부통과, 관측 태스크는 제목 중단으로 종료0. 환경변수는 복원했다. 인벤토리는 보관된9월28일 읽기 전용 영수증과 새 로컬 코드를 대조했으며 운영을 다시 조회하지 않았다.
3. Node23개 전부통과, 361기록/277공고·196/223·잔여27·미연결1·엄격16 재현, `git diff --check` 통과를 확인했다.

## 다음 단계와 미실행 항목

천안은 제목 조합을 충족하는 공식 표본과 실제 파일 다운로드를 추가 확인해야 한다. 기존 대표 홈페이지 목록과 새올 상세 연결도 상시 수집 준비의 별도 후속이다. 울산 남구의 목록 응답 문제, 다른25개 지역의 미확인 다운로드 복구를 계속한다.

운영 DB·endpoint·worker·정책·ENFORCE·재분류·배포, Flyway V85, HWP 추출기1.0.16은 변경하지 않았다. 전체 Java 테스트·운영 E2E·AWS 확인은 미실행이며 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 과거 연제·구례 임시 폴더 정리 차단은 별도 보류다. 이번 결과는 작업 브랜치 `[skip deploy]` 범위이며 전체 goal은 미완료다.
