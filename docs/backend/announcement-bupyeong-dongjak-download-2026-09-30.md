# 부평·동작 본문·첨부 연결 및 공개 조회 POST

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결이 현재 우선순위다. 수집 가능한 파일은 보존하고 발견·다운로드·추출 오류는 별도로 기록한다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 부평 `LGS-000060 / HEURISTIC_NOTICE`와 동작 `LGS-000021 / SAFE_SAEOL_EMINWON` 본문·첨부 프로필 연결.
- [x] 부평 본문 362자·HWPX 84,859byte, 동작 본문 458자·HWPX 78,955byte 실제 수집.
- [x] 동작 최초 본문 호스트 불일치와 첨부 성공을 분리하고 검증 입력 수정 후 재관측.
- [x] 관련 Java 346개 범위 검증, 수정 후 156개 집중 검증, bootJar, Node 23개 통과.
- [x] 323개 검증 기록·259개 최신 공고 재현, 기존 지역 근거와 프로필 지문 보존.
- [ ] 울산 동구 공식 본문·첨부 구조 연결 및 첫 파일 다운로드.
- [ ] 옹진 목록 제목 selector 계약 보정·목록→상세 상시 수집 검증.
- [ ] 첨부 텍스트 추출·worker 영속화·DB/API·관리자 검증·운영 E2E 후속.

## 구현 경계

### 부평

`BupyeongNoticePage`는 `www.icbp.go.kr/main/eminwon/eminwonAnnounceDetail.do`의 숫자 `mgt_no`를 검증한다. `div.board_view` 안의 제목 `div.title > h5`, 본문 `div.con > div.detail`, 첨부 `div.add_file > dl > dd > ul`을 분리한다. 목록 검색 인자는 상세 요청에서 제거한다.

`BupyeongAttachmentDiscoveryProfile / LOCAL_BUPYEONG_PORTAL_V1`은 공식 첨부 영역의 `li > a`와 `/share/images/filetype/` 아이콘을 대조한다. `eminwon.icbp.go.kr/emwp/jsp/ofr/FileDownNew.jsp`의 불투명 세 인자는 해석·복호화하지 않고 그대로 전송한다. 감사 locator에는 공고번호와 파일 식별 해시만 저장한다. 최대 10파일·PDF/HWP/HWPX·UNKNOWN 역할·부분 descriptor 보존을 유지한다.

### 동작

`DongjakNoticePage`는 `dongjak.eminwon.seoul.kr`의 공식 새올 상세 경로, 일곱 query 필드, 조회 메서드와 숫자 공고번호를 검증한다. 해당 사이트는 상세 GET에서 HTTP400, 공식 조회 POST에서 HTTP200을 반환했다. 임의 POST나 JavaScript를 실행하지 않고 고정 공개 조회 폼만 사용한다.

`DongjakAttachmentDiscoveryProfile / LOCAL_DONGJAK_POST_DETAIL_V1`은 기존 `AttachmentDownloadFlowProfile`을 사용해 논리 상세 URL을 동일 공고의 고정 POST로 변환한다. 공식 `form[name=form][method=post] > div.view`의 제목·본문·첨부 `dt/dd`를 분리한다. 첨부 함수의 세 문자열 인자만 해석해 `/emwp/jsp/ofr/FileDown.jsp` GET 요청을 구성하며, 파일 요청을 POST로 바꾸지 않는다. 기존 새올 파일 검증기를 재사용하고 공통 MIME·호스트·redirect 허용 범위를 넓히지 않았다.

본문 HTTP 전송에도 같은 고정 조회 폼을 적용했다. DNS pinning·TLS 검증·용량 제한·timeout·자동 redirect 금지는 유지한다. 다른 사이트의 본문 요청은 기존 GET을 유지한다. 관측 harness도 동작 상세에 한해 실제 worker와 같은 공통 다운로드 flow를 사용한다.

### 최초 본문 실패와 수정

첫 동작 관측은 포털 주소를 `registeredSourceUrl`로 사용해 `DETAIL_HOST_NOT_ALLOWED`로 본문 요청이 차단됐다. 실제 운영 서비스는 `collectionEndpointUrl`을 우선 사용하며 V61에도 새올 endpoint가 등록돼 있다. 따라서 운영 검증기를 완화하지 않고 테스트 입력을 등록 endpoint에 맞췄다. V61 등록값·상세 호스트 일치를 확인하는 회귀 테스트를 추가했다.

첫 기록의 첨부 다운로드 성공은 유지하고 본문 실패도 지우지 않았다. 보관 파일의 대소문자만 바꾸는 이동 후 파일이 없어져, 이미 전체를 읽어 확보한 도구 출력으로 최초 JSON을 복원했다. 이를 대장에 `initialDongjakArchiveReconstructedFromCapturedToolOutput=true`로 명시했다. 복원본은 `DONGJAK-29506-BEFORE-ENDPOINT.json`이며 기존 검증 기록을 덮어쓰거나 재실행 결과로 대체하지 않았다.

## 실제 관측

| 시각(KST) / 공고 | 본문 | 첨부 |
|---|---|---|
| 2026-09-30 17:00:01 / 부평 50550 | AVAILABLE/ACCEPTED, 362자 | HWPX 1개, 84,859byte |
| 2026-09-30 17:00:26 / 동작 29506 최초 | FETCH_FAILED, DETAIL_HOST_NOT_ALLOWED, 요청 0 | HWPX 1개, 78,955byte |
| 2026-09-30 17:11:57 / 동작 29506 재검증 | AVAILABLE/ACCEPTED, 458자 | 동일 HWPX 1개, 78,955byte |

- 부평 파일 SHA-256: `a424970f62fb1f77195ee0265cb13ac14c71ac3579a39b824434be32daaeba39`.
- 동작 파일 SHA-256: `cbe8ee8984bd410199744741101348e5f874e9397e5737ba81b51ad110388b37`.
- 부평 프로필 지문: `4759465eee43898009b9fa9b655688fe160f4673333664e057da56f4f34c436a`.
- 동작 프로필 지문: `3fc76c9a3691cda2e9b4e9feda69063955a2a8831f08371b42d8d538bf4afd31`.
- 각 시도 최대 6요청·24,117,248byte. 세 관측 예약 상한 사용 합계 12회·6,701,353byte다. 구조 조사 요청과 별도다.
- 실제 파일은 중복 재관측 포함 3회·242,769byte, 서로 다른 파일은 2개·163,814byte다. 지역/공고 집계는 중복을 제거했다.
- 각 보고서 원본 정리=true, 운영 쓰기=0, 추출·정책 QA·기대값 승인=false다. `collectionStageComplete=true`는 첨부 수집 범위이며 본문·추출·운영 E2E 완료를 뜻하지 않는다.

## 실행 명령 / 결과

1. `gradlew.bat --no-daemon :test`에서 부평·동작·본문 HTTP·목록 수집기·본문·카탈로그·snapshot·inventory·파일 형식·worker probe를 선택하고 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=METRO_REMAINDER -PsanebCollectionWindowsTrust=true` 실행: 6분 51초, Java 346개·실패/오류/생략 0, build 성공.
2. endpoint 입력 회귀 추가 후 부평·동작·본문 HTTP·본문 테스트와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=METRO_REMAINDER_DONGJAK -PsanebCollectionWindowsTrust=true`: 1분 57초, Java 156개·실패/오류/생략 0. 부평 외부 파일은 재요청하지 않았다. 생산 코드가 같아 bootJar는 UP-TO-DATE였으며 앞 실행의 성공 산출물을 사용했다.
3. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
4. `node scripts/qa/verify-collection-receipt-index.mjs`: 323개 기록·최신 259공고 재현. 최초 동작과 최신 동작은 별도 기록이며 최신 공고는 하나다.
5. `node scripts/qa/report-collection-availability.mjs`: **180/223수집원(80.7%) 다운로드 확인·43잔여(미연결15+등록 다운로드 미확인28)**. 오류 지역 45개는 성공 지역과 겹친다. 엄격한 전체 세트 Gate는 16/223이다.

프로필 209개(지역208+기업마당1), 카탈로그 268공고/208대상/참조267+기존 기대값1이다. 분모는 2026-09-28 inventory의 활성 지역 수집원이며 운영 목록을 이번에 다시 조회한 수치는 아니다.

## 구조 조사·다음 대상

별도 구조 조사 총 14회(3GET+11POST): 부평 상세 1GET, 동작 목록/상세 2GET+6POST, 울산 남구 등록 목록 1POST, 울산 동구 목록/검색/상세 구조 4POST다. 요청당 12초·2MiB 응답 상한·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 원문을 파일로 저장하지 않았고 응답 객체는 종료했다.

- 울산 남구의 1회 요청은 `MethodInvocationException`으로 종료했다. root cause를 확인하지 않아 timeout/TLS로 단정하지 않는다.
- 울산 동구 `LGS-000080` 공고 28029의 공식 POST 응답 HTTP200, `form[name=form1] > div#viewTable1vw` 안의 `div.bbs_detail_tit > h2`, `div.bbs_detail_content`, `div.bbs_detail_file#download` 구조와 HWPX 링크를 확인했다. 함수 세 인자 기반 `/emwp/jsp/ofr/FileDown.jsp` 파일 GET을 다음 연결 대상으로 삼는다. 실제 파일 다운로드 전이므로 성공 집계에는 넣지 않았다.
- 옹진 목록 첫 공고번호 링크와 실제 제목 링크의 선택 계약은 별도 후속이다. 고정 상세 다운로드 성공으로 상시 목록 연계를 증명하지 않는다.

DB migration·API·분류 규칙·HWP 추출기·운영 정책·worker·ENFORCE·배치·배포는 변경하지 않았다. 브라우저는 사용자 정책상 미실행이다. AWS 인증과 이전 연제·구례 조사 파일 정리 미완료는 별도 후속으로 유지한다.
