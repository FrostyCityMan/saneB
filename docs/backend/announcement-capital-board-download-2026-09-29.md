# 양주·군포·여주 본문 경계 및 첨부 수집 연결

## 현재 단계 / Gate

성공 파일을 수집하고 발견·다운로드·파싱 실패를 분리하는 전 지역 확대 단계다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지 및 HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 양주·군포·여주 프로필 3개 및 정확한 본문 경계 연결.
- [x] HWP 2개·HWPX 1개, 총 273,603byte 다운로드.
- [x] Java 테스트 221건 통과·조건부 1건 생략, Node 23건·bootJar 통과.
- [x] 190영수증/148공고 재현, 이전 근거 보존.
- [x] 조사 원본 12개 2,665,101byte 및 관측 임시 원본 정리.
- [ ] 미등록 116지역 연결, 등록 후 다운로드 미관측 19지역 후속.
- [ ] 전국 목록→상시 worker·텍스트 추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 현재 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 85 | **88/223 (약 39.5%)** |
| 다운로드 미관측 지역 | 138 | **135** |
| 로컬 프로필 등록 지역 | 104 | **107** |
| 미등록 지역 | 119 | **116** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 최신 등록 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역 107+기업마당 1=108프로필, 카탈로그 163참조/지역 104개다. 신규 expectation은 null이며 기존 승인 기대값 1개를 유지했다. 이전 187영수증·145공고 및 실행 metadata를 보존한다.

## 실제 결과 및 구현 계약

| 지역 / 표본 | 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 양주 LGS-000101 / 65326 | 55자 | 1 / 1 | HWP 125,440byte |
| 군포 LGS-000103 / 43659 | 537자 | 1 / 1 | HWP 56,832byte |
| 여주 LGS-000111 / 51859 | 53자 | 1 / 1 | HWPX 91,331byte |

세 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 양주·여주 본문은 공고/붙임 안내에 해당하는 짧은 텍스트다. 첨부 세부조건까지 분석된 것으로 표현하지 않으며 파일 수집을 계속한다. 텍스트 추출·구간 분석·DB/API 저장·최종 정책 승인으로 확대 해석하지 않는다.

표본은 양주 2026년 중소기업·소상공인 운전자금 이차보전 지원 공고, 군포 2026년 소상공인 특례보증·이차보전 지원 계획, 여주 2026년 청년 주택 임차보증금 대출이자 지원 모집 공고다. 현재 접수 가능 여부를 판정하거나 운영 공고를 생성하지 않았다.

- `CapitalEminwonNoticePage`: 공식 `/www/selectEminwonView.do`의 source host·메뉴 key·공고 ID를 확인하고, 알려진 목록 표시용 인자만 identity에 보존한 뒤 재요청에서 제거한다. 군포는 일반공고 분류 `01`을 고정한다. 중복·미확인 인자와 다른 메뉴·출처는 거부한다.
- 양주: `table.bbs_default.view`의 제목/내용/파일 셀. 여주: 공식 compact 표의 제목 span·내용 td·첨부 td. 두 지역은 각 공식 `eminwon.*.go.kr`의 FileDown.jsp GET 3인자만 허용한다.
- 군포: 공식 compact 표의 제목/상세내용/첨부 셀, form2 POST hidden 3필드를 확인했다. 정해진 군포 새올 FileDown.jsp에 평문 3인자를 POST한다. 화면 JavaScript는 실행하지 않고 고정 호출의 문자열 인자만 읽는다.
- 군포 미리보기는 같은 다운로드의 파일명·저장명·경로·공고 ID와 완전히 일치할 때만 보조 링크로 분리한다. 미리보기 자체는 요청하지 않는다. 상충·미확인 미리보기는 발견 오류로 기록하면서 정상 파일 descriptor를 보존한다.
- `LocalGovernmentNoticeProviderContentClient`의 해당 3개 공식 상세 경로에 본문 선택을 추가했다. 메뉴·담당자 metadata·첨부 파일명·푸터는 본문 분류 근거에서 배제하고 본문 내부 업무 표와 제외 조건 문장은 보존한다. 본문 selector가 사라지면 페이지 전체를 대신 사용하지 않는다.
- HTTPS443·고정 2개 출처·동일 요청 redirect·파일 signature/MIME·10파일 상한·UNKNOWN 역할·부분 성공 보존을 유지한다. 기존 공유 프로필과 hash는 변경하지 않았다.
- 등록 parser는 모두 기존 `SPRING_BBS`와 연결한다. 저장소 V61 공식 목록과 공개 검색 결과에서 상세 ID를 확보했다. 현재 목록 parser의 전체 자동 유입을 이번 고정 상세 관측만으로 검증했다고 주장하지 않는다.
- migration·DB/API·화면·운영 정책·worker·추출기 변경 없음.

Profile hash: 양주 `0b8ef5c8eb7af1c1286594b8e5065354351f6a04fcb728c9629090246428e34d`, 군포 `9f2d406dea21fe49c3ccea84887787da2ebd7089e7d955664f6467e4a37b5b90`, 여주 `e6da970bea85b8e49ea4db6d114fb002d4f909498a5b48f017a3989365576dc8`. Producer class는 `88d5d050b56b8e42572184b2712d8462fc47f7901f56c76845e4945be39644b9`다.

## 요청량 / 정리

공식 조사 GET 12회: 양주 4·군포 3·여주 5. 요청당 15초/연결 7초/1MiB/자동 redirect 0/TLS 검증 유지. 양주·여주는 `searchCnd=all`에 검색어가 반영되지 않아 공식 폼의 `B_Subject` 조건을 사용했다. 여주 소상공인 제목 검색은 결과가 없어 지원 제목 검색에서 청년 공고를 선택했다.

관측 예약 상한 12회·본문 예약 포함 7,269,571byte. 조사 포함 24회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 각 관측 최대 6요청·23MiB다. 조사 원본 12개 2,665,101byte를 길이·SHA256 대조 후 삭제했다. 관측 임시 원본도 정리했으며 비식별 hash·결과 metadata만 보존한다. 사용자 output·__pycache__ 및 기존 프로세스는 보존했다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*CapitalBoardDownloadContractTest' --tests '*CapitalNextDownloadContractTest' --tests '*ChungcheongNextDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YANGJU,GUNPO,YEOJU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. `SANEB_CAPITAL_BOARD_SURVEY_FIXTURE=true`로 조사 HTML 3건의 제목·첨부 경계도 검사했다. 환경변수는 실행 후 복원하고 원본은 삭제했다. 향후 일반 테스트의 해당 실측 fixture 검사는 조건부 생략이며 합성 회귀 테스트는 원본 없이 실행된다.

첫 실행 222건 중 **221통과·1생략·실패/오류0**, 실파일 관측·bootJar 포함 2분16초 BUILD SUCCESSFUL. 생략은 이전 용인·의왕 `CapitalNextDownloadContractTest.actualOfficialBoundaryMatches`로, 이전 조사 원본 정리 후 환경변수를 활성화하지 않았기 때문이다. Node 23건 및 190영수증/148공고 재현·diff 검사 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
