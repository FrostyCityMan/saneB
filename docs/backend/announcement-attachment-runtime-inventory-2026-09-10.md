# 첨부 수집 운영 범위: 읽기 전용 현황

## 기준과 한계

- 2026-09-10 서울 리전 saneB 배포 대상의 실행 중 서비스와 같은 DB에서 읽기 전용 집계했다. SSM 명령 `72208bc4-d9d0-4043-8be8-4e676ad302b4`는 Success, DB transaction 기본 read-only·statement timeout 10초·lock timeout 3초를 적용했다.
- 자격증명은 서버 내부 실행 환경에서만 사용했다. 응답에는 비밀값·공고 제목/본문·사용자 정보가 없다. 실제 서비스의 DB TLS 설정을 유지했으며 이 조회로 인증서 검증 수준 개선을 주장하지 않는다.
- Flyway V72. ACTIVE 기본 규칙 `ASCR-000001`. 이전 단일 파일 CLI QA의 DRAFT 규칙과 구분한다.
- 운영 첨부 정책 0건, 첨부 작업 0건. 따라서 상시 첨부 worker/ENFORCE가 운영에서 동작 중이라고 볼 근거가 없다.
- 저장 원문은 LOCAL_GOV_NOTICE/PRODUCTION 2,945건: ACCEPTED 후보 2,107건, REVIEW_REQUIRED 838건. 최종 자격 확정이나 운영 공고 활성 상태를 뜻하지 않는다.
- source link는 9건이며 후속 업무 보호 대상이다. 기존 데이터 처리 대상은 별도 고정 scope에서 현재 base/rule/보호 조건을 재검증한 뒤 확정해야 한다. 단순 차감 건수를 승인 범위로 사용하지 않는다.
- 분류 V2, 기본 수집 요청 배치, 지자체 스케줄, 지자체 상세 본문, 기존 본문 재분류 worker의 명시 환경 flag는 모두 true였다. flag만으로 개별 실행 성공을 판단하지 않는다.
- 실행 중 Java 프로세스 환경에서 기업마당 API key, 정부24 API key/base URL은 확인되지 않았다. 비밀값을 출력하거나 새로 주입하지 않았다. 시스템 property/기타 외부 설정 경로의 실제 Provider `isConfigured()` 결과까지 확인한 것은 아니므로 별도 확인이 필요하다.

## 지자체 목록 파서 현황

- 삭제되지 않은 수집원 244개: 활성 223개, 비활성 21개.
- 전체 목록 파서 코드 42종, 활성 수집원에 연결된 목록 파서 코드 41종. 이는 구현 클래스 수 또는 첨부 프로필 수가 아니다.
- 활성 수집원의 현재 상태: SUCCESS/NO_CHANGE/READY 합계 197개, 그 외 오류 상태 26개. 현재 저장된 최근 상태이며 이번 작업에서 전수 수집을 재실행한 결과는 아니다.
- 비활성 MANUAL_ONLY 13개는 QA/수동 보류 범주다. 활성 수집원의 최근 오류 26개와 혼동하지 않는다.
- 첨부 발견 프로필은 별도 exact host/path/응답 구조 검증이 필요하다. 아래 목록의 SUCCESS를 첨부 다운로드·추출 성공으로 사용하지 않는다.

| 목록 파서 코드 | 활성 수집원 | 비활성 수집원 | 활성 수집원의 현재 상태 |
|---|---:|---:|---|
| CHANGWON_GOSI_TABLE | 1 | 0 | SUCCESS 1 |
| CHUNCHEON_NOTICE_JSON | 1 | 0 | PARSER_UNSUPPORTED 1 |
| DAEJEON_EMINWON_AGGREGATOR | 1 | 0 | SUCCESS 1 |
| DAMYANG_NOTICE_JSON | 1 | 0 | PARSER_UNSUPPORTED 1 |
| DOBONG_NOTICE_TABLE | 1 | 0 | SUCCESS 1 |
| GUNWI_NOTICE_TABLE | 3 | 0 | SUCCESS 3 |
| HEURISTIC_NOTICE | 18 | 2 | ACCESS_BLOCKED 1, SUCCESS 17 |
| JUNGGU_NOTICE_TABLE | 1 | 0 | SUCCESS 1 |
| MANUAL_ONLY | 0 | 13 | 활성 없음 |
| MAPO_LEGAL_NOTICE_TABLE | 1 | 0 | SUCCESS 1 |
| NOWON_NOTICE_TABLE | 1 | 0 | SUCCESS 1 |
| SAEOL_GOSI | 43 | 1 | FAILED 6, NO_CHANGE 2, PARSER_UNSUPPORTED 1, SUCCESS 33, URL_ERROR 1 |
| SAFE_ANSAN_BBS | 1 | 0 | SUCCESS 1 |
| SAFE_DAEGU_LEGAL_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_DAEJEON_DATA_KEY_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_EGOV_DATA_LIST_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_EGOV_DETAIL_CELL | 3 | 0 | FAILED 1, SUCCESS 2 |
| SAFE_GORYEONG_BOARD | 1 | 0 | SUCCESS 1 |
| SAFE_GWANAK_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_GWANGJU_NAMGU_NOTICE | 1 | 0 | PARSER_UNSUPPORTED 1 |
| SAFE_GWANGMYEONG_LEGAL_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_GWD_BULLETIN | 1 | 0 | SUCCESS 1 |
| SAFE_HWASEONG_LEGAL_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_INCHEON_CITYNET_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_PAJU_SUMMARY | 1 | 0 | SUCCESS 1 |
| SAFE_PORTAL_SAEOL_BOARD_VIEW | 2 | 0 | FAILED 1, SUCCESS 1 |
| SAFE_SAEOL_EMINWON | 44 | 2 | FAILED 5, SUCCESS 39 |
| SAFE_SAEOL_EMINWON_CELL | 8 | 0 | FAILED 1, SUCCESS 7 |
| SAFE_SAEOL_EMINWON_COMPACT | 10 | 0 | SUCCESS 10 |
| SAFE_SAEOL_EMINWON_HREF | 1 | 0 | SUCCESS 1 |
| SAFE_SAEOL_EMINWON_LEGACY | 3 | 0 | SUCCESS 3 |
| SAFE_SAEOL_EMINWON_LIST | 1 | 0 | FAILED 1 |
| SAFE_SANGJU_GOSI | 1 | 0 | SUCCESS 1 |
| SAFE_SEODAEMUN_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_SEOUL_NOTICE | 1 | 0 | SUCCESS 1 |
| SAFE_YANGCHEON_SEOL | 1 | 0 | SUCCESS 1 |
| SAFE_YUSEONG_LEGAL_NOTICE | 1 | 0 | SUCCESS 1 |
| SCMS_CARD_NOTICE | 7 | 0 | SUCCESS 7 |
| SEONGBUK_EMINWON_TABLE | 1 | 0 | NO_CHANGE 1 |
| SPRING_BBS | 51 | 3 | ACCESS_BLOCKED 1, FAILED 3, NO_CHANGE 1, PARSER_UNSUPPORTED 1, SUCCESS 45 |
| SUBJECT_NOTICE_TABLE | 2 | 0 | SUCCESS 2 |
| YEONGCHEON_LEGAL_NOTICE | 1 | 0 | SUCCESS 1 |

## 다음 구현/검증

- [~] 실제 운영 원문이 있는 지자체를 첨부 연결의 우선 대상으로 둔다.
- [ ] source별 시스템 seed/상세 URL/다운로드 구조와 첨부 profile 매핑을 확인한다. 일반 목록 파서 코드를 곧바로 첨부 승인 코드로 취급하지 않는다.
- [ ] 공통 사이트 계열은 검증된 adapter를 공유하되 host/path와 QA 증거는 각각 고정한다.
- [ ] 지원 가능한 파일과 발견 실패/HTTP-only/POST-only/미지원 구조를 실제 확인하고 전체 범위 추적표에 남긴다. 미지원 상태를 임의로 완료 범위 밖으로 옮기지 않는다.
- [ ] 정책 게시·ENFORCE·기존 원문 적용 전 대상 ID·건수·최대 요청/bytes·보호 제외·복구 방법으로 실행 범위를 승인받는다.
- [ ] 실제 운영 URL과 역할별 브라우저 E2E를 검증한다. 현재 직접 확인된 공인 주소의 health HTTP 200은 로그인·화면 검증을 대신하지 않는다.
