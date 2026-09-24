# 부산 남구 지원사업 고정 표본과 3단계 관측

## 범위와 판정

전체 격리 QA 승인 범위에서 기존 `LOCAL_BUSAN_NAMGU_GET_V1` 프로필의 지원사업3건을 확인했다. 새 엔진/프로필을 추가하지 않았으며 코드 등록7엔진/19프로필은 그대로다. 목표인 제목→정제 본문→전체 첨부 텍스트→관리자 최종 검증을 유지한다.

**관측3건은 통과했지만 HWP3파일 모두 PARTIAL_TEXT다. 정상 공고·정상 기대값·정책 QA 통과가 아니다.** 관리자 검수와 수동 원문 확인이 남는다. 운영 DB/정책/worker 설정/공고 활성화/기존 데이터/배포는 변경하지 않았다.

## 공식 표본 선정과 계약

기존 남구46034는 구조 검증용 과거 표본이며 지원 후보나 정상3공고를 증명하지 않는다. 공식 검색 결과에서 아래 지원사업을 확인한 뒤 프로필이 허용하는 기존7개 query의 고정 상세 주소로 직접 제목과 전체 첨부를 확인했다. 검색 도구의 canonical 주소 열기는 실패했으나 실제 pinned Java 요청은 성공했다. 검색 결과만으로 성공을 기록한 것이 아니다.

| 고정 표본 | 공식 제목 | 직접 확인한 파일 |
|---|---|---|
| [44466](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=44466&subCheck=Y) | 2026년 청년 사업자 임차료 지원사업 참여자 모집 공고 | HWP1 |
| [44381](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=44381&subCheck=Y) | 2026년 남구 청년 자격시험 응시료 지원사업 참가자 모집 공고 | HWP1 |
| [42871](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=42871&subCheck=Y) | 2025년 남구 청년 자기개발 도서구입비 지원사업 참여자 모집 변경공고 | HWP1 |

- 기관 `LGS-000034`, 목록 parser `SAFE_SAEOL_EMINWON_LEGACY`, 기존 exact host/path/query·HTTPS·공인주소/redirect·파일 signature 경계를 유지했다. 현재 운영 목록 binding을 다시 조회한 것은 아니다.
- 제목은 기존 실측 구조인 유일한 POST form1→table.table_03→직접 소속 th[colspan=4]에서 확인한다. 중복·다른 제목·중첩 표에서 제목 차용을 거부한다. 임의 제목/URL/기관/파일을 실행 입력으로 받지 않는다.
- classpath catalog schema2에 `NAMGU-44466/44381/42871`의 canonical identity를 추가했다. 총33참조/11기관/저장된 기대값1/정상0이다. 새3건은 모두 expectation:null이다. 태백 기대값 내용·시각·profile hash와 schema1/기존 migration/v1 API는 수정하지 않았다.
- 새 catalogVersion은 `2026-09-24-namgu-support-references-v2`다. 전체 catalog/코드 지문이 달라지므로 이전 정책 QA snapshot을 새 catalog 성공으로 재사용할 수 없다. 운영에 설치된 catalog30과 구분한다. 등록19개 중 catalog 참조가 없는 프로필은9→8개다. 전체 미등록 기관은 별도로 남는다.

## 1. 로컬 실제 발견·다운로드·형식 검증

`attachmentProfileDiscoveryQa --tests '*SaeolAttachmentProfileLiveQaTest.fixedNamguSupportReferencesValidateTitleAndWholeFileSignature'`를 Windows 신뢰 저장소로 실행했다. TLS 검증을 해제하지 않았다. 3실행/3통과/실패·생략0, JUnit3.723초다.

사례별 최대8요청/24MiB/120초, 파일20MiB 상한이다. 실제는 각 상세1+파일1로 **총6요청/248,801예약byte**, 파일 합계232,960byte다. Windows에서 외부 binary를 파싱하지 않았으며 각 임시 HTML/HWP 삭제를 확인했다. 비식별 근거는 `build/reports/attachment-profile-discovery-qa/LOCAL_BUSAN_NAMGU_GET_V1-{id}.json`이다.

## 2. 서울 제목→본문→HWP 격리 추출·분류

명시 `NAMGU_OBSERVATION` 모드만 실행했다. 기존 태백/옥천/보은 모드는 보존하고 새 모드는 실제3건·각20요청/32MiB로 한정한다. 서울 최대60요청/96MiB/20분·CPU1개/메모리768MiB/임시1GiB다. 현재 코드를 임시 패키지로 전송하며 운영 설치 경로를 교체하지 않는다. 동일 계획 재시작과 다른 표본으로의 변경을 거부한다.

- execution `f5bada861ef2444fb0cacad8543804ff`, SSM `9156b397-2da9-4e25-be4e-8f933145d084`.
- 관측3/3·실패/생략/중단0,46.347초. 실제 예약12요청/6,540,257byte다. 이번 고정3건의 로컬+서울 합계18요청/6,789,058예약byte이며 웹 검색/검색 도구 접근을 포함한 전체 네트워크 호출 수는 아니다. 과거 다른 남구 공고의 사용량을 초기화하는 기록도 아니다.
- 임시 원본/격리 프로세스 정리, unit inactive, 운영 JAR 불변, health UP을 확인했다. 소유 S3 전송 객체 부재와 plan.cleaned=true를 확인하고 로컬 자기 package.zip만 hash/경로 대조 후 삭제했다. 재생성 가능한 전송 패키지는 제거했고 plan/result JSON 영수증은 보존했다.

| 표본 | 제목 / 본문 | 추출 문자 / block | 품질 / 최종 판정 |
|---|---|---:|---|
| NAMGU-44466 | COMBINATION_MATCHED / AVAILABLE·202자 | 5,031 / 345 | PARTIAL_TEXT / REVIEW_REQUIRED |
| NAMGU-44381 | COMBINATION_MATCHED / AVAILABLE·358자 | 3,307 / 160 | PARTIAL_TEXT / REVIEW_REQUIRED |
| NAMGU-42871 | COMBINATION_MATCHED / AVAILABLE·576자 | 4,152 / 267 | PARTIAL_TEXT / REVIEW_REQUIRED |

전체 목록 각1파일·FOUND/발견 complete, 본문 attempts1/redirect0을 확인했다. HWP binary hash는 선행 로컬 다운로드와 세 파일 모두 일치한다. 본문 판정은 ACCEPTED이나 첨부 불완전으로 최종 판정이 REVIEW_REQUIRED이고 `isWholeTextAnalysisComplete=false`다. 파일 제목이나 본문 성공으로 첨부 완전성을 대체하지 않는다.

상시 worker의 DB/API 저장·검수 확인/DRAFT 생성 증거는 이번 관측에 포함되지 않는다. 별도 loopback 임시 DB의 Flyway DRAFT 규칙을 읽어 사용했지만 운영 DB는 사용하지 않았다. 이번 결과에는 HWP 레코드 구조 진단이 전송되지 않았으므로 부분 추출의 직접 원인을 그림/표/양식으로 단정하지 않는다. 기존 extractor의 검증된 hwpStructure metadata를 관측 보고서에 연결하고 필요한 추가 관측의 예산을 대조하는 것이 다음 분석 단계다.

## 재현 지문

- 실행 codeHash: `32d813afcc3177697013a9eea4b1b607aca062b6b7106f90815ea4a836cce2ef`
- probeHash: `08342ab6de2aa618d5ee192923349b5ad68d38cb24d6cf2203bbb984e17e4345`
- 전송 ZIP SHA256: `fb2b7f85709201499720ab58e227567fdf4fe2562e0b057efe9ff661c171515f`
- 운영 불변 JAR SHA256: `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`
- 로컬 영수증: `build/temporary-bbs-qa-f5bada861ef2444fb0cacad8543804ff/plan.json`, `result.json`.

## 검증과 남은 Gate

- 최초 제목 구조 단위 시험3건 중 중첩 표 차용1건이 실패했다. assertion을 완화하지 않고 중첩 표를 명시적으로 거부했으며3건 재검증과 실제 사이트3건이 통과했다.
- 참조·제목·catalog·probe 표적 시험/QA 패키지20/bootJar는1분56초 성공. Node/Bash4·Python21건 실패/생략0. CI에는 단위 검증만 연결되며 남구 실파일 요청을 자동 실행하지 않는다.
- 확대 회귀 첫 실행은 기존 정책 snapshot 시험의 catalog30 고정 수량1건이 실패했다.33개 전체 고정 참조와 실행0·정책QA false를 유지하도록 수량을 갱신했다. 재실행 `:test --tests 'com.saneb.domain.announcementattachment.*' :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`은3분22초 성공이다. XML1960=1938통과/22조건부 생략·실패/오류0, 패키지20건 실패/오류/생략0이다. bootJar는 동일 코드 산출물 UP-TO-DATE이며 Node 확대12·Python21건 실패/생략0도 확인했다.
- 선행caba129 [Linux36002040454](https://github.com/FrostyCityMan/saneB/actions/runs/36002040454) success/artifact를 확인했다. root3000=2715통과/285조건부 생략·실패/오류0, 별도 extractor122·패키지20·jobPG206·migration18·정책부모PG2·runtime5·workerPG12·Flyway3은 실패/오류/생략0이다. 이번 남구 변경의 CI 증거로 대체하지 않는다.
- 남구 정상3공고와 정상 다중 첨부1건은 아직 충족하지 않았다. 단일 HWP3건을 다중 첨부나 정상3건으로 계산하지 않는다. 전체 Provider/형식·부분 추출 해소·DB/API worker·정책 게시·기존 데이터·동일SHA 운영 배포/브라우저 E2E가 남는다. 브라우저는 현재 명시 요청 정책에 따라 미실행이다.

전체9Gate=8부분/1차단, goal active를 유지한다.
