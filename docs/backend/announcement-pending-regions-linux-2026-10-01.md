# 잔여12수집원 Linux 첨부 발견·다운로드 비교

## 현재 단계 / Gate

- [x] 기존 고정 표본12개 및 현재 프로필·예산 확인
- [x] 두 묶음 순차 실행과 오류·성공 증거 보존 구현
- [x] 로컬 예산·workflow 계약 검증
- [~] QA 브랜치의 명시적 최초 push로 Linux 관측
- [ ] 보고서별 파일 서명·hash·현재 프로필·원본 정리 확인 및 대장 반영
- [ ] 전체 목표의 worker·DB/API/UI·운영 E2E 완료

기준 HEAD `fe285ebfdc12dd7ef97c18f316e1a572cf69ae2d`. 최신 다운로드 확인203/223, 잔여20개다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수·현재 운영 조회·전체 goal 완료율이 아니다. HWP 추출 고도화는 보류하고 수집 가능한 파일과 실패 원인을 각각 기록한다.

## 대상과 예산

| 묶음 | 기존 고정 표본 | 요청 상한 / 응답 예산 |
| --- | --- | --- |
| A | 은평50607·서대문313956·검단235·이천70639·동두천45339·속초32983 | 38요청 / 138MiB |
| B | 영동759FDCD3·아산76469·의성39093·성주586507·봉화32956·남해35694 | 36요청 / 138MiB |

총12공고, 최대74요청·276MiB. 각 표본은 기존6요청·23MiB(은평·서대문은7요청)를 그대로 유지한다. 현재 코드의 고정 ID·수집원 중복 없음·합계 예산을 테스트로 검증한다. 새 URL·다른 게시판·임의 공고를 탐색하지 않는다. 제목 단계 중단·본문 오류·발견 실패·파일 다운로드 오류·유효 파일을 구분하며 파일 수신이나 job 성공만으로 수집 성공을 선언하지 않는다.

아직 Linux 파일 비교를 수행하지 않은 대상만 선택했다. 최근 Linux에서 실패한 포천·강릉·충북·공주·평택·성남은 이번에 반복하지 않는다. 철원은 기존 Windows·서울 전송 실패를 유지하며, 울산 남구는 목록 성공 후 후속 조회 시간 초과 및 프로필 미연결로 별도 관리한다.

## 실행과 증거 보존

`[pending-regions-linux-01]` 최초 push만 실행한다. 기존6지역 표식·충북 표식과 상호 배타적이다. contracts job이 종료한 후 별도 regional job에서 6개씩 순차 실행하므로 동시 JVM은 없다. 재실행은 외부 요청을 하지 않는다.

A묶음에서 실패해도 B묶음은 실행하되 어느 쪽이라도 실패하면 최종 종료1을 반환한다. A의 JUnit XML이 B에 덮어쓰이지 않도록 생성 보고서를 `junit-a`·`junit-b`로 각각 보존한다. 결과 JSON은 `*-PENDING-LINUX-A-01.json`·`*-PENDING-LINUX-B-01.json`, 생산 클래스 지문과 함께 metadata artifact에 보관한다. 원문·바이너리 artifact는 올리지 않는다.

관측 job20분 제한과 기존 표본별 예산을 유지한다. 운영 DB·설정·worker·규칙·ENFORCE·재분류·배포·추출기 실행은 하지 않는다. 성공 파일이 일부 확보되더라도 본문 또는 다른 첨부 오류를 삭제하지 않는다. 전송 실패는 해당 단계 오류로 수집하고 다음 지역으로 진행한다.

## 로컬 검증

```powershell
.\gradlew.bat --no-daemon :test --tests '*RegionalTransportLinuxContractTest' --tests '*AttachmentContractWorkflowTest' --rerun
node --test scripts/qa/attachment-regional-linux-workflow.test.mjs scripts/qa/attachment-ulsan-namgu-survey.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

집중 Gradle22초 종료0(지역 예산2개·workflow19개), 관련 Node15개 통과. 실제 사이트 결과는 아직 미확인이다. 애플리케이션 Java·프로필·Flyway를 변경하지 않아 기존 inventory와 영수증 지문을 보존한다. bootJar는 이번에 재실행하지 않았고 브라우저는 사용자 정책상 미실행이다.
