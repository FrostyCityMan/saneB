# 성남 수집 처리기 연결과 로컬 TLS 실패 분리

## 단계 / Gate

지역별 첨부 수집 연결 단계다. 성공 파일은 먼저 수집하고 개별 오류는 별도로 남긴다. 성남 처리기는 연결했으나 실제 Java 다운로드는 TLS 실패로 미확인이다. 전체 장기 goal과 운영 완료를 선언하지 않는다.

- [x] 성남 공식 상세 구조 확인 및 기존 새올 GET 엔진 재사용
- [x] 본문·제목·첨부 경계, 수집원 바인딩, 회귀·패키징 검증
- [!] 성남 Java 본문 NETWORK_ERROR / 상세 TLS_FAILED, 파일 0개
- [!] 서울 강서구 공개 목록 15초 시간 초과
- [x] 서울 AWS 인증 및 배포 대상 EC2 읽기 전용 조회
- [ ] 서울 서버 격리 수집 전용 진단
- [ ] 운영 목록 자동 유입·첨부 추출·정책 QA·운영 E2E

## 확인한 공식 경로와 구현

공식 검색 결과에서 성남 새올의 소상공인 특례보증 공고144735를 확인했다. 검색 URL의 조회 옵션을 실행 코드에 복사하지 않고, 기존 새올 파서의 7개 상세 인자로 정규화한 공식 GET을 2회 확인했다. 모두 HTTP200·7,035byte였다.

- [성남시 공식 공고144735](https://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=144735&subCheck=Y)
- 수집원: `LGS-000089 / SAFE_SAEOL_EMINWON`
- 처리기: `LOCAL_SEONGNAM_GET_V1`
- 제목·본문: `form[name=form1][method=post] div.boardWrap > table.bd00view`의 제목 셀과 `td.bd01tdC[colspan=4]`
- 첨부: 정확한 ‘첨부파일’ th의 다음 td에서 `goDownLoad` 3인자를 읽는 기존 `SaeolGetAttachmentDiscoveryProfile`
- 다운로드: 같은 기관 `eminwon.seongnam.go.kr/emwp/jsp/ofr/FileDown.jsp`, HTTPS GET

공통 엔진·URL 검증·TLS·MIME·파일 signature 정책을 변경하지 않았다. 정상 descriptor 보존, 미지원 형식 분리, 10파일 상한, 원문 URL hash·source/parser 결합을 재사용한다. 본문에서는 메뉴·담당자·첨부 파일명을 제외한다.

이번 고정 상세 관측의 목록 힌트는 과거 공식 새올 진입 경로다. V55의 포털 등록 주소 `https://www.seongnam.go.kr/notice/publicNotice.do?menuIdx=1000499&returnURL=/main.do`와 동일한 경로라고 주장하지 않는다. 이전 포털404 문제·현재 운영 목록 연결·자동 유입은 해결 증거가 없으며 등록 주소도 변경하지 않았다.

## 실제 관측과 실패

2026-09-30 11:57:38 KST `SEONGNAM-144735`:

| 단계 | 결과 |
|---|---|
| 제목 | 기존 DRAFT 규칙 COMBINATION_MATCHED |
| Java 본문 | FETCH_FAILED / NETWORK_ERROR, 본문0자 |
| 상세·첨부 발견 | DETAIL_DISCOVERY / TLS_FAILED |
| 파일 | 0개. 첨부 없음이 아니라 발견 단계 미도달 |
| 최종 상태 | INCOMPLETE |
| 요청 예약 | 상한6회·24,117,248byte, 사용3회·2,097,152byte |
| 임시 원본 | 관측 도구 정리 확인 |

PowerShell의 공개 상세 조회 성공과 Java pinned client 실패를 구분한다. TLS 상세 원인은 아직 확정하지 않았다. 인증서 검증 해제·HTTP 강등·무한 재시도는 하지 않았다. 파일 성공·지역 다운로드 성공으로 계수하지 않는다.

보관 근거는 `build/reports/attachment-regional-collection/SEONGNAM-144735.json`이며 source text나 인증값 없이 실패 단계·코드·실행 지문을 수집 대장에 반영했다. 기존296영수증/237공고는 보존하고 297영수증/238공고가 되었다.

## 집계 / 별도 조사

다운로드 확인 **162/223수집원(72.6%), 잔여61개**를 유지한다. 미등록35→34개, 등록 후 다운로드 미확인26→27개다. 지역 처리기189개와 기업마당1개를 합쳐190개, 카탈로그247공고/189대상이다. 신규 기대값은 null이며 기존 기대값1개만 유지한다. 오류 근거가 있는 수집원은40→41개다. 기존3표본·전체 첨부 Gate16충족/207잔여와 다운로드 가용성 집계는 구분한다.

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 고유 행정구역 수나 현재 운영 성공률이 아니다.

공식 검색9개 쿼리와 별개로, 직접 사이트 조사3회(GET3: 성남2성공, 강서1시간 초과)를 수행했다. 이천 공식 검색 결과에는 일반공고 메뉴가 있으나 등록된 고시 메뉴와 별개이므로 범위를 변경하지 않았다. 강서 시간 초과는 재요청하지 않았다. 조사 응답은 메모리에서만 구조를 확인하고 폐기했으며 신규 조사 원문 파일은 없다. 이전 연제·구례 조사 HTML7개 정리 차단은 해결하지 않았고 해당 대장의 `originalsRemoved=false`를 보존했다.

## AWS 읽기 전용 확인

기본 AWS CLI의 로그인 갱신 요청은 CA 신뢰 오류로 실패했다. 저장된 공개 CA bundle이 현재 Windows Root 저장소에 존재하고 유효한 인증서1개임을 대조한 뒤, 해당 명령에서만 `AWS_CA_BUNDLE`을 지정했다. STS 인증과 `SanebDeployTarget=true`/running EC2 조회가 성공했고 서울 `ap-northeast-2d` 대상1개를 확인했다. 인증값·계정 식별 정보는 문서에 복사하지 않는다.

전역 신뢰 저장소·환경변수·운영 설정을 변경하지 않았다. SSM 명령·서버 임시 프로세스·다운로드·DB 조회·배포·health 검증은 이번에 실행하지 않았다. 따라서 이는 다음 격리 QA의 접근 준비 증거이지 서버 수집 성공의 증거가 아니다.

## 실행 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :test --tests '*SeongnamDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest.seongnamReferenceDoesNotApproveExtractionOrRepairOperatingList' --tests '*LocalGovernmentNoticeProviderContentClientTest.seongnamBodyExcludesMetadataAndAttachments'
.\gradlew.bat --no-daemon :test --tests '*SeongnamDownloadContractTest' --tests '*YeonjeGuryeDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEONGNAM' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

신규 계약8개 통과. 확장 Java280개 실패·생략0, bootJar 성공, Node23개 통과. 실제 관측1건은 TLS 실패하여 두 번째 Gradle 명령 전체는 실패(3분5초)다. 실패를 테스트 성공과 섞어 보고하지 않는다. 297영수증/238공고 재현 검증 통과. 관측 원본은 도구에서 정리됐고 이번 Java/Node 프로세스는 종료됐다. 기존 사용자 Java와 output·__pycache__는 보존했다.

DB/migration·API/UI·추출기·운영 정책·worker 설정·ENFORCE·기존 데이터·배포는 변경하지 않았다. 사용자 정책에 따라 브라우저 검증은 미실행이다. 다음은 미등록34개 확장과 별도로 로컬 TLS/접속 실패 대상의 서울 서버 격리 수집 전용 진단 계약을 준비한다.
