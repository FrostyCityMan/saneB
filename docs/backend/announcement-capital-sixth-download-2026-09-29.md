# 시흥·안산 연결 및 양평 첨부 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하며 본문·발견·다운로드·형식·파싱 오류를 따로 기록한다. 모든 오류 해결이나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 시흥·안산 공식 본문/첨부 경계와 프로필 2개 연결. 양평 기존 프로필 재사용.
- [x] 시흥 HWP 1개, 양평 HWPX·PDF 각 1개, 합계 471,810byte 다운로드 확인.
- [x] 안산 MIME 불일치·양평 미지원 형식·제목 조합 미충족을 별도 기록.
- [x] Java 293통과·조건부 1생략, Node 23통과, bootJar·수집 전용 관측 통과.
- [x] 204영수증/162공고 재현 및 기존 199영수증/157공고 보존.
- [x] 조사 원본 12개 3,591,383byte와 관측 임시 원본 정리. 길이·SHA256 보존.
- [ ] 미등록 105지역 연결, 등록 후 다운로드 미확인 21지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

대상 분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 지역 | 95 | **97/223 (43.5%)** |
| 다운로드 미확인 지역 | 128 | **126** |
| 프로필 등록 지역 | 116 | **118** |
| 미등록 지역 | 107 | **105** |
| 등록 후 다운로드 미확인 | 21 | **21** |
| 발견·파일 오류 관측 지역 | 25 | **27** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 118+기업마당 1=119프로필, 카탈로그 174참조/117대상이다. 새 기대값은 null이며 기존 승인 기대값 1개는 유지한다. 위 비율은 전체 개발·운영 완료율이 아니다.

## 실제 결과

| 지역 / 공고 | 본문 | 파일 결과 |
|---|---|---|
| 시흥 LGS-000095 / 82127 | 281자 확보 | HWP 1개 117,248byte 성공 |
| 안산 LGS-000092 / 1660335 | 230자 확보 | 1개 발견, 110,080byte 응답 수신 후 MIME 불일치로 실패 |
| 양평 LGS-000110 / 312241 | 288자 확보 | HWPX 1개 116,740byte 성공 |
| 양평 LGS-000110 / 311846 | 312자 확보 | PDF 1개 237,822byte 성공·미지원 첨부 1개 다운로드 안 함 |
| 양평 LGS-000110 / 311507 | 요청 안 함 | 제목 COMBINATION_NOT_MATCHED, 외부 요청 0회 |

- 안산은 추가 GET 1회에서 HTTP200·110,080byte·`application/unknown;charset=UTF-8`를 확인했다. 원본은 저장하지 않았다. 공통 validator는 이 MIME을 허용하지 않으므로 성공 집계하지 않는다. 바이트 수나 hash만으로 수집 성공을 주장하지 않고 기관별 MIME 대응 후속으로 남긴다. 공통 보안 검사나 허용 MIME 목록은 변경하지 않았다.
- 양평 311507의 기존 status 문자열은 `TITLE_EXCLUDED_NOT_FETCHED`, 원인은 제목 대상·지원 조합 미충족이다. B그룹 제외라고 단정하지 않는다. 제목 정책을 우회해 파일을 받지 않았다.
- 양평은 기존 프로필이 있으나 이 수집 근거 대장에 표본이 없던 지역이었다. 기존 고정 3공고를 재사용했으며 parser·프로필 hash·worker 기대값을 변경하지 않았다.
- 과천·가평은 사전 공식 목록 조회 후 대장의 비활성 상태를 확인했다. 과천 MANUAL_ONLY와 가평 비활성을 유지하고 추가 수집·프로필 연결·운영 활성화를 하지 않았다. 활성 223지역의 완료나 잔여 감소에 포함하지 않는다.
- 시흥의 기존 HTTP 포털 진입과 공식 게시판의 메뉴/상세 경로가 다르다. 관측한 HTTPS 포털 이동 후 상세 GET을 검증했으나 운영 목록 parser의 신규 공고 유입과 저장 identity 연결은 별도 Gate다. 안산도 고정 공식 표본의 상세 GET/첨부 발견 검증이며 상시 유입 검증을 대신하지 않는다.

## 구현 계약

- `CapitalSixthNoticePage`: HTTPS443·정확한 host/path·mId 또는 bbs_code·공고 ID와 알려진 검색 인자만 허용한다. 시흥은 `detailForm > bod_wrap > bod_view`, 안산은 GET `aform`의 `p-wrap.bbs.bbs__view > table.p-table`에서 제목/내용/파일 셀을 구분한다. 경계 변경 시 페이지 전체를 본문으로 사용하지 않는다.
- 시흥: `view_file`의 `div#updateFileList > ul > li` 안 고정 goDownload 3리터럴을 읽어 공식 `eminwon.siheung.go.kr/emwp/jsp/ofr/FileDown.jsp` GET으로 연결한다. window.open·sleep 등 원격 스크립트는 실행하지 않는다.
- 안산: 공식 GET `aform`의 bbs_code·bbs_seq·빈 file_id와 첨부 셀을 검증한다. fnFileDownLoad의 영숫자 파일 ID만 `/common/file/FileDown.do?file_id=...`로 연결한다. 같은 ID의 미리보기는 보조 링크로만 구분하고 요청하지 않는다. 다른 form 값·계정 정보는 보내지 않는다.
- 정상 descriptor를 보존하며 미확인 링크·미리보기 충돌·미지원 확장자를 별도 기록한다. GET·고정 출처·동일 요청 redirect·10파일 상한·UNKNOWN 역할·signature/MIME 검사를 유지한다.
- 기존 공유 프로필·Flyway·DB/API·관리자 UI·추출기·운영 정책·worker 설정 변경 없음. 운영 게시·ENFORCE·배치·배포 없음.

Profile hash: 시흥 `322e1bcba46d62483cecbbaab6a7f6102602cb173d28f22bf5227d5929fb320f`, 안산 `c03d4d363352dffd3b7fe9966acfac11f9c32e3ed505af7f6d18b6ba40b93c9a`, 양평 기존 `5d89388d094f4e1acad26fec725b36d7c41f65e112cf9ea60fa0b29e10a23a02`.

Producer class: `7fc6abb90cefd456272ae141d05bcc0e3b6fc826676877337c50d97385f0700d`.

## 요청량 / 정리

공식 조사 GET 13회: 시흥 4·안산 4(파일 MIME 진단 1 포함)·양평 3·과천 1·가평 1. 요청당 15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 조사 HTML 12개를 길이·SHA256 대조 후 개별 정리했고 MIME 진단 파일은 저장하지 않았다. 관측 보고서의 임시 원본 정리도 확인했다.

시흥·안산 관측 상한은 각각 6요청·23MiB, 양평은 기존 표본별 44요청·80MiB다. 실제 예약은 전체 본문 포함 16회·10,420,482byte, 조사 포함 예약 상한은 29회다. 예약 수와 실제 HTTP 호출 수를 동일시하지 않는다. 사용자 output·__pycache__·기존 프로세스는 보존했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalSixthDownloadContractTest' --tests '*CapitalFifthDownloadContractTest' --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SIHEUNG,ANSAN,YANGPYEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

첫 실행은 새 테스트의 빈 POST 입력 2건이 공통 Request 생성 단계에서 거부되어 실패했다(1분25초, 외부 관측 미실행). 공통 거부 검증을 명시하고 비어 있지 않은 POST로 프로필의 메서드 거부를 검증하도록 수정했다. 재실행은 Java XML 294건 중 293통과·1조건부 생략·실패/오류0, bootJar UP-TO-DATE, 외부 수집 전용 관측 포함 1분34초 성공이다. 안산 MIME 실패와 양평 미지원 형식은 결과에 남으므로 태스크 성공을 전체 첨부 성공으로 표현하지 않는다.

Node 23통과, 204영수증/162공고 재현 통과. 전체 테스트·새 운영 조회·AWS·운영 배포·텍스트 추출은 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청 정책에 따라 미실행이다. 조사 fixture는 이번에 통과했으며 원본 정리 후 기본 테스트에서는 조건부 생략된다.
