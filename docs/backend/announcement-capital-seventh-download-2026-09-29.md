# 포천·강릉 본문/첨부 연결 및 접근 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하고 발견·다운로드·형식·파싱·본문 오류를 분리한다. 한 지역의 접근 실패를 다음 지역 진행의 차단 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 포천·강릉 공식 HTML 조사 및 본문/첨부 프로필 2개 연결.
- [x] 저장한 공식 HTML의 제목·본문 경계와 첨부 1개씩 발견 검증.
- [!] 실제 수집 경로: 포천 상세 전송 실패, 강릉 상세 시간 초과. 다운로드 성공 0개.
- [x] 이천 등록 목록 HTTP400을 조사 오류로 기록. 임의 대체 주소·운영 활성화 없음.
- [x] Java 208통과·조건부 1생략, Node 23통과, bootJar 통과.
- [x] 206영수증/164공고 재현 및 기존 204영수증/162공고 보존, 조사 원본 8개 정리.
- [ ] 미등록 103지역 연결, 등록 후 다운로드 미확인 23지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 상태를 새로 조회하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 지역 | 97 | **97/223 (43.5%)** |
| 다운로드 미확인 지역 | 126 | **126** |
| 프로필 등록 지역 | 118 | **120** |
| 미등록 지역 | 105 | **103** |
| 등록 후 다운로드 미확인 | 21 | **23** |
| 발견·파일 오류 관측 지역 | 27 | **29** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 120+기업마당 1=121프로필, 카탈로그 176참조/119대상이다. 신규 기대값은 null이며 기존 승인 기대값 1개는 보존했다. 오류 지역 수는 수집 영수증 집계이며, 이천처럼 목록 조사에서 중단된 미등록 지역을 이 수치에 더하지 않는다.

## 실제 결과와 한계

| 지역 / 공고 | 저장 HTML 검증 | 실제 수집 경로 |
|---|---|---|
| 포천 LGS-000108 / 64129 | 제목·본문·HWP 링크 1개 검증 | 본문 NETWORK_ERROR, 상세 TRANSPORT_FAILED, 파일 요청 단계 미도달 |
| 강릉 LGS-000119 / 60798 | 제목·본문·HWPX 링크 1개 검증 | 본문 NETWORK_ERROR, 상세 TRANSPORT_TIMEOUT, 파일 요청 단계 미도달 |
| 이천 LGS-000105 | 등록 목록 HTTP400 | 프로필 미구현 유지 |

공식 HTML 조사 성공은 Java 수집 경로의 전송 성공을 보증하지 않는다. 이번 보고서의 `files=[]`는 첨부 없음이 아니라 상세 발견 단계 미도달이다. 두 지역의 프로필 등록을 다운로드 성공으로 계산하지 않는다. 원인 코드는 관측 사실이며 TLS·서버 차단·일시 장애 중 어느 원인인지 단정하지 않는다. 무제한 재시도나 안전 검사 완화 없이 접근 오류 후속으로 남긴다.

포천의 기존 bare host 목록은 www 호스트로 301 이동했다. 관측한 HTTPS 공식 상세를 사용했으며 운영 목록 seed·parser를 변경하지 않았다. 향후 기존 공고 URL과 새 유입의 identity/redirect 연결은 별도 검증 대상이다. 강릉도 고정 공식 공고 표본의 파싱 계약이며 전국 신규 목록 유입을 증명하지 않는다.

## 구현 계약

- `CapitalSeventhNoticePage`: HTTPS443·정확한 host/path·key·공고 ID·공고 구분과 알려진 검색 인자만 허용한다. 포천은 `bbs_viewbox`의 제목/본문/첨부 블록, 강릉은 `bbs_default.view`의 제목/내용/파일 셀을 분리한다. 경계가 바뀌면 페이지 전체를 본문으로 사용하지 않는다.
- 포천: 공식 첨부 영역의 `attach_item > a.attach_btn.down`에서 `eminwon.pcs21.net/emwp/jsp/ofr/FileDown.jsp`의 고정 3인자를 읽는다.
- 강릉: `view_attach > li > down_view > a.file_down`의 `eminwon.gangneung.go.kr/emwp/jsp/ofr/FileDownNew.jsp` HTTPS GET만 연결한다. `fn_goPreView`의 2개 리터럴은 같은 파일인지 정적으로 비교하며 스크립트 실행·미리보기 요청은 하지 않는다. 미리보기의 구형 HTTP 문자열은 비교에만 사용하며 HTTP 다운로드를 허용하지 않는다.
- 정상 descriptor는 보존하고 미확인 링크·미리보기 충돌·미지원 확장자를 별도 기록한다. 10파일 상한·UNKNOWN 역할·고정 출처·동일 요청 redirect·signature/MIME 검사를 유지한다.
- 기존 공유 프로필·Flyway·DB/API·UI·추출기·운영 정책·worker 설정은 변경하지 않았다. 운영 게시·ENFORCE·배치·배포 없음.

Profile hash: 포천 `9ca7ff2c5220b6bcc75813ad30c6485369d8db18f177a94a1564d9ec83c2ecf3`, 강릉 `713f360e094825cf01a9b2bfc877978cce709c585c18a997aa974ebbf67bcb7d`.

관측 producer class: `3d041b3af9a19f1a7ab68e5544e5dac62a741ff0b2ea4ec1e13c64ca3b15e8bc`.

## 요청량 / 검증

공식 조사 GET 9회: 포천 5(redirect 목적지 확인 1 포함)·강릉 3·이천 1. 요청당 15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. HTML 8개 1,397,340byte의 길이·SHA256을 대장에 기록한 뒤 전체 대조 후 개별 삭제했다. redirect 진단 응답 원본은 저장하지 않았다. 사용한 일회성 Node·Gradle 프로세스는 종료됐으며 기존 사용자 Java 프로세스·output·__pycache__는 보존했다.

관측 상한은 공고별 6요청·23MiB다. 실제 예약은 본문 포함 합계 6회·4,194,304byte, 조사 포함 예약 상한은 15회다. 예약 수와 실제 HTTP 호출 수를 동일시하지 않는다. 실패 관측 보고서의 임시 원본 정리는 확인했다.

첫 실행: 대상 Java 테스트 208건 중 207통과·1조건부 생략, bootJar 생성 성공. 외부 수집 관측 2건은 상세 전송 실패로 실패하여 **전체 명령은 1분38초 실패**다. 이후 본문 연결 회귀 테스트를 추가하면서 AutoCloseable 미구현 클래스에 try-with-resources를 사용한 테스트 컴파일 오류가 발생했다(16초). 기존 stub 사용 방식으로 수정했으며 외부 관측은 반복하지 않았다.

최종 로컬 재검증은 1분7초 성공: Java XML 209건 중 208통과·1조건부 생략·실패/오류0, bootJar UP-TO-DATE. Node 23통과, 206영수증/164공고 재현 통과다. 공식 HTML fixture는 이 실행에서 통과했으며 원본 정리 후 기본 테스트에서는 조건부 생략된다. 외부 관측의 실패 결과를 이 로컬 성공으로 대체하지 않는다.

재검증 명령:

```powershell
.\gradlew.bat :test --tests '*CapitalSeventhDownloadContractTest' --tests '*CapitalSixthDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar --no-daemon
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

관측은 첫 명령에 `:attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=POCHEON,GANGNEUNG' -PsanebCollectionWindowsTrust=true`를 추가해 실행했다. 저장 HTML fixture와 기존 비식별 운영 분모 영수증의 환경변수는 해당 명령에서만 주입하고 복원했다. 전체 테스트·새 운영 조회·AWS·운영 배포·텍스트 추출은 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청 정책에 따라 미실행이다.
