# 운영 대상 전체와 로컬 첨부 프로필 대조

## 판정과 근거 경계

2026-09-28 15:56:12 KST 운영 DB의 미삭제 지자체 전체244개를 읽기 전용으로 확인했다. 활성223개·비활성21개다. 현재 QA 코드의 실제 Spring 등록은19프로필(지자체18·기업마당1)이고 정부24 프로필은 없다. **정책 대상225개 중 연결19개·미등록206개**다. 연결은 수집/추출 성공이 아니며 전체 진행률로 환산하지 않는다.

| 비교 범위 | 전체 분모 | 코드 등록 연결 | 미등록 | 목록 파서 불일치 | 중복 연결 |
|---|---:|---:|---:|---:|---:|
| 활성 지자체 + 국가 Provider2 | 225 | 19 | 206 | 0 | 0 |
| 미삭제 지자체 + 국가 Provider2 | 246 | 19 | 227 | 0 | 0 |

비활성21개는 조회 결과에서 유지하되 현재 정책의 활성 분모에는 넣지 않는다. 기업마당·정부24는 시스템 정책 분모에 포함된다는 뜻이며 이번 감사에서 실제 운영 enabled/API key/호출 성공을 확인한 것은 아니다. 운영에 설치된 프로필 목록과 최신 로컬 프로필 목록도 동일하다고 주장하지 않는다.

## 검증 방법

- SSM `20fbb35a-601f-4cd6-a536-da44419e1132`, Success. 서울/root 및 저장소 계정 일치·SSM online·기존 운영 revision9a1bb45 확인 후 고정 SQL 실행.
- `BEGIN READ ONLY`, `default_transaction_read_only=on`, statement8초/lock2초·접속5초 상한·`ROLLBACK`, 쓰기0. 조회는 public_code/parser_profile_code/is_enabled 세 필드뿐이다. DB 접속정보·실제 사용자·공고 원문·URL은 영수증에 포함하지 않는다.
- 영수증 `build/qa-results/target-inventory-20260928-receipt.txt`, SHA256 `721d00e03c4823c00bc19a654d7ec6fabd0b4fbf79a787338879579e24ff5249`.
- `AttachmentProviderInventoryAuditTest`가 실제7종 Spring 등록 클래스/설정을 생성하고 기존 `AttachmentProviderQaPlan`으로 비교한다. 영수증 성공/rollback/쓰기0·행수·활성수·중복·형식을 검증한다. 단순 소스 문자열 검색으로 등록수를 세지 않는다.
- 산출물 `build/reports/attachment-target-inventory/operating-targets-vs-local-code.json`에는 전체246개 대상의 연결 상태와 catalog 참조 수를 보관한다. 운영 PK 대신 비교용 UUID를 내부에서만 만들며 산출물에 PK/URL/설정은 기록하지 않는다.
- 명시 실행 환경변수 `SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT=true`, `SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT=build/qa-results/target-inventory-20260928-receipt.txt`. 읽기 대상은 build/qa-results 하위 실제 경로로 제한한다. 기본 CI는 실제 영수증 대조를 생략하고 합성 회귀만 수행한다. 외부 사이트 요청0이다.
- 표적 Java9개 통과: 영수증 대조 포함 신규3개 + 기존 계획6개. 잘린 목록/문자열 boolean/중복 코드 거부, 비활성·미등록·파서 불일치 분리, 국가 Provider 활성 상태 미추정 검증.
- 명시 환경변수 없이 재실행한 신규 시험은 합성2개 통과/실제 영수증1개 조건부 생략으로 CI의 외부/운영 자동 조회가 없음을 확인했다. Node 대조로 문서39개 파서 행의205코드가 산출물과 정확히 일치하고 중복 없음을 확인했다. 시험용 Node/Gradle 프로세스는 종료했다. 제품 코드·빌드 설정이 변하지 않아 전체 root/bootJar를 이번 감사 때문에 다시 실행하지 않았다.

조회 SQL의 핵심은 다음과 같다. 접속 권한·서울 대상 확인과 읽기 전용 session/시간 상한은 실행 환경에서 별도 고정한다.

```sql
BEGIN READ ONLY;
-- 삭제된 기관은 제외하되 비활성 기관은 별도 분모로 보존한다.
SELECT public_code, parser_profile_code, is_enabled
FROM local_government_notice_sources
WHERE deleted_at IS NULL
ORDER BY public_code;
ROLLBACK;
```

## 등록됐지만 고정 참조가 없는5개

기업마당 `BIZINFO_DETAIL_V1`, 강북 `LOCAL_GANGBUK_LEGAL_GET_V1`, 부산 본청 `LOCAL_BUSAN_LEGAL_GET_V1`, 대전 서구 `LOCAL_DAEJEON_SEOGU_V1`, 화천 `LOCAL_HWACHEON_POST_V1`이다. 함안은1건만 보유한다. 나머지 참조 보유와 합쳐 catalog40참조/14개 프로필·저장 기대값1이다. 태백 기대값1은 정상 기대값 승인이 아니며 정상0 상태를 유지한다.

## 활성 미등록 지자체205개 분포

아래 숫자는 `LGS-` 뒤 여섯 자리다. 같은 목록 파서라는 이유로 첨부 엔진 호환/다운로드 성공을 추정하지 않는다. 정부24 미등록1개는 이 표와 별도로 합산한다.

| 현재 목록 파서 | 기관 수 | 기관 코드 |
|---|---:|---|
| SPRING_BBS | 46 | 000004·000007·000011·000047·000055·000057·000061·000064·000065·000067·000076·000095·000101·000103·000111·000124·000127·000132·000147·000148·000156·000157·000163·000164·000168·000178·000179·000181·000182·000187·000189·000190·000192·000193·000195·000196·000197·000198·000199·000202·000204·000210·000213·000214·000217·000219 |
| SAFE_SAEOL_EMINWON | 43 | 000021·000023·000030·000033·000035·000037·000042·000043·000048·000049·000050·000051·000056·000069·000072·000078·000081·000089·000109·000122·000128·000133·000134·000142·000143·000146·000152·000155·000159·000162·000165·000166·000167·000170·000172·000173·000174·000175·000184·000185·000188·000226·000244 |
| SAEOL_GOSI | 41 | 000006·000017·000018·000019·000020·000024·000025·000040·000046·000077·000082·000083·000091·000093·000097·000099·000100·000104·000105·000107·000108·000112·000119·000131·000135·000136·000141·000151·000177·000200·000201·000203·000205·000209·000215·000218·000220·000223·000230·000238·000243 |
| HEURISTIC_NOTICE | 13 | 000005·000059·000060·000062·000063·000176·000194·000212·000221·000227·000232·000234·000235 |
| SAFE_SAEOL_EMINWON_COMPACT | 10 | 000013·000028·000029·000032·000036·000038·000039·000041·000079·000153 |
| SCMS_CARD_NOTICE | 7 | 000031·000123·000225·000228·000236·000237·000240 |
| SAFE_SAEOL_EMINWON_CELL | 7 | 000086·000120·000144·000145·000158·000171·000241 |
| GUNWI_NOTICE_TABLE | 3 | 000053·000211·000222 |
| SAFE_EGOV_DETAIL_CELL | 3 | 000073·000149·000160 |
| SAFE_PORTAL_SAEOL_BOARD_VIEW | 2 | 000098·000106 |
| SUBJECT_NOTICE_TABLE | 2 | 000169·000206 |
| SAFE_SEOUL_NOTICE | 1 | 000001 |
| JUNGGU_NOTICE_TABLE | 1 | 000003 |
| SEONGBUK_EMINWON_TABLE | 1 | 000009 |
| NOWON_NOTICE_TABLE | 1 | 000012 |
| SAFE_SEODAEMUN_NOTICE | 1 | 000014 |
| MAPO_LEGAL_NOTICE_TABLE | 1 | 000015 |
| SAFE_YANGCHEON_SEOL | 1 | 000016 |
| SAFE_GWANAK_NOTICE | 1 | 000022 |
| SAFE_SAEOL_EMINWON_HREF | 1 | 000026 |
| SAFE_DAEGU_LEGAL_NOTICE | 1 | 000044 |
| SAFE_INCHEON_CITYNET_NOTICE | 1 | 000054 |
| SAFE_SAEOL_EMINWON_LIST | 1 | 000066 |
| SAFE_GWANGJU_NAMGU_NOTICE | 1 | 000068 |
| DAEJEON_EMINWON_AGGREGATOR | 1 | 000071 |
| SAFE_YUSEONG_LEGAL_NOTICE | 1 | 000075 |
| DOBONG_NOTICE_TABLE | 1 | 000080 |
| SAFE_HWASEONG_LEGAL_NOTICE | 1 | 000088 |
| SAFE_ANSAN_BBS | 1 | 000092 |
| SAFE_PAJU_SUMMARY | 1 | 000096 |
| SAFE_GWANGMYEONG_LEGAL_NOTICE | 1 | 000102 |
| SAFE_GWD_BULLETIN | 1 | 000116 |
| CHUNCHEON_NOTICE_JSON | 1 | 000117 |
| SAFE_EGOV_DATA_LIST_NOTICE | 1 | 000161 |
| DAMYANG_NOTICE_JSON | 1 | 000183 |
| YEONGCHEON_LEGAL_NOTICE | 1 | 000207 |
| SAFE_SANGJU_GOSI | 1 | 000208 |
| SAFE_GORYEONG_BOARD | 1 | 000216 |
| CHANGWON_GOSI_TABLE | 1 | 000224 |

## 다음 구현 순서와 Gate

1. 등록19개 중 참조 없는5개와 함안의 부족한2건을 확보한다. 수집 요청·byte 원장을 먼저 확인하고, 인증401/연결 차단을 우회하지 않는다.
2. 미등록 상위3목록 계열130개를 기관별 상세/첨부 경로로 나누어 구현한다. 기존 StandardBbs/SaeolGet/Chungju 계열의 재사용 가능성을 실제 구조로 확인하고 exact host/source/parser 결합·음성 사례·임시 DB/API 시험을 추가한다. 130기관을 하나의 새 프로필로 일괄 허용하지 않는다.
3. 별도 계열75지자체 및 정부24를 이어서 구현·검증한다. 키 부재/프로필 부재/사이트 실패/부분 추출을 서로 다른 blocker로 유지한다.
4. HWP 미확정 구간과 PDF 미검증 구조 해소, 검토된 정상 기대값, 정책 QA를 병행한다. 현재 PDF 구조 태그 요구를 임의로 낮추거나 참고용 양식을 긍정 근거로 승격하지 않는다.
5. 전체 ATT-001~062+SEG와9Gate를 유지한다. 운영 정책 게시·ENFORCE·기존 데이터 실행은 정확한 범위 승인 뒤 수행한다. 현재 브라우저는 명시 요청 정책상 미실행이다.

이번 증분은 전체 분모·구현 공백을 확정한 진척이며 첨부 지원 기관이 늘어난 것은 아니다. 운영 코드/DB/정책/worker 설정은 변경하지 않았다. 직전 코드72ec2b2의 Linux36387683529는 completed/success이며, 새 감사 회귀의 CI는 별도다.
