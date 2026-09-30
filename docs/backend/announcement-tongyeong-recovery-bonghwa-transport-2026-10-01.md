# 통영 첨부 재확보와 봉화 연결 단계 진단

## 현재 단계 / Gate

- [x] 직전 회차의 본문 QA7곳 복구는 진전으로 분류
- [x] 통영·봉화의 기존 시간 초과 단계 대조
- [x] 통영 공식3단계 요청의 별도 전송 진단과 실제 Java 수집 성공
- [x] HWP165,888바이트·동일 해시 확인, 미해석 링크 경고 보존
- [x] Node23·652기록/282공고 재현 통과
- [!] 봉화 포털은 정상이나 첨부 서버 HTTPS 연결 실패 유지
- [~] 현재 지문 첨부197/223(88.3%)·남은26곳
- [ ] 추출·상시 worker·DB/API/UI·운영 E2E와 전체 goal 완료

기준 HEAD `6570b20eef5e44492a8eaaf0c001a15accc65bf0`. 응용/테스트 코드·프로필·카탈로그·migration·운영 설정은 변경하지 않았다. 분모223은 2026-09-28 활성 수집원 스냅샷이며 전체 개발 완료율이 아니다. HWP 추출 고도화 보류와 개별 오류 분리 방침을 유지한다.

## 기존 실패와 이번 진단

통영49251의 이전 관측은 본문247자 AVAILABLE, 상세 식별 성공, 파일 전송 두 번째 단계에서 TRANSPORT_TIMEOUT이었다. `downloadTrace`는3단계 중1전송 완료·2전송 시도를 기록했다. 봉화32956은 본문1,334자 AVAILABLE, 상세 식별 성공이지만 HWPX/HWP 직접 다운로드2개 모두 전송 미완료였고 미지원1항목도 있었다.

이번 별도 진단은 Windows .NET HttpClient로 공식 고정 상세와 페이지에 실린 파일만 순차 조회했다. collector User-Agent, TLS 검증, redirect0, cookie 저장0, 연결5초·요청12초, 상세/파일 각2MiB·기간 폼32KiB·기간 응답256바이트 상한을 유지했다. 원문·폼·파일은 메모리에서만 처리하고 출력은 상태·길이·해시·signature prefix에 제한했다. Java 수집 성공으로 대체 집계하지 않고 전송 대조 자료로만 사용했다.

| 진단 요청 | 결과 | 수신 바이트 | 경과 |
|---|---|---:|---:|
| 통영 포털 상세 GET | HTTP200 | 183,778 | 4,472ms |
| 통영 FileDownNewPbs.jsp GET | HTTP200, 공식 중간 폼 | 2,596 | 307ms |
| 통영 기간 조회 POST | HTTP200, EmptyYMD | 16 | 271ms |
| 통영 FDSendNewPbs.jsp POST | HTTP200, HWP signature | 165,888 | 290ms |
| 봉화 포털 상세 GET | HTTP200 | 239,090 | 139ms |
| 봉화 HWPX 파일 GET | 연결 대기 취소 | 미확보 | 5,025ms |
| 봉화 HWP 파일 GET | 연결 대기 취소 | 미확보 | 5,011ms |

HTTP 요청7회이며 정상 응답 합계591,368바이트다. 원본 디스크 기록0. 봉화 첨부 호스트 `eminwon.bonghwa.go.kr`는 DNS 주소1개가 반환됐으나 별도 TCP443 연결1회도5,018ms에 취소됐다. 이 TCP 진단의 HTTP 요청은0이다. 현재 이 PC에서 연결이 안 된다는 증거이며 서버 영구 장애·차단 원인·운영 서울 서버에서도 동일함을 확정하는 증거는 아니다. 반복 동일 요청, HTTP 다운그레이드, TLS 우회, 타임아웃 증가는 하지 않았다.

## 통영 실제 수집 코드 재검증

별도 전송 진단이 정상 응답한 다음 기존 Java 수집기로 한 번 재검증했다. 공식 GET→기간 POST→파일 POST를 모두 수행했고 `transportInvocations=3`, `completedTransports=3`, `phase=COMPLETE`를 기록했다. 본문 AVAILABLE, HWP165,888바이트, SHA-256 `3df1f725f507ad3f63f65c8e36191dd1a3acf2797cdd1c1cc51659b2be6beb69`로 별도 진단과 일치했다.

`ATTACHMENT_LINK_UNRESOLVED` 경고는 그대로 보존하여 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 파일 확보를 첨부 집합 완전성·추출 품질·정책 승인·자동 활성화로 바꾸지 않았다. 이번 재확보는 코드 변경 없이 확인된 결과이며 이전 시간 초과의 근본 원인은 미확정이다.

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=TONGYEONG' -PsanebCollectionReportLabel=TONGYEONG-TRANSPORT-RECOVERY-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 관측1통과, Gradle 종료0·36초. Node23통과, 652영수증/282최신 공고 재현 통과. 코드 변경이 없어 Java 단위 전체·bootJar는 재실행하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다.

## 대장·예산·남은 범위

보고서는 `build/reports/attachment-regional-collection/TONGYEONG-49251-TONGYEONG-TRANSPORT-RECOVERY-20261001.json`, 집계는 regional ledger의 `tongyeongRecoveryBonghwaTransportRun`이다. 기존651영수증을 보존하고1개를 추가했다. 최신282표본 중 통영1개만 갱신하고281개는 그대로다. 봉화는 파일 실패를 덮어쓰지 않았고 native/TCP 진단을 다운로드 근거로 등록하지 않았다.

Java 상한6요청·23MiB, 본문 포함 예약6회·2,454,068바이트. 별도 진단7HTTP와 TCP1회는 이 예약량 밖이다. 합산 HTTP 요청/예약 상한13회이며 native 상한은 상세2건/파일3건 각2MiB+32KiB+256바이트다. 전송 실패의 미수신량을 정상 바이트로 세지 않는다. 관측 원본 정리true·운영쓰기0·추출/정책 QA/기대값 승인false다.

실행 클래스 지문 `230ae19f3fa90c5620da125bfcd137f96fcbbcece3363c1e7cbc0c6a6dbcf87c`, 인벤토리 지문 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4` 유지. 현재 지문 다운로드196→197, 미확보27→26이다. 엄격 첨부 집합16/223, 오류 수집원33, 과거 포함204/223·최초 미확인19는 유지한다.

남은26곳은 최초 미확인19곳과 최신 실패7곳(강남·미추홀·아산·금산·봉화·남해·거창)이다. 봉화 연결 문제는 서버 환경 진단 후보이나 AWS 인증 갱신 대기와 분리한다. 광주 남구 포털/직접 상세 연계, 기존 본문/파일 예외, 전체 worker/DB/API/UI/운영 Gate도 남아 있다.

운영 DB·worker·정책·ENFORCE·재분류·배포 변경 없음. 과거 CA/HTML 임시 자원 정리 차단은 별도 미해결이며 이번 원본 미기록·정리 결과로 과거 자원까지 정리됐다고 보고하지 않는다.
