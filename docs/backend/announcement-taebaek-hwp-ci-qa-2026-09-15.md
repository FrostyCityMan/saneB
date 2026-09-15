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
- [x] `cc79d597ed34c3549f380e90162383f86b570093`의 수동 [Linux34939913277](https://github.com/FrostyCityMan/saneB/actions/runs/34939913277) 성공. push 중복 실행만 생략했으며 실제 전체 계약과 공식 HWP1건을 실행했다. 배포는 없다.
- [x] 실제 HWP 품질·숫자 구조 진단·DB/API 보고서와 JUnit 회수·판정. 완전 추출 성공이 아니라 아래 부분 추출 결과다.
- [ ] 운영 재배포·전체 Provider/형식·승인된 활성화/기존 데이터·업무 브라우저 E2E.

## 2026-09-15 16:19 KST — 실제 실행 결과

같은 SHA의 CI가 성공했고, 별도 HWP 단계는16:18:35~16:18:59 KST에 실행됐다. 보관 artifact의 JUnit과 metadata를 다시 검사했다. root2676건=2411통과/265조건부 생략/실패0, 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0이다. 공식 HWP는 별도1건/실패·오류·생략0이며 보고서 판정기도 통과했다. 이 HWP 표본을 다른 외부 opt-in 시험의 성공으로 합산하지 않는다.

| 항목 | 관측 |
|---|---|
| 입력 | 태백176153/HWP125,952bytes, binary SHA256 `a424fffd308588d07a985076d379c4aedfe25c8b4576821065dbffac2c16c1d9`. 이전1.0.1 관측과 동일 파일 |
| 본문·첨부 | BODY AVAILABLE/1시도, 첨부 발견·처리1/1, 실제 추출기1.0.3 호출1 |
| 추출 | PARTIAL_TEXT/9,734자/495블록, 분리 구간 블록24, 불확실 scope 블록0, 문자 대체 표시0 |
| 처리 결과 | PARTIAL_FAILED/TECHNICAL_EXCEPTION/REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE, 문서 전체 완전성false·최종 관리자 검증true |
| 실제 연결 | 전체 저장 텍스트·파일/근거/분류 API projection 일치. 다른 공고로 근거 조회 시404. 자동 confirmation/link0 |
| 구조 metadata | section1/record2831/최대 깊이5. 표 레코드(tag77)18, 개체(tag76)3, 그림 개체(tag85)3 |
| 예산·정리 | 본문 상한 포함4요청/2,354,176bytes. 원본 정리true·남은 lease0·운영 DB 쓰기0 |
| 승인 경계 | 정책 QA false·기대값 승인false·인증 브라우저 E2E false. 운영 설치·활성화 없음 |

한컴 명세의 HWPTAG_BEGIN=16 및 본문 레코드 표에서 tag76은 개체, tag85는 그림 개체다. 현재 `HwpSectionText`는 이 미지원 레코드에서 부분 추출로 표시하므로 **부분 판정을 유발하는 실제 조건을 최소 하나 확인했다**. 그림에 문자가 있는지, 장식뿐인지, 다른 미지원 컨트롤도 있는지는 이 숫자 진단만으로 알 수 없다. 이미지 OCR이나 전체 문자 완전성을 주장하지 않는다. [한컴 HWP5 명세, 4.1·표57](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf)을 근거로 한다.

이전1.0.1의9,743자/480블록과 새1.0.3의9,734자/495블록은 텍스트·위치 지문이 다르다. 표 순서/분리/정규화 변경이 있으므로 숫자 차이만으로9글자 누락·복원 또는 모든 표의 정확성을 판정하지 않는다. 현재 정상 후보0/전체 Provider QA 미완료를 유지한다.

보관 경로는 `build/qa-results/run-34939913277-contracts/`와 `build/qa-results/run-34939913277-hwp/`다. 후자의 고정 JSON/JUnit을 `validateHwpWorkerReport`로 재검증했다. 원본/추출 텍스트는 로컬로 내려받지 않았다.

후속으로 같은 SHA의 [Linux34941434590](https://github.com/FrostyCityMan/saneB/actions/runs/34941434590)를 `observe-bbs-official-files=true`로1회 실행해 성공했다. 기존 태백184816/HWPX2개 모두 완전 추출이며 구파일/텍스트/블록/역할 기대값 변경0을 확인했다. 완료된 HWP 시험의 재시작이 아니다.5요청/2377939bytes·원본 정리·운영 쓰기0, 정상 후보0을 유지한다. 코드 변경과 실제 관측을 검토한 후 내용은 보존한 채 기대값의 지문/관측 시각만 별도 갱신했다. [태백 재검토 기록](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)을 따른다. 새 catalog의 고정 CaseExecutor 재비교는 아직 별도 검증이 필요하다.
