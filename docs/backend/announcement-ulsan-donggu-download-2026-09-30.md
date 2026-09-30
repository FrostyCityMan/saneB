# 울산 동구 본문·첨부 연결과 형식 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·추출 오류는 별도 기록한다. HWP 추출기 추가 개선은 보류한다. 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] `LGS-000080 / DOBONG_NOTICE_TABLE`의 공식 제목·본문·첨부 영역 구현.
- [x] 첫 공고 28029의 본문 446자와 파일 전송·형식 검증 실패를 분리.
- [x] 다른 공고에서 GET의 불완전 응답과 공식 POST의 정상 상세 응답을 확인하고 고정 조회 POST 연결.
- [x] 카드수수료 공고 27835의 본문 458자·HWPX 3개와 최종 Java 352개·bootJar 검증.
- [x] 325개 관측 기록·261개 공고 대장 재현, 181/223수집원 다운로드 확인·42잔여.
- [ ] 화성 일반공고 연결 및 등록 수집 주소와의 차이 해결.
- [ ] 추출·worker 영속화·DB/API·관리자 최종 검증·운영 E2E 후속.

## 구현

`UlsanDongguNoticePage`는 `eminwon.donggu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do`의 고정 일곱 필드와 숫자 공고번호를 검증한다. 공식 범주 `01,03,04,05`와 조회 메서드를 임의 입력으로 확장하지 않는다. `form[name=form1][method=post] > div#viewTable1vw > div.bbs_detail` 아래의 제목 `div.bbs_detail_tit > h2`, 본문 `div.bbs_detail_content`, 첨부 `div.bbs_detail_file#download`를 분리한다.

`UlsanDongguAttachmentDiscoveryProfile`은 공식 첨부 영역의 `a[href=#download]`에 있는 `goDownLoad` 세 문자열 인자만 해석한다. 빈 `onkeypress`와 `return false`는 실제 공식 마크업 범위에서 허용하고 추가 스크립트·비어 있지 않은 이벤트 핸들러·다른 호스트는 거부한다. JavaScript는 실행하지 않는다.

- 상세는 고정 공개 조회 POST, `/emwp/jsp/ofr/FileDown.jsp` 파일은 GET이다. `AttachmentProfileDownloadFlow`를 사용해 실제 worker와 관측 harness의 요청 경계를 맞춘다.
- 최초 등록한 프로필 코드 `LOCAL_ULSAN_DONGGU_GET_V1`은 관측 identity 보존을 위해 유지한다. 이름의 GET과 달리 현재 상세 전송은 POST이며 파일 다운로드만 GET이다. 전송 변경은 새 코드 지문으로 구분한다.
- 기존 새올 파일 경로·파일명 검증기를 재사용한다. 최대 10파일·PDF/HWP/HWPX·UNKNOWN 역할·동일 공고/파일 요청 경계를 유지한다.
- 미해석 링크·미지원 형식·개별 파일 실패가 이미 확인한 다른 정상 descriptor를 삭제하지 않는다.
- 본문 요청도 고정 공개 POST에 맞췄다. DNS pinning·TLS 검증·timeout·용량 제한·redirect 차단을 유지하고 다른 기관의 요청 경계는 변경하지 않는다.
- 기존 migration, API, 분류 규칙, HWP 추출기, 운영 정책·worker·ENFORCE·기존 데이터는 변경하지 않았다.

## 관측과 실패 보존

첫 28029 관측은 2026-09-30 17:48:53 KST에 본문 AVAILABLE/ACCEPTED 446자, 첨부 발견 1개를 확인했다. 파일 전송은 60,928byte로 끝났으나 HWPX 힌트와 실제 signature가 달라 `FILE_SIGNATURE / ATTACHMENT_FORMAT_MISMATCH`로 기록했다. 이를 다운로드 성공이나 추출 성공으로 집계하지 않는다.

- 실패 파일 바이트 SHA-256: `2c7efe8d706e01fd790e4df7354781036a4c1d91c9a81d0d5c91de034728b364`.
- 최초 GET 프로필 지문: `e85d5a86036cc59b2e691839c19019cb307a7e43bf7338592f68f7f1c2971704`.
- 기록 `ULSAN_DONGGU-28029.json`을 보존한다. POST 전환 후 지문과 다르므로 최신 코드의 수집 성공 근거로 재사용하지 않는다.
- 첫 관측 예약 4/6회·2,182,656byte, 원본 정리=true, 운영 쓰기=0, 추출·정책 QA·기대값 승인=false다.

추가 조사에서 27835 GET은 HTTP200이지만 예상 제목·첨부가 없는 응답이었다. 동일 공고의 공식 POST는 제목·본문과 첨부 3개를 정상 반환했다. 단순 HTTP200을 상세 수집 성공으로 처리하지 않고 공식 조회 방식에 맞췄다. 이 변경을 위해 기존 형식 검증을 완화하지 않았다.

2026-09-30 17:57:20 KST의 27835 관측은 본문 AVAILABLE/ACCEPTED 458자, 첨부 발견3·다운로드3·실패0으로 끝났다. 정상 파일 합계 208,039byte다.

| 형식 | byte | SHA-256 |
|---|---:|---|
| HWPX | 82,484 | `62503fd53bea5b698998421306e7c4a8e6d0767cf8796d43589c865774a69dc0` |
| HWPX | 75,136 | `a463da582732d1f90cbdbc181f9c3ef98b58cba39f68f8fad713e39ed8e60337` |
| HWPX | 50,419 | `918991e1f089d2761eb373fd8ab6dc9c3441a6c4ca56f323a448aa186b8061e5` |

현재 POST 프로필 지문은 `d158aae90a879b465369365e8fd72b467c6c775efcb4213aceae57ec11b39eee`다. 관측 예약 6/6회·2,329,767byte, 원본 정리=true, 운영 쓰기=0이다. 추출·정책 QA·기대값 승인은 false다. 두 관측의 예약 합계는 10회·4,512,423byte이며 정상3파일과 형식실패1파일을 혼합해 성공 처리하지 않는다.

## 검증

- 첫 중단된 실행은 사용자 일시정지에 따라 종료했다. 테스트 통과로 집계하지 않는다.
- 재개 후 집중 검증: `gradlew.bat --no-daemon :test --tests '*UlsanDongguDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest'`, 144개·실패/오류/생략 0.
- 최초 범위 검증과 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ULSAN_DONGGU -PsanebCollectionWindowsTrust=true`: 3분 24초, Java 348개·실패/오류/생략 0. 실제 파일 관측은 위 형식 실패이며 build 성공과 구분한다.
- POST 전환 후 동일 범위 테스트와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=ULSAN_DONGGU_CARD -PsanebCollectionWindowsTrust=true`: 4분, Java 352개·실패/오류/생략 0. 부평·동작 등 다른 지역 파일은 재요청하지 않았다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`: 325개 기록·261개 최신 공고 재현. 기존 지역의 근거·지문·상태를 보존했다.
- `node scripts/qa/report-collection-availability.mjs`: **181/223수집원(81.2%) 다운로드 확인·42잔여(미연결14+등록 다운로드 미확인28)**. 현재 지문 기준 오류 지역45개는 성공 지역과 겹친다. 최초 GET의 파일 형식 오류는 이 현재 지문 집계와 별도로 관측 기록에 남아 있다. 엄격한 전체 세트 Gate는 16/223으로 유지한다.

프로필 210개(지역209+기업마당1), 카탈로그 270공고/209대상/참조269+기존 기대값1이다. 수집원 분모는 2026-09-28 inventory 기준이다. 지역별 파일1개 이상 관측과 전체 세트·상시 운영 검증을 구분한다.

## 다음 대상 조사

공개 구조 조사 총 20회(17GET+3POST), 요청당 12초·2MiB 응답 상한·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 파일 수집 harness 예산과 별도이며 조사 HTML은 파일로 저장하지 않았다.

1. 울산 동구 6회: 고정 상세 GET/POST, 다른 표본의 상세 GET/POST, 공식 소상공인 목록 검색. 입력은 공식 조회 폼의 고정 값이며 운영 DB를 사용하지 않았다.
2. 울주 3GET: 등록 URL은 고시 범주 `seCode=01`로 이동한다. 이동 응답의 일회성 값은 보관하지 않았다. 일회성 값을 제외한 안정 목록 URL은 HTTP200이었다.
3. 광양 3GET: 등록 URL은 `/saeol/gosi.es?mid=a10909010000&type_code=01`로 이동하며 안정 목록은 HTTP200이다.
4. 화성 8GET: 등록 `BD_notice.do`는 고시 `q_notAncmtSeCode=01`이다. 공식 제목 검색에서 소상공인 결과는 없었다. 공식 일반공고 `BD_selectGosiList.do`는 범주04이며 같은 제목 검색에서 2026년 공고 141781·141338이 확인됐다. `BD_selectNoticeDetail.do?q_notAncmtMgtNo=141781&q_notAncmtSeCode=04`는 HTTP200, `div.board_write > table` 안의 본문·첨부 HWP 링크를 확인했다. 공식 함수는 `https://eminwon.hscity.go.kr/emwp/jsp/ofr/FileDown.jsp` 세 인자 GET이다. 실제 파일 다운로드 전이며 성공 집계에 넣지 않는다. 일반공고 상시 수집에는 등록 주소 보정이 필요하다. 운영 설정은 변경하지 않았다.

브라우저는 사용자 정책상 미실행이다. AWS 인증과 이전 연제·구례 조사 파일 정리 미완료는 별도 후속으로 유지한다.
