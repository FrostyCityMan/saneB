# 태백 공식 공고의 제목·본문·전체 첨부 관측

## 단계와 목표

전체 Gate0~8/ATT001~062는 Not ready다. 이 증분은 실제 파일 기대값이 없는 상태를 해소하기 위한 **관측**이며 정상 후보·정책 QA·운영 성공 또는 전체 기관 지원을 선언하지 않는다. 운영 DB/정책/ENFORCE/기존 데이터 변경은 없다.

현재 로컬 Docker Linux 엔진 pipe에 연결할 수 없음을 확인했다. Linux 격리 요구를 Windows 비격리 추출로 우회하지 않는다. 기존 기업마당 관측의 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT3건도 해결된 것으로 표시하거나 같은 조건에서 반복하지 않는다. 다른 공식 host의 고정 표본을 기존 Linux QA workflow에서 별도로 관측한다.

## 고정 범위와 요청 예산

- 대상: [태백시 공식 공고184816](https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=184816), 청년농업인 육성지원 공고1건.2026-09-15 공식 페이지의 제목·본문·첨부2개를 재확인했다. 웹 조회는 Linux 파일 추출 성공 근거가 아니다.
- 프로필: 기존 `LOCAL_TAEBAEK_BBS_V1`, `LGS-000121/SPRING_BBS` 및 고정 source identity를 사용한다. 외부 URL·parser·성공값을 입력하는 기능은 없다. 추가 수집원 지원 코드를 이번 작업에 포함하지 않는다.
- 전체 상한:420초/44요청/80MiB. 파일별20MiB·최대10개·기존 pinned transport/SSRF/TLS/같은 host 검증·Linux bwrap/prlimit를 유지한다.
- BODY는 기존 production client의 최대2시도·redirect0·응답1MiB를 사용한다. 전체 예산에서2요청/2MiB를 먼저 확보하므로 나머지 상세/파일은42요청/78MiB 이내다. 보고서의 예약량은 BODY 상한을 포함한 보수적 값이며 실제 전송 합계로 표시하지 않는다. 정상 예상 경로는 BODY1+첨부 상세1+파일2=4 HTTP지만 BODY 예약량을 포함한 요청 예약은5다.
- 전체 첨부가2개에서 바뀌면 모든 발견 파일을 분모에 남겨 관측하되 마지막 고정 목록 대조는 실패한다. 일부 성공 파일만 선별하여 성공시키지 않는다.

## 변경된 프로세스 유지

1. 현재 migration의 격리 DRAFT 규칙을 읽어 고정 공식 제목을 판정한다. 제목 제외/조합 미충족이면 BODY/상세/파일 요청 전에 중단한다. 규칙을 수정·게시하지 않는다.
2. production BODY client로 공식 내용 영역만 확보·정제하고 같은 규칙으로2차 분류한다. A/B/정보 부족은 첨부를 건너뛰는 조건이 아니다.
3. production 첨부 profile로 상세의 고정 제목을 재확인하고 공식 첨부 영역의 전체 목록을 발견한다. 메뉴·미리보기·임의 링크를 파일로 수집하지 않는다.
4. 각 파일의 다운로드·signature·Linux 격리 추출·텍스트/위치 기반 역할 판정을 수행한다. 파일명으로 NOTICE/GUIDE를 지정하지 않는다. 미지원·UNKNOWN·부분·실패 파일도 분모와 종합 입력에 남긴다.
5. BODY 판정과 모든 파일의 실제 역할·텍스트·block을 기존 첨부 종합 분류기에 전달한다. 본문/파일 B를 제목 자동 삭제로 변경하지 않는다. 결과는 관리자 최종 검증이 필요한 관측이며 DB 저장·검수 결정·DRAFT 생성·활성화를 실행하지 않는다.
6. 상세/파일 원본은 전용 임시 디렉터리에서만 사용하고 finally에서 정리한다. 보고서에는 상태·개수·지문·고정 코드/위치 metadata만 남긴다. 본문/파일명/추출문/locator 원문·인증 정보는 artifact에 넣지 않는다.

## 구현과 검증

- `AnnouncementAttachmentBbsOfficialObservationTest`: 고정1공고의 실제 순차 관측과 metadata 보고서. `SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION=true`에서만 동작한다.
- `attachmentBbsOfficialFileObservation`: 기존 Linux 격리 추출 설치물을 사용하는 전용 Gradle task. 일반 root test에서는 실사이트 호출을 생략한다.
- 기존 Linux QA workflow의 별도 기본false 입력 또는 QA 브랜치의 `[official-bbs-observation]` 명시 표식으로만 실행한다. 기업마당 관측과 별도이며 일반 push에서 외부 파일을 요청하지 않는다. 결과 JSON/XML만7일 artifact로 남긴다.
- [x] 표적14건(신규5+기존 관측9) 통과·44초 성공. 고정 제목의 현재 DRAFT 규칙 통과, 제목B 차단, BODY B 검수 후 첨부 진행, source 제목 identity, 전체 예산, 실제 역할/위치 입력을 확인했다. 임시 PostgreSQL seed는 운영 DB가 아니다.
- [x] 전체 회귀3분16초 성공: root2504=2251통과/253조건부 생략/실패0, QA 패키지20/20. 새 실제 관측1건은 일반 로컬 시험에서 생략했다. extractor/bootJar/설치 task는 UP-TO-DATE이며 새 실행으로 세지 않는다. 현재 production JAR SHA256은 이전과 같은 `fd57b761ceb2dcd7cab381ae3e3db9cb2fb46aced6c927f59ed3a01b0b2402e1`이다. 이번 변경은 test/검증 workflow·문서이며 production Java/DB/API/UI/프로필/migration 변경은 없다.
- [ ] 같은 SHA의 실제 Linux BODY/전체 첨부 관측. 성공/실패·정리·원본 비노출을 확인한 후에만 기대값 검토로 이동한다.
- [ ] 검토된 실제 파일·내용·역할 기대값 작성과 독립 재실행. 관측 보고서를 catalog에 자동 복사하거나 기대값 승인으로 취급하지 않는다.

catalog 참조15/실행 기대값0은 이 구현으로 바뀌지 않았다. PDF/HWP/HWPX 전체 형식, 모든 대상의 정상3공고/다중첨부, 운영 worker/DB/API/UI·전체 배치·배포·운영 브라우저는 장기 goal의 필수 잔여다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --no-daemon --max-workers=1
```

실제 Linux 명령은 `bash ./gradlew attachmentBbsOfficialFileObservation --no-daemon --console=plain --max-workers=1`이다. 로컬 Windows에서 실행하지 않았으며 미실행을 통과로 계산하지 않는다.
