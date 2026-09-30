# 잔여 지역 Linux 파일 수집 비교

## 현재 단계 / Gate

- [x] HEAD `67339f9987bb16a94dac303ecf73e02d4e528af8`의 실제 GitHub 실행 확인
- [x] 고정 6지역·6공고·지역별 6요청/23MiB 계약 테스트 통과
- [x] Linux 고정6건 비교 및 6개 metadata 영수증 대장 반영
- [!] 화천 고정 worker 계약 불일치 유지, 전체 회귀 통과 아님
- [~] 현재 파일 확인 203/223, 잔여20. 원격 결과 반영 후에도 성공 수 불변

## CI 조회 정정

이전의 HEAD별 목록 조회 0건은 실행되지 않았다는 증거가 아니었다. check suite `99603676088`의 연결 실행을 조회하여 [run 36774807470](https://github.com/FrostyCityMan/saneB/actions/runs/36774807470)을 확인했다. 해당 실행은 같은 HEAD이며 실패다. 루트 테스트 콘솔은 4,409개 중 4,088통과·1실패·320생략이다. 실패는 `AnnouncementAttachmentHwacheonWorkerProbeTest.fixedCaseVersionBudgetAndTitleDoNotExpandOtherGroups`다. 독립 DB 산출물 후속 단계는 생략되었으며 전체 Gate 성공으로 해석하지 않는다.

workflow는 활성 상태이고 로컬/원격 blob은 일치한다. 기본 브랜치 master에는 해당 QA workflow가 없다. 원격 실행 목록이나 suite 자체의 상태만으로 성공·미실행을 단정하지 않고 연결 run과 job 결과를 확인한다.

## 이번 변경과 실행 범위

HWP 추출 고도화 보류를 유지하면서 발견·다운로드를 먼저 마무리하라는 사용자 지시에 따라 별도 job을 추가한다. 기존 contracts job의 실패는 그대로 유지한다. 새 job은 contracts 종료 후 순차 실행하므로 무거운 Gradle 작업을 동시에 실행하지 않는다.

| 지역 | 고정 공고 |
|---|---|
| 포천 | POCHEON-64129 |
| 강릉 | GANGNEUNG-60798 |
| 충북 | CHUNGBUK-67302 |
| 공주 | GONGJU-59971 |
| 평택 | PYEONGTAEK-95902 |
| 성남 | SEONGNAM-144735 |

기존 Windows 전송 실패와 다른 Linux 환경을 비교하는 1회 관측이다. 지역별 최대6요청·23MiB, 합계 최대36요청·138MiB이며 job은20분 제한이다. 기존 URI·제목·파일 수·프로필·TLS 검증·본문 및 파일 예산을 변경하지 않는다. 고정 표본·상한은 `RegionalTransportLinuxContractTest`로 검증한다.

실행은 QA 브랜치 push의 `[regional-transport-observation-01]` 표식과 `run_attempt == 1`에만 허용한다. 일반 push와 재실행은 외부 요청을 하지 않는다. 원격 수집 전용 task는 기존 제목 판정→본문 확보→공식 첨부 발견→파일 다운로드/서명 확인을 실행한다. HWP/PDF/HWPX 추출기와 운영 DB·worker·정책·배포는 실행하지 않는다. 원본은 기존 finally 정리 경로로 삭제하며 JSON/JUnit metadata와 생산 클래스 지문만 보관한다.

성공 파일은 유지하고 실패는 별도 오류로 기록한다. 파일 일부 확보를 전체 첨부 집합·구간 분석·운영 E2E 성공으로 승격하지 않는다. Linux 결과는 현재 inventory/프로필과 대조한 뒤 대장에 추가한다. 기존 영수증을 덮어쓰지 않는다.

## 로컬 검증

`./gradlew.bat --no-daemon :test --tests '*RegionalTransportLinuxContractTest'`: 20초, 종료0, 고정6공고와 합계 예산 검증 통과. 외부 요청 없음.

초기 Node workflow 경계3개와 기존 대장 판정기32개, 총35개가 통과했다. 이후 원격 결과와 추가 검증은 아래에 기록한다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이며 운영 변경은 없다.

## Linux 실행 결과

[run 36775945586](https://github.com/FrostyCityMan/saneB/actions/runs/36775945586), HEAD `9e835c595c3ae0084d1dcd27673aec9e669deb94`, 수집 job `110096466110`, metadata artifact `11125864406`을 확인했다. 실행 생산 클래스 SHA-256은 로컬과 동일한 `9a4d2f0639349d1d81fda75aca4a8d5dd6fe2b1134b2bd966d6927187714f2f0`이다. 6개 프로필 지문 모두 현재 inventory와 일치한다.

| 지역 | 실제 결과 |
|---|---|
| 충북 | 본문75자 확보·첨부1건 발견·179,201바이트 수신. `FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패; 정상 HWP로 집계하지 않음 |
| 포천·공주·평택 | 본문 `NETWORK_ERROR`, 상세 `TRANSPORT_FAILED` |
| 강릉 | 본문 `NETWORK_ERROR`, 상세 `TRANSPORT_TIMEOUT` |
| 성남 | 본문 `NETWORK_ERROR`, 상세 `TLS_FAILED` |

수집 task는6개 중5개 테스트 실패이며 충북1개는 부분 수집 보고서 생성으로 테스트가 종료0이지만 **파일 수집 성공은 아니다**. 정상 파일 추가0건, 모든 임시 원본 정리true, 운영 쓰기0이다. 보고서상 본문 상한을 포함한 예약 요청19회·예약 byte12,995,780이며 실제 HTTP 요청 수·실수신 총량으로 해석하지 않는다.

GitHub 출처는 `GITHUB_COLLECTION_ONLY` 및 `GITHUB_COLLECTION_ONLY_REPORT`로 분리한다. 실행 ID·SHA·job·artifact ID를 보존하며 로컬 실행이나 운영 worker 검증으로 표시하지 않는다. 대장은666→672영수증, 최신 표본289개 유지로 재현을 통과했다. 현재 지문이 아니어서 유보했던6개 지역의 실패가 새 지문으로 확인되어 오류 포함 지역은33→39, 오래된 지문만 있는 지역은12→6이다. 새로 고장난6지역이라는 뜻은 아니다. 파일 확인203/223(91.0%)과 전체 집합 Gate16/223은 그대로다.

### 원격 회귀에서 추가로 발견한 계약 수정

기존 contracts job은4,410개 중4,088통과·2실패·320생략으로 실패했다. 화천 고정 계약과 `AttachmentContractWorkflowTest`의 기존 단일-job 단언이다. 후자는 허용 job을 정확히 `contracts`와 `regional-transport-observation`으로 고정하고 순차·최초 실행·표식·권한 및 기존 실패 보존 검증을 추가했다. 안전 단언을 제거하거나 화천 계약을 완화하지 않았다.

수정 후 로컬 `:test --tests '*AttachmentContractWorkflowTest' --tests '*RegionalTransportLinuxContractTest'`:23초·20개 통과·실패/생략0. Linux 전체 회귀를 수정본으로 재실행한 결과는 아직 없다. Node는 GitHub 출처 및 Content-Type 오류 분리 검증을 포함40개, 대장672영수증/289표본 재현을 확인한다.

## 다음 조치

1. 충북 응답의 실제 Content-Type·파일 헤더를 최소 범위에서 확인한다. 현재 영수증에는 실제 Content-Type 문자열이 없으므로 추측으로 MIME 허용 목록을 넓히지 않는다. HWP 추출 고도화는 여전히 보류다.
2. 나머지5곳은 Windows와 GitHub Linux 모두 실패했다. 이를 서울 서버 성공/실패 증거로 대체하지 않는다. 기존 AWS 로그인·인증서 점검 승인 요청은 별도이며 운영 설정을 변경하지 않는다.
3. 남은20지역은 은평·서대문·검단·울산남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·공주·아산·의성·성주·봉화·남해다.

이번 변경은 QA workflow·테스트·읽기 전용 보고 도구·근거 대장이다. 응용 코드·Flyway·운영 DB/API·정책·worker·배포는 변경하지 않았다. `long-goal-operating-protocol`에 따라 파일 전송과 보류한 HWP Gate를 분리하되 전체 goal 미완료를 유지한다.
