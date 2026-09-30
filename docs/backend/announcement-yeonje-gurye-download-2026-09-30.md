# 연제·구례 본문 및 첨부 수집 연결

## 단계와 범위

2026-09-30 지역별 첨부 발견·다운로드 우선 단계다. 정상 파일은 수집하고 발견·다운로드·형식·추출 오류는 별도로 남긴다. HWP 추출기 개선, 운영 DB·정책·worker 설정·ENFORCE·기존 데이터 배치·배포는 변경하지 않았다. 이번 요청에 별도 브라우저 검증 지시가 없어 정책상 미실행이다. 전체 장기 goal 완료가 아니다.

- [x] 공식 상세·본문·첨부 영역과 공개 다운로드 절차 연결
- [x] 로컬 계약·회귀·패키징·실파일 다운로드 검증
- [x] 구례 HWPX·PDF 다운로드 및 형식 검증
- [!] 연제 HWPX 표기 파일의 형식 검증 불일치 분리
- [ ] 첨부 텍스트 추출·정책 QA·관리자 최종 검증·운영 E2E

## 공식 경로와 경계

| 수집원 | 처리기 | 고정 공고 | 다운로드 |
|---|---|---|---|
| LGS-000040 연제구 / SAEOL_GOSI | LOCAL_YEONJE_GET_V1 | 43358, 다자녀가구 전세자금 대출이자 지원 | eminwon.yeonje.go.kr의 FileDown.jsp GET |
| LGS-000185 구례군 / SAFE_SAEOL_EMINWON | LOCAL_GURYE_POST_V1 | 25440, 청년 문화복지카드 지원 | eminwon.gurye.go.kr의 FileDownNew.jsp POST |

연제는 공식 목록의 정상 공개 세션과 CSRFToken을 이용한 검색으로 상세 링크를 확인했다. 본문은 `form#detailForm > div.bod_wrap > div.bod_view > div.view_cont`, 첨부는 같은 상세의 `dl.view_file`에 한정한다. `goDownload`의 3개 문자열만 읽고, 5개 인자를 사용하는 미리보기는 공고·파일 식별이 같은 경우에만 중복 링크로 취급한다. JavaScript를 실행하거나 미리보기 URL을 요청하지 않는다.

구례도 정상 공개 세션과 CSRFToken을 이용한 검색을 거쳤다. 본문은 `div.boardGroup > div.board_view > div.board_con`, 첨부는 같은 상세의 `ul.file_down`이다. 현재 DOM의 공식 POST 폼과 3개 파일 필드를 검증하고, 파일 경로의 opaque 값을 임의 해독하지 않는다. 검색 세션·CSRF 값은 파일 다운로드 요청이나 저장소에 전달·기록하지 않는다.

정확한 source/parser/URL 식별, HTTPS·host·path·query/form allowlist와 동일 요청 경계를 유지한다. 미지원 형식은 메타데이터만 남기고, 미해석 링크가 있어도 검증된 descriptor는 보존한다. 파일 수는 최대 10개다. 기존 공통 프로필·다운로드 검증기를 수정해 다른 지역의 지문을 무효화하지 않았다.

## 실제 관측

| 항목 | 연제 43358 | 구례 25440 |
|---|---|---|
| 관측 시각 KST | 2026-09-30 11:43:03 | 2026-09-30 11:43:18 |
| 본문 | AVAILABLE, 112자 | AVAILABLE, 471자 |
| 상세 식별 / 첨부 발견 | 확인 / FOUND, complete=true | 확인 / FOUND, complete=true |
| 파일 결과 | 96,256byte 전송 후 FILE_SIGNATURE / ATTACHMENT_FORMAT_MISMATCH | HWPX 127,471byte + PDF 497,976byte 검증 성공 |
| 수집 상태 | COLLECTION_ONLY_PARTIAL_NOT_APPROVED | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| 요청 예약 상한 / 실제 예약 | 6 / 4 | 6 / 5 |
| byte 예약 상한 / 실제 예약 | 24,117,248 / 2,693,120 | 24,117,248 / 3,033,895 |

연제 파일을 정상 HWPX로 계수하거나 확장자를 바꿔 검증을 우회하지 않았다. 현재 결과만으로 실제 형식을 단정하지 않는다. 구례의 정상 파일은 연제 실패와 무관하게 수집 근거에 반영한다. 전송 원본은 관측 도구에서 정리했다.

사전 조사는 총 10요청(GET 8, POST 2)이다. 상세 HTML은 2MiB 이내이며 curl은 요청 15초·연결 7초·자동 redirect 금지·TLS 검증을 유지했다. 공개 폼 세션은 요청 후 폐기했다. 구례 상세에서 비상업·변경금지 안내 문구가 관측되어 상업 운영 재사용의 승인 근거로 취급하지 않는다. 파일별 적용 범위는 별도 검토 대상이다.

## 집계와 근거

**161→162/223수집원(72.6%) 다운로드 확인, 잔여62→61(미등록35 + 등록 미확인26)**이다. 오류가 있는 수집원은 39→40개이며 정상 파일이 있는 수집원과 중복될 수 있다. 분모는 2026-09-28 15:56:12 KST 활성 수집원 스냅샷이다. 고유 행정구역 수·전체 첨부 추출률·운영 완료율이 아니다.

지역 처리기 186→188개, 기업마당 1개를 합쳐 189개다. 카탈로그는 246공고/188대상, 245개 참조 전용과 기존 기대값 1개이며 신규 기대값 승인은 없다. 기존 294영수증/235공고를 보존하고 296영수증/237공고로 확장했다. 기존 3표본·전체 첨부 Gate는 16충족/207잔여로 별도 유지한다.

- 로컬 보고서: `build/reports/attachment-regional-collection/YEONJE-43358.json`, `GURYE-25440.json`
- 집계: `attachment-collection-receipt-index-2026-09-28.json`, `attachment-collection-regional-ledger-2026-09-28.json`의 `yeonjeGuryeDownloadRun`
- SHA-256·형식·크기·프로필/실행 코드 hash만 근거에 저장하며 본문·파일 원문·세션 값은 복사하지 않는다.

## 검증 및 초기 실패 처리

초기 테스트 112개 중 44개가 실패했다. 테스트용 프로필 목록 중복 등록과 연제의 다른 함수명·5인자 미리보기 파싱이 원인이었다. 중복 등록을 제거하고 연제 전용 선형 파서를 추가했다. 이후 좁은 검증의 루트 테스트는 통과했으나 `test` 명령이 추출기 하위 모듈에 같은 필터를 적용해 전체 명령이 실패하여, 최종 명령은 루트 `:test`로 지정했다. 이 실패를 성공 이력으로 바꾸지 않는다.

최종 실행은 `SANEB_YEONJE_GURYE_SURVEY_FIXTURE=true`, 기존 읽기 전용 inventory receipt를 사용했다.

```powershell
.\gradlew.bat --no-daemon :test --tests '*YeonjeGuryeDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONJE_GURYE' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

Java 272개 실패·생략 0, bootJar와 관측 작업 성공(3분 11초), Node 23개 통과다. 관측 작업 성공은 오류 분리 보고서 생성까지의 성공이며 연제 파일 성공을 뜻하지 않는다. 보관 영수증296개/공고237개 재현 검증도 통과했다. 이번 작업의 Java 프로세스는 종료됐고 기존 사용자 Java 프로세스는 보존했다.

조사 HTML 7개2,423,995byte의 삭제 명령은 실행 정책에서 거부되어 실행하지 못했다. `build/qa-yeonje-gurye-20260930`에만 남아 있으며 저장소에는 추가하지 않는다. 공개 폼의 일시적 세션·CSRF 값이 포함될 수 있어 원문을 출력하거나 문서에 복사하지 않는다. `yeonjeGuryeDownloadRun.originalsRemoved=false`를 유지한다. 이는 관측 도구가 성공적으로 정리한 다운로드 임시 파일과 구분되는 잔여 정리 항목이다.

운영 쓰기 0, 신규 정책·실행 기대값 승인 0, 추출기 실행 0이다. 다음 단계는 남은 61개 중 미연결 35개를 우선 확장하고, 연결된 26개 오류·미확인 대상은 별도 후속으로 관리한다.
