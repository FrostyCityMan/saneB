# 철원군 고시·공고 본문·첨부 모델

## 범위와 완료 경계

2026-09-15 작성. 제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증의 기존 설계를 유지한다. 철원군 `LGS-000129`의 공식 고시·공고 게시판에 한정한 연결과 회귀 시험이다. 다른 철원 게시판·다른 기관에 일반화하지 않는다.

- 공통 첨부 엔진은 6종 그대로다. 등록 첨부 모델은 17→18개, 전용 본문 연결은 16→17개다. **철원 신규 연결의 실제 수집 성공까지 확인한 수량이 아니다.**
- DB/API/UI/기존 Flyway V1~V83 변경 없음. 외부 공고 자동 ACTIVE, 운영 정책 게시·ENFORCE·기존 데이터 실행 없음.
- 공식 참조 catalog는 24→27개, 보관된 기대값은 기존 태백 1개 그대로다. 새 BBS 코드에서는 그 지문이 달라 `PROFILE_CHANGED`이며 **현재 실행 가능한 기대값은 0개**다. 철원 3개는 `expectation: null`/`REFERENCE_ONLY`다. 정상 공고·전체 Provider QA 통과로 계산하지 않는다.

## 기관 및 관측 근거

V61 `correct_general_notice_sources_to_official_legal_boards`와 V62 `apply_corrected_legal_notice_parser_qa_results`의 seed에서 목록은 `www.cwg.go.kr/www/selectBbsNttList.do?bbsNo=25&key=1226`, 목록 parser는 `SAEOL_GOSI`다. seed 근거이며 현재 운영 binding을 재조회한 결과는 아니다.

| 고정 공식 참조 | 확인 수준 |
|---|---|
| [288915 소상공인 카드수수료 지원](https://www.cwg.go.kr/www/selectBbsNttView.do?key=1226&bbsNo=25&nttNo=288915) | 직접 HTML 1회 확인. 공식 파일 셀과 HWPX 다운로드 링크 3개 관측 |
| [291529 다자녀가정 특별지원](https://www.cwg.go.kr/www/selectBbsNttView.do?key=1226&bbsNo=25&nttNo=291529) | 공식 검색 참조와 HWPX 표시 1개. 로컬 직접 응답은 시간 초과 |
| [291245 신혼부부 주거자금 대출이자 지원](https://www.cwg.go.kr/www/selectBbsNttView.do?key=1226&bbsNo=25&nttNo=291245) | 공식 검색 참조와 HWPX 표시 1개. 직접 다운로드 미실행 |

최초 HTML에서 `table.p-table.block`, 소속 제목 `span.p-table__subject_text`, `파일` 행의 `ul.p-attach > li.p-attach__item > a.p-attach__link`와 아이콘/파일명 span 2개를 확인했다. 링크는 `./downloadBbsFile.do?atchmnflNo=<파일번호>&bbsNo=25&nttNo=<현재공고번호>`다. 해당 표본의 첨부 영역에는 미리보기가 없었다.

**관측 한계:** 최초 출력에서 본문 셀을 비식별 처리하면서 정확한 본문 attribute와 바깥 wrapper를 보존하지 않았다. 후속 목록/같은 상세/다른 상세 조회가 각 20초 시간 초과로 3회 실패하여 동일 조건의 반복 요청을 중단했다. 서버 원인·접근 차단 여부는 미확인이다. 따라서 본문 `td[title=내용]` 및 `div.p-wrap.bbs.bbs__view` 조합은 현재 엄격한 구현 계약이지 철원 실제 HTML 재확인 결과가 아니다. 누락되면 실패로 남으며 페이지 전체로 대체하지 않는다. MIME·Content-Disposition·실제 파일 signature·추출 품질도 미확인이다.

## 구현 계약

1. `LOCAL_CHEORWON_BBS_V1`은 provider/기관 code/목록 parser/원문 URL canonical hash를 함께 검증한다. HTTPS/443·정확한 host/path·게시판25·메뉴1226·양의 공고번호가 필요하다. 태백과 게시판 번호가 같아도 태백 메뉴·HTTP 예외를 공유하지 않는다.
2. 새 `NOTICE_BOUND` 다운로드 모드는 정확히 `atchmnflNo`, `bbsNo`, `nttNo`만 허용한다. 누락/중복/미지 인자·게시판 불일치를 거부하고, descriptor 생성 시 현재 상세 공고번호와 일치해야 한다.
3. 공통 다운로드 흐름은 최초 요청을 승인 predicate에 전달한다. 철원은 redirect에서도 동일 path·공고·파일 query를 요구한다. 인자 순서 변경은 허용하되 다른 파일·다른 공고·상세↔다운로드 전환은 거부한다. 기존 기관의 기본 승인 계약과 요청 상한4는 보존한다.
4. 첨부 목록 전체를 검사한다. 잘못된 항목·잔여 링크·미지원 형식·중복 충돌·10개 초과를 숨기지 않는다. 알려지지 않은 미리보기는 불완전 발견으로 남고 요청하지 않는다. 파일명/아이콘으로 NOTICE나 FORM 역할을 추정하지 않는다.
5. `STRICT` 파일 헤더 정책은 MIME 오기·구형 MIME·UTF-8 헤더 복원 예외를 허용하지 않는다. 공식 파일 응답을 관측하기 전 다른 기관의 예외를 차용하지 않는다.
6. 본문은 단일 공식 표에 직접 소속된 비어 있지 않은 제목과 본문 셀만 사용한다. 메뉴·첨부명·담당자 행을 본문으로 합치지 않고 본문 내 실제 지원/제외 문구와 표 텍스트는 보존한다. 구조 모호/변경은 `BODY_SELECTOR_CHANGED`, 빈 정제 본문은 `BODY_TEXT_EMPTY`다.
7. 새로운 다운로드 모드와 공통 승인 인터페이스/흐름은 `AttachmentProfileFingerprint`의 계산 입력이다. 따라서 BBS뿐 아니라 기존 다른 기관을 포함한 모든 profile·실행 코드 지문이 바뀐다. 동작 계약을 유지해도 과거 정책 QA를 새 코드의 성공으로 재사용하지 않는다. 기존 태백 고정 기대값을 새 실행 결과에 맞춰 덮어쓰지 않으며 새 지문에 대한 검토·실파일 재검증 후 별도 갱신해야 한다.
8. 검색 결과의 세션 경로는 catalog·요청·문서에 복사하지 않는다. 현재 철원 모델은 세션 없는 canonical 상세 경로만 지원한다. 목록 원문이 세션 경로를 쓰는 경우 해당 실제 계약을 추가 검증해야 하며 현재는 `PROFILE_REQUIRED`다.

## 검증과 다음 Gate

### 2026-09-15 후속 접속 확인

TLS 검증을 유지한 10초 상한 HEAD 요청은 HTTP200을 반환했다. 이 상태 변화 후 실제 본문 수집을 재검증했으나 고정3건 모두 TIMEOUT(6.474/6.022/6.024초)으로 실패했다. HTTP200을 본문 성공으로 계산하지 않으며 파일 다운로드로 확대하지 않았다. 같은 조건의 후속 재시도는 중단했다. 응답의 구조·차단 원인은 여전히 미확인이다.

처음에는 일반 `:test`에 환경변수를 주었지만 이 프로젝트는 일반 test에서 외부 QA를 false로 고정하므로 대상 시험이 실행되지 않았다(`No tests found`, 외부 요청0). 이후 실제 전용 task인 `attachmentProfileDiscoveryQa --tests '*StandardBbsBodyContentLiveQaTest.readsCheorwonBodyWithoutRequestingFiles'`와 Windows 신뢰 저장소를 사용했다. 이 전용 실행의 JUnit은3실행/3실패/생략0이다. 환경변수나 타임아웃을 바꿔 성공으로 위장하지 않았다.

- [x] 표적 회귀: 기관/hash/게시판 경계, 본문 정제, 전체 첨부 분모, 공고 소속, redirect 파일 소속, 미지원·변경·중복·상한 검증. 최초 표적 실행 54초 성공.
- [x] catalog 참조·기존 기관/공통 다운로드 전체 회귀와 패키지 검증. 첫 전체 회귀에서 QA 실행기 mock의 새 승인 overload 미연결47건과 태백 구지문의 무효화4건을 확인했다. mock은 실제 기본 메서드를 호출하도록 연결했다. 배포 catalog의 태백 지문은 보존하고 `PROFILE_CHANGED`/실행0을 별도 시험한다. 기간·제목 규칙·파일 계약의 양성 시험은 명시적인 메모리 전용 fixture로 유지하며 배포 catalog를 변경하지 않는다. 표적202건/생략0 통과 후 최종 전체2675건=2411통과/264조건부 생략/실패0, 패키지20통과/생략0이다. 추출기88건·웹 JAR·probe는 기존 산출물 재사용(UP-TO-DATE)이며 새 실파일 검증이 아니다.
- [ ] 공식 3개 공고의 전용 BODY 응답 및 전체 파일 다운로드/signature.
- [ ] 실제 추출→worker→임시 DB→API와 사전 검토한 기대값 비교.
- [ ] 전체 Provider/형식 적용성·정상 다중 첨부 검증, 새 SHA 배포·운영 업무 브라우저 E2E.

실제 사이트 QA는 기존 `SANEB_ATTACHMENT_PROFILE_QA=true` opt-in의 `StandardBbsBodyContentLiveQaTest.readsCheorwonBodyWithoutRequestingFiles`, `StandardBbsAttachmentProfileLiveQaTest.discoversCheorwonFilesBoundToTheCurrentNotice`로 분리했다. 후자는 사례당 상세1MiB/파일20MiB/합산50MiB, 고정 파일 수3·1·1, 실패 시 원문·임시 파일 정리를 검증한다. 일반 회귀에서 이 opt-in이 생략되면 실제 사이트 성공으로 표시하지 않는다. 접속 조건 변화 확인 후 제한된 실제 검증을 재개한다.

전체 ATT-001~062/9개 Gate는 미완료이며 이 모델 하나의 합성 시험으로 전체 수집 성공을 선언하지 않는다.

최종 명령: `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1`, 2026-09-15 15:49 KST/5분11초 성공. Node 실행 스크립트15통과/Linux2생략, 변경분 자격증명 패턴0·diff 검사 통과. 소유 단발 Node/Gradle 종료·Java0을 확인하고 기존 사용자/앱 Node와 `output/`은 보존했다. 새 SHA Linux/배포와 철원 실제 외부 QA는 별도다.
