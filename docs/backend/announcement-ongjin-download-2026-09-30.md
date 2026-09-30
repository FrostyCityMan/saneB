# 옹진 공식 본문·첨부 연결 및 제목 중단 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·추출 오류는 별도로 남긴다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 옹진 `LGS-000065 / SPRING_BBS` 본문·첨부 연결 구현.
- [x] 제목 조합 미충족 표본은 요청 0으로 중단하고 별도 기록 보존.
- [x] 제목 조합 충족 표본의 본문 75자·HWP 1개 71,680byte 수집.
- [x] 관련 Java 332개·bootJar·Node 23개·320영수증/257공고 재현 검증.
- [x] 이번 임시 원본 및 단발 검증 프로세스 종료 확인.
- [ ] 목록 제목 선택 계약 점검·보정 후 상시 목록→상세 연결 검증.
- [ ] 첨부 텍스트 추출·DB/API·관리자 최종 검증·운영 E2E 후속.

## 구현과 경계

등록 목록은 `https://www.ongjin.go.kr/open_content/main/community/board/announce.jsp`이며 공식 검색은 `/open_content/main/eminwon/eminwonAnnounceList.do`의 `keyfield=title`, `keyword` GET 폼이다.

- `OngjinNoticePage`: HTTPS 고정 호스트·상세 경로·숫자 `mgt_no`와 알려진 목록 검색 필드를 검증한다. 상세 요청에서는 검색 필드를 제외한다. `div.board_view`의 직접 자식 제목·본문·첨부 영역을 분리하고 중복/변경 구조는 오류로 처리한다.
- `OngjinAttachmentDiscoveryProfile / LOCAL_ONGJIN_PORTAL_V1`: 공식 `dl.file > dd > ul > li.margin_b5` 안의 링크만 읽는다. `eminwon.ongjin.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 세 인자는 해석하거나 재구성하지 않고 전송한다. 감사 locator에는 공고번호·파일 식별 해시만 저장한다.
- 파일명/아이콘 형식 대조, 최대 10파일, PDF/HWP/HWPX, UNKNOWN 역할, 동일 요청 redirect 제한을 유지한다. 미지원 파일·미해석 링크가 있어도 정상 descriptor를 보존한다.
- 기존 계양·강화 프로필/공유 클래스/지문과 공통 MIME·헤더 허용 범위는 변경하지 않았다. 카탈로그의 두 옹진 표본은 `expectation=null` 참조이며 승인된 정책 QA가 아니다.
- DB migration·API·분류 규칙·추출기·운영 설정·기존 데이터는 변경하지 않았다.

## 실제 관측

| 시각(KST) / 공고 | 결과 |
|---|---|
| 2026-09-30 16:26:58 / 36423 경영환경개선사업 | `TITLE_EXCLUDED_NOT_FETCHED / TITLE_COMBINATION_NOT_MATCHED`, 본문·파일 요청 0 |
| 2026-09-30 16:35:25 / 36422 카드수수료 지원사업 | 제목 조합 충족, 본문 AVAILABLE/ACCEPTED 75자, HWP 1개 다운로드 성공 |

첫 표본을 통과시키려고 지원 키워드를 추가하지 않았다. 기존 제목 정책을 유지하고, 동일 공식 목록에서 제목 조합을 충족하는 다른 표본을 확인했다. 두 기록을 모두 보존했다. 사전 구조 조사 GET과 실제 수집 harness의 요청 0은 구분한다.

- 성공 파일 SHA-256: `1d7709dea198268bba75b2dab062da01902c60a1463f56787cf41e8f4786f769`.
- 프로필 지문: `29bb91d5952324f1720a7b271b5420511113da3c025311d4e0c0589432c13e10`.
- 수집 상태: `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 발견 1/다운로드 1/실패 0, 해당 표본 collectionStageComplete=true.
- 예약 4/6회·2,283,520byte, 상한 24,117,248byte. 원본 정리=true, 운영 쓰기=0, 추출·정책 QA·기대값 승인=false.
- 본문과 파일은 같은 표본 관측에서 확인했다. 한 표본의 수집 완료를 지역 전체·텍스트 추출·상시 운영 완료로 바꾸지 않는다.

## 실행 명령 / 결과

`gradlew.bat --no-daemon :test`에서 Ongjin 계약, 목록 수집기, 본문, 카탈로그, snapshot, inventory, 파일 형식, worker probe 테스트를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ONGJIN -PsanebCollectionWindowsTrust=true`를 실행했다.

- 첫 실행 6분 35초: Java 331개·bootJar 통과, 제목 단계 중단 기록 생성.
- 제목 정책 회귀 테스트·두 번째 표본 추가 후 6분 40초: Java 332개·실패/오류/생략 0, bootJar 성공, 실제 다운로드 성공.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`: 320영수증·최신 257공고 재현. 기존 지역 근거·상태를 보존했다.
- `node scripts/qa/report-collection-availability.mjs`: **178/223수집원(79.8%) 다운로드 확인·45잔여(미연결17+등록 다운로드 미확인28)**. 오류 지역 45개는 성공 지역과 겹친다. 엄격한 전체 세트 Gate는 16/223으로 유지한다.
- 프로필 207개(지역206+기업마당1), 카탈로그 266공고/206대상/참조265+기존 기대값1이다. 수집원 분모는 2026-09-28 inventory 기준이다.

## 구조 조사와 후속

별도 구조 조사 7GET: 옹진 상세 2회·공식 목록 1회, 부평 목록/폼 1회·검색 2회·상세 1회다. 요청당 12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 조사 원문은 파일로 저장하지 않았고 Java 관측 예산과 별도다.

1. **옹진 목록 제목:** 실제 행은 공고번호 링크 다음에 `td.left.title > a` 제목이 있다. 저장소의 SPRING_BBS 기본 selector `td a`와 수집기의 `selectFirst` 조합은 첫 공고번호를 선택한다. 현재 운영 DB selector는 이번에 조회하지 않았다. 운영 설정을 변경하지 않고, 목록 계약 보정 및 목록→제목 판정 회귀를 별도 후속으로 남긴다.
2. **부평 다음 표본:** 공식 날짜 검색에서 [신혼부부 대출이자 지원 공고 50550](https://www.icbp.go.kr/main/eminwon/eminwonAnnounceDetail.do?mgt_no=50550)를 확인했다. 제목은 `div.board_view > div.title > h5`, 첨부는 `div.add_file > dl > dd > ul > li > a`, 본문은 `div.con > div.detail`이다. `eminwon.icbp.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 세 인자 링크와 HWPX 1개를 확인했으나 실제 다운로드는 미실행이다. 아이콘 경로는 `/share/images/filetype/hwpx.gif`다. 불투명 인자 원문은 기록하지 않는다.
3. 첫 검증에서 카탈로그 회귀 106개가 약 257초를 차지했다. 다음 지역은 구조가 확인된 묶음 단위로 구현하고 공통 회귀를 합쳐 실행하는 방식을 우선한다. 검증 항목을 삭제하지 않는다.

운영 정책·worker·ENFORCE·배치·배포는 변경하지 않았다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이며 이번 원본 정리 성공으로 덮어쓰지 않는다.
