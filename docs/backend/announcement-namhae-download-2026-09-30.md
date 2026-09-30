# 남해 고정 상세 주소·본문·첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일은 수집하고 개별 실패는 별도로 보존한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 남해 공식 상세·직접 파일 경로와 고정 URL 계약 구현.
- [x] 목록 링크의 일회성 값 제거 후 rawLink·저장 payload·공고 hash 안정성 검사.
- [x] 본문 영역 분리와 SCMS 첨부 엔진 연결·요청 경계·오류 분리 회귀 검사.
- [x] Java324개·bootJar·Node23개 검사 통과.
- [x] 본문286자·HWPX1개102,547byte 실제 수집·형식 검증과 임시 원본 정리.
- [x] 영수증313건·최신250공고 대장 재현.
- [ ] 첨부 텍스트 추출·전체 분석·운영 상시 수집·운영 E2E는 별도 후속.

## 구현 근거 및 범위

수집원은 `LGS-000236 / SCMS_CARD_NOTICE`, 등록 목록은 `https://www.namhae.go.kr/socialm/Index.do?c=SM010110000`이다. 고정 표본은 [2026년 남해군 소상공인 임대료 지원사업 공고35694](https://www.namhae.go.kr/modules/saeol/gosi.do?amode=_view&not_ancmt_mgt_no=35694&scd=01&pageCd=SM010110000&siteGubun=socialm)다. 접수 종료된 공고의 파일 접근 검증이며 현재 신청 가능한 공고로 승인하지 않는다.

- `NamhaeNoticePage`: 정확한 HTTPS 호스트·모듈 경로, `_view`, 숫자 공고번호, `scd=01/pageCd=SM010110000/siteGubun=socialm`을 검증한다. 목록의 페이지·검색 조건과 관측된 일회성 두 필드는 저장 전 제거한다. 알 수 없는 필드·중복 필드·다른 게시판·fragment 등은 거부한다.
- `LocalGovernmentNoticeCollector`: 남해 모듈의 링크에만 위 변환을 적용한다. 원본 href의 일회성 값이 남지 않도록 rawLink와 absoluteLink 모두 고정 주소로 만든 뒤 기존 canonical/hash/payload 과정을 적용한다. 다른 지역 URL·공통 identity normalizer는 변경하지 않는다.
- `NamhaeAttachmentDiscoveryProfile`: 저장 source의 provider·기관·parser·URL hash와 일회성 값 부재를 검증한다. 네트워크에는 공식 `_view` URI만 요청하고, 기존 SCMS 해석기에 전달하는 내부 `amode=view` 표현은 요청하지 않는다. 동일 요청 redirect 제한을 유지한다.
- 공식 `form#saeolGosiVO > div.bbs1view1`의 제목·본문·첨부를 분리한다. `eminwon.namhae.go.kr/emwp/jsp/ofr/FileDown.jsp` GET3필드만 승인하며 기존 파일명/경로 검증·최대10파일·UNKNOWN 역할·정상 파일 보존·오류 분리를 재사용한다.
- 헤더 조사에서 octet-stream·Content-Length102,547·ZIP 선두·파일명 헤더 없음이 확인됐다. MIME 예외나 공통 검증기를 변경하지 않았다.
- 카탈로그는 참조용 expectation null이다. 기존 migration·DB/API·키워드·추출기·운영 설정은 변경하지 않는다. 기존 저장 데이터의 URL 정리·재분류는 이번에 실행하지 않는다.

## 검증 결과

`gradlew.bat --no-daemon :test`의 남해 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=NAMHAE -PsanebCollectionWindowsTrust=true`를 실행했다.

- Gradle 3분44초·종료0, Java324개 통과·실패/오류/생략0, bootJar 성공.
- 2026-09-30 14:56:12 KST: 제목 조합 통과, 본문286자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWPX1개102,547byte 다운로드·형식 검증 성공.
- 파일 SHA-256: `d771f64e215a36acec6cdb2cb70f190e85c24eef65c68960ea817d2ac896500a`.
- 프로필 지문: `d27029a27c738f97d7d78b00b925353fd612a5a6523917c034fdbf32ea3b1126`.
- 요청 예약 상한 포함4/6회·2,240,659/24,117,248byte. 관측 임시 원본 정리·운영 쓰기0.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 정책 QA·추출·전체 분석·기대값 승인은 미실행이다.
- Node23개 검사, `verify-collection-receipt-index.mjs`의 영수증313건·최신250공고 재현과 `report-collection-availability.mjs` 집계 통과. 기존 타 지역 근거·프로필 지문을 보존했다.
- **172/223수집원(77.1%) 확인·51잔여(미연결23+등록 다운로드 미확인28)**다. 프로필201개(지역200+기업마당1), 카탈로그259공고/200대상/참조258+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로다.

## 조사 요청 및 다음 지역

별도 구조 조사8GET: 남해 상세·파일 선두2, 의령 목록1, 산청 목록·검색·폼·상세5회다. 요청당12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했고 원문 파일은 저장하지 않았다. Java 관측 예산은 별도다.

- 의령 `LGS-000232`: 등록 목록 HTTP302, 이번에는 이동을 따라가지 않았다. 실패 원인이나 파일 수집 성공으로 단정하지 않는다.
- 산청 `LGS-000238`: 등록 목록 HTTP200. `searchCnd=all`은 검색어를 반영하지 않았으므로 공식 폼에서 제목 조건 `SJ`를 확인한 뒤 검색했다. [2026년 소상공인 소규모 경영환경 개선지원 사업(3차) 공고164021](https://www.sancheong.go.kr/www/selectBbsNttView.do?key=158&bbsNo=118&nttNo=164021)의 제목·본문/파일 표와 HWPX2개를 확인했다. 직접 다운로드는 `/downloadBbsFile.do?atchmnflNo=171939`, `171940`, 미리보기는 `/previewBbs.do`다. 아직 파일 다운로드 전이다.
- 산청은 관측된 BBS 구조와 기존 등록 parser의 적합성을 후속 확인해야 한다. 과거 HTTP400만으로 현재 접근 불가로 취급하지 않으며, 목록 HTTP200만으로 상시 수집 성공을 주장하지 않는다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다.
