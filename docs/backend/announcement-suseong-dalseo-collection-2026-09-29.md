# 수성구·달서구 첨부 수집 연결 — 2026-09-29

## 범위와 판정

HWP1.0.16 추출 개선은 보류한다. 제목 정책 → 공식 상세 식별 → 첨부 전체 발견 → binary 다운로드·signature·hash → 원본 정리만 검증했다. 운영 DB·정책·worker·기존 데이터·배포·API/DB 계약은 변경하지 않았다.

- [x] 수성구 GET 연결 및1공고 HWPX1개 검증.
- [~] 수성구는 표본1건이므로 지역 수집 Gate에 필요한2건이 더 남음.
- [x] 달서구 POST 연결 및3공고 HWP2·HWPX1 전체 다운로드 검증.
- [ ] 전 지역 완료: 활성223지역 중 **16충족/207잔여 = 등록 미완료19 + 미등록188**.

09-28 15:56:12.116778+09:00 운영 읽기 전용 영수증의 `LGS-000050` 수성구·`LGS-000051` 달서구는 활성/SAFE_SAEOL_EMINWON/첨부 미연결이었다. 동일 영수증과 최신 로컬 코드를 비교했으며 운영을 새로 조회하거나 배포한 것이 아니다. 비활성21지역은 활성화하지 않는다.

## 공식 경로와 구현

수성구 대표 메뉴 `https://www.suseong.kr/index.do?menu_id=00000064`의 실제 iframe은 `eminwon.suseong.kr/emwp/jsp/ofr/OfrNotAncmtL.jsp?not_ancmt_se_code=01,04`다. HTML 주석에 있는 과거 HTTP iframe은 사용하지 않았다. 공식 form의 청년 검색은 결과0건, 지원·소상공인 검색에서는52705를 확인했다. 검색0건을 첨부 없음으로 해석하지 않는다.

수성구 상세는 form1 POST·table.readtype2·th(scope=row) 제목/인접td·첨부파일 th/td이며 공개 goDownLoad3인자/FileDown.jsp GET이다. 기존 새올 GET 엔진에 고정 bean만 추가했다. 공식 목록 진입 form의 subCheck=N은 조사 요청에서 유지했다. 고정 상세는 기존 목록 parser의 V37 SAFE_TEMPLATE가 생성하는7개 query/subCheck=Y를 그대로 사용하여 제목과 공식 상세 응답을 확인했다. 목록 설정이나 기존 원문 identity를 변경하지 않았다.

달서구 대표 메뉴 `https://dalseo.daegu.kr/index.do?menu_id=10000104`에서 `eminwon.dalseo.daegu.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04`를 확인했다. 첫 청년 검색 POST는15초/0byte 시간 초과였다. 후속 공식 지원·소상공인 검색은200으로 응답했으며 타임아웃을 성공 요청으로 기록하지 않는다.

달서구는 form1 > div#bbsView > div.form_group > dl.title 제목과 dl.attfile 첨부 영역이다. 다운로드는 별도 nnn POST form의3개 hidden 필드와 FileDownNew.jsp다. 불투명 인자는 해독하거나 원문 영구 저장하지 않고 같은 HTTPS 출처의 고정 POST로만 전달한다. GET 다운로드 대체·isHome 추가·임의 host/path·추가 query·파일 소속이 달라지는 redirect는 거절한다. 기존 부산 북구 POST 코드는 변경하지 않고 달서구 독립 프로필을 추가해 이전 hash를 보존했다.

두 지역 모두 첨부 영역 전체를 처리하고 미지원 파일·미해결 링크·중복 충돌·10개 초과를 숨기지 않는다. 파일명으로 NOTICE/FORM 역할을 확정하지 않고 UNKNOWN을 유지한다. catalog4건은 `expectation:null`이며 정상 기대값 승인·정책 게시가 아니다.

## 표본과 실제 결과

|지역|공고|실제 첨부|다운로드 검증|
|---|---|---|---|
|수성구|52705 소상공인 정책자금 이차보전|HWPX1|51,649byte 성공|
|달서구|54290 2026 소상공인 경영안정자금 변경|HWP1|56,832byte 성공|
|달서구|53625 청년 자격증 응시료|HWPX1|93,865byte 성공|
|달서구|42293 2023 하반기 소상공인 경영안정자금 변경|HWP1|21,504byte 성공|

공고4건·파일4개·고유 binary4개·223,850byte다. 달서구에는2023년 과거 공고가 포함되며3건을 현재 신청 가능한3개 사업으로 표현하지 않는다. 수성구1건 성공을 기관 전체/지역3표본 충족으로 확대하지 않는다.

조사 수성6회·달서8회(0byte 타임아웃1 포함), HTML13개560,091byte. 연결7초/요청15초/1MiB 상한과 자동 redirect 없음으로 제한했다. 검증은 공고당6요청/23MiB·파일20MiB 상한이며 실제 예약은 수성4요청/2,153,408byte, 달서12요청/6,489,217byte다. 조사 포함 **수성10/30·달서20/30**, 총30요청 예약이고 본문 최대치 예약을 포함하므로 실제 전송량과 다르다. 추가 재다운로드/추출은 없었다.

조사 원본13개는 삭제했다. 실제 작업의 상세·binary 원본은 각 검증에서 정리됐으며 비식별 실행 보고서/hash만 보관한다. 작업용 helper와 빈 소유 폴더도 삭제 확인했다. 기존 사용자 output/와 __pycache__는 보존했다.

## 검증과 결과 근거

```powershell
.\gradlew.bat :test --tests '*SuseongDalseoCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SUSEONG -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DALSEO -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 공식 HTML 검사는 `SANEB_SUSEONG_DALSEO_SAVED_DETAILS`, 등록 대조는 기존 비식별 운영 영수증을 주입했다. Windows-ROOT는 로컬 인증서 저장소이며 TLS 우회가 아니다.

- 로컬100검사(새 계약7·새올26·catalog64·등록3), 실패/생략0. bootJar 생성 통과. 전체 프로젝트 테스트는 재실행하지 않았다.
- 실제 수성1검사·달서3검사, 실패/생략0. 추출기는 실행하지 않았다.
- Node19검사 및 보관98영수증/최신75공고 재현 통과. 이전15충족 지역의 프로필·근거 deepEqual 통과.
- 수성 프로필 hash=`80c0fa8e8bcf205847328546ba083a1d36ec908d09760dde6a50be200e54d508`, 달서=`af0a2aa2da26d454e87da819185b9b30e6aa117777c00263dee4d8241ba509ec`.
- 수성 JUnit hash=`4c943c606b984b4fb3cb83f772013183b83527f5ba8750a4db272df119814d3e`, 달서=`12941ad07e88b78ab50327211bcbd2a4145b6635d8bb165393c8180712599208`. 각 실행 직후 기록했으며 공통 JUnit은 후속 실행으로 교체된다. 비식별 보고서는 `build/reports/attachment-regional-collection/SUSEONG-52705.json`, `DALSEO-*.json`이다.
- 등록36프로필(지역35·기업마당1), catalog90참조/32지역 프로필·보관 기대값1/정상 승인0. 정책225대상 연결36/미등록189(지역188+정부24), 활성223지역 등록35/미등록188이다.
- [지역 수집 대장](attachment-collection-regional-ledger-2026-09-28.json)은 **16충족/207잔여**다. 영도 HTTP400·연제403·북구 signature 등 기존 실패는 그대로 남는다.

HWP 추출 개선·운영 정책 게시·ENFORCE·기존 데이터 적용·배포는 하지 않았다. Linux/운영 E2E는 이번 검증 범위가 아니며 브라우저는 현재 요청의 명시 지시가 없어 정책상 미실행이다. 수집 성공은 텍스트 분석·정책 QA·상시 운영 worker·전체 Goal 완료가 아니다. 다음은 미연결 지역 확대와 수성 추가 적격 표본 확보다.
