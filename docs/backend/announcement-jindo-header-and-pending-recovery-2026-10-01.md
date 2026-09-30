# 진도 파일명 헤더 복구와 순창 첨부 수집

## 현재 단계 / Gate

- [x] 순창31739 HWP98,816byte 최초 다운로드 확인
- [x] 진도25159 파일명 응답 헤더 오류 재현·진도 전용 복원·HWP148,992byte 확인
- [x] 공유 구현의 곡성34854 HWPX85,169byte 회귀 수집
- [x] 집중 Java27개 통과, 확장324개 중 기존 기대값 오류1개 수정 후 관련25개 통과, bootJar·Node23개 통과
- [x] 기존399영수증 보존,405영수증/279최신공고 재현
- [~] 과거 관측 포함201/223수집원(90.1%) 다운로드 경험, 미확인22개
- [~] 현재 코드 실파일 관측27/223, 나머지196개 신규 확인·재검증
- [ ] 전체 첨부 집합·추출·상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E

2026-09-30 시작 작업을10-01에 마쳤다. 실행 라벨의20260930은 작업 식별자이며 실제 시각은 각 보고서 observedAt을 따른다. 분모223은2026-09-28T15:56:12.116778+09:00의 활성 지역 수집원 스냅샷이다. 고유 지자체 수·현재 운영 조회·전체 프로젝트 진행률이 아니다. HWP 추출 고도화는1.0.16에서 보류한다.

## 변경과 근거

기준 HEAD는 `fbc3c35a379400bf808cd0370b9342e1c80d6836`이다. 진도 공식 상세의 `/doc/dataDown.jsp` 링크는 실제 HWP를 반환했지만 Content-Disposition의 UTF-8 한글 바이트가 Latin-1 문자열로 전달되어 기존 엄격 검사에서 `ATTACHMENT_DISPOSITION_INVALID`가 발생했다. 이전 보고서의148,992byte를 성공으로 바꾸지 않고 수정 전 실패와 수정 후 성공을 별도 영수증으로 보존했다.

`JeonnamCountyAttachmentDiscoveryProfile.selectUtf8DispositionOctets()`를 진도에서만 true로 선택한다. 기존 복원기·공통 다운로드 클라이언트·파일 형식 검사·TLS·허용 호스트·공고 식별·redirect 조건은 변경하지 않았다. 곡성은 false를 유지한다. 실제 시그니처, 확장자 불일치, 제어문자, 경로 탈출, 잘못된 UTF-8, HTML 응답 차단 테스트를 추가했다.

진도 파일 SHA-256은 수정 전 실패·별도 HTTP 조사·수정 후 정규 다운로드 모두 `126cd3ccfff199c0d191d1da6fccf38cf64e94bddb27ed8352a6c2792163eba7`로 같다. 파일 본체 변경이 아니라 헤더 해석 호환임을 뒷받침한다. 내부 HWP 텍스트 추출 성공을 뜻하지 않는다.

공유 클래스 변경으로 실행 지문이 달라진 프로필은 진도·곡성2개뿐이다. 두 프로필 모두 수정 후 정규 다운로드를 확인했다. 나머지 지문과 카탈로그286공고·222대상·기존 기대값은 그대로다. DB·API·화면·Flyway V85·분류 규칙·운영 설정은 변경하지 않았다.

## 관측 결과

| 공고 | 결과 | 별도 남은 항목 |
| --- | --- | --- |
| 순창31739 | 본문464자, HWP98,816byte | 과거 HTTP400 뒤 동일 코드 성공. 과거400 원인은 미확정 |
| 진도25159 | 수정 전 헤더 오류, 수정 후 본문3,262자·HWP148,992byte | 추출·최종 분류·승인 미검증 |
| 곡성34854 | HWPX85,169byte | 본문 FETCH_FAILED / HTTP_STATUS_ERROR 별도 유지 |
| 속초32983 | 상세 TLS_FAILED, 다운로드0 | 본문 NETWORK_ERROR, 첨부 발견 전 중단 |
| 의성39093 | 본문534자·첨부1개 발견, 파일 TLS_FAILED | 다운로드0 |

순창 파일 SHA-256은 `f644ab8b3210734d9154c4b17950b88b4d7e53f47c712b0d3997cd0675b80f61`, 곡성은 `49b376a018b2faa3d419ff4fb898325434e49cfe6cee6cb41a3d7483129b6cda`다. 성공 파일3건332,977byte를 확인했으며 전체 파이프라인 성공으로 확대하지 않는다.

### 예산과 원본

- `PENDING-FOUR-20260930`:4보고서, 최대24요청·92MiB, 본문 포함 예약16회·8,937,984byte. 속초 상세 실패로 Gradle exit1이며 다른 정상 파일은 보존했다.
- `JINDO-HEADER-20260930`:2보고서, 최대12요청·46MiB, 본문 포함 예약8회·4,648,113byte. Gradle exit0, 곡성 본문 실패는 별도다.
- 합계 예약24회·13,586,097byte. 예약량은 실제 wire 요청 수·실전송량과 다르다.
- 별도 공식 HTTP 조사7요청, 각12초·2MiB, TLS검증·collector UA 유지·쿠키/redirect 없음. 원문은 메모리에서만 읽고 저장하지 않았다.
- 모든6보고서 원본 정리true·운영쓰기0·추출 검증false·정책 승인false다. 이전 연제·구례 임시 폴더 정리 차단이 해소됐다는 뜻은 아니다.

## 검증·실패 수정

1. `gradlew.bat --no-daemon :test --tests '*JindoHeaderCompatibilityTest' --tests '*JeonnamSecondDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentPinnedDownloadClientTest' :bootJar`:27개 통과.
2. 위 테스트에 inventory·catalog·정책 snapshot·본문 client·관측 transfer·collection summary·archive 회귀를 추가한324개 중323통과·1실패. 원인은 기존 snapshot 테스트가 카탈로그282건을 기대하지만 실제 HEAD 카탈로그는286건이었던 것이다. 카탈로그는 수정하지 않고 기대값을286으로 갱신했다. 이 실패로 뒤의 외부 수집은 실행되지 않았다.
3. `JindoHeaderCompatibilityTest`, `JeonnamSecondDownloadContractTest`, `AttachmentProviderInventoryAuditTest`, `AttachmentPolicyValidationSnapshotFactoryTest`를 재실행하여25개 통과·실패/생략0, bootJar 통과. 이어 진도·곡성 정규 관측2개가 종료0으로 완료됐다. 전체324개를 다시 실행했다고 주장하지 않는다.
4. inventory 비교에만 기존 읽기 전용 영수증과 환경변수를 사용하고 finally에서 복원했다. 운영 재조회가 아니다.
5. Node stage/receipts/availability23개 통과,405영수증/279공고 재현, 현재 코드27다운로드·오류 포함7수집원·엄격3표본 전체 집합 Gate0을 확인했다. 곡성 본문 오류는 첨부 오류7개 집계와 별도다.

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JINDO,SUNCHANG,UISEONG,SOKCHO' -PsanebCollectionReportLabel=PENDING-FOUR-20260930 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :test --tests '*JindoHeaderCompatibilityTest' --tests '*JeonnamSecondDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JINDO,GOKSEONG' -PsanebCollectionReportLabel=JINDO-HEADER-20260930 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

현재 인벤토리 SHA-256은 `8c63e502b0db2e121dd65955708cd0b182638de88ec39f363fd426f3c47e2496`다. 변경 전 JSON은 `build/reports/attachment-target-inventory/before-jindo-20260930.json`에 보존했고 JSON 내용 동일성을 대조했다. 보관본은 텍스트 개행 정규화 때문에 SHA가 `f0b2ec6103b0ade2479594f4a8b0e2f75481d1e8d60e08c54deea18f419683cd`이며 원본 SHA `8186842d80c326c7d7c215e59724c469c07ace7a66ab1ba9fd4e4b14403c6a28`과 구분한다. 각 영수증의 과거 profile/producer/hash는 치환하지 않는다.

## 남은22개와 다음 대상

미연결 울산남구1개, 등록 후 다운로드 미확인21개(은평·서대문·송파·부산광역시·연제·검단·성남·평택·포천·이천·동두천·강릉·속초·평창·철원·충북도·영동·천안·공주·의성·성주)다. 과거 성공까지 포함한 미확인 수이며, 이21개 모두의 현재 장애를 이번에 재조회한 것은 아니다. 최신 코드 재검증 잔여196개는 과거 성공174개와 미확인22개로 구분한다.

동두천 공식 목록의 실제 검색 필드 `searchCnd=SJ`, `searchKrwd`로45339(2026년 동두천시 청년구직비용 패키지 지원사업 공고)를 확인했다. 공식 상세에서HWPX1개 링크를 확인했으나 다운로드는 아직 수행하지 않았다. 기존44784 제목 중단 기록은 유지하며, 새 표본도 제목 조건과 공식 공고 식별을 통과한 뒤 수집해야 한다. 게시기간 종료 파일 접근 제한을 우회하지 않는다.

작업 브랜치의 `[skip deploy]` 커밋·푸시 범위다. 운영 DB·설정·worker·정책·ENFORCE·재분류·배포와 AWS 확인은 수행하지 않았다. 브라우저 검증은 현재 명시 요청이 없어 정책상 미실행이다. 전체 goal은 계속 미완료다.
