# 원주·대전 서구 기존 처리기 다운로드 확인

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일 보존·오류 분리와 제목→본문→첨부→관리자 최종 검증을 유지한다. 상시 worker·DB/API·운영 E2E 전체 완료와 구분한다.

- [x] 공식 목록·상세에서 원주 소상공인482226, 대전 서구 소상공인49944를 선택.
- [x] 기존 처리기 재사용, 기관별 상세 제목 확인, 수집 전용6요청·23MiB 상한 연결.
- [x] 실제 Java 경로로 본문2건·HWP2개1,034,240byte 다운로드·형식 확인.
- [x] 신규 참조2개는 기대값 null, 승인·정상 후보 승격 없음.
- [x] Node23건·293영수증/234공고 재현, 조사 원본10개 정리.
- [x] 확장 Java177건 실패·생략0, probe JAR 패키징·bootJar 검증.
- [ ] 첨부 추출·운영 반영·운영 E2E·전체 Goal 완료.

## 조사와 실패 분리

부산 본청 등록 목록 `https://www.busan.go.kr/nbgosi`는 HTTP401/481byte다. 인증을 우회하거나 예전 다운로드 성공을 현재 근거로 승격하지 않았다. 원주의 기존 참조491340은 수탁기관 선정 결과 공고여서 새 모집 표본을 공식 제목 검색으로 확보했다. 과거 참조를 삭제하거나 현재 모집 공고로 바꾸지 않았다.

대전 서구는 초기 임산부51908 제목이 DB 초기 규칙의 대상·지원 조합을 충족하지 못했다. 최초110건 중1건 실패와 진단2건 중1건 실패로 확인했고, 실제 관측 태스크는 실행되지 않았다. 상세 HTML 조사와 첨부 링크3개 관측은 있었으나 파일 요청은0회다. JPG2개를 다운로드하거나 형식을 임의로 지원 처리하지 않았다. 규칙을 바꾸지 않고 공식 소상공인 검색에서49944를 선택했다. 임산부 제목을 통과시키지 않는 회귀를 추가했고 수정 후 표적3건과 실제 관측은 통과했다.

선택한 공고는 수집 경로 검증용 과거 표본이다. 현재 신청 가능한 공고·최종 지원 대상으로 해석하지 않는다. 대전 서구 직접 수집원과 대전시 통합 수집원의 서구 공고 경로는 서로 다른 source다. 같은 행정구역을 새로운 고유 지역으로 센 것이 아니라 고정223수집원 분모의 직접 경로를 검증했다.

## 실제 관측

2026-09-30 10:57~10:58 KST, 각 표본1회다.

| 수집원 / 공고 | 본문 | 파일 | 결과 |
|---|---|---|---|
| 원주 LGS-000118 /482226 |323자, AVAILABLE/ACCEPTED|HWP927,232byte|다운로드 확인|
| 대전 서구 LGS-000074 /49944 |149자, AVAILABLE/ACCEPTED|HWP107,008byte|다운로드 확인|

두 공고 모두 상세 식별, FOUND/complete=true, `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 파일 SHA-256은 원주 `1c89b25c3b85f0a504a9e7e8875921bfe09e28decc06c990c9fbbce18845f320`, 서구 `aec927743b661eb27f96606f84d9f61e27df1d98e5f25663aef8b54ae5ab3d1a`다. HWP binary 검증이지 텍스트 추출 성공은 아니다. 추출기1.0.16 추가 개발 보류를 유지했다.

**158→160/223수집원(71.7%) 확인, 잔여65→63(미등록38+등록 미확인25)**다. 오류가 있는 수집원39개, 기존3표본·전체 첨부 Gate16충족/207잔여는 유지한다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수·모든 파일·운영 완료율이 아니다.

프로필186(지역185+기업마당1)은 그대로이며 실행 지문 변경은 없다. 카탈로그는241→243공고,184→185대상,242개 참조 전용+기존 기대값1이다. 기대값1도 정상 승인으로 세지 않는다. 원주·서구 새 참조의 source hash·기존 프로필 결합·REFERENCE_ONLY를 테스트한다. 목록 parser·운영 정책·Flyway·API·UI는 변경하지 않았다.

## 검증·자원

보관된291영수증/232공고와 다른 모든 지역의 최신 근거를 보존하고293영수증/234공고를 재현했다. 인벤토리는 기존 운영 스냅샷에 현재 로컬 카탈로그 참조 수만 대조한 결과이며 이번 운영 재조회가 아니다.

공식 조사10요청(GET8·POST2), 각15초/연결7초/최대2MiB/TLS 검증/자동 redirect0이다. Java 실제 관측은 본문 포함 예약8회·6,481,920byte다. 조사 포함 예약 상한18회다. 조사 원본10개5,991,857byte는 소유 경로·크기·SHA-256 대조 후 삭제했다. 관측 원본도 정리했고 비식별 보고서·해시는 보존했다. 원본 복구에는 재수집이 필요하다.

```powershell
.\gradlew.bat :test --tests '*ExistingSecondDownloadContractTest' :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EXISTING_SECOND' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*ExistingSecondDownloadContractTest' --tests '*ExistingProfileDownloadContractTest' --tests '*AnnouncementAttachmentBbs*ContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

최종 확장 Java177건 실패·생략0, probe JAR 패키징·bootJar 통과(2분26초), Node23건 실패·생략0을 확인했다. 사용한 단발 Node와 이번 Gradle/Java는 종료하고 기존 사용자 프로세스는 보존한다. 운영 DB·정책·ENFORCE·기존 데이터·배포는 변경하지 않는다. 브라우저는 현재 단계에서 명시 지시가 없어 사용자 정책상 생략한다. 다음은 남은 미등록 수집원 연결과 부산401·충주400 등 파일/접속 오류의 분리 처리다.
