# 함평 본문/첨부 연결과 접속 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 우선 단계다. 정상 파일을 확보하고 발견·전송·형식·파싱·본문 오류는 별도로 기록한다. 개별 오류나 3표본 확보를 다음 지역 진행 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 함평 공식 제목·본문·첨부 셀과 POST 프로필 연결.
- [x] HWPX 1개 81,510byte 다운로드와 signature/MIME/파일명 검증.
- [x] 영광 차단 안내·신안 목록400·구례 검색403을 별도 후속으로 분리.
- [x] Java 211통과·조건부 1생략, Node 23통과, bootJar·실제 관측 1건 성공.
- [x] 222영수증/178공고 재현, 기존 221영수증/177공고 보존.
- [x] 조사 HTML 7개 659,919byte 길이·SHA256 확인 후 개별 삭제.
- [ ] 미등록 89지역 연결, 등록 후 다운로드 미확인 27지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 운영 설정을 다시 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 | 106 | **107/223 (48.0%)** |
| 다운로드 미확인 | 117 | **116** |
| 프로필 등록 지역 | 133 | **134** |
| 미등록 지역 | 90 | **89** |
| 등록 후 다운로드 미확인 | 27 | **27** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 32 | **32** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 134+기업마당 1=135프로필, 카탈로그 190참조/133대상이다. 신규 기대값은 null, 기존 승인 기대값 1개를 유지한다. 오류 지역에는 성공 파일과 공존하는 지역도 포함되므로 미확인 수에 합산하지 않는다. 이번 미등록 지역 조사 오류는 위 32지역에 가산하지 않는다. 다운로드 비율은 전체 개발·운영 완료율이 아니다.

## 실제 관측

| 대상 | 확인 결과 |
|---|---|
| 함평 LGS-000194 / 32368 | 본문 438자 AVAILABLE / ACCEPTED, HWPX 1개 81,510byte 성공 |
| 영광 LGS-000195 | HTTP200이나 실제 응답은 필터 차단 안내, 목록 수집 성공 아님 |
| 신안 LGS-000199 | 공식 목록 HTTP400, 166byte, 상세·첨부 미실행 |
| 구례 LGS-000185 | 공식 목록200, 공식 검색 POST403, 상세·첨부 미실행 |

함평 표본 제목은 `「2026년 소상공인 카드수수료 지원사업」공고`다. 제목·본문 조합 통과와 다운로드는 확인했지만 첨부 텍스트 추출·분류·정책 승인·운영 DB 저장은 실행하지 않았다. 파일 역할은 UNKNOWN이다.

- 파일 SHA256: `640d8bfc76c4cceeb99f121e1d241979dcf635e6d50b78eccbcdb6e7daa5f6f7`
- 함평 프로필 hash: `eb7e2f28dba16274b62b1d63dc816d251eb65962ace8e50cf0d07c9749a2ea4a`
- 관측 producer class hash: `758bb4866da7dc4e7e7e5720ffdc8401cf49f4d10d5667c8a8df46bcc9dfffa7`

구례 검색403의 원인을 WAF·세션·CSRF 중 하나로 단정하지 않는다. 공식 폼의 정상 요청 조건과 조회 경로를 별도 진단해야 한다. 영광 차단 안내나 신안400을 우회·반복하지 않았고, 다른 지역 수집을 계속했다.

## 구현 계약과 한계

- `HampyeongNoticePage`는 `#board_view > table.basic_table` 아래 제목 라벨 셀, colspan=4인 본문 단독 셀, 첨부파일 라벨 셀을 분리한다. 메뉴·푸터·담당자·첨부 이름은 본문에서 제외하고 본문의 지원 제외 조건은 유지한다.
- 공식 `ffile` form의 HTTPS endpoint·POST·빈 hidden 4필드를 검증한다. 실제 스크립트가 빈 `seq`를 유지한 채 세 파일 값을 설정하는 것을 확인했다. `goDown`의 고정 문자열 3개만 읽으며 JavaScript·사이트 console logging은 실행하지 않는다.
- 파일 관련 opaque 값은 복호화·재구성하지 않고 요청에만 전달한다. locator에는 공고 ID와 파일/경로 결합 해시만 남긴다. 빈 `seq`를 다른 값으로 채우거나 필드를 추가한 요청은 거부한다.
- 같은 기관·공고 URL hash·고정 menu/page·query 중복·HTTPS/port/path·POST 필드 경계를 검증한다. 상세 1MiB·파일 20MiB·최대10파일, 미지원 확장자 미다운로드를 유지한다.
- 일부 첨부 링크가 바뀌거나 처리에 실패해도 정상 descriptor는 보존한다. 미해석 링크·행동·이미지는 오류로 분리한다. 실제 HTML과 합성 PDF/HWP/HWPX/미지원·중복·충돌·한도 초과 회귀 테스트를 함께 사용했다.
- 공식 페이지에는 공공누리 4유형 표시가 있었다. 기술 QA 성공은 서비스 상업적 이용·재배포 권한 확인을 뜻하지 않는다. 공개 전 개별 자료 이용조건은 별도 확인 대상이다.
- 공통 프로필 인터페이스·fingerprint·validator·기존 migration·DB/API/UI·HWP 추출기·운영 정책·worker 설정은 변경하지 않았다.

## 요청량과 정리

조사 총7회: 함평 목록·제목검색·상세 GET3회, 구례 목록 GET·공식 검색 POST2회, 영광·신안 목록 GET 각1회. 요청당 15초·연결7초·2MiB·자동 redirect0·TLS 검증을 유지했다.

실제 Java 관측은 최대6요청/23MiB 범위에서1회 실행했다. 본문 포함 예약4회·2,293,350byte, 조사 포함 예약 상한11회다. 예약 수는 실제 HTTP 호출 수와 같지 않다. 다운로드 원본은 관측 finally에서 정리했고 조사 HTML7개는 길이·hash 대조 후 삭제했다. 비식별 metadata만 보존한다.

## 검증 명령 / 결과 / 미검증

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_JEONNAM_FIFTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*JeonnamFifthDownloadContractTest' --tests '*JeonnamFourthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=HAMPYEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

환경변수는 실행 전 값을 보관하고 finally에서 복원했다. 첫 Gradle 실행은1분43초에 성공했다. Java JUnit212건 중211통과·이전 보성 실제 HTML fixture1건 조건부 생략, 신규 실제 HTML 구조 검사 통과, 지역 관측1건 통과, Node23통과, bootJar 성공이다. 조사 원본 삭제 후 신규 fixture 환경변수는 켜지 않는다.

전체 테스트·전국 상시 worker·첨부 텍스트 추출·운영 DB/API/UI·브라우저 E2E는 이번에 검증하지 않았다. 브라우저는 현재 요청의 명시적 실행 지시가 없어 정책상 생략했다. 커밋·푸시는 `[skip deploy]` 범위이며 운영 배포·정책 활성화·재분류는 실행하지 않는다.
