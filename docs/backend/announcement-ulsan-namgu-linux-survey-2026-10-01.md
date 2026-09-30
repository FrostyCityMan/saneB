# 울산 남구 공식 목록 Linux 진단

## 현재 단계 / Gate

- [x] 기존 조사와 V61 공식 endpoint 확인
- [x] 고정 경로 전용 진단기·단위 테스트·명시적 CI 실행 조건 구현
- [~] 로컬 검증 후 QA 브랜치 최초 push에서 Linux 공개 조회
- [ ] 실제 목록 구조와 고정 상세 공고 근거 확보
- [!] 울산 남구 첨부 프로필 미연결 유지

전 지역 첨부 발견·다운로드 우선 방침에 따라 진행한다. 기준 HEAD `73f38fcfef386ae463a27657f64f9c60c2539e0e`의 다운로드 근거는203/223이고 잔여20개다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수나 전체 goal 완료율이 아니다. HWP 추출 고도화는 보류한다.

## 문제와 실행 범위

Windows 기존 조사에서 공식 iframe 조회 폼은 한 차례 확보했으나 목록 POST와 후속 초기 GET이 SocketException으로 실패했다. 같은 환경에서 반복하지 않고 GitHub Linux runner에서 공개 목록의 전송·문자셋·숫자 상세 ID만 확인한다. 다른 소식 게시판으로 대체하거나 운영 endpoint를 변경하지 않는다.

1. 공식 고시공고 메뉴 GET1회.
2. 기존 조사로 확인한 공식 새올 종료공고 iframe GET1회.
3. 두 번째 응답에서 조회 폼·목록 method·endpoint 구조가 확인될 때만 V61 기반 공개 목록 POST1회. 첫10행·page1·종료공고(Y), 검색어 없음.

총 최대3요청·응답당2MiB·누적6MiB·요청당15초다. 자동 재시도·redirect 추적·인증 우회·TLS 검증 해제는 없다. 공개 JSESSIONID는 같은 새올 호스트의 POST에 메모리로만 전달하고 로그·보고서에 쓰지 않는다. HTML과 파일 원본도 저장하지 않는다. 문자셋이 불명확하거나 공개 폼을 확인하지 못하면 POST를 생략하고 오류를 기록한다.

`attachment-ulsan-namgu-survey.mjs`의 보고서는 HTTP 상태·응답 크기/hash·문자셋·구조 여부·알려진 상세 호출의 숫자 ID만 포함한다. 상세 ID 추출은 목록 진단이며 제목 분류·첨부 발견 성공 판정이 아니다. 임의 상세 URL이나 첨부를 후속 요청하지 않는다. 수집 성공 대장에 추가하지 않는다.

## CI 및 검증

`[ulsan-namgu-list-survey-01]`이 있는 QA 브랜치 최초 push에서만 실행한다. 재실행에는 외부 요청을 하지 않는다. 기존 전체 계약 job과 그 실패는 보존하고 진단 metadata만 별도 artifact로 올린다. 운영 배포·DB·worker·정책·규칙·자동 활성화는 변경하지 않는다.

```powershell
node --test scripts/qa/attachment-ulsan-namgu-survey.test.mjs scripts/qa/attachment-regional-linux-workflow.test.mjs
.\gradlew.bat --no-daemon :test --tests '*AttachmentContractWorkflowTest'
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

브라우저는 현재 사용자 지시가 없어 정책상 미실행이다. 실제 Linux 결과 확인 전 목록 연결·첨부 수집이 가능하다고 보고하지 않는다.

로컬 Node 관련57개 통과, workflow 집중 Gradle17초 종료0, 대장674영수증/289표본 재현 통과, diff 공백 검사 통과다. 애플리케이션 Java·Flyway·프로필은 이번에 수정하지 않았고 bootJar는 재실행하지 않았다. 충북 실파일 재검증은 연결 단계 실패로 별도 문서에 기록했다.
