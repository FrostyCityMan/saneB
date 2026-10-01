# 최신 프로필의 경기·광역권·경북 첨부 재검증

## 현재 단계 / Gate

- [x] 기존 성공 표본22곳을 현재 코드로 순차 관측. 21곳에서32파일·6,554,471byte 확보.
- [x] 안성의 정상 파일1개와 본문/파일 오류를 함께 보존. 포항 상세 시간 초과도 실패 영수증으로 기록.
- [x] 710개 기존 영수증 보존, 22개 추가하여732개/289표본 재현. 관련 Node43개 통과.
- [~] 현재 프로필 파일 확인 **21→42/223**, 현재 지문 파일 미확인181개.
- [~] 최초 미확인13개 유지. 최신 표본 미확보 **13→14개 = 최초13 + 포항 재확보1**.
- [ ] 엄격3표본·전체 파일 집합 Gate 및 전체 텍스트·상시 worker·운영 DB/API/UI·운영 E2E 완료.

기준 HEAD `5de4d6e29ae2cccdab194a10a5f6b38aef6881e4`. 장기 goal 운영 스킬을 적용하여 같은 조건의 미해결 요청 반복 대신 기존 성공 지역을 묶어 최신 프로필 재검증을 진행했다. 응용/테스트 코드·프로필·카탈로그·Flyway·정책·운영 설정은 변경하지 않았다. HWP 추출 고도화는 보류한다.

## 실제 결과

| 묶음 | 수집처 | 실파일 확보 | 파일 / 바이트 | Gradle 결과 |
|---|---|---:|---:|---|
| 경기 | 용인·안산·파주·안성·구리·의왕·여주 | 7/7 | 8개 / 955,857 | 7통과·종료0·49초 |
| 광역권 | 광주 동구·서구·북구, 대전 동구, 울산시·중구·북구, 세종 | 8/8 | 10개 / 1,168,809 | 8통과·종료0·37초 |
| 경북 | 포항·경주·안동·상주·문경·경산·고령 | 6/7 | 14개 / 4,429,805 | 6통과/1실패·종료1·90초 |

총22관측 중21통과/1실패이며, **경북 Gradle 명령은 실패**다. 관측 통과는 부분 오류의 정확한 기록을 포함하므로 전체 파일 성공과 다르다. 광명·영주는 이미 현재 프로필 근거가 있어 이번 묶음에서 재요청하지 않았다. 고정된 과거 공고의 파일 검증이며 신규 목록의 상시 유입 완료를 의미하지 않는다.

### 남은 개별 오류

- `ANSEONG-72476`: 본문 `FETCH_FAILED/TIMEOUT`, 정상 파일1개와 파일2개 `TRANSPORT_TIMEOUT`. 본문 실패로 첨부 성공을 폐기하지 않으며, 과거 파일2개 성공을 이번 결과에 합산하지 않는다.
- `POHANG-73525`: 본문385자 `AVAILABLE/ACCEPTED` 이후 `DETAIL_DISCOVERY/TRANSPORT_TIMEOUT`, 첨부 요청 미도달·파일0. 과거 성공은 이력으로 보존하지만 최신 관측은 재확보 대기로 표시한다. 포항 전체 사이트 장애로 단정하지 않는다.
- 나머지20표본은 본문 AVAILABLE 및 관측된 전체 첨부 수집 완료다. 지역별3공고 전체 집합 Gate를 충족했다는 뜻은 아니다.

## 요청·원본 정리·근거

각 표본 기존 상한6요청·23MiB, 총132요청·506MiB다. 실제 영수증 예약은100회·56,952,078byte이며 본문 예약 상한을 포함하므로 wire 요청/전송량과 구분한다. 과거 요청량을 초기화하지 않고 이번22개를 추가 이력으로 남겼다. 추가 진단 요청·자동 재실행0이다.

22개 보고서 모두 원본 정리true·운영쓰기0·첨부 추출false·정책 QA/기대값 승인false다. 원문 파일은 남기지 않고 비식별 metadata만 보존했다. Gradle은 `--no-daemon --max-workers=1`, 관측 JVM은384MiB·단일 fork로 순차 실행했다. 이번 프로세스들은 종료 확인 후 다음 묶음을 시작했다. 각 JUnit XML은 `build/qa-current-regional-20261001-b/{capital,metro,gyeongbuk}`로 별도 보존하여 다음 실행으로 덮지 않았다.

보고서 경로는 `build/reports/attachment-regional-collection/` 아래 `CURRENT-CAPITAL-20261001-B`, `CURRENT-METRO-20261001-B`, `CURRENT-GYEONGBUK-20261001-B` 라벨22개다. 개별 경로/SHA256은 receipt index에 기록했다. 인벤토리 SHA256 `5a15c0f7ffcbedf5103ca02851ffcfd969b32c84d8202bb87edab5f6da488858`, 생산 클래스 SHA256 `020fb8bc2256fab329fa07060218181a9fedb5d536b39a27d1a53d43e0c74e2b`를 확인했다. 모든 새 보고서의 프로필 지문이 인벤토리와 일치하며 다른267개 표본은 불변이다.

## 검증 명령

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YONGIN,ANSAN,PAJU,ANSEONG,GURI,UIWANG,YEOJU' -PsanebCollectionReportLabel=CURRENT-CAPITAL-20261001-B -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GWANGJU_DONGGU,GWANGJU_SEOGU,GWANGJU_BUKGU,DAEJEON_DONGGU,ULSAN_CITY,ULSAN_JUNGGU,ULSAN_BUKGU,SEJONG' -PsanebCollectionReportLabel=CURRENT-METRO-20261001-B -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=POHANG,GYEONGJU,ANDONG,SANGJU,MUNGYEONG,GYEONGSAN,GORYEONG' -PsanebCollectionReportLabel=CURRENT-GYEONGBUK-20261001-B -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs scripts/qa/attachment-collection-diagnostics.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git diff --check
```

응용 코드 불변으로 전체 Java 단위·bootJar는 이번 회차 재실행하지 않았다. Windows 신뢰 저장소를 사용하되 인증서·호스트 검증을 해제하지 않았다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다.

## 집계 경계와 다음 단계

과거 한 번 이상 실제 다운로드한 곳은210/223, 최초 미확인13개다. 공고별 최신 표본은209/223이며 미확보14개 중 포항1개는 과거 성공 후 재확보 대기다. 현재 코드 지문 성공42/223·미확인181개 및 엄격3표본 전체집합0/223은 별도다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 지자체 수 또는 현재 운영 재조회가 아니다.

다음은 아직 현재 코드 근거가 없는 성공 이력 지역의 묶음 재검증과, 남은 오류의 새로운 공식 응답/구조 근거 확인이다. 동일 실패의 무조건 반복·과거 성공으로 최신 오류 덮기·제목 정책 우회는 하지 않는다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용·전체 goal 완료는 수행하거나 선언하지 않았다.
