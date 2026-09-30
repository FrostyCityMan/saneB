# 김제 고시공고 본문·첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 보존하고 개별 실패를 분리한다. HWP 추출기 추가 개선은 보류하며 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함하는 전체 goal은 미완료다.

- [x] 등록 고시공고 메뉴의 공식 게시판 이동·공개 검색·상세 구조 확인.
- [x] 김제 본문 선택기·첨부 프로필·고정 표본과 회귀 검사 구현.
- [x] Java278개·bootJar·Node23개 및 실제 첨부 관측2회.
- [x] 302영수증/241공고와 지역 대장 재현 검사.
- [x] HWPX·HWP2개186,159byte 다운로드·기존 파일 형식 검증·임시 원본 정리.
- [!] 재검증의 본문 REDIRECT_LIMIT_EXCEEDED. 첨부 성공과 별도로 유지.

## 조사와 구현 경계

등록 수집원은 `LGS-000169 / SUBJECT_NOTICE_TABLE`이다. 등록 메뉴 `/index.gimje?menuCd=DOM_000000104003000000`에서 공식 `/board/list.gimje`로 이동하며 게시판은 `BBS_0000044`다. 과거 반복 redirect 기록은 보존한다. 공개 GET 제목 검색의 `searchType=DATA_TITLE`, `searchOperation=AND`, `keyword=소상공인`에서 `2026년 영세소상공인 카드수수료 지원사업 공고`(310426)를 찾았다. 등록 메뉴·운영 수집원 설정은 변경하지 않았다.

상세 조사에는 수집기와 동일한 `saneB-attachment-collector/1.0` User-Agent와 TLS 검증을 사용했다. 이번 상세 2회 중 첫 요청은302, 다음 요청은200이었다. 두 번째 요청은 쿠키를 허용한 새 HttpClient였으나 redirect 없이 바로200을 받았으므로 쿠키가 필수였다고 단정하지 않는다. 이전 검색 요청도 쿠키 없이200이었다. 인증·차단 우회는 하지 않는다.

`div.bbs_skin > div.bbs_view` 아래 제목(`bbs_vtop > h4`), 본문(`bbs_con`), 첨부(`bbs_filedown > dl`)를 각각 선택한다. 이름 표시용 `javascript:void('0')`, 실제 `sbtn_down`, 미리보기 `sbtn_file2`를 구분한다. 다운로드는 같은 공고의 `/board/download.gimje`에 한정하며 fileSid196565(HWPX 공고문),196574(HWP 신청서)를 발견했다. 쿼리의 `command=update`는 공식 공개 GET 다운로드 링크의 인자이며 운영 데이터 수정 요청이 아니다.

- 공식 HTTPS 호스트·게시판·메뉴·공고·파일 인자를 검증한다. 중복 쿼리·임의 경로·다른 공고·외부 호스트·POST 전환을 거부한다.
- 이름 표시용 링크·미리보기는 실행하지 않는다. 미해석 링크는 오류로 분리하고 검증된 다운로드는 유지한다.
- 최대10파일·중복 제거·역할 UNKNOWN·기존 파일 서명/MIME/파일명 검증을 유지한다.
- 첨부 영역을 찾지 못한 경우 실패이며 첨부 없음으로 표시하지 않는다. 명시적인 빈 첨부 영역만 NO_FILES다.
- 새 카탈로그는 reference-only이며 기대값 null이다. 다른 프로필·키워드 정책·DB/API·migration·추출기는 변경하지 않는다.

이전 이어가기 구간의 조사9GET과 이번 상세2GET·파일 응답 헤더2GET, 총13GET은 원문을 메모리에서만 읽었으며 원본 파일을 저장하지 않았다. 요청당15초, 공식 호스트만 검토하고 자동 redirect는 껐다. 파일 헤더 조사에서는 본문을 다운로드하지 않았다. 세션·쿠키·인증정보는 저장·출력하지 않았다. 실제 Java 고정 관측은 별도6요청·23MiB 상한으로 최대2회 수행했다.

운영 DB·설정·정책·worker 변경, ENFORCE, 재분류·배포는 이번 범위 밖이다. 브라우저는 현재 사용자 정책에 따라 미실행이다. AWS 인증 갱신·이전 연제/구례 조사 원본 정리 미완료는 별도 후속이다.

## 검증 결과

2026-09-30 실제 Java 관측:

| 항목 | 첫 관측 13:07:11 KST | 재검증 13:08:59 KST |
|---|---|---|
| 제목 | COMBINATION_MATCHED | COMBINATION_MATCHED |
| 본문 | AVAILABLE,620자 | FETCH_FAILED / REDIRECT_LIMIT_EXCEEDED |
| 첨부 발견 | FOUND,complete=true,2개 | FOUND,complete=true,2개 |
| 파일 다운로드 | 2개 모두 ATTACHMENT_PATH_NOT_APPROVED | HWPX103,215byte / HWP82,944byte |
| 요청 예약 | 6 | 6 |
| byte 예약 | 2,203,648 | 2,389,807 |
| 임시 원본 | 정리 확인 | 정리 확인 |

첫 관측은 요청 예약 상한6회에 도달했다. 공통 다운로드 클라이언트는 경로 검증 callback이 false이면 ATTACHMENT_PATH_NOT_APPROVED를 반환하며 관측 callback에는 예산 검사도 포함되므로 이 코드만으로 잘못된 URL이라고 단정할 수 없다. 후속 헤더 조사에서 동일 URI로의302와 별도 요청의200/application-octet-stream을 확인했다. 허용 경로·요청 상한·User-Agent·쿠키 처리 코드를 바꾸지 않고 최대1회 재검증하여 두 파일을 확보했다. 최초 실패 보고서를 ATTEMPT1로 별도 보존했다.

두 관측을 합쳐 한 번의 전체 파이프라인 성공으로 표현하지 않는다. 첫 본문 성공과 최신 본문 실패가 모두 남아 있으며 본문 오류를 해결했다고 보고하지 않는다. 다운로드 성공은 내부 텍스트 추출·정책 QA·상시 운영·관리자 E2E 완료가 아니다.

프로필 지문은 `ce100ee16081a7f73900ca9d7207da185230c4c8b5a8d3f1780a9c0c794ddc3b`다. 보고서는 `build/reports/attachment-regional-collection/GIMJE-310426-ATTEMPT1.json`, `GIMJE-310426.json`이고 비식별 metadata·해시는 대장 `gimjeDownloadRun` 및 영수증 인덱스에 기록했다.

**163/223수집원(73.1%) 확인·60개 잔여(미연결32+등록 미확인28)**다. 프로필은 지역191+기업마당1=192개, 카탈로그250공고/191대상이며 참조 전용249개와 기존 기대값1개다. 첨부 오류 관측 수집원43개는 성공 수집원과 중복될 수 있고 본문 오류 집계가 아니다. 분모는2026-09-28 15:56:12 KST 활성 수집원 스냅샷이며 고유 행정구역 수가 아니다. 기존 엄격한3표본·전체 첨부 Gate16충족/207잔여는 별도 유지한다.

실행 명령:

```powershell
.\gradlew.bat --no-daemon :test --tests '*GimjeDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest'
.\gradlew.bat --no-daemon :test --tests '*GimjeDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GIMJE' -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GIMJE' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

초기101검사 통과, 확대 Java278개 실패·생략0, bootJar 성공, Node23개 통과다. 관측 명령2회는 기록 생성에 성공(exit0)했으며 첫 관측의 파일 실패와 두 번째 본문 실패까지 성공으로 바꾸지 않는다. 기존300영수증/240공고 및 타 지역 근거를 보존한302영수증/241공고의 해시·최신 표본·대장 재현을 통과했다. 전체 goal은 계속 진행한다.
