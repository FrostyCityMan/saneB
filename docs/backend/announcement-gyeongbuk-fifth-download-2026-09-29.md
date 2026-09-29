# 영덕·울진 첨부 연결과 경북 잔여 조사

## 현재 단계 / Gate

성공한 파일은 보존하고 개별 발견·다운로드·파싱 오류를 분리하면서 다음 지역을 진행한다. 모든 오류 해결이나 3표본 확보를 다음 지역의 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 청송·영양·영덕·울진·울릉 목록·검색·상세 조사.
- [x] 영덕·울진 직접 링크 처리기1개·시스템 프로필2개·미승인 참조2개 추가.
- [x] 울진 HWP1개 실제 다운로드·파일 형식 검증.
- [x] 영덕 파일 형식 실패, 울진 미리보기 발견 오류를 별도 기록.
- [x] 계약137건·Node23건·bootJar·실제 관측2공고 실행.
- [x] 152영수증/최신116공고 재현, 이전 근거 보존, 임시 원본 정리.
- [ ] 청송·영양·울릉·고령을 포함한 미등록147지역 연결.
- [ ] 본문 정제·첨부 추출·상시 worker·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 현재 운영 설정 재조회나 운영 DB·정책·worker 변경은 수행하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 61 | **62/223 (약27.8%)** |
| 다운로드 미관측 지역 | 162 | **161** |
| 로컬 프로필 등록 지역 | 74 | **76** |
| 미등록 지역 | 149 | **147** |
| 등록 후 다운로드 미관측 | 13 | **14** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 14 | **16** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약27.8%는 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 지역76+기업마당1=77프로필, 카탈로그131참조/지역73개다. 신규 expectation은 null이며 기존 승인 기대값1개는 변경하지 않았다. 기존 처리기 코드는 수정하지 않아 기존 프로필 근거는 유지한다.

## 실제 수집 결과

| 지역 / 공고 | 본문 확보 길이 | 발견 | 실제 파일 |
|---|---:|---|---|
| 영덕 LGS-000214 / 366709 | 1,579자 | 1개·완료 | 364byte 응답의 HWP 시그니처 검증 실패, 성공0개 |
| 울진 LGS-000221 / 35041 | 777자 | 1개·부분 오류 | HWP84,480byte 성공1개 |

두 공고는 DRAFT seed 제목 조합과 상세 제목 식별을 통과했다. 관측 상태는 모두 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 영덕은 `FILE_SIGNATURE/ATTACHMENT_SIGNATURE_UNSUPPORTED`이며 원인 원문까지 확인한 것은 아니므로 권한 오류라고 단정하지 않는다. 울진은 `ATTACHMENT_LINK_UNRESOLVED`를 남기면서 검증된 다운로드 링크는 계속 처리했다. 미리보기는 호출하지 않았다.

영덕 본문 중간 결과는 `REVIEW_REQUIRED/BODY_GROUP_A_MATCHED`, 울진은 `ACCEPTED/TARGET_SUPPORT_CONFIRMED`다. **본문 길이는 확보한 문자열 길이일 뿐 정제 정확성·본문 전체 확보를 증명하지 않는다.** 첨부 텍스트 추출·관리자 승인·운영 활성화는 검증하지 않았다. 과거 고정 표본의 파일 수집 성공은 현재 모집 중인 공고라는 뜻이 아니다.

## 구현 범위

- 영덕: `div.kboard-document-wrap`의 제목·`kboard-attach` 영역, 고정 게시판 `page_id=763`. HTTPS 공식 호스트의 `action=kboard_file_download`, 공고 uid와 file 식별자를 결합한다.
- 울진: `table.bbs_tablev`에서 제목·첨부파일 레이블의 셀만 읽는다. 상세 메뉴 `DOM_000000103002007001`, 공식 `eminwon.uljin.go.kr/emwp/jsp/ofr/FileDown.jsp`와 공개 파일3query만 허용한다. 파일명의 공백은 URI 인코딩 후 처리한다.
- 공통: source/parser/URL hash 결합, HTTPS443·공식 호스트·고정 경로, 동일 요청 redirect 제한, 첨부 영역 밖 링크 무시, 중복 충돌·10파일 상한, 형식별 분리, 문서 역할 UNKNOWN을 유지한다.
- 미확인 링크·동적 요소가 있어도 정상 descriptor는 버리지 않는다. 전체 발견 성공으로는 보고하지 않는다.
- migration·DB/API·화면·운영 정책·worker 설정·추출기는 변경하지 않았다.
- 영덕 상세에 공공누리 제4유형 표시가 관측됐다. 이번 공개 파일 연결 QA와 별개로 서비스 재배포·재이용 범위는 후속 정책 검토 대상으로 남긴다.

## 다음 연결에 사용할 조사 결과

아래3지역은 상세 화면까지만 조사했으며 다운로드 성공 수에 포함하지 않는다. 각3GET, 원본은 정리했다. 고령은 이번 요청을 보내지 않았다.

- 청송 LGS-000212: `/news/00002679/00006203.web`, `amode=view&not_ancmt_mgt_no=22287&withPast=Y`. 공식 검색은 `stype=title&sstring=소상공인&withPast=Y`. 첨부는 `eminwon.cs.go.kr` 직접 FileDown.jsp 링크와 미리보기 script가 섞여 있다. 고정 표본 제목은 `2022년 청송군 소상공인 맞춤형 재난지원금 접수 안내`.
- 영양 LGS-000213: `/www/organization/yyg_news/notification?idx=26774&mode=view`. 공식 검색 `search_type=title&search_word=소상공인`. 다운로드 button의 `goDownLoad`3개 인수와 암호화된 저장명·경로가 관측됐다. form action·요청 계약 추가 확인 필요. 표본 제목은 `2026년 소상공인 고효율기기 지원사업 지방보조금 지원 공고`.
- 울릉 LGS-000222: `/ko/page.do?mnu_uid=571&not_ancmt_mgt_no=23003&cmd=2`. 공식 검색 `srchColumn=title&srchKwd=소상공인`. 직접 `/programs/board/saeol/notice/download.do?file_seq=1&not_ancmt_mgt_no=23003`와 fileView.do 미리보기. 다운로드 전환 호스트·헤더는 아직 미확인.

## 요청 예산 / 정리

조사15GET, 관측 예약9회로 합계24회(본문 예약 상한 포함)다. 영덕 조사3+관측4, 울진 조사3+관측5, 나머지3지역 조사각3이다. 조사당15초/연결7초/최대1MiB/자동 redirect0/TLS 검증 유지. 관측당 최대6요청·23MiB, 예약 byte 합계4,590,444다.

임시 원본20개 총2,216,760byte와 관측 임시 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하고 사용자 프로세스·미추적 `output/`, `scripts/qa/__pycache__/`는 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukFifthDownloadContractTest' --tests '*GyeongbukFourthDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONGDEOK,ULJIN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 테스트의 enum/import·도우미 호출 컴파일 오류를 수정한 뒤 최종 계약137건 실패·오류·skip0, bootJar·실제 관측2공고 실행 완료(1분44초). Node23/23. 152영수증/116공고 재현 통과. 이전150영수증·기존 실행 metadata·이전114공고 근거를 보존했다. 외부 파일 오류2종은 테스트 실패가 아니며 정상 수집으로 숨기지 않는다.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 미실행이다. 브라우저는 현재 지역 연결 작업에서 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
