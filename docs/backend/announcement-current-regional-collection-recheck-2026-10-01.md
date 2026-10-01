# 최신 프로필의 지역별 첨부 재검증

## 후속 H: 영도 부분 재확보, 현재207/223·미확보16

2026-10-01 AWS 로그인 갱신 후 읽기 전용 Inventory로 서울 리전 프로젝트 계정 일치·Ubuntu 대상1대·SSM Online을 확인했다. 배포 이력은 `d-NCB2HF3YK`/`9a1bb4569bcc3c13bf3bc30b51021b9149c67054`로 이전과 같으며 이번 운영 설치·DB·정책·worker 변경은0이다. 이 조회는 현재 운영 기능 E2E의 증거가 아니다.

영도36435 공식 상세를 기존 두 수집기 식별자와 HTML/압축 헤더 조합으로 제한 진단했다. 3요청 모두200·본문119,913byte·공식 첨부영역1개·링크3개였다. TLS 검증 유지·브라우저 위장 없음·첨부 다운로드0·원문/쿠키 저장0이다. 헤더 차이만으로 과거 상세400을 재현하지 못했으므로 원인을 확정하거나 파서를 수정하지 않았다.

이어 기존 고정3공고와 동일 프로필로 수집기를1회 실행했다. **본문3건 AVAILABLE·발견3건 FOUND, 정상8파일675,329byte·파일400 오류3건**이다. 이전 상세400 이력은 보존한다.

| 고정 공고 | 정상 파일 | 실패 파일 | 전체 첨부 완료 |
|---|---:|---:|---|
| YEONGDO-36435 | 2 | 1·HTTP400 | 아니오 |
| YEONGDO-36164 | 3 | 1·HTTP400 | 아니오 |
| YEONGDO-35633 | 3 | 1·HTTP400 | 아니오 |

관측 JUnit3개 실패/생략0·Gradle41초 성공은 부분 파일 오류를 기록하는 collection-only 검증의 성공이다. 파일11개 모두 성공 또는 추출/정책QA 통과를 뜻하지 않는다. 보고서는 `build/reports/attachment-regional-collection/YEONGDO-<ID>-YEONGDO-TRANSPORT-20261001-H.json`, JUnit은 `build/qa-yeongdo-recovery-20261001-h/`에 보존했다.

기존956영수증을 보존하고3개 추가하여 **959영수증/293표본**을 재현했다. 다른290표본 불변·프로필/인벤토리/생산클래스 hash 일치를 확인했다. 현재 파일 확보 **207/223(92.8%)·미확보16=최초13+포항/괴산/합천 재확보3**이다. 영도는 부분 성공과 남은 파일 오류를 함께 유지한다. 엄격3적격표본·전체 첨부 집합은16/223으로 변함없다. 수집처 분모는2026-09-28 스냅샷이며 고유 지자체 수·상시 운영 성공률·전체goal 완료율이 아니다.

실행 명령: `.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation -PsanebCollectionWindowsTrust=true -PsanebBbsObservationGroup=YEONGDO -PsanebCollectionReportLabel=YEONGDO-TRANSPORT-20261001-H`. 기존 예산은21요청·69MiB, 실제 예약은20회·7,335,425byte이며 별도 상세진단3요청·최대3MiB를 추가했다. 예약값은 본문 상한을 포함하며 wire 실측과 구분한다. 원본정리true·운영쓰기0·추출/정책QA/기대값승인false다.

Node 수집/영수증/가용성/진단/GitHub영수증49개 실패/생략0·대장959개 재현·`git diff --check` 통과다. 응용 코드 불변으로 전체 Java 단위/bootJar는 재실행하지 않았다. 최신 응용 코드SHA `5220a85ca924e6577d071de2c8c11043d478b92d`의 Linux CI36829764848은 기록 시 실행 중이며 성공으로 보고하지 않는다. 응용 코드·DB/API·정책·HWP 고도화 변경 없이 수집 증거만 추가했다. 브라우저는 사용자 정책상 미실행, 전체goal 미완료다.

## 후속 G: 부평·남동 유효 참조 재확보, 현재206/223·미확보17

[부평·남동 공식 목록과 새 고정 공고](announcement-incheon-reference-recovery-2026-10-01.md)에서 본문과 HWP/HWPX4파일326,957byte를 확보했다. 기존 부평50550 상세404·남동71702 목록redirect 실패 이력은 보존하고 새49419·69607을 참조 전용으로 추가했다. 파서·규칙·운영 설정은 변경하지 않았다. 현재 파일 확인 **204→206/223·미확보19→17=최초13+포항/영도/괴산/합천 재확보4**다.956영수증/293표본 재현·실제2관측통과·Node49통과, 선택Java146개 중 첫144통과와 수정2개 재통과·bootJar 생성/최종명령 성공. 전체CI·운영E2E 검증은 아니다. 아래F의19곳은 이 복구 이전 시점 기록이다.

## 후속 F 묶음: 잔여 과거 성공53곳 재검증, 현재204/223·미확보19

기준 HEAD `b2f9610f685b3338dd24d88e504526e097c50456`. 현재 코드 미확인71곳 중 과거 최신 표본에 정상 파일이 있던53곳의72고정 공고를8묶음으로 관측했다. **52곳·63공고·정상76파일·12,518,983byte**를 확보했다. 합천은 정상 파일0개로 재확보 대기에 추가한다. 응용 코드·프로필·정책·운영 설정 변경은 없다.

| 묶음 | 확보 / 수집처 | 공고 | 정상 파일 / 바이트 | Gradle 결과 |
|---|---:|---:|---:|---|
| 남부1 | 8 / 8 | 8 | 8 / 951,241 | 8통과·43초 |
| 남부2·연제 | 6 / 6 | 7 | 7 / 1,335,079 | 7통과·44초 |
| 경남1·충남도 | 8 / 8 | 8 | 9 / 1,005,121 | 8통과·56초 |
| 경남2·경북권 | 6 / 7 | 7 | 7 / 1,124,156 | 7통과·56초 |
| 광역권 | 8 / 8 | 12 | 16 / 3,485,048 | 12통과·64초 |
| 수도권·천안 | 5 / 5 | 7 | 6 / 849,166 | 6통과/1실패·37초 |
| 강원 | 8 / 8 | 14 | 17 / 2,716,935 | 14통과·66초 |
| 충주·보은·옥천 | 3 / 3 | 9 | 6 / 1,052,237 | 9통과·41초 |

전체72관측은 **71통과/1실패·생략0**, 순차 실행기 최종 종료1이다. 양평312241은 상세 제목 확인 `OBSERVATION_ASSERTION_FAILED`로 실패했다. 파일을0개 확보한 합천도 개별 오류를 보존하는 collection-only 검사는 통과하므로 테스트 통과와 다운로드 성공을 분리한다.

- `HAPCHEON-44432`: 발견 경고 `ATTACHMENT_LINK_UNRESOLVED`, 파일 `TRANSPORT_TIMEOUT`·`ATTACHMENT_FORMAT_MISMATCH`, 정상0개. 파일 종류나 전송 제한을 완화하지 않았다.
- 연제43358은 형식 불일치지만 별도42552의 정상 파일을 보존했다. 유성49380도 정상1개와 파일 시간 초과를 함께 보존했다.
- 양평311846은 정상1개·미지원1개,312241은 제목 확인 실패,311507은 제목 제외·요청0이다.
- 춘천 본문TIMEOUT·곡성 본문HTTP 오류·광주 남구 본문호스트 오류는 정상 첨부와 별개다. 울진·진주·김해 발견 경고도 유지했다.
- 제목 중단6건은 양평311507·영월156846/157529·충주72039/72625·옥천193187이며 모두 요청0이다. 파일 확보가 없는 나머지3건은 연제43358·합천44432·양평312241이다.

현재 프로필 파일 확인은 **152→204/223(91.5%)·미확인19**이다. 과거 확보210/223·최초 미확인13은 유지한다. 최신 표본 성공204/223·미확보19와 현재 코드 성공 수가 같아졌지만 운영 완료를 뜻하지 않는다. 엄격3적격표본·전체 첨부 집합은 **11→16/223**으로 달서·달성·횡성·영월·보은5곳이 추가됐다. 분모는2026-09-28 활성 수집원 스냅샷223이며 전체goal 완료율이나 고유 지자체 수가 아니다.

남은19곳은 다음과 같다.

- 최초 미확인13: 은평·서대문·울산 남구·성남·평택·이천·포천·강릉·속초·철원·영동·공주·의성.
- 재확보6: 포항·영도·부평·남동·괴산·합천.
- 울산 남구는 프로필 미연결1곳이다. 다른18곳의 전송/TLS/상세/파일 오류를 미등록과 혼동하지 않는다. 같은 조건 재시도 없이 새 URL 구조·공식 표본·전송 근거가 있는 항목부터 대응한다.

기존882영수증 보존·72개 추가로 **954영수증/291표본**을 재현했다. 다른219표본 불변·현재 성공152곳과 새 대상53곳 비중복·profile/inventory/생산 클래스 hash 일치를 확인했다. 모든 새 원본정리true·운영쓰기0·추출/정책QA/기대값 승인false다. 두 JSON의 `currentRegionalRecheck20261001F`에 메타데이터를 저장했다.

이번에는 실행 전 실제 Java 카탈로그와 예산 함수만 HTTP 없이 호출하여 `build/qa-tools/current-regional-20261001-f-plan.json`을 생성했다. 계획 hash는 `b797c821b45b5c0ae913fa0538c47979939fd78a81bb7b4a4b58eb75e86df0c9`이며72표본 각각의 원래 프로필·예산과 실제 결과가 일치했다. 설정 상한 **767요청·2,332MiB**, 실제 예약279회·165,911,228byte다. 예약값은 본문 상한을 포함하며 실제 wire와 구분한다. B~F 합계 설정 상한1,990요청·6,571MiB, 이전 이력도 보존한다. 추가 진단·자동 반복0, 원본/임시 파일 정리를 확인했다.

공통 명령은 `.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation -PsanebCollectionWindowsTrust=true -PsanebBbsObservationGroup=<그룹> -PsanebCollectionReportLabel=<라벨>`이다. 단일384MiB JVM 순차 실행·Windows 신뢰 저장소 사용·TLS 검증 유지다. 아래 각 그룹1회, 라벨 `CURRENT-<묶음>-20261001-F`를 사용했다.

| 묶음 | 그룹 |
|---|---|
| `SOUTH-FIRST` | `MOKPO,YEOSU,NAJU,GWANGYANG,GOKSEONG,HWASUN,JANGHEUNG,GANGJIN` |
| `SOUTH-SECOND` | `MUAN,WANDO,JINDO,SHINAN,YEONJE_GURYE,YEONJE_FIRST_HALF` |
| `GYEONGNAM-FIRST` | `PROVINCE_NEXT,JINJU,SACHEON,GIMHAE,GEOJE,UIRYEONG,CHANGNYEONG` |
| `GYEONGNAM-SECOND` | `HADONG,SANCHEONG,GEOCHANG_SUPPORT,HAPCHEON,GYEONGBUK_PROVINCE,YEONGDEOK,ULJIN` |
| `METRO` | `BUSAN_CITY_SUPPORT,DALSEO,DALSEONG,GWANGJU_NAMGU,DAEJEON_AGGREGATOR,YUSEONG_SUPPORT,ULSAN_DONGGU_CARD,ULJU` |
| `CAPITAL` | `ONGJIN,HWASEONG,OSAN,YANGPYEONG,CHEONAN_SUPPORT` |
| `GANGWON` | `GANGWON_PROVINCE,CHUNCHEON,TAEBAEK,SAMCHEOK,HOENGSEONG,YEONGWOL,PYEONGCHANG,YANGYANG` |
| `CHUNGBUK` | `CHUNGJU,BOEUN,OKCHEON` |

JUnit은 `build/qa-current-regional-20261001-f/<묶음 소문자>`에 보존했다. Node49개 실패/생략0·대장954개 재현·공백검사 통과다. 전체 Java 단위/bootJar는 코드 불변으로 재실행하지 않았다. HWP 고도화 보류·운영 DB/worker/정책/배포 변경0·브라우저 정책상 미실행·전체goal 미완료다. 다음 Gate는19곳의 미확보/연결 문제와 전체 worker·DB/API/UI 동작을 각각 증명하는 것이다. 아래 수치는 과거 시점 기록이다.

## 후속 E 묶음: 37곳 중34곳 확보, 현재 지문152/223

기준 HEAD `bb760d5d11f01e83926906296a17554300d19d99`. 서울·인천권·전북·충청37수집처의39고정 공고를5묶음으로 순차 관측하여 **34곳·35공고·정상54파일·6,516,679byte**를 확보했다. 응용 코드·프로필·규칙·운영 설정은 변경하지 않았다. 성공/오류를 함께 기록하며 파일 확보가 없는 수집처를 성공으로 처리하지 않는다.

| 묶음 | 수집처 확보 / 대상 | 공고 | 정상 파일 / 바이트 | 관측 결과 |
|---|---:|---:|---:|---|
| 서울: 성동·송파·광진·동대문·성북·영등포·양천·관악 | 8 / 8 | 8 | 19 / 3,089,390 | 8통과·종료0·41초 |
| 수도권: 마포·서울강서·강동·부평·동작·남동·검단 | 5 / 7 | 7 | 7 / 527,821 | 5통과/2실패·종료1·37초 |
| 전북: 김제·완주·진안·무주·장수·임실·순창 | 7 / 7 | 7 | 8 / 661,632 | 7통과·종료0·33초 |
| 충남: 서산·논산·당진·서천·청양·홍성·예산·태안 | 8 / 8 | 8 | 12 / 1,285,170 | 8통과·종료0·40초 |
| 충북: 청주·제천·증평·진천·괴산·음성·단양 | 6 / 7 | 9 | 8 / 952,666 | 9통과·종료0·38초 |

전체 관측39건은 **37통과/2실패·생략0**, 순차 실행기 최종 종료1이다. 이 테스트는 개별 파일 실패의 보존도 검증하므로 통과 수가 다운로드 성공 수는 아니다. 괴산은 파일 HTTP400을 보존하여 테스트가 통과했지만 정상 파일은0개다. 제천3공고 중1개는 제목 중단·요청0이고 나머지2공고에서3파일을 확보했다. 제천의 두 번째 적격 공고를 추가 지역으로 중복 집계하지 않았다.

- `BUPYEONG-50550`: 본문 `HTTP_STATUS_ERROR`, 상세 `ATTACHMENT_HTTP_404`, 파일0.
- `NAMDONG-71702`: 본문 `REDIRECT_LIMIT_EXCEEDED`, 상세 `ATTACHMENT_PATH_NOT_APPROVED`, 파일0. 경로 허용이나 redirect 제한을 완화하지 않았다.
- `GOESAN-29655`: 본문/발견 후 파일 `ATTACHMENT_HTTP_400`, 파일0.
- 김제·순창은 각각 본문 redirect 한도/HTTP 오류와 정상 파일을 함께 보존했다. 동대문·강동·당진의 발견 경고, 송파 상세 제목 경고, 영등포·장수 미지원 파일도 유지했다.
- 집계 도구에서 본문 `REDIRECT_LIMIT_EXCEEDED`가 `UNCLASSIFIED_ERROR`로 축약되던 문제를 수정했다. 본문 단계에서만 고정 코드를 허용하며 파일 단계 오용·임의 URL 유출을 거부하는 회귀2개를 추가했다. 실제 수집기/API 계약 변경은 없다.

현재 프로필 파일 확인 **118→152/223·미확인71**이다. 과거 확보210/223·최초 미확인13은 유지하고, 최신 표본은 **205/223·미확보18=최초13+포항·영도·부평·남동·괴산 재확보5**다. 엄격3적격표본·전체 첨부 집합11/223은 유지한다. 분모는2026-09-28 활성 수집원 스냅샷223이며 고유 지자체 수나 현재 운영 성공률·전체goal 완료율이 아니다.

기존843영수증을 보존하고39개를 추가해 **882영수증/291표본**을 재현했다. 기존 다른252표본 불변·새 대상37곳과 기존 현재 성공118곳 비중복·profile/inventory/생산 클래스 hash 일치를 검증했다. 모든 신규 원본 정리true·운영쓰기0·추출/정책QA/기대값 승인false다. 두 JSON에 `currentRegionalRecheck20261001E`를 기록했다. 생산 클래스·인벤토리 hash는 D/B/C와 같다.

초기 상한 안내226요청·854MiB는 제천 그룹의 추가2표본을 누락했으므로 **238요청·900MiB**로 정정했다. 표본별 기존 한도는 바꾸지 않았다. 실제 예약175회·94,403,636byte는 본문 예약 상한을 포함하며 실제 wire와 구분한다. B/C/D/E 합계 설정 상한1,223요청·4,239MiB, 그 이전 이력도 보존한다. 추가 진단·자동 반복0이며 Gradle은 `--no-daemon --max-workers=1`·관측 JVM384MiB로 순차 종료했다.

실행 명령은 아래 그룹별 `.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation -PsanebCollectionWindowsTrust=true -PsanebBbsObservationGroup=<그룹> -PsanebCollectionReportLabel=<라벨>`이다. Windows 신뢰 저장소를 사용하고 TLS/호스트 검증을 유지했다.

| 그룹 | 라벨 |
|---|---|
| `SEONGDONG,SONGPA,GWANGJIN,DONGDAEMUN,SEONGBUK,YEONGDEUNGPO,YANGCHEON,GWANAK` | `CURRENT-SEOUL-20261001-E` |
| `MAPO,SEOUL_GANGSEO,GANGDONG,METRO_REMAINDER,NAMDONG,GEOMDAN` | `CURRENT-CAPITAL-20261001-E` |
| `GIMJE,WANJU,JINAN,MUJU,JANGSU,IMSIL,SUNCHANG` | `CURRENT-JEONBUK-20261001-E` |
| `SEOSAN,NONSAN,DANGJIN,SEOCHEON,CHEONGYANG,HONGSEONG,YESAN,TAEAN` | `CURRENT-CHUNGNAM-20261001-E` |
| `CHEONGJU,JECHEON,JEUNGPYEONG,JINCHEON,GOESAN,EUMSEONG,DANYANG` | `CURRENT-CHUNGBUK-20261001-E` |

JUnit XML은 `build/qa-current-regional-20261001-e/{seoul,capital,jeonbuk,chungnam,chungbuk}`에 보존했다. Node 수집/영수증/가용성/진단45개와 GitHub 영수증4개, 총49개 실패/생략0이며 `node scripts/qa/verify-collection-receipt-index.mjs`와 `git diff --check`도 통과했다. 전체 Java 단위·bootJar는 응용 코드 불변으로 재실행하지 않았다. 운영 DB·정책 게시·ENFORCE·기존 데이터 적용·배포0, HWP 고도화 보류·브라우저 정책상 미실행·전체goal 미완료다. 다음은 현재 코드 미확인71곳의 검증과 별도 오류 원인별 대응이다. 아래 수치는 과거 시점 기록이다.

## 후속 D 묶음: 부산·남부·서울32곳 확보, 현재 지문118/223

기준 HEAD `76198becb1b751482794137a10af8d2a3666c10a`. 현재 코드로 확인하지 않은33수집처·66고정 공고를5묶음으로 순차 관측하여 **32곳·56공고·정상93파일·17,030,260byte**를 확보했다. 금산·강남은 기존 실패 표본 대신 이미 카탈로그에 등록된 유효 지원 공고 `GEUMSAN_SUPPORT`·`GANGNAM_SUPPORT`를 사용했다. 응용 코드·프로필·규칙·운영 설정은 변경하지 않았다.

| 묶음 | 수집처 확보 / 대상 | 공고 | 정상 파일 / 바이트 | 관측 결과 |
|---|---:|---:|---:|---|
| 부산1: 중구·서구·동구·영도·부산진·동래·남구 | 6 / 7 | 23 | 25 / 2,772,488 | 20통과/3실패·종료1·61초 |
| 부산2: 북구·해운대·사하·금정·강서·수영·사상·기장 | 8 / 8 | 22 | 31 / 5,935,624 | 22통과·종료0·53초 |
| 남부: 보성·함평·장성·영암·담양·영광·통영 | 7 / 7 | 8 | 7 / 853,480 | 8통과·종료0·47초 |
| 혼합: 함안·제주시·서귀포·울릉·금산 | 5 / 5 | 5 | 7 / 1,242,013 | 5통과·종료0·38초 |
| 서울: 강북·도봉·노원·구로·금천·강남 | 6 / 6 | 8 | 23 / 6,226,655 | 8통과·종료0·47초 |

전체66관측은 **63통과/3실패·생략0**이며 순차 실행기 최종 종료1이다. 앞선 부산1 실패를 뒤 묶음 성공으로 지우지 않았다. 정상 파일 확보가 없는10공고는 제목 중단3·공식 첨부 없음1·형식 검증 실패3·상세 실패3으로 구분된다.

- `YEONGDO-36435/36164/35633`: `DETAIL_DISCOVERY/ATTACHMENT_HTTP_400`, 파일0. 36164는 본문도 `FETCH_FAILED/HTTP_STATUS_ERROR`, 나머지2건은 본문 AVAILABLE이다. 과거 성공을 보존하되 현재3표본 실패로 재확보 대기에 추가한다. 사이트 전체 장애나 삭제 원인은 미확정이며 자동 반복하지 않았다.
- `BSBUKGU-35116`: `ATTACHMENT_SIGNATURE_UNSUPPORTED`, `HAEUNDAE-53342/53828`: `ATTACHMENT_FORMAT_MISMATCH` 유지. 수신 바이트나 확장자만으로 성공 처리하지 않았다.
- `SASANG-40426`: 정상2파일·미지원1항목, `TONGYEONG-49251`: 정상1파일·`ATTACHMENT_LINK_UNRESOLVED`를 각각 병행 보존했다.
- `NOWON-20260824152519260`: 정상3파일과 본문 `FETCH_FAILED/BODY_TEXT_EMPTY`를 분리했다. 파일 성공을 전체 분석 완료로 승격하지 않았다.
- 부산진47592/50698·영광29278은 제목 단계에서 요청0으로 중단했다. 사하43993은 공식 첨부 없음 확인이며 다운로드 성공이 아니다.

현재 코드 파일 확인은 **86→118/223·미확인105**, 엄격3적격표본·전체 첨부 집합 기준은 **0→11/223**이다. 해당11곳은 구로·부산 중구·서구·동구·부산진·동래·남구·사하·강서·수영·기장이다. 이것은 수집 단계에만 해당하며 정책 QA·추출·상시 worker·운영 E2E Gate 통과가 아니다. 과거 파일 확보210/223·최초 미확인13개는 유지하되 최신 표본은208/223·미확보15개(최초13+포항·영도 재확보2)다. 분모는 기존 활성 수집원 스냅샷223이며 현재 운영 성공률이 아니다.

기존777영수증 보존·66개 추가로 **843영수증/289표본**을 재현했다. 다른223표본 불변, 대상33개가 기존 현재 성공86개와 겹치지 않음, 모든 신규 profile hash·원본 정리true·운영쓰기0·추출/정책QA/기대값 승인false를 확인했다. 생산 클래스·인벤토리 hash는 아래 B/C와 같다. 두 JSON 대장에 `currentRegionalRecheck20261001D`로 기록했다. Node43검사 및 공백 검사 통과다. 전체 Java 단위·bootJar는 코드 불변으로 재실행하지 않았다.

이번 상한544요청·1,741MiB, 영수증 예약300회·158,335,520byte다. 예약은 본문 상한을 포함하며 실제 wire 사용량과 구분한다. B/C/D 합계 상한985요청·3,339MiB이며 그 이전 이력도 보존한다. 추가 진단·자동 재시도0이다. 모든 Gradle은 단일 worker·384MiB 관측 JVM·`--no-daemon`으로 순차 실행했고, XML은 `build/qa-current-regional-20261001-d/{busan-first,busan-second,south,mixed,seoul}`에 각각 보존했다.

공통 Gradle 명령과 Windows 신뢰 저장소 선택은 C 묶음과 같다. 아래 그룹/라벨을 각1회 실행했다.

| 그룹 | 보고서 라벨 |
|---|---|
| `BSJUNGGU,BSSEOGU,BSDONGGU,YEONGDO,BUSANJIN,DONGNAE,NAMGU` | `CURRENT-BUSAN-FIRST-20261001-D` |
| `BSBUKGU,HAEUNDAE,SAHA,GEUMJEONG,BSGANGSEO,SUYEONG,SASANG,GIJANG` | `CURRENT-BUSAN-SECOND-20261001-D` |
| `BOSEONG,HAMPYEONG,JANGSEONG,YEONGAM,DAMYANG,YEONGGWANG,TONGYEONG` | `CURRENT-SOUTH-20261001-D` |
| `HAMAN,JEJUSI,SEOGWIPO,ULLEUNG,GEUMSAN_SUPPORT` | `CURRENT-MIXED-20261001-D` |
| `GANGBUK,DOBONG,NOWON_SUPPORT,GURO,GEUMCHEON,GANGNAM_SUPPORT` | `CURRENT-SEOUL-20261001-D` |

운영 쓰기·배포·정책 게시·ENFORCE·기존 데이터 적용0, HWP 고도화 보류·브라우저 정책상 미실행·전체goal 미완료를 유지한다. 다음은 남은 현재 코드 미확인 지역의 묶음 검증과 새로운 근거가 있는 오류 해결이다. 아래 C/B 수치는 각 과거 시점 기록이다.

## 후속 C 묶음: 44수집처·정상56파일, 현재 지문86/223

기준 HEAD `ca7735aec387c631a11b9271aa0983828a4c67d4`. 기존 성공 이력은 있지만 현재 프로필 지문 근거가 없는44수집처·45고정 공고를6묶음으로 순차 실행했다. 새 코드·프로필·정책·카탈로그 변경 없이 **44곳 모두에서 정상56파일·12,945,322byte**를 확보했다. 정선의 별도 제목 부적격 표본1개는 요청0·파일0으로 중단했다.

| 묶음 | 수집처 / 공고 | 정상 파일 / 바이트 | 관측 결과 |
|---|---:|---:|---|
| 경기: 남양주·김포·의정부·경기광주·하남·양주·군포 | 7 / 7 | 8 / 692,308 | 7통과·43초 |
| 전북: 전북도·전주·군산·익산·정읍·남원·부안·고창 | 8 / 8 | 8 / 936,091 | 8통과·44초 |
| 경북: 김천·구미·영천·청송·영양·청도·칠곡 | 7 / 7 | 9 / 737,919 | 7통과·42초 |
| 인천권·대덕: 인천시·제물포·계양·강화·서해·대덕 | 6 / 6 | 13 / 6,532,916 | 6통과·38초 |
| 강원·대전 서구: 원주·동해·홍천·화천·인제·고성·정선·대전 서구 | 8 / 9 | 10 / 3,412,514 | 제목 중단 포함9통과·43초 |
| 대구권·대전 중구: 대구시·동구·서구·남구·북구·수성·군위·대전 중구 | 8 / 8 | 8 / 633,574 | 8통과·36초 |

45관측 실패/생략0·6개 Gradle 명령 및 순차 실행기 종료0다. 단, `CHEONGSONG-22287`은 정상 파일2개와 `ATTACHMENT_LINK_UNRESOLVED` 경고가 함께 있어 부분 성공이다. `JEONGSEON-37876`은 `TITLE_NOT_ELIGIBLE_NOT_FETCHED`이며 같은 수집처의 적격34952 파일2개 성공과 분리한다. 나머지43적격 표본은 해당 파일 집합 수집 완료지만 지역별3공고·첨부 텍스트 분석·운영 승인을 의미하지 않는다.

현재 프로필 파일 확인은 **42→86/223·미확인137**이다. 과거 성공210/223·최초 미확인13, 최신 표본209/223·미확보14(최초13+포항 재확보1)는 그대로다. 등록222/223·울산 남구 미연결1, 엄격3표본 전체집합0/223도 유지한다. 포항·안성 등 이전 오류를 이번 관측으로 복구했다고 표시하지 않았다. 분모223은 기존 활성 수집원 스냅샷이다.

기존732영수증을 보존하고45개를 추가해 **777영수증/289표본**을 재현했다. 다른244개 표본 불변·새 관측 대상44개가 기존 현재 성공42개와 겹치지 않음·모든 새 profile hash/inventory hash/producer class hash 일치를 검증했다. 인벤토리와 생산 클래스 hash는 아래 B 묶음과 같다. metadata는 두 JSON 대장의 `currentRegionalRecheck20261001C`에 기록했다. 관련 Node43개·공백 검사 통과, 전체 Java 단위/bootJar 재실행 없음이다.

이번 상한은 **309요청·1,092MiB**(화천44요청/80MiB, 계양7요청/23MiB, 나머지43표본 각6요청/23MiB)다. 실제 예약189회·113,786,259byte는 본문 상한을 포함하며 wire 사용량과 구분한다. B+C 합계 상한441요청·1,598MiB이며 이전 모든 관측 이력도 대장에 남긴다. 추가 진단·자동 재시도0, 원본 정리45건 모두true·운영쓰기0·추출/정책QA/기대값 승인false다. 각 실행은 `--no-daemon --max-workers=1`, 단일384MiB 관측 JVM이며 종료 후 다음 묶음으로 넘어갔다. JUnit XML은 `build/qa-current-regional-20261001-c/{capital,jeonbuk,gyeongbuk,incheon,gangwon,daegu}`에 보존했다.

공통 명령은 `.\gradlew.bat --no-daemon --max-workers=1 :attachmentRegionalCollectionObservation -PsanebCollectionWindowsTrust=true`이고, 아래 그룹과 라벨을 각1회 지정했다. 인자 `-PsanebBbsObservationGroup=`와 `-PsanebCollectionReportLabel=`을 사용했다.

| 그룹 | 보고서 라벨 |
|---|---|
| `NAMYANGJU,GIMPO,UIJEONGBU,GG_GWANGJU,HANAM,YANGJU,GUNPO` | `CURRENT-CAPITAL-20261001-C` |
| `JEONBUK,JEONJU,GUNSAN,IKSAN,JEONGEUP,NAMWON,BUAN,GOCHANG` | `CURRENT-JEONBUK-20261001-C` |
| `GIMCHEON,GUMI,YEONGCHEON,CHEONGSONG,YEONGYANG,CHEONGDO,CHILGOK` | `CURRENT-GYEONGBUK-20261001-C` |
| `INCHEON_CITY,JEMULPO,GYEYANG,GANGHWA,SEOHAE,DAEDEOK` | `CURRENT-INCHEON-20261001-C` |
| `EXISTING_SECOND,DONGHAE,HONGCHEON,HWACHEON,INJE,GW_GOSEONG,JEONGSEON` | `CURRENT-GANGWON-20261001-C` |
| `DAEGU_CITY,DAEGU_DONGGU,DAEGU_SEOGU,DAEGU_NAMGU,DAEGU_BUKGU,SUSEONG,GUNWI,DAEJEON_JUNGGU` | `CURRENT-DAEGU-20261001-C` |

운영 DB·worker·배포·정책 게시·ENFORCE·기존 데이터 적용 변경0, HWP 고도화 보류·브라우저 정책상 미실행·전체goal 미완료를 유지한다. 아래 B 묶음의42/223 등은 이전 시점 기록이다.

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
