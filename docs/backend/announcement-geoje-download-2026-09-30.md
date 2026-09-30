# 거제 본문·새올 LGA 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 보존하고 개별 발견·다운로드·파싱 실패는 별도로 관리한다. HWP 추출기 추가 개선은 보류하며 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 거제 공식 고시공고 상세와 다운로드 함수의 실제 목적지 확인.
- [x] 본문·메타데이터·첨부·미리보기 iframe 분리와 정상 파일 보존 구현.
- [x] 관련 Java 332개·bootJar·Node 23개 검사 통과.
- [x] 본문 248자·HWP 1개 121,856byte 실제 다운로드·파일 형식 검증과 임시 원본 정리.
- [x] 영수증 316건·최신 253공고 대장 재현.
- [ ] 첨부 텍스트 추출·전체 분석·목록부터 상시 유입·운영 E2E는 별도 후속.

## 구현 범위

수집원은 `LGS-000230 / SAEOL_GOSI`, V57 등록 목록은 `https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1`이다. 고정 표본은 [2026년 소상공인 디지털 인프라 지원사업 계획 공고 67219](https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&m=D&idx=67219)다.

- `GeojeNoticePage`: HTTPS·정확한 호스트와 경로·고정 `menuCd`·`m=D`·숫자 `idx`를 검증한다. 기존 원문 identity는 보존하되 상세 요청에서는 공식 목록의 검색/페이지 필드를 제거한다.
- `div.board-view > div.view01`의 유일한 제목 `div.title`, 본문 `div.substan`, 첨부 `div.attach`를 분리한다. 담당부서·첨부 파일명·본문 밖 미리보기 iframe을 본문으로 사용하지 않는다.
- `GeojeAttachmentDiscoveryProfile / LOCAL_GEOJE_GET_V1`: 실제 공식 `goDownLoad` 함수가 가리킨 `https://eminwon.geoje.go.kr/emwp/jsp/lga/homepage/FileDown.jsp`만 연결한다. 세 문자열 인자를 기존 선형 파서로 읽고 JavaScript를 실행하지 않는다. 공유 새올 검증기의 파일 경로/파일명 검사만 재사용하며 가상 `/ofr/FileDown.jsp`로 HTTP 요청하지 않는다.
- 공식 첨부 영역의 표시명과 인자를 대조한다. 같은 공고/순서/확장자의 `/synap/skin/doc.html` 미리보기는 요청하지 않는다. 확인되지 않은 미리보기·링크는 오류로 기록하고 정상 파일은 계속 수집한다.
- 최대 10파일·UNKNOWN 역할, PDF/HWP/HWPX만 다운로드, GET·정확한 호스트·동일 요청 redirect 제한을 유지한다. MIME·헤더 예외는 추가하지 않았다. 파일명은 공식 첨부 표시명을 사용하며 서버의 구형 파일명 헤더를 UI 원문으로 저장하지 않는다.
- 카탈로그는 `expectation=null` 참조 자료다. migration·DB/API·규칙·추출기·운영 설정·기존 데이터는 변경하지 않았다.

## 실행 명령과 결과

`gradlew.bat --no-daemon :test`에서 거제 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 검사를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GEOJE -PsanebCollectionWindowsTrust=true`를 실행했다.

- Gradle 5분 33초·종료 0. 선택 Java 332개 통과·실패/오류/생략 0, bootJar 성공. 전체 테스트 스위트나 운영 worker 검증을 의미하지 않는다.
- 2026-09-30 15:45:59 KST: 제목 조합 통과, 본문 248자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWP 1개 다운로드·파일 형식 검증 성공.
- 파일: 121,856byte, SHA-256 `84b63a4f066fa43c6e8db2f7cd3ad64dd43b818af7093e3aba462154dd0f8a05`.
- 프로필 지문: `7ac467ba2d9477f1114233cdb466e0e90eeb7fecdf1ab4cbe07a575e8b355e8b`.
- 요청 예약 상한 포함 4/6회·2,440,192/24,117,248byte. 관측 임시 원본 정리·운영 쓰기 0.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 정책 QA·텍스트 추출·전체 분석·기대값 승인은 미실행이다.
- Node 23개, `verify-collection-receipt-index.mjs`의 영수증 316건/최신 253공고 재현, `report-collection-availability.mjs` 통과. 기존 타 지역 근거·상태·프로필 지문을 보존했다.
- **175/223수집원(78.5%) 확인·48잔여(미연결20+등록 다운로드 미확인28)**다. 프로필 204개(지역203+기업마당1), 카탈로그 262공고/203대상/참조261+기존 기대값1이다. 전체 첨부 세트 엄격 Gate 16/223은 그대로다. 분모는 9월 28일 수집원 목록이며 고유 지자체 수와 같다고 단정하지 않는다.

## 조사와 다음 연결

별도 구조 조사 12요청(10GET·2POST): 거제 상세/함수·파일 선두/헤더 6GET, 경남도 목록·상세·파일 선두 3GET 및 공식 검색 폼 2POST, 충남도 목록/폼 1GET이다. POST는 공개 검색 조회이며 데이터 저장 요청이 아니다. 요청당 12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 조사 원문 파일은 저장하지 않았다. Java 관측 예산은 별도다.

경남도 `LGS-000223 / SAEOL_GOSI`의 등록 목록은 `https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009000`이다. 공식 목록은 하위 메뉴 `DOM_000000135003009001`의 `mode=view&sno=...&gosiGbn=A` 상세로 연결한다. 검색 폼 `searchfrm`의 실제 POST action과 `conTitle`, `conIfmStdt`, `conIfmEnddt` 등 필드를 사용했다. 기본 3개월 검색에서 지원 표본이 부족해 같은 폼의 시작일을 2026-01-01로 지정했고 [소상공인 정책자금 운용계획 변경공고 54749](https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009001&mode=view&sno=54749&gosiGbn=A)를 확보했다.

경남도 상세는 `div.basicView.view-v2` 구조다. 공식 `a.file`의 `/index.gyeong?contentsSid=3651&fn=공고문(26.6.25.).hwpx`는 HTTP 200, `application/octet-stream; charset=UTF-8`, ZIP 선두 바이트를 반환했다. 전체 다운로드·형식 검증·공고-파일 identity 계약 구현 전이며 성공 지역에 포함하지 않는다. 다음 작업은 경남도 연결이다. 충남도 `LGS-000147`도 정상 목록과 GET 검색 폼까지 확인했지만 상세/파일은 아직 미검증이다.

거제의 공공누리 제4유형 표시는 운영 재사용 검토 후속이다. 다운로드 검증이 이용 허가나 상업 재사용 승인을 뜻하지 않는다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. 직전 의령 SHA `5f802bc`의 GitHub Actions `36678832921`은 조회 시 실행 중이었으며 CI 통과로 보고하지 않는다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다. 이번 단발 Node·Gradle 자원은 종료했으며 기존 사용자 프로세스는 유지한다.
