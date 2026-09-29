# 삼척 본문·첨부 연결과 구형 MIME 호환

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선 확대한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 goal은 미완료다.

- [x] 삼척 LGS-000123 / SCMS_CARD_NOTICE 본문·첨부 프로필 연결.
- [x] 기존 SCMS 파서 재사용 및 공식 상세 ID·파일 중계 경계 검증.
- [x] 본문92자·HWP1개77,312byte 실제 다운로드 및 기본 형식 검증.
- [x] 최초 MIME 실패 보존, 고정 파일 진단, 삼척 한정 호환 처리 후 재검증.
- [x] Java228통과/0실패/0생략, Node23/23, bootJar, 255영수증/210공고 재현 통과.
- [ ] 미등록57지역 및 등록 후 다운로드 미확인36지역 후속.
- [ ] 실제 파일 텍스트 추출·구간 분석·운영 상시 worker·DB/API/UI·운영 E2E.

## 구현과 계약

공식 목록 `https://www.samcheok.go.kr/media/00084/00095.web`의 공개 표본 `SAMCHEOK-36177`을 사용한다. 상세 query는 `amode=view&mgtNo=36177&cd=01`, 제목은 `2026년 소상공인 육성자금 융자추천 계획 공고(수정)`이다. [직전 조사](announcement-pyeongchang-download-2026-09-30.md)의 삼척3요청 이력을 보존했다.

`SamcheokNoticePage`는 고정 HTTPS host/path·`amode=view`·숫자 `mgtNo`·`cd=01`을 검증한다. 검색·페이지 query는 허용 목록 내에서 제거해 상세 요청으로 정규화한다. 본문은 공식 `form#saeolGosiVO[name=saeolGosiVO][method=get] > .bbs1view1 > .substance`에서만 가져오며 메뉴·담당부서·첨부명을 섞지 않는다.

`SamcheokAttachmentDiscoveryProfile`은 기존 SCMS 파서를 재사용한다. 공고 번호 전달을 위해 메모리 내 파싱 입력의 `mgtNo`를 공통 파서의 `not_ancmt_mgt_no`로 변환하지만, 내부 URI는 외부 요청 승인 대상이 아니고 저장된 source identity를 바꾸지 않는다. locator의 공고 번호36177은 동일하다. 테스트로 내부 URI 외부 요청 차단과 원래 source hash 검증을 확인했다.

첨부는 `attach1`의 공식 `/DownloadEx.do` 중계 링크만 처리한다. 중계 URL의 내부 목적지도 `eminwon.samcheok.go.kr/emwp/jsp/ofr/FileDown.jsp`, 고정 업로드 디렉터리·안전한 두 파일명·동일 표시 이름으로 검증한다. 내부 HTTP 문자열은 중계 주소 검증에만 사용하고 직접 HTTP 요청하지 않는다. 실제 네트워크는 삼척 대표 사이트의 HTTPS GET만 허용하며 동일 요청 redirect 경계를 유지한다. 임의 중계 주소·사설 host·다른 경로는 차단한다.

부분 실패는 정상 descriptor를 지우지 않는다. 중복·상충·미지원 형식·최대10파일·첨부 없음과 발견 실패를 구분하며 역할은 UNKNOWN이다. 기존 SCMS 공통 클래스와 다른 지역의 프로필 지문은 수정하지 않았다.

## 실제 관측과 MIME 보정

첫 관측(2026-09-30 03:33 KST)에서 본문92자와 HWP1개를 확인하고 77,312byte를 전송했으나 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패했다. 최초 보고서를 `SAMCHEOK-36177-MIME-INITIAL.json`으로 보존한다. 실패 당시에는 다운로드 성공으로 집계하지 않았다.

동일 파일을 진단1회로 확인하여 같은 SHA256·OLE 서명, `application/x-msdownload`, UTF-8 octet 파일명 헤더를 확인했다. 기존 형식 검증기의 구형 MIME/엄격한 UTF-8 복원 옵션을 **삼척 프로필에만** 활성화했다. 공통 MIME 목록을 넓히지 않았고 예상 형식·바이너리 서명·attachment 헤더·파일명 확장자 일치를 계속 요구한다. HTML 오류 응답, 잘못된 예상 형식, 누락/inline/경로 포함 헤더 거부 테스트를 추가했다.

수정 후 실제 Java 재관측(2026-09-30 03:37 KST):

- 제목 COMBINATION_MATCHED, 본문 AVAILABLE92자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true, HWP1개77,312byte / DOWNLOADED.
- SHA256 `60a745d3fdb0b8ebd7959e64c7b8ffedbf5382a2cf084c9b531d5251ca32124e`.
- 프로필 `LOCAL_SAMCHEOK_SCMS_V1`, 최종 지문 `83cc28264b983a005adce08793ecc672209529a1e10ddb259252ea79452bb26c`.
- 결과 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리 true, 운영 쓰기0.

이 검증은 예상 확장자·MIME·8바이트 서명·첨부 헤더의 기본 형식 검사다. OLE 내부 HWP 구조·텍스트 추출·구간 분석 성공을 의미하지 않는다. 본문 ACCEPTED도 후보 판정이며 관리자 확정이 아니다.

## 지역 집계 / 요청 원장

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이며 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 129 | 130/223 (58.3%) |
| 다운로드 미확인 | 94 | 93 |
| 미등록 지역 | 58 | 57 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존 3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역166+기업마당1=167프로필, 카탈로그222공고/165대상이다. 신규 expectation은 null이다. 최초 실패와 재관측 영수증2건을 모두 보존하며 최신 공고 상태만 성공 근거로 선택한다. 기존253영수증·209공고·다른 지역 상태·이전 요청 원장은 비교 검증하여 보존했다.

이전 삼척 조사3회 + 이번 상세 fixture1회 + 파일 진단1회 + 관측2회의 본문 포함 예약8회 = 누적 예약 상한13회다. 수정 후 재검증은 최초 실패와 진단 근거에 따른 것이며 변경 없는 재시도가 아니다. 이번 관측2회는 각각 최대6요청/23MiB·상세1MiB, 조사 상세는15초/2MiB, 단일 파일 진단은20초/20MiB로 제한했다. 이전 조사 포함 계획 상한17회 이내다. 관측2회의 예약 byte 합계4,636,672이며 예약값을 실제 wire 요청 수로 단정하지 않는다.

조사 HTML·진단 HWP·진단 헤더는 원장의 SHA256·길이와 정확한 경로를 대조한 후 삭제했다. 실제 관측 임시 파일도 실행기가 정리했다. 원본은 공식 사이트에서 다시 조회해야 확보할 수 있으며 원장에 원문·연락처·헤더 값은 저장하지 않았다.

## 검증 명령 / 결과 / 미검증

```powershell
# 보관 운영 대상 스냅샷과 실제 fixture 환경변수 활성화 후 실행
.\gradlew.bat :test --tests '*SamcheokDownloadContractTest' --tests '*GyeongnamThirdDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SAMCHEOK -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적117통과, 첫 확대218통과/bootJar 성공이었지만 실파일 MIME 검증은 실패했다. 보정 후 최종228통과/0실패/0생략·bootJar·실파일 관측 성공(2분9초), Node23/23, 255영수증/210공고 재현 통과다. 실제 fixture 검증은 실행했고 원본 정리 후 환경변수 없이 실행하면 해당 조건부1건은 생략된다.

이번 변경 관련 표적 회귀를 실행했으며 전체 프로젝트 테스트·운영 DB·AWS·실제 운영 worker/DB/API/UI·추출·운영 E2E는 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책에 따라 생략했다. migration·API·UI·분류 규칙·추출기·운영 설정·배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않는다. 전체 장기 goal은 계속 진행한다.
