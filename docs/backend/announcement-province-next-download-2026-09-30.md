# 경남도·충남도 본문·첨부 연결 및 부분 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일을 보존하고 개별 오류는 분리한다. HWP 추출기 추가 개선은 보류하며 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 경남도·충남도 공식 상세/첨부 영역·고정 메뉴/공고 identity 계약 구현.
- [x] 정상 파일 보존, 미지원 형식·미해석 링크·개별 HTTP 실패 분리.
- [x] 관련 Java 344개·bootJar·Node 23개 검사 통과.
- [x] 두 지역 본문과 HWPX 각 1개, 합계 197,992byte 실제 다운로드·파일 형식 검증.
- [!] 충남도 첨부 2개 중 첫 파일 HTTP 400. 두 번째 정상 파일은 보존하고 전체 세트 완료로 표시하지 않음.
- [x] 임시 원본 정리, 영수증 318건·최신 255공고 대장 재현.
- [ ] 첨부 텍스트 추출·전체 분석·상시 유입·운영 E2E는 별도 후속.

## 구현 범위

### 경남도

`LGS-000223 / SAEOL_GOSI`, V61 등록 목록은 `https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009000`이다. 공식 목록에서 연결한 하위 메뉴의 [소상공인 정책자금 변경공고 54749](https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009001&mode=view&sno=54749&gosiGbn=A)를 고정 표본으로 사용했다.

- `GyeongnamProvinceNoticePage`: 고정 하위 메뉴·`mode=view`·숫자 `sno`·관측한 공고 구분 `gosiGbn=A`를 검증하고 검색/페이지 필드는 요청에서 제외한다. 다른 구분의 연결을 검증했다고 주장하지 않는다.
- 유일한 `div.basicView.view-v2` 안의 `titleField > h4`, `conText`, `conField`의 `첨부파일` 레이블 셀을 분리한다. 담당자·전화번호·파일명을 본문으로 혼합하지 않는다.
- `GyeongnamProvinceAttachmentDiscoveryProfile / LOCAL_GYEONGNAM_PROVINCE_V1`: 공식 첨부 span의 `a.file`만 읽는다. 같은 호스트 `/index.gyeong?contentsSid=3651&fn=...`로만 GET하며 안전한 파일명과 화면 표시명을 대조한다. 감사 locator에는 공고번호와 파일명 해시를 결합한다. MIME/헤더 예외는 없다.

### 충남도

`LGS-000147 / SPRING_BBS`, 등록 목록은 `https://www.chungnam.go.kr/cnportal/province/province/list.do?menuNo=500487`이다. 실제 GET 검색 폼의 `searchCnd=1`, `searchWrd=소상공인`으로 [소상공인 화재보험료 지원사업 수정 공고 2182547](https://www.chungnam.go.kr/cnportal/province/province/view.do?nttId=2182547&menuNo=500487)를 확보했다.

- `ChungnamProvinceNoticePage`: 고정 메뉴·숫자 `nttId`와 `board-view`의 제목/본문/첨부파일 셀을 검증한다.
- `ChungnamProvinceAttachmentDiscoveryProfile / LOCAL_CHUNGNAM_PROVINCE_V1`: `ul.view-file-list > li`의 파일명과 `a.ico_file`을 연결한다. `minwon.chungnam.go.kr/citynet/jsp/cmm/attach/download.jsp`의 `mode/fid/index/other`만 허용하고 불투명 식별값은 해시로 기록한다.
- 정확히 같은 URL·파일명에 결합된 `previewEncodingUrlAjax`와 `preListenEncodingUrlAjax`는 실행하지 않는다. 불일치 시 정상 파일을 폐기하지 않고 미해석 오류를 유지한다.
- 실제 두 파일에서 관측한 `application/file`·EUC-KR 파일명 응답에 기존 `CitynetAttachmentFileResponse`를 재사용했다. 이 공유 클래스와 기존 기관 프로필은 변경하지 않았고 공통 MIME 허용 목록도 확장하지 않았다.

두 프로필은 최대 10파일·UNKNOWN 역할·PDF/HWP/HWPX 다운로드·동일 요청 redirect 제한을 유지한다. 카탈로그는 `expectation=null` 참조용이다. migration·DB/API·분류 규칙·추출기·운영 설정·기존 데이터는 변경하지 않았다.

## 실행 명령 / 결과

`gradlew.bat --no-daemon :test`에서 ProvinceNext 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 검사를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=PROVINCE_NEXT -PsanebCollectionWindowsTrust=true`를 실행했다.

- Gradle 6분 36초·종료 0, 선택 Java 344개 통과·실패/오류/생략 0, bootJar 성공. 전체 테스트나 운영 worker E2E를 의미하지 않는다.

| 실제 관측(KST) | 본문 | 파일 결과 |
|---|---|---|
| 2026-09-30 16:05:35 경남도 54749 | AVAILABLE/ACCEPTED 359자 | HWPX 1개 101,090byte 성공 |
| 2026-09-30 16:06:03 충남도 2182547 | AVAILABLE/ACCEPTED 858자 | 첫 파일 HTTP 400, 두 번째 HWPX 96,902byte 성공 |

- 경남도 파일 SHA-256: `4ebc9fbcb97c9654a717df8fe70c99a4877af34641d392681947bdfa197e4ba0`.
- 충남도 성공 파일 SHA-256: `5062e3399ec51ef29cdb7976fcb2946740042591bfeb2a09a4d01388c5e65749`.
- 프로필 지문: 경남도 `fc085c7c46f5d1f2de19676d6ccffd134fdd835bd4faa0d43e3e2d283d3ec128`, 충남도 `ba023595785f3630268551f8de8facffc3619aed6804327fd49e9e44b61a2d72`.
- 경남도 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 충남도 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 충남도는 발견 complete/2개이나 수집 complete=false이며 첫 파일의 `FILE_DOWNLOAD / ATTACHMENT_HTTP_400`를 남겼다. 사전 .NET 선두 확인 성공으로 Java 전체 다운로드 실패를 덮어쓰지 않는다. HTTP 400의 정확한 원인은 미확정이며 즉시 반복 요청하지 않았다.
- 예약은 경남도 4/6회·2,711,552byte, 충남도 5/6회·2,415,238byte이며 각 byte 상한은 24,117,248이다. 총 9예약·5,126,790byte, 원본 정리·운영 쓰기 0이다.
- Node 23개·영수증 318건/최신 255공고 재현·수집 가능 집계 통과. 기존 타 지역 상태/근거/지문을 보존했다.
- **177/223수집원(79.4%) 다운로드 확인·46잔여(미연결18+등록 다운로드 미확인28)**다. 부분 성공인 충남도를 포함하는 ‘파일 1개 이상 수집’ 지표이며 모든 첨부 처리 완료가 아니다. 오류가 있는 지역은 45개로 성공 지역과 겹친다. 프로필 206개(지역205+기업마당1), 카탈로그 264공고/205대상/참조263+기존 기대값1이다. 엄격한 전체 세트 Gate 16/223은 그대로다.

## 조사·다음 대상

별도 구조 조사 20GET: 경남도 상세/레이블 2회, 충남도 목록/폼/검색/상세 및 두 파일 선두 8회, 동작 목록 1회, 부평 목록/폼/검색 4회, 옹진 목록/폼/검색/상세 5회다. 요청당 12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 조사 원문 파일은 저장하지 않았고 Java 관측 예산과 별도다.

- 동작 `LGS-000021`, 부평 `LGS-000060`, 옹진 `LGS-000065` 공식 목록은 HTTP 200이다. 과거 접근 실패를 현재 장애로 단정하지 않는다. 동작은 포털 목록 응답만 확인했으며 실제 새올 backend 성공은 아직 미검증이다.
- 부평/옹진 공식 검색 폼은 `eminwonAnnounceList.do?keyfield=title&keyword=...`다. 부평 소상공인 검색에서 이번에 지원 표본을 확보하지 못했다.
- 옹진은 [2026년 소상공인 경영환경개선사업 공고 36423](https://www.ongjin.go.kr/open_content/main/eminwon/eminwonAnnounceDetail.do?mgt_no=36423)와 HWP 1개 링크를 확인했다. 본문 `board_view`, 파일은 `eminwon.ongjin.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 인코딩된 세 인자 방식이다. 다운로드는 미실행이며 기존 IncheonPortal 계열과의 계약 비교가 다음 작업이다. 불투명 원문 식별값은 문서/대장에 복사하지 않는다.

경남도 페이지는 공공누리 제1유형, 충남도 페이지는 제4유형 표시를 관측했다. 운영 재사용 조건은 별도 검토하며 다운로드 검증을 이용 허가로 취급하지 않는다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. 직전 거제 SHA `62f237c`의 Actions `36680260574`는 조회 시 실행 중이었다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다. 이번 단발 Node·Gradle 프로세스는 종료했고 사용자 프로세스는 보존했다.
