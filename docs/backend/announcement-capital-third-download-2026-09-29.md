# 남양주·하남·구리 본문 경계 및 첨부 수집 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하고 발견·전송·형식·파싱 오류는 별도 처리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기 1.0.16 개선 보류는 유지한다.

- [x] 세 지역의 공식 목록·제목 검색·상세 경계 확인 및 프로필 연결.
- [x] HWP 2개·PDF 1개·HWPX 1개, 총 306,522byte 실제 다운로드.
- [x] Java 212건 통과·조건부 1건 생략, Node 23건 및 bootJar 통과.
- [x] 193영수증/151공고 재현, 이전 190영수증/148공고 근거 보존.
- [x] 조사 원본 9개 2,425,498byte와 관측 임시 파일 정리.
- [ ] 미등록 113지역 연결 및 등록 후 다운로드 미관측 19지역 후속.
- [ ] 전국 목록 자동 유입·상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 확인 지역 | 88 | **91/223 (40.8%)** |
| 다운로드 미확인 지역 | 135 | **132** |
| 프로필 등록 지역 | 107 | **110** |
| 미등록 지역 | 116 | **113** |
| 등록 후 다운로드 미확인 | 19 | **19** |
| 최신 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 110+기업마당 1=111프로필, 카탈로그 166참조/109대상이다. 신규 expectation은 null이며 기존 승인 기대값 1개는 유지한다. 다운로드 관측률을 전체 개발·운영 완료율로 표현하지 않는다.

## 실제 결과 / 계약

| 지역 / 표본 | 본문 | 첨부 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 남양주 LGS-000091 / 84824 | 218자 | 1 / 1 | HWP 75,264byte |
| 하남 LGS-000100 / 51521 | 44자 | 2 / 2 | HWP 61,440byte·PDF 104,299byte |
| 구리 LGS-000107 / 46706 | 331자 | 1 / 1 | HWPX 65,519byte |

세 표본 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 본문 중간 판정 `TARGET_SUPPORT_CONFIRMED`다. 하남의 44자 본문은 붙임 안내문으로, 첨부 세부조건을 분석한 상태가 아니다. 실제 접수 가능 여부나 운영 공고 활성화는 판정하지 않았다.

- `CapitalThirdNoticePage`: 지역별 공식 host·상세 경로·메뉴 key·공고 ID·목록 분류 인자를 검증한다. 알려진 검색/표시 인자는 저장 identity에 남기되 상세 재요청에서는 제거한다. 중복·미확인 인자와 다른 출처·메뉴는 거부한다.
- 남양주: `selectEminwonWebView.do`, `pk`, 공식 카드 제목 및 표의 내용/첨부파일 셀. `sa1Join=01;02;04;05`, `sc4=2024`는 실제 목록 링크 값이다. `sc4`만 보고 공고 연도가 2024년이라고 단정하지 않는다. 공식 form `nnn`의 action·POST·빈 hidden 3개를 검증한 뒤 `eminwon.nyj.go.kr` FileDownNew.jsp에 파일명·저장명·경로를 POST한다.
- 하남: `selectGosiData.do`, `not_ancmt_mgt_no`, 공식 제목 span 및 첨부 셀. 본문은 `textarea[title=내용][disabled]` 안의 텍스트다. `gourl`의 고정 문자열 3개를 읽어 `eminwon.hanam.gyeonggi.kr` FileDown.jsp에 GET한다. 외부 JavaScript를 실행하지 않는다.
- 구리: `selectGosiNttView.do`, `gosiNttNo`, 제목/내용/파일 셀. `eminwon.guri.go.kr` FileDownNew.jsp GET 링크의 파일명·저장명·경로를 검증한다.
- 남양주/하남 미리보기 인자는 동일 파일의 세 인자와 대조한다. 미리보기는 요청하지 않는다. 상충/미확인 미리보기·추가 링크는 발견 오류로 기록하면서 정상 파일 descriptor를 보존한다. 미지원 확장자는 다운로드하지 않고 별도 descriptor로 보존한다.
- HTTPS443·고정 출처·동일 요청 redirect·파일 signature/MIME·10파일 상한·UNKNOWN 역할을 유지한다. 기존 공유 프로필/다운로드 리터럴 파서를 수정하지 않아 기존 hash에 영향을 주지 않았다.
- `LocalGovernmentNoticeProviderContentClient`는 세 공식 상세 경로에 정확한 본문 경계를 적용한다. 메뉴·metadata·첨부 파일명·푸터를 제외하고 본문 내 표와 제외 문장은 보존한다. 본문 경계가 사라지면 전체 페이지로 대체하지 않는다.
- 기존 `SAEOL_GOSI` 시스템 binding에 연결한다. 고정 상세의 성공은 목록 parser 전체 자동 유입이나 상시 worker 운영 성공의 증거가 아니다.
- migration·DB/API·관리자 UI·추출기·운영 정책·worker 설정 변경 없음.

Profile hash: 남양주 `f35dc8d85097184195c44b01dbf88bb87a36cc1c26e5f8e287db6a5df7c80f90`, 하남 `ab767a754fa6b58ebc2dbf83a08ee5fb340a10bdecf0d9342cfcabd80a0cbaa2`, 구리 `a815a9c26b085fd70b87c3456d3c4d7e71068bd76cd669a028965676c48ee034`. Producer class `b9a75a19d5021b40e5c4cc9693531391147ddd228f3640921b3ca5f1f11796bc`.

## 요청량 / 정리

공식 조사 GET 9회(지역별 목록·검색·상세 각 1회). 요청당 15초/연결 7초/1MiB/자동 redirect 0/TLS 검증 유지. 관측은 각 최대 6요청·23MiB이며 본문을 포함한 예약 상한 합계 13회·7,417,178byte다. 조사 포함 상한 22회는 실제 HTTP 호출 수와 동일시하지 않는다.

조사 원본 9개 2,425,498byte는 대장의 길이·SHA256 대조 후 개별 삭제했다. 관측 임시 원본도 정리했으며 비식별 hash·결과 metadata만 보존한다. 기존 사용자 output·__pycache__와 기존 프로세스는 보존했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalThirdDownloadContractTest' --tests '*CapitalBoardDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=NAMYANGJU,HANAM,GURI' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 보관한 `target-inventory-20260928-receipt.txt`를 사용했다. `SANEB_CAPITAL_THIRD_SURVEY_FIXTURE=true`로 현재 조사 HTML 3건의 제목/첨부 경계도 검사했다. 환경변수는 실행 후 복원했다. 원본 정리 후 해당 검사는 조건부 생략되지만 합성 회귀 테스트는 실행할 수 있다.

첫 실행 **213건 중 212통과·1생략·실패/오류0**, bootJar·실파일 관측 포함 **2분2초 BUILD SUCCESSFUL**. 생략 1건은 이전 양주·군포·여주 원본 정리 후 환경변수를 켜지 않은 `CapitalBoardDownloadContractTest.actualOfficialBoundaries`다. Node 23건 및 193영수증/151공고 재현·diff 검사 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포는 미실행이다. 현재 지역 확대 요청에 브라우저 실행 지시가 없어 브라우저 QA는 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
