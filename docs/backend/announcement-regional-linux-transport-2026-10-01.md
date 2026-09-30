# 잔여 지역 Linux 파일 수집 비교

## 현재 단계 / Gate

- [x] HEAD `67339f9987bb16a94dac303ecf73e02d4e528af8`의 실제 GitHub 실행 확인
- [x] 고정 6지역·6공고·지역별 6요청/23MiB 계약 테스트 통과
- [~] Linux 파일 수집 비교: 원격 실행 결과 확인 필요
- [!] 화천 고정 worker 계약 불일치 유지, 전체 회귀 통과 아님
- [~] 현재 파일 확인 203/223, 잔여20. 원격 결과 반영 전 기준

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

Node workflow 경계3개와 기존 대장 판정기32개, 총35개가 통과했다. 원격 결과는 후속 확인한다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이며 운영 변경은 없다.
