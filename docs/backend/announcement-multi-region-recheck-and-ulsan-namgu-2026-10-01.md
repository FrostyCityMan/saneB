# 24수집원 첨부 재검증과 울산 남구 연결 진단

## 현재 단계 / Gate

- [x] 3개 순차 묶음으로24수집원·고정24공고 재검증
- [x] 23수집원에서 유효 파일34개·5,084,127byte 확인
- [x] 아산 상세 실패, 본문 오류4건, 발견 경고2건, 미지원 파일1개 분리
- [x] 기존427영수증 보존, 451기록/282공고 재현, Node23개 통과
- [~] 최신 코드 다운로드68/223, 확인·재검증155개
- [~] 과거 다운로드204/223(91.5%), 최초 미확인19개 유지
- [!] 울산 남구 고시공고 프로필 미연결1개 유지
- [ ] 전체 텍스트·상시 worker·DB/API/UI·운영 E2E 및 전체 goal 완료

기준 HEAD `a86f41b41e0fda1b055ca27cccc9814433260145`. 지역별 발견·다운로드 우선 방침을 유지하고 성공 파일과 오류를 분리했다. 응용 Java·테스트 소스·카탈로그·프로필·규칙·Flyway V85·운영 설정은 변경하지 않았다. 분모223은2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수, 새 운영 조회 결과, 전체 개발 완료율이 아니다. HWP 추출기1.0.16 고도화 보류는 그대로다.

## 실제 수집 결과

각 묶음은 한 JVM에서 순차 실행하며 이전 Gradle 종료 확인 후 다음 묶음을 시작했다. 기존 표본별6요청·23MiB 상한을 유지했다. 전체 허용 상한144요청·552MiB, 본문 포함 예약106회·59,890,203byte다. 예약량과 실제 wire 요청 수·전송량을 구분한다. 다운로드 원본은 각 실행 종료 시 정리됐다.

| 묶음 | 수집원 | 다운로드 확인 | 유효 파일 / 바이트 | 관측 태스크 |
| --- | --- | ---: | ---: | --- |
| PORTAL | 울주·화성·청주·광양·남동·경남도·충남도·옹진 | 8/8 | 9개 / 1119878 | 8통과, 종료0, 43초 |
| SEOUL | 서초·마포·동대문·성북·영등포·양천·관악·서울시 | 8/8 | 15개 / 2856821 | 8통과, 종료0, 42초 |
| CHUNGCHEONG | 음성·논산·당진·청양·진천·홍성·아산·서산 | 7/8 | 10개 / 1107428 | 7통과·아산1실패, 종료1, 42초 |

**관측 태스크 합계는23통과·1실패**다. 태스크 통과는 오류를 정확히 기록한 경우도 포함하므로 다운로드·본문·전체 발견 성공을 대신하지 않는다. 이번 유효 파일34개 중 실패 파일로 기록된 항목은0개지만, 아산은 상세 확인 전에 중단되어 파일 요청 자체가 미실행이고 영등포는 미지원1파일을 다운로드하지 않았다.

보고서24개: `build/reports/attachment-regional-collection/*-{PORTAL,SEOUL,CHUNGCHEONG}-RECHECK-20261001.json`의 세 라벨 묶음. 모두 원본 정리true·운영쓰기0, 추출·정책 QA·기대값 승인false다. 보고서 경로·hash는 영수증 인덱스 및 지역 대장의 `multiRegionCurrentCodeRecheckRun`에서 확인한다.

## 오류는 성공과 별도 관리

| 공고 | 성공 근거 | 남은 오류 |
| --- | --- | --- |
| ASAN-80727 | 없음 | BODY_SELECTOR_CHANGED·TITLE_CONFIRMATION/OBSERVATION_FAILED |
| NONSAN-50928 | 첨부1개151040byte | 본문 DETAIL_HOST_NOT_ALLOWED |
| DANGJIN-57265 | 첨부2개226812byte | 본문 DETAIL_HOST_NOT_ALLOWED·ATTACHMENT_LINK_UNRESOLVED |
| CHEONGYANG-37758 | 첨부2개221970byte | 본문 DETAIL_HOST_NOT_ALLOWED |
| DONGDAEMUN-22587 | 본문972자·첨부2개131833byte | ATTACHMENT_LINK_UNRESOLVED |
| YEONGDEUNGPO-38256 | 본문503자·첨부1개596430byte | UNSUPPORTED_NOT_DOWNLOADED1개 |

본문 오류4건은 아산과 논산·당진·청양이다. 첨부 가능 현황의 오류 수집원16개는 발견/파일 오류 집계이며, 본문만 실패한 수집원을 모두 포함한 전체 장애 수가 아니다. 본문 오류가 있는데 정상 정밀 후보나 전체 분석 완료로 표시하지 않는다. 이번에는 기존 호스트 정책을 완화하지 않았으며 본문 허용 경로 보완은 별도 구현 후 검증할 항목이다.

아산 [기존 공식 상세80727](https://www.asan.go.kr/main/cms/?no=257&m_mode=view&mgt_no=80727)는 Windows HTTP에서도200이지만 예상 공고 제목과 `viewForm`이 없었다. 수신된 HTML은 `customContents` 시작 부분까지였으며 본문·첨부를 확인하지 못했다. 삭제·사이트 개편·서버 렌더링 실패 중 어느 원인인지 확정하지 않는다. 헤더의 HTTP200만으로 공고 성공을 주장하거나 선택자를 넓히지 않는다.

## 울산 남구 미연결 진단

[공식 고시공고 메뉴](https://www.ulsannamgu.go.kr/cop/bbs/selectSaeolGosiList.do)의 진행·종료 메뉴와 공식 새올 iframe 연결을 확인했다. 종료 메뉴가 가리키는 `OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04&list_gubun=Y`는 한 차례HTTP200으로 공개 조회 폼과 목록 조회 함수를 제공했다. 이 응답에는 실제 공고 목록 성공 근거가 없으며 자바스크립트가 후속 공개 POST를 수행하는 구조다.

해당 폼의 종료 공고·소상공인 검색 POST는 SocketException으로 끝났다. 같은 공개 세션/Referer 흐름을 확인하기 위한 새 초기 GET도 SocketException으로 실패하여 후속 POST는 실행되지 않았다. 계속 같은 요청을 반복하지 않는다. 프로필을 추측하여 등록하거나, 별도 일반 소식/청년 소식 게시판을 현재 고시공고 수집원으로 임의 대체하지 않았다. 운영 endpoint·parser·수집 정책은 유지한다.

직접 진단은 울산 남구5요청(GET4/POST1), 아산3GET으로 **총8요청(GET7/POST1)**이다. 각12초·2MiB 상한, TLS 검증·collector User-Agent·자동 redirect 금지를 유지했고 HTML·쿠키·첨부 원본을 디스크에 저장하지 않았다. 공식 사이트 한정 검색2질의는 직접 요청과 별도다. 브라우저·인증 우회는 사용하지 않았다.

## 검증 명령과 대장 보존

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=ULJU,HWASEONG,CHEONGJU,GWANGYANG,NAMDONG,PROVINCE_NEXT,ONGJIN' -PsanebCollectionReportLabel=PORTAL-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEOCHO,MAPO,DONGDAEMUN,SEONGBUK,YEONGDEUNGPO,YANGCHEON,GWANAK,SEOUL' -PsanebCollectionReportLabel=SEOUL-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EUMSEONG,NONSAN,DANGJIN,CHEONGYANG,JINCHEON,HONGSEONG,ASAN,SEOSAN' -PsanebCollectionReportLabel=CHUNGCHEONG-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Node23개 통과, 451기록/282공고 재현 통과다. 코드 변경이 없으므로 Java 단위 전체·bootJar는 이번에 재실행하지 않았다. 이전 회차 결과를 이번 검증으로 대신하지 않는다. 인벤토리 SHA `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA `a31012696a334a6bc3360bf0c34fe26bb9998f8e1495e34c9cead81d871a2b9a`를 유지한다.

이전427영수증을 수정하지 않고24개를 추가했다. 최신 공고는282개 그대로이며 해당24개만 새 관측으로 갱신한다. 현재 코드 다운로드45→68개, 확인·재검증155개, 첨부 오류 포함 수집원12→16개다. 엄격3표본 전체 집합 Gate는0/223으로 별도 유지한다. 과거 다운로드204/223·최초 미확인19개는 바뀌지 않는다.

최초 미확인19개: 은평·서대문·송파·검단·울산남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북도·영동·천안·공주·의성·성주. 모든 지역의 현재 장애를 이번에 재확인한 목록은 아니다.

운영 DB·설정·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 대기와 과거 조사 원본·공개 CA 임시 파일 정리 차단은 별도 미해결이다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 전체 goal은 미완료로 유지한다.
