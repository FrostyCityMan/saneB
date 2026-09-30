# 경남 9수집원 재검증과 천안·서대문·거창 원천 오류 분리

## 현재 단계 / Gate

- [x] 경남 9수집원의 기존 고정 표본을 최신 코드로 순차 재검증
- [x] 7수집원·유효 파일8개(1,219,671byte) 성공 근거 보존
- [x] 거창 상세 확인 실패·남해 파일 TLS 실패·김해 첨부 발견 경고 분리
- [x] 영수증427개·최신 공고282개 대장 재현, Node23개 통과
- [~] 최신 코드 다운로드45/223, 확인·재검증178개
- [~] 과거 다운로드204/223(91.5%), 최초 다운로드 미확인19개 유지
- [ ] 상시 유입·전체 첨부 텍스트·DB/API/UI·운영 E2E 및 전체 goal 완료

기준 HEAD는 `d2a105e605b81a3be1adb2449fb7e7d71f432ffa`다. 이번에는 응용 Java·테스트 소스·카탈로그·프로필·Flyway V85·규칙·운영 설정을 변경하지 않았다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수나 새 운영 조회 결과가 아니다. 최소1파일 다운로드 이력 비율과 전체 개발 완료율을 구분한다. HWP 추출기1.0.16 추가 고도화는 보류한다.

## 실제 파일 재검증

2026-10-01 01시57분 KST 관측. 기존 수집 전용 그룹8개에 속한9표본을 한 JVM에서 순차 실행했다. 표본별 상한6요청·23MiB, 합계54요청·207MiB를 유지했다. 본문 포함 요청 예약36회·바이트 예약21,460,367byte였다. 예약량은 실제 wire 요청 수·전송량과 다르다.

| 수집원 / 공고 | 본문 | 실제 유효 파일 | 별도 오류 |
| --- | --- | --- | --- |
| 김해109947 | 44자 | HWPX1개 / 148680byte | ATTACHMENT_LINK_UNRESOLVED, 전체 발견 미완료 |
| 창녕46407 | 64자 | HWP1개 / 82432byte | 없음 |
| 사천2106170 | 70자 | HWPX1개 / 89353byte | 없음 |
| 하동45193 | 402자 | HWPX1개 / 156622byte | 없음 |
| 거창47689 | FETCH_FAILED | 없음 | BODY_SELECTOR_CHANGED, TITLE_CONFIRMATION 실패 |
| 남해35694 | 286자 | 없음 | 발견한1파일의 TLS_FAILED |
| 산청164021 | 84자 | HWPX2개 / 514232byte | 없음 |
| 의령35341 | 222자 | HWP1개 / 106496byte | 없음 |
| 거제67219 | 248자 | HWP1개 / 121856byte | 없음 |

Gradle 관측 태스크는 **9개 중8개 통과·1개 실패, 종료코드1**이다. 거창의 상세 제목 확인 실패를 숨기지 않았다. 실패 뒤에도 나머지 표본을 실행하여 유효 파일을 보존했다. 남해는 파일 실패를 보고서에 기록하는 관측 테스트가 통과했을 뿐 다운로드 성공이 아니다. 김해도 발견 경고가 있으므로 전체 파일 집합 성공으로 표현하지 않는다.

보고서9개: `build/reports/attachment-regional-collection/*-GYEONGNAM-RECHECK-20261001.json`. 모두 임시 원본 정리true·운영쓰기0, 추출·정책 QA·기대값 승인false다. 거창은 다운로드하지 않았으므로 원본 정리true를 파일 성공으로 해석하지 않는다.

- 인벤토리 SHA-256: `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`
- 관측 실행 클래스 SHA-256: `a31012696a334a6bc3360bf0c34fe26bb9998f8e1495e34c9cead81d871a2b9a`
- 이전 영수증418개 보존, 새9개 추가. 기존282공고 중 해당9개 최신 관측만 갱신하며 동일 표본을 중복 공고로 추가하지 않는다.
- 현재 코드 다운로드38→45개, 오류 포함 수집원9→12개. 성공과 오류 수는 중복 가능하다. 엄격3표본 전체 집합 Gate는0/223으로 별도 유지한다.

## 원천 사이트 진단

직접 공개 HTTP 진단은9요청이다(천안 GET2/POST1, 서대문 GET3, 거창 GET3). 요청별12초·2MiB, TLS 검증·수집기 User-Agent·자동 redirect 금지를 유지했다. 원문·파일·쿠키를 디스크에 저장하지 않았다. 공식 사이트 한정 검색14질의는 직접 HTTP9회와 별도다. 브라우저 제어·인증 우회·User-Agent 위장은 없다.

1. **천안**: [공식 새올 상세107780](https://eminwon.cheonan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=107780&subCheck=Y)는 HTTP200이며 공식 목록 복귀 함수와 폼을 확인했다. 그 폼의 공개 검색 POST 및 GET은 SocketException으로 끝났다. 제목 조합을 만족하는 새 지원 공고와 파일 성공은 확보하지 못했다. 기존107953의 제목 중단을 보존하고 제목 규칙을 수정하지 않았다. 최초 PowerShell 구문 오류1회는 요청 전 실패였으며 HTTP 횟수에 포함하지 않는다.
2. **서대문**: [공식 상세313956](https://www.sdm.go.kr/news/notice/notice.do?sdmBoardConfSeq=82&mode=view&sdmBoardSeq=313956)의 HWPX4개 링크를 다시 확인했다. 첫 파일의 공식 `path=/board/82`와 인코딩한 `path=%2Fboard%2F82`를 각각 Windows HTTP로 조회했으나 모두 SocketException이었다. HTTP 상태·파일 바이트를 얻지 못했고 단순 인코딩 오류나 Java만의 문제로 확정하지 않았다. 과거 다운로드 실패를 새 성공으로 덮지 않는다.
3. **거창**: [기존 상세47689](https://www.geochang.go.kr/00445/00451.web?amode=view&not_ancmt_mgt_no=47689)는 HTTP200이지만 공고 제목·본문·첨부 대신 **‘요청하신 정보를 찾을 수 없습니다!’**가 표시된다. 현재 응답에는 기존 공고 폼이 없으며 공식 ‘이전 고시공고’ 링크만 확인했다. 삭제·이관·일시 오류 중 어느 원인인지 확정하지 않았다. 선택자를 넓히거나 목록 제목을 대신 넣지 않으며, 공식 대체 상세 또는 새 적격 표본 확인을 후속으로 둔다.

동일 오류의 무제한 재시도는 하지 않는다. 이 진단만으로 새로운 운영 수집 성공을 주장하지 않으며, 원천 오류 때문에 다른 지역의 성공 자료를 폐기하지 않는다.

## 명령 / 검증 / 후속

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GYEONGNAM_NEXT,SACHEON,HADONG,GEOCHANG,NAMHAE,SANCHEONG,UIRYEONG,GEOJE' -PsanebCollectionReportLabel=GYEONGNAM-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

관측 태스크49초·종료1(거창1건 실패), Node23통과, 427기록/282공고·45/223 대장 재현 통과다. 코드 변경이 없어 Java 단위 전체·bootJar는 이번 회차에 재실행하지 않았다. 이전 회차 통과를 이번 회차 결과로 대신하지 않는다.

최초 다운로드 미확인19개는 은평·서대문·송파·검단·울산남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북도·영동·천안·공주·의성·성주다. 이 목록의 모든 지역을 이번에 재조회한 것은 아니다. 최신 코드 재검증과 신규 다운로드 확인을 함께 이어가되, 실패는 별도 오류로 관리한다.

운영 설치·DB·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 대기와 과거 조사 원본·공개 CA 임시 파일 정리 차단은 별도 미해결이다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 이번 다운로드 임시 원본의 정리와 과거 남은 파일 정리 문제를 혼동하지 않는다.
