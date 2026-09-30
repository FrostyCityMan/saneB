# 강남·도봉 본문·첨부 연결과 도봉 파일명 헤더 복원

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·파싱 오류는 별도로 남긴다. 제목→본문→첨부→관리자 최종 검증, 상시 worker·DB/API·관리자 UI·운영 E2E 전체 목표는 유지한다. HWP 추출기 추가 개선은 이번 범위가 아니다.

- [x] 강남·도봉 공식 목록의 ‘지원’ 검색으로 고정 표본 확보.
- [x] 시스템 프로필2개, 본문 선택기, 첨부 영역·미리보기 분리 구현.
- [x] 도봉 공개 사전 확인 POST→OK→개별 파일 GET 절차 구현.
- [x] 도봉 UTF-8 파일명 헤더의 실측 오류 복원, 기존 실패 보고서 보존.
- [x] 관련 Java350건·bootJar·Node23건·실파일8개 검증,286영수증/228최신 공고 재현.
- [ ] 나머지69개 수집원 및 추출·상시 운영·운영 E2E 전체 Gate.

## 구현과 기존 조사와의 차이

과거 강남·도봉의 ‘소상공인’ 제목 검색 결과 없음은 전체 지원 공고 부재가 아니었다. 이번에는 공식 검색 화면이 제공하는 제목 ‘지원’으로 범위를 넓혀 각각 대학생·중소기업 인턴십, 청년 지원금 공고를 확보했다. 분류 키워드·제목 제외 정책을 변경한 것은 아니다.

`SeoulEighthNoticePage`와 `SeoulEighthAttachmentDiscoveryProfile`을 시스템 설정으로 등록한다. 공통 파일 검증기·기존 프로필 구현은 수정하지 않는다. 기존 migration·DB·API 계약·운영 parser 설정도 변경하지 않는다.

| 수집원 | 강남 LGS-000024 | 도봉 LGS-000011 |
|---|---|---|
| 목록 parser 바인딩 | SAEOL_GOSI | SPRING_BBS |
| 첨부 프로필 | LOCAL_GANGNAM_BOARD_V1 | LOCAL_DOBONG_BOARD_V1 |
| 상세 | www.gangnam.go.kr/notice/view.do | www.dobong.go.kr/WDB_DEV/gosigong_go/detail.asp |
| 제목 | bbs-view의 post-title, 첫 br 이전 | boardView의 td.title |
| 본문 | post-content | bbsView 직하위 bbsCont |
| 첨부 | bbs-view-file의 fileListCollap | boardView의 ‘첨부파일’ 셀 |
| 다운로드 | gangnam.eminwon.seoul.kr의 FileDown_gn.jsp GET | 같은 도봉 호스트의 공개 사전 확인 POST 후 download_unitsvc_gosing.asp GET |

강남은 상세에 있는 파일명·저장명·디렉터리의 세 query 필드를 검증한다. 미리보기는 같은 저장 파일 ID·순번과 일치하는 보조 링크만 제외하고 실행하지 않는다. 도봉은 `filedown(fcode,idx)`의 숫자 두 개만 읽으며 상세 idx와 일치해야 한다. 도봉의 `/WDB_DEV/gosigong_go/ajax_user_attach.asp`에 해당 idx만 POST하고 응답이 정확히 OK인 경우에만 파일을 받는다. nodata·error·다른 응답은 해당 파일의 오류로 분리한다. JavaScript 실행·미리보기·접근 확인 우회는 없다.

유효한 PDF/HWP/HWPX를 최대10개까지 보존한다. 미해석 링크·잘못된 항목·미지원 형식 때문에 다른 정상 파일을 폐기하지 않는다. 첨부 영역 누락을 NO_FILES로 바꾸지 않고, 파일 역할은 UNKNOWN으로 남긴다. 파일 다운로드는 텍스트 추출이나 관리자 최종 확정을 의미하지 않는다.

도봉 최초 관측은 본문·첨부 발견에 성공했지만4파일 모두 ATTACHMENT_DISPOSITION_INVALID였다. 원본 응답은 Content-Disposition에 UTF-8 한글 파일명을 직접 담고 있었다. 도봉 전용 `DobongUtf8AttachmentDiscoveryProfile`로 기존 헤더 복원 옵션을 적용하되 사전 확인 flow를 그대로 위임한다. 공통 검증기·강남 지문은 변경하지 않았고 실제 바이너리 서명·확장자 검증을 유지했다. 성공한 강남은 재요청하지 않았다.

카탈로그는240공고/183대상, 참조전용239개·기존 승인 기대값1개다. 신규 두 공고의 expectation=null을 유지한다. 프로필은 지역184+기업마당1=185개다. 참조 등록을 정책 승인·운영 활성화로 해석하지 않는다.

## 실제 결과

| 항목 | 강남 GANGNAM-64668 | 도봉 DOBONG-4734 |
|---|---|---|
| 최종 관측 KST | 2026-09-30 09:49:08 | 2026-09-30 09:53:41 |
| 제목 | COMBINATION_MATCHED | COMBINATION_MATCHED |
| 본문 | AVAILABLE,314자 | AVAILABLE,493자 |
| 본문 단계 판정 | ACCEPTED/TARGET_SUPPORT_CONFIRMED | ACCEPTED/TARGET_SUPPORT_CONFIRMED |
| 첨부 발견 | FOUND/complete | FOUND/complete |
| 다운로드·형식 검증 | PDF2+HWPX2,1,350,436byte | PDF2+HWPX2,440,044byte |
| 최종 수집 상태 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |

8파일 합계1,790,480byte다. 도봉 최초 실패(09:48:54)는 DOBONG-4734-BEFORE-UTF8.json으로 보존했으며 보정 전후 파일 해시는 동일하다. 첨부 텍스트 추출·임시 DB/API·운영 상시 유입·정책 게시·ENFORCE·기존 데이터 적용은 실행하지 않았다.

실제 다운로드 확인은 **152→154/223(69.1%)**, 미확인은 **71→69(미등록39+등록 후 미확인30)**다. 최신 오류를 가진 수집원38개와 기존3표본·전체 첨부 Gate16개 충족/207개 잔여는 유지된다. 분모는 2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성 지역 수집원이며 고유 행정구역·전체 파일·운영 완료율과 구분한다. 타 지역의 최신 표본과 결과는 변경되지 않았음을 대조했다.

## 검증 기록

초기129건에서 강남 실제 HTML의 중간 레이아웃 요소 때문에 선택기1건이 실패했다. 본문 루트의 유일성을 유지하면서 레이아웃 래퍼를 허용하도록 수정했다. 그 후 확장 Java349/349·실패/생략0·bootJar(2분30초)가 통과했으며 실제 관측에서 강남 성공/도봉 헤더 오류를 분리했다.

도봉 보정·실파일 회귀 추가 후 Java350/350·실패/오류/생략0·bootJar(2분26초), 도봉4파일 수집 성공을 확인했다. 공식 상세 원본2건·헤더/바이너리 회귀1건도 실행했다. 원본 정리 후 해당3건은 환경변수 조건으로 기본 테스트에서 생략된다. 전체 프로젝트 테스트·이번 SHA의 Linux CI·운영 검증을 수행했다는 의미가 아니다.

```powershell
# SANEB_SEOUL_EIGHTH_SURVEY_FIXTURE=true 및 보관 inventory 영수증 환경은 실행 후 복원
.\gradlew.bat :test --tests '*SeoulEighthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEOUL_EIGHTH' -PsanebCollectionWindowsTrust=true --no-daemon
# 도봉 헤더 보정 후에는 같은 명령의 group=DOBONG으로 실행, 강남 재요청 없음
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Node23/23,286영수증/228최신 공고 재현 통과. 직전 담양 SHA dee7fc8의 [Linux CI](https://github.com/FrostyCityMan/saneB/actions/runs/36650984374)는 이번 조회 시 실행 중이었다. 이번 변경의 CI 성공 근거로 대신하지 않는다.

## 요청 예산·정리·남은 범위

- 조사11요청: 강남3GET, 도봉6GET+2POST. 요청당15초, HTML2MiB·사전 확인32KiB·진단 파일10MiB, 자동 redirect0·TLS 검증 유지. 도봉 공식302 두 단계는 관측된 Location만 개별 조회했다.
- 실제 관측: 강남1회(최대8요청/43MiB), 도봉2회(각 최대12요청/43MiB). 본문 최대2시도를 포함한 실제 예약은7+11+11=29회/9,318,394byte, 조사 포함 예약 상한40회다. 예약값을 실제 HTTP 횟수와 같다고 주장하지 않는다.
- 조사 원본22개/1,976,755byte는 검증된 정확한 경로·크기·해시 대조 후 개별 삭제했다. 복구에는 공식 사이트 재조회가 필요하다. 비식별 영수증·해시·최초 실패 보고서는 보존한다.
- 단발 Node·Gradle 자원은 종료하고 기존 사용자 프로세스·output/·scripts/qa/__pycache__/는 보존한다.
- [skip deploy] 커밋으로 이번 범위만 전달한다. 운영 DB·설정·정책·배포·재분류 변경 없음. 브라우저는 현재 사용자 정책에 따라 미실행이다.

다음은 미등록39개 연결과 등록 후 미확인30개 중 원인이 달라진 경우의 복구다. 오류를 별도 보존하면서 실제 수집 가능한 지역을 계속 확장한다.
