# 노원 추가 표본 수집과 은평 파일 오류 분리

## 2026-10-01 후속 진단: 새 링크와 제목 통과를 분리

- [x] AWS 갱신 후 서울 리전의 저장소 계정·유일한 saneB 배포 대상 일치 확인.
- [x] 공식 목록에서 신규 후보50503·49704를 확인하고 상세 HTTP200 및 HWPX 링크4개·2개를 관측.
- [x] 두 제목 모두 현재 저장소 DRAFT seed에서 `COMBINATION_NOT_MATCHED / TITLE_COMBINATION_NOT_MATCHED`. 파일 다운로드0, 성공 대장 추가0.
- [!] 기존48267·50607 파일404는 미해결. 새 링크 발견을 다운로드 성공이나 기존 장애 복구로 해석하지 않는다.
- [~] 9월28일 inventory223수집원 기준 과거/최신표본 파일 확보209·최초 미확인14, 현재 실행 지문 실파일20·미확인203. 전체 집합 및 운영 E2E 미완료.

이번 진단은 목록/상세의 공식 구조를 읽는 QA이며 실제 제목→본문→첨부 파이프라인 실행이 아니다. 원문 HTML은 메모리에서만 다루고 파일명·다운로드 인자·쿠키를 출력하거나 보관하지 않았다. 링크가 있어도 제목 정책을 통과하지 못하므로 첨부 요청을 하지 않았다. 정책·migration·프로필·운영 DB·worker를 변경하지 않았으며 운영 ACTIVE 규칙을 조회한 결과도 아니다.

| 관측 | 결과 / 추적 근거 |
|---|---|
| 로컬 공식 목록1요청 | 실패 `MethodInvocationException`; 내부 원인을 확보하지 않아 timeout으로 단정하지 않음 |
| 서울 기본 목록1요청 | SSM `2186eddd-c263-40dc-a040-a410f788a0ab`, 종료Success/0, HTTP200·33,155byte. 앵커 전용 탐색 결과0은 공고 부재 증거가 아님 |
| 서울 공식 제목 검색1요청 | 관측한 `not_ancmt_sj` 필드로 지원 검색. SSM `d34ac06e-8103-4c2a-a96f-9fd027ce3d5a`, 종료Success/0, HTTP200·31,309byte. 공식 `searchDetail`의 ID를 데이터로 확인하며 JS는 실행하지 않음 |
| 서울 신규 상세2요청 | SSM `b51832d2-d13a-4933-8030-92f7dadedab1`, 종료Success/0. 50503: HTTP200·15,933byte·HWPX링크4; 49704: HTTP200·15,208byte·HWPX링크2 |

서울 상세 관측시각은 `2026-10-01T05:24:07.232612+00:00`. 50503 상세 SHA256 `9931fb9249d5f3d32c64a015b52a56cb49f68c734bc9d7eae51230e55b709d34`, 49704 상세 SHA256 `1cf24ffeb117a3291c3d4b70a277899dbc0029e85b255c12083defcab062e7a9`다. 50503 제목은 ‘2026년 가정용 음식물류 폐기물 소형감량기 구매 지원사업 시행 공고’, 49704 제목은 ‘1인가구 전입 생활 지원 '은빛SOL라이프' 사업 공고(2차)’다. `가구`는 V65의 CONTEXT이며 `1인가구`를 승인된 TARGET으로 임의 추가하지 않는다. 이 정책 범위를 바꾸려면 별도 규칙 검토가 필요하다.

합계 로컬1+서울4 HTML 요청이며 첨부 다운로드0, 원본 저장0, 운영 데이터 쓰기0이다. 서울 각 요청12초·본문2MiB, 상세 묶음2요청·4MiB·40초 상한, CPU1개·메모리128MiB 내로 실행했다. TLS 검증·공식 User-Agent·리다이렉트 금지 유지. AWS CLI 출력 인코딩은 로컬 UTF-8로 지정하여 같은 SSM 결과를 재조회했으며 한국어 표시 복구를 위해 HTTP를 재요청하지 않았다. 원격 프로세스는 종료됐고 서버에 임시 파일을 생성하지 않았다.

검증: `./gradlew.bat :test --tests '*EunpyeongFreshTitleContractTest' --tests '*SeoulFirstDownloadContractTest' --no-daemon --max-workers=1 --console=plain`에서12개 통과·실패/생략0. 신규2개는 실제 DRAFT seed를 격리 DB로 읽는 **제목 정책 테스트**이며 실제 첨부나 운영 검증이 아니다. Node 진단의20/223·209/223·14잔여는 그대로다. 브라우저는 현 요청에 명시 지시가 없어 정책상 미실행. 아래9월30일 수치는 당시 이력이다.

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일을 보존하고 본문·발견·다운로드·추출 오류는 분리한다. 제목→본문→첨부→관리자 최종 검증 순서와 자동 활성화 금지를 유지한다. HWP 추출기1.0.16 추가 개선은 보류 상태다.

- [x] 노원 새 공식 공고의 PDF1·HWP2, 총602,727byte 다운로드·형식 검사·원본 정리.
- [x] 은평 새 공식 공고의 본문447자·첨부4개 발견, 파일별 HTTP404 기록.
- [x] 기존 노원 형식 불일치·PNG 미지원·본문 공백, 은평 이전404 기록 보존.
- [x] 제목 정책·정상 파일 보존·오류 분리 회귀, 선택 Java437개 중435통과/2조건부 생략, bootJar 통과.
- [x] Node23개,337기록/273최신 공고 재현. 이전335기록 hash·상태·샘플 및 두 지역 외 결과 불변 확인.
- [~] 다운로드 확인190/223수집원(85.2%),33잔여(미연결5+등록 미확인28).
- [!] 노원 신규 본문 BODY_TEXT_EMPTY, 은평 파일404, 울산 남구 등록 조회12초 시간 초과.
- [ ] 나머지 지역, 상시 목록 유입·worker·추출·구간 분석·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 변경 범위와 계약

수집 엔진을 변경하지 않고 기존 시스템 프로필을 사용하는 참조 표본2개와 독립 실행 그룹을 추가했다.

| 표본 | 시스템 프로필 / 수집원 | 실행 그룹 | 첨부 목록 |
|---|---|---|---|
| NOWON-20260824152519260 | LOCAL_NOWON_BOARD_V1 / LGS-000012 | NOWON_SUPPORT | PDF1·HWP2 |
| EUNPYEONG-50607 | LOCAL_EUNPYEONG_GET_V1 / LGS-000013 | EUNPYEONG_SUPPORT | HWPX4 |

노원 제목은 ‘2026년 노원구 청년 창업기업 인증 및 지원계획 공고’, 은평 제목은 ‘2026년 4분기 중소기업육성기금 융자지원 계획 공고’다. 두 표본 모두 변경하지 않은 DRAFT 제목 정책에서 COMBINATION_MATCHED를 확인했다. 공식 상세의 실제 제목을 다시 대조하고, 고정 표본 제목만으로 파일을 수집하지 않는다.

이전 NOWON-20260915151630474, EUNPYEONG-48267과 caseCode·공고 식별자를 분리했다. 기존 프로필 지문은 변경하지 않았으며 기존 실패 표본을 새 성공으로 덮어쓰지 않는다. 기존 NOWON/EUNPYEONG 실행 그룹도 유지한다. 카탈로그282공고/218대상(참조281+기존 기대값1), 시스템 프로필219개(지역218+기업마당1)다. 신규 expectation은 null이며 정책 승인이나 실행 기대값으로 자동 승격하지 않는다.

Flyway V85까지, DB/API, A/B 우선순위, 관리자 최종 검증, HWP 추출기는 변경하지 않았다.

## 실제 관측 결과

### 노원: 파일3개 성공, 본문 오류는 별도

2026-09-30 20:46:25 KST, 공식 상세·첨부 영역 확인 후3개를 모두 다운로드했다.

| 형식 | 바이트 | SHA-256 |
|---|---:|---|
| PDF | 498,791 | 770181bab363739bcd26fbab75c8bb7626f03bc34aa5b1f3a1a1efc63321885f |
| HWP | 34,816 | e5fcb87bdb26ae0bdb96ebc29c958b50a1013f4f0c3848d7c01c5b92ebfc59eb |
| HWP | 69,120 | bf67a4916e8f633471be776fac4abc010218ca0f1af1a2ac623dd958bf967126 |

- 프로필 지문: `bd4805169a14fc3ec760f201eac9bce4e8b34197f127aa2bbd36eead1ede3ca0`.
- 본문 FETCH_FAILED/BODY_TEXT_EMPTY·REVIEW_REQUIRED, 확보 텍스트0자. 별도 공개 상세 확인에서도 article-body 구간에 이미지2개가 존재했다. 이미지 OCR은 수행하지 않았다.
- 첨부 FOUND/complete=true, 다운로드3·실패0·미지원0. 본문 오류가 첨부 시도를 막지 않았다.
- 파일 수집 단계는 완료했지만 본문→첨부 텍스트 분석 전체 완료는 아니다. HWP 파일의 다운로드/서명 검사와 내부 텍스트 추출 성공을 구분한다.
- 보고서: `build/reports/attachment-regional-collection/NOWON-20260824152519260.json`.
- 요청 예약6/6회, 예약량3,101,287/24,117,248byte. 원본 정리=true, 운영 쓰기0.

### 은평: 본문·발견 성공, 파일4개404

2026-09-30 20:46:38 KST, 본문447자 AVAILABLE, 본문 판정 REVIEW_REQUIRED/BODY_GROUP_A_MATCHED다. 이는 관리자 확정 결과가 아니다.

- 프로필 지문: `1a8524e32e6e677733d4d590095c083bd30cfb18eb50e0cf67943031367faa9d`.
- 첨부 FOUND/complete=true, HWPX4개 모두 FILE_DOWNLOAD/ATTACHMENT_HTTP_404. 파일별 시도1회이며 다른 파일 시도를 중단하지 않았다.
- 공식 goDownLoad 함수에 기재된 FileDown.jsp와 인코딩 방식(encodeURI의 괄호·슬래시 보존)을 적용한 첫 파일 별도 진단1회도 HTTP404·text/html·1,040byte였다. 임의 경로·User-Agent 변경·TLS 해제·HTTP 하향 우회는 하지 않았다.
- 따라서 단순 표본 변경이나 이 인코딩 차이로 회복되지 않았다. 서버 내부 원인은 미확인이다. 다른 공식 제공 경로 또는 제공기관 확인이 후속이며 같은 요청 반복은 중단한다.
- 보고서: `build/reports/attachment-regional-collection/EUNPYEONG-50607.json`.
- 요청 예약7/7회, 예약량2,113,536/24,117,248byte. 원본 정리=true, 운영 쓰기0. 수집 태스크의 정상 종료를 다운로드 성공으로 집계하지 않는다.

## 오류 분리 / 잔여 범위

상시 worker의 기존 회귀를 다시 실행했다. 정상→503실패→정상 파일의 결과를 즉시 함께 저장하고 전체를 자동 재시도하지 않는 동작, 부분 발견에서도 검증한 파일을 수집하는 동작, 추출 예외가 다음 파일을 막지 않는 동작, checkpoint 재개 시 성공 파일을 다시 받지 않는 동작을 통과했다. Intake의 정책 불일치 차단과 제목 미판정/비활성 처리도 통과했다. 해당 테스트는 로컬 mock 기반이며 운영 worker 활성화 증거가 아니다.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개다. 운영 재조회나 고유 행정구역 수가 아니다. 최소1표본 다운로드 확인190개·미확인33개이며, 성공 지역과 중복되는 오류 지역은46개다. 엄격한 전체 세트 Gate16/223은 그대로다. 85.2%를 전체 개발·텍스트 추출·운영 완료율로 사용하지 않는다.

미연결5개는 강동(LGS-000026), 울산 남구(079), 괴산(144), 천안(148), 서천(158)이다. 이번 울산 남구 등록 공개 조회 POST는12초 제한에서 `TaskCanceledException > TimeoutException`을 포함한 원인 체인으로 종료했다. 과거의 원인 미확인 MethodInvocationException과 구분하며 기관 전체 장애로 단정하지 않는다. 나머지4개는 이번에 재요청하지 않았다.

등록 후 다운로드 미확인28개는 은평·서대문·송파·부산시·연제·검단·유성·성남·평택·오산·포천·이천·동두천·강릉·속초·평창·철원·충북도·충주·영동·공주·순창·진도·의성·영덕·성주·진주·합천이다. 개별 원인은 저장된 관측 시점 기준이며 이번에 모두 다시 확인한 상태가 아니다.

## 요청 경계 / 검증 / 미실행

공개 조사10회(노원5GET, 은평4GET, 울산 남구1POST), 각12초·2MiB 상한·자동 redirect 금지·TLS 검증·수집기 User-Agent를 유지했다. HTML/진단 응답 원문을 파일로 저장하지 않았고 응답 자원을 종료했다. 실제 수집 요청 예약13회와 합산한 상한은23회다. 실제 요청 횟수와 본문 최대시도를 포함한 예약 상한은 구분한다.

검증 이력:

1. `gradlew.bat --no-daemon :test`에서 NowonDownloadContractTest·AnnouncementAttachmentWorkerServiceTest·AnnouncementAttachmentIntakeServiceTest 선택:47초,64개 중62통과/2조건부 생략.
2. 첫 선택 회귀:3분13초,427개 중1실패/2생략. 추가 표본에 맞지 않는 노원 referenceCount=1 기대값 때문에 실제 HTTP 관측 전에 중단됐다. 새 표본2개 모두 참조 전용임을 검증하도록 보정했다.
3. Nowon·SeoulFirst 계약과 해당 catalog 검사2개 선택:53초,21개 중19통과/2생략.
4. Nowon·SeoulFirst·SeoulGangseo 계약, worker, intake, 목록 collector, 본문 client, catalog, policy snapshot, inventory, 파일 형식, probe 선택 회귀와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=NOWON_SUPPORT,EUNPYEONG_SUPPORT -PsanebCollectionWindowsTrust=true`:3분33초,437개 중435통과/2생략·실패/오류0. 전체 프로젝트 suite 통과로 확대하지 않는다.
5. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과.
6. `node scripts/qa/verify-collection-receipt-index.mjs`:337기록/273최신 공고 재현. `report-collection-availability.mjs`:190확인/33잔여 재현.

Java 생략2개는 SANEB_NOWON_SURVEY_FIXTURE 환경변수와 과거 HTML/binary 원본이 필요한 조건부 검사다. 이번 실제 파일 관측2건과 구분한다. 검증 중 대장 patch 초안 출력이 길어 잘렸으나 적용하지 않았으며, 항목별 patch로 다시 생성해 적용·재현했다.

장기 goal 운영 절차에 따라 계약·실제 파일·운영 검증을 분리했다. 운영 DB·설정·worker·정책·ENFORCE·기존 데이터·배포 변경 없음. 브라우저는 현재 요청에 명시 지시가 없어 정책상 미실행이다. Linux 격리 추출·임시 DB/API·AWS 운영 조회도 미실행이다. 기존 연제/구례 조사 원본 정리 미완료, AWS 인증, 상시 목록 유입 후속은 유지한다. 새 관측 임시 원본만 정리되었으며 사용자 output/·__pycache__는 보존한다.
