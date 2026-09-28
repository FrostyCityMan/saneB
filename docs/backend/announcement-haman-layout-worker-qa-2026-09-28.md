# 함안 최신 HWP 추출 결과의 worker·DB·API 격리 QA

## 목표와 범위

함안41306에서 관측한 추출기1.0.15 결과를 구간1.0.4 실제 worker → 임시 PostgreSQL → API·검수 context까지 검증한다. 기존 `HAMAN_SEGMENT`의1.0.12 입력·영수증은 수정하지 않고 `HAMAN_LAYOUT_SEGMENT` 명시 실행을 추가한다.

- 고정 공고1건·공식 HWP1개만 사용한다. CI 자동 외부 호출은 추가하지 않는다.
- 전체 격리 QA 승인 및 함안 기존 누적 상한60회/96MiB 안에서 진행한다. 이전31회/19,520,892byte를 보존하고 신규 최대5요청/24MiB를 예약한다. 최악 누적36회/44,686,716byte이다.
- 서울 서버 CPU1·메모리768MiB·임시공간1GiB·20분 이내. 운영 설치·DB·정책·worker 설정은 변경하지 않는다.
- 원본·임시 DB·자원·전송 객체는 회수하며 메타데이터 영수증만 보존한다. 브라우저 검증·운영 인증은 이 실행의 범위가 아니다.

## 성공·실패 기준

- [x] 실행 전 추출기1.0.15를 확인한다. 다른 버전은 임시 DB/외부 요청 전에 실패한다.
- [x] 입력 지문을 [1.0.15 실제 관측](announcement-hwp-page-layout-v1.0.15-2026-09-28.md)에 고정한다. HWP101,888byte·4,647자·214block·textHash `88bb6aebc74813186f8d9f3ac32b457e4a674c98bbf3649e3a1435077435542a`다.
- [x] 미지원record2/control1·PARTIAL_TEXT를 확인한다. 진단 누락·다른 수치·문자열 boolean·다른 파일·예산 초과를 통과시키지 않는다.
- [x] 기존 worker 검증을 재사용해 원문/DB·분석/DB·평가 입력FK·API projection·검수 context를 비교한다. GET의 쓰기·구버전 fallback·다른 source 접근·자동확정·공고 링크 생성은 허용하지 않는다.
- [x] UNKNOWN1/COMPLETE_TEXT_REQUIRED·REVIEW_REQUIRED를 유지한다. 이 검증은 미지원 레코드 해소나 정상 기대값 승인을 뜻하지 않는다.
- [x] 새 소스의 로컬 Java45·패키지20·Node12·Python36 검증 통과, 실패/생략0. bootJar와 probe JAR 확인 완료다. 단일 실파일 실행은 별도다.
- [x] 누적 예산 검사의 오프라인 정상31회/19,520,892byte 및 실패한 선행 SSM 거부를 확인했다. CheckOnly는 새 예약을 만들지 않는다.
- [x] 새 단일 실행 영수증을 확인했다. SSM Success/exit0·38.33초·JUnit1통과/실패·생략·취소0이다.
- [x] 원본·임시 자원 정리·전송 객체 제거와 운영 불변을 확인했다.

## 완료 경계

기존1.0.12 worker 성공이나1.0.15 메모리 관측을 새 worker 실행 성공으로 대체하지 않는다. 전체9Gate·ATT-001~062·운영 E2E 범위는 유지하며 실제 저장 검증이 통과해도 부분 추출·정상 기대값·정책 승인·운영 반영은 별도 미완료다.

## 검증 명령·준비 상태

- `:test --tests '*AnnouncementAttachmentHamanWorkerProbeTest' --tests '*AnnouncementAttachmentJungguWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' attachmentContractQaTest attachmentOfficialWorkerProbeJar bootJar`:2분20초 성공. 최초 `test` 명령은 하위 추출기 모듈에 동일 필터가 전파되어 해당 모듈의 시험0건 오류였으며 루트 `:test`로 정정했다.
- `node --test scripts/qa/attachment-official-worker-probe.test.mjs`, Python `-B -m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py`, `git diff --check`:통과.
- 선행1.0.15 소스0b565819의 [Linux CI36395464220](https://github.com/FrostyCityMan/saneB/actions/runs/36395464220)는 completed/success다. 이번 신규 worker 모드의 CI/실파일 성공으로 대체하지 않는다.
- 실행준비6e52839b7dbf4cfdac5ebe38e92c0b7a,135파일/88,417,838byte. archiveSHA256 `4d22cd1433b36fd8934d219c5e5af8b03e9c86590ea42fef00374d4130a3855c`, executionCodeHash `eb49922fe4ad08c3a1eefa1b8328478100aac353dc49d0c0f6a3f9eff7d629ea`. 아직 외부 요청 성공 근거가 아니다.

## 서울 실제 worker·DB·API 결과

- 소스 `a113ef653478163a113e07d618a2022c90739ac8`, 실행 `6e52839b7dbf4cfdac5ebe38e92c0b7a`, SSM `ed5a8b3f-9d7e-4f66-93bd-61e6467105fe`. 관측시각2026-09-28T08:47:44.228235863Z. 같은 업로드 세션76115가 terminal로 끝났으며 중복 제출하지 않았다. 전송이 약11분, 실제 probe가38.33초다.
- 제목 조합 통과·본문AVAILABLE/1attempt·공식 첨부1개 발견/1개 다운로드/1개 추출. 직전1.0.15 관측과 binaryHash/textHash/locatorHash·4,647자·214block·PARTIAL_TEXT가 일치했다. 미지원record2/control1은 그대로다. 이전 압축 영수증에는 전체 좌표가 없어 구·신 block 좌표 전수 비교로 표현하지 않는다.
- 실제 worker EVALUATED, job PARTIAL_FAILED/processing TECHNICAL_EXCEPTION, 최종 REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE다. 본문 BODY_COMBINATION_NOT_CONFIRMED와 첨부 미완료를 정상 후보로 승격하지 않았다.
- 구간1.0.4의 전체 텍스트 coverage UNKNOWN1/NOTICE0/COMPLETE_TEXT_REQUIRED, 분석 hash `b3e3f3390ae59742ce92f648a36578d94d0f9798dd2cdc981cd60ca2a9c187ef`다. 구간 개수와 저장 성공은 정상 역할 판정이나 정상 기대값 승인이 아니다.
- 원문/DB·독립 계산 분석/DB·평가 입력FK·구간 API·평가결합 API·검수 context 일치를 확인했다. 기본/구버전 GET의 쓰기·fallback 없음, 다른 source404·no-store·자동확정/공고 링크0을 검증했다. block API 비교는 첫10개이며214개 전체 API 페이지 전수 비교를 주장하지 않는다. API 검증은 MockMvc로 수행했으며 운영 인증·브라우저 E2E는 아니다.
- 신규4요청/2,204,906byte, 누적 **35/60회·21,725,798/100,663,296byte**. 남은25회가 자동 반복 승인을 뜻하지 않는다. 이번 단일 예약을 재사용하지 않고 후속 실행은 현재 영수증을 누적 보호 조건에 포함해야 한다. 중구32/32·강북16/21은 변경하지 않았다.
- 원본 제거·lease0·unit inactive·probe cleanup·서버 전송파일 정리, 운영JAR불변/healthUP·운영DB미사용/쓰기0 확인. 해당 S3 객체 삭제/부재와 plan.cleaned=true를 확인한 뒤 정확한 경로/길이/hash를 대조한 소유 로컬ZIP88,417,838byte만 제거했다. ZIP은 재생성 가능하며 plan/영수증/예약은 보존했다. AWS/시험용 Node 프로세스 종료, 사용자 기존 프로세스는 보존했다.
- 영수증 `build/temporary-bbs-qa-6e52839b7dbf4cfdac5ebe38e92c0b7a/result.json`, SHA256 `468900d40bbddccb4777f17f1d8acd5b7af12a2824d6f63505b0b8a50cb5ca4d`.
- [Linux CI36398310987](https://github.com/FrostyCityMan/saneB/actions/runs/36398310987)는17:49 KST 조회in_progress다. 실제 단일 worker 성공과 새 SHA의 전체 Linux 계약 통과를 구분한다. GSO/그림계열 미지원 구조·검토된 정상3건/형식별 기대값·전체 Provider 지원·정책 승인·운영 적용/E2E는 미완료다. 전체9Gate8부분/1차단·catalog정상0을 유지한다.

실행 명령: `TemporaryRun`의 동일 handle 추적 → `TemporaryTransferRead`(해당 객체만 읽기) → `TemporaryPoll`로 terminal 결과 보관 → `TemporaryCleanup` → Node 입력 지문 비교 및 정확한 소유 ZIP 정리. 브라우저는 현재 명시 실행 지시 정책상 미실행이다.
