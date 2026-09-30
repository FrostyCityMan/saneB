# 거창 본문·직접 첨부 다운로드 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일은 수집하고 개별 실패는 별도로 보존한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 거창 공식 상세·직접 파일 경로 확인 및 기존 SCMS 엔진 연결.
- [x] 본문에서 부서·첨부 영역 분리, 잘못된 링크·미지원 파일·redirect 경계 회귀 검사.
- [x] Java299개·bootJar·Node23개 검사 통과.
- [x] 본문785자와 HWPX1개97,676byte 실제 수집·형식 검증.
- [x] 영수증312건·최신249공고 재현, 임시 관측 원본 정리.
- [ ] 첨부 텍스트 추출·전체 분석·운영 상시 수집·운영 E2E 검증은 별도 후속.

## 구현 근거

수집원은 `LGS-000240 / SCMS_CARD_NOTICE`, 등록 목록은 `https://www.geochang.go.kr/00445/00451.web`이다. 고정 표본은 [거창군 청년 지역활동 프로젝트 지원사업 참여자 모집 공고](https://www.geochang.go.kr/00445/00451.web?amode=view&not_ancmt_mgt_no=47689)다.

- 기존 `ScmsSaeolAttachmentDiscoveryProfile`을 변경하지 않고 `LOCAL_GEOCHANG_SCMS_V1` bean을 추가했다. 기존 지역 프로필 지문은 보존한다.
- `form#saeolGosiVO > div.bbs1view1`의 유일하고 비어 있지 않은 제목, 직접 자식 `div.substance`만 본문으로 사용한다. 부서 정보·첨부 파일명을 분류 본문에 섞지 않는다.
- 공식 `div.attach1`에서 `https://eminwon.geochang.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 고정 GET3필드만 승인한다. 하동의 `FileDown.jsp`와 경로를 구분한다. 실제 파일명·시스템 파일명·경로 검증, 동일 요청 redirect 제한, 최대10파일, UNKNOWN 역할, 정상 파일 보존·오류 분리를 재사용한다.
- 헤더 사전 조사에서 HTTP200·octet-stream·Content-Length97,676·ZIP 선두를 확인했다. 파일명 헤더는 없었다. MIME 예외나 공통 파일 검증기 변경은 없다. 선두 확인만으로 전체 파일 수집 성공을 주장하지 않았다.
- 카탈로그 참조1건을 추가하고 expectation은 null로 유지한다. 키워드 규칙·migration·DB/API·추출기·운영 정책은 변경하지 않았다.

## 검증 결과

실행은 `gradlew.bat --no-daemon :test`의 거창·하동 계약, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GEOCHANG -PsanebCollectionWindowsTrust=true`다.

- Gradle 4분25초·종료0, Java299개 통과·실패/오류/생략0, bootJar 성공.
- 2026-09-30 14:44:25 KST 실제 관측: `GEOCHANG-47689`, 제목 조합 통과, 본문785자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWPX1개97,676byte 다운로드·형식 검증 성공.
- 파일 SHA-256: `d8b50e425075e9f6f11ed1ccddd7a194882b6ec42a2bc65d630d83e109ee74e2`.
- 프로필 지문: `123252f0d6c0f24873a0aa0766819288f6a5bcd3c326ab08e2c98e931c1dadde`.
- 요청 예약 상한 포함4/6회·2,252,172/24,117,248byte. 관측 임시 원본 정리·운영 쓰기0.
- 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 정책 QA·추출·전체 분석·기대값 승인은 미실행이다.
- Node23개 검사, `verify-collection-receipt-index.mjs`의 영수증312건·최신249공고 재현, `report-collection-availability.mjs` 집계 통과.
- **171/223수집원(76.7%) 확인·52잔여(미연결24+등록 다운로드 미확인28)**다. 프로필200개(지역199+기업마당1), 카탈로그258공고/199대상/참조257+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로다.

## 조사 요청 및 다음 지역

별도 구조 조사는 총10GET(거창 상세2·파일 헤더/선두1, 남해7)이며 요청당12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 같은 사이트의 관측된 공식 이동만 명시적으로 따라갔고 원문 파일은 저장하지 않았다. Java 관측 예산은 별도다.

남해 `LGS-000236 / SCMS_CARD_NOTICE`의 등록 목록은 `https://www.namhae.go.kr/socialm/Index.do?c=SM010110000`이다. 공식302 이동 후 `/modules/saeol/gosi.do` 목록과 소상공인 검색을 조회했다. 이동에 쓰인 일회성 요청 검증 값은 메모리에서만 사용하고 출력은 마스킹하며 문서·대장에 저장하지 않았다.

[2026년 남해군 소상공인 임대료 지원사업 공고35694](https://www.namhae.go.kr/modules/saeol/gosi.do?amode=_view&not_ancmt_mgt_no=35694&scd=01&pageCd=SM010110000&siteGubun=socialm)는 일회성 값·쿠키가 없는 별도 클라이언트에서도 HTTP200·동일 제목·본문·공식 첨부 영역을 반환했다. 공식 `eminwon.namhae.go.kr/emwp/jsp/ofr/FileDown.jsp`의 HWPX 링크를 확인했다. `_view`와 고정 scd/pageCd/siteGubun 계약을 처리해야 하며, 아직 파일 다운로드 전이므로 성공 수에는 포함하지 않는다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이며 이번 관측 원본 정리와 혼동하지 않는다.
