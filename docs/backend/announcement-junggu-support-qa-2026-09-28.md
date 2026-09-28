# 대구 중구 지원사업 고정 표본 QA

## 목표·범위·판정 경계

기존 LOCAL_DAEGU_JUNGGU_GET_V1/LGS-000045에서 지원사업 참조가 없는 공백을 채운다. 사용자 모든 격리 QA 승인 범위의 고정 공개 표본 검증이며 운영 DB/설치/정책/worker/기존 데이터는 변경하지 않는다. 보은 추가2회 승인 대기와 별개 기관·표본이다. 정상 기대값을 추측하거나 과거 중구34295의 PDF signature 성공을 새3건의 성공으로 재사용하지 않는다.

전체9Gate=8부분/1차단. 목표는 제목→본문→전체 첨부→구간/분류→최종 관리자 검증이며 아래 발견·signature 증거만으로 전체 완료를 선언하지 않는다.

## 공식 표본과 관측 근거

V41의 공식 목록과 실제 제목 검색 필드 not_ancmt_sj를 확인한 뒤 지원 검색 결과에서 선택했다. 검색 도구의 공식 목록 접근 실패1회와 로컬 직접 HTTP 성공은 별개로 기록한다. 로컬 목록2회(16,219/15,366byte), 상세3회(6,797/5,536/6,097byte) 모두 HTTP200이다. TLS·고정 origin·공개 DNS 확인, 무리다이렉트·12초·응답1MiB 상한으로 읽고 메모리에서만 조사했다. 아직 파일 다운로드/격리 추출/DB 저장 증거가 아니다.

| 공고 ID | 공식 제목 | HTML 첨부 호출 수 |
|---|---|---:|
| 34196 | 2026 다국어 QR메뉴판 지원사업 참여 사업체 모집 | 1 |
| 33626 | 「대구 중구 청년 부동산중개보수 및 이사비 지원사업」모집 공고 | 2 |
| 33315 | 2026년 음식점 위생등급제 컨설팅 지원 업소 모집 공고 | 1 |

세 상세의 table.boardView/제목 th 인접 td와 고정 제목 일치를 확인했다. 제목에 지원사업 표현이 있다는 사실은 운영 규칙의 제목 통과·최종 적합 판정을 의미하지 않는다. 첨부 호출 수는 production profile로 전체 파일 발견을 재검증해야 한다. 원문·파일명·다운로드 인자를 로그/문서에 보관하지 않는다.

## 실행 체크리스트와 한도

1. [x] 공식 목록/상세 고정 ID·제목·구조를 확인한다. 관측용 Node는 종료했으며 원본 파일 저장0.
2. [x] 전용 jungguSupportDiscoveryQa와 오프라인 제목/고정 ID 계약을 검증했다.
3. [x] 고정3건의 production pinned transport·공식 첨부 전체 발견·다운로드 signature 및 정리를 확인했다.
4. [x] 성공한 고정 identity만 catalog reference로 추가했다. expectation은 null을 유지한다.
5. [ ] 본문/전체 PDF·HWP·HWPX Linux 추출·구간 분류·worker DB/API·검토한 기대값은 별도 후속 검증이다.

전용 task는 SANEB_ATTACHMENT_PROFILE_QA와 SANEB_JUNGGU_SUPPORT_QA를 모두 명시한다. 일반 test와 기존 지역 task에서 새 외부 요청을 실행하지 않는다. 세 사례는 각각120초·8요청·24MiB, 합계 최대24요청·72MiB이며 단일 JVM으로 순차 실행한다. 다운로드 파일당20MiB, 상세1MiB, 원본은 finally에서 삭제한다. 이미 수행한 로컬5회와 접근 실패1회를 보수적으로 포함하면 이번 단계 전체 상한30요청·78MiB다. 실제 요청/예약 byte는 실행 영수증으로 차감하며 동일 실행을 반복하지 않는다. 이 단계 상한은 이후 재실행의 무제한 승인이 아니다.

성공 기준: 고정 제목·ID·기관·profile 결합, 발견완전/전체4파일, 지원형식 signature, 실패/추가파일/구조변경을 숨기지 않음, 임시 원본 제거. 실패 기준: 파일명으로 역할 확정, 일부 파일만 성공 처리, 제목 불일치 후 다운로드, 예산 초기화, 새 기대값 자동 승인, 운영 쓰기.

```powershell
.\gradlew.bat :test --tests '*JungguSupportReferenceContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --no-daemon --max-workers=1
.\gradlew.bat jungguSupportDiscoveryQa --no-daemon --max-workers=1
```

현재 브라우저는 명시 요청 정책상 미실행이다. source/migration/API/UI/운영 규칙은 이 QA 준비에서 변경하지 않았다.

## 실제 실행 결과

- 표적 테스트26건(새 제목/고정범위9·기존 새올17)은29초에 실패/오류/생략0으로 통과했다.
- 전용 실사이트 task는23초 성공, 실제3건/4파일이다. 공식 상세 제목 일치, FOUND/전체 발견완전, 경고0, documentRole UNKNOWN 유지, temporaryCleaned=true를 확인했다. 추출기는 실행하지 않았다.

| 공고 | 실제 첨부 형식·byte | 실제 요청 | 예약 byte |
|---|---|---:|---:|
| 34196 | HWPX 159,994 | 2 | 166,791 |
| 33626 | HWP 127,488 + PDF 209,769 | 3 | 342,793 |
| 33315 | HWP 100,864 | 2 | 106,961 |

- signature 실행7요청/616,545예약byte다. 앞선 목록·상세5회와 도구 열기 실패1회에6MiB를 배정하여 이번 지원사업 캠페인 원장에 **13회/6,908,001byte**를 기록한다. 이는 예약 집계이지 도구 내부 HTTP 횟수/트래픽의 검증된 상한이 아니다. 실제 직접 요청은 Node5회+Java7회, Node 응답50,015byte이며 검색 도구 내부 트래픽은 미확인으로 분리한다. 기존2026-09-12 구조 표본34295의 별도 이력은 유지한다.
- 영수증 `build/reports/junggu-support-discovery-qa/LOCAL_DAEGU_JUNGGU_GET_V1-{34196,33626,33315}.json`, JUnit `build/test-results/jungguSupportDiscoveryQa`를 보존했다. 원본을 영구 저장하지 않았다.
- profileHash `e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef`. 파일 binaryHash는 각각34196=`4a544f3c98451eb7c002c626157c2b92468962c14756f7b38e3e218954b1b554`,33626 HWP=`ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6`/PDF=`6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c`,33315=`0d192cd2ad69fde6271fb2f8fc6018a98aada490b35ab1a4c66901b8f7e3bc77`이다.
- catalogVersion `2026-09-28-junggu-support-references-v2`, 전체40참조/14지역/기대값1/정상0이다. 기존 태백 기대값은 내용·시각·지문 모두 보존했다. 참조 추가는 정책 QA 통과·운영 게시가 아니다.
- 최종 표적+bootJar39초 성공: 제목9·새올17·catalog62·workflow18=106건 실패/오류/생략0. canonical source identity를 production normalizer와 대조하고 새3건 모두 REFERENCE_ONLY/inputHash null/normal false임을 검사했다. 기존 VM class-sharing 경고가 있다. Linux 동일SHA·본문·격리 추출·worker/DB/API 검증은 별도로 남는다.

이 변경은 QA task/시험·catalog 참조·문서다. 운영 parser/추출기/분류 규칙/DB/migration/API/UI는 변경하지 않았다. 새3건이 현재 제목 필터를 통과하는지와 원문의 지원 자격·문서 역할은 후속 실제 세 단계 검증으로 판정한다.

후속 확대 검증은 `:test`에 위4종과 `*AttachmentProviderQaCaseExecutorTest`를 포함하고 `:bootJar`를 함께 실행했다.37초 성공,179건(실행기73 포함) 실패/오류/생략0이다. source가 같은 bootJar는 UP-TO-DATE이며 실사이트는 다시 실행하지 않았다. 기존37개 reference/기대값의 JSON 내용이 모두 불변이고 새3개 null-expectation만 추가됐음을 Node로 대조했다. 사용한 단발 Node·Gradle 시험 프로세스는 종료하고 다른 사용자 프로세스와 output/·scripts/qa/__pycache__/는 보존한다.

## 후속 CI 실패와 오프라인 복구

5067d26의 Linux36382856909는 패키지 검증 단계에서 실패했다. 확인 시점 전체 job과 DB/migration 검사는 아직 실행 중이다. Node 검사에서 함안 task부터 attachmentQaRuleSnapshot 직전까지를 잘라 중간에 추가한 중구 task까지 포함한 것이 원인이다. 실제 Gradle task 실행 범위가 합쳐진 것은 아니다.

다음 최상위 tasks.register 선언까지만 읽도록 시험 helper를 수정하고 중구/함안 분리와 CRLF·마지막/누락 task 회귀를 추가했다. 아래 명령은 로컬에서35건/32통과/실패0/Windows에서 Linux 심볼릭 링크3건 생략,15.8초다. 표적8건은 생략 없이 통과했다. 동일SHA Linux 성공은 아직 별도 확인 대상이다.

```powershell
node --test scripts/qa/attachment-contract-release.test.mjs scripts/qa/attachment-official-worker-probe.test.mjs scripts/qa/attachment-bbs-observation-probe.test.mjs
```

새 공고 요청·추출·DB·운영 변경은 없다. 중구 제목 단계 통과 여부를 먼저 실제 seed로 판정해야 하며, 탈락 표본은 본문/첨부0요청으로 유지한다. 발견용4첨부 성공을 세 단계 정상 후보 성공으로 변경하지 않는다. 이 회차의 Node 시험은 정상 종료했고 관련 PID30432/35684 부재를 확인했다.
