# 청송·영양·울릉 첨부 연결과 실제 파일 수집

## 현재 단계 / Gate

전 지역 첨부 연결을 우선한다. 정상 파일은 수집하고 발견·다운로드·파싱 실패는 별도 기록하며 다음 지역으로 진행한다. 모든 오류 해결이나 3표본 확보를 다음 지역 착수 조건으로 두지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 청송 GET·영양 공개 POST 폼·울릉 공식 파일 서버 이동 확인.
- [x] 공유 처리기1개·시스템 프로필3개·미승인 참조3개 추가.
- [x] 청송2개·영양1개·울릉1개 실제 HWP 다운로드와 형식 검증.
- [x] 청송 미리보기 발견 오류와 정상2파일 수집을 분리.
- [x] 계약155건·Node23건·bootJar·실제 관측3공고 실행.
- [x] 155영수증/최신119공고 재현, 이전 근거 보존, 임시 원본 정리.
- [ ] 고령을 포함한 미등록144지역 연결, 등록 미관측14지역 후속.
- [ ] 본문 정제·첨부 추출·상시 worker·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이며 이번 작업에서 현재 운영 설정을 다시 조회하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 62 | **65/223 (약29.1%)** |
| 다운로드 미관측 지역 | 161 | **158** |
| 로컬 프로필 등록 지역 | 76 | **79** |
| 미등록 지역 | 147 | **144** |
| 등록 후 다운로드 미관측 | 14 | **14** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 16 | **17** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약29.1%는 다운로드 관측률이지 전체 개발·운영 완료율이 아니다. 지역79+기업마당1=80프로필, 카탈로그134참조/지역76개다. 신규 expectation은 null이며 기존 승인 기대값1개를 유지한다. 기존 처리기의 실행 코드와 이전116공고의 profile hash를 보존했다.

## 실제 수집 결과

| 지역 / 공고 | 본문 확보 길이 | 발견 | 실제 다운로드 |
|---|---:|---|---|
| 청송 LGS-000212 / 22287 | 8,542자 | 2개·부분 오류 | HWP15,872 + 15,872byte |
| 영양 LGS-000213 / 26774 | 5,943자 | 1개·완료 | HWP94,720byte |
| 울릉 LGS-000222 / 23003 | 2,150자 | 1개·완료 | HWP327,680byte |

총4개 **454,144byte**다. 세 공고는 DRAFT seed 제목 조합과 상세 제목 식별을 통과했다. 청송은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 영양·울릉은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 고정 과거 표본의 다운로드 성공은 현재 모집 중임을 의미하지 않는다.

청송은 직접 파일 링크 옆의 미리보기 anchor/script를 실행하거나 해석하지 않는다. `ATTACHMENT_LINK_UNRESOLVED`, 발견 `FAILED/complete=false`를 기록하면서 검증된 직접 링크2개는 보존한다. 영양·울릉은 이번 표본의 첨부 발견과 다운로드를 완료했지만 엄격3표본 Gate 또는 운영 상시 수집 완료를 의미하지 않는다.

세 공고의 본문 중간 판정은 `REVIEW_REQUIRED/BODY_GROUP_A_MATCHED`다. **본문 길이는 수집 문자열의 길이일 뿐 정제 품질·전체 본문 확보를 증명하지 않는다.** 특히 영양의 실제 `view_box` 본문은 짧은 문장인데 확보 길이는5,943자여서 주변 텍스트 혼입 점검이 필요하다. 청송도8,542자 문자열의 본문 경계 점검을 후속으로 남긴다. 이 판정을 최종 지원 후보 승인으로 표현하지 않는다. 첨부 텍스트 추출은 이번 실행 범위가 아니다.

## 구현 계약

- 청송: `form#saeolGosiVO`의 제목과 `dl.attach > dd`만 읽는다. HTTPS 공식 `eminwon.cs.go.kr`의 고정 FileDown.jsp, 공개 파일3query를 사용한다.
- 영양: `div.view_title > p.title`, 공식 `div.view_box.file_area`의 파일 항목·다운로드 button을 사용한다. `goDownLoad` 세 문자열만 정적으로 해석하고 임의 JavaScript는 실행하지 않는다. 표시 파일명과 인수를 공백 정규화 후 대조한다.
- 영양 form은 `form[name=nnn]`, 공식 FileDownNew.jsp POST, 빈 파일3필드와 일회성 폼 필드1개로 제한한다. 일회성 값은 요청 메모리에서만 사용하며 locator·로그·근거 대장에 저장하지 않는다. 원본 HTML은 정리한다.
- 울릉: `div.boardView > dl.title`의 제목·공식 첨부 목록, 고정 `mnu_uid=571`, 공고번호·file_seq 결합을 검증한다. 다운로드는 공식 `/programs/board/saeol/notice/download.do`에서 확인한 `eminwon.ulleung.go.kr/emwp/jsp/ofr/FileDown.jsp`로의 이동만 허용한다. paired fileView.do는 같은 공고·파일 query를 대조한 뒤 호출하지 않는다.
- source/parser/URL hash 결합, HTTPS443, 공식 호스트·고정 경로, 중복 충돌·10파일 상한, 형식별 분리, 문서 역할 UNKNOWN을 유지한다.
- migration·DB/API·화면·운영 정책·worker 설정·HWP 추출기는 변경하지 않았다.

## 요청 예산 / 정리

이번 조사4GET(각 상세1회와 울릉 다운로드 전환1회), 관측 예약14회로 합계18회다. 본문 예약 상한이 포함되므로 실제 HTTP 횟수와 동일하다고 표현하지 않는다. 청송 조사1+관측5, 영양 조사1+관측4, 울릉 조사2+관측5다. 이전5차 조사의 요청량은 별도 이력에 보존한다.

조사당15초/연결7초/최대1MiB/자동 redirect0/TLS 검증 유지. 관측당 최대6요청·23MiB, 예약 byte 합계7,171,584. 임시 조사 원본5개 총417,281byte와 관측 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하며 사용자 프로세스와 미추적 `output/`, `scripts/qa/__pycache__/`를 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukSixthDownloadContractTest' --tests '*GyeongbukFifthDownloadContractTest' --tests '*GyeongbukThirdDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=CHEONGSONG,YEONGYANG,ULLEUNG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

계약155건 실패·오류·skip0, bootJar·실제 관측3공고 실행 완료(1분38초). Node23/23. 155영수증/119공고 재현 통과. 이전152영수증·실행 metadata·기존 profile hash를 보존했다. 청송 발견 오류는 정상 전체 발견으로 숨기지 않는다.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 미실행이다. 브라우저는 현재 지역 연결 작업에 명시적 요청이 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
