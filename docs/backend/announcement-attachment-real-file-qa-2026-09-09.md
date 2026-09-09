# 공고 첨부 실파일 QA 진행 기록

- 시작 기준선: master / a84893b, clean.
- 범위: 공개 공고의 고정 표본을 로컬에서 다운로드 → 격리 추출 → 별도 키워드 판정한다.
- 비목표: 운영 worker 연결, 정책 게시·활성화, 운영 DB 저장, 기존 데이터 재분류, 커밋·푸시·배포.
- 실파일 QA는 전체 출처 지원이나 운영 적용 완료의 근거가 아니다.

## 계획과 Gate

- [x] AGENTS·첨부 설계·구현 및 재부팅 후 Docker 상태 확인.
- [!] Docker 지속 복구: 재부팅만으로 소켓 오류가 해소되지 않았다. 기존 소켓 폴더를 보존하고 새 실행 폴더로 기동한 임시 복구 상태다.
- [x] 고정된 공개 PDF/HWP/HWPX 표본 4개와 opt-in 검증 task 구성.
- [x] Docker 읽기 전용·비권한·네트워크/환경변수·메모리/PID/CPU 제한 확인.
- [x] 실제 bytes·signature·한국어 추출·블록 근거·판정 및 OCR 필요 상태 확인.
- [x] 기본 회귀·빌드, 원본/임시 컨테이너 제거, 비밀정보 보조 검사.

성공 기준: 실제 공개 파일의 해시·크기·추출 형식·상태·근거 건수를 재현하고, 제목 제외 및 부분/OCR 상태를 정상 후보로 위장하지 않는다. 실패 기준: host 비격리 파싱, 실제 텍스트·파일 원본의 Git/로그 유출, 운영 데이터를 읽거나 변경하여 QA로 사용하는 것, 표본 성공을 모든 채널 성공으로 보고하는 것.

## 검증 환경과 한계

`attachmentRealFileQa`는 명시적으로 호출했을 때만 외부 고정 표본을 요청한다. 기본 `test`에서는 skip이다. 공고 상세의 현재 제목과 직접 연결된 다운로드 링크를 확인하고 기존 제목 Gate를 먼저 적용한다. 규칙은 Flyway로 생성한 별도 loopback 임시 DB의 DRAFT seed를 읽기 전용 사용하며 활성화하지 않는다. 운영 ACTIVE 규칙과 같다고 가정하지 않는다.

각 판정은 선택한 파일 하나만 봉인된 입력으로 간주한 진단이다. `evaluationScope=SINGLE_SELECTED_FILE_DIAGNOSTIC`, `entireNoticeAttachmentSetVerified=false`로 기록한다. 해당 공고의 다른 첨부 전체를 발견·검증했다는 뜻이 아니며, 전체 집합 봉인·운영 판정을 대신할 수 없다. `ACCEPTED`도 단일 파일 진단 후보일 뿐 사용자 자격 확인·지원 확정·운영 활성화 결과가 아니다.

추출은 기존 Java 21 CLI를 고정된 로컬 Temurin 이미지의 일회성 Docker 컨테이너에서 실행한다. Docker 소켓·DB·전체 저장소는 컨테이너에 마운트하지 않는다. CLI library와 해당 입력 파일만 읽기 전용으로 제공한다. 운영 `bwrap` 경로의 실증을 대체하지 않으며 Windows host 파싱으로 우회하지 않는다.

파일은 테스트 전용 임시 디렉터리에만 보관하고 finally에서 제거한다. Docker 로그 저장을 끄고 JSON stdout은 메모리에서만 읽는다. 결과 보고서는 공개 공고 ID, hash, bytes, 품질·판정 코드·개수만 저장하며 제목·파일명·원문 전문·일치 구절은 저장하지 않는다.

## 공개 표본 출처

- [서대문구 소상공인 라이브커머스 지원사업](https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=PBLN_000000000124628): PDF 포스터와 HWPX 본문출력파일.
- [안양시 소상공인 이자차액 보전금 지원계획](https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=PBLN_000000000117918): HWP 본문출력파일.
- [소상공인 도약 지원사업](https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=PBLN_000000000120120): PDF 본문출력파일.

재배포 허가가 확인된 fixture로 간주하지 않는다. 원본 binary·추출 전문은 저장소에 추가하지 않는다. 본문출력파일의 NOTICE 역할은 이 QA 표본의 수동 확인값이며 자동 profile 게시를 뜻하지 않는다. 일반 첨부 영역의 PDF는 UNKNOWN으로 둔다.

## 실파일 결과

2026-09-09 로컬 실파일 QA에서 아래 결과를 확인했다. 추출 상태는 reader의 품질 코드이며 사람이 원문 전체와 대조한 정확도 수치가 아니다. 일치 건수는 키워드 근거 개수이지 지원 가능성 점수가 아니다.

| 표본 | bytes | 추출 상태 | 문자 / 블록 | 일치 근거 | 단일 파일 진단 |
|---|---:|---|---:|---:|---|
| PDF-SDM-2026 | 330,877 | OCR_REQUIRED | 0 / 0 | 0 | REVIEW_REQUIRED / ATTACHMENT_INCOMPLETE |
| PDF-SEMAS-2026 | 1,225,522 | PARTIAL_TEXT | 31,498 / 35 | 1,013 | REVIEW_REQUIRED / ATTACHMENT_INCOMPLETE |
| HWP-ANYANG-2026 | 57,856 | COMPLETE_TEXT | 565 / 17 | 34 | ACCEPTED / EXTENDED_TARGET_SUPPORT_CONFIRMED |
| HWPX-SDM-2026 | 73,535 | COMPLETE_TEXT | 2,413 / 89 | 92 | REVIEW_REQUIRED / ATTACHMENT_CONTEXT_REVIEW |

- 4개 모두 상세 페이지의 현재 제목이 COMBINATION_MATCHED였다. 제목·본문 기존 결과는 BODY_UNAVAILABLE로 유지했다.
- HWP/HWPX/텍스트형 PDF에 실제 `소상공인` 텍스트가 포함됨을 boolean assertion으로 확인했다. 원문 구절은 로그에 기록하지 않았다.
- 이미지형 PDF는 1페이지·추출 텍스트 0으로 OCR 필요를 확인했다. OCR 엔진은 실행하지 않았으며 이미지 내용을 키워드 분석했다고 표현하지 않는다.
- 텍스트형 PDF는 35페이지다. 부분 추출 및 page scope 불확실성으로 검수 처리했다. 페이지 내 표·다단의 서로 다른 조건을 자동 후보의 AND 근거로 쓰지 않는다.
- HWPX는 부정 문맥 경고가 있어 검수 처리됐다. 검수 상태의 다중 태그도 최종 지원대상으로 확정된 값이 아니다.
- 규칙: 별도 임시 DB의 DRAFT `ASCR-000001`, 394 rules. 이 실행의 rules SHA-256은 `0514e8b4fcec106fd708c99d7615f7c8fad852a8bdf954368abe3c4143c7350c`다.
- report는 Git 제외된 `build/reports/attachment-real-file-qa/*.json`에만 생성한다. binary/text SHA-256과 검사 시각·상태를 포함한다.

## 실행·실패 기록

```powershell
.\gradlew.bat attachmentRealFileQa --no-daemon '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
```

첫 실행은 격리 사전 점검의 QA_CONTAINER_FAILED로 실패했다. 그 시점에는 실제 파일을 요청하지 않았다. 고정 점검문을 중첩된 Windows 인자가 아닌 stdin으로 전달하도록 수정한 뒤 3개 표본 실행(41초), 텍스트형 PDF를 추가한 4개 표본 실행(45초)이 각각 BUILD SUCCESSFUL이었다. TLS 인증 검증은 끄지 않았다.

최종 검증 명령:

```powershell
.\gradlew.bat attachmentRealFileQa test :attachment-extractor:test attachmentMigrationTest bootJar :attachment-extractor:installDist --no-daemon '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
.\gradlew.bat :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
git -c safe.directory=C:/PersonalProject/saneB diff --check
git -c safe.directory=C:/PersonalProject/saneB status --short --branch
git -c safe.directory=C:/PersonalProject/saneB rev-parse HEAD
```

- 첫 종합 명령: BUILD SUCCESSFUL, 2분 53초. 기본 test 583건 중 562건 실행 통과·21건 skip(조건부 통합 및 opt-in 실파일 QA), 실패/오류 0. 실파일 task 4건과 attachmentMigrationTest 2건은 별도 실행 통과했다.
- 추출 CLI test/bootJar/installDist가 UP-TO-DATE였으므로 두 번째 명령으로 실제 재실행했다. BUILD SUCCESSFUL, 35초, 10개 task 모두 실행. 합성 CLI fixture 12건 실패/오류/skip 0.
- 최종 실파일 실행 시각은 2026-09-09 19:27 KST다. root filesystem ro, UID 65534, CapEff 0, NoNewPrivs 1, 외부 network interface 없음, host canary 환경변수 미전달, memory 512 MiB·swap 0·PID 64·CPU 1을 probe에서 확인했다.
- 기존 migration 69개는 HEAD와 CRLF/LF 정규화 후 내용이 동일하다. Windows checkout의 기존 CRLF 때문에 raw byte 비교는 차이가 났으며 이를 schema 변경으로 취급하지 않았다. migration 파일을 수정하지 않았다.
- git diff --check 통과. 변경 파일 6개에서 고위험 secret 패턴 보조 검사 검출 0. 비밀정보 부재의 절대 보증으로 표현하지 않는다.
- HEAD는 계속 a84893b9360e868d0a21d6010cc2a7ff16ee4ba7이다. build.gradle·QA 테스트 3개·문서 2개의 로컬 변경만 있으며 commit/push/deploy는 하지 않았다.

## 자원 정리

- 4개 실파일 report 모두 originalFilesRemoved=true. 각 실행의 임시 binary·상세 HTML을 finally에서 제거했으며 원본을 저장소에 추가하지 않았다.
- 종료 직전 `docker ps -a --filter name=saneb-attachment-qa-` 결과 0건, `saneb-attachment-real-qa-*` 임시 디렉터리 0건을 확인했다.
- `docker desktop stop --timeout 20`이 이번에는 성공 응답을 반환했다. Docker Desktop/backend/build 프로세스가 남지 않았고 docker-desktop WSL 배포판이 Stopped임을 별도 확인했다. 재기동은 반복하지 않았다.
- 기존 Docker 이미지·컨테이너·볼륨·VHDX를 삭제하거나 초기화하지 않았다. 원본 소켓 폴더는 보존했으며 지속 복구 Gate는 계속 미통과다.
- 임시 PostgreSQL은 try-with-resources로 종료했다. Gradle은 --no-daemon, Node 검사는 일회성 실행을 사용했다. 다른 프로젝트 프로세스는 종료하지 않았다.

## 남은 Gate

- [!] Docker는 빈 소켓 실행 경로를 다시 준비했을 때만 기동했다. 재부팅으로 지속 복구되지 않았으며 다음 재기동 성공을 보장하지 않는다. 소켓 원본은 별도 보존했다.
- [ ] 운영 Linux의 bwrap/prlimit 격리, OOM·timeout 시 process-tree 정리 실증. Docker QA는 이 경로의 검증이 아니다.
- [ ] 출처별 전체 첨부 발견·역할 profile QA. 현재는 기업마당의 고정된 4개 파일뿐이다.
- [ ] 다양한 기관의 실제 암호·손상·표·내장 개체 호환성. 해당 합성 fixture 회귀와 실제 기관 표본 근거를 구분한다.
- [ ] worker/lease/전역 예산/논리 다운로드 deadline, DAO·Mapper 저장, API·검수 화면, 기존 확정·재분류 우회 방지 연결.
- [ ] 운영 COLLECT_ONLY/ENFORCE, 기존 데이터 배치와 배포. 이번 QA에서는 실행하지 않는다.

최종 운영 판정은 계속 `Not ready`다. 브라우저 QA는 사용자가 현재 요청에서 지시하지 않아 정책상 미실행이다.
