# 연제구 상반기 지원 공고의 HWP 수집

## 현재 단계 / Gate

- [x] 공식 공개 검색으로 별도 지원 공고42552 확인
- [x] 제목 정책·공식 상세 제목·본문111자·첨부HWP1개 검증
- [x] 실제HWP95,232byte 다운로드와 임시 원본 정리
- [x] 기존43358 형식 불일치 기록 및 카탈로그288개 보존
- [x] 신규 참조1개·관측 그룹·회귀·패키징·대장 재현
- [~] 과거 다운로드204/223(91.5%), 미확인19개
- [~] 현재 코드38/223, 신규 확인·재검증185개
- [ ] HWP 추출·구간 분석·전체 파일 집합·상시 운영 E2E

기준 HEAD `67ba058e92794f3f03a4d117d2a388ee71c12015`. 지역별 첨부 발견·다운로드 우선 방침을 유지한다. 응용 Java·DB/API·Flyway·프로필·키워드 규칙·운영 설정은 변경하지 않았다. HWP 추출기1.0.16 추가 개선은 보류다. 기간이 지난 지원 공고도 처리기 검증을 위한 공식 참조로만 사용하며 현재 신청 가능한 운영 공고로 활성화하지 않는다.

## 공식 공개 검색과 표본 선택

연제구 공식 목록은 `/portal/saeol/gosi/list.do?mId=0206030000`이다. 공개 페이지의 `form#list`는 `page`, `seCode`, `searchType`, `searchTxt`를 POST로 제출한다. 페이지의 `csrf_token` meta와 `CSRFToken` 폼 필드, 공개 세션 쿠키를 해당 세션 내에서만 사용하면 검색HTTP200을 확인했다. 로그인·관리자 계정·인증 우회는 사용하지 않았다. 토큰·쿠키 값은 출력·파일 저장·문서화·첨부 요청 전달을 하지 않았다.

‘소상공인’ 검색은 교육 모집 공고1건으로 제목 정책 통과 여부를 임의 가정하지 않았다. ‘다자녀’ 검색에서 기존 하반기43358과 별도로 상반기42552를 확인했다. 선정 결과 공고는 새 표본으로 선택하지 않았다.

표본: `YEONJE-42552`, **2026년 상반기 연제구 다자녀가구 전세자금 대출이자 지원사업 시행 공고**.

공식 상세 제목과 `dl.view_file`의 HWP1개, 같은 파일의 미리보기 링크를 확인했다. 첫 조사 정규식은 함수 인자 사이 공백 때문에 링크를0개로 계산했으나 실제 영역을 확인하여1개임을 바로잡았다. 이를 ‘첨부 없음’으로 저장하지 않았으며 기존 Java 선형 파서는 공백을 이미 지원하므로 응용 파서를 수정하지 않았다.

## 구현 범위

- `YeonjeGuryeDownloadCases.selectYeonjeFirstHalfCase()`와 `YEONJE_FIRST_HALF` 그룹을 추가했다.
- 기존 `YEONJE_GURYE` 그룹은43358·구례25440 그대로다. 형식 오류가 난43358을 새 성공 표본으로 덮어쓰지 않는다.
- 카탈로그는288→289개이며 기존288개 내용과 기대값을 보존했다. 신규42552의 `expectation=null`, 참조 전용, 실행 가능/정책 승인0이다.
- 연제구 참조 수2·구례1을 정확히 검증하도록 테스트를 갱신했다. 지원대상·지원유형·A/B 우선순위는 변경하지 않았다.
- 정확한 source identity, 제목, HWP descriptor, 공고 식별자가 일치하는 미리보기 중복 제거, 다른 공고 미리보기 오류 분리, 수집 전용6요청/23MiB 예산을 검증했다.
- 인벤토리의 모든 프로필 지문이 이전과 같음을 전체 목록 해시로 비교했다. 인벤토리 내용 변경은 카탈로그 참조 추가에 따른 것이며 운영 대상 재조회가 아니다.

## 실제 수집 근거

2026-10-01 01:40:13 KST 시작, `build/reports/attachment-regional-collection/YEONJE-42552-YEONJE-FIRST-HALF-20261001.json`:

| 단계 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE111자, TARGET_SUPPORT_CONFIRMED |
| 첨부 | FOUND·전체 발견 완료, HWP1개 |
| 다운로드 | 95,232byte, 실패0 |
| SHA-256 | `93528a0466c99f9b5063fa6e334f5c1348eb419a9cd9e517fa97d77fe7d766b3` |
| 프로필 지문 | `5559ec6ac8e0b9e570e541ba78bd9b29918c368a4b29247726e8012db32cb419` |
| 원본·운영 | 임시 다운로드 원본 정리true, 운영쓰기0 |
| 미검증 | 격리 추출·텍스트 분석·정책 QA·운영 E2E |

실제 본문 포함 요청 예약 상한4회·바이트 예약2,692,096이다. 허용 상한6회·23MiB를 유지한다. 공개 조사는8요청(GET6·조회 POST2), 각각12초·2MiB 상한이며 원문을 파일에 저장하지 않았다. 조사 포함 실제 예약 상한12회, 최대 예산14회다. 예약량과 실제 wire 전송량은 다르다.

## 검증 결과와 실패 이력

1. 첫 선택11개:9통과·1실패·1조건부 생략. 새 테스트가 일반 관측 Budget을 호출한 탓에44요청을 받아 실패했다. 실제 수집 전용 `selectBudget(profile,true,false)`를 테스트하도록 수정했다. 예산 제한을 늘리지 않았다.
2. 확대142개:140통과·1실패·1조건부 생략. 기존 연제 카탈로그 참조 수1 가정이 실패했다. 연제2·구례1과 두 연제 공고의 참조 전용 상태를 검증하도록 갱신했다. 이 실패 실행에서는 실제 파일 관측 태스크가 실행되지 않았다.
3. 수정 후 관련12개:11통과·기존 조사 HTML fixture1조건부 생략·실패0. bootJar와 실제 단일 수집 관측이 통과했다. 전체142개를 최종 상태로 재실행한 것은 아니다.
4. Node 수집 대장 테스트23개 통과. 영수증418개·최신 공고282개 재현 통과. 기존 영수증417개·최신 공고281개 내용은 보존한다.
5. 재개 후 인벤토리 생성 태스크를 제외한 관련6개 클래스 전체를 다시 실행했다. 총139개 중138통과·기존 조사 HTML fixture1조건부 생략·실패0, bootJar 정상이다(2분56초). 외부 재다운로드 없이 보관 기록 재현·과거204/223 집계·기존 카탈로그288개 보존을 다시 확인했다. 민감정보 패턴 검사0건·diff 공백 검사 통과, 작업 소유 Java/Node/Python/curl 잔여 프로세스0개를 확인했다.

```powershell
.\gradlew.bat --no-daemon :test --tests '*YeonjeFirstHalfCollectionContractTest' --tests '*YeonjeGuryeDownloadContractTest' :bootJar
# 보관 인벤토리 조회 조건만 지정한 확대 검증
.\gradlew.bat --no-daemon :test --tests '*YeonjeFirstHalfCollectionContractTest' --tests '*YeonjeGuryeDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*RegionalCollectionReportArchiveTest' --tests '*AttachmentCollectionOnlySummaryTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YEONJE_FIRST_HALF -PsanebCollectionReportLabel=YEONJE-FIRST-HALF-20261001 -PsanebCollectionWindowsTrust=true
# 수정 후 집중 재검증과 실제 파일 관측
.\gradlew.bat --no-daemon :test --tests '*YeonjeFirstHalfCollectionContractTest' --tests '*YeonjeGuryeDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest.yeonjeGuryeReferencesDoNotApproveExtraction' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=YEONJE_FIRST_HALF -PsanebCollectionReportLabel=YEONJE-FIRST-HALF-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
# 재개 후 외부 요청 없는 확대 회귀
.\gradlew.bat --no-daemon :test --tests '*YeonjeFirstHalfCollectionContractTest' --tests '*YeonjeGuryeDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*RegionalCollectionReportArchiveTest' --tests '*AttachmentCollectionOnlySummaryTest' :bootJar
```

인벤토리 SHA-256: `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`. 관측 실행 코드는 신규 그룹 추가로 지문이 바뀌었으므로 새 영수증에만 새 producer hash를 기록했다. 이전 관측 hash를 갱신하지 않는다.

## 남은 범위

분모223은2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수나 전체 개발 완료율이 아니다. 누적 다운로드204/223, 미확인19개다. 최신 코드 다운로드38/223, 확인·재검증185개이며 현재 코드의 엄격한3표본·전체 집합 Gate는0/223을 유지한다. 오류 포함 수집원9개와 성공 수는 중복될 수 있다.

미확인19개: 은평, 서대문, 송파, 검단, 울산남구, 성남, 평택, 이천, 포천, 동두천, 강릉, 속초, 철원, 충북도, 영동, 천안, 공주, 의성, 성주. 연제43358 파일 오류는 여전히 별도 후속이다. 모든 지역 오류를 이번에 재조회한 것은 아니다.

운영 설치·DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. AWS 인증 대기와 기존 조사 원본·공개 CA 임시 파일 정리 차단은 별도 미해결로 유지한다. 이번 공개 검색 세션과 다운로드 원본은 정리했으며 과거 미해결 정리와 혼동하지 않는다.
