# 서해구 공식 게시판 본문·첨부 연결

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드를 우선하고 개별 발견·다운로드·파싱 실패는 분리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기1.0.16 개선·운영 설정·DB 변경은 이번 범위가 아니다.

- [x] LGS-000062 / HEURISTIC_NOTICE / LOCAL_SEOHAE_BOARD_V1 연결.
- [x] 공식 상세 제목 확인, 본문219자, HWP1개60,416byte 다운로드·기본 파일 형식 검증.
- [x] 관련 Java230통과·조건부1생략, bootJar, Node23/23,270영수증/224최신공고 재현.
- [x] 조사 HTML·헤더6개364,074byte 및 이번 실행 자원 정리.
- [ ] 잔여82개 수집원: 미등록43 + 등록 후 다운로드 성공 미확인39.
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 공식 조사와 구현

등록 목록은 `https://seohae.go.kr/open_content/main/community/news/gosi.jsp`다. 목록의 실제 GET 검색 폼 `/open_content/main/bbs/bbsMsgList.do`에서 `bcd=gosi`, `keyfield=title`, `keyword=소상공인`, `listsz=10`을 사용했다. 목록·검색·상세3요청 모두200이었다. 사이트 명칭은 현재 등록 수집원 기준이며 행정구역의 고유 개수로 집계하지 않는다.

- 표본: `SEOHAE-42495`, 2025년도 소기업, 소상공인 특례보증 추천 공고.
- 상세: `https://seohae.go.kr/open_content/main/bbs/bbsMsgDetail.do?msg_seq=42495&bcd=gosi`.
- 파일: 같은 호스트 `/open_content/main/bbs/bbsMsgFileDown.do`, `bcd=gosi`, `msg_seq=42495`, `fileno=1`.
- 2025년 과거 공고를 연결 검증에 사용했다. 현재 신청 가능한 공고로 표현하지 않는다.

`SeohaeNoticePage`는 단일 `div.board_view`의 `h4.title`, 직접 자식 `div.con`, `첨부파일` label에 연결된 `dd`를 분리한다. 부서·연락처·메뉴·푸터·첨부 파일명은 본문 분류에 넣지 않는다. 검색 query는 원문 identity에는 보존하고 실제 첨부 상세 fetch에서는 검증 후 제거한다.

`SeohaeAttachmentDiscoveryProfile`은 공식 첨부 영역의 직접 링크만 사용한다. source/parser·HTTPS443·같은 호스트·고정 경로/query·같은 공고 번호·GET을 제한한다. POST·리다이렉트·임의 호스트·다른 공고 파일은 허용하지 않는다. 파일 아이콘·크기 표시는 알려진 형식만 제외하며, 같은 파일 식별값의 `bbsMsgFileView.do` 미리보기 링크는 요청하지 않는다. 알려지지 않은 링크·버튼·상충 식별은 발견 오류로 남기고 이미 찾은 정상 파일은 보존한다. 최대10개, 중복 정리, 미지원 형식 비다운로드, 문서 역할 UNKNOWN을 유지한다.

기존 인천권 프로필·공통 파일 검증기·추출기 지문을 바꾸지 않았다. DB/migration·API/UI·운영 정책도 변경하지 않았다. 카탈로그에는 기대값 승인 없는 reference-only 표본1개를 추가했으며 기존235개 항목을 보존했다.

## 실제 관측

2026-09-30 07:11 KST(UTC `2026-09-29T22:11:39.899214800Z`):

- 프로필 지문: `e3501c7052d74c3e971c97d84dc6d7833ceb05412868bc4589a97fe528158323`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE219자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·HWP1개 DOWNLOADED·60,416byte.
- 파일 SHA256: `40234c9beae5792ecdcd08b182f5e9b54ffa2cd9295ff94c01499cdc37a18647`.
- COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 다운로드 원본 정리true, 운영 쓰기0.

이는 파일 다운로드와 기본 형식 검증 근거다. HWP 내부 텍스트 추출·문서 구간 분석·정책 승인·상시 운영 성공 근거가 아니다.

## 집계와 검증

분모는2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223개 지역 수집원이다. 운영 재조회는 하지 않았다. 최소1파일 다운로드 확인140→141/223(63.2%), 잔여83→82, 미등록44→43, 등록 미확인39 유지다. 첨부 오류46개는 성공 수집원과 중복될 수 있다. 기존3표본·전체 파일 Gate는16충족/207잔여다.

지역180+기업마당1=181프로필, 카탈로그236공고/179대상(235reference-only),270영수증/224최신공고다. 표본1개의 성공을 모든 공고·하위 경로·전체 운영 완료로 표현하지 않는다.

```powershell
# 실제 서해구 HTML fixture 환경변수로 1차 검증
.\gradlew.bat :test --tests '*SeohaeDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon
# 실제 fixture와 기존 읽기 전용 inventory 환경변수로 확장 검증
.\gradlew.bat :test --tests '*SeohaeDownloadContractTest' --tests '*IncheonThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SEOHAE -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

1차119/119통과(53초), 확장231건 중230통과·실패0·조건부1생략 및 bootJar·실제 관측 완료(2분4초)다. 생략은 원본이 이미 정리된 인천권 실제 HTML fixture다. 서해구 실제 HTML 검사는 통과했다. 원본 정리 후에는 fixture 환경변수를 켜지 않으며, 재검증을 위한 외부 재요청은 자동으로 실행하지 않는다.

조사3요청(각15초/2MiB), 실제 관측1회 최대6요청/23MiB·상세1MiB, 본문 포함 실제 예약4회/2,272,256byte, 조사 포함 예약 상한7회다. 예약과 wire 요청 수를 동일시하지 않는다. 조사 원본6개는 절대 경로·크기·SHA256 대조 후 삭제했고, 다운로드 원본은 실행기가 정리했다. 복구하려면 공식 사이트 재조회가 필요하다. 보고서·해시는 보존한다. 이번 Node·Gradle·Java는 종료했고 기존 프로세스와 관련 없는 미추적 파일은 보존했다.

전체 프로젝트 테스트·추출·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저 검증은 현재 단계의 명시적 요청이 없어 정책상 생략했다. 작업 브랜치 `[skip deploy]` 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 실행하지 않았다.
