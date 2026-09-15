# 태백 고정 HWP의 배포 없는 Linux QA

## 목적과 범위

운영 배포 재개와 별개로 현재 소스의 추출기1.0.3을 실제 HWP에 적용해 남은 부분 추출 원인을 확인한다. 기존 제목→본문→실제 첨부→관리자 최종 검증의 전체 장기 goal에서 실파일 증거를 확보하는 단계다. 실제 운영 설치·정책 QA·사용자 브라우저 성공을 대체하지 않는다.

- 표본은 이미 관측한 `TAEBAEK-176153` 공고/HWP1개로 고정한다. 임의 URL·임의 그룹·전체 기관 실행 입력은 없다.
- 기존 `AnnouncementAttachmentOfficialWorkerIntegrationTest`의 `TAEBAEK_HWP` 사례를 그대로 사용한다. 별도 목업 텍스트나 파일로 교체하지 않는다.
- 제목/공식 본문→발견/다운로드→현재 격리 추출기→worker→직접 생성한 임시 PostgreSQL→API 전체 텍스트·근거·상태 비교를 수행한다.
- 사례 상한420초·44요청·80MiB, worker의 파일/시간/격리/네트워크·DB lease 경계를 보존한다. DB는 fixture용 loopback 인스턴스로만 만들고 종료한다. 운영 DB 접속값을 읽지 않는다.
- 정책 게시·ENFORCE·기존 데이터 적용·배포·AWS/IAM/운영 설정 변경은 없다. 후보의 자동 confirmation/link/ACTIVE도 만들지 않는다.

## 실행 연결

`build.gradle`의 명시적 task `attachmentTaebaekHwpWorkerIntegrationTest`는 그룹을 `TAEBAEK_HWP`로 고정한다. 기존 양평3건 task의 기본값·분모·보고서 경로는 바꾸지 않는다. 일반 `test`에서는 외부 실행을 계속 끈다.

`.github/workflows/attachment-contract-qa.yml`의 `verify-taebaek-hwp-worker`는 기본false·workflow_dispatch에서만 실행한다. 같은 SHA의 기존 전체 계약 단계가 성공해야 이어서 실행하며 실제 추출기를 해당 빌드에서 준비한다. 취소된 운영 배포의 재실행이 아니다.

```powershell
gh workflow run attachment-contract-qa.yml --ref codex/attachment-three-stage-linux-qa -f verify-taebaek-hwp-worker=true
```

실제 실행 ID/SHA는 실행 뒤 조회한 값을 사용한다. 명령 존재를 성공 증거로 계산하지 않는다. 공식 사이트 시간 초과나 보고서 관측 지연만으로 같은 작업을 재시작하지 않는다.

## 증거 판정

- 필수 JUnit: 정확한 `AnnouncementAttachmentOfficialWorkerIntegrationTest`, 실행1·실패/오류/생략0.
- 필수 metadata: `build/reports/attachment-taebaek-hwp-worker/TAEBAEK-176153.json`.
- 두 파일 모두 이번 실행 시작 이후 생성되어야 한다. 명시 경로·파일 크기 상한을 검사하고 누락/오래된 결과를 거부한다.
- `attachment-taebaek-hwp-report.mjs`는 대상·범위·본문·전체 발견/처리1개·HWP 실제 텍스트/구조·요청 예산·원본/lease 정리·운영 쓰기0·정책/브라우저 미승인을 함께 검사한다.
- **PARTIAL_TEXT와 COMPLETE_TEXT 모두 연결 시험으로 관측할 수 있으나 완전성은 별도로 보고한다.** PARTIAL_TEXT는 정상 분석 완료가 아니며 두 경우 모두 관리자 최종 검증 요구를 유지한다. 이번 표본의 품질을 보기 전에 완전 추출을 선언하지 않는다.
- 업로드는 고정 metadata와 JUnit XML만이다. 원본 HWP·본문·추출 텍스트·DB 파일·일반 로그는 업로드하지 않는다. 실행 중 예외는 기존 시험의 원문 비노출 처리로 고정 코드만 전달한다.

## 현재 상태

- [x] 고정 task·수동 opt-in·보고서 판정기와 음성 조건 시험 구현.
- [x] Node 판정기31건 통과/생략0. 기존 공통 JUnit/보고서 작성 시각 검사10건을 포함한다.
- [x] workflow·공식 worker probe·고정 사례의 Java 표적35통과/외부 opt-in1생략(41초). 실파일 실행 성공이 아니다.
- [x] 패키지20통과/생략0, 빌드22초 성공. 웹 JAR/probe/추출기88건은 UP-TO-DATE다. 웹 JAR SHA256은 `00c6602595909e2a3bc45d4f789e29a0a16a3dcce06e00c6213a51e6e001cb46`으로 직전7a414e8과 같으며 production Java/DB/API/UI 변경은 없다. 로컬 전체 일반 회귀를 새로 실행한 것으로 계산하지 않는다.
- [~] 새 SHA를 원격 Linux에서 실행. 중복 push CI는 커밋의 `[skip ci]`로 생략하고, 별도의 수동 실행에서 같은 SHA의 전체 계약과 공식 HWP를 함께 검증한다. `[skip deploy]`와 배포 없는 전용 workflow를 유지한다. 수동 실행을 시작하지 못하면 새 SHA CI는 미실행이다.
- [ ] 실제 HWP 품질·숫자 구조 진단·DB/API 결과 수집 및 후속 구현 판단.
- [ ] 운영 재배포·전체 Provider/형식·승인된 활성화/기존 데이터·업무 브라우저 E2E.
