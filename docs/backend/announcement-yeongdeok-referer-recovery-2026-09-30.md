# 영덕 Referer·UTF-8 헤더 복구와 공통 전송 재검증

## 현재 단계 / Gate

- [x] 영덕 공개 첨부의 Referer 누락과 원시 UTF-8 Location 헤더 오류 재현·수정
- [x] 같은 공고·공식 호스트·공식 임시 경로 제한, 잘못된 인코딩·다른 공고·인증 경로 차단 테스트
- [x] 수정 후 선택 Java 23개와 bootJar 통과
- [x] 최종 회귀443통과·조건부15생략·실패0, bootJar 통과
- [x] 최종8개 수집 유형 모두 최소1파일 확인, 377영수증/277공고 대장 재현
- [~] 과거 관측 포함 누적197/223수집원(88.3%)에서 다운로드 경험, 아직 미확인26
- [~] 최신 공통 코드의 실제 다운로드 재확인8/223, 나머지215는 신규 확인 또는 재검증 필요
- [ ] 다른 지역의 최신 공통 전송 코드 재검증, 다운로드 미확인 지역 복구
- [ ] HWP 추출 고도화·상시 운영 수집·정책 승인·운영 E2E

분모223은 2026-09-28T15:56:12.116778+09:00 읽기 전용 운영 인벤토리의 활성 지역 수집원이다. 현재 운영 재조회나 고유 지자체 수를 뜻하지 않는다. 이 작업은 발견·다운로드 단계이며, 파일 성공을 추출·최종 분류·자동 활성화 성공으로 표현하지 않는다.

## 변경 전 근거 보존

시작 HEAD는 `4ae48409616c5b0dc51fdf94e3ed27bfa693346f`다. 변경 전 인벤토리로 **196/223 수집원 다운로드 확인·27잔여·미연결1·엄격한 3표본 전체 집합 Gate16/223**을 재현했다.

- 과거 인벤토리: `build/reports/attachment-target-inventory/before-referer-20260930.json`
- SHA-256: `2d62e8d666e46afab8969eb823bbc8202f11231a4d6f70abe683b8d044163635`
- 기존 영수증361개·최신 공고277개를 보존한다.
- 공통 `Request`·다운로드 클라이언트·프로필 인터페이스가 변경되어 모든 실행 지문이 바뀐다. 과거 영수증의 profile hash·producer hash를 새 값으로 바꾸지 않는다. 같은 코드에서 검증한 현재 수치와 과거 수집 실적은 별도다.
- 기존 태백 기대값은 그대로 남기며 새 코드에서는 `PROFILE_CHANGED`로 재검증을 요구한다. 승인 기대값을 자동 갱신하지 않는다.

## 원인과 구현

1. 영덕366709의 공식 첨부 링크는 Referer가 없으면 HTTP200 HTML 364byte를 반환했다. 공개 상세 Referer가 있으면 같은 호스트의 공식 임시 HWP로 HTTP302가 발생했다.
2. 초기 Referer 수정 후 실제 공통 클라이언트에서는 `ATTACHMENT_URL_BLOCKED`가 발생했다. raw `Location`이 한글 파일명을 UTF-8 원시 바이트로 전달한다는 것을 확인했다. HTTP 라이브러리의 헤더 해석을 합성 테스트로 재현했고 수정 전 동일한 실패를 확인했다.
3. 영덕 프로필만 공개 상세 Referer와 엄격한 UTF-8 원시 Location 해석을 선택한다. ASCII·percent-encoded Location은 그대로 둔다. 잘못된 UTF-8, 제어 문자, 이중 percent decoding은 허용하지 않는다.
4. Referer는 초기 공고 UID로 생성하며 HTTPS·동일 호스트·443·사용자 정보 없음·fragment 없음으로 제한한다. 자유 헤더 입력·쿠키·인증 세션은 추가하지 않았다.
5. 리다이렉트는 `www.yd.go.kr`의 `/wp-content/uploads/kboard_temp/<13자리 hex>/<파일명>.hwp|hwpx|pdf`만 허용한다. query·다른 공고·다른 호스트·로그인·임의 uploads·경로 탈출·실행 파일은 차단한다. 임시 주소는 descriptor locator에 저장하지 않는다.
6. DNS pinning, TLS 검증, 요청·용량·시간 제한, 실제 파일 시그니처 검증, 실패 파일 정리, 부분 성공 보존을 유지한다. 기본 프로필과 기존 Request 생성자는 Referer·UTF-8 호환을 사용하지 않는다.

DB·API·화면 계약, Flyway V85, HWP 추출기1.0.16은 변경하지 않았다. 카탈로그286사례·222대상·reference-only285·기존 기대값1도 변경하지 않는다. 괴산 프로필이 이미 추가된 데 비해 뒤처진 세 registry fixture의 개수와 괴산 포함 검증만 현재 등록 상태로 맞췄다.

## 제한된 공식 HTTP 조사

직접 조사 총9요청, 요청당12초·최대2MiB, TLS 검증·collector UA 유지, 자동 redirect 추적 없음으로 실행했다. 공개 상세와 첨부 응답·Location만 조사했으며 원문 파일을 보관하지 않았다. 조사 중 공식 임시 HWP는135,680byte·OLE signature를 확인했지만, 이 별도 HTTP 결과를 수집 harness 성공으로 대체하지 않는다.

성주586507 첨부는 Referer를 넣어도 권한 없음 HTML374byte가 반환됐다. 권한 검사를 우회하지 않았다. 연제 형식 불일치와 충주 HTTP400도 이 영덕 수정의 성공 범위에 포함하지 않는다.

## 실제 수집과 최종 검증

`REFERER-V1-20260930`과 `REFERER-V2-20260930` 라벨로 관측을 분리하여 초기 실패를 덮어쓰지 않았다. 최종 배치는8개 지역 모두 실제 파일을 확인했으며, 발견 경고·다운로드 실패·형식 검증 실패는 파일별로 남겼다.

| 고정 공고 | 본문 문자 | 최종 다운로드 | 별도 오류·남은 조건 |
| --- | ---: | --- | --- |
| 영덕366709 | 1,579 | HWP1, 135,680byte | 최초 URL 차단 기록 보존, 최종 전체1파일 성공 |
| 괴산29655 | 287 | HWP1, 144,896byte | 최종 전체1파일 성공 |
| 강동37327 | 1,163 | HWP2, 50,688byte | 미리보기 관련 발견 경고 유지 |
| 서울 강서66840 | 550 | PDF1·HWPX1, 284,716byte | 최종 전체2파일 성공 |
| 오산50603 | 125 | HWP2, 280,576byte | 최종 전체2파일 성공 |
| 진주64420 | 146 | HWPX1, 132,063byte | 첫 배치 상세 시간 초과, 최종 발견 경고 유지 |
| 유성49380 | 495 | HWP1, 68,096byte | 다른 PDF는 시간 초과 |
| 합천44432 | 4,517 | HWPX1, 87,214byte | 다른 파일159,232byte는 형식 불일치, 발견 경고 유지 |

최종 성공11파일·1,183,929byte다. 실패 파일에 바이트·해시가 있어도 성공으로 집계하지 않는다. 영덕 HWP SHA-256은 `cb59f516dcc3cb163cfe8ded5a6e4d99fa272844cecdabf48c8f802a3af8eb85`로 별도 HTTP 조사와 실제 harness가 일치했다. 두 배치16개 보고서 모두 `originalFilesRemoved=true`, 운영쓰기0, 추출·정책 QA·기대값 승인false다.

이번 8개 고정 표본은 영덕·괴산·강동·서울 강서·오산·진주·유성·합천이다. 실행은 한 JVM에서 순차 처리하고 각 표본의 기존 예산을 유지한다. 한 배치의 상한은52요청·187MiB다. 요청 예약량은 실제 wire 요청 수가 아니라 본문 상한을 포함한 보수적 예약 값이다.

- 첫 배치: 요청예약40, 바이트예약22,207,082, 성공9파일916,186byte. 진주 상세 시간 초과로 Gradle exit1이며 영덕 URL 차단도 별도 기록했다.
- 최종 배치: 요청예약42, 바이트예약22,703,210, 성공11파일1,183,929byte, Gradle exit0. 부분 실패가 있는4개 지역을 전체 파일 성공으로 승격하지 않는다.
- 합계 예약82회·44,910,292byte. 직접 조사9요청은 이 수치와 별도다. 두 배치 파일 수를 서로 다른20개 파일로 표현하지 않는다.
- 새 인벤토리 SHA-256: `8186842d80c326c7d7c215e59724c469c07ace7a66ab1ba9fd4e4b14403c6a28`
- 보고서 producer SHA-256: `99427d305bb8148d1b4e3c41306905a1985b2de2cc733c8ce976d7a05e86fcf1`
- 기존361영수증은 전부 보존하고16개를 추가하여377영수증/277최신공고다. 8개 공고만 최신 관측으로 교체했으며, 기존 카탈로그는 Git HEAD와 내용 일치를 확인했다.

### 수치 해석

과거196개에 영덕1개가 추가되어 다운로드 경험이 있는 수집원은 누적197개다. 이는 **현재 코드에서197개가 모두 재검증됐다는 뜻이 아니다**. 현재 코드의 재검증은8개이며, 나머지215개는 과거 성공189개와 아직 다운로드 미확인26개다. 최신 코드 기준 3표본 전체 집합 Gate는0/223이며 변경 전16/223은 과거 근거로 유지한다. 지역 구현을 삭제한 것이 아니라 변경된 공통 실행 지문에 맞춰 검증 상태를 분리한 것이다.

다운로드 미확인26개는 미연결 울산 남구1개와 등록된25개(은평·서대문·송파·부산광역시·연제·검단·성남·평택·포천·이천·동두천·강릉·속초·평창·철원·충북도·충주·영동·천안·공주·서천·순창·진도·의성·성주)다. 이 목록은 과거 관측 포함 미확인 목록이며 모든 기관의 현재 장애를 재확인한 결과는 아니다.

### 실행 명령 / 결과

1. `.\gradlew.bat --no-daemon :test --tests '*AttachmentRefererDownloadTest' --tests '*AttachmentPinnedDownloadClientTest' --tests '*GyeongbukFifthDownloadContractTest' :bootJar`
   - UTF-8 원시 헤더 실패를 먼저 재현한 뒤 수정했다. 수정 후23개 통과·bootJar 통과.
2. 인벤토리 입력을 프로세스 한정으로 설정한 뒤 다음을 실행하고 환경변수를 복원했다.

```powershell
.\gradlew.bat --no-daemon :test --tests 'com.saneb.domain.announcementattachment.discovery.*' --tests '*AttachmentRefererDownloadTest' --tests '*AttachmentPinnedDownloadClientTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*GyeongbukFifthDownloadContractTest' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AnnouncementAttachmentIntakeServiceTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONGDEOK,GOESAN,GANGDONG,SEOUL_GANGSEO,OSAN,JINJU,YUSEONG_SUPPORT,HAPCHEON' -PsanebCollectionReportLabel=REFERER-V2-20260930 -PsanebCollectionWindowsTrust=true
```

   - 회귀 XML458개 중443통과·15조건부생략·실패0, 관측8개 정상 종료, bootJar 통과. 생략은 opt-in 지역 live QA와 별도 로컬 원본 fixture 입력이며 성공으로 계산하지 않았다. Windows 신뢰 저장소를 명시했으며 TLS/호스트 검증은 해제하지 않았다.
3. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과.
4. `node scripts/qa/verify-collection-receipt-index.mjs`, `node scripts/qa/report-collection-availability.mjs`:377/277·현재8/223·미연결1·엄격0 재현.
5. `git diff --check`: 통과. 기존 영수증 prefix와 카탈로그 보존을 별도 assertion으로 확인했다.

## 미실행과 후속

운영 DB·설정·worker·정책·ENFORCE·재분류·배포는 실행하지 않는다. 전체 Java 테스트, 운영 E2E, AWS 검증은 미실행이며 브라우저는 현재 사용자 지시가 없어 정책상 미실행이다. 기존 연제·구례 임시 폴더의 정리 차단은 별도 보류이며 이번 원본 정리 결과로 덮지 않는다.

최신 코드로 다른 지역의 재검증을 순차 배치로 이어가되, 파일·네트워크 실패는 별도 오류로 남기고 성공한 파일은 보존한다. 울산 남구 미연결, 천안·동두천 제목 적합 표본, 송파 제목 식별, TLS·HTTP 오류 등 기존 잔여 사항도 병행한다. 최종 관리자 검증과 자동 활성화 금지는 유지한다.
