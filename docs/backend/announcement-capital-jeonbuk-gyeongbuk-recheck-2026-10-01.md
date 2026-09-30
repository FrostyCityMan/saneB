# 경기·전북·경북 24수집원 첨부 재검증

## 현재 단계 / Gate

- [x] 고정 공고 24건을 세 묶음으로 순차 재검증
- [x] 24수집원에서 본문 AVAILABLE 및 정상 첨부 27개 확인
- [x] 청송의 첨부 발견 경고를 성공 파일과 분리
- [x] 기존 455영수증 보존, 479기록/282공고 재현, Node 23개 통과
- [~] 현재 코드 다운로드 92/223, 확인·재검증 131개
- [~] 과거 포함 다운로드 204/223(91.5%), 최초 미확인 19개 유지
- [ ] 전체 텍스트·상시 worker·DB/API/UI·운영 E2E 및 전체 goal 완료

기준 HEAD는 `29fb7a4330583f1a17c13a1c3ec04d9ec41bd096`다. 이번 회차는 응용 코드·테스트 코드·프로필·카탈로그·migration을 변경하지 않은 실제 수집 재검증이다. HWP 추출 고도화 보류와 성공 파일 보존·개별 오류 분리 방침을 유지했다. 분모 223은 2026-09-28 활성 수집원 스냅샷이며, 고유 지자체 수나 현재 운영 재조회 결과 또는 전체 개발 완료율이 아니다.

## 실제 수집 결과

| 묶음 | 수집원 | 다운로드 확인 | 정상 파일 / 바이트 | 실행 결과 |
|---|---|---:|---:|---|
| 경기 | 남양주·시흥·김포·의정부·경기광주·하남·양주·군포 | 8/8 | 9개 / 809,556 | 8통과, 종료0, 41초 |
| 전북 | 전북도·전주·군산·익산·정읍·남원·부안·고창 | 8/8 | 8개 / 936,091 | 8통과, 종료0, 42초 |
| 경북 | 김천·구미·영천·청송·영양·청도·칠곡·예천 | 8/8 | 10개 / 831,615 | 8통과, 종료0, 41초 |

합계 **24수집원·27파일·2,577,262바이트**다. 본문 상태는 24건 모두 AVAILABLE이지만, 이는 본문 확보 근거이며 텍스트 전체의 정확성·첨부 분석·운영 최종 승인까지 검증했다는 뜻은 아니다. 보관된 과거 공고도 포함하므로 최신 모집 공고의 상시 유입 증거로 대체하지 않는다.

청송 `CHEONGSONG-22287`은 본문 8,542자와 정상 첨부 2개·31,744바이트를 확인했으나 `ATTACHMENT_LINK_UNRESOLVED`가 남았다. 따라서 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`를 보존한다. 나머지 23건은 해당 고정 표본의 첨부 집합 수집 완료이며, 수집원별 3표본 검증이나 운영 완료로 승격하지 않는다. 관측 테스트 통과는 오류의 정확한 기록도 허용하므로 실제 다운로드 수와 별도로 확인했다.

세 Gradle 실행은 각각 종료를 확인한 다음 순차 진행했다. 고정 표본당 6요청·23MiB 상한, 전체 상한 144요청·552MiB를 유지했다. 본문 포함 예약은 100회·57,924,417바이트다. 예약 수치와 실제 wire 전송 수치를 구분한다. 모든 새 보고서에서 원본 정리 true, 운영 쓰기 0, 첨부 추출·정책 QA·기대값 승인 false를 확인했다.

## 실행 명령

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=NAMYANGJU,SIHEUNG,GIMPO,UIJEONGBU,GG_GWANGJU,HANAM,YANGJU,GUNPO' -PsanebCollectionReportLabel=CAPITAL-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JEONBUK,JEONJU,GUNSAN,IKSAN,JEONGEUP,NAMWON,BUAN,GOCHANG' -PsanebCollectionReportLabel=JEONBUK-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GIMCHEON,GUMI,YEONGCHEON,CHEONGSONG,YEONGYANG,CHEONGDO,CHILGOK,YECHEON' -PsanebCollectionReportLabel=GYEONGBUK-NEXT-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

실제 관측 24개와 Node 23개가 통과했다. 소스 변경이 없으므로 Java 단위 전체·bootJar는 이번 회차에 재실행하지 않았다. 이전 회차의 Java169·bootJar 결과를 이번 실행 결과로 표현하지 않는다.

보고서는 `build/reports/attachment-regional-collection/`의 `CAPITAL-NEXT-RECHECK-20261001`, `JEONBUK-NEXT-RECHECK-20261001`, `GYEONGBUK-NEXT-RECHECK-20261001` 라벨 24개다. 각 경로·해시는 receipt index에 추가했고 집계는 regional ledger의 `capitalJeonbukGyeongbukCurrentCodeRecheckRun`에 기록했다. 실행 클래스 SHA-256은 `19be9b3d87ce787eb47989f11745d39e68c6cf31da5d4c7579c0ea680385a9ab`, 인벤토리 SHA-256은 `b74017aa8a20fe2c693b1fefa8346ed8b0b281bfa37df7e6af68834d5717eaf4`로 기존과 같다.

## 보존과 남은 범위

이전 455영수증은 수정하지 않고 24개를 추가했다. 최신 공고는 282개로 유지하고 해당 24개만 새 관측으로 갱신했다. 현재 코드 다운로드는 68→92개, 확인·재검증은 155→131개다. 첨부 오류가 포함된 수집원은 16→17개이며 정상 다운로드 수와 중복될 수 있다. 엄격한 3표본 전체 집합 Gate는 0/223으로 별도 유지한다.

과거 포함 최초 다운로드 미확인 19개는 은평·서대문·송파·검단·울산남구·성남·평택·이천·포천·동두천·강릉·속초·철원·충북도·영동·천안·공주·의성·성주다. 이 19개의 외부 상태를 이번에 다시 조회한 것은 아니다. 최신 코드 미확인 131개에는 이들과 과거 성공 후 재검증이 필요한 수집원 등이 함께 포함된다.

운영 DB·설정·worker·정책·ENFORCE·재분류·배포는 변경하지 않았다. AWS 인증 갱신 대기와 과거 공개 CA·HTML 임시 자원 정리 차단은 별도 미해결이다. 이번 관측의 원본 정리 성공으로 과거 자원까지 정리됐다고 보고하지 않는다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이며 전체 goal은 미완료로 유지한다.
