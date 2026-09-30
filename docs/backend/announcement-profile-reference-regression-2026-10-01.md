# 프로필 기준값 대조와 전체 루트 회귀 검증

## 현재 단계 / Gate

- [x] 잔여12수집원의 지문 불일치와 공통 요청 코드 변경 대조
- [x] 검단 헤더 테스트의 오래된 영종 지문 실패 재현·정정
- [x] 루트 전체 회귀 실행: 통과4,083·실패6, 조건부 생략 별도
- [x] 일반 참조 기준값5건 정정 후 집중24개 통과
- [!] 화천 과거 고정 worker 프로필 불일치1건 보존
- [x] Node32개·666영수증/289표본 재현 통과
- [~] 파일 확인203/223·잔여20개 유지, 전체 goal 미완료

기준 HEAD `307ed019b3bdffc52f44e87255e42bfc08654ba7`. `long-goal-operating-protocol`에 따라 실제 실패를 먼저 재현하고 근거가 있는 참조만 갱신했다. 이번 응용 코드·프로필·DB/API·규칙·운영 변경과 공식 사이트 재요청은 없다. HWP 고도화 보류와 성공 파일 보존 정책을 유지한다.

## 지문 불일치와 재시도 판단

`AttachmentProfileFingerprint`는 기관별 클래스뿐 아니라 공통 `AttachmentDiscoveryProfile`, `AttachmentPinnedDownloadClient.Request`와 전송기 등 실행 바이트코드를 포함한다. `bf623a0`에서 영덕의 실측 Referer·UTF-8 Location 처리를 추가하면서 공통 Request 필드와 기본 요청 검증 코드도 변경됐다. 기본 생성자는 Referer=null·UTF-8 redirect=false를 유지하고 명시적으로 연결한 프로필만 호환 처리를 사용한다.

따라서 지문 불일치는 기관 사이트가 바뀌었거나 이전 네트워크 오류가 해결됐다는 증거가 아니다. 은평·서대문·검단·성남·평택·이천·포천·강릉·충북·영동·공주·성주의 보관된 실패를 현재 성공 근거로 승격하지 않았다. 디스크에 이12곳의 현재 지문 관측 보고서가 이미 있는지도 확인했으나 발견하지 못했다. 새 전송 조건 없이 같은 실패 요청을 반복하지 않았다.

광주 남구 본문 오류도 V61과 테스트를 확인했다. 등록 endpoint는 포털이고 별도 direct endpoint가NULL이므로 QA 주소만 새올 호스트로 바꿔 성공시키는 수정은 하지 않는다. 실제 포털 상세 계약 연결이 별도 구현 대상이다.

## 정정한 일반 참조 테스트

갱신 대상 지문은 현재 inventory와 기존 최신 실파일 영수증에서 일치함을 확인했다. 실제 파일을 새로 수집하거나 정책 기대값을 승인한 것이 아니다.

| 테스트 | 정정 근거 | 보존·추가한 검증 |
|---|---|---|
| GeomdanHeaderCompatibilityTest | 영종332418의 현재 지문·파일 성공 | 영종에서 검단용 UTF-8 헤더 복원을 거부하는 음성 검증 추가 |
| AnnouncementAttachmentJungguObservationContractTest | 중구34196·33626 파일 성공,33315 제목 중단 | 고정3공고·파일 수·expectation null·제목 중단 유지 |
| AnsanInjeMimeCompatibilityTest | 시흥82127·양구 고정 표본 파일 성공 | 시흥 MIME 예외 없음, 양구 자체 `application/octer-stream`만 유지 |
| JemulpoHeaderCompatibilityTest | 미추홀315063 파일 성공 | 제물포 옵션이 미추홀에 적용되지 않는 검증 유지 |
| PajuHeaderCompatibilityTest | 광명65908 파일 성공 | 파주 옵션이 광명에 적용되지 않는 검증 유지 |
| AttachmentProviderQaPlanTest | `4b1d424`에서 괴산 bean 추가 | 지정 configuration의35프로필·36대상·35결합·정부24 미연결1개, 괴산 포함 검증 |

프로필 수35는 이 단위 테스트가 선택한 configuration들의 부분 집합이다. 전체 수집원223개 또는 전체 등록 프로필 수와 혼동하지 않는다. 테스트 기준값을 현재 파일에서 동적으로 읽어 자기 자신을 정답으로 삼게 바꾸지 않았으며 정확한 지문 검증을 유지했다.

## 실행 결과와 남은 실패

1. 최초 집중 회귀: 콘솔59개 중1실패/1생략. 실패는 영종의 오래된 지문이었다.
2. 검단 테스트 정정 후 같은 집중 회귀: 종료0,58통과/1조건부 생략. bootJar는 UP-TO-DATE로 새 패키징 실행은 아니다.
3. 루트 전체 `:test`:11분5초·종료1. **통과4,083·실패6**. 콘솔은4,409개/320생략, JUnit XML 합계는4,467개/378생략으로 집계 표현이 달랐다. 통과·실패 수는 일치한다. 생략을 통과로 계산하지 않았다.
4. 일반 참조5건 정정 후 집중 회귀에서 새로 추가한 양구 MIME 단언1건이 실패했다. 양구의 기존 고유 예외를 확인해 정확한 `containsExactly` 검증으로 수정했다. 응용 MIME 허용 목록은 변경하지 않았다.
5. 최종 집중 회귀:34초·종료0, **JUnit 기준29개 중24통과/5조건부 생략**, 실패0. 생략은 원본 fixture 및 Linux 전용 검증이다. bootJar는 UP-TO-DATE다.
6. Node32개 통과, 기존 대장666영수증/289표본 재현 통과. 전체 루트 회귀는 수정 후 다시 실행하지 않았으므로 전면 통과라고 보고하지 않는다.

전체 회귀의 실패6개는 화천 고정 계약과 위 표의 검단을 제외한5개 일반 참조였다. 검단 실패는 전체 회귀 전에 정정했다.

### 화천은 승인 경계로 별도 보존

`AnnouncementAttachmentHwacheonWorkerProbeTest.fixedCaseVersionBudgetAndTitleDoNotExpandOtherGroups`는 `HWACHEON_PROFILE_CHANGED`로 실패했다.

- 과거 고정 프로필: `4e34a0852383aad7ab5c20635340c845acc0bc43683df3d7fdf6d44429271e84`
- 현재 프로필: `65179ef4fdc567c6fb9f780ea993b6e40ff6f54e9eb5113d091b9d7759a4e730`

`HwacheonOfficialWorkerContract`는 고정32258·HWP82,944바이트·locator·본문/구간 DB/API·추출기1.0.15·최대5요청/24MiB를 함께 제한한다. 새 프로필로 같은 worker 근거가 증명되지 않았으므로 고정값 갱신·assertion 제거·테스트 생략 처리를 하지 않았다. HWP 개선 보류 중이며, 재개 시 승인 범위에 맞는 격리 worker 검증이 필요하다. 현재의 파일 수집 성공을 해당 worker 검증으로 대체하지 않는다.

## 명령

```powershell
.\gradlew.bat --no-daemon :test --tests '*AttachmentRefererDownloadTest' --tests '*GeomdanHeaderCompatibilityTest' --tests '*IncheonThirdDownloadContractTest' --tests '*AttachmentPinnedDownloadClientTest' --tests '*ProviderContentUrlValidatorTest' --tests '*RegionalObservationSourceContractTest' :bootJar
.\gradlew.bat --no-daemon :test
.\gradlew.bat --no-daemon :test --tests '*GeomdanHeaderCompatibilityTest' --tests '*AnnouncementAttachmentJungguObservationContractTest' --tests '*AnsanInjeMimeCompatibilityTest' --tests '*JemulpoHeaderCompatibilityTest' --tests '*PajuHeaderCompatibilityTest' --tests '*AttachmentProviderQaPlanTest' --tests '*AttachmentRefererDownloadTest' :bootJar
node --test scripts/qa/attachment-collection-diagnostics.test.mjs scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

외부 관측·fixture·DB opt-in 환경변수를 활성화하지 않았고 Gradle은 하나씩 순차 실행했다. 사용한 Node·Gradle 자원 종료를 확인했다. 운영 AWS·DB·배포·브라우저는 미실행이며 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 사용자 output·캐시는 보존한다. 수집원 스냅샷은2026-09-28이며 현재 운영 조회가 아니다. 전체 goal은 미완료다.
