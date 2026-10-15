# 수집 원문 문단·줄바꿈 보존 운영 반영

## 범위와 수용 기준

- 사용자 요청: 수집 원문의 문단과 줄바꿈이 운영 메모에 유지되도록 수정하고 운영 반영.
- 원인: 지자체 본문 추출에서 `Element.text()`와 `replaceAll("\\s+", " ")`가 HTML 구조와 개행을 평탄화했다.
- [x] 문단, br, div, 목록, 표 행 경계 및 원문 CR/LF를 일반 텍스트 개행으로 보존한다. 표의 셀은 공백으로 구분한다.
- [x] 원문에 없는 문장 구분은 추정하지 않는다. 연속 빈 줄은 최대 한 빈 줄로 정리하며 가로 공백을 정규화한다. 원문 CSS·표 모양을 복제하는 기능은 아니다.
- [x] 기존 본문 영역 선택·메뉴/첨부 링크 제외·네트워크 제한을 유지한다.
- [x] 기업마당·정부24 목록 API의 본문 필드에 같은 추출기를 사용한다. 제목·기관명·일자 등 기존 필드 정제는 바꾸지 않는다. 정부24 운영 수집 설정은 켜지 않는다.
- [x] 본문 복구 미리보기 버전을 NOTICE_BODY_3으로 변경하여 이전 정제 버전의 미적용 preview 적용을 거절한다.
- [x] 공고 전환 시 summary에 원문의 개행을 전달하는 회귀 검증을 추가한다. 기존 textarea·저장·재조회 구조와 API 계약은 유지한다.
- DB migration, 신규 의존성, 분류 규칙·운영 정책 변경, 자동 활성화, 수동 재수집 실행, 기존 운영 메모 일괄 덮어쓰기는 범위 밖이다.
- 브라우저 QA는 현재 요청에 명시 지시가 없어 사용자 정책상 실행하지 않는다.

## 적용 경계

신규 수집 또는 승인된 본문 재수집에서 새 추출기가 실행될 때부터 개행이 보존된다. 이미 평탄화되어 저장된 본문과 이미 작성된 운영 메모는 배포만으로 복원되지 않는다. 기존 중복·변경 감지 정책 때문에 일반 목록 재수집이 반드시 기존 본문 갱신을 뜻하지 않는다. 복구가 필요한 기존 건은 원문 재조회와 기존 메모 보존 여부를 확인한 별도 작업으로 다룬다.

## 검증 기록

- 최초 표적 검증: 283건 중 29건 실패. 기존 지역별 fixture의 한 줄 기대값과 새 개행 결과 차이였다. 모든 실패의 공백 정규화 결과가 기존 기대 문구와 같음을 확인한 뒤 개행을 포함한 정확한 문자열 assertion으로 갱신했다. 내용 누락·메타데이터/첨부명 혼입 방지 검증을 제거하지 않았다.
- 전체 검증 중 하남 textarea fixture는 원문에 줄바꿈이 없으므로 다른 지역의 표 경계와 별도 기대값으로 보정했다.
- Node: `node --test scripts/qa/attachment-conversion.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/workspace-style-contract.test.cjs` — 69건 통과.
- 로컬 전체 테스트는 8분 36초 후 실행 중단했다. 중단 시 1,912건 실행/1건 실패/290건 생략이었으며, 실패는 수정 전 하남 기대값으로 컴파일된 테스트였다. 전체 통과로 취급하지 않는다. 배포 파이프라인의 Linux 전체 검증으로 대체한다.
- 표적 재실행 중 `test bootJar --tests ...` 옵션 위치 오류로 실행 전 실패했다. `test --tests ... bootJar` 순서로 바로잡아 재실행했다.
- 후속 표적 실행에서 루트 289건은 통과했지만 하위 모듈에 동일 필터가 전달되어 `No tests found`로 명령이 실패했다. 최종 명령은 `:test ... :bootJar`로 모듈 범위를 명시한다.
- Java 표적 최종 명령: `.\gradlew.bat :test --tests '*AnnouncementBodyTextExtractorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementSource*ServiceImplTest' --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --tests '*AnnouncementSourceClassificationEngineTest' --tests '*AnnouncementSourceTextNormalizerTest' :bootJar --no-daemon --max-workers=2`.
- Java 표적 최종 명령 성공: 루트 테스트 289건/실패 0/오류 0/생략 0 결과와 bootJar 산출물의 UP-TO-DATE 재확인. 배포 전 Linux 전체 검증은 별도로 수행한다.

## 사전 운영 기준선

- 로컬 기준 HEAD: `3e30361fb91255329a8d04f789c8f900e90162a3`.
- 운영 기준: `bf7f41e65e420788daaaa88929a14bd0adcc6da4`, CodeDeploy `d-PINIC4CBL`.
- [사전 읽기 전용 진단 38706358060](https://github.com/FrostyCityMan/saneB/actions/runs/38706358060) 성공.
- 설치/bundle SHA-256: `19cfdb04b288bed4f84916d1ed61c190a5ecad9c9c00332f4e13ddf2e01ac887`.
- DB V91/실패 0, endpoint SHA-256 `54d78493dd529524fc95495bf94711d44a35aaf08f63c915ce9e2549b581d26a`.
- 정책 COLLECT_ONLY v2/row version 2, hash `88c3d36238dfe4d356186a434353ae770a6daeff491fbfb11e2bf9c4def9c4fc`.
- 첨부 worker/source batch true, 정부24 본문 flag UNSET. 활성·대기 첨부 작업 0. 연결 실행 없는 9월 RUNNING 일정 1건은 기존 기록으로 보존.
- 진단 readOnly=on/writes=0/ROLLED_BACK. 기존 사용자 미추적 파일을 보존한다.
- 복구: 기존 배포 hook의 app.jar.previous 및 health 실패 시 직전 JAR 복구. 신규 migration 없음.

## Gate

Decision: **Conditionally ready**. 로컬 표적 검증 완료. Linux 전체 검증 성공을 조건으로 배포하며, 아래 사후 기록이 작성되기 전에는 운영 배포 완료를 의미하지 않는다.

| Gate | 상태 | 근거 |
|---|---|---|
| 변경 범위·DB/API 보존 | [x] | 본문 정제 및 관련 회귀 테스트 한정 |
| Java 표적 테스트·bootJar | [x] | 표적 289건, 최종 명령 성공 |
| Linux 전체 회귀 | [ ] | 배포 파이프라인에서 검증 후에만 설치 |
| Node 회귀 | [x] | 69건 통과 |
| 운영 사전 진단·복구 | [x] | 고정 운영 배포의 읽기 전용 진단 |
| 운영 배포·사후 진단 | [ ] | 검증 후 실행 |
| 브라우저 QA·실공고 전환 저장 | [ ] | 정책상 미실행, 운영 데이터 수정 테스트 없음 |
