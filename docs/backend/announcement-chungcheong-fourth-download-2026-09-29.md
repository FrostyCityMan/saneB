# 홍성·예산 본문/첨부 연결과 실제 HWPX 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일을 먼저 수집하고 발견·전송·형식·파싱·본문 오류를 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 홍성·예산 공식 본문/첨부 경계와 프로필 2개 연결.
- [x] 홍성 HWPX 1개·예산 HWPX 2개 총 354,530byte 다운로드 및 signature/MIME/파일명 검증.
- [x] 예산의 큰 상세 HTML을 위한 시스템 프로필 전용 2MiB 상한과 worker/Provider QA/관측 전달 검증.
- [x] Java 324통과·조건부 1생략, Node 23통과, bootJar·실제 수집 관측 성공.
- [x] 214영수증/170공고 재현, 기존 212영수증/168공고와 기존 프로필 지문 보존.
- [x] 조사 HTML 9개 9,272,984byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 97지역 연결, 등록 후 다운로드 미확인 26지역 후속.
- [ ] 금산 공식 목록 확인 후 상세·첨부 연결은 다음 묶음으로 진행.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 98 | **100/223 (44.8%)** |
| 다운로드 미확인 | 125 | **123** |
| 프로필 등록 지역 | 124 | **126** |
| 미등록 지역 | 99 | **97** |
| 등록 후 다운로드 미확인 | 26 | **26** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 31 | **31** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 126+기업마당 1=127프로필, 카탈로그 182참조/125대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 오류 지역은 성공 파일과 오류가 공존하는 지역도 포함하므로 미관측 지역 수와 합산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 홍성 LGS-000160 / 44858 | 389자, AVAILABLE | HWPX 1개 132,470byte 성공 |
| 예산 LGS-000161 / 47075 | 333자, AVAILABLE | HWPX 2개 125,169byte·96,891byte 성공 |
| 금산 LGS-000156 | 공식 목록 1,992,853byte 확인 | 상세·첨부 미실행, 미등록 유지 |

세 파일은 production pinned transport와 기존 signature/MIME/파일명 validator를 통과했다. HWPX ZIP 내부 구조·텍스트 추출·분류·운영 DB 저장·관리자 검증을 완료했다는 의미는 아니다. 파일 역할은 UNKNOWN이며 임의로 공고/신청서 역할을 확정하지 않았다.

파일 byte hash:

- 홍성: `decca6d0c10d46d0cc217e426da1d4c08c99e16f899eafb43c681e58aaaf2c0a`
- 예산 1: `2c7f6f860704a95b3df5ccaa1ed85b28e66118cf9264a9be233794ae6bc59d10`
- 예산 2: `44579e7b982a0af52959f4173d99c735b8fe76a6e3a55410cd734403964c5aa0`

## 구현 계약과 용량 예외

- `ChungcheongFourthNoticePage`: 홍성은 `program--contents > ui.bbs--view`의 제목/본문/첨부, 예산은 `card.program--view > card-body.prog.bucket-form`의 라벨과 고정 span ID로 제목·본문을 분리한다. 예산 파일은 파일 라벨에 연결된 `bbs--view--file`에서만 찾는다. 메뉴·푸터·담당자·파일명이 본문에 섞이지 않는다.
- 공식 `fn_egov_downFile`의 고정 문자열 3개만 기존 선형 파서로 읽는다. 홍성 href 호출과 예산 onclick/return false를 구분한다. JavaScript를 실행하지 않는다.
- `user_file_nm`은 두 공식 핸들러의 `encodeURI(encodeURIComponent(...))` 동작을 재현한다. 공백·한글·괄호를 보존하고 역변환 및 재인코딩 일치를 검증한다. `sys_file_nm`과 `/ntishome/file/upload/ofr/ofr/YYYYMMDD`는 평문 검증 후 전달한다. 다른 폼·토큰·인증 값은 보내지 않는다.
- 홍성의 미리보기는 동일 3개 인자와 공식 경로가 일치하는 경우에만 대응 링크로 인정하며 요청하지 않는다. 예산의 실제 표본에는 다운로드 링크만 있다. 미확인 링크/미리보기와 정상 파일 descriptor는 분리하고 10파일 제한·미지원 확장자 미다운로드·UNKNOWN 역할을 유지한다.
- 예산 공식 상세는 1,145,882byte로 기존 1MiB 한도를 초과한다. 새 선택형 `AttachmentDetailLimitProfile`의 기본값은 기존 프로필 1MiB이며 실측한 예산에만 2MiB를 적용한다. 0·음수·임의 중간값·2MiB 초과는 거부한다. 예외 인터페이스와 값은 신규 프로필 지문에 포함한다.
- worker, 공통 Provider QA 실행기, 관측 상세 다운로드가 같은 상한을 사용한다. 관측 본문도 같은 응답 상한을 사용하고 최대2시도 예약 바이트를 반영한다. 파일당 20MiB·공고 전체 상한·SSRF/TLS/목적지 검증·동시 실행 제한·임시 저장 총량은 그대로다. 상세를 파일 다운로드 전에 삭제하므로 상세2MiB와 파일20MiB를 동시에 보관하지 않는다.
- 최초 공통 인터페이스에 메서드를 추가한 시도는 기존 프로필 지문 변경 회귀에 걸려 폐기했다. 최종 코드는 기존 `AttachmentDiscoveryProfile`과 `AttachmentProfileFingerprint`를 변경하지 않는다. 승인된 태백 지문과 기존 영수증/표본을 수정해 테스트를 통과시키지 않았다.
- 공통 validator·기존 프로필·Flyway·DB/API/UI·추출기·운영 정책·운영 worker 설정은 변경하지 않았다. worker 실행 코드의 상세 한도 선택만 추가했으며 운영에는 배포하지 않았다.

현재 profile hash: 홍성 `8048d2a3423d6e6756abfe9b913de4aeaf242ef57363d39f273205f772a02a9c`, 예산 `7426cde146c61645277bda3fc33f6ace087e49ed04df08db2f7e5b69e1854c1e`.

관측 producer class hash: `6eaeb9245d0e760a29b40bb38aaf5d431c543b94355c5442ebbdf29860431a52`.

## 요청량과 정리

조사 GET 9회: 홍성 3(목록/제목검색/상세), 예산 4(1MiB 중단 목록/2MiB 전체 목록/검색/상세), 금산 2(1MiB 중단 목록/2MiB 전체 목록). 요청당 15초·연결7초·자동 redirect0·TLS 검증 유지. 5요청은 1MiB, 나머지 4요청은 2MiB 응답 상한이다. 예산·금산 최초 중단본은 정상 전체 HTML로 간주하지 않았다.

Java 관측은 홍성 6요청/23MiB, 예산 6요청/26MiB 이내로 각 1회 실행했다. 본문 포함 실제 예약 합계는 9회·8,366,306byte, 조사 포함 요청 예약 상한은 18회다. 예약과 실제 HTTP 호출 수는 동일하지 않다. 관측 임시 원본을 정리했고 조사 HTML 9개도 hash·길이 검증 후 삭제했다. 비식별 metadata 보고서는 재현용으로 유지한다. 일회성 Node·Gradle은 종료하고 기존 사용자 output·__pycache__·다른 프로세스는 보존한다.

## 실행 명령 / 결과 / 미검증

```powershell
.\gradlew.bat :test --tests '*ChungcheongFourthDownloadContractTest' --tests '*ChungcheongThirdDownloadContractTest' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=HONGSEONG,YESAN' -PsanebCollectionWindowsTrust=true --no-daemon
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

첫 실행 1분35초는 기존 승인 지문 불일치 1건으로 실패해 외부 관측은 실행되지 않았다. 선택형 인터페이스 분리 및 Provider QA 한도 전달 검증 추가 후 1분59초 성공했다. 최종 Java XML 325건 중 324통과·1조건부 생략·실패/오류0, 실제 다운로드 관측 2건 및 bootJar 성공이다. Node 23통과, 214영수증/170공고 재현 통과. worker/Provider QA 회귀의 HTTP와 추출기는 대역이며 운영 E2E 증거가 아니다. 공식 HTML fixture와 보관 운영 분모 영수증은 명령 단위 환경변수로 주입 후 복원했다. 원본 정리 후 HTML fixture는 기본 실행에서 조건부 생략된다.

전체 테스트·추출 검증·신규 AWS/운영 조회·운영 배포·정책 게시·ENFORCE·기존 데이터 배치는 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청 정책에 따라 미실행이다. 커밋/푸시는 `[skip deploy]` 범위이며 운영 완료를 의미하지 않는다.
