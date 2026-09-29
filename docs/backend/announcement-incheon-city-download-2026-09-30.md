# 인천광역시 본문·첨부 발견 연결과 응답 오류 분리

## 현재 단계 / Gate

전 지역에서 수집 가능한 첨부를 먼저 확보하고 발견·다운로드·추출 실패는 별도 기록한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 이번 범위가 아니다.

- [x] 인천광역시 LGS-000054 / SAFE_INCHEON_CITYNET_NOTICE 본문·첨부 발견 연결.
- [x] 기존 HTTP 소스 식별자 보존, 실제 상세·파일 요청은 검증된 HTTPS로 한정.
- [x] 실제 EUC-KR 본문58자·HWPX로 표시된 첨부1개 발견.
- [!] 파일74,696byte 수신 후 MIME 검증 실패. 다운로드 성공으로 집계하지 않음.
- [x] 표적113건 통과, 확대221건 통과·조건부1건 생략, Node23/23·bootJar 성공.
- [x] 266영수증/220공고 재현, 기존 근거 보존, 조사 원본·실행 자원 정리.
- [ ] 남은84지역: 미등록47 + 등록 후 다운로드 성공 미확인37.
- [ ] 첨부 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E의 전체 Gate.

## 공식 조사와 구현

등록 목록 `http://announce.incheon.go.kr/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchList&flag=gosiGL&svp=Y&sido=ic`과 동일 주소의 HTTPS에서 각각 HTTP200을 확인했다. 운영 source URL이나 DB는 변경하지 않았다.

실제 `myform`의 EUC-KR POST 검색 필드 `conTitle`, `currPageNo`, 화면에 표시된 검색 기간을 사용했다. 목록의 `viewData`와 기존 SAFE_TEMPLATE 파서가 만드는 상세 경로가 일치한다. 별도 인증·세션·토큰을 주입하지 않고 공개 조회했다.

- 표본: `INCHEON_CITY-66970`
- 제목: 2026년도 하반기 소상공인시장진흥자금 융자 계획 공고
- 원래 식별 URL: `http://announce.incheon.go.kr/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchDetail&flag=gosiGL&svp=Y&sido=ic&sno=66970&gosiGbn=A`
- 실제 요청은 동일 고정 호스트의 HTTPS로 전환한다. 현재 신청 가능·지원 자격 충족을 확인한 것은 아니다.

`IncheonCityNoticePage`는 명시된 command·flag·svp·sido·숫자 sno·고시 구분의 정확한6개 query를 검증한다. HTTP의 기본80포트 또는 HTTPS의 기본443포트로 저장된 식별자만 인정하며, 실행 URL은 HTTPS로 만든다. `form[name=myform]`의 sno/gosiGbn/flag를 URL과 대조한다. 자기 테이블에 제목 label이 있는 단일 표에서 제목·본문·첨부를 분리한다. 본문은 ‘내용’ label 뒤 구분선과 `td.tb_left[colspan=4][wrap=VIRTUAL]`의 관계를 검증한다. 연락처·담당부서·파일명은 본문 분류에 포함하지 않는다.

`IncheonCityAttachmentDiscoveryProfile`은 첨부 셀 내부 표의 직접 링크만 선택한다. HTTPS 동일 호스트 `/citynet/jsp/cmm/attach/download.jsp`와 mode=download·숫자 index·고정 길이 fid/other만 허용한다. 파일 식별값은 메모리 요청에서만 사용하고 locator에는 SHA256을 기록한다. POST·redirect·임의 링크는 허용하지 않는다. 최대10파일·중복/상충·미지원 형식·부분 실패/정상 파일 보존을 유지한다.

본문 client에도 인천시 한정 HTTPS 변환과 리다이렉트 차단을 연결했다. 기존 지역의 요청 규칙은 변경하지 않았다. 기존 상세 바이트 파싱의 EUC-KR 처리를 그대로 사용하며 인코딩을 추측해서 덮어쓰지 않는다. Spring 프로필·QA 실행기·카탈로그에 등록했고 expectation은 null이다. migration·DB·API·UI·공통 파일 검증기·추출기는 변경하지 않았다.

## 실제 결과와 남은 오류

2026-09-30 06:04 KST(UTC `2026-09-29T21:04:18.784863600Z`) 관측:

- 프로필: `LOCAL_INCHEON_CITY_CITYNET_V1`
- 지문: `62ef746d7f8b4a9fcd820e0dc448a191683a03b3ff8a7c46d302a3025ab497fa`
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE58자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true·1개.
- 파일 실패 단계: FILE_SIGNATURE.
- 오류: ATTACHMENT_CONTENT_TYPE_MISMATCH.
- 수신74,696byte, SHA256 `220ba26656d6626505bcb896efacb01d682330feda18627dc80202f5ce102fa5`.
- 결과 COLLECTION_ONLY_PARTIAL_NOT_APPROVED, 원본 정리true, 운영 쓰기0.

추가 진단 GET1회에서 같은 크기·해시, ZIP prefix, `Content-Type: application/file`, attachment 파일명 헤더를 확인했다. 파일명 헤더 octet은 엄격한 UTF-8 디코딩에 실패했다. 실제 헤더 charset을 확정하거나 임의로 복원하지 않았다. ZIP prefix만으로 HWPX 내부 구조·텍스트 추출 성공을 판정하지 않는다.

기존 공통 검증기의 MIME 예외에는 application/file이 없고, 기존 UTF-8 헤더 옵션만으로 해결할 수도 없다. 이 회차에서는 공통 검증 범위를 확대하지 않고 별도 응답 호환 후속으로 남긴다. 발견한 파일 전체를 삭제하거나 정상 후보로 위장하지 않으며, 프로필은 다른 정상 파일을 독립적으로 처리할 수 있다. 운영 상시 작업에서 실제로 그렇게 동작했다는 증거는 이번 회차에 없다.

## 집계·예산·검증

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다. 최소1파일 다운로드 확인139/223(62.3%)·잔여84는 유지한다. 미등록48→47, 등록 후 다운로드 미확인36→37이다. 첨부 오류가 남은 지역은43→44이며 성공 지역과 중복될 수 있다. 기존3표본·전체 파일 Gate는16충족/207잔여로 유지한다.

지역176+기업마당1=177프로필, 카탈로그232공고/175대상, 영수증266개/최신220공고다. 본문·첨부 연결 완료를 다운로드 성공이나 전체 운영 완료로 표현하지 않는다.

```powershell
# 실제 인천 HTML fixture와 기존 운영 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*IncheonCityDownloadContractTest' --tests '*ChungcheongThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=INCHEON_CITY -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적 실행은 테스트 괄호 오타로 compileTestJava 실패(21초). 수정 후113건 통과(33초). 확대222건 중221통과·실패0·조건부1생략, bootJar와 관측 실행 완료(2분6초)다. 생략은 원본이 이미 정리된 기존 충청 실물 fixture1건이며 인천 실제 EUC-KR HTML은 통과했다. 실제 관측 태스크 성공을 파일 다운로드 성공으로 간주하지 않는다. 원본 정리 후 인천 fixture 환경변수 없이 실행하면 인천 실제 HTML 검사도 조건부 생략된다.

조사5요청: HTTP 목록·HTTPS 목록·검색·상세·진단 파일 GET 각1. 각15초·HTML2MiB, 진단 파일1MiB로 제한했다. 실제 관측1회는 최대6요청/23MiB·상세1MiB다. 본문 포함 관측 예약4회·2,188,232byte, 조사 포함 누적 예약 상한9회다. 예약 수를 실제 wire 요청 수로 단정하지 않는다. 조건 없는 반복 재시도는 하지 않았다.

조사 HTML·헤더·진단 HWPX7개164,805byte는 절대 경로·크기·SHA256 대조 후 삭제했다. 실제 관측 파일 원본도 실행기가 정리했다. 근거 보고서·해시는 보존하며 원본 복구는 공식 사이트 재조회가 필요하다. 임시 편집 도구와 이번 Node·Gradle·Java 실행 자원을 정리하고 기존 다른 작업 프로세스는 보존했다.

전체 프로젝트 테스트·첨부 텍스트 추출·AWS·운영 DB·운영 상시 유입·운영 E2E는 이번 회차 미실행이다. 현재 요청에 브라우저 지시가 없어 정책상 생략했다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용은 변경하지 않았다. 다음은 다른 미등록 지역 연결을 우선하며 인천 파일 응답 호환을 별도 오류 목록으로 유지한다.
