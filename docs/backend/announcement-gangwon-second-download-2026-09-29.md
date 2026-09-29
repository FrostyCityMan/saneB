# 양구·인제 본문/첨부 연결과 양구 파일 수집

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 정상 파일은 수집하며 발견·다운로드·형식·파싱·본문 오류를 분리한다. 모든 오류 해결이나 3표본 확보를 다음 지역 진행의 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 양구·인제 공식 본문/첨부 경계와 프로필 2개 연결.
- [x] 양구 HWPX 1개 72,208byte 다운로드 및 signature/MIME/파일명 검증.
- [x] 초기 MIME 오류·파일명 인코딩 오류와 수정 후 성공 근거 모두 보존.
- [!] 인제 MIME 불일치, 홍천 제목 검색 연결 시간 초과는 별도 후속.
- [x] Java 209통과·조건부 1생략, Node 23통과, bootJar·수집 전용 관측 태스크 통과.
- [x] 210영수증/166공고 재현 및 기존 206영수증/164공고 보존.
- [x] 조사 HTML 7개 776,211byte를 길이·SHA256 대조 후 개별 정리.
- [ ] 미등록 101지역 연결, 등록 후 다운로드 미확인 24지역 후속.
- [ ] 전국 목록 유입·상시 worker·추출·DB/API/UI·정책 승인·운영 E2E Gate.

대상 분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 새로 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 최소 1파일 다운로드 확인 지역 | 97 | **98/223 (43.9%)** |
| 다운로드 미확인 지역 | 126 | **125** |
| 프로필 등록 지역 | 120 | **122** |
| 미등록 지역 | 103 | **101** |
| 등록 후 다운로드 미확인 | 23 | **24** |
| 현재 프로필 hash와 일치하는 오류 관측 지역 | 29 | **29** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 122+기업마당 1=123프로필, 카탈로그 178참조/121대상이다. 신규 기대값은 null, 기존 승인 기대값 1개는 유지한다. 위 비율은 전체 개발·운영 완료율이 아니다.

## 실제 결과

| 지역 / 공고 | 본문 | 파일 결과 |
|---|---|---|
| 양구 LGS-000131 / IHINR260828133751437 | 117자 확보 | HWPX 1개 72,208byte 성공 |
| 인제 LGS-000132 / 245926, 초기 관측 | 447자 확보 | 1개 발견, 81,920byte 응답 후 MIME 불일치 |
| 홍천 LGS-000124 | 목록 HTTP200 | 제목 검색 연결 시간 초과, 프로필 미등록 유지 |

양구의 초기 응답 MIME은 `application/octer-stream;charset=UTF-8`이다. 공통 validator의 기존 기관별 opt-in을 적용한 후 파일명 헤더의 UTF-8/Latin-1 해석 차이를 확인했다. 기존 엄격한 UTF-8 복원 기능을 양구에만 활성화한 최종 관측에서 signature·예상 HWPX·attachment disposition·파일명 확장자가 모두 일치했다. 세 번의 파일 바이트 hash는 동일하다. 구형 MIME·헤더 처리를 전역에 허용하지 않았다.

인제는 `application/x-tika-msoffice;charset=UTF-8`이며 기존 validator의 허용 MIME이 아니다. 바이트와 hash만으로 성공 집계하지 않는다. 양구 보완으로 신규 공유 프로필 클래스의 hash가 바뀌었으므로 인제의 초기 외부 실패 근거는 `EVIDENCE_VERSION_MISMATCH`로 보존한다. 인제의 최신 코드는 계약/저장 HTML 테스트를 통과했지만 외부 재관측은 하지 않았다. 따라서 현재 hash 기준 오류 지역 29개에 인제를 포함하지 않으며, 이 숫자를 전체 알려진 오류 지역 수로 해석하지 않는다. 인제 성공·완료를 주장하지 않는다.

양구의 파일 byte hash는 `5175cbad998fbd39942fa60b38149c331a9f7b737b7b85694ac824cff3053a71`이다. 본문/파일 전송 검증이며 ZIP 내부 HWPX 구조·텍스트 추출·분류 근거·운영 저장·관리자 최종 검증을 대신하지 않는다.

## 구현 계약

- `GangwonSecondNoticePage`: HTTPS443·정확한 host/path·공고 식별자와 알려진 검색 인자만 허용한다. 양구는 `user_board_whole > registform > fieldset`의 제목/본문/파일 테이블, 인제는 `skinTb-data-bgSbj`의 제목/본문/첨부 셀을 분리한다. 메뉴·푸터·파일명·작성자 metadata가 본문에 섞이지 않는다.
- 양구: 공식 파일 셀의 `opendownload('announcement', 게시물번호, 파일순번)` 고정 리터럴을 `/fnc_bbs/user_bbs_download` GET으로 결합한다. 파일명 링크와 다운로드 버튼의 일치 여부, 미리보기의 저장 파일명/게시판/게시물번호를 확인한다. 일반 미리보기·별도 뷰어 스크립트는 실행하거나 요청하지 않는다. 폼 전체를 제출하지 않으며 인증·서명 값은 보내지 않는다.
- 인제: 공식 첨부 셀의 `/egf/bp/board/article/download?fileSeq=...`만 GET으로 연결한다. 다른 링크나 폼을 첨부로 간주하지 않는다.
- 정상 descriptor 보존, 미확인 링크·미리보기 충돌·미지원 확장자 별도 기록, 10파일 상한, UNKNOWN 역할, 고정 출처·동일 요청 redirect 및 signature/MIME 검사 유지.
- 기존 프로필·공통 validator·Flyway·DB/API·UI·추출기·운영 정책·worker 설정은 변경하지 않았다. 운영 게시·ENFORCE·배치·배포 없음.

현재 profile hash: 양구 `ad236849cd3a20da25b4dc4c32e66a94927478258b1fb0e205a392dccd0e3a37`, 인제 `14563ede7348fc868570105841a5d5bce5d93134e9aeba6b1bb0c0ca15a2ba40`.

관측 producer class: `7bc4331056406c20341458311fdcad9d4bfa7cec2f79e9519ea79cd10d55697b`.

## 요청량 / 정리

조사 GET 11회: 홍천 2(목록/검색 실패)·양구 5(목록/검색/상세/MIME 진단/비식별 헤더 진단)·인제 4(목록/검색/상세/MIME 진단). 요청당 15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 진단 파일 응답은 저장하지 않았고 원문 헤더를 근거 대장에 남기지 않았다. 조사 HTML 7개 776,211byte는 길이·SHA256 대조 후 개별 삭제했다.

관측 4건(양구 3회, 인제 1회)의 공고별 상한은 각 6요청·23MiB다. 실제 예약은 본문 포함 16회·9,191,424byte, 조사 포함 예약 상한은 27회다. 예약 수와 실제 HTTP 호출 수를 동일시하지 않는다. 초기 보고서는 hash 대조 복사로 보존했고 최신 결과가 같은 공고의 이전 실패를 집계상 대체한다. 실패 이력을 삭제하거나 공고 수를 중복 합산하지 않았다. 각 관측의 임시 원본 정리를 확인했다.

일회성 Node·Gradle 프로세스는 종료되며 사용자 output·__pycache__·기존 Java 프로세스는 보존한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GangwonSecondDownloadContractTest' --tests '*CapitalSeventhDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YANGGU,INJE' -PsanebCollectionWindowsTrust=true --no-daemon
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
git diff --check
```

첫 실행 1분39초, 양구 MIME opt-in 후 양구만 재관측 1분32초, UTF-8 복원 추가 후 양구만 최종 재관측 1분33초 모두 태스크는 성공이다. 첫 두 실행의 파일 결과는 부분 실패이며 태스크 성공을 첨부 전체 성공으로 해석하지 않는다. 최종 Java XML 210건 중 209통과·1조건부 생략·실패/오류0, bootJar 생성 성공, 양구 수집 성공이다. Node 23통과와 210영수증/166공고 재현 통과.

저장 HTML fixture 및 기존 비식별 운영 분모 영수증의 환경변수는 해당 명령에서만 주입하고 복원했다. 공식 HTML fixture는 통과했으며 원본 정리 후 기본 테스트에서는 조건부 생략된다. 전체 테스트·신규 운영 조회·AWS·운영 배포·텍스트 추출은 실행하지 않았다. 브라우저 QA는 현재 지역별 수집 요청 정책에 따라 미실행이다.
