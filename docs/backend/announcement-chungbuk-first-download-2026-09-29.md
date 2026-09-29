# 증평·단양 첫 첨부 수집과 충북 접근 오류 분리

## 현재 단계 / Gate

2026-09-29 사용자 지시에 따라 전 지역 연결과 첫 파일 수집을 우선한다. 지역별 세 표본 확보·본문 오류·HWP 파싱 문제 때문에 다음 지역을 중단하지 않는다. HWP 추출기 1.0.16 개선은 계속 보류한다.

- [x] 증평·단양 시스템 프로필 추가 및 실제 첨부 다운로드.
- [x] 괴산 접근 차단·진천 적격 표본 미확보를 성공과 분리.
- [x] 회귀 테스트·bootJar·근거 대장 재현.
- [ ] 나머지 지역 연결, 본문/추출 보완, 운영 상시 수집과 전체 E2E Gate.

활성 **223지역**은 2026-09-28 15:56:12 KST 운영 대상 읽기 전용 스냅샷 기준이다. 이번에는 운영 상태를 다시 조회하거나 수정하지 않았다.

| 지표 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 다운로드 관측 지역 | 27 | **29** |
| 다운로드 미관측 지역 | 196 | **194** |
| 로컬 시스템 프로필 연결 지역 | 35 | **37** |
| 미등록 지역 | 188 | **186** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

미관측194는 미등록186 + 등록됐지만 다운로드 미관측8이다. 29/223(약13.0%)는 첫 다운로드 관측률이지 전체 장기 목표 진행률이나 운영 적용률이 아니다.

## 구현 및 실측

공통 `LegacyFormSaeolAttachmentDiscoveryProfile`은 실측한 form 이름·제목 표 구조를 검사한 뒤 기존 새올 GET 파서에 **전체 첨부 영역**을 전달한다. 증평은 `form1`/100%, 단양은 `form`/98%이다. 소스 코드·목록 parser·공식 호스트·고정 쿼리·동일 URI redirect 검사와 TLS 검증을 유지했다. 미해석 링크가 있어도 확인한 파일 descriptor는 보존한다. 기존 프로필 구현과 hash는 변경하지 않았다.

| 지역 / 소스 | 고정 공고 | 결과 | 남은 문제 |
|---|---|---|---|
| 증평 / LGS-000142 | 31159, 소상공인 지원자금 이차보전금 | HWPX 1개, 81,836byte 다운로드·signature 확인 | 본문 `DETAIL_HOST_NOT_ALLOWED`, 추출 미실행 |
| 단양 / LGS-000146 | 32263, 소상공인 이차보전금 지원 | HWP 1개, 118,272byte 다운로드·signature 확인 | 본문 `DETAIL_HOST_NOT_ALLOWED`, 추출 미실행 |
| 진천 / LGS-000143 | 공식 고시 목록의 소상공인 검색 | 해당 검색에서 적격 공고 미확보 | 첨부 없음으로 판정하지 않음; 별도 공식 공고 경로 조사 필요 |
| 괴산 / LGS-000144 | 공식 새올 목록 iframe | HTTP201 응답에 웹 방화벽 차단 문구 | 자동 재요청·우회 없음, 미등록 유지 |

증평·단양 본문 오류는 공식 홈페이지 메뉴 호스트와 새올 상세 호스트가 다르고, 본문 클라이언트가 등록 호스트와 상세 호스트 일치를 요구하기 때문이다. 본문 요청 시도는 각각0이다. 첨부는 별도 시스템 프로필의 공식 상세/다운로드 허용 경계에서 성공했다. 이번에 본문 호스트 검사를 완화하거나 운영 source URL을 수정하지 않았다. 후속 본문 작업에서는 공식 iframe 출처 연결 계약을 검증해야 한다.

파일2개 총 **200,108byte**, 제목 DRAFT seed 조합 통과·공식 상세 제목 일치·형식 signature·원본 정리를 확인했다. 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 정책 승인·텍스트 분석·운영 worker DB/API 성공이 아니다. QA catalog 2참조의 `expectation`은 null로 유지했다. 전체 등록38프로필(지역37+기업마당1), catalog92참조/지역34개, 정상 승인0이다.

## 요청량과 정리

- 공식 메뉴4 + iframe4 + 검색3 + 상세2 = 조사 **13요청**, 저장 HTML **592,709byte**.
- 수집 관측은 본문 상한 예약 포함 **8요청 예약**, 실제 상세2 + 파일2 = **4회 전송**. 본문은 네트워크 요청 전 중단.
- 이번 실제 전송 합계17회, 상한 예약 합계21회. 지역별 누적 예약: 증평8/30, 단양8/30, 진천3/30, 괴산2/30.
- 수집 관측 예약 용량4,406,504byte. 각 공고 상한6요청/23MiB, 파일 최대20MiB. 조사는 요청당1MiB/15초/redirect0 이내.
- 임시 HTML13개는 소유 경로를 확인해 삭제했다. 실파일/상세 임시 파일은 관측 harness가 finally에서 삭제했고 `originalFilesRemoved=true`를 남겼다. 원문 복구본은 보존하지 않는다. 비식별 해시·용량·결과 보고서는 유지한다.
- 운영 DB·정책·worker 설정·설치 변경0, 원격 QA/브라우저 실행0.

## 검증과 근거

```powershell
.\gradlew.bat :test --tests '*ChungbukFirstDownloadContractTest' --tests '*AttachmentCollectionOnlySummaryTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEUNGPYEONG,DANYANG' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*ChungbukFirstDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

첫 검사 성공38초, 실파일 관측 **2/2 통과**26초. 마지막 계약 검사 **88/88 통과(실패·생략0)** 및 bootJar 성공1분8초. 마지막 검사에는 `SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT=true`와 보관된 읽기 전용 inventory receipt 경로를 프로세스 환경으로만 주입해 현재 로컬 등록을 다시 계산했고 원래 환경을 복원했다. Node **23/23** 통과. 전체 프로젝트 테스트·Linux worker 통합 검사는 이번에 재실행하지 않았다. 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책상 생략했다.

보관 **100영수증 / 최신77공고**를 재이관해 대장과 일치했다. 재현 과정은 외부 요청0이며 기존 이력 metadata도 보존했다. 실측 보고서는 `build/reports/attachment-regional-collection/JEUNGPYEONG-31159.json`, `DANYANG-32263.json`, 생산자 class hash는 `cee71e9b15d6bd8545f212229a0246afe4657e9710bed5d37b0738a4ec74b740`이다. 영수증·파일·JUnit hash와 누적 예산은 [근거 index](attachment-collection-receipt-index-2026-09-28.json)의 `chungbukFirstDownloadRun`에 기록한다.

다음은 미등록186지역 중 공식 목록/상세를 접근할 수 있는 공통 구조 지역의 첫 파일 연결이다. 괴산 차단·진천 표본 미확보·증평/단양 본문 오류는 후속 오류 항목으로 남기며 다른 지역 진행의 선행 조건으로 삼지 않는다. migration·기존 API·자동 활성화 정책을 변경하지 않는다.
