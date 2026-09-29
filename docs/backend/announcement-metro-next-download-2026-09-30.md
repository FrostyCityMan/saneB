# 광주 남구·대전 중구 첨부 수집과 본문 host 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 상시 worker·DB/API/UI·운영 E2E를 포함하는 전체 장기 goal은 미완료다.

- [x] 광주 남구·대전 중구 본문 선택·첨부 프로필 2개 구현.
- [x] 광주 남구 PDF1개/HWPX1개, 대전 중구 HWPX1개, 총 485,364byte 실제 다운로드·형식 검증.
- [x] 광주 남구 본문 DETAIL_HOST_NOT_ALLOWED를 첨부 성공과 별도 보존.
- [x] 실제 HTML 계약, Java 235통과/조건부 1생략, Node 23검사, bootJar 검증.
- [x] 기존 243영수증/199공고 보존, 245영수증/201공고 근거 재현.
- [x] 조사 HTML 12개 4,394,026byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 66지역 연결, 등록 후 다운로드 미확인 33지역 후속.
- [ ] 광주 서구 리다이렉트 목적지 확인, 유성 공식 상세·첨부 연결.
- [ ] 전국 자동 목록 유입·첨부 텍스트 추출·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성 223지역이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 122 | **124/223 (55.6%)** |
| 다운로드 미확인 | 101 | **99** |
| 프로필 등록 지역 | 155 | **157** |
| 미등록 지역 | 68 | **66** |
| 등록 후 다운로드 미확인 | 33 | **33** |
| 첨부 발견·파일 오류 관측 지역 | 41 | **41** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 157+기업마당 1=158프로필, 카탈로그 213공고/156대상이다. 신규 expectation=null이며 기존 승인 기대값 1개를 보존했다. 첨부 오류 지역 수에는 본문 host 오류나 표본 미선정 목록 조사 문제가 포함되지 않으므로 전체 오류 지역 수로 표현하지 않는다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다.

## 실제 관측

2026-09-30 01:17 KST 로컬 Java 수집 경로의 고정 공개 표본 관측이다.

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 광주 남구 LGS-000068 / 45698 | FETCH_FAILED / DETAIL_HOST_NOT_ALLOWED, REVIEW_REQUIRED | HWPX1개 61,317byte·PDF1개 357,199byte, 총418,516byte 검증 통과 |
| 대전 중구 LGS-000073 / 46404 | 68자, 대상·지원 조합 확인 | HWPX1개 66,848byte 검증 통과 |

광주 남구 표본은 `2026년 「청년 1인가구 주거 안심물품 지원」대상자 모집 공고`, 대전 중구 표본은 `2026년 대전광역시 중구 소상공인 특례보증 지원사업 공고`다. 고정 표본 기술 검증이며 현재 모집 여부·운영 등록·관리자 확정을 뜻하지 않는다. 대전 중구 본문 ACCEPTED도 규칙상 후보 판정이다.

광주 남구의 공식 메뉴가 제공하는 eminwon iframe·상세·첨부는 확인됐지만, 본문 수집기의 등록 대표 홈페이지 host 제한에 의해 본문 HTTP 요청이 차단됐다. 본문 선택자 구현과 실제 HTML 계약은 통과했으나 운영 수집 경로의 본문 확보 성공으로 표현하지 않는다. host 제한을 우회하거나 운영 source 설정을 변경하지 않았다. 첨부 프로필은 실측된 공식 eminwon host에 한정되어 정상 파일 2개를 수집했다. 제목·본문·첨부의 통합 완료가 아니라 파일 수집 가능 근거다.

프로필 지문:

- 광주 남구 `92f619f3895e262d90a7a650c17d025c486ae47a8c6692026766df7408196319`.
- 대전 중구 `bc79b270b2b2c8a3b3d8a6e3333382f2d3bd289e8448fa23ff3e137b93f47ece`.

파일별 SHA256·관측 시각은 수집 근거 인덱스에 연결된 영수증을 참조한다. 두 관측 모두 파일 원본 삭제를 확인했다. 추출·첨부 역할·정책 QA·기대값 승인·운영 E2E는 검증하지 않았으며 첨부 역할은 UNKNOWN이다.

## 구현과 후속 지역

`MetroNextNoticePage`는 광주 남구의 form1/tstyle_view, 대전 중구의 program--contents/bbs--view에서 제목·본문·첨부를 분리한다. 담당자·부서·메뉴·파일 이름을 본문에 섞지 않는다. 광주 남구는 공식 첨부 칸 전체를 기존 `SaeolGetAttachmentDiscoveryProfile`에 전달해 GET 다운로드 엔진을 재사용한다. 알 수 없는 요소를 제거해 첨부 없음으로 바꾸지 않는다.

대전 중구는 공식 fileForm의 고정 HTTPS 목적지와 세 필드 구조를 검증하고 `fn_egov_downFile`의 세 문자열 인자만 읽는다. JavaScript는 실행하지 않는다. 같은 파일 인자의 미리보기 링크는 제외하고 호출하지 않는다. 서버 고정 host/path·source/parser 결합·동일 요청 redirect·파일명/저장 경로·10파일 상한을 유지한다. 정상 descriptor는 다른 링크 오류와 별도로 보존한다.

광주 서구 LGS-000067 공식 목록은 HTTP302·0byte였으며 목적지를 확인하지 못해 따라가지 않았다. 오류 원인을 단정하거나 수집 성공으로 세지 않는다. 유성 LGS-000075는 제목 소상공인 검색에 결과가 없었으나 지원 검색에서 10개 공고를 확인했다. 상세·파일 요청은 아직 하지 않았다. 다음 조사에서는 공식 목록에서 확인한 지원 공고 52705 등의 상세 구조를 확인할 수 있다. 목록 후보에는 행정성 공고도 있어 제목 분류를 반드시 적용해야 하며 검색 결과를 지원 후보 확정으로 보지 않는다. 기존 조사3회는 누적 요청에 포함한다.

기존 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다. 고정 상세 표본 성공은 전국 목록 자동 유입이나 상시 처리 완료를 대신하지 않는다.

## 실행 명령 / 결과 / 정리

`SANEB_METRO_NEXT_SURVEY_FIXTURE=true`에서 신규 계약·본문 테스트가 31초에 통과했다. 이후 같은 fixture와 기존 읽기 전용 inventory receipt를 사용했다.

```powershell
.\gradlew.bat :test --tests '*MetroNextDownloadContractTest' --tests '*IncheonThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GWANGJU_NAMGU,DAEJEON_JUNGGU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

Gradle 1분 46초 exit 0. 일반 Java 테스트 236개 중 235통과/1생략, 실패 0개다. 생략은 이전 검단·영종 원본 정리 후 비활성인 조건부 fixture 테스트이며 이번 광주 남구·대전 중구 실제 HTML 테스트는 실행했다. Node23개·bootJar·245영수증/201공고 재현 검증 통과.

조사12회(광주 남구5, 대전 중구3, 광주 서구1, 유성3). 관측 요청 예약 상한 합계9회·4,869,861byte이며 조사 포함 요청 상한21회다. 공고별 한도6요청·23MiB, 조사별 한도15초·2MiB였다. 조사 HTML12개 4,394,026byte의 길이·해시 대조 후 개별 삭제했고 파일 관측 원본도 삭제 확인했다. 원문·개인정보·세션 값은 문서/대장에 복사하지 않았다.

운영 DB/설정/정책/worker 변경, ENFORCE, 재분류, 배포는 수행하지 않았다. 브라우저 검증은 현재 사용자 정책에 따라 미실행이다. 다음은 유성 등 미등록 지역 연결이며 본문 host 오류는 첨부 확대와 분리해 후속으로 유지한다.
