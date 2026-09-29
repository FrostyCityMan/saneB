# 울산시 첨부 연결 및 울산권 수집 오류 분리

## 현재 단계 / Gate

전 지역에서 가능한 첨부를 먼저 수집하고 발견·다운로드·추출 실패를 별도로 기록한다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기1.0.16 개선은 이번 범위가 아니다.

- [x] 울산시 LGS-000077 / SAEOL_GOSI 본문·시티넷 첨부 발견 연결.
- [x] 공식 본문119자·HWPX로 표시된 첨부1개 발견,70,212byte 수신.
- [!] FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH. 다운로드 성공으로 집계하지 않음.
- [!] 남구 LGS-000079: 공식 메뉴200, 연결된 새올 iframe 응답 시간 초과·0byte.
- [!] 동구 LGS-000080: 공식 메뉴200, 연결된 새올 iframe은200이지만 오류 페이지.
- [x] 수정 후 Java224통과·조건부1생략, bootJar·Node23/23·269영수증/223공고 재현.
- [x] 조사 원본15개968,129byte와 실행 자원 정리. 기존 근거·다른 지역 보존.
- [ ] 잔여83개 수집원: 미등록44 + 등록 후 다운로드 성공 미확인39.
- [ ] 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 공식 조사와 연결

울산시 등록 목록 `https://www.ulsan.go.kr/u/rep/contents.ulsan?mId=001004002000000000`은 같은 호스트의 `/u/rep/transfer/notice/list.ulsan`으로302를 반환했다. Location을 검증한 뒤 목록200, 실제 검색 폼의 `srchType=srchSj`, `srchWord=소상공인`, `srchGubun=` POST200, 상세200을 확인했다. 별도 인증·세션·토큰은 주입하지 않았다.

- 표본 `ULSAN_CITY-47059`: 2026년 4차 소상공인 경영안정자금 융자지원계획 공고.
- 상세 `https://www.ulsan.go.kr/u/rep/transfer/notice/47059.ulsan?mId=001004002000000000&gosiGbn=A`.
- 공식 `#contents_inner table.tbl_bd_view`에서 제목·첨부파일 label의 다음 셀과 ‘내용’ header 다음 행의 본문을 분리한다. 담당자·연락처·담당부서·파일명·메뉴는 본문 분류에 넣지 않는다.
- 첨부는 `https://minwon.ulsan.go.kr/citynet/jsp/cmm/attach/download.jsp`의 고정4개 query(mode/fid/index/other)를 사용한다. 불투명 식별값은 요청 메모리에서만 사용하며 locator에는 해시를 남긴다.

`UlsanCityAttachmentDiscoveryProfile`은 울산시 source/parser, 상세의 숫자 경로·menu·고시 구분, 파일 서버·경로·query를 고정한다. HTTPS·기본443·GET·동일 요청만 허용하며 POST·리다이렉트·임의 링크를 거절한다. 최대10개·중복/상충·미지원 형식·부분 성공 보존·UNKNOWN 문서 역할을 유지한다.

실제 화면의 `fn_fileNoticePreivew` 버튼은 같은 순번의 바로 앞 공식 첨부 링크와 고정 호출 형식이 일치하는 경우에만 장식으로 제외한다. 미리보기 URL을 요청하거나 JavaScript를 실행하지 않는다. 알려지지 않은 버튼은 오류로 남기고 정상 다운로드 링크는 보존한다. 기존 인천시·공통 파일 검증기·추출기·DB/migration·API/UI·운영 설정은 변경하지 않았다.

남구는 공식 메뉴에서 확인한 iframe을15초 내1회 조회했으나0byte 시간 초과였다. 동구는 공식 iframe의200응답이 정상 목록이 아닌 오류 안내 HTML이었다. 이를 수집 성공·첨부 없음으로 기록하지 않는다. 두 곳 모두 동일 요청 반복·차단 우회 없이 미등록 상태와 조사 오류를 보존했다. 시간 초과로 남구 본문 파일이 생성되지 않아 첫 로컬 HTML 조사 명령은 ENOENT로 끝났으며, 누락 파일을 성공 응답으로 대체하지 않았다.

## 실제 관측

2026-09-30 06:53 KST(UTC `2026-09-29T21:53:28.147427500Z`):

- 프로필 `LOCAL_ULSAN_CITY_CITYNET_V1`, 지문 `49a1e7cce49a42003534da0d59752cfc05b1f5c6c075d31a32be3b62bec15c4a`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE119자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·1개, 파일 FAILED / FILE_SIGNATURE / ATTACHMENT_CONTENT_TYPE_MISMATCH.
- 수신70,212byte, SHA256 `11739be61c8f6aebe974c25ab3ffd84b2409fa3e50adb762310846bba92baa68`.
- 결과 COLLECTION_ONLY_PARTIAL_NOT_APPROVED, 원본 정리true, 운영 쓰기0.

이번 보고서만으로 응답의 정확한 MIME 값·파일명 헤더 charset을 확정하지 않는다. 별도 진단 다운로드는 하지 않았다. 수신량·해시만으로 HWPX 내부 형식 검증이나 텍스트 추출 성공을 선언하지 않는다. 파일 응답 호환은 후속 오류로 분리하고 다른 수집원 연결을 계속한다.

## 집계와 검증

분모는2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역 수집원이다. 이번 운영 재조회는 없다. 최소1파일 다운로드 확인140/223(62.8%)·잔여83은 유지한다. 미등록45→44, 등록 미확인38→39다. 첨부 오류45→46은 성공 수집원과 중복될 수 있으며, 남구·동구 조사 오류는 별도 surveyNotes에 보존한다. 기존3표본·전체 파일 Gate는16충족/207잔여다.

지역179+기업마당1=180프로필, 카탈로그235공고/178대상(234reference-only), 영수증269개/최신223공고다. 공고 수223과 대상 수223은 다른 집계다. 수집원별 최소1표본을 모든 하위 경로·전체 운영 완료율로 표현하지 않는다.

```powershell
# 실제 울산 HTML fixture와 기존 읽기 전용 inventory 환경변수 활성화 후
.\gradlew.bat :test --tests '*UlsanCityDownloadContractTest' --tests '*IncheonCityDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ULSAN_CITY -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초117건 중5건은 파서 코드 불일치로 실패(37초), 다음224건 중2건은 host 없는 URI 처리와 미리보기 버튼 잔여 판정으로 실패·1건 생략(1분48초)했다. 이 두 실행에서는 외부 관측 태스크에 도달하지 않았다. 수정 후225건 중224통과·실패0·조건부1생략, bootJar·관측 태스크 완료(2분5초)다. 생략은 원본이 이미 정리된 인천 실제 HTML 검사이며 울산 실제 HTML은 통과했다. 원본 정리 후 fixture 환경변수 없이 실행하면 울산 실물 검사도 조건부 생략된다. 관측 태스크 성공과 파일 다운로드 성공은 구분한다.

조사8요청(울산시4·남구2·동구2), 각15초/2MiB. 실제 관측1회는 최대6요청/23MiB·상세1MiB, 본문 포함 예약4회/2,323,012byte, 조사 포함 누적 예약 상한12회다. 예약 수와 실제 wire 요청 수·파일 수신량을 동일시하지 않는다. 무조건 반복 재시도는 하지 않았다.

조사 HTML·헤더15개968,129byte를 허용된 절대 경로·크기·SHA256 대조 후 삭제했다. 실제 다운로드 원본은 실행기가 정리했다. 복구에는 공식 사이트 재조회가 필요하며 보고서·해시는 보존한다. 임시 편집 도구와 이번 Node·Gradle·Java 자원은 정리하고 기존 다른 프로세스·미추적 파일은 보존했다.

전체 프로젝트 테스트·첨부 추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 작업 브랜치 `[skip deploy]` 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 하지 않았다.
