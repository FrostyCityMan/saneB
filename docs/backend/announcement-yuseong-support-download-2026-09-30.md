# 유성 지원 공고 추가 표본과 제목 정책 회귀 검증

## 현재 단계 / Gate

전 지역 첨부 수집 연결 단계다. 성공 파일을 보존하고 발견·전송·형식·추출 오류는 별도 관리한다. 전체 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E Gate는 미완료다. HWP 추출기 추가 개선은 이번 범위가 아니다.

- [x] 공식 유성 고시공고 목록에서 지원 공고를 검색했다.
- [x] 기존 유성 처리기와 제목 정책을 변경하지 않고 추가 표본을 연결했다.
- [x] 일반 노인 지원 공고를 부모님 대상 공고로 자동 인정하지 않는 정책을 회귀 테스트로 보존했다.
- [x] 계약·카탈로그·형식 회귀 검증과 실제 다운로드 관측 2회 실행.
- [x] 최초 상세 시간 초과와 후속 파일 2개 시간 초과를 각각 보존.
- [x] 관측 근거 이관·최종 집계·검증 결과 기록.
- [!] 유성 파일 다운로드 미확인. 동일 환경 추가 반복 요청 없이 서버 전송 진단 대상으로 분리.

## 조사와 표본 선정

강서 LGS-000017은 공식 구청 채널에서 연결한 고시공고 상세 62174를 확인했으나 로컬 HTTPS GET은 HTTP 400이었다. 파일 요청·재시도 없이 별도 후속으로 남겼다. 다른 게시판이나 검색엔진의 본문을 수집 성공으로 대체하지 않는다.

유성 LGS-000075의 공식 목록은 `https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do`다. 공개 검색 폼의 제목 검색 필드를 확인하고 지원·노인·학생승마 순으로 조사했다.

- 51626: `2026년 노인 일자리 및 사회활동 지원사업 참여자 모집 공고`. 상세의 HWP 1개는 기술 조사에서만 확인했다. 제목 통과를 예상한 초기 테스트 1개가 실패했으며, V65의 일반 `노인` 키워드는 비활성이라는 정책을 확인했다. 정책을 변경하거나 다운로드 성공으로 계수하지 않는다. 이 표본의 실제 파일은 요청하지 않았다.
- 49380: `2025년 학생승마체험 지원사업 추가모집 안내`. 기존 제목 규칙을 통과하는 과거 모집 공고다. 공식 상세에서 PDF 매뉴얼 1개와 HWP 공고 1개를 확인했다. 현재 신청 가능한 공고라는 뜻이 아니다.

추가 표본은 `YUSEONG_SUPPORT` 관측 그룹으로 분리했다. 기존 `YUSEONG-35533` 제목 조합 미충족 표본과 그 중단 기대값을 보존한다. 신규 카탈로그 참조의 `expectation`은 null이며 정책 QA나 실행 기대값을 승인하지 않는다.

## 요청·자원 경계

사전 조사 HTTPS GET은 총 7회(강서 1회, 유성 6회)다. 자동 리다이렉트를 금지하고 각 15초 제한과 TLS 검증을 유지했다. 강서 응답은 스트리밍 2MiB 상한을 설정했다. 유성 조사에는 Invoke-WebRequest를 사용했으며 응답은 메모리에서만 분석했다. 조사 HTML·세션·파일 원본을 새 파일로 저장하지 않았다. 공개 검색엔진 질의 8회는 별도이며 실행 파일 다운로드 증거가 아니다.

실제 관측은 기존 Java 유성 프로필의 회당 7요청·26MiB 예약 상한을 사용했다. 최대 2회로 제한했으며 메인 프로세스의 파일 서명·MIME·파일명 검증은 유지하고 첨부 텍스트 추출은 실행하지 않았다. 실제 예약 합계는 8회·9,641,984byte이며 실제 HTTP 요청 수와는 구분한다. 조사 포함 요청 예약 상한은 21회다.

운영 DB·정책·worker·설정 변경, ENFORCE, 재분류 및 배포는 이번 범위 밖이다. 브라우저 검증은 현재 사용자 정책에 따라 미실행이다. 이전 연제·구례 조사 원본의 정리 미완료와 AWS 인증 만료에 따른 서울 서버 QA 미실행 상태를 이번 로컬 관측으로 해소했다고 보지 않는다.

## 최종 검증 결과

| 항목 | 첫 관측 | 제한된 재시도 |
|---|---|---|
| 관측 시각 KST | 2026-09-30 12:27:58 | 2026-09-30 12:29:26 |
| 제목 / 본문 | COMBINATION_MATCHED / AVAILABLE 495자 | 동일 |
| 상세·첨부 발견 | DETAIL_DISCOVERY / TRANSPORT_TIMEOUT | 상세 식별 확인, FOUND, complete=true, 2개 |
| 파일 | 미요청 | PDF·HWP 각각 FILE_DOWNLOAD / TRANSPORT_TIMEOUT |
| 다운로드 확인 | 0개 | 0개 |
| 보고서 상태 | INCOMPLETE | COLLECTION_ONLY_PARTIAL_NOT_APPROVED |
| 예약 요청 / byte | 3 / 4,194,304 | 5 / 5,447,680 |
| 관측 임시 원본 | 정리 확인 | 정리 확인 |

첫 관측 실패를 `build/reports/attachment-regional-collection/YUSEONG-49380-ATTEMPT1.json`에 보존하고 재시도 보고서는 `YUSEONG-49380.json`에 기록했다. 양쪽 모두 영수증 대장에 이관했으며 최신 관측을 현재 공고 상태로 선택한다. 새 대장은 299영수증/239공고이며 기존 297영수증과 238공고의 내용은 보존했다.

**162/223수집원(72.6%) 확인·61잔여(미연결34+등록 미확인27)를 유지**한다. 오류 관측 수집원은 41→42개이며 다운로드 성공 수집원과 겹칠 수 있다. 프로필은 지역189+기업마당1=190개로 그대로다. 카탈로그는 248공고/189대상, 참조 전용247개와 기존 기대값1개다. 기존 엄격한 3표본·전체 첨부 Gate는 16충족/207잔여로 별도 유지한다. 분모는 2026-09-28 15:56:12 KST 활성 수집원 스냅샷이며 고유 행정구역 수나 운영 완료율이 아니다.

실행 명령:

```powershell
.\gradlew.bat --no-daemon :test --tests '*YuseongSupportDownloadContractTest'
.\gradlew.bat --no-daemon :test --tests '*YuseongSupportDownloadContractTest' --tests '*DaejeonNextDownloadContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YUSEONG_SUPPORT' -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :test --tests '*YuseongSupportDownloadContractTest' :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YUSEONG_SUPPORT' -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git -c core.autocrlf=false diff --check
```

결과:

- 초기 노인 제목 테스트: 1개 실패. 실제 비활성 정책 확인 후 부정 회귀 테스트를 추가했다. 정책을 변경하지 않았다.
- 카탈로그 패치 생성 명령: JSON 배열 키를 잘못 지정한 로컬 오류 1회. 파일 쓰기 없이 종료됐고 실제 `notices` 키로 수정했다.
- 본 검증: Java131개 중130통과·조건부 fixture1생략, bootJar 성공. 이어진 첫 외부 관측 실패로 통합 명령은 exit1이다.
- 마지막 검증: 신규 카탈로그 참조 계약을 포함한 Java3개 통과, 관측 작업 exit0(1분14초). 이는 부분 실패 보고서 생성까지의 성공이며 파일 수집 성공이 아니다.
- Node23개 통과. 299영수증/239공고의 보관 해시·최신 표본·대장 재현 검증 통과.

이번 변경은 수집 검증 표본·회귀 테스트·참조 카탈로그·근거 문서에 한정한다. 운영 수집 처리기, V1 API, DB schema, 기존 migration, 제목 규칙, 파일 검증기를 변경하지 않았다. 남은 작업은 다른 미연결 지역과 별도 전송 오류 처리이며, 유성은 AWS 인증 갱신 후 서울 서버에서 경로 차이를 확인할 후보로 남긴다.
