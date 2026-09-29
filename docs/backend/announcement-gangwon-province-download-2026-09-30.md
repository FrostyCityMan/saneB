# 강원특별자치도 첨부 연결과 춘천 JSON 상세 후속

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. 성공한 파일을 보존하고 발견·다운로드·추출 오류는 별도로 기록한다. HWP 추출기 1.0.16 추가 개선은 보류한다. 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함한 전체 장기 goal은 미완료다.

- [x] 강원특별자치도 LGS-000116 / SAFE_GWD_BULLETIN 프로필 1개 연결.
- [x] 실제 HTML 제목·본문·첨부 경계 및 부분 오류 보존 계약 확인.
- [x] 본문 133자와 HWPX 1개 150,256byte 다운로드·형식 검증.
- [x] Java 235통과/조건부 1생략, Node 23통과, bootJar, 251영수증/207최신 공고 재현.
- [x] 조사 원본 8개 길이·SHA256 대조 후 개별 정리. 운영 변경 없음.
- [ ] 미등록 60지역 연결, 등록 후 다운로드 미확인 35지역 후속.
- [ ] 춘천 JSON 상세·첨부 지원. 이번에는 구조 조사만 완료했고 파일 다운로드는 미실행.
- [!] 이천 등록 목록 HTTP 400. 같은 주소 재요청 없이 별도 후속으로 보존.
- [ ] 전국 목록 자동 유입·첨부 내용 분석·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성 223지역이다. 이번에 운영 목록을 다시 조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 127 | **128/223 (57.4%)** |
| 다운로드 미확인 | 96 | **95** |
| 프로필 등록 지역 | 162 | **163** |
| 미등록 지역 | 61 | **60** |
| 등록 후 다운로드 미확인 | 35 | **35** |
| 첨부 발견·파일 오류 관측 지역 | 42 | **42** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 163 + 기업마당 1 = 164프로필이다. 카탈로그는 219공고/162대상이며 신규 expectation=null, 기존 승인 기대값은 유지했다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 42지역은 이번 이천 목록 오류를 포함한 모든 목록·본문 오류의 합계가 아니다.

## 실제 관측

2026-09-30 02:31 KST 로컬 Java 수집 경로에서 고정 공개 표본 `GANGWON_PROVINCE-272329`를 관측했다.

- 공식 상세: `https://state.gwd.go.kr/portal/bulletin/notification?articleSeq=272329`.
- 제목: `2026년도 강원특별자치도 소상공인 경영안정 특별자금 지원사업 공고`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE 133자 / TARGET_SUPPORT_CONFIRMED, 발견 FOUND·complete=true.
- HWPX 1개 150,256byte / SHA256 `e400bcf0023de05df9e52c7a1c4727fe2df95be2bd2a508f07236e5fec8cb2f5`.
- 프로필 `LOCAL_GANGWON_PROVINCE_BOARD_V1` / 지문 `21e8029aa925dd7a5f1bac378b3909ccf69be51e9f7b4c4bac6fa9d38c477498`.
- 보고 상태 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`. 추출·구간 분석·정책 QA·기대값 승인·운영 등록은 실행하지 않았다.

본문 ACCEPTED는 규칙 기반 후보 판정이지 관리자 최종 확정이나 현재 모집 중이라는 의미가 아니다.

## 구현 경계

`GangwonProvinceNoticePage`는 해당 host와 `/portal/bulletin/notification` 경로의 숫자 articleSeq만 사용한다. 제목·본문·첨부는 `content-bx` 안의 단일 고시공고 표에서 선택하고, 담당부서·메뉴·파일명·이전/다음 글을 본문 근거에 섞지 않는다. 중복 영역·누락 제목은 구조 오류로 처리한다.

`GangwonProvinceAttachmentDiscoveryProfile`은 공식 첨부 영역 안의 `/egf/bp/common/front/{숫자}/download` GET만 허용한다. 미리보기는 같은 파일 경로인지 비교할 뿐 실행하지 않는다. 파일별 식별자·파일명 상충, 미해석 항목, 미리보기 상충은 오류로 남기되 이미 검증한 descriptor를 유지한다. PDF/HWP/HWPX 이외 확장자는 미지원으로 보존하고 다운로드하지 않는다.

HTTPS443·고정 host·source/parser/URL hash·동일 요청 redirect·안전한 파일명·최대 10파일 계약을 유지한다. 파일 역할은 UNKNOWN이며 파일명으로 공고/신청서를 추정하지 않는다. 새 프로필을 로컬 Spring registry에 등록했지만 운영에 배포하거나 정책 QA 대상으로 승인하지 않았다.

기존 지역 프로필 지문·migration·DB/API/UI·HWP 추출기·분류 규칙·운영 source 설정은 변경하지 않았다. HTML-only 공통 경로에 JSON을 무조건 허용하는 변경도 하지 않았다.

## 춘천 후속 구현 근거

공식 HTML의 정적 코드와 실제 JSON 응답을 읽었으며 JavaScript·미리보기·첨부 다운로드는 실행하지 않았다.

1. 목록 화면은 `/_chuncheon/noticeList.do?pageIndex=1&searchWrd={검색어}&searchCnd=SJ` GET으로 목록을 받는다. 소상공인 제목 검색 결과 중 `73071`의 `2026년 소상공인 경영환경개선 지원사업 공고`를 선택했다.
2. 상세 화면은 `/cityhall/administrative-info/notice-info/notice-announcement/view/?notAncmtMgtNo=73071...`이며, 실제 본문·첨부는 `/_chuncheon/noticeView.do?notAncmtMgtNo=73071` GET에 있다. 응답 `board.not_ancmt_mgt_no`, `board.not_ancmt_sj`, `board.not_ancmt_cn`과 배열 `file`을 확인했다. 연락처·작성자·본문 원문은 이 문서나 감사 metadata에 복사하지 않는다.
3. 첨부 배열 1개는 HWP, 메타데이터 크기 104,960byte다. **파일 전송과 형식 검증은 하지 않았으므로 다운로드 성공으로 세지 않는다.**
4. 공식 `goDownLoad`는 `https://eminwon.chuncheon.go.kr/emwp/jsp/ofr/FileDown.jsp`에 `user_file_nm`, `sys_file_nm`, `file_path`를 URL 인코딩하여 GET한다. 관측한 경로 형식은 `/ntishome/file/upload/ofr/ofr/{8자리 날짜}`다. JSON 문자열을 코드로 실행하거나 임의 URL로 사용하지 않는다.
5. 공식 화면은 공고 18207·18265·26243·40349·65304·68021의 첨부 표시를 생략한다. 후속 프로필도 이 제한을 우회하지 않아야 한다. 고정 표본 73071은 이 목록에 없다.
6. 다음 구현은 source/hash/공고 ID에 결합한 명시적 JSON 처리 경로와 제목·본문·첨부 매핑을 worker·QA 양쪽에 연결한다. 현재 worker/QA의 HTML MIME 검사 때문에 프로필만 추가해서는 충분하지 않다. 기존 HTML 프로필의 허용 범위는 유지하고, 중복 JSON key·틀린 공고 ID·불완전 파일 배열·다운로드 host/경로·부분 성공을 테스트해야 한다.

춘천은 여전히 미등록 지역이다. 위 구조 확인을 운영 자동 수집이나 구현 완료로 표시하지 않는다. 이천 LGS-000105 등록 목록 `https://www.icheon.go.kr/portal/contents.do?mid=0402000000`는 HTTP 400이므로 다른 근거 없이 주소를 추측하거나 반복 호출하지 않았다.

## 요청 예산·원본 정리

- 조사 8회: 강원 목록·제목 검색·상세 3회, 춘천 목록 HTML·검색 JSON·상세 HTML·상세 JSON 4회, 이천 목록 1회. 각 최대 15초/2MiB, TLS 검증 유지, 자동 redirect·인증·쿠키 사용 없음.
- 조사 원본 8개 총 1,023,074byte의 길이·SHA256을 대장 `gangwonProvinceDownloadRun.surveyRawFiles`에 기록했다. 정확한 경로를 검증한 뒤 개별 삭제하며 원본은 다시 공개 사이트를 조회해야 확보할 수 있다.
- 별도 강원 관측 상한 6요청/23MiB, 상세 1MiB. 보고된 본문 포함 예약 상한 4요청/2,433,024byte. 조사 포함 요청 상한 합계 12회다. 예약 상한은 실제 wire 요청 수와 구분한다.
- 관측 임시 상세·파일은 관측기가 정리했다. 임시 조사·패치 도구도 정리하고, 기존 사용자 `output/`·`scripts/qa/__pycache__/`는 보존한다.

## 검증 명령 / 결과 / 미검증

```powershell
# 실제 HTML fixture 환경변수 활성화 후 표적 검사
.\gradlew.bat :test --tests '*GangwonProvinceDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon
# 보관된 운영 대상 스냅샷과 실제 HTML fixture를 사용하는 확대 검사
.\gradlew.bat :test --tests '*GangwonProvinceDownloadContractTest' --tests '*CapitalEighthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GANGWON_PROVINCE -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

표적 검사 34초, 확대 검사 1분53초 성공. 확대 JUnit 236건 중 **235통과/1조건부 생략/0실패**, Node 23/23, bootJar 성공. 생략 1건은 이전 파주·광명 실제 HTML fixture가 정리된 상태의 조건부 컨테이너이며, 이번 강원 fixture는 실행했다. 251영수증/207최신 공고를 다시 이관해 대장과 일치했고 기존 지역 상태와 기존 250영수증/206공고를 보존했다.

브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책에 따라 생략했다. 운영 배포·DB·정책 게시·ENFORCE·기존 데이터·상시 worker·Linux 추출·운영 E2E는 미실행이다. 승인된 QA 브랜치의 `[skip deploy]` 커밋·푸시와 운영 반영은 구분한다.
