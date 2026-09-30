# 잔여 광역·중부·남부 첨부 수집 재검증

## 현재 단계 / Gate

- [x] 기존 프로필·고정 표본으로24수집원·43공고 순차 관측
- [x] 23수집원·35공고·정상47파일 확보, 개별 실패 분리
- [x] 기존601영수증 보존, 644기록/282공고 재현, Node23통과
- [~] 현재 프로필 지문 기준 다운로드196/223(87.9%)·남은27곳
- [~] 과거 포함 다운로드204/223·최초 미확인19곳
- [!] 양평 상세 제목 확인1실패, 본문 호스트 검증8실패, 파일 형식·미지원 항목 별도
- [ ] 전체 추출·상시 worker·DB/API/UI·운영 E2E 및 장기 goal 완료

기준 HEAD `34c6bfb2151114ef8bdd6f78d385d75eb6662c6e`. 응용/테스트 코드·프로필·카탈로그·migration·운영 설정은 변경하지 않았다. 분모223은 2026-09-28 활성 수집원 스냅샷이다. 다운로드 가능 범위이며 전체 개발 완료율이 아니다. HWP 추출 고도화 보류와 성공 파일 보존·오류 분리 원칙을 유지한다.

## 관측 결과

| 묶음 | 수집원 / 공고 | 다운로드 수집원 / 공고 | 정상 파일 / 바이트 | 관측 테스트 |
|---|---:|---:|---:|---|
| 광역권 | 6 / 13 | 6 / 11 | 16 / 3,162,872 | 13통과, 종료0, 45초 |
| 중부권 | 10 / 22 | 10 / 17 | 22 / 3,749,203 | 21통과·1실패, 종료1, 약60초 |
| 남부·예산 | 8 / 8 | 7 / 7 | 9 / 1,224,164 | 8통과, 종료0, 37초 |

광역권: 대구 중구·달서·달성, 광주 남구, 대전 집계 수집원, 울산 동구. 중부권: 양평·횡성·영월·태백·제천·충주·보은·옥천·증평·단양. 남부·예산: 예산·완주·진안·무주·장수·임실·연제·구례. 대전 집계 수집원처럼 지방자치단체 행정구역과 수집원은 반드시 일대일은 아니다.

태백·충주는 기존 묶음 `EXISTING_FIRST`에 포함되어 다시 검증했다. 연제는 구례와 묶인 `YEONJE_GURYE`의 기존 형식 오류 표본43358을 재관측했으며, 별도 정상 표본42552의 성공 기록을 삭제하거나 덮어쓰지 않았다. 이번23곳 중 새로 현재 지문 다운로드 확인을 확보한 수집원은21곳이다.

엄격한3개 적격 표본의 첨부 집합 Gate는11→16/223이다. 달서·달성·횡성·영월·보은5곳이 추가됐다. 이는 첨부 수집만의 Gate이며 본문/추출 품질·관리자 승인·운영 완료를 뜻하지 않는다.

## 성공 파일과 분리한 문제

- `YANGPYEONG-312241`: 본문 AVAILABLE이지만 상세 제목 확인에서 `TITLE_CONFIRMATION / OBSERVATION_ASSERTION_FAILED`. 파일 발견·다운로드 성공으로 계산하지 않았다. 현재 공식 상세 제목과 고정 표본의 일치 여부가 후속 진단 대상이다. 원인을 삭제·개편으로 단정하지 않았다.
- `ULSAN_DONGGU-28029`, `YEONJE-43358`: `ATTACHMENT_FORMAT_MISMATCH`. 각각 같은 수집원의 다른 정상 표본과 분리했다.
- `YANGPYEONG-311846`, `JANGSU-32491`: 정상 파일을 각각1개 확보했고 미지원1항목씩은 다운로드하지 않았다.
- 제목 중단5개: 중구33315, 옥천193187, 양평311507, 영월156846·157529. 모두 요청0·파일0이며 수집 성공이나 기술 오류로 계산하지 않았다.
- 본문 `DETAIL_HOST_NOT_ALLOWED` 8개: 광주 남구45698, 증평31159, 단양32263, 완주43355, 진안33307, 무주34488, 장수32491, 임실33179. 첨부 확보와 본문 성공을 동일시하지 않았다.

본문 QA는 현재 `sample.listUrl()`을 등록 기준 URL로 전달한다. 증평·단양의 표본은 포털 목록 URL을 사용하지만 V61에는 `eminwon` 공식 수집 endpoint가 별도로 있다. 이는 QA 입력 계약 불일치 가능성을 뒷받침한다. 나머지6곳과 실제 서비스의 endpoint 선택 및 운영 값을 대조하기 전에는 운영 장애 원인으로 확정하지 않는다. 보안 검증기를 완화하거나 운영 등록 URL을 변경하지 않았다.

## 남은27곳

현재 지문에서 실제 다운로드를 확인하지 못한27수집원이다. 오류 수집원33개는 일부 다운로드 성공 지역과 겹치므로27개에 더하지 않는다.

- 최초 다운로드 미확인19곳: 은평·서대문·송파·검단·울산 남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북·영동·천안·공주·의성·성주.
- 과거 다운로드가 있으나 최신 관측에서 미확보8곳: 강남·미추홀·아산·금산·봉화·통영·남해·거창.

후속은 오류별로 분리한다. 강남·미추홀·아산·금산·거창은 유효 상세/목록 이동 계약, 봉화·통영은 다운로드 전송, 남해 등은 인증서·접속 환경, 울산 남구는 미연결 프로필이 대상이다. 동일 실패 요청을 반복하거나 다른 게시판을 대체 근거로 사용하지 않는다. AWS 인증 갱신이 필요한 서버 진단은 로컬 수집 성공과 별도 Gate다.

## 예산·대장·검증

43표본 기존 상한 합계592요청·1,665MiB, 본문 포함 예약163회·95,895,740바이트. 예약량은 실제 wire 전송량이 아니다. 추가 진단 HTTP0, 운영쓰기0, 원본 정리true, 추출·정책 QA·기대값 승인false다. 실행은 단일 Gradle/JVM으로 순차 수행했다.

보고서는 `build/reports/attachment-regional-collection/`의 `REMAINING-METRO-RECHECK-20261001`, `REMAINING-CENTRAL-RECHECK-20261001`, `REMAINING-SOUTH-RECHECK-20261001` 라벨43개다. 경로·해시는 receipt index, 집계는 regional ledger의 `remainingMultipleSampleCurrentCodeRecheckRun`에 있다. 기존601영수증과 최신 표본 중239개를 보존하고43개만 갱신했다.

인벤토리 SHA-256 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`, 실행 클래스 SHA-256 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab` 유지.

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JUNGGU,DALSEO,DALSEONG,GWANGJU_NAMGU,DAEJEON_AGGREGATOR,ULSAN_DONGGU,ULSAN_DONGGU_CARD' -PsanebCollectionReportLabel=REMAINING-METRO-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YANGPYEONG,HOENGSEONG,YEONGWOL,EXISTING_FIRST,BOEUN,OKCHEON,JEUNGPYEONG,DANYANG' -PsanebCollectionReportLabel=REMAINING-CENTRAL-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YESAN,WANJU,JINAN,MUJU,JANGSU,IMSIL,YEONJE_GURYE' -PsanebCollectionReportLabel=REMAINING-SOUTH-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 관측42통과/1실패, Node23통과, 644기록/282공고 재현 통과. 코드 변경이 없어 Java 단위 전체·bootJar는 재실행하지 않았다. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. 테스트 실패를 제외한 전면 통과나 전체 개발 완료로 보고하지 않는다.

운영 DB·worker·정책·ENFORCE·재분류·배포 변경 없음. AWS 인증 갱신 대기와 과거 CA/HTML 임시 자원 정리 차단은 별도 미해결이다. 이번 임시 원본 정리가 과거 잔여 자원의 정리까지 증명하지 않는다.
