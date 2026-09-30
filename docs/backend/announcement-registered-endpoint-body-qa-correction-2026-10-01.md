# 지역별 본문 QA 기준 주소 정합성 보정

## 단계와 Gate

- [x] QA 입력을 V61의 등록 수집 endpoint와 일치시킨다.
- [x] 호스트·리다이렉트·사설 주소 차단 계약을 유지한다.
- [x] 논산·당진·청양·화순의 실제 본문과 첨부를 재확인한다.
- [x] 이전 실패 기록과 정상 다운로드를 함께 보존한다.
- [~] 나머지 지역의 첨부 수집·현재 코드 재검증을 계속한다.
- [ ] 운영 DB endpoint 조회·상시 worker·운영 E2E는 이번 검증에 포함하지 않는다.

## 원인과 변경 범위

기준 HEAD는 `c999628325ee23c346779080284c9104d1f47a0a`다. 네 지역의 관측용 테스트가 본문 검증 기준 주소로 대표 포털을 전달했다. 실제 서비스 `AnnouncementSourceServiceImpl`은 이미 `collectionEndpointUrl`을 우선하며, 값이 없을 때만 대표 주소로 대체한다. 따라서 이번 변경은 응용 코드 수정이나 운영 장애 복구가 아니라 **QA 입력을 기존 서비스·V61 계약에 맞춘 것**이다.

`AnnouncementAttachmentBbsOfficialObservationTest`의 논산·당진·청양과 `HwasunDownloadCases`의 화순 기준 주소만 각 공식 새올 endpoint로 바꿨다. `RegionalObservationSourceContractTest` 9개는 migration과 입력의 일치, 기존 공고 식별자·미승인 상태 보존, 다른 호스트와 사설 DNS 차단, 음성의 기존 목록 검색 인자 보존을 검증한다.

보안 검증기, 응용 Java, 프로필, 카탈로그, 기존 migration, 운영 DB·설정·정책은 변경하지 않았다. 운영 DB의 현재 endpoint 값은 조회하지 않았다.

## 실제 수집 결과

| 지역·고정 공고 | 본문 | 정상 첨부 | 바이트 | 별도 오류 |
|---|---:|---|---:|---|
| 논산 50928 | 656자 | HWP 1개 | 151,040 | 없음 |
| 당진 57265 | 516자 | HWPX 2개 | 226,812 | ATTACHMENT_LINK_UNRESOLVED |
| 청양 37758 | 218자 | HWPX 2개 | 221,970 | 없음 |
| 화순 39230 | 542자 | HWPX 1개 | 134,544 | 없음 |

네 본문 모두 `AVAILABLE`, 본문 판정 `ACCEPTED/TARGET_SUPPORT_CONFIRMED`다. 정상 첨부는 합계 **6개·734,366바이트**다. 당진은 정상 파일을 보존하되 첨부 발견 전체 완료로 표시하지 않는다. 본문 판정은 최종 운영 공고 승인이나 자동 활성화를 뜻하지 않는다.

총 요청 예약은 18/24회, 바이트 예약은 9,151,545/96,468,992다. 관측 보고서의 원본 정리는 모두 true, 운영 쓰기는 0이다. 첨부 추출·정책 QA·기대값 승인·운영 E2E는 false를 유지한다.

보고서는 `build/reports/attachment-regional-collection/*-BODY-ENDPOINT-20261001.json`에 있으며, 재현용 비식별 결과는 receipt index에 추가했다. producer class SHA-256은 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`다.

## 검증 명령과 결과

```powershell
.\gradlew.bat --no-daemon :test --tests '*RegionalObservationSourceContractTest' --tests '*ProviderContentUrlValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HWASUN -PsanebCollectionReportLabel=BODY-ENDPOINT-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

최종 선택 Java 169개 통과·실패/생략 0, bootJar 성공이다. 앞선 논산·당진·청양 3건과 최종 화순 1건의 실제 관측을 합쳐 4건을 확인했다. 앞선 실행의 계약 테스트명은 변경 전 `ChungcheongObservationSourceContractTest`였으며, 화순을 추가하면서 지역 공통 명칭으로 바꿨다. Node 23개 통과, 455기록/282공고 재현을 확인했다. 이전 451기록은 그대로 보존한다.

## 집계와 남은 업무

- 과거 포함 적격 첨부 다운로드: 204/223곳(91.5%), 최초 다운로드 미확인 19곳.
- 현재 실행 지문 기준 다운로드: 68/223곳, 확인·재검증 필요 155곳. 이번 네 곳은 기존 성공 지역이므로 지역 수를 늘리지 않는다.
- 개별 오류가 있는 수집원 16곳은 성공 수와 중복될 수 있다.
- 현재 코드의 엄격한 전체 첨부 검증 Gate는 0/223이며, 과거 엄격 검증 기록은 별도 보존한다.
- 운영 인벤토리는 2026-09-28 스냅샷이며 현재 운영 재조회 결과가 아니다.

HWP 추출 고도화는 보류하고 지역별 수집을 우선한다. AWS 인증 갱신·서버 검증, 운영 worker/DB/API/UI와 최종 승인 흐름은 별도 Gate다. 브라우저 검증은 현재 사용자 정책상 미실행이다. 이번 다운로드 원본 정리 결과가 과거 삭제 차단된 임시 CA·HTML 자원의 정리까지 의미하지 않는다.
