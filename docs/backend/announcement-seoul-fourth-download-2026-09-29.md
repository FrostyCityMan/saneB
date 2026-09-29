# 성동·송파·광진 첨부 연결과 실제 파일 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계이며 전체 Gate는 미완료다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지, 정상 파일 보존·개별 오류 분리 원칙을 유지한다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 성동·송파·광진 시스템 프로필3개 및 본문 경계 연결.
- [x] 성동356569 본문104자와 HWP2개65,024byte 수집.
- [x] 광진6415244 본문1,930자와 PDF2개·HWPX1개598,852byte 수집.
- [!] 송파33174 공식 상세 제목 칸 공백, TITLE_CONFIRMATION 실패 및 BODY_SELECTOR_CHANGED. 다운로드 미실행.
- [x] Java225통과/조건부1생략, Node23·bootJar 통과. 실제 수집3건 중2건 성공/1건 실패.
- [x] 기존226영수증/182공고를 보존하고 총229영수증/185공고 재현.
- [x] 조사 HTML11개3,354,837byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록82지역 연결, 등록 후 다운로드 미확인30지역 후속.
- [ ] 전국 목록 자동 유입·상시 worker·첨부 텍스트 추출·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번 운영 조회·변경은 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 109 | **111/223 (49.8%)** |
| 다운로드 미확인 | 114 | **112** |
| 프로필 등록 지역 | 138 | **141** |
| 미등록 지역 | 85 | **82** |
| 등록 후 다운로드 미확인 | 29 | **30** |
| 첨부 오류 관측 지역 | 34 | **35** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역141+기업마당1=142프로필. 카탈로그197공고/140대상, 신규 expectation=null, 기존 승인 기대값1개 유지. 실제 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 지역은 성공 지역과 겹칠 수 있어 잔여 수에 합산하지 않는다.

## 실제 관측과 남은 오류

성동 `LGS-000005 / SEONGDONG-356569`: `성동구 아동·청소년 체험학습카드 지원사업 가맹점 모집 공고`. 제목 조합·상세 제목 일치, 본문104자 AVAILABLE/ACCEPTED, 첨부2개 전부 다운로드.

- HWP48,640byte SHA256 `6e9b30094343c0000f1abeaf67810f9dc9c09d79b19c72cc1aac711bfba64ae4`.
- HWP16,384byte SHA256 `0de9f9585d3406230911ec0996e030f2a742997c3d94c7c1f65c3eaf4cc50c95`.
- 프로필 hash `05a27757315d00228114bbb1d14c1bb09ba3b0450eec28149747542860108cb2`.

광진 `LGS-000006 / GWANGJIN-6415244`: `2025년 하반기 소상공인 냉·난방기 클린케어 지원사업 모집 공고`. 본문1,930자 AVAILABLE/ACCEPTED, 첨부3개 전부 다운로드. **2025년 보관 공고의 기술 검증이며 현재 모집 중이라는 뜻이 아니다.**

- PDF326,660byte SHA256 `a69a7dc26aab0a8225ef945be5bd920ea77f815ce239bd1aef4663a579ad965f`.
- HWPX84,509byte SHA256 `6984006080f75b04845507e2e45e7527fe7707a50e0eafb8e3381676a5a71ed8`.
- PDF187,683byte SHA256 `6d6b932b4d46293671a557c660e33e14569506974c314f120f610fc1b069e535`.
- 프로필 hash `4113dbb00715de51de43c097caa044c41ebf2a9915e8069ebc897a9feb27e28a`.

송파 `LGS-000025 / SONGPA-33174`: 목록의 `2026년 송파구 중소기업 융자지원(협력자금) 계획 공고`는 확인했으나 상세 HTML의 제목 셀과 title hidden 값이 비어 있다. HTML 안의 파일 링크3개 존재와 별개로 현재 수집 관측은 상세 식별 검증에서 중단했다. `files=[]`를 첨부 없음으로 해석하지 않는다. 프로필 hash `2b681462d8406be07b00d949f60992a7e486140ad5a5f7d6fb8a9f6a26dc51a6`. 제목이 있는 다른 공식 표본 확보 또는 빈 제목 상세의 대체 식별 근거 검증은 별도 후속이며 다른 지역 진행을 막지 않는다.

이번 파일 역할은 UNKNOWN이고 기대값 승인·정책 QA·첨부 텍스트 분석·운영 쓰기는 없다. 송파의 발견/다운로드까지 성공했다고 표현하지 않는다.

## 구현 계약

`SeoulFourthNoticePage`는 기관별 공식 제목·본문·첨부 셀을 분리한다. 성동은 `ntt_cn_container`만 본문으로 사용하고 같은 셀의 iframe을 요청하지 않는다. 송파는 `gosiFrm`의 표, 광진은 `view > t`의 라벨 기반 dl을 읽는다. 메뉴·연락처 metadata·첨부명을 본문에 섞지 않는다.

`SeoulFourthAttachmentDiscoveryProfile`은 시스템 수집원·목록 parser·원문 hash·공식 host/path/query를 검증한다. 성동은 파일 경로에 서버가 삽입한 ASCII 공백만 정규화하고 공고번호·게시판·메뉴를 고정한다. 송파 `gourl`의 단일 URL 리터럴은 실행 없이 읽고, 공식 함수에서 확인한 HTTPS 전환 및 기존 새올 파일 검증기를 사용한다. 광진은 고정 파일 ID·순번·메뉴를 확인한다. 광진의 화면 파일명 줄임 표기 대신 전체 title과 확장자를 읽고 최종 전송에서는 기존 파일 검증을 적용한다.

미리보기·바로듣기는 다운로드와 같은 파일인지 대조만 하며 요청하지 않는다. 다른 링크의 오류가 있어도 유효한 descriptor는 유지한다. 10파일 상한, 미지원 확장자, 잘못된 host/HTTP/추가·중복 query, 중복·충돌, 부분 오류, 본문/제목 구조 변경을 테스트했다. 기존 공통 프로필 지문·migration·DB/API·UI·HWP 추출기·운영 설정은 변경하지 않았다.

## 실행·검증·정리

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SEOUL_FOURTH_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SeoulFourthDownloadContractTest' --tests '*SeodaemunDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEONGDONG,SONGPA,GWANGJIN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

첫 실행1분37초에 송파 공식 HTML 제목 검증1건이 실패해 실제 수집 태스크에는 도달하지 않았다. 빈 제목을 허용하도록 코드를 완화하지 않고 해당 HTML에서 실패 반환을 확인하는 회귀 테스트로 명시했다. 두 번째 실행2분: Java226건 중225통과·과거 서대문 HTML fixture1건 조건부 생략, bootJar는 첫 생성 성공/두 번째 UP-TO-DATE다. 실제 수집은 성동·광진2건 성공/송파1건 실패로 **Gradle 전체 exit1**이다. 전체 명령 성공으로 표현하지 않는다. Node23검사 및 증거 재현은 통과했다. 환경변수는 실행 전 값을 보관하고 finally에서 복원했다.

조사11요청: 성동4·송파4·광진3. 각15초/2MiB·TLS 검증·자동 redirect0 유지. Java 관측은 지역당6요청/23MiB 상한, 본문 포함 총예약14회·7,881,028byte다. 조사 포함예약 상한25회이며 예약 수와 실제 HTTP 호출 수는 다르다. 관측 파일 finally 정리와 조사11파일 길이·SHA256 대조 후 개별 삭제를 확인했다. 원본 삭제 후 실제 HTML fixture 환경변수는 켜지 않는다.

전체 테스트·전국 자동 유입·첨부 텍스트 추출·Linux worker/DB/API·운영 UI·배포·정책 활성화·재분류는 이번 검증 범위가 아니다. 브라우저는 현재 명시적 지시가 없어 사용자 정책상 미실행했다. 검증한 변경은 `[skip deploy]`로 커밋·푸시하며 다음 미등록 지역 연결을 계속한다.
