# 인천시·울산시 시티넷 파일 응답 복구

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 우선하고 개별 오류를 분리한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 개선은 이번 범위가 아니다.

- [x] 인천시 LGS-000054·울산시 LGS-000077의 application/file 및 EUC-KR 파일명 헤더 실측·복구.
- [x] 실제 Java 수집 경로에서 본문58자/119자, HWPX 각1개 총144,908byte 다운로드 검증.
- [x] 관련 Java369통과/4조건부생략·bootJar·Node23·280영수증/224최신공고 재현.
- [x] 이전 실패·다른 지역 근거 보존, 조사 원본8개316,403byte 및 이번 프로세스 정리.
- [ ] 잔여73수집원: 미등록43 + 등록 후 다운로드 성공 미확인30.
- [ ] Linux CI 종료 결과, 추출·상시 worker·운영 DB/API/UI·DRAFT·배치·운영 E2E 전체 Gate.

## 원인과 구현

기관별 공식 상세1회·공식 첨부1회, 총4회 GET 진단은 모두200이었다. 두 서버 모두 `Content-Type: application/file`을 반환했다. 파일은 ZIP prefix를 가진 HWPX이며 Content-Disposition 원래 바이트는 엄격한 UTF-8 해석에 실패했다. EUC-KR 해석은 성공했고 그 파일명이 공식 첨부 링크의 표시명과 정확히 일치했다. 이는 문자열 추측이 아니라 두 실파일 fixture에서 다시 검증한 결과다. 실제 파일명·전체 헤더·본문은 문서에 복사하지 않는다.

`CitynetAttachmentFileResponse`를 두 시티넷 프로필의 기존 bounded download flow에 연결했다.

1. 기존 공식 HTTPS 호스트·고정 다운로드 경로·query4개·동일 요청 제한과 전송 예산을 유지한다. 추가 요청은 하지 않는다.
2. 정확히 `application/file`인 응답만 처리한다. 서버의 raw `application/x-msdownload`나 다른 미허용 MIME까지 추가 허용하지 않는다.
3. Latin-1 문자열로 전달된 EUC-KR 헤더 바이트를 엄격하게 한 번 복원한다. 잘못된 바이트에서 다른 문자셋으로 재시도하지 않는다. ASCII/RFC5987 및 이미 Unicode인 헤더는 재인코딩하지 않는다.
4. C0·DEL·복원 후 제어문자, 경로 포함 파일명, 누락·inline disposition, 확장자 불일치를 거부한다. PDF/OLE/ZIP 기본 서명과 attachment 파일명을 검증한 뒤 내부 legacy-binary 표현으로 정규화한다.
5. 호출자가 descriptor의 예상 형식과 다시 대조한다. 파일 byte·SHA256은 변경하지 않는다.

내부 MIME `application/x-msdownload`는 기존 강화된 legacy 검증을 사용하는 표현이며 서버 원래 MIME이 아니다. 서버 실측 MIME과 문자셋은 대장에 별도 기록했다. OLE·ZIP 기본 서명 확인은 HWP/HWPX 내부 구조·텍스트 추출 성공이 아니다. 공통 validator·gateway·전송·fingerprint 구현·기존 MIME adapter는 변경하지 않았다. 두 프로필의 지문은 응답 처리 코드까지 포함하도록 갱신했으며 다른 지역 표본·집계의 동일성을 이관 시 검증했다.

## 실제 관측

| 항목 | 인천시 | 울산시 |
|---|---|---|
| 표본 | INCHEON_CITY-66970 | ULSAN_CITY-47059 |
| 관측 UTC | 2026-09-29T23:30:42.056393Z | 2026-09-29T23:30:54.486610700Z |
| 관측 KST | 2026-09-30 08:30 | 2026-09-30 08:30 |
| 본문 | AVAILABLE58자·ACCEPTED | AVAILABLE119자·ACCEPTED |
| 첨부 | FOUND·complete=true·HWPX1개74,696byte | FOUND·complete=true·HWPX1개70,212byte |

두 제목은 COMBINATION_MATCHED, 본문 근거는 TARGET_SUPPORT_CONFIRMED였다. 파일은 DOWNLOADED, 관측 상태는 COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 원본 정리true·운영 쓰기0이다. 후보 판정을 관리자 확정으로 표현하지 않는다. 고정 표본의 현재 모집 여부·모든 공고 성공도 증명하지 않는다.

- 인천 프로필 지문: `5f55ebcf48f54b6259fc76481d0f96d8d74162487db30798400d839d2b76be51`
- 인천 파일 SHA256: `220ba26656d6626505bcb896efacb01d682330feda18627dc80202f5ce102fa5`
- 울산 프로필 지문: `4f953bb3e91989217a116aeefdc0a30be8b12c67549a6a5865f36b926b7888f8`
- 울산 파일 SHA256: `11739be61c8f6aebe974c25ab3ffd84b2409fa3e50adb762310846bba92baa68`

이전 MIME 실패 보고서는 `INCHEON_CITY-66970-BEFORE-MIME.json`, `ULSAN_CITY-47059-BEFORE-MIME.json`으로 해시 일치 보존하고 새 관측2건을 추가했다.

## 검증·집계·정리

```powershell
# SANEB_CITYNET_FILE_RESPONSE_FIXTURE=true와 기존 읽기 전용 inventory 환경변수 사용
.\gradlew.bat :test --tests '*CitynetFileResponseCompatibilityTest' --tests '*IncheonCityDownloadContractTest' --tests '*UlsanCityDownloadContractTest' --tests '*LegacyThreeMimeCompatibilityTest' --tests '*AnsanInjeMimeCompatibilityTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --tests '*AttachmentContractWorkflowTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=INCHEON_CITY,ULSAN_CITY' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

집중 테스트26건 중24통과·2조건부생략(29초), 확장373건 중369통과·4조건부생략, bootJar와 실제 관측2건 성공(2분18초)이다. 생략은 이전 조사 원본 fixture의 조건부 검증이며 이번 시티넷 실파일 검증은 실행했다. 실제 생성된 두 probe JAR를 읽는 회귀 테스트도 포함했다. 원본 정리 후에는 해당 fixture 환경변수를 켜지 않는다.

직전 SHA `7bdecd4bdbf828ddf3d5c436687f258c759c6408`의 [Linux CI](https://github.com/FrostyCityMan/saneB/actions/runs/36644914723)는 이번 조회 시 in_progress였으며 성공으로 기록하지 않는다. 이전 SHA의 CI 실패 원인·로컬 패키지 수정 검증은 [직전 보고서](announcement-legacy-three-mime-recovery-2026-09-30.md)에 구분되어 있다.

분모는2026-09-28 15:56:12 KST 읽기 전용 스냅샷의 활성223수집원이다. 최소1파일 다운로드 확인148→150/223(67.3%), 잔여75→73(미등록43+등록 미확인30), 현재 오류 기록38수집원이다. 오류는 성공 수집원과 겹칠 수 있다. 지역180+기업마당1=181프로필·236카탈로그 공고/179대상 유지,280영수증/224최신공고 재현 통과다. 기존3표본·전체 파일 Gate는16충족/207잔여이며 전체 구현 완료율과 구분한다.

진단4요청은 요청당15초·연결7초·상세2MiB·파일10MiB 상한이었다. 실제 관측 기관별1회는 각6요청/23MiB·상세1MiB 이내이며, 본문 포함 예약 합계8회/4,511,244byte, 진단 포함 예약 상한12회다. 예약과 실제 wire 요청 수는 구분한다. 조사 원본8개316,403byte는 두 임시 디렉터리의 정확한 경로·크기·SHA256 대조 후 개별 삭제했다. 복구에는 공식 재조회가 필요하며 보고서·해시는 보존했다. 이번 Node·Gradle·Java는 종료했고 기존 프로세스·무관한 미추적 파일은 보존했다.

운영 설치·DB/migration·API/UI·정책 게시·ENFORCE·기존 데이터·추출기·배포는 변경하지 않았다. 전체 로컬 테스트·Linux DB 통합·상시 유입·운영 E2E는 이번 로컬 수집 검증과 별개다. 브라우저는 현재 단계의 명시적 요청이 없어 정책상 생략했다. 작업 브랜치 `[skip deploy]` 범위이며 장기 goal은 진행 중이다.
