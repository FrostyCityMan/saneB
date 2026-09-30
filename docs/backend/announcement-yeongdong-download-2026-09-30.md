# 영동 고시공고 본문·첨부 연결과 은평 오류 진단

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 보존하고 개별 실패를 분리한다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함하는 전체 goal은 미완료다.

- [x] 은평 공식 다운로드 인코딩 대조. 두 방식 모두 HTTP404이며 공통 엔진 변경 없음.
- [x] 영동의 등록된 공식 고시공고 목록 현재 HTTP200 및 공개 제목 검색 확인.
- [x] 영동 본문 선택기와 고정 mode=D 첨부 프로필 구현.
- [x] Java276개·Node23개 회귀 검사, bootJar 및 실제 수집 관측 실행.
- [x] 실패를 포함한300영수증/240공고 재현과 최종 대장 갱신.
- [!] Java 수집기의 본문·상세 HTTP404. 파일 다운로드 미도달.

## 조사 근거

은평 `LGS-000013`의 기존 상세 48267은 HTTP200이었다. 공식 `goDownLoad` 함수는 `/emwp/jsp/ofr/FileDown.jsp`와 3개 파일 인자를 사용한다. 대표 HWP에 대해 공식 encodeURI 방식의 경로와 기존 percent-encoded 경로를 각각 한 번 요청했으나 모두 HTTP404/text-html이다. 인코딩 오류가 원인이라고 단정하지 않으며 임의 파일 경로·인증 우회·TLS 검증 생략은 하지 않았다. 이 진단은 새 수집 성공 근거가 아니다.

영동 `LGS-000141`의 V61 등록 목록은 `https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L`이며 이번 조사에서는 HTTP200이다. 과거404 기록을 삭제하거나 현재 응답의 원인을 추정하지 않는다. 공식 상세의 과거 표본과 별도 보조사업 메뉴는 구조 조사에만 사용했고 보조사업 게시판을 등록 고시공고 범위에 추가하지 않았다.

공식 목록의 `bbsNttSearchForm`에서 `skey=title`과 `sval`을 확인하고 공개 POST 검색을 수행했다. 선정 표본은 `2026년 영동군 소상공인 이차보전금 지원사업 공고`, 공고 ID `759fdcd35933d6237c5cf16b4908416b`, 파일 ID `174573`의 HWP 1개다. 고정 관측 코드 `YEONGDONG-759FDCD3`를 사용한다.

## 구현 경계

- `YeongdongNoticePage`: `program--contents > ui.bbs--view`의 제목·본문·첨부 경계를 선택한다. 메뉴·부서·파일명을 본문에 혼합하지 않는다.
- `YeongdongAttachmentDiscoveryProfile`: `www.yd21.go.kr`의 고시공고 경로와 정확한 공고/파일 인자만 허용한다. query-only 상대 링크의 문서 경로를 보존하고 동일 공고 소속을 검사한다.
- 공식 첨부 영역의 직접 다운로드와 미리보기를 구분한다. 미리보기는 파일 요청으로 실행하지 않는다. 알 수 없는 링크와 미지원 파일은 성공 파일과 별도 상태로 남긴다.
- 최대10파일·중복 제거·역할 UNKNOWN·기존 파일 서명/MIME/파일명 검증을 유지한다. 외부 호스트·다른 게시판·공고 변경 redirect·POST 전환을 허용하지 않는다.
- 프로필·관측 참조만 추가하며 카탈로그 기대값은 null이다. 기존 프로필과 migration·DB/API 계약·키워드 규칙은 변경하지 않는다.

조사는 후속 User-Agent 대조1회를 포함해 GET10회·POST1회, 합계11회다. 요청당15초와 TLS 검증·자동 redirect 금지를 유지했다. 은평 파일 응답은2MiB 읽기 상한으로 제한했다. HTML 조사는 메모리에서만 처리했으며 원문 파일·세션·인증정보를 저장소에 보관하지 않았다. 검색엔진 질의3회는 이 횟수와 별도다. Java 실제 관측은6요청·23MiB 상한을 적용했다.

운영 DB/설정/정책/worker 변경·ENFORCE·재분류·배포는 이번 범위 밖이다. 브라우저는 현재 사용자 정책에 따라 미실행이다. AWS 인증 갱신 및 이전 연제·구례 조사 원본 정리 미완료는 별도 후속으로 남긴다.

## 검증 결과

2026-09-30 12:44:35 KST 실제 Java 관측:

| 항목 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED |
| 본문 | FETCH_FAILED / HTTP_STATUS_ERROR, 0자 |
| 첨부 발견 | DETAIL_DISCOVERY / ATTACHMENT_HTTP_404 |
| 파일 다운로드 | 미도달, 0개. 첨부 없음 판정이 아님 |
| 전체 관측 | INCOMPLETE |
| 요청 예약 / byte 예약 | 3 / 2,097,152 |
| 관측 원본 | 임시 디렉터리 정리 확인 |

동일 상세 주소를 기본 PowerShell 요청에서는 HTTP200으로 확인했으나, 수집기와 같은 `saneB-attachment-collector/1.0` User-Agent를 사용한 후속 PowerShell 대조에서는 HTTP404였다. User-Agent 관련 응답 차이의 정황이며 서버의 실제 차단 규칙·원인을 확정한 것은 아니다. 수집기 User-Agent를 일반 브라우저로 바꾸어 우회하지 않았으며 추가 반복 요청은 하지 않았다. 사이트의 허용 수집 경로·정책 확인이 필요하다.

프로필 지문은 `a7388ece540d068476c562d34e496ba2a6695390e9dfd91dc30441e2615740f9`다. 보고서는 `build/reports/attachment-regional-collection/YEONGDONG-759FDCD3.json`이며, 대장 `yeongdongDownloadRun`에 조사·관측 경계를 기록했다. 원문 HTML의 링크 발견이나 fixture 검사를 실제 다운로드로 계수하지 않는다.

**162/223수집원(72.6%) 확인·61잔여를 유지**한다. 미연결은34→33개, 등록 후 다운로드 미확인은27→28개다. 오류 관측 수집원은42→43개이며 성공 수집원과 중복될 수 있다. 지역190+기업마당1=191프로필, 카탈로그249공고/190대상, 참조 전용248개와 기존 기대값1개다. 분모는2026-09-28 15:56:12 KST 활성 수집원 스냅샷이며 고유 행정구역 수가 아니다. 기존 엄격한3표본·전체 첨부 Gate16충족/207잔여는 별도 유지한다.

실행 명령:

```powershell
.\gradlew.bat --no-daemon :test --tests '*YeongdongDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest.yeongdongBodyExcludesAttachmentsAndDetectsChangedStructure'
.\gradlew.bat --no-daemon :test --tests '*YeongdongDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONGDONG' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

초기8검사 중1개가 빈 POST form으로 Request를 생성하여 실패했다. 프로필이 금지한 POST인지 검사하기도 전에 공통 Request 계약에서 거부된 테스트 작성 오류였다. 유효한 POST 객체를 생성한 뒤 프로필에서 거부되는지를 검증하도록 수정했다. 최종 Java276개는 실패·생략0, bootJar 성공, Node23개 통과다. 실제 외부 관측1개는 HTTP404로 실패하여 통합 명령은 exit1(2분34초)이다. 전체 명령을 성공으로 보고하지 않는다.

기존299영수증/239공고를 보존한300영수증/240공고의 해시·최신 표본·대장 재현 검증을 통과했다. 다른 지역 프로필 근거는 변경하지 않았다. 다운로드·추출·운영 완료 수치는 증가시키지 않았으며, 다음은 다른 미연결 지역 확장과 분리된 외부 접근 오류 처리다.
