# 공개 세션 변경 후 첨부 전송 회귀와 집계 정정

## 현재 단계 / Gate

- [x] 공통 전송 변경 후 inventory와 수집 대장 재구성
- [x] GET·POST·redirect 대표 3수집원 재검증
- [x] 고정 지문 회귀 대상 12수집원·14공고 재검증
- [x] 지문 기대값 실패 7건 재현·정정, 기존 보안 검증 보존
- [x] 집중 Java 230통과·6조건부 생략·0실패, Node39통과, bootJar 성공
- [!] 기존 화천 고정 worker 계약은 미해결, 전체 Linux CI 재통과 미확인
- [~] 전체 지역 파일 연결: 최신 표본 기준 미확보18개, 현재 코드 실파일 확인16/223
- [ ] 운영 worker·DB/API/UI·배포·브라우저 E2E

기준 HEAD `cc44ebd92db59f4b1c717bcb4546b38879c8b879`. `long-goal-operating-protocol`에 따라 공통 전송 변경과 실제 사이트 실패를 구별했다. 이 회차에서는 응용 코드·Flyway V85·API·규칙·HWP 추출기1.0.16을 변경하지 않았다. 성공 파일과 발견/전송/형식 오류를 분리하며 HWP 고도화는 계속 보류한다.

## 진척 수치 정정

분모223은 2026-09-28 운영 대상 스냅샷의 활성 수집원 수이며 고유 지자체 수가 아니다. 현재 운영 조회나 전체 goal 완료율로 사용하지 않는다.

| 집계 기준 | 확인 | 미확인 또는 미확보 |
| --- | ---: | ---: |
| 보관된 모든 영수증에서 과거 적격 파일 성공이 한 번이라도 있음 | 208 | 15 |
| 공고별 최신 표본에서 적격 파일 성공이 있음, 과거 실행 지문 포함 | 205 | 18 |
| 현재 실행 지문과 일치하는 실제 파일 성공 | 16 | 207 |
| 현재 지문의 엄격한 세 표본·전체 첨부 집합 Gate | 0 | 223 |

기존 보고의 “최초 다운로드 미확인18개”는 잘못된 표현이다. 정확히는 **최초 미확인15개 + 과거 성공 후 최신 표본에서 미확보3개**다. 아산(LGS-000151)·봉화(000220)·남해(000236)의 과거 성공을 삭제하지 않으며 최신 실패도 성공으로 덮지 않는다.

최초 미확인15개: 은평·서대문·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·공주·의성. 등록은222/223이며 울산 남구만 프로필 미연결이다. 과거 엄격 Gate16/223은 이전 지문 시점의 이력이며 현재 값으로 치환하지 않는다.

## 대장 무결성

이전 inventory 원본은 `build/reports/attachment-target-inventory/operating-targets-pre-public-session-20261001.json`에 동일 바이트로 보존했다(SHA `bcb1f65eeab9daab01c832a390f948256e3eda7b5930cb062405592f3e01cab2`). 새 inventory SHA는 `57ddccfedcbf0080d63111572790f83c03c01d78e29eb806626c2121551d1e70`이다.

기존686영수증을 보존하고 성주1·대표 전송3·기준값 회귀14영수증을 추가하여 **704영수증·289최신 표본**을 재현했다. 동일 공고 재검증을 신규 공고로 늘리지 않았다. 생산 클래스 SHA는 `020fb8bc2256fab329fa07060218181a9fedb5d536b39a27d1a53d43e0c74e2b`이며 테스트 수정 후에도 동일하다. `PUBLIC-SESSION-01`은 당시 생산 클래스 봉인 근거가 없어 대장에 넣지 않았고 진단 이력만 보존한다.

## 실제 전송 결과

모든 관측은 기존 고정 공고, 표본별 한도, TLS 검증 및 기존 형식 검사기를 사용했다. 원본 정리true·운영쓰기0이다. 보고서는 `build/reports/attachment-regional-collection/` 아래 개별 JSON이며 경로·SHA·프로필 지문은 영수증 색인에 고정했다. 텍스트 추출이나 규칙 승인 근거가 아니다.

| 관측 표식 | 대상 | 정상 파일 | 결과 경계 |
| --- | --- | ---: | --- |
| PUBLIC-SESSION-REGRESSION-01 | 서초·예천·영주 | 4 | GET·POST·redirect 확인, 영주 미지원1건 별도 |
| PUBLIC-SESSION-PINS-01 | 시흥·양구·영종·미추홀·광명·대구 중구 | 10 | 8공고, 중구33315 제목 중단·요청0 |
| PUBLIC-SESSION-PINS-02 | 서울시·서울 중구·부여·고성·용산·창원 | 12 | 고성 파일 성공과 첨부 발견 경고 병행 |

대표 전송3곳: 512240byte, 요청 예약15/상한19, 바이트 예약7068178/상한72351744.
기준값 회귀12곳: 2845741byte, 요청 예약61/상한84, 바이트 예약32458875/상한337641472. 예약량을 실제 wire 요청 수·전송량으로 표현하지 않는다. 본문 상태는 각 원본 보고서에 별도 보존한다.

## 일반 기준값과 승인 계약 구분

Linux run36791362494의 contracts job110144771882는 4465개 중4137통과·8실패·320생략으로 끝났다. 실패8개는 기존 화천 고정 worker1건과 공통 지문 변경에 따른 일반 참조7건이다. 로컬 집중 실행에서도 29개 중7실패·4생략으로 동일한 기준값 실패를 재현했다.

6개 일반 참조 테스트는 위 실제 파일 관측 지문으로 갱신했다. 기존 MIME·헤더·경로·HTML 거부와 파일 수·제목 중단·expectation null 검사는 유지했다. 고성의 지문 갱신은 전체 첨부 발견 완료를 의미하지 않는다.

`ChungbukCitynetResponseTest`의 공주 지문은 실행 코드 기준값으로 갱신하고 기존 POST form·공개 세션 없음·Referer 없음·redirect 옵션 없음·MIME 예외 없음 검증을 추가했다. **공주의 외부 파일 성공은 미확인**이다. 이 정적 계약 검증을 대장 수집 성공이나 정책 승인으로 사용하지 않는다.

화천 `HwacheonOfficialWorkerContract`의 고정 파일·추출기·worker DB/API 계약은 수정하거나 생략하지 않았다. HWP 개선 보류 중이며 기존 실패를 이 회차의 다운로드 회귀로 대체하지 않는다.

## 은평·동두천 별도 진단

이 직전 읽기 전용 진단도 성공으로 올리지 않는다.

- 동두천45339: 공식 상세200·568123byte와 첨부 링크1개 확인. 상세 쿠키는 다른 첨부 호스트에 적용되지 않았고 파일 요청은 전송 실패했다. 정확한 하위 오류는 미확정이다.
- 은평50607: 진단용 첨부 라벨 처리 보정 후 공식 링크4개 확인. 공식 함수의 파일 주소에 공개 세션과 Referer를 적용해도 HTTP404·HTML1040byte였다. 처음 라벨 판독 실패는 진단기의 NBSP/콜론 처리 문제로, 응용 파서 변경으로 오인하지 않는다.
- 두 진단 합계 시도5요청, 메모리 내 처리·원문 파일 저장0·운영쓰기0. 쿠키 값은 기록하지 않았다. 같은 조건의 재요청이나 다른 지역으로 세션 처리를 확대하지 않는다.

## 검증 명령과 후속

```powershell
.\gradlew.bat --no-daemon attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SIHEUNG,YANGGU,YEONGJONG,MICHUHOL_SUPPORT,GWANGMYEONG,JUNGGU' -PsanebCollectionReportLabel=PUBLIC-SESSION-PINS-01 -PsanebCollectionWindowsTrust=true --rerun
.\gradlew.bat --no-daemon attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEOUL,SEOUL_JUNGGU,BUYEO,GOSEONG,YONGSAN,CHANGWON' -PsanebCollectionReportLabel=PUBLIC-SESSION-PINS-02 -PsanebCollectionWindowsTrust=true --rerun
.\gradlew.bat --no-daemon :test --tests '*ChungbukCitynetResponseTest' --tests '*AnnouncementAttachmentJungguObservationContractTest' --tests '*AnsanInjeMimeCompatibilityTest' --tests '*GeomdanHeaderCompatibilityTest' --tests '*JemulpoHeaderCompatibilityTest' --tests '*LegacyThreeMimeCompatibilityTest' --tests '*PajuHeaderCompatibilityTest' --tests '*AttachmentPublicSession*Test' --tests '*AttachmentPinnedDownloadClientTest' --tests '*AttachmentDownloadBoundaryTest' --tests '*AttachmentRefererDownloadTest' --tests '*GyeongbukThirdDownloadContractTest' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AttachmentProviderQaCaseExecutorTest' :bootJar --rerun
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-github-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.safecrlf=false diff --check
```

관측 묶음은43초·41초 종료0. 마지막 집중 Java는1분2초, JUnit XML236개 중230통과·6조건부 생략·실패0, bootJar 재실행 성공이다. 생략된 원본 fixture·Linux 전용 검사를 성공에 포함하지 않는다. Node39개 전부 통과. 전체 루트 테스트와 Linux CI는 수정 후 재통과가 확인되지 않았다.

다음 우선순위는 미확보18개 중 접근 조건이나 공식 경로를 새롭게 확인할 수 있는 대상이다. 동일한 오류 반복과 모든 수집원의 세 표본 확보를 다음 지역의 선행 조건으로 삼지 않는다. 운영 DB·설정·worker·정책·ENFORCE·배포는 미실행이고 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 사용자 미추적 output/·캐시는 보존한다. 장기 goal은 계속 진행 중이다.
