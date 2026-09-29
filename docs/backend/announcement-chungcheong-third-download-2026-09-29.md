# 충북·공주 본문/첨부 연결 및 전송 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일을 수집하고 발견·전송·형식·파싱·본문 오류를 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 충북·공주 본문/첨부 프로필 2개 연결과 공식 HTML 경계 대조.
- [x] 합성 계약·본문 클라이언트·기존 회귀 Java 210통과, 조건부 1생략. Node 23통과, bootJar 성공.
- [!] 실제 Java 수집은 두 지역 모두 본문 NETWORK_ERROR / 상세 TRANSPORT_FAILED. 다운로드 성공으로 세지 않는다.
- [!] 청주 공식 목록 HTTP302 차단 페이지 이동은 우회하지 않고 별도 후속.
- [x] 212영수증/168공고 재현, 기존 210영수증/166공고 보존.
- [x] 조사 HTML 7개 2,070,796byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 99지역 연결, 등록 후 다운로드 미확인 26지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 98 | **98/223 (43.9%)** |
| 다운로드 미확인 | 125 | **125** |
| 프로필 등록 지역 | 122 | **124** |
| 미등록 지역 | 101 | **99** |
| 등록 후 다운로드 미확인 | 24 | **26** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 29 | **31** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 124+기업마당 1=125프로필, 카탈로그 180참조/123대상이다. 신규 기대값은 null이며 기존 승인 기대값 1개는 유지한다. 오류 지역 수는 성공 파일과 오류가 공존하는 지역을 포함하므로 미관측 지역 수와 합산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 구현 및 검증 근거

| 지역 / 고정 공고 | 공식 HTML 대조 | 실제 Java 수집 |
|---|---|---|
| 충북 LGS-000135 / 67302 | 제목·본문·HWP 1링크 및 대응 미리보기 확인 | 본문 NETWORK_ERROR, 상세 TRANSPORT_FAILED, 파일 처리 미진입 |
| 공주 LGS-000149 / 59971 | 제목·본문·PDF/HWPX 2링크 및 대응 미리보기 확인 | 본문 NETWORK_ERROR, 상세 TRANSPORT_FAILED, 파일 처리 미진입 |
| 청주 LGS-000136 | 공식 목록 HTTP302 → `/common/block.do` | 프로필 미등록 유지, 차단 페이지 미요청 |

- 충북: `bbs_viewbox`의 제목·`contenttext` 본문·`attachedfile`을 구분한다. `sido.chungbuk.go.kr/citynet/jsp/cmm/attach/download.jsp`의 mode/fid/index/other 네 인자만 GET으로 허용한다. 미리보기의 공고번호·파일 인덱스·파일 식별자 일치를 확인하지만 미리보기는 요청하지 않는다.
- 공주: `program--contents > ui.bbs--view`에서 제목·본문·파일 영역을 분리한다. `fn_egov_downFile`의 고정 문자열 3개를 기존 선형 리터럴 파서로 해석한다. 공식 `fileForm`의 POST action, 빈 hidden 필드 3개를 대조하고 user_file_nm/sys_file_nm/file_path만 전송한다. JavaScript를 실행하지 않고 다른 폼·인증 값은 보내지 않는다. 파일명 공백은 보존한다.
- HTTPS443·출처 host/path·공고 identity·중복 query 거부·동일 요청 redirect·최대 10파일·UNKNOWN 역할을 유지한다. 미확인 링크/미리보기 충돌과 정상 descriptor를 분리하고 미지원 확장자는 다운로드하지 않는다.
- 본문 클라이언트 테스트는 표 안의 제외 조건을 보존하면서 메뉴·푸터·담당자·첨부 파일명이 본문에 섞이지 않음을 검증한다. 중복/누락 본문 경계는 BODY_SELECTOR_CHANGED로 분리한다.
- 공통 파일 validator·기존 프로필·Flyway·DB/API/UI·추출기·운영 정책·worker 설정은 변경하지 않았다.

사전 제한된 curl 진단에서는 충북 파일 endpoint가 HTTP200/100byte/text/html, 공주 POST 두 건이 HTTP200/360,926byte·98,782byte/application/octet-stream으로 응답했다. 이 진단은 signature/MIME/파일명 검증을 거친 애플리케이션 수집이 아니므로 **다운로드 성공 근거로 승격하지 않는다**. 이후 Java 경로 전송 실패의 정확한 TLS/네트워크 원인은 이번 범위에서 확정하지 않았다. 재시도나 인증서 검증 완화 없이 별도 후속으로 남긴다.

프로필 hash: 충북 `ee61af8fb3b5f76d8a23d9934a65b8ec51429d34410ed54246ec1150b654af4f`, 공주 `88efa874fced2213dde423db73b19ad38af9aec3fe33b6104c14f088324c7b07`.

관측 producer class hash: `b1b7e6d8d80a344671e425930c4348750d616d2ae85889e57c402e851333c38c`.

## 요청량과 정리

조사 11회: 충북 4(목록/검색/상세/파일 응답 진단), 공주 5(목록/검색/상세/두 파일 POST 진단), 청주 2(목록/redirect 목적지 진단). 요청당 15초·연결7초·1MiB·자동 redirect0·TLS 검증 유지. 파일 진단 바이트/원문 헤더는 보관하지 않았다.

Java 관측 2건의 공고별 상한은 6요청·23MiB이며 실제 요청 예약 합계는 본문 포함 6회·4,194,304byte다. 조사 포함 요청 예약 상한은 17회다. 예약과 실제 HTTP 호출 수는 동일하지 않다. 관측 임시 원본 정리를 확인했고 조사 HTML 7개를 대장 hash·길이 대조 후 개별 삭제했다. 결과 metadata 보고서는 재현용으로 보존한다. 일회성 Node·Gradle은 종료되며 사용자 output·__pycache__·기존 프로세스는 보존한다.

## 실행 명령 / 결과 / 미검증

```powershell
.\gradlew.bat :test --tests '*ChungcheongThirdDownloadContractTest' --tests '*GangwonSecondDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHUNGBUK,GONGJU' -PsanebCollectionWindowsTrust=true --no-daemon
# 본문 클라이언트 회귀 추가 후 위 :test 필터와 :bootJar만 재실행. 외부 요청 재실행 없음.
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

첫 실행 1분36초: 로컬 계약과 bootJar 통과, 외부 관측 2건은 실패. 본문 클라이언트 회귀 추가 후 로컬 검증 1분10초 성공, 최종 XML 211건 중 210통과·1조건부 생략·실패/오류0이다. Node 23통과와 212영수증/168공고 재현 통과. 공식 HTML fixture와 기존 운영 분모 영수증은 명령 단위 환경변수로 주입 후 복원했다. 원본 정리 후 HTML fixture 검증은 기본 실행에서 조건부 생략된다.

전체 테스트·추출 검증·신규 AWS/운영 조회·운영 배포·정책 게시·ENFORCE·기존 데이터 배치는 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청 정책에 따라 미실행이다. 커밋/푸시는 `[skip deploy]` 범위이며 운영 완료를 의미하지 않는다.
