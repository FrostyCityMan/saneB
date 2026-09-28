# 공고 첨부 전달 단계 진단

## 목적과 범위

### 실제 결과: 강북4번째 파일 중계 GET에서 HTTP400

- [x] 소스61a9543a59b560e3d0f56518000566746eba744f, 실행51fcfdc4112940db8c6f03a47216adba, SSM0750caec-9a50-4541-a816-9824e29a4523의 terminal Failed/exit1을 확인했다. 실제8.568초·JUnit1실패/0생략이다.
- [x] 공식 상세와 전체4첨부 지문/형식을 다시 확인했다. 1~3번째는 NOT_SELECTED, 4번째는 FILE_DOWNLOAD/ATTACHMENT_HTTP_400이다. downloadTrace는 BRIDGE_GET/TRANSPORT/호출1/정상반환0으로, 기간 조회와 최종파일 POST에 도달하지 않은 사실을 확인했다.
- [!] binary/signature/격리 추출은 실행되지 않았다. 기존 전체 관측과 이번 선택 파일 진단 모두 같은 파일에서 HTTP400을 받았지만 서버 거부 원인(원본 링크/인코딩/서버 정책 등)은 아직 미확인이다. 기간 만료나 추출기 오류로 단정하지 않는다. 같은 실행을 자동 재시도하지 않는다.
- [x] 이번2요청/278,528예약byte, 누적16/21요청·3,236,178/34,603,008byte다. 새 진단 시작 전 남은5요청을 사용한 별도 범위·예약 검토가 필요하며 전체20요청 관측은 불가하다. 선택 진단 예약 파일은 재사용하지 않는다.
- [x] 서버 원본 삭제/probe cleanup/unit inactive/서버 전송 파일 제거·운영JAR불변/healthUP·운영DB미사용/쓰기0을 영수증으로 확인했다. 소유 S3 객체 제거/부재·plan.cleaned=true, 경로/지문 대조한 로컬ZIP 제거까지 완료했다. plan/영수증/예약은 보존한다.
- 영수증: `build/temporary-bbs-qa-51fcfdc4112940db8c6f03a47216adba/result-utf8.json`, SHA256 `3e9216e2065c9beb93af46a3daf64738895a0bc16ec36f42fd3542a313d93607`.
- 관측 범위는 전달 실패 위치 확정까지다. HWP 부분 추출·전체4첨부 검증·정상 기대값·명시구간worker/DB/API·운영E2E는 미완료이며 catalog40/정상0과 전체9Gate=8부분/1차단을 유지한다.
- [x] 후속 로컬 :test 전체3,108개=2,811통과/297조건부 생략·실패/오류0,6분52초. owned Gradle3프로세스 종료 확인, 다른 사용자 Java/Node는 건드리지 않았다. 61a9543 Linux36393605515는 최종 확인 시pending이므로 성공으로 표시하지 않는다.

### 선행: 강북4번째 파일 한정 진단 준비

- [x] GANGBUK_SELECTED_DOWNLOAD 전용 모드·별도 환경 opt-in·고정179490/4번째 locator를 연결했다. 상세 식별/제목 및 전체4첨부 순서·형식을 대조한 후 선택1개만 전달·signature·Linux 격리 추출한다. 1~3번째는 NOT_SELECTED이고 전체 분석/정책/기대값 승인false다.
- [x] 신규 최대5요청/24MiB, 기존14요청/2,957,650byte에 예약하면19요청/28,123,474byte다. 누적 상한21요청/34,603,008byte는 늘리지 않는다. 본문 요청0·CI 자동 외부 실행0이다.
- [x] 선행 영수증SHA256 bbe9144f…와 사전 확인영수증SHA256 fef3c6fe…·단일 실행 정리를 대조하는 CREATE_NEW 예약을 준비했다. 계획51fcfdc4112940db8c6f03a47216adba의 CheckOnly는 통과했으며 실행 여부는 실제 영수증으로 따로 판정한다.
- [x] Java57통과/외부조건부1생략·패키지20/Node10/Python35통과. 첫 Java 컴파일의 checked exception lambda 오류를 명시 loop로 수정해 재검증했다. probe JAR의 진단 클래스/trace4클래스를 실제 확인했다. bootJar는 UP-TO-DATE다.
- [x] AWS root/저장소 계정 일치·서울 단일 Ubuntu·SSM online을 재확인했다. 운영 최근 배포 d-NCB2HF3YK/9a1bb456이며 이번 작업은 운영 배포가 아니다.
- [x] 실제 선택 파일 결과, 누적 사용량, 원본/unit/전송객체 정리를 위 실패 영수증으로 기록했다. 단일 파일 진단을 전체4첨부 동시 검증이나 정상 후보·worker DB/API 성공으로 확대하지 않는다.

실행 패키지:135파일/88,420,561byte, archiveSha256 `d7915cc3e74aa150b759b9087ad9c6f5b8491f3bc957a047cec0a131736de569`. source codeHash는 `a15049147cd15f81eb45d07615e5b20c87c3785659b850d41641f39c19c8d549`로 운영 코드 변경이 없다.

강북179490의 기존 서울 실행63e2cb23은 전체4첨부 중4번째 다운로드가 HTTP400으로 실패했다. 기존 영수증에는 FILE_DOWNLOAD만 있어 중계GET, 기간조회POST, 최종파일POST 중 어디서 실패했는지 확정할 수 없다. 요청 수로 추정하지 않고 다음 관측에서 실제 호출 위치를 기록한다.

- [x] 관측 helper가 실제 worker의 AttachmentProfileDownloadFlow를 사용하는 경로를 유지한다.
- [x] 파일별 downloadTrace에 고정 enum과 숫자만 추가한다. 운영 DB/API/정책/추출기/프로필 지문은 변경하지 않는다.
- [x] 성공·실패 모두 기록하고 다음 파일로 상태가 섞이지 않게 파일마다 새 trace를 생성한다.
- [x] 외부 요청 추가0. 강북 누적14/21요청·2,957,650byte와 중구32/32요청은 그대로다.
- [ ] 새 기록을 포함한 서울 실제 실패 위치 확인은 아직 실행하지 않았다. 이전 영수증을 보완하거나 성공으로 바꾸지 않는다.

## 기록 계약

| 필드 | 의미 |
|---|---|
| schemaVersion | 1 |
| step | NOT_STARTED / DIRECT / FLOW_STEP / BRIDGE_GET / PERIOD_POST / FINAL_POST |
| phase | BEFORE_TRANSPORT / TRANSPORT / PROFILE_PROCESSING / COMPLETE |
| transportInvocations | 다운로드 transport 호출 횟수, 0~4 |
| completedTransports | 정상 반환한 transport 횟수, 0~4 |

TRANSPORT는 호출 중 실패이며 실제 HTTP가 전송됐다는 증명은 아니다. DNS·허용 경로·예산 검사에서 중단될 수 있다. 리다이렉트도 transport 내부에서 처리하므로 이 숫자는 HTTP 요청 예약 원장을 대체하지 않는다. PROFILE_PROCESSING은 마지막 transport가 반환된 후 프로필 응답 해석/검증 중 실패한 경우다. 기존 failureCode와 함께 해석한다.

강북의 이름 있는 단계는 고정 profile code·메서드·경로에만 대응한다. 다른 다단계 프로필은 FLOW_STEP으로 기록한다. URL/query/form/파일명/응답/임의 예외문자열은 trace에 포함하지 않는다. 전송 전 거부는 0회/NOT_STARTED이고 원본 정리·4HTTP상한·공고 누적 예산은 기존 구현을 유지한다.

## 검증과 완료 경계

- 로컬 Java55개(전달13·probe42) 통과, 실패/오류/생략0. 각 단계 HTTP400, 기간 만료, 전송 전 거부, 리다이렉트 예약, 직접GET, metadata 요약 전달을 검증했다. 합성 HTTP이며 실제 강북 성공 증거가 아니다.
- Node9개 통과. BBS 관측JAR와 workerJAR에 trace와 중첩 상태 클래스를 함께 포함하도록 패키징 구성을 수정했다.
- 패키징20개 통과·두 probe JAR 생성 성공·각 JAR의 trace/Step/Phase/Snapshot4클래스 포함을 실제 ZIP 목록으로 확인했다. bootJar는 UP-TO-DATE이며 운영 빌드가 새로 실행됐다는 뜻은 아니다. 전체 :test는 이번에 재실행하지 않았다.
- 성공 기준: 기존 예산과 판정을 유지하면서 실패 단계 및 전송/해석 상태가 정확히 구분되고 비민감 metadata만 남는다.
- 실패 기준: 전송 호출 수를 HTTP 전송 수로 보고, 부분 다운로드를 성공 처리하거나 URL/폼값을 로그로 전송하는 경우다.
- 후속 강북 전체 관측은 최대20요청이므로 잔여7요청으로 재실행하지 않는다. 별도 고정4번째 파일 진단을 최대5요청 이내로 구성할 경우에도 기존4첨부 분모와 실패 영수증·단일 예약을 유지해야 한다. 이 문서는 실행 예약이 아니다.

## HWP 쪽 번호 분석 보류 근거

강북 HWP는 PAGE_NUMBER16byte와 UNSUPPORTED_CONTROL1이 관측됐다. 코드상 PAGE_NUMBER는 식별되지만 지원되지 않는다. 다만 기존 metadata에는 속성/필드 값이 없어 유일한 원인 또는 안전한 생략 대상으로 단정하지 않는다.

[한컴 형식5.0 revision1.2 표141/142](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.2.pdf)의 쪽 번호 위치 필드 설명과 [공개 읽기 구현](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/ForControlPageNumberPosition.java)은 속성 뒤16bit 필드 해석이 다르다. 길이 일치만으로 장식·사용자 문자를 생략하지 않는다. 이번 변경은 HWP 품질 판정을 완화하지 않는다. 실제 속성·앵커·자식 구조와 독립 fixture를 확인한 후 지원 범위를 확정한다.

## 실행 명령

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentObservationTransferContractTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
.\gradlew.bat attachmentBbsObservationProbeJar attachmentOfficialWorkerProbeJar attachmentContractQaTest bootJar --no-daemon --max-workers=1
```

브라우저는 현재 명시 실행 요청 정책에 따라 미실행이다. 전체9Gate=8부분/1차단이며 실제 worker·DB/API·정상 기대값·운영 E2E 범위는 유지한다.
