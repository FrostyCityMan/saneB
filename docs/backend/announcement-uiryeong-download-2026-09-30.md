# 의령 본문·새올 GET 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 수집하고 발견·다운로드·파싱 실패를 별도로 보존한다. HWP 추출기 추가 개선은 보류하며 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 의령 공식 상세와 직접 파일 경로, 공고 식별 계약 구현.
- [x] 본문·메타데이터·첨부 영역 분리, 같은 파일 미리보기 비실행.
- [x] 정상 파일 보존·오류 분리, 관측된 query 시작 탭 한 개만 정규화.
- [x] 관련 Java 331개·bootJar·Node 23개 검사 통과.
- [x] 본문 222자·HWP 1개 106,496byte 실제 다운로드·파일 형식 검증과 임시 원본 정리.
- [x] 영수증 315건·최신 252공고 대장 재현.
- [ ] 첨부 텍스트 추출·전체 분석·목록부터 상시 유입·운영 E2E는 별도 후속.

## 구현 근거 및 범위

수집원은 `LGS-000232 / HEURISTIC_NOTICE`, 등록 목록은 `https://www.uiryeong.go.kr/index.uiryeong?menuCd=DOM_000000203003000000`이다. 고정 표본은 [2026년 의령군 소상공인 육성지원 사업 공고 35341](https://www.uiryeong.go.kr/board/view.uiryeong?boardId=BBS_0000070&menuCd=DOM_000000203003001001&startPage=1&dataSid=316445&gosiNo=35341)이다.

- `UiryeongNoticePage`: HTTPS·정확한 호스트/상세 경로·게시판/메뉴를 고정하고 `dataSid`와 실제 공고번호 `gosiNo`를 모두 유지한다. 같은 `dataSid`를 공유하는 서로 다른 `gosiNo`의 source hash와 첨부 locator가 구분되는지 검사했다.
- 유일한 `div.boardViewWrap` 안의 `p.bdvTit`, `div.bdvCntWrap`, `dl.fileBox`의 `첨부` 셀을 분리한다. 메타데이터·파일명·스크립트를 본문 판정에 혼합하지 않는다.
- `UiryeongAttachmentDiscoveryProfile / LOCAL_UIRYEONG_GET_V1`: 공식 `ul.fileBoxs > li.gosiWrap`의 `eminwon.uiryeong.go.kr/emwp/jsp/ofr/FileDown.jsp` 직접 GET만 연결한다. 파일 경로·시스템 파일명·표시 파일명을 검증한다. 실제 관측된 `?` 다음 탭 한 개만 정리하고 다른 탭·제어문자·외부 호스트·추가 query·POST·다른 요청으로의 redirect는 허용하지 않는다.
- `/customuser/synap.uiryeong` 미리보기의 이중 URL 인코딩 인자를 원래 파일의 세 값과 대조하되 미리보기는 요청하지 않는다. 잘못된 미리보기와 미해석 링크는 오류로 남기고 독립적으로 확인한 정상 다운로드는 유지한다.
- 최대 10파일·UNKNOWN 역할을 유지하고 PDF/HWP/HWPX 외 형식은 다운로드하지 않는다. MIME/파일명 헤더 예외는 추가하지 않았다.
- 고정 표본은 카탈로그 참조용 `expectation=null`이다. migration·DB/API·키워드·추출기·운영 설정·기존 데이터는 변경하지 않았다. 목록부터 상시 worker로 전달되는 운영 흐름은 이번 표본 관측만으로 확인되지 않는다.

## 검증 결과

실행 명령: `gradlew.bat --no-daemon :test`에서 의령 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 검사를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=UIRYEONG -PsanebCollectionWindowsTrust=true`를 함께 실행했다.

- Gradle 5분 9초·종료 0. 선택 Java 331개 통과·실패/오류/생략 0, bootJar 성공. 전체 테스트 스위트를 실행했다는 의미는 아니다.
- 2026-09-30 15:29:27 KST: 제목 조합 통과, 본문 222자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWP 1개 다운로드·파일 형식 검증 성공.
- 파일: 106,496byte, SHA-256 `92d6c39379624e27838003fdf0fc88b696565a09432334542b8977f86e922baa`.
- 프로필 지문: `281fd844d4d1c948623b6fb10abbc9cb5de7adc908ff546d7beef2563e0ba7c0`.
- 요청 예약 상한 포함 4/6회·2,359,296/24,117,248byte. 관측 임시 원본 정리·운영 쓰기 0.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 정책 QA·텍스트 추출·전체 분석·기대값 승인은 미실행이다.
- Node 23개 검사, `verify-collection-receipt-index.mjs`의 영수증 315건·최신 252공고 재현과 `report-collection-availability.mjs` 집계 통과. 기존 타 지역 상태·근거·프로필 지문을 보존했다.
- **174/223수집원(78.0%) 확인·49잔여(미연결21+등록 다운로드 미확인28)**다. 프로필 203개(지역202+기업마당1), 카탈로그 261공고/202대상/참조260+기존 기대값1이다. 전체 첨부 세트 엄격 Gate 16/223은 그대로다. 분모는 9월 28일 수집원 목록 기준이며 고유 지자체 수와 동일하다고 단정하지 않는다.

## 조사 요청과 다음 연결

별도 구조 조사 9GET: 의령 상세·파일 선두 확인 2회, 거제 등록 목록/폼 확인·검색·상세 5회, 경남도와 충남도 등록 목록 각 1회다. 요청당 12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했고 조사 원문은 파일로 저장하지 않았다. Java 관측 예산과 별도다.

- 거제 `LGS-000230 / SAEOL_GOSI`: V57 등록 주소 `https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1`이 HTTP 200으로 응답했다. 실제 공식 GET 검색 폼의 `searchType=NOT_ANCMT_SJ`, `keyword=소상공인`으로 [2026년 소상공인 디지털 인프라 지원사업 계획 공고 67219](https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&m=D&idx=67219)를 확인했다. `board-view / view01` 구조와 `goDownLoad(표시명,시스템명,경로)` HWP 1개, 별도 `/synap/skin/doc.html` 미리보기를 관측했다. 다운로드 함수의 실제 목적지 확인과 기관별 계약 구현이 다음 작업이며 아직 파일 수집 성공으로 집계하지 않는다.
- 경남도 `LGS-000223`과 충남도 `LGS-000147`의 V61 등록 목록도 HTTP 200·정상 제목을 반환했다. 과거 접근 오류를 현재 확정 장애로 재사용하지 않는다. 상세·파일은 아직 미검증이다.
- 의령·거제 페이지의 공공누리 제4유형 표시는 운영 재사용 검토 후속이다. 다운로드 검증을 이용 허가나 상업 재사용 승인으로 해석하지 않는다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신 및 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다. 이번 검증에 사용한 단발 Node·Gradle 자원은 종료했으며 기존 사용자 프로세스는 유지한다.
