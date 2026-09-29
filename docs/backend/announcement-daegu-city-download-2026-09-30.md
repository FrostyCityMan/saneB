# 대구광역시 본문·첨부와 구형 MIME 호환

## 현재 단계 / Gate

전 지역에서 수집 가능한 첨부를 먼저 확보하고 발견·다운로드·추출 실패는 별도 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 1.0.16 추가 개선은 이번 범위가 아니다.

- [x] 대구광역시 LGS-000044 / SAFE_DAEGU_LEGAL_NOTICE 본문·첨부 연결.
- [x] 실제 본문130자·HWP1개37,376byte 다운로드와 기본 형식 검증.
- [x] 최초 MIME 실패 기록 보존, 대구시 한정 기존 호환 옵션 적용.
- [x] 수정 후 표적114건·확대228건 통과, Node23/23, bootJar 성공.
- [x] 265영수증/219공고 재현, 기존 근거 보존, 원본·실행 자원 정리.
- [ ] 남은84지역: 미등록48 + 등록 후 다운로드 성공 미확인36.
- [ ] 첨부 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E의 전체 Gate.

## 공식 조사와 계약

목록 `https://www.daegu.go.kr/index.do?menu_id=00940170`의 실제 폼 `sidoGosiAPIVO`에서 POST 제목 검색 필드 `searchTitle`과 `pageIndex`를 확인했다. 소상공인 제목 검색에서 과거 지원금 공고를 선정했다.

- 표본: `DAEGU_CITY-33505`
- 제목: 「소기업·소상공인 방역물품 지원금」 시행 연장 공고
- 상세: `https://www.daegu.go.kr/index.do?menu_id=00940170&menu_link=/front/daeguSidoGosi/daeguSidoGosiView.do&sno=33505&gosi_gbn=A`
- 2022년 과거 공고다. 현재 신청 가능성이나 지원 자격을 확인한 것은 아니다.

공식 화면의 `fn_goLinkView`는 상세 폼을 POST하지만, 기존 목록 파서가 생성하는 위 상세 GET의 HTTP200도 실제 확인했다. 이는 운영 스케줄러가 현재 자동 수집한다는 증거는 아니다.

`DaeguCityNoticePage`는 HTTPS 고정 호스트·상세 경로·메뉴00940170·명시된 menu_link·숫자 공고 번호·고시 구분과 정확한4개 query를 검증한다. 단일 `form#sidoGosiAPIVO > div#bbsView`에서 숨김 sno/gosi_gbn을 상세 URL과 대조한다. `dl.title`, `dl.content`, `dl.attfile`의 정확한 label과 dd를 선택해 제목·본문·첨부를 분리한다. 담당부서·연락처·메뉴는 본문에 포함하지 않는다.

`DaeguCityAttachmentDiscoveryProfile`은 공식 첨부 셀의 `span.attfile > a.download`만 읽고, `fn_egov_downFile`의 제한된 파일 그룹·숫자 순번을 HTTPS 동일 호스트 `/icms/cmm/fms/FileDown.do`의 GET으로 변환한다. 공식 링크의 파일 그룹 끝 공백은 요청에 보존하고 숨김 그룹과 비교할 때만 정리한다. 추가 JavaScript·임의 호스트·POST·redirect·미리보기 다운로드는 허용하지 않는다.

명시된 fileListCnt와 실제 표시 항목 수를 대조한다. 미리보기는 같은 파일 그룹·순번일 때만 보조 링크로 구분하고 실행하지 않는다. 첨부 셀의 인라인 함수 정의도 실행하지 않으며, 누락된 개수·불일치·미해결 링크는 오류로 남긴다. 정상 파일 descriptor는 보존한다. 최대10파일·미지원 형식 구분·UNKNOWN 문서 역할을 유지한다.

본문 client·Spring 프로필 등록·QA 실행기·카탈로그에 연결했다. 신규 expectation은 null이다. 기존 프로필/공통 검증기/추출기 지문은 수정하지 않았고 DB migration·API·UI 계약 변경은 없다.

## 실패와 복구 근거

첫 관측(2026-09-30 05:45 KST, UTC `2026-09-29T20:45:59.364617500Z`)은 본문 AVAILABLE130자, FOUND complete=true였지만 파일 검증이 `FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패했다. 수신37,376byte를 성공으로 집계하지 않았다. 초기 프로필 지문은 `eae62e8011423fdb6d0cba55339a5191117764f9e5eb7f853d990b5d40bd063d`다.

최초 보고서는 `build/reports/attachment-regional-collection/DAEGU_CITY-33505-INITIAL.json`으로 보존했다. HEAD1회는405였고, 제한된 진단 GET1회에서 HTTP200·`application/x-msdownload`·attachment disposition·UTF-8 파일명 octet·동일37,376byte 해시를 확인했다. 실제 파일 prefix도 OLE 시그니처였다.

대구시 프로필에만 기존 `selectLegacyBinaryContentTypes`의 `application/x-msdownload`와 `selectUtf8DispositionOctets=true`를 적용했다. 공통 검증기의 허용 범위는 변경하지 않았다. 예상 형식·바이너리 시그니처·attachment disposition·파일명/확장자 일치 검증은 유지한다. HTML 오류 응답, 확장자 불일치, 경로가 포함된 파일명, inline disposition은 테스트로 거부를 확인했다.

수정 후 실제 재관측은 2026-09-30 05:50 KST(UTC `2026-09-29T20:50:39.192105900Z`)에 성공했다.

- 최종 프로필: `LOCAL_DAEGU_CITY_GOSI_V1`
- 최종 지문: `b0e7be108b67440356780e5c1927ae5691240001e9e20558db5c03c7a72adfbb`
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE130자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true, HWP1개37,376byte DOWNLOADED.
- 파일 SHA256: `ddcf9f1032c5e5dc07751c1411b36ca60c10fc53cebdcd3ab78f765f57cce379`.
- 결과 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

기본 형식 확인은 HWP 내부 파싱·텍스트 추출 성공이 아니다. 본문 ACCEPTED는 관리자 최종 확정이 아니다.

## 집계·검증·정리

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이며 이번 운영 재조회는 없다. 최소1파일 다운로드 관측은138→139지역(62.3%), 잔여85→84(미등록49→48, 등록 후 미확인36 유지)다. 첨부 오류가 남은43지역은 성공 지역과 중복될 수 있다. 기존3표본·전체 파일 Gate는16충족/207잔여로 유지한다.

지역175+기업마당1=176프로필, 카탈로그231공고/174대상, 영수증265개/최신219공고다. 같은 대구시 공고의 실패/복구2영수증은1공고로 집계한다. 다운로드 확인율은 전체 구현·운영 완료율이 아니다.

```powershell
# 대구시 실제 HTML/진단파일 fixture와 기존 운영 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*DaeguCityDownloadContractTest' --tests '*AttachmentDownloadInvocationTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DAEGU_CITY -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적113건 통과(36초), 확대218건 통과·bootJar 성공이나 실제 파일 MIME 실패(1분58초). 호환 수정 후 실제 수신 파일 검사를 포함한 표적114건 통과(26초), 확대228건 모두 통과·bootJar·실제 관측 성공(1분59초). Node23/23과265영수증/219공고 재현 통과다. 원본 정리 후 fixture 환경변수 없이 실행하면 실제 HTML/진단파일 검사1건은 조건부 생략된다.

조사5요청은 목록·검색·상세 각1, HEAD405 진단1, 파일 GET 진단1이다. 각15초·HTML2MiB, 진단파일1MiB로 제한했다. 실제 관측은2회, 각 최대6요청/23MiB·상세1MiB다. 본문 포함 관측 예약은각4회, 합계8회·4,695,040byte이며 조사 포함 누적 예약 상한13회다. 예약 수를 실제 wire 요청 수로 단정하지 않는다. 최초 실패와 재시도는 모두 보존하며 예산을 초기화하지 않았다.

조사 HTML·헤더·진단 HWP7개673,766byte는 절대 경로·크기·SHA256 대조 후 삭제했다. 실제 관측 첨부 원본은 실행기가 정리했다. 보고서·해시는 보존하며 원본 복구는 공식 사이트 재조회가 필요하다. 임시 편집 도구와 이번 Node·Gradle·Java 자원도 정리했고 기존 다른 작업 프로세스는 건드리지 않았다.

전체 프로젝트 테스트·HWP 텍스트 추출·AWS·운영 DB·운영 상시 유입·운영 E2E는 이번 회차 미실행이다. 브라우저는 현재 요청에 명시되지 않아 정책상 생략했다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다.
