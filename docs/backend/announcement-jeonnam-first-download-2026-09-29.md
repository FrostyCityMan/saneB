# 전남권 5지역 공식 첨부 연결과 실제 다운로드

## 현재 단계 / Gate

사용자 지시대로 전 지역 첫 첨부 수집을 우선한다. 파일 수집 오류와 파싱 오류를 따로 기록하고, 개별 오류 해결·추가 표본이 다음 지역 연결을 막지 않도록 한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 Goal의 본문·첨부 구간 분석, worker DB/API, 최종 관리자 검증, 운영 E2E 범위는 유지한다.

- [x] 목포·여수·나주·강진·무안 공통 처리기 1개와 기관 프로필 5개 추가.
- [x] 5공고에서 HWP4개·HWPX1개 다운로드 및 signature 확인.
- [x] 5공고 본문 확보. 본문 정제 품질·최종 정책 승인과는 구분.
- [!] 장흥 공식 목록 HTTP400, 보성 이번 검색 결과 없음은 별도 조사 기록으로 유지.
- [x] 로컬 계약130건·Node23건·bootJar·실파일5건·126영수증/98공고 재현 검증.
- [ ] 나머지 지역 연결 및 전체 Goal의 분석·운영·E2E Gate.

지역 분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷** 기준이다. 이번 실행은 운영 DB·정책·설치·worker 설정을 조회하거나 변경하지 않았다.

| 지표 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 관측 지역 | 43 | **48** |
| 다운로드 미관측 지역 | 180 | **175** |
| 로컬 프로필 등록 지역 | 53 | **58** |
| 미등록 지역 | 170 | **165** |
| 등록 후 다운로드 미관측 | 10 | **10** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

48/223(약21.5%)는 첫 파일 다운로드 관측률이며 전체 개발 진행률이나 운영 적용률이 아니다. 기존 3표본 Gate는 유지하되 실행 우선순위에서 다음 지역 진입을 막지 않는다. 현재 등록 프로필 근거상 오류 지역9개도 그대로다. 장흥·보성 같은 미등록 지역의 조사 결과를 모두 포함하는 오류 집계는 아니다.

## 실제 관측

| 지역 / source | 고정 공고 | 첨부 파일 | 본문 문자 수 |
|---|---|---|---:|
| 목포 / LGS-000178 | 54011 소상공인 융자금 이차보전 정정공고 | HWP 155,136byte | 9,145 |
| 여수 / LGS-000179 | 79153 소상공인 융자금 이차보전 변경 공고 | HWPX 102,353byte | 9,778 |
| 나주 / LGS-000181 | 40288 소상공인 이차보전 지원사업 | HWP 180,736byte | 9,225 |
| 강진 / LGS-000190 | 28097 소상공인 융자금 이차보전 지원사업 | HWP 109,056byte | 3,494 |
| 무안 / LGS-000193 | 33617 소상공인 디지털 전환 지원사업 | HWP 165,376byte | 3,586 |

총 **5파일·712,657byte**. 5건 모두 수집 단계 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 첨부 텍스트 추출·구간 분석·운영 저장·관리자 최종 승인은 실행하지 않았다. 본문 `AVAILABLE` 및 2차 규칙 실행은 확인했지만 메뉴·푸터 제거 품질을 이번에 검증하지 않았다. 특히 9천자대 본문은 정제 품질 확인 대상이며, 문자 수가 많다는 사실만으로 혼입을 확정하지 않는다.

본문 중간 판정은 목포·무안 `BODY_GROUP_A_MATCHED`, 여수 `BODY_GROUP_B_MATCHED`로 검수 대상이고, 나주·강진은 `TARGET_SUPPORT_CONFIRMED`다. 이는 전체 문서의 최종 확정이나 자동 승인 결과가 아니다.

장흥(LGS-000189)은 최초 공개 목록 GET HTTP400 1회로 중단했다. 보성(LGS-000187)은 목록 GET와 공식 제목 검색 POST 2회 후 `검색내역이 없습니다`를 확인했다. 이번 검색에서 표본이 없었다는 의미이며 지역 전체에 공고·첨부가 없다는 뜻이 아니다. 우회·반복 검색 없이 다음 지역으로 진행한다.

## 코드와 계약

`JeonnamNoticeAttachmentDiscoveryProfile`은 기관별 공식 상세 주소·파일 호스트·첨부 영역을 고정한다. 목포·여수·무안은 공식 다운로드 폼의 POST, 나주·강진은 공개 파일 GET을 처리한다. 나주의 고정 `window.open` 문자열은 URL만 파싱하며 JavaScript를 실행하지 않는다. 화면 인쇄 PDF·메뉴 링크·미리보기는 실제 첨부와 구분한다.

공식 폼의 일회성 필드는 제한된 메모리 요청에만 유지한다. 저장용 locator는 공고 ID와 파일 식별 hash만 포함하며 원문 인자·일회성 값은 로그·문서·감사 metadata에 남기지 않는다. 요청 host/path/형식·초기 요청과 후속 요청의 일치를 검사한다. 미해석 링크·미지원 확장자가 있어도 정상 파일을 보존하며, 첨부 영역 미확인이나 표시 파일 수 불일치를 `NO_FILES`로 숨기지 않는다.

기존 프로필 코드/hash·migration·DB/API 계약·운영 정책·자동 활성화 조건은 수정하지 않았다. 로컬 등록은 **지역58+기업마당1 = 59프로필**이다. 카탈로그 **113참조/지역55개**이며 신규5건의 expectation은 null, 기존 저장 기대값1개를 보존하고 신규 정상 승인은 추가하지 않았다.

## 명령 / 결과 / 검증 경계

```powershell
.\gradlew.bat :test --tests '*JeonnamFirstDownloadContractTest' --tests '*JeonbukSecondDownloadContractTest' :bootJar --no-daemon
.\gradlew.bat :test --tests '*JeonnamFirstDownloadContractTest' --tests '*JeonbukFirstDownloadContractTest' --tests '*JeonbukSecondDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=MOKPO,YEOSU,NAJU,GANGJIN,MUAN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

첫 검사28/28·bootJar 성공(52초). 확장 계약130/130·실파일5/5·bootJar 성공(합계1분38초). 최종 검토에서 호스트 없는 HTTPS 링크의 예외 가능성을 보완하고, 동일 계약130/130·실파일5/5·bootJar를 변경 코드로 재검증했다(1분39초). 실패·오류·skip0, Node23/23. 최초 관측5건은 `-INITIAL.json` 영수증으로 보존했으며 같은 파일 hash가 일치한다. 재검증을 새 공고나 지역으로 중복 집계하지 않는다. inventory export는 보관된 읽기 전용 영수증을 임시 환경으로 지정해 실행 후 복원했다. **126영수증/최신98공고** 재현 일치 및 과거 영수증·실행 metadata 보존을 확인했다.

- 사전 조사18요청(GET12·POST6), 요청당15초/1MiB/자동 redirect0/TLS 검증 유지.
- 실제 관측은 공고별 실행당 최대6요청/23MiB. 최초와 보완 후 각 예약20회·11,976,657byte, 누적40회·23,953,314byte. 두 실행 합계 본문10회·상세10회·파일10회 직접 전송이며 고유 표본/파일은5개다.
- 조사+관측 예약58회. 목포·여수·나주·강진·무안 각11/30, 보성2/30, 장흥1/30.
- 조사 HTML18개/2,676,984byte는 삭제했다. 관측 상세·첨부 원본도 삭제했고 전 결과 `originalFilesRemoved=true`다. 원문 복구 사본은 없으며 hash·비식별 근거만 보존한다.
- 운영 변경0, 추출기 실행0, 원격 서버 QA0, 브라우저 실행0. 단발 Node·Gradle은 종료했다.

전체 프로젝트 테스트·Linux worker DB/API·운영 배포·운영 브라우저 E2E는 이번에 수행하지 않았다. 브라우저는 현재 지역 연결 범위에서 사용자 정책에 따라 생략했다. 근거 hash·요청량·결과는 [영수증 index](attachment-collection-receipt-index-2026-09-28.json)의 `jeonnamFirstDownloadRun`과 `jeonnamFirstDownloadValidationRefreshRun`에 기록했다.

다음은 미등록165지역 연결 확대다. 장흥 접근 오류·보성 추가 표본·기존 부안/김제/순창 등의 오류와 본문 정제 품질 확인은 별도 후속 항목으로 유지한다.
