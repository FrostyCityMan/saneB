# 부산시 목록 검색 URL과 본문·첨부 연결 보완

## 현재 단계와 Gate

- [x] 공식 목록 검색 인자를 포함한 URL 계약 재현·수정
- [x] 실제 목록 파서가 저장하는 URL·식별자의 첨부 처리기 전달 계약 테스트
- [x] 동일 공고 79622의 검색 문맥 포함 URL로 본문·실파일 재검증
- [x] 과거 영수증 보존 및 현재 코드 대장 재현
- [ ] 강북구 공유 프로필 변경 후 외부 실파일 재검증
- [ ] 운영 목록 유입·worker·DB/API·브라우저 E2E

이번 범위는 로컬 구현과 첨부 수집 검증이다. HWP 추출 고도화는 기존 1.0.16에서 보류하고, 운영 DB·설정·정책·worker·배포는 변경하지 않았다. 브라우저 검증은 현재 요청의 명시적 지시가 없어 정책상 미실행이다.

## 문제와 변경 범위

부산시 공식 목록은 상세 주소에 검색 기간·유형·검색어를 추가한다. 기존 본문·첨부 계약은 `sno`, `gosiGbn`, `curPage`만 허용하여 단순 고정 URL은 성공해도 실제 목록 URL은 거부할 수 있었다.

`BusanLegalNoticePage`에서 두 처리기가 사용하는 허용 계약을 통합했다.

- HTTPS `www.busan.go.kr:443`, 정확한 `/nbgosi/view`만 허용한다.
- 기존 3개 인자와 `conIfmStdt`, `conIfmEnddt`, `conGosiGbn`, `schKeyType`, `srchText`만 허용한다.
- 중복·알 수 없는 인자·제어문자·잘못된 UTF-8·존재하지 않는 날짜를 거부한다. 검색어는 1회 디코딩 기준 256자 이내로 제한한다.
- 본문은 기존과 같이 `gosiGbn=A`를 요구한다. 상세 제목과 단일 본문 영역 검증은 유지한다.
- 상세 리다이렉트는 같은 `sno`와 `gosiGbn`, 첨부 리다이렉트는 같은 `fileId`와 `seq`만 허용한다. 상세→파일, 다른 공고·파일로의 이동을 차단한다.
- 파일 다운로드 허용 경로·형식·용량 검증은 확대하지 않는다.

목록 수집기는 정규화된 URL과 그 URL의 해시를 저장한다. 기존 정규화기는 검색어의 percent encoding을 다시 인코딩할 수 있으므로, 부산 프로필에서 검증된 원문 URL의 기존 canonical hash 또는 저장 URL 자체의 hash를 인정한다. 전역 정규화기·저장 식별자·DB/API·migration은 변경하지 않았다. 검색 문맥에 따른 중복 식별자 통합까지 해결한 것은 아니다.

## 실파일 근거

공고: `BUSAN_CITY-79622`, 「2026년도 부산광역시 중소기업·소상공인 자금지원계획 10차 변경 공고」.

기존 표본의 URL만 실제 목록 검색 문맥을 가진 저장 URL로 교체했다. 같은 공고를 새 표본으로 추가하지 않았으며 카탈로그는 288개를 유지한다.

| 항목 | 결과 |
|---|---|
| 관측 시각 | 2026-10-01 01:04:51 KST |
| 제목·본문 | 제목 조합 통과, 본문 146자, 대상·지원 조합 확인 |
| 첨부 | HWPX 1개 발견·다운로드, 692,641바이트 |
| 파일 SHA-256 | `775f683ea8802d4b90a7fde915b587c0579626cbda984b0c471e768cc1b9b567` |
| 요청·전송 예산 | 최대 예약 6회·23MiB, 본문 포함 실제 예약 상한 4회·2,896,289바이트 |
| 정리·운영 | 검증 원본 정리 완료, 운영 쓰기 0 |
| 미검증 | 추출·구간 분석·정책 승인·운영 E2E |

보고서: `build/reports/attachment-regional-collection/BUSAN_CITY-79622-BUSAN-QUERY-20261001.json`.

인벤토리 SHA-256: `7a0dcd6a6497bb792c4bfd8b89801e43f828df2c87c987412fa352a4888d878d`.
변경 전 JSON은 `build/reports/attachment-target-inventory/before-busan-query-20261001.json`에 보존했다. LF 저장본의 SHA-256은 `fff1d67c13feb843ad97a74cf3cfcf16e5710aaa81c5a30554a4123c20407862`이다.

공유 클래스 수정으로 부산시·강북구 2개 프로필 지문만 변경됐다. 나머지 221개 프로필 지문은 유지됐다. 강북구 동작의 단위 회귀는 통과했으나 외부 다운로드는 이번에 재실행하지 않았다.

## 검증 명령과 결과

1. 수정 전 신규 계약 테스트 4개 실행: 3개 실패로 문제 재현.
2. 수정 후 부산·법정게시판 선택 테스트 34개 통과.
3. `gradlew.bat --no-daemon :test`에서 부산 URL, 본문 전체, 법정게시판 발견·전송, 부산 수집, worker, 식별자 정규화 테스트 선택 실행 및 `:bootJar`: **227개 통과·실패/생략 0, 패키징 통과**.
4. 최종 목록 수집기·URL·카탈로그·부산 수집·인벤토리 선택 실행: **147개 통과·실패/생략 0**. 인벤토리는 2026-09-28 운영 대상 스냅샷과 현재 로컬 코드의 비교이며 새로운 운영 조회가 아니다.
5. `:attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BUSAN_CITY_SUPPORT -PsanebCollectionReportLabel=BUSAN-QUERY-20261001 -PsanebCollectionWindowsTrust=true`: 단일 고정 표본 실파일 관측 통과. Windows 신뢰 저장소를 해당 실행에만 지정하고 TLS 검증은 유지했다.
6. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: **23개 통과**.
7. `node scripts/qa/verify-collection-receipt-index.mjs`: **416개 영수증·281개 최신 공고 재현 통과**.
8. `node scripts/qa/report-collection-availability.mjs`: 현재 코드 다운로드 관측 **36/223**, 미확인·재검증 **187**, 오류를 가진 수집원 **9**. 오류 수는 성공 수와 중복될 수 있다.

## 남은 범위

- 과거 제목 적격 파일 다운로드 관측 누적 **202/223(90.6%)**, 한 번도 확인되지 않은 수집원 **21개**는 변하지 않았다. 이는 현재 배포·운영 성공률이 아니다.
- 과거 자료를 최신 지문으로 치환하지 않는다. 현재 코드의 엄격한 3표본·전체 파일 집합 Gate는 **0/223**이며 기존 과거 통과 기록은 보존한다.
- 실제 목록 파서 연결은 로컬 계약 테스트, 외부 수집은 같은 URL의 고정 공고 관측이다. 운영 상시 목록→DB→worker 전체 성공으로 표현하지 않는다.
- AWS 인증 갱신 미완료로 서울 서버 진단은 대기다. 이전 임시 공개 CA 파일 2개와 지역 조사 HTML 정리 차단도 해결되지 않았다. 이번 다운로드 원본 정리와 구분한다.
- 다음 작업은 다운로드 미확인 21개 수집원의 원인별 처리와 현재 코드 재검증이다. 실패는 별도 오류로 보존하고 성공 파일의 수집을 막지 않는다.
