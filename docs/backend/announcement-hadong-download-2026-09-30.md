# 하동 본문·직접 첨부 다운로드 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일은 수집하고 개별 실패는 별도로 보존한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 하동 공식 상세·직접 파일 경로 확인 및 기존 SCMS 엔진 연결.
- [x] 본문에서 부서·첨부 영역 분리, 잘못된 링크·미지원 파일·redirect 경계 회귀 검사.
- [x] Java290개·bootJar·Node23개 검사 통과.
- [x] 본문402자와 HWPX1개156,622byte 실제 수집·형식 검증.
- [x] 영수증311건·최신248공고 재현, 임시 관측 원본 정리.
- [ ] 첨부 텍스트 추출·전체 분석·운영 상시 수집·운영 E2E 검증은 별도 후속.

## 구현 근거

수집원은 `LGS-000237 / SCMS_CARD_NOTICE`, 등록 목록은 `https://www.hadong.go.kr/media/00012.web`이다. 고정 표본은 [2026년 하동군 소상공인 디지털 인프라 지원사업 추가공고](https://www.hadong.go.kr/media/00012.web?amode=view&not_ancmt_mgt_no=45193)다.

- 기존 `ScmsSaeolAttachmentDiscoveryProfile`을 변경하지 않고 `LOCAL_HADONG_SCMS_V1` bean을 추가했다. 다른 지역 프로필 지문은 보존한다.
- `form#saeolGosiVO > div.bbs1view1`의 유일하고 비어 있지 않은 제목, 직접 자식 `div.substance`만 본문으로 사용한다. 부서 정보·첨부 파일명을 분류 본문에 섞지 않는다.
- 공식 `div.attach1`에서 `https://eminwon.hadong.go.kr/emwp/jsp/ofr/FileDown.jsp`의 고정 GET3필드만 승인한다. 실제 파일명·시스템 파일명·경로 검증과 동일 요청 redirect 제한, 최대10파일, UNKNOWN 역할, 정상 파일 보존·오류 분리를 재사용한다.
- 헤더 사전 조사에서 octet-stream·Content-Length156,622·ZIP 선두를 확인했다. 파일명 헤더는 없었다. MIME 예외나 공통 파일 검증기 변경은 필요하지 않았다. 선두 확인만으로 전체 파일 수집 성공을 주장하지 않았다.
- 카탈로그 참조1건을 추가하고 expectation은 null로 유지한다. 키워드 규칙·migration·DB/API·추출기·운영 정책은 변경하지 않았다.

## 검증 결과

실행 명령은 `gradlew.bat --no-daemon :test`의 하동 계약·본문·카탈로그·snapshot·inventory·파일 형식·worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HADONG -PsanebCollectionWindowsTrust=true`다.

- Gradle 4분15초·종료0, Java290개 통과·실패/오류/생략0, bootJar 성공.
- 2026-09-30 14:35:04 KST 실제 관측: `HADONG-45193`, 제목 조합 통과, 본문402자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete, HWPX1개156,622byte 다운로드·형식 검증 성공.
- 파일 SHA-256: `6fe5fbf260146c78bf5503d6d08e92c872c7630b33c657146616bc2f1fd7418c`.
- 프로필 지문: `19d9ecf4538fea5842de8e5a42cd3c56d74c63f1a0c254df2dc561fc88c01562`.
- 요청 예약 상한 포함4/6회·2,343,886/24,117,248byte. 관측 임시 원본 정리·운영 쓰기0.
- 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 정책 QA·추출·전체 분석·기대값 승인은 미실행이다.
- Node23개 검사, `verify-collection-receipt-index.mjs`의 영수증311건·최신248공고 재현, `report-collection-availability.mjs` 집계 통과.
- **170/223수집원(76.2%) 확인·53잔여(미연결25+등록 다운로드 미확인28)**다. 프로필199개(지역198+기업마당1), 카탈로그257공고/198대상/참조256+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로다.

## 조사 요청 및 다음 지역

이번 연결에 사용한 별도 구조 조사는 총8GET(하동 사전 상세2·파일 헤더/선두2, 거창4)이며 요청당12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 조사 원문은 파일로 저장하지 않았다. 첫 헤더 조사 출력에서 없는 Content-Disposition에 대한 진단 코드 오류가 발생했으며, 수정한 출력으로 재확인했다. 이를 사이트 실패나 전체 다운로드 성공으로 계수하지 않는다. 위 Java 관측 예산은 이 조사와 별도다.

거창 `LGS-000240 / SCMS_CARD_NOTICE`의 공식 목록·소상공인 검색·지원 검색·상세 총4GET에서 HTTP200을 확인했다. 소상공인 검색에서는 표본을 확보하지 못했으나 지원 검색에서 [청년 지역활동 프로젝트 지원사업 참여자 모집 공고47689](https://www.geochang.go.kr/00445/00451.web?amode=view&not_ancmt_mgt_no=47689)를 찾았다. 동일한 SCMS 제목·본문·첨부 영역과 `eminwon.geochang.go.kr/emwp/jsp/ofr/FileDownNew.jsp` 경로를 확인했다. 거창은 아직 파일 다운로드 전이며 다음 연결 대상으로 남긴다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이며 이번 관측 원본 정리와 혼동하지 않는다.
