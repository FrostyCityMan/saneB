# 완도 고시공고 본문·공개 POST 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일 수집을 우선하며 발견·전송·본문·추출 실패는 별도로 보존한다. HWP 추출기 개선은 보류하고 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E를 포함한 전체 goal은 미완료다.

- [x] 등록 수집원에 해당하는 공식 완도 고시공고 상세 구조와 다운로드 폼 조사.
- [x] 완도 본문 선택기·첨부 프로필·고정 표본 및 회귀 테스트 구현.
- [x] Node23개 수집 근거·집계 검사 통과.
- [x] Java280개·bootJar·실제 HWP 다운로드 관측 통과.
- [x] 303영수증/242공고 및 지역 대장 재현 검증.
- [x] 관측 임시 원본 정리 확인.

## 조사 근거

V61 등록 수집원은 `LGS-000197 / SPRING_BBS`, 목록은 `https://www.wando.go.kr/wando/sub.cs?m=318`이다. 공식 검색 결과에서 [2026년 소상공인 디지털 전환 지원사업 신청ㆍ접수 안내](https://www.wando.go.kr/wando/sub.cs?m=1031&nttId=30322)를 찾고 동일 수집기 User-Agent와 TLS 검증을 사용한 직접 상세 조회2회로 HTTP200·제목·본문·첨부 구조를 확인했다. 과거 목록400 기록은 삭제하지 않으며 등록 목록·운영 설정도 변경하지 않는다.

공식 상세의 `board_basic_view` 아래 `news_tit > h3`, `board_cont`, `file_attach > attach_thum > ul`을 구분한다. 실제 원문에 표 밖의 td 요소가 섞여 있으므로 Jsoup으로 정규화된 구조를 검사하며 전체 페이지·부서·메뉴를 본문으로 대체하지 않는다.

첨부는 `공고문_2026년 소상공인 디지털 전환 지원사업 신청접수 안내_완도군.hwp` 1개다. 공식 `goDownLoad` 함수는 공개 파일 인자3개를 `form[name=nnn]`의 `user_file_nm`, `sys_file_nm`, `file_path`로 전달하고 `https://eminwon.wando.go.kr/emwp/jsp/ofr/FileDownNew.jsp`에 POST한다. 인자를 해독·추측하지 않고 공식 폼의 고정 필드만 사용한다. JavaScript 코드를 실행하지 않는다.

부평 `LGS-000060`은 공식 과거 안내에서 연결된 상세33400을 조회했으나 HTTP404였다. 과거 주소의 현재 유효성을 확인하지 못했으므로 부평 처리기·다운로드 성공 수에 반영하지 않았다. 옹진 검색에서는 군의회 등 등록 범위 밖 자료만 나왔으며 임의로 수집 범위를 확대하지 않았다.

직접 조사 요청은4GET(부평 상세1, 완도 목록1, 완도 상세2), 요청당15초다. 완도 목록의200 응답만으로 목록 파싱 성공을 주장하지 않는다. 검색 질의6회와 검색 도구의 완도 상세 열기1회는 별도이며 상세 열기는400/timeout이었다. 로컬 직접 상세200과 검색 도구 실패를 혼동하지 않는다. 원문은 메모리에서만 처리했고 파일로 저장하지 않았다. 실제 Java 관측은6요청·23MiB 상한으로 분리한다.

## 구현 경계

- `WandoNoticePage`: 고시공고 상세 메뉴1031만 적용하고 제목·본문·첨부 수와 첨부 영역을 확인한다.
- `WandoAttachmentDiscoveryProfile`: 시스템 수집원·URL hash·공식 상세 호스트·파일 호스트·정확한 POST 경로·hidden3필드를 검증한다.
- 최초 요청과 달라지는 redirect·외부 호스트·변경된 폼·추가 실행문·경로 조작은 거부한다.
- 미지원 형식과 미해석 링크를 별도 기록하고 정상 파일 descriptor를 보존한다. 첨부 수 불일치도 정상 파일을 폐기하지 않는다.
- 최대10파일·중복 제거·역할 UNKNOWN·기존 파일 서명/MIME/파일명 검증을 유지한다.
- 카탈로그 참조만 추가하고 기대값은 null이다. 키워드 정책·공통 다운로드 엔진·다른 지역 프로필·DB/API·migration·추출기는 변경하지 않는다.

운영 DB/설정/worker/정책 변경·ENFORCE·재분류·배포는 이번 범위가 아니다. 브라우저는 현재 사용자 정책에 따라 미실행이다. AWS 인증 갱신 및 이전 연제/구례 조사 원본 정리 미완료는 별도 후속이다.

## 검증 결과

2026-09-30 13:20:59 KST 실제 Java 관측:

| 항목 | 결과 |
|---|---|
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE,566자 / TARGET_SUPPORT_CONFIRMED |
| 첨부 발견 | FOUND,complete=true,1개 |
| 파일 | HWP176,640byte, 기존 서명/MIME/파일명 검증 통과 |
| 관측 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| 요청 예약 / byte 예약 | 4 / 2,404,864 |
| 임시 원본 | 정리 확인 |

프로필 지문은 `f1508609522ce10f8d4ea736734da6b8d1283dc1293260ca9ff5691a5240a12f`다. `build/reports/attachment-regional-collection/WANDO-30322.json`의 비식별 관측 metadata·해시를 영수증 인덱스와 `wandoDownloadRun` 대장에 반영했다. 다운로드 성공은 내부 텍스트 추출·정책 QA·상시 운영·관리자 E2E 완료가 아니다.

**164/223수집원(73.5%) 확인·59개 잔여(미연결31+등록 미확인28)**다. 프로필은 지역192+기업마당1=193개, 카탈로그251공고/192대상이며 참조 전용250개와 기존 기대값1개다. 분모는2026-09-28 15:56:12 KST 활성 수집원 스냅샷이며 고유 행정구역 수가 아니다. 첨부 오류 관측 수집원43개와 기존 엄격한3표본·전체 첨부 Gate16충족/207잔여는 별도로 유지한다.

실행 명령:

```powershell
.\gradlew.bat --no-daemon :test --tests '*WandoDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=WANDO' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

Java280개 실패·생략0, bootJar·실제 관측 성공(exit0,3분25초), Node23개 통과다. 기존302영수증/241공고와 타 지역 근거를 보존한303영수증/242공고의 해시·최신 표본·대장 재현이 통과했다. 등록 목록 유입·상시 worker·추출·운영 Gate는 미완료이며 다음 미연결 지역 확장과 분리된 오류 처리를 계속한다.
