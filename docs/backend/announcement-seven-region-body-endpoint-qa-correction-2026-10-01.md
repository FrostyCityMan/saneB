# 증평·단양·전북5곳 본문 QA 등록 주소 보정

## 현재 단계 / Gate

- [x] 본문 오류8건의 QA 입력과 DB/서비스 계약 대조
- [x] 확인된7건의 QA 등록 주소 보정, 공고 식별자·프로필·카탈로그 보존
- [x] 실제7곳 본문 AVAILABLE·첨부7개 확보
- [x] 선택 Java186·최종 계약26·관측7·Node23·651기록/282공고 재현 통과
- [!] 광주 남구의 포털 상세 계약과 직접 상세 표본 불일치 유지
- [~] 현재 지문 다운로드196/223·미확보27곳, 과거 포함204/223·최초 미확인19곳
- [ ] 전체 추출·상시 worker·DB/API/UI·운영 E2E 및 장기 goal 완료

기준 HEAD `904a15a4f4d4b79aa8b5ad807ddc93b39cc8cc53`. 이전 회차는 실제 파일 확보와 대장 갱신으로 진전이 있었으며 이번은 본문 QA 입력 불일치를 수정했다. 응용 코드·운영 DB·worker·정책·migration은 변경하지 않았다. HWP 추출 고도화 보류와 성공 파일 보존·오류 분리를 유지한다.

## 확인한 원인과 수정

`AnnouncementSourceServiceImpl`은 `collectionEndpointUrl`이 있으면 그 주소를 본문 요청의 등록 기준 URL로 전달하고, 없으면 `noticeUrl`을 사용한다. `ProviderContentUrlValidator`는 상세 URL이 등록 URL과 같은 호스트인지 확인한다.

증평·단양·완주·진안·무주·장수·임실은 V61에 `eminwon` 수집 endpoint가 명시돼 있지만 QA의 `listUrl`은 다른 호스트의 포털 메뉴 주소였다. 본문 단계가 `DETAIL_HOST_NOT_ALLOWED`로 실패하는 입력 불일치였다. 실제 서비스와 동일하게 기존 V61 endpoint를 QA에 전달했다. host/redirect 검증·사설 주소 차단을 완화하지 않았다.

수정 파일:

- `src/test/java/com/saneb/domain/announcementattachment/qa/AnnouncementAttachmentBbsOfficialObservationTest.java`: 증평·단양 QA 주소.
- `src/test/java/com/saneb/domain/announcementattachment/qa/JeonbukFirstDownloadCases.java`: 전북5곳 QA 주소. 익산·순창의 기존 목록 query 보존.
- `src/test/java/com/saneb/domain/announcementsource/provider/content/RegionalObservationSourceContractTest.java`: 이전4곳+추가7곳의 migration endpoint·카탈로그 식별자·기대값 미승인·동일 호스트·redirect·사설 DNS 계약, 익산/순창 보존, 광주 남구 미해결 경계.

## 실제7곳 결과

| 공고 | 본문 문자 수 | 정상 첨부 바이트 | 별도 상태 |
|---|---:|---:|---|
| JEUNGPYEONG-31159 | 193 | 81,836 | 없음 |
| DANYANG-32263 | 405 | 118,272 | 없음 |
| WANJU-43355 | 455 | 84,480 | 없음 |
| JINAN-33307 | 520 | 81,286 | 없음 |
| MUJU-34488 | 342 | 70,168 | 없음 |
| JANGSU-32491 | 991 | 72,192 | 다른1항목 UNSUPPORTED_NOT_DOWNLOADED |
| IMSIL-33179 | 524 | 68,531 | 없음 |

모두 본문 AVAILABLE이며 정상 첨부는 각1개, 합계7개576,765바이트다. 장수의 미지원 파일은 자동 성공으로 바꾸지 않았다. 본문 수집 성공은 첨부 텍스트 추출·전체 분석·관리자 승인·운영 상시 수집 완료와 다르다. 운영 DB에 저장된 현재 endpoint 값은 이번에 조회하지 않았다.

## 광주 남구는 다른 계약 문제

V61의 `LGS-000068`은 포털 메뉴 URL과 NULL endpoint를 사용한다. 목록 파서 `SAFE_GWANGJU_NAMGU_NOTICE`는 같은 포털 호스트의 `/api/eminwon/gosiView.es?...&not_ancmt_mgt_no={arg:1}`를 만든다. 반면 첨부 프로필/QA 고정 표본45698은 `eminwon.namgu.gwangju.kr/.../OfrAction.do`를 사용한다.

따라서 광주 남구는 단순히 누락 endpoint를 추가하면 끝나는 문제라고 단정할 수 없다. 실제 목록이 만드는 포털 상세 URL과 현재 직접 상세 프로필의 연계가 후속 구현·관측 대상이다. QA만 직접 호스트로 바꿔 성공을 만들거나 운영 목록 수집 설정을 변경하지 않았다. 회귀 테스트는 포털 상세의 동일 호스트 허용과 직접 호스트 차단을 함께 확인한다. 실제 포털 상세 HTTP/본문/첨부는 이번 미검증이다.

## 검증 명령과 결과

첫 명령은 `test --tests ... bootJar`로 실행하여 루트 테스트186개는 통과했지만 하위 `attachment-extractor:test`에는 같은 이름의 테스트가 없어 전체 명령이 실패했다. 테스트를 삭제하거나 완화하지 않고 루트 경로를 명시해 수정했다.

```powershell
.\gradlew.bat --no-daemon :test --tests com.saneb.domain.announcementsource.provider.content.RegionalObservationSourceContractTest --tests com.saneb.domain.announcementsource.provider.content.LocalGovernmentNoticeProviderContentClientTest --tests com.saneb.domain.announcementsource.provider.content.ProviderContentUrlValidatorTest :bootJar
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEUNGPYEONG,DANYANG,WANJU,JINAN,MUJU,JANGSU,IMSIL' -PsanebCollectionReportLabel=ENDPOINT-SEVEN-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :test --tests com.saneb.domain.announcementsource.provider.content.RegionalObservationSourceContractTest :bootJar
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

선택 Java186개(계약26·본문155·URL5), 최종 계약26개, 실제 관측7개, Node23개 모두 통과. 명시 루트 명령은14초 성공이며 이미 실행한 테스트 결과를 UP-TO-DATE로 재사용했다. 최종 계약 보강 후26개는19초 명령에서 다시 실행했다. 실파일 관측은31초, 종료0이다. `bootJar`는 응용 코드 불변으로 UP-TO-DATE 검증이며 신규 운영 배포를 의미하지 않는다. Java 전체 suite와 브라우저·운영 E2E는 실행하지 않았다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.

## 대장·예산·남은 범위

7보고서의 기존 상한42요청·161MiB, 본문 포함 예약28회·15,317,324바이트다. 실제 wire 전송량과 예약량을 구분한다. 보고서는 `build/reports/attachment-regional-collection/`의 `ENDPOINT-SEVEN-RECHECK-20261001` 라벨7개, 원본 정리true·운영쓰기0·추출/정책/기대값 승인false다.

기존644영수증을 보존하고7개를 추가했다. 최신282표본 중7개만 갱신하고275개는 보존한다. 기존 지문은 이전 영수증에 그대로 남겼다. 새 실행 클래스 SHA-256 `230ae19f3fa90c5620da125bfcd137f96fcbbcece3363c1e7cbc0c6a6dbcf87c`; 인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4` 유지. 집계는 regional ledger의 `registeredEndpointSevenBodyCorrectionRun`에 기록한다.

지역 다운로드196/223·남은27곳, 엄격 첨부 집합16/223, 과거 포함204/223·최초 미확인19곳은 변하지 않았다. 이번 작업은 기존 파일 확보 지역의 본문 QA를 복구한 것이므로 새 수집 지역으로 중복 계산하지 않는다. 남은27곳 목록은 직전 `announcement-remaining-regions-collection-recheck-2026-10-01.md`를 따른다.

운영·정책·ENFORCE·재분류·배포 변경 없음. AWS 인증 갱신 대기와 과거 CA/HTML 임시 자원 정리 차단은 별도 미해결이다. 전체 goal은 미완료로 유지한다.
