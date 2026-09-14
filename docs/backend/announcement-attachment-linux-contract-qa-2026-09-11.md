# 첨부 DB·작업 계약 Linux 검증 경로

## 범위와 현재 상태

### 2026-09-15 — 미확정 역할의 고정 항목명·위치 관측 보완

실행16의 완전 추출5건이 UNKNOWN인 원인을 구분하기 위해 기존 Linux 공식 파일 관측에 `roleStructureObservation`을 추가했다. 원문·파일명·개인정보·locator를 내보내지 않고, 고정 사전 코드와 token의 정수 index, 줄/블록 위치, 단독 항목명·줄 끝·콜론 유무만 기록한다. 최대20,000줄을 검사하고 최대128개 신호를 내보내며, 상한 초과는 `isTruncated/isLineLimitReached`로 구분한다. 블록 경계를 넘거나 위치가 불확실한 신호를 신뢰 가능한 단일 근거로 표시하지 않는다.

관측은 기존 역할 판정 결과를 변경하지 않는다. 부분 추출/OCR에는 역할 구조 관측을 부여하지 않으며, 이 metadata를 기대값 승인·정책 QA·본문/worker/운영 성공으로 사용하지 않는다. 로컬 관측 계약8건·역할 분류22건은19초에 통과했고 bootJar는 UP-TO-DATE다. 새 관측의 실제 Linux 결과는 별도로 확인해야 한다. 이 증분에는 production 코드·migration·API·운영 데이터 변경이 없다.

### 2026-09-15 — 공식7파일 격리 추출 관측 완료, 역할·정책 검증은 미완료

[실행16 / 34876277622](https://github.com/FrostyCityMan/saneB/actions/runs/34876277622), SHA `a314aa9576f28db3376e2b828d7dc4789f70d20b`는 **workflow 성공**이다. 이는 전체 운영/정책 QA Gate 통과가 아니다.

- 실제 DB job192, migration3+분할14, worker11, Linux 합성12파일(1시험), 부모 연결/취소2건은 생략0·실패0이다. 독립220/220 통과·미실행0·container실패0, 전용 계정 정리SUCCEEDED. V83 1,001건 분할11.593초, tuple동등성0.071초다. 속도는 해당 실행의 관측값이다.
- root2403건=2155통과/248조건부 생략. 일반 root의 조건부 생략을 별도 전용 task 통과와 중복 합산하지 않는다. 새올2기관 본문 증분은 이 SHA 이후의 변경이므로 이 실행의 성공 범위에 포함하지 않는다.
- 기업마당3공고는 모두 제목 조합 Gate 및 전체 첨부 발견을 통과했고,7파일 전부 실제 다운로드·형식 검증·Linux 격리 처리를 완료했다. 직전 FILE_SIGNATURE 차단은 해소됐다. 원본 정리3/3, 정책 승인/운영 쓰기0이다.

| 공고/파일 순서 | 형식 | 실제 추출 품질 | 글자 수 | 역할 규칙 결과 |
|---|---|---|---:|---|
| SEMAS120120 / 1 | PDF | PARTIAL_TEXT | 31,498 | 미판정 — 완전 추출 아님 |
| ANYANG117918 / 1 | HWP | COMPLETE_TEXT | 565 | UNKNOWN / ROLE_STRUCTURE_INCOMPLETE |
| SDM124628 / 1 | HWPX | COMPLETE_TEXT | 1,407 | UNKNOWN / INITIAL_HEADING_REQUIRED |
| SDM124628 / 2 | HWPX | COMPLETE_TEXT | 1,469 | UNKNOWN / INITIAL_HEADING_REQUIRED |
| SDM124628 / 3 | HWPX | COMPLETE_TEXT | 3,230 | UNKNOWN / ROLE_STRUCTURE_INCOMPLETE |
| SDM124628 / 4 | PDF | OCR_REQUIRED | 0 | 미판정 — OCR 미구현 범위 |
| SDM124628 / 5 | HWPX | COMPLETE_TEXT | 2,413 | UNKNOWN / MIXED_DOCUMENT_ROLES |

공식 관측3건의 결과는 모두 `OBSERVED_NOT_VALIDATED`이며 `isPolicyQaPassed/isExpectationApproved/isBodyPipelineVerified=false`다. 현재 완전 추출5/7이고 완전 PDF 표본은0이다. UNKNOWN5건을 NOTICE로 승격하거나 이3공고를 정상 후보3건으로 세지 않는다. catalog의 참조9/실행 기대값0도 그대로다. 실제 텍스트 구조와 역할 근거를 검토하여 양식·참고자료·공고문 구분을 보완하고, 별도 완전 PDF 및 전체 출처 기대값/형식 적용성을 확인해야 한다. OCR·부분 추출과 역할 미인식은 서로 다른 잔여다.

metadata/XML은 `build/qa-results/run-34876277622`에 보관했다. 원문 파일·텍스트·파일명은 artifact에 없다. 같은 SHA의 운영 배포/정책 게시/ENFORCE/기존 데이터 APPLY/운영 브라우저 검증은 아직 없으며 **Not ready**를 유지한다.

### 2026-09-15 — V83 실제 DB 통과·공식 전체 파일 헤더 차단 확인

[실행15 / 34873944066](https://github.com/FrostyCityMan/saneB/actions/runs/34873944066), SHA `8fe54d094970f9be4a2dbabf87903be28d6f7a1d`는 **전체 workflow 실패**다. 실패 단계는 공식 파일 관측이며 DB 계약 단계와 구분한다.

- [x] root2397건=2149통과/248조건부 생략, extractor25, 패키징20, 실제 job192, migration3+분할14, worker11, Linux 합성12파일(1시험) 및 bootJar/설치 생성 통과. 전용 DB/runtime/worker 시험은 생략0이다.
- [x] 독립 namespace 실제 계약220/220 통과·실패/생략/미실행/container실패0. 부모 실제 연결·취소2/2 통과, 전용 계정 정리 SUCCEEDED(동일 UID14 thread/부모 JVM6 thread 관측).
- [x] V83 fresh/upgrade/기존 checksum·deferred trigger 유지와17개 tuple 동등성 비교가 실제 PostgreSQL에서 통과했다. 1,001건 전수 시험20.636초, 직전 실행14의63.932초보다 짧았다. runner 실행 조건 차이가 있으므로 이를 고정 성능 보장 또는 모든 간헐 실패 원인의 확정으로 해석하지 않는다.
- [!] 공식3공고의 빈 템플릿 제목 처리 수정은 통과했고 발견7/다운로드7개(PDF2/HWP1/HWPX4)를 확인했다. 그러나7개 모두 FILE_SIGNATURE 단계 실패로 격리 추출은 실행하지 못했다. 원본 정리3/3, 운영 쓰기0, 기대값 자동 승인0이다.

고정 metadata artifact는 `build/qa-results/run-34873944066`에 받았다. 파일명·원문·응답 헤더·다운로드 폼을 보고서에 넣지 않았다. XML의 root 조건부 생략과 별도 실제 DB task 실행을 중복 성공으로 합산하지 않는다.

추가 읽기 전용 진단에서 최신 공식 상세가 제공하는7개 파일 URL에 GET 응답 헤더와8-byte prefix만 확인했다. 모두 HTTP200/application/octet-stream이며 PDF/HWP/ZIP signature 및 표시 확장자가 일치했다. 각 Content-Disposition에는 Latin-1로 읽힌 UTF-8 octet의 C1 문자가5~22개 있었고, 엄격한 UTF-8 복원 후 제어문자는0이었다. 따라서 기존 엄격한 복원 기능을 기업마당 프로필에만 명시 적용한다. 공통 기본 거부·경로 이탈/제어문자/형식 불일치 검사는 유지하며 profile hash는 변경된다. 최초 보관 URL을 이용한 HEAD403/GET500은 성공 진단으로 사용하지 않았다.

수정 후 실제 Linux 격리 추출·역할 관측은 재실행해야 한다. 이7개의 다운로드/헤더 확인을 텍스트 판정·정상 후보·Provider QA 통과로 계산하지 않는다. 운영 정책 게시/ENFORCE/기존 데이터 적용·동일 SHA 배포/운영 브라우저는 미실행이며 **Not ready**다.

후속 기업마당 헤더/대전 서구 본문 증분의 로컬 표적53건·bootJar는28초에 통과했다. 전체 `test bootJar attachmentContractQaTest --no-daemon --max-workers=1`은3분11초 성공: root2403건=2156통과/247조건부 생략/실패0, 독립 패키징20/20통과. extractor 시험은 변경 없이 UP-TO-DATE이며 이번 실제 재실행으로 세지 않는다. Node 보고서 판정기10/10도 통과했다. 기존 Windows native initdb 실패를 해소한 것이 아니며247건의 조건부 생략을 실제 통과로 세지 않는다. 이번 소스의 Linux/공식 파일 재검증은 별도 SHA에서 수행한다.

### 2026-09-14 — 변경된 3단계 흐름의 Linux QA 재개

- GitHub 저장소 소유 계정의 push 권한을 현재 API로 확인했다. 아래의 과거 접근 차단 기록은 현재 권한 상태가 아니다.
- 기본 브랜치를 변경하거나 배포 workflow를 실행하지 않고 `codex/attachment-three-stage-linux-qa` 브랜치에 검증 범위를 게시한다. 이 정확한 브랜치의 push와 수동 실행만 QA trigger로 허용하며, 운영 자격증명·OIDC·정책 활성화는 사용하지 않는다.
- 제목 제외 후 상세/첨부 요청 금지, 본문 판정 뒤 첨부 분석 지속, 기술 미완료와 최종 검증 후보 분리라는 [변경된 처리 설계](announcement-three-stage-filtering-workflow-2026-09-14.md)를 유지한다. 새 대기열 Node 18사례와 실제 DB 9상태·29행/전체 count 검증을 Linux 실행 범위에 포함한다.
- 현재 독립 계약은 job192·migration2·backfill13·worker8=215건이고 additive migration은 V81까지다. 다섯 번째 실행에서 실제215건이 통과했으나 정책 부모 연결2건은 실패했다. 아래 첫 재개 당시213건 목록과 이전 실패를 현재 성공으로 혼동하지 않는다.
- 변경한 workflow 구조 테스트 5건과 대기열/보고서 Node 28건은 로컬 통과했다. 이전 전체 로컬 결과 및 환경 생략은 최신 실증 결과와 분리한다. 운영 배포 판정은 계속 **Not ready**다.

첫 원격 결과: [34824820039](https://github.com/FrostyCityMan/saneB/actions/runs/34824820039), SHA `bd148024611284b6c8ca2d6997e5ed2e87309721`, **실패**. 실제 PG job192건 중147통과/45실패/생략0이다. V81까지의 초기 migration은 수행됐지만 migration 전용 회귀·격리 runtime/worker·독립 산출물 실행은 이 실행에서 미실행이다. 컬럼 매핑39건과 시험 fixture6건을 수정 후 같은 범위로 다시 실행한다. 이후 Gradle `--continue`로 독립 task 실패도 함께 모으되 실패 종료 코드와 보고서 Gate를 유지한다.

두 번째 [34825668605](https://github.com/FrostyCityMan/saneB/actions/runs/34825668605), SHA `17243d54a132598d37e13753482eaef8fe6a9959`도 **실패**. job191/192, 첨부 migration2/2, 분할1/13, Linux 합성12파일 격리12/12, worker7/8통과를 확인했다. 분할의 GROUP BY parameter SQL 오류와 지연 제약/미지원 파일 상태의 시험 기대값을 수정 후 재검증한다. 독립 실행에 포함할 DB2개 parameterized 입력을 고정 @Test로 분리하여 사전 목록은215건으로 일치시켰다. 독립 namespace 전체 실행과 정책 부모 연결은 아직 미실행이다.

세 번째 [34826506160](https://github.com/FrostyCityMan/saneB/actions/runs/34826506160), SHA `6dc12e12073e6e46a9ab7566380949eb9cd48c42`는 주 Gradle 검증을 통과했다. job192/192, migration2/2, 분할13/13, Linux 합성12파일 격리12/12, worker8/8, root2064통과/241조건부 생략, extractor25·독립 QA 패키지15·Node152통과 및 bootJar/설치 패키지 생성 성공이다. 그러나 독립 namespace 실행은 `CLEAN_ENVIRONMENT_REQUIRED`로 실패했고 임시 자원 정리는 성공했다. 이후 정책 부모 연결은 미실행이므로 workflow 전체는 실패다.

원인: bubblewrap의 `--clearenv`는 `PWD`를 제거하지 않으며 `--chdir /work`에 맞춘 값을 유지한다([공식 v0.6.1 문서](https://github.com/containers/bubblewrap/blob/v0.6.1/bwrap.xml)). 실행기의 허용 목록에서 이 고정 값이 누락됐다. `PWD=/work`만 검증하도록 수정하고 누락·다른 경로·경로 정규화 표현과 외부 설정/DB 환경변수는 계속 거부한다. 보안 격리나 전체 시험 분모를 완화하지 않는다. 수정 뒤 전체 원격 검증을 다시 실행해야 한다.

네 번째 [34827786609](https://github.com/FrostyCityMan/saneB/actions/runs/34827786609), SHA `bd92dff3068363b8a4c8a2795ce2609cfc3d523d`: 주 Gradle 검증은 통과했고 독립 namespace도 환경 검사를 통과하여215건을 모두 실행했다.214통과/1실패/생략·미실행·container 실패0, 임시 원본/DB 정리 성공이다. 실패 case hash를 고정 JUnit ID와 대조한 결과 `full1001InventoryReservesBothSegmentsWithoutDuplicateOrNewArrivalAndAccountsAllDimensions`였다. 같은 SHA의 일반 실행은 이 시험이71.123초에 통과했으며, 독립 실행기의 기본 시험 제한은60초다. 이 시간 충돌을 원인으로 판단하여 해당 대량 시험만 명시적120초로 한정한다. 전체 namespace600초/정책 lease/DB transaction30초 및1,001건 전수 assertion은 유지한다. 수정 후 실제 독립 실행으로 확인해야 하며, 정책 부모 연결은 이번에도 미실행이다.

다섯 번째 [34828914963](https://github.com/FrostyCityMan/saneB/actions/runs/34828914963), SHA `97bf0369d296ebf49661f38edd7518ffd448607c`: 본문 중첩 메뉴 정제를 포함한 주 Gradle 검증은6분50초 성공했다. root2067통과/241조건부 생략, extractor25·독립 패키지16·Node152, PG job192·migration2·분할13·worker8, Linux 합성12파일(1시험), bootJar/설치 산출물은 통과다. 독립 namespace는09:44:11~09:47:36 UTC에215/215통과/실패·생략·미실행·container실패0, 정리 성공이다. 다만 정책 부모의 `AttachmentWorkerDbQaLinuxIntegrationTest`2건은0.646초에 `QA_CHILD_FAILED`로 실패했다. inventory 실행부터 자식 비정상 종료이며, 취소 시험도 예정 취소 전에 같은 오류다. 자식 시작 단계의 원인은 미확정이고 전체 workflow는 실패다.

다음 진단은 shell 독립 실행과 부모 `AttachmentWorkerDbQaProcess.selectCommand/selectProcessResult`의 실행 환경·경로·mount·자원 한도를 대조하고, 원문/secret을 출력하지 않는 시작 실패 정보를 확보하는 것이다. stderr 전체 노출·격리 해제·추측성 자원 한도 확장·반복 재실행으로 실패를 숨기지 않는다. 이번5회 Linux 진단은 여기서 종료하며 성공 범위와 연결 실패를 남긴다. 공식 Provider 전수/운영 정책/배포/브라우저 Gate는 통과하지 않았다.

### 2026-09-14 후속 재개 — 부모 시작 실패 진단

`AttachmentWorkerDbQaProcess`의 stderr를 64KiB 한도로 stdout과 동시에 소비하고, 비정상 종료에서만 고정된 시작 실패 코드로 분류한다. 원문/경로/외부 출력 값은 예외·로그·보고서에 복사하지 않는다. stdout 1MiB·namespace·환경 정리·기존 자원 제한·취소·소유 프로세스 정리와 성공 보고서 검증은 유지한다. 메모리/스레드/클래스 로딩/격리 권한/마운트의 구분은 진단 힌트이며 성공 근거가 아니다.

표적 `AttachmentWorkerDbQaProcessTest` 13건은 Windows에서 통과했다. 실제 부모 시작 실패 원인을 확정하거나 Linux 통과로 간주하지 않는다. 같은 QA 브랜치에서 전체 검증을 재실행하여 고정 코드와 결과를 확인한다.

여섯 번째 [34832431117](https://github.com/FrostyCityMan/saneB/actions/runs/34832431117), SHA `7267a30289563f0f77706ea615ef3c676f9054be`: 주 Gradle6분3초 통과. 독립 namespace는4분36초에214/215통과·실패1·생략/미실행0·정리 성공이지만, 다시1,001건 전수 분할 사례가 실패했다. 동일 SHA의 일반 실행에서 해당 사례는76.872초 통과했다. 독립 실패 원인은 현재 결과 hash만으로 확정할 수 없으며 추가로 시간 한도를 늘리지 않는다. 정책 부모는 앞선 단계 실패 때문에 미실행됐다. 이후 workflow는 주 산출물 생성이 성공하고 취소되지 않았다면 독립 실행 실패와 별개로 부모 연결을 검증한다. `continue-on-error` 없이 각각의 실패 종료와 전체 보고서 Gate를 유지한다.

일곱 번째 [34833700369](https://github.com/FrostyCityMan/saneB/actions/runs/34833700369), SHA `2c3734506ff8a49cdef434979bef801be303abd3`: 주 검증5분46초 통과(root2077통과/242조건부 생략·패키지16·Node152·job192·migration/분할15·worker8·Linux 합성12파일). 독립215/215는2분56초 통과·생략/실패/미실행0·정리 성공이다. 그러나 부모2건은0.478초에 `QA_CHILD_PROCESS_LIMIT`로 실패하여 전체 workflow는 실패다. 자식 시작 시 프로세스/스레드 생성 자원 부족 계열임을 구분했으나 같은 UID의 정확한 동시 스레드 수는 이 실행에서 수집하지 않았다.

후속은 자식 `--nproc=128`·메모리·시간·namespace를 유지하고, 검증 task JVM을 ActiveProcessorCount1/SerialGC/heap256MiB로, 해당 task를 실행하는 Gradle daemon을 ActiveProcessorCount1/SerialGC/heap512MiB로 한정한다. Linux `RLIMIT_NPROC`는 같은 real UID의 스레드에도 적용되므로 부모의 불필요한 병렬 실행을 줄이는 조치다([Linux 매뉴얼](https://man7.org/linux/man-pages/man2/getrlimit.2.html)). 보안 한도를 확장하거나 실패를 허용하지 않는다. 부모 시험에서 UID/PID/명령/환경을 제외한 관측 스레드 집계만 기록하며 이 값 자체는 통과 근거가 아니다. 로컬 구조/프로세스19건·패키지16건이 통과했다. 실제 Linux 재검증과 운영 앱의 상위 JVM 자원 여유는 별도이며, 이번 조치를 운영 검증 통과로 보지 않는다.

여덟 번째 [34834798383](https://github.com/FrostyCityMan/saneB/actions/runs/34834798383), SHA `d6d9bb0bfd431497adcbf1f83deda6fda7503a42`: 주 검증6분44초 통과(root2077통과/242조건부 생략·패키지16·job192·migration/분할15·worker8·Linux 합성12파일). 독립215/215는3분24초 통과·실패/생략/미실행0·정리 성공이다. 부모2건은0.485초에 같은 `QA_CHILD_PROCESS_LIMIT`로 실패했다. 관측은 `QA_PARENT_JVM_THREADS=9`, `QA_SAME_UID_THREADS_OBSERVED=158`이다. 부모 JVM 병렬성을 낮추어도 CI 공유 계정 전체가128 한도를 이미 초과하므로 그 조치만으로 해결되지 않았다. 서로 다른 UID의 작업을 같은 제한으로 셌다는 추측이 아니라 같은 real UID로 필터링한 집계다. 관측값과 실제 fork/스레드 생성 실패 및 Linux 자원 제한 의미가 일치한다.

[!] 부모 시작 실패는 실제 실행5·7·8에서3번 확인했다. 실행6은 부모 미실행이므로 실패 시도로 세지 않는다. 이번 재개에서3회의 원격 실행 후 유한 반복 원칙에 따라 진단을 종료한다. 다음 조치는 **임시 CI runner의 전용 비관리자 QA 계정 분리**다. 새 계정에서 명시적으로 고른 소스/환경만 사용하고 같은128개 제한을 유지하며, 결과 XML만 회수하고 해당 계정이 소유한 프로세스·임시 디렉터리·계정을 정리하는 경로를 구현·검증해야 한다. 아직 구현하지 않았다. runner 공용 프로세스 종료·root 실행·격리 해제·추측성 한도 증가는 대안으로 사용하지 않는다. 운영 서비스의 실제 계정/스레드 여유는 별도 Gate이며 이번 관측으로 정상이라고 판단하지 않는다. 독립1,001건 분할의 실행6 간헐 실패 원인도 후속 profiling 대상이다.

### 2026-09-15 — 임시 QA 전용 UID 분리 구현

`scripts/qa/run-policy-db-qa-isolated-user.sh`는 임시 GitHub Linux runner의 비root 호출 및 현재 HEAD/GITHUB_SHA 일치를 확인하고, 기존 계정/그룹이 없는 경우에만 새 비관리자 QA 계정을 생성한다. `git archive HEAD`로 커밋된 소스만 전달하며 `.git`, 미추적 파일, 기존 build/cache와 runner 자격증명은 복사하지 않는다. `env -i`와 명시한 Java/PATH/새 HOME/Gradle cache로 부모 연결 시험을 실행한다. 자식128개/2GiB 한도와 namespace 및 시험 assertion은 변경하지 않는다.

부모 단계 종료 후 지정 클래스의 일반 JUnit XML만 원래 작업 디렉터리로 회수한다. 생성한 UID만 종료하고, 검증한 `/tmp/saneb-policy-parent-qa.*` 경로와 생성한 계정/그룹을 정리한다. 시험 실패·보고서 부재·정리 실패는 모두 실패다. root QA 실행, 기존 runner 프로세스 종료, 운영 계정/DB 변경은 없다.

로컬: workflow 계약7건·자식 process13건 및 Node152건 통과, Bash 구문/비CI 실행 거부 통과. 이는 실제 Linux 계정 분리·정리의 성공 증거가 아니다. 같은 QA 브랜치 원격 실행에서 부모 정상215건 연결과 취소 시험, 필수 XML 판정, 정리 결과를 확인해야 한다. 현재 **Not ready**이며 이전 실행6의 간헐 분할 실패와 전체 Provider/운영 Gate는 별도 잔여다.

아홉 번째 [34860558789](https://github.com/FrostyCityMan/saneB/actions/runs/34860558789), SHA `8cb55d1aa6d2cba823f139f913269c20b6765428`: 주 검증 통과(root2078통과/242조건부 생략·추출기25·패키지16·job192·migration/분할15·runtime12합성파일/1시험·worker8·Node152·bootJar). 독립215/215·생략/실패/미실행0도 통과했다. 부모2건 중 취소1건은0.487초 통과, 정상 연결1건은1.809초에 inventory 처리 중 `QA_PROCESS_FAILED`다. 관측은 같은UID108·부모JVM9이며 새 전용 계정/임시 경로 정리는 `POLICY_DB_QA_CLEANUP=SUCCEEDED`다. 기존 공유 UID 기동 실패와 같은 오류라고 단정하지 않는다. 전체 workflow 및 보고서 Gate는 실패다.

후속은 동일 전용 계정에서 준비 빌드와 실제 시험을 서로 다른 single-use Gradle 수명으로 분리하고, wrapper JVM에도 CPU1/SerialGC를 명시한다. 로컬 wrapper `help --task attachmentPolicyDbQaIntegrationTest`로 확인한 task 전용 `--rerun`을 사용하여 시험2건은 재실행하되 준비 산출물은 재빌드하지 않는다. 전체900초·자식128/2GiB·namespace·XML 엄격 검증은 유지한다. 부모 실행기는 기동/입출력/응답 대기/JSON 해석 실패를 고정 코드로 구분한다. exit0이어도 경고가 섞인 JSON을 잘라내 성공시키지 않는다. 표적 workflow7·process16·역할22=45건과 bootJar가 통과했다. 실제 원인은 다음 Linux 응답으로 확인하며 원문/경로/자격증명을 예외에 넣지 않는다.

### 이전 구현 기록


09-12 후속: 독립 QA 산출물 `installAttachmentContractQa`에 실제 worker/격리 추출/DB 연결 8사례와 정부24 출처 정합성 1사례를 추가했다. 현재 고정 목록은 job168·migration2·backfill13·worker8=191건이며, Linux 실제 실행은 여전히 미확인이다. migration 검증은 V78까지 포함한다. HTTP는 고정 합성 입력이고 실제 사이트 수집이 아니다. 아래 과거159건은 당시 기록이다. 상세·명령·승인 경계는 [독립 worker·DB QA 산출물](announcement-attachment-contract-runtime-2026-09-12.md)을 따른다.

- [x] 수동 실행 전용 workflow와 필수 보고서 판정기 구현.
- [x] 보고서 판정기 자체 테스트 10건, workflow YAML 구조·승인 경계 테스트 4건 로컬 통과.
- [ ] 원격 workflow 실행, 정책 초안/개정7건·분류 이력3건·비동기 QA계약12건·배치 범위5건·수집 제어6건·preview/선택6건·적용8건·확인 복구 근거6건·원복 승인/worker8건·일반 작업 예약/적용 근거8건·일반 승인/원자적 복구12건·일반 작업 이력1건·배치 승인 이력3건·게시 영향 관측3건을 포함한 최신159개 job DB 테스트 및2개 migration 테스트 실제 통과 확인.
- [ ] 추가한 `attachmentRuntimeIntegrationTest`의 12개 고정 합성 파일 Linux 실제 격리 실행.
- [ ] `attachmentWorkerIntegrationTest`의 8개 worker/DB/추출 사례와 독립 산출물의 191개 전체 계약 실행. 목록 발견만으로 통과하지 않는다.
- [ ] 별도 Spring Flyway integration, 실제 공개 파일/worker/운영 API/브라우저 검증.

Windows Code Integrity의 embedded PostgreSQL library 거부와 Docker 엔진 불가를 우회하기 위해 OS 보안을 완화하지 않는다. 기존 GitHub 저장소의 임시 Ubuntu runner에서 검증 가능한 경로를 추가한 것이며, 이 문서 작성이나 로컬 단위 테스트로 Linux Gate를 통과 처리하지 않는다.

## 실행 경로

- workflow: `.github/workflows/attachment-contract-qa.yml`, 이름 `첨부 DB·작업 계약 Linux QA`.
- trigger: `workflow_dispatch`와 정확한 QA 브랜치 `codex/attachment-three-stage-linux-qa`의 push만 사용한다. 기본 브랜치 push 자동 실행, 배포, cron, AWS OIDC, 운영 secret 참조가 없다. 권한은 `contents: read`다.
- Ubuntu 22.04/Java 21, 최대 30분, 동시 실행 1개, Gradle worker 1개로 실행한다.
- 대상 task: `:test :attachment-extractor:test attachmentContractQaTest attachmentJobIntegrationTest attachmentMigrationTest attachmentRuntimeIntegrationTest attachmentWorkerIntegrationTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --console=plain --max-workers=1 --continue`. 이후 `run-attachment-contract-qa.sh`와 `attachmentPolicyDbQaIntegrationTest --rerun-tasks --no-daemon --console=plain --max-workers=1`을 순서대로 실행한다. 실패를 무시하지 않으며 현재 원격 결과는 위 다섯 번째 기록을 따른다.
- 해당 임시 runner에서만 `bubblewrap`을 설치하고 `/usr/bin/bwrap`, `/usr/bin/prlimit`을 확인한다. 보안 정책/sysctl/AppArmor 해제나 privileged container를 사용하지 않는다. 도구 설치·실제 namespace 실행 실패는 실패로 남긴다.
- `attachmentRuntimeIntegrationTest`는 설치된 distribution으로 12개 고정 합성 PDF/HWP/HWPX를 처리한다. 파일별 기대 품질·정확한 문자/좌표·원본 정리·전후 runtime 지문을 검사하며, 운영 URL/DB/임의 관리자 파일 입력을 사용하지 않는다. Windows에서 이 전용 task를 실행하면 격리 부재로 실패한다.
- `attachmentJobIntegrationTest`와 `attachmentMigrationTest`가 각자의 환경 조건을 true로 설정하고 loopback 전용 임시 PostgreSQL을 생성·종료한다. DB URL/사용자/비밀번호를 입력받거나 운영 환경에서 가져오지 않는다.
- 공개 첨부 QA·규칙 export·별도 Spring Flyway integration 환경 조건은 false다. 의존성 다운로드를 제외하고 외부 공고 Provider를 호출하지 않는 계약 검증이다.

커밋된 대상 SHA가 원격에 있고 현재 로그인 계정에 Actions 실행 권한이 있을 때 이 workflow를 선택해 수동 실행한다. 실행하지 않았다면 Actions 성공 링크를 만들거나 성공했다고 보고하지 않는다. 기존 `deploy.yml`은 변경하지 않았으며 별도 배포·승인 경계를 그대로 따른다.

## 생략 방지와 증거

- `scripts/qa/attachment-contract-report.mjs`는 다섯 전용 task(DB job, migration, 설치 runtime, worker, 정책 부모 연결)의 필수 6개 클래스 XML 헤더를 읽는다. 보고서 부재, 다른 클래스, 0건, 실패, 오류, 조건부 생략이 있으면 종료 코드 1이다. workflow가 Gradle 시작 전에 기록한 `ATTACHMENT_QA_STARTED_AT`보다 오래된 보고서 또는 시작 시각 누락도 거부한다. 과거 로컬 전용 task 결과를 최신 실행으로 재사용하지 않는다.
- 판정기는 JUnit 보고서 집계의 검증 장치다. 테스트 내용을 보증하거나 전체 ATT-001~062/실파일/운영 E2E 완료를 판정하지 않는다. 다른 root test의 환경 조건 생략도 별도로 보고해야 한다.
- `--rerun-tasks`와 새 runner checkout을 사용하여 이전 로컬 결과를 채택하지 않는다. XML은 해당 코드 SHA 이름의 Actions artifact로 7일 보관한다. 이 경로는 합성 테스트 데이터만 사용하며 운영 원문/자격증명/HAR를 artifact에 넣지 않는다.
- 실행 증거에는 대상 commit SHA, Actions run URL, 각 suite의 실행/통과/실패/오류/생략 건수와 시각을 기록한다. DB 계약 통과는 설치 artifact·운영 migration 성공과 다르다.

## 실패 시

- runner의 initdb/라이브러리/테스트/제약 위반은 실제 실패로 남긴다. skip·assertion 삭제·격리 해제로 성공시키지 않는다.
- workflow 시간 제한 또는 취소 시 runner 수명 종료로 임시 자원이 폐기된다. 정상 테스트 종료는 embedded PostgreSQL close와 single-use Gradle daemon 종료를 사용한다.
- 서버 test를 운영 DB에 직접 실행하거나 운영 서비스를 중지하지 않는다. 정책 게시·ENFORCE·기존 데이터 일괄 적용은 이 workflow와 무관하며 정확한 범위 승인 이후 별도로 수행한다.
