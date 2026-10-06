# 청양 본문 복구·오류 목록 분리 배포

## 범위와 승인

- 사용자는 운영 배포·서비스 재시작·`SRC-017843` 단건 본문/분류 버전 갱신을 승인했다.
- 대상 코드: `019dae32cc53ee6c1974b9671453f16874544b18`, 고정 태그 `deploy-approved-019dae3-20261006`.
- 청양 전용 본문 파서, 정상 수집 실행의 실패 코드 저장, 선택적 V1 목록 필터와 ‘본문 수집 오류’ 탭을 추가한다.
- 상세 원인·구현·검증 한계는 [복구 기록](../backend/cheongyang-body-recovery-2026-10-06.md)을 따른다.
- migration 추가/수정, 자동 활성화, 전체 재수집, 정책 게시, worker 설정 변경 없음.
- 다른 미추적 파일과 목업·AWS 도구는 커밋에 포함하지 않았다.

## 코드 검증

- 로컬 관련 Java351건 및 Node11건 통과, bootJar 생성 성공.
- 서울 임시 읽기 전용 진단에서 HTTP200/8174바이트 응답에 새 파서 적용 시 본문718자 추출. 기존 설치 클라이언트는 `BODY_SELECTOR_CHANGED` 재현. 운영 DB 쓰기0.
- Windows 실제 DB 검증은 Application Control의 pgcrypto.dll 차단으로 실패했다. 보안 정책을 변경하지 않았다.
- 첫 두 Linux CI는 신규 테스트 fixture의 NOT NULL/원문 해시 유일성 위반으로 실패했다. 실패를 보존하고 실제 schema 계약에 맞춰 테스트를 수정했다. 제품 schema 제약은 완화하지 않았다.
- 최종 [Linux CI37428267307](https://github.com/FrostyCityMan/saneB/actions/runs/37428267307): 성공, 대상 SHA 일치.
  - Flyway 실제 DB7/7(오류 목록 실패/복구/레거시 분리 포함), 실패·생략0.
  - 첨부 job229, migration18, worker12, runtime7, 계약 실행기20, 정책 부모2건 실패·생략0.
  - 기본 test4703건 중 실패0, 조건부 시험403건 생략. 전체 시험을 생략0으로 표현하지 않는다.
  - 독립 산출물의 임시 namespace/PostgreSQL 실행 및 취소·정리 Gate 성공.

## 배포 전 운영 기준선

[직전 사전 점검37430184117](https://github.com/FrostyCityMan/saneB/actions/runs/37430184117): 성공.

- 기존 정상 배포 `d-27REJCB7L`, 코드 `45d39ae1f0f478a5378af19486e98166e7dbeade`.
- 설치/배포 원본 JAR SHA-256: `6b4442a25a3d0c4f71a90a1b50b5d11e4aa7f5843878fb74c5145dd00fe97480`.
- DB V91, migration 실패0. 진행/대기 첨부 작업0, 정책QA/수집원QA0, 대기 일정0.
- COLLECT_ONLY 개정2/row_version2, worker/source-batch=true, 정부24 상세 flag UNSET 유지.
- 2026-09-07 예정/09-09 갱신/연결 run 없는 RUNNING 이력1건은 기존 미종결 기록이다. 현재 실행 중 수집으로 단정하거나 이번 작업에서 수정하지 않는다.
- 읽기 전용 트랜잭션 ROLLBACK, 업무 데이터 쓰기0. 직전 정상 JAR을 보존하는 기존 backup/validate 절차를 사용한다.

## 배포·사후 Gate

- [x] 고정 SHA Linux 검증 및 직전 운영 사전 점검.
- [x] [배포37430315039](https://github.com/FrostyCityMan/saneB/actions/runs/37430315039): `attachment_qa=false`, 고정 SHA 일치, 성공.
- [x] CodeDeploy `d-7870D16X8`/ValidateService 성공 및 설치 JAR 지문 일치.
- [x] 배포 후 health200/UP, 수정 JS 정규화 지문 일치, DB·정책 유지.
- [ ] SRC-017843 인증된 미리보기·적용·복구 결과 확인.
- [ ] 브라우저 조작 명시 승인 및 실제 화면 검증.

인증/버전/진행 작업/운영 연결 보호를 우회한 직접 SQL 갱신은 하지 않는다. 본문 복구 후에도 관리자 최종 검수와 운영 공고 활성화는 별개이며 이번 범위가 아니다.

## 사후 영수증과 남은 작업

- [사후 진단37430962217](https://github.com/FrostyCityMan/saneB/actions/runs/37430962217) 성공. 서비스 시작2026-10-06 16:37:42 KST.
- 설치/해당 배포 원본 JAR SHA-256 `54ad28b30d836162e855c9b200ba8e2c6b1349821972f1438bb995ff6ba28e7a` 일치. 복구 JAR은 배포 직전 `6b4442a25a3d0c4f71a90a1b50b5d11e4aa7f5843878fb74c5145dd00fe97480`과 일치한다.
- V91/실패0, COLLECT_ONLY 개정2/row_version2 및 정책 hash, worker/source-batch/정부24 flag는 사전과 동일하다. active/waiting 첨부 작업0, 진단 READ ONLY/ROLLBACK/쓰기0.
- Node HTTP GET: health200/UP, `/js/saneb-collected-announcements.js`200 및 저장소와 CRLF 정규화 후 일치. JS SHA-256 `270f11f8a680c85c25436b4ad74604dfeb52a7d4ca1ecdc2fdfdba5c900acd14`.
- 배포 산출물 본문 클라이언트의 서울 읽기 전용 재현 SSM `aef161b8-941c-4b41-a648-a6d08f6a1b84`: HTTP200, AVAILABLE, 오류 없음, 본문718자, 시도1/리다이렉트0. 운영 DB 쓰기0. 임시 클래스/실행 디렉터리는 종료 시 정리했다.
- 사후 단건 DB 읽기 SSM `f4cc7769-abef-485c-8a41-48803ffa37f6`: SRC-017843 body_length=null, 현재 body_stage_code=FETCH_FAILED. 기존 실패 이력은 그대로다.
- 브라우저 조작 승인 응답이 없어 사용자 정책에 따라 브라우저를 열거나 제어하지 않았다. 인증된 미리보기 생성/적용은 **미실행**이며 별도 승인을 기다린다. 직접 SQL이나 가짜 관리자 principal로 우회하지 않는다.
- 사용한 Node/Java/PowerShell 단발 작업은 종료됐다. 기존 사용자 프로세스·미추적 파일은 종료/삭제하지 않았다.

## 최종 판정

**Conditionally ready** — 승인된 코드 설치와 서버 검증은 완료했다. 단건 본문 DB 복구·오류 목록 이탈 확인·브라우저 검증은 남아 있다. 운영 파서가 본문을 추출할 수 있다는 증거와 실제 공고에 저장됐다는 증거를 구분한다. 새 오류 탭의 실제 화면/모바일 검증도 미실행이다. 모든 청양 공고의 구조가 동일함을 보장하지 않으며 구조가 달라지면 별도 오류로 남는다.
