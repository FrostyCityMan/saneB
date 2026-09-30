# 첨부 수집 성공과 단계별 오류의 분리 진단

## 현재 단계 / Gate

- [x] 봉인된666영수증·289최신 표본을 다시 검증하는 읽기 전용 진단 추가
- [x] 성공한 다운로드와 본문·첨부 발견·파일 전송·형식 오류를 별도로 출력
- [x] 현재 프로필 근거·오래된 프로필 근거·관측 없음·프로필 없음 분리
- [x] Node32개 통과·구문 검사·기존 대장 재현 통과
- [~] 파일 확인203/223(91.0%), 잔여20개 유지
- [ ] 잔여 수집원 파일 확보·상시 유입·추출·운영 DB/API/UI·운영 E2E

기준 HEAD `24377e19a5da96705e2e4fedf50a8b45de1aa602`. 직전 동두천 실패 근거 보존은 진전이다. 이번에는 `long-goal-operating-protocol`에 따라 불명확한 장애를 반복 조회하기보다 보관된 근거의 범위를 먼저 구분했다. 운영 변경·HWP 추출 고도화·브라우저 실행은 하지 않는다.

## 개선한 문제와 계약

기존 `report-collection-availability.mjs`는 첨부 수집 가능 현황을 집계한다. 그 `regionsWithErrors`는 본문까지 포함한 전체 장애 수가 아니며, 요약된 sample에는 상세 오류 코드가 없다. 기존 집계를 바꾸거나 과거 영수증을 덮지 않고 다음 진단을 추가했다.

- `attachment-collection-diagnostics.mjs`: inventory hash, receipt hash, 안전한 파일 경로, 기존 importer의 격리 출처, 최신 sample의 정확한 일치를 확인한다. 그 후 같은 receipt와 case의 원본 보고서를 연결한다.
- `report-collection-diagnostics.mjs`: 기본 요약, `--pending` 다운로드 미확인 수집원, `--details` 전체 상세를 출력한다. 파일 읽기와 stdout 외 부작용이 없다.
- `attachment-collection-diagnostics.test.mjs`: 본문 실패와 성공 파일 공존, 단계별 오류, 원문 비노출, 최신 관측 선택, 정책 검수와 기술 오류 구별, 프로필 불일치, 격리 worker 보고서, 증거 변조 거부를 검증한다.

결과에는 고정 식별자·해시·관측 시각·허용한 상태/오류 코드만 들어간다. 원문·파일명·URL·임의 오류 메시지를 복사하지 않으며 모르는 코드는 `UNCLASSIFIED_ERROR`다. 본문 AVAILABLE에서 정책상 REVIEW_REQUIRED인 결과는 기술 장애로 계산하지 않는다. 제목 정책 중단도 오류로 취급하지 않는다.

현재 프로필과 지문이 다른 보고서는 현재 성공/오류로 판정하지 않고 `STALE_EVIDENCE_ONLY` 및 건수로 표시한다. 빈 오류 배열이 정상 상태라는 뜻은 아니다. `NO_OBSERVATION`은 가져온 대장에 근거가 없다는 뜻으로, 다른 문서에 진단 이력이 전혀 없다는 의미가 아니다.

이 변경은 **로컬 QA 진단 도구**이며 운영 worker·관리자 UI 개선이나 자동 재시도 구현이 아니다. DB/API·화면·Flyway·카탈로그·프로필·규칙·666영수증·289표본·기존 대장은 변경하지 않았다.

## 실제 대장에 적용한 결과

2026-09-28 활성 수집원223개 스냅샷과2026-10-01까지 보관된 관측을 대조했다. 현재 운영 상태 재조회가 아니다.

| 지표 | 결과 |
|---|---:|
| 현재 프로필 기준 파일 확보 | 203/223 |
| 다운로드 미확인 | 20 |
| 기존 첨부 가능 현황의 오류 포함 수집원 | 33 |
| 새 진단의 기록된 단계별 문제 포함 수집원 | 37 |
| 본문 수집 오류 기록이 있는 수집원 | 9 |
| 파일 확보 이력과 본문 오류가 함께 있는 수집원 | 7 |
| 엄격한3표본 첨부 집합 Gate | 16/223 |

33과37은 집계 범위가 다르므로 합산하지 않는다. 7개는 수집원 단위 교집합이며 반드시 같은 공고에서 파일 성공·본문 실패가 발생했다는 의미는 아니다. 예를 들어 강남·미추홀·거창은 새 표본에서 파일을 확보했지만 별도 예전 공고의 본문 실패는 보존되어 있다.

본문 오류9개: 노원(BODY_TEXT_EMPTY), 강남·미추홀·거창·아산(BODY_SELECTOR_CHANGED), 광주 남구(DETAIL_HOST_NOT_ALLOWED), 담양·속초(NETWORK_ERROR), 곡성(HTTP_STATUS_ERROR). 이 중 아산·속초는 현재 프로필에서 파일 다운로드도 미확인이다. 후속 본문 개선 목록이며 현재 지역별 파일 확보 우선순위를 대체하지 않는다.

### 잔여20개 분류와 후속

| 근거 상태 | 수 | 수집원 | 다음 조치 |
|---|---:|---|---|
| 현재 프로필과 이전 검증 지문 불일치 | 12 | 은평·서대문·검단·성남·평택·이천·포천·강릉·충북·영동·공주·성주 | 지문 변경 범위 확인 후 필요한 현재 프로필 검증만 선택 |
| 현재 프로필의 오류 근거 보유 | 6 | 동두천·속초·아산·의성·봉화·남해 | HTML 비파일 응답·TLS·전송 시간 초과 등을 각각 처리 |
| 대장 관측 없음 | 1 | 철원 | 기존 연결 진단과 대장 범위 구분, 접근 조건 변경 시 검증 |
| 프로필 미연결 | 1 | 울산 남구 | 공식 상세·첨부 근거 확보 후 구현 |

지문 불일치는 사이트 상태가 달라졌다는 뜻이 아니다. 기존 실패의 정확한 코드와 현재 프로필 변경을 비교한 뒤 재관측 필요성을 결정한다. 서울 서버 인증 대기가 해결되지 않은 상황에서 같은 실패 요청을 무제한 반복하지 않는다.

## 울산 남구 확인 범위

[공식 홈페이지](https://www.ulsannamgu.go.kr/cmm/main/mainPage.do)를 직접1GET으로 확인했다. HTTP200,332,722바이트, SHA-256 `42e3fdf5e5a338bf1de53374359a7bfc1ef1f6a7a0cc5bf636af664d958dc343`. 공식 고시공고 메뉴 연결은 있으나, 홈페이지에 고정 고시공고 상세 식별자와 첨부 계약을 구현할 새 근거는 없었다. 별도 새소식 게시판을 현재 수집원으로 대체하지 않았다.

직접 요청 상한12초·2MiB·연결5초, 쿠키0·redirect0·TLS검증 유지, 원문 파일 저장0. 공식 도메인 검색은 울산 남구2질의·성주1질의였다. 검색 결과 부재를 서비스 장애로 해석하지 않는다. 이전 실패 새올 POST를 재실행하지 않았고 프로필도 추가하지 않았다.

## 실행 명령 / 검증

```powershell
node --test scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-diagnostics.mjs
node scripts/qa/report-collection-diagnostics.mjs --pending
node --check scripts/qa/attachment-collection-diagnostics.mjs
node --check scripts/qa/report-collection-diagnostics.mjs
git diff --check
```

신규9개 포함32개 통과, 실패·생략0.666영수증/289표본 재현 통과. Node 프로세스 종료 확인. Java·migration·응용 코드 변경이 없어 Gradle·bootJar·DB 통합을 이번에는 재실행하지 않았다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 운영 DB·설정·정책·worker·ENFORCE·배포는 변경하지 않았다. 사용자 미추적 파일은 보존한다. 전체 goal은 미완료로 유지한다.
