# 첨부 수집 이력과 재확보 대상 분리

## 현재 단계 / Gate

- [x] 아산·봉화·남해의 과거 파일 성공과 최신 실패 원본 영수증 대조
- [x] 현재 코드 검증과 과거 성공·공고별 최신 관측을 별도로 자동 집계
- [x] 과거 지문의 최신 오류에 관측 시각·지문·현재 일치 여부 명시
- [x] `--unrecovered`로 최신 표본 미확보18개만 조회
- [x] Node46개·구문 검사·704영수증/289표본 재현 통과
- [ ] 미확보 수집원 실제 파일 재확보·울산 남구 연결
- [ ] 운영 worker·DB/API/UI·배포·브라우저 E2E

기준 HEAD `27efa7980aa0f474f109a699ab63ced62672c638`. `long-goal-operating-protocol`의 근거 분리 원칙을 적용했다. 변경은 로컬 읽기 전용 QA 진단기·테스트·진행 기록이며 응용 기능이나 운영 오류 화면 구현이 아니다. 새로운 공식 사이트 요청·첨부 다운로드·운영 쓰기는0이다.

## 문제

공통 전송 코드 변경 후 모든 프로필 실행 지문이 바뀌면 기존 진단기는 과거 근거를 `STALE_EVIDENCE_ONLY`로 표시하고 현재 오류 배열을 비웠다. 현재 검증과 과거 검증을 분리하는 동작 자체는 맞지만, 기존 실패 복구 대상을 추려내려면 보관 영수증을 매번 수작업으로 다시 읽어야 했다. 이 때문에 “현재 코드 미검증207개”와 “최신 관측에서 미확보18개”, “최초 다운로드 미확인15개”를 혼동할 수 있었다.

## 변경 계약

1. 기존 `availabilitySummary`, `diagnosticSummary`, `regions[].issues` 의미는 그대로다. 현재 프로필과 다른 근거로 현재 성공이나 오류를 만들지 않는다.
2. `historySummary`는 봉인된 모든 영수증의 이력과 공고별 최신 표본을 별도로 집계한다. `ARCHIVED_HISTORY_NOT_CURRENT_CODE_VERIFICATION`을 명시한다.
3. `regions[].history`에는 과거 적격 다운로드 유무, 최신 표본 적격 다운로드 유무, 마지막 성공 관측 시각, 최신 표본의 단계별 오류를 추가한다. 오류별 `profileHash`와 `isCurrentProfile`을 표시한다. 원문·URL·파일명·알 수 없는 오류 문자열을 복사하지 않는다.
4. 과거 성공도 제목 적격·상세 식별·파일 서명·원본 정리가 확인된 자료만 인정한다. 같은 수집원의 반복 영수증이나 비활성 수집원으로 성공 수가 늘지 않는다.
5. 새 표본에 정상 파일이 있으면 다른 과거 표본 실패 때문에 수집원 전체를 미확보로 세지 않는다. 다른 표본의 오류는 별도로 유지한다.
6. 제목 정책 중단은 기술 오류가 아니다. 과거 성공 후 최신 표본이 제목 중단이면 `RECHECK_AFTER_PAST_DOWNLOAD`로 구분하되 장애를 만들어내지 않는다.
7. `--pending`은 기존대로 현재 코드 다운로드 미확인 목록이다. 새 `--unrecovered`는 과거 지문을 포함한 공고별 최신 표본에서 정상 파일이 없는 수집원만 출력한다. 운영 재시도·정책 활성화·배치 실행을 하지 않는다.

## 실제 집계

분모는2026-09-28 활성 수집원 스냅샷223개다. 현재 운영 조회·고유 지자체 수·전체 goal 완료율이 아니다.

| 구분 | 결과 |
| --- | ---: |
| 모든 영수증에서 과거 다운로드 성공 존재 | 208 |
| 최초 다운로드 근거 없음 | 15 |
| 공고별 최신 표본에서 다운로드 성공 존재, 과거 지문 포함 | 205 |
| 최신 표본 미확보 | 18 |
| 과거 성공 후 최신 미확보 | 3 |
| 현재 실행 지문에서 다운로드 성공 | 16 |
| 현재 실행 지문 미검증/미확보 | 207 |

`--unrecovered`는18개를 반환했다. 아산·봉화·남해는 `RECHECK_AFTER_PAST_DOWNLOAD`이고 나머지15개는 최초 미확인이다. 울산 남구·철원의 `NO_OBSERVATION`은 이 수집 성공 대장에 표본이 없다는 뜻이다. 별도 구조·접속 진단 이력이 전혀 없다는 뜻으로 사용하지 않는다. 과거 상세 응답이 HTTP200이라는 이유로 이 대장에 다운로드 성공을 추가하지 않았다.

### 재확보3개 근거

| 수집원 | 과거 성공 | 최신 표본의 별도 문제 |
| --- | --- | --- |
| 아산 LGS-000151 | 80727 HWPX2개, 2026-09-29T11:49:16Z | 80727 제목/본문 구조 미확인, 추가76469는 Linux 본문·상세 시간 초과 |
| 봉화 LGS-000220 | 32956 HWPX·HWP2개, 2026-09-29T04:55:23Z | 파일2개 전송 시간 초과, 발견 경고와 미지원1개 병행 |
| 남해 LGS-000236 | 35694 HWPX1개, 2026-09-30T05:56:12Z | Linux 본문·상세 시간 초과, 이전 Windows 파일 TLS 실패 |

성공과 실패는 서로 다른 관측 시점·실행 지문이다. 이 회차에 접속을 새로 시도하지 않았으며 코드 결함·영구 장애·차단 원인을 확정하지 않았다. 기존 성공 파일을 삭제하거나 최신 실패를 정상으로 덮지 않았다.

## 검증 명령 / 결과

```powershell
node --test scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-collection-availability.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-github-collection-receipts.test.mjs
node scripts/qa/report-collection-diagnostics.mjs
node scripts/qa/report-collection-diagnostics.mjs --unrecovered
node scripts/qa/verify-collection-receipt-index.mjs
node --check scripts/qa/attachment-collection-diagnostics.mjs
node --check scripts/qa/report-collection-diagnostics.mjs
git -c core.safecrlf=false diff --check
```

신규7개 포함46개 통과·실패/생략0. 과거 성공 후 실패·다른 표본 성공·원본 정리 실패·식별 실패·반복 영수증·비활성 수집원·제목 중단·민감 문자열 비노출을 검증했다. 704영수증/289최신 표본은 수정하지 않고 재현했다. 프로필·inventory·Flyway·API·HWP 추출기는 변경하지 않았으며 Gradle·bootJar·DB 통합·브라우저는 이 Node 진단 변경에서 재실행하지 않았다. 브라우저는 사용자 정책상 미실행이다.

이 개선은 재검증 우선순위를 명확히 하는 도구이며 실제 파일 확보 수를 늘린 구현으로 보고하지 않는다. 다음 작업은 미확보18개의 접근 조건·공식 경로 또는 새로운 응답 근거가 있는 대상의 실제 연결이다. 울산 남구 보조 게시판 추가는 사용자 결정 대기이고, 운영 인증·화천 고정 worker·운영 E2E는 별도 Gate로 남는다.
