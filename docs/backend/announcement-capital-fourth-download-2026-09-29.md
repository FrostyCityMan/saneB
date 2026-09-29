# 김포 첨부 수집 및 동두천·평택 후속 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하고 발견·전송·형식·파싱 오류를 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 김포·동두천·평택 공식 본문/첨부 경계와 시스템 프로필 3개 연결.
- [x] 김포 HWP 1개 130,560byte 다운로드.
- [x] 동두천 제목 조합 미충족 중단, 평택 수집 경로 네트워크 오류를 별도 기록.
- [x] 로컬 Java 210건 통과·조건부 1건 생략, Node 23건·bootJar 태스크 통과.
- [!] 외부 관측 태스크는 평택 `DETAIL_DISCOVERY / TRANSPORT_FAILED`로 실패. 전체 명령 성공이 아니다.
- [x] 196영수증/154공고 재현 및 이전 193영수증/151공고 보존.
- [x] 조사 원본 12개 3,798,220byte 및 관측 임시 원본 정리.
- [ ] 미등록 110지역 연결, 등록 후 다운로드 미확인 21지역 후속.
- [ ] 전국 목록 자동 유입·상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 현재 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 확인 지역 | 91 | **92/223 (41.3%)** |
| 다운로드 미확인 지역 | 132 | **131** |
| 프로필 등록 지역 | 110 | **113** |
| 미등록 지역 | 113 | **110** |
| 등록 후 다운로드 미확인 | 19 | **21** |
| 최신 표본의 발견·파일 오류 지역 | 23 | **24** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 113+기업마당 1=114프로필, 카탈로그 169참조/112대상이다. 신규 expectation은 null이며 기존 승인 기대값 1개는 유지한다. 다운로드 관측률은 전체 개발·운영 완료율이 아니다.

## 실제 결과와 한계

| 지역 / 표본 | 본문 | 관측 결과 |
|---|---|---|
| 김포 LGS-000097 / 73069 | 222자, TARGET_SUPPORT_CONFIRMED | HWP 1개 130,560byte, COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| 동두천 LGS-000112 / 44784 | 관측 미실행 | 제목 COMBINATION_NOT_MATCHED, 관측 요청 0회, 파일 0개 |
| 평택 LGS-000093 / 95902 | NETWORK_ERROR | DETAIL_DISCOVERY / TRANSPORT_FAILED, 파일 0개, INCOMPLETE |

- 김포 첫 조사 표본 74352는 HTTP200이지만 “게시기간이 아닙니다.” 안내였다. 이를 빈 첨부 목록으로 보지 않고, 같은 공식 검색 목록의 연간 운전자금 공고 73069로 진행했다.
- 동두천 44784는 조사 HTML에서 첨부 3개 경계를 확인했으나, 관측용 초안 규칙의 **제목 대상·지원 조합 미충족**으로 중단했다. 기존 관측 보고서 status 문자열은 `TITLE_EXCLUDED_NOT_FETCHED`, reason은 `TITLE_COMBINATION_NOT_MATCHED`다. B그룹 자동 제외로 단정하지 않는다. 제목 규칙을 우회하거나 변경하지 않았고, 다운로드를 검증한 지역으로 계산하지 않았다.
- 동두천 상세의 게재기간 종료 후 다운로드 제한 안내는 확인했지만, 관측이 제목 단계에서 멈췄으므로 이번 파일 전송 실패의 원인이라고 단정할 수 없다. 다음 작업은 제목 정책을 통과하는 별도 공식 표본 확보다.
- 평택은 조사 curl에서 공식 상세 HTTP200을 받았지만 실제 Java 본문/고정 주소 다운로드 경로에서는 네트워크 오류가 발생했다. 원인은 미확정이며 TLS 검증이나 출처 제한을 우회하지 않았다. 반복 요청 없이 후속 오류로 분리한다. 표본은 **2024년 보관 공고**이며 현재 접수 가능한 지원사업의 증거가 아니다.
- 동두천 V61 등록 URL은 HTTP, 이번 조사/프로필은 실제 접근한 HTTPS다. 평택 V61 포털 메뉴는 두 번 이동해 현재 게시판으로 연결된다. 운영 목록 parser의 실제 최종 URL·상세 identity 유입은 별도 검증해야 하며 운영 URL이나 migration을 변경하지 않았다.

## 구현 계약

- `CapitalFourthNoticePage`: 지역별 상세 host·경로·메뉴 key/mid·공고 ID·분류 인자를 검증한다. 알려진 목록 검색 인자는 저장 identity에 보존하고 실제 재요청에서는 제외한다. 정확한 제목/본문/첨부 영역이 사라지면 페이지 전체로 대체하지 않는다.
- 김포: `ntfcPblancView.do`, `ntcn_no`, `#contents > table.p-table.block`. 제목 다음의 본문 행만 읽는다. 첨부 `data-user-file-nm`, `data-sys-file-nm`, `data-file-path`를 정해진 `eminwon.gimpo.go.kr` FileDown.jsp GET으로 변환한다. 이벤트 스크립트를 실행하지 않는다.
- 동두천: `selectGosiData.do`, `not_ancmt_mgt_no`, `table.bbs_default.view`의 “제 목”/내용/첨부파일 셀. `div.down_view` 안의 파일명과 `eminwon.ddc.go.kr` FileDownNewPbs.jsp GET만 허용한다. 만료 안내와 장식 스타일은 파일로 세지 않는다. 다른 다운로드 경로로 우회하지 않는다.
- 평택: 공식 `detailForm`의 `bod_view` 제목·`view_cont` 본문·`view_file` 첨부 영역. 고정 `goDownload` 3리터럴을 읽어 `eminwon.pyeongtaek.go.kr` FileDown.jsp GET으로 변환한다. 미리보기의 공고 ID·파일 순번·파일명·저장명·경로가 일치할 때만 보조 링크로 분리하며 미리보기를 요청하지 않는다.
- 미확인 링크·미리보기 상충은 발견 오류로 남기되 정상 파일 descriptor는 보존한다. 미지원 확장자는 별도 기록하고 다운로드하지 않는다. HTTPS443·고정 출처·동일 요청 redirect·signature/MIME·10파일 상한·UNKNOWN 역할을 유지한다.
- 기존 공유 프로필/리터럴 파서와 hash는 변경하지 않았다. migration·DB/API·관리자 화면·추출기·운영 정책·worker 설정 변경 없음.

Profile hash: 김포 `239327e732d3f36c6eab1020d55d7e1be6396f62a751f73eac4e4cf8bfb0be0a`, 동두천 `1b9ee82ed5a680f879c075fe9d8d7d8430fe199773445f106b7e775b293d53a5`, 평택 `01aab9c08f03fbf82b7ad27e91570ba653dc1a50c9b8c095ec62fdee8746be21`. Producer class `71e14cefcf1209b08897123b69ef03fdcfd261aff41bf71564db9cef6f29f078`.

## 요청량 / 정리

공식 조사 GET 12회: 김포 4·동두천 3·평택 5. 요청당 15초/연결 7초/1MiB/자동 redirect 0/TLS 검증 유지. 평택 이동은 같은 공식 host와 관측된 목적지만 확인해 요청했다. 관측 상한은 지역당 6요청·23MiB, 실제 예약 합계는 본문 포함 7회·4,595,200byte다. 조사 포함 예약 상한 19회는 실제 HTTP 호출 수와 동일시하지 않는다.

조사 원본 12개 3,798,220byte는 대장의 길이·SHA256 대조 후 개별 삭제했다. 관측 임시 원본도 정리했다. 비식별 hash·결과 metadata만 보존하며 기존 사용자 output·__pycache__와 기존 프로세스는 건드리지 않았다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalFourthDownloadContractTest' --tests '*CapitalThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GIMPO,DONGDUCHEON,PYEONGTAEK' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 보관한 `target-inventory-20260928-receipt.txt`를 사용했다. `SANEB_CAPITAL_FOURTH_SURVEY_FIXTURE=true`로 조사 HTML 세 지역의 경계 및 김포 만료 안내도 검사했다. 실행 후 환경변수를 복원했다. 이전 남양주/하남/구리 원본은 정리되어 해당 조건부 테스트 1건은 생략했고, 합성 회귀 테스트는 실행했다.

첫 실행은 평택의 검증된 링크 자체 onclick을 다시 오류로 세는 문제로 로컬 테스트 2건 실패, **2분16초 BUILD FAILED**. 하위 요소만 재검사하도록 수정했다. 이 실행은 외부 관측 전에 중단됐다.

재실행: 로컬 Java **211건 중 210통과·1생략·실패/오류0**, bootJar 태스크 통과. 외부 관측 3건 중 김포 다운로드·동두천 제목 중단이 기록됐고 평택 전송 오류 1건으로 **전체 명령 2분3초 BUILD FAILED**다. 외부 오류를 숨기거나 전체 실행 성공으로 바꾸지 않는다. Node 23건·196영수증/154공고 재현·diff 검사 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 브라우저 QA는 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
