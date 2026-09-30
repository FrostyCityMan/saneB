# 김해·창녕 본문·첨부 연결과 사천 후속 조사

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일을 우선 보존하고 발견·전송·파일 검증 오류는 분리한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 김해·창녕·사천 공식 목록·검색·지원 공고 상세 구조 확인.
- [x] 김해·창녕 공통 처리기1개·시스템 프로필2개·본문 선택기·카탈로그·회귀 테스트 구현.
- [x] Node23개 검사 통과.
- [x] Java288개 회귀·bootJar·두 지역 실제 첨부 관측.
- [x] 영수증309건·공고246개 지역 대장 재현 검증.
- [ ] 사천 세션 경로 첨부 연결.

## 조사 근거

과거 HTTP400 기록은 삭제하지 않는다. 이번 동일 공식 목록 조회에서는3기관 모두 HTTP200이며, 현재 성공 원인을 과거 요청과의 차이로 단정하지 않는다. 세 기관의 공개 검색 폼에서 `stype=title`, `sstring=소상공인`을 사용했다.

| 수집원 | 등록 목록 | 고정 표본 |
|---|---|---|
| 김해 LGS-000228 / SCMS_CARD_NOTICE | `https://www.gimhae.go.kr/03360/00023/00029.web` | 109947, 2026년 하반기 김해시 소상공인 육성자금 지원 계획 공고 |
| 창녕 LGS-000234 / HEURISTIC_NOTICE | `https://www.cng.go.kr/03517/01553.web` | 46407, 2026년 창녕군 소상공인 육성자금 지원계획 공고 |
| 사천 LGS-000227 / HEURISTIC_NOTICE | `https://www.sacheon.go.kr/news/00009/00014.web` | 2106170, 2026년 하반기 사천시 소상공인 육성자금 지원 공고 |

직접 구조 조사14GET(각 목록2회·검색1회·상세1회, 김해·사천 상세 구조 축약 확인 각1회 추가), 요청당12초, `saneB-attachment-collector/1.0`, TLS 검증·자동 redirect 금지를 유지했다. 원문 HTML 파일 저장0이다. 사이트 내 공식 검색 외 웹 검색·브라우저는 사용하지 않았다. 실제 Java 관측은 이 조사와 별도로 각 지역6요청·23MiB 상한이다.

첫 관측의 MIME 실패 후 상세2GET·파일 헤더/선두8byte2GET을 추가해 조사 합계18GET이다. 응답 헤더 확인은 전체 파일 다운로드 성공으로 집계하지 않는다. 김해는 `application/x-msdownload; charset=UTF-8`와 ZIP 선두 `50 4B 03 04`, 창녕은 `application/x-msdownload`와 OLE 선두 `D0 CF 11 E0 A1 B1 1A E1`이다. 창녕 파일명 헤더의 비ASCII 바이트도 확인했다. 따라서 두 프로필에만 기존 `LEGACY_BINARY_UTF8` 호환기를 적용하고 동일6요청·23MiB 상한으로 각1회 재관측한다. 최초 실패 JSON은 `-ATTEMPT1.json`으로 보존한다.

김해의 `form#saeolGosiVO`, 창녕의 `form#seolVO` 아래 `div.bbs1view1` 제목·본문·첨부를 분리한다. 두 기관 모두 같은 호스트의 `/DownloadEx.do?url=...&name=...` 공개 프록시가 실제 파일 링크다. 중첩 주소는 각 기관 `eminwon` 호스트의 `/emwp/jsp/ofr/FileDown.jsp`와 정확한 파일3필드로 제한하며 내부 HTTP 주소를 직접 호출하지 않는다.

김해 첨부 영역 안의 script는 실행하지 않고 미해석 발견 오류로 기록하면서 정상 파일 링크는 보존한다. 주석 처리된 직접 파일 URL은 사용하지 않는다. 창녕은 본문 밖 공공누리 제1유형 안내를 표시하며, 이를 첨부의 별도 이용 조건 전체 확인으로 확대하지 않는다.

사천은 `gcode=2017`, `idx=2106170` 상세에 `/board/download.do` 세션 경로와 파일명 query가 있고, 별도 바로보기·바로듣기 경로가 있다. 이번에는 구조만 확인하고 프로필 등록·다운로드 성공에 포함하지 않는다. 세션 값은 코드·문서·대장에 저장하지 않는다.

## 구현 경계

- `GyeongnamNextAttachmentDiscoveryProfile` 공통 처리기와 고정 시스템 bean2개를 추가한다. 기존 SCMS 지역 프로필·지문은 수정하지 않는다.
- 본문은 `div.substance`만 사용하고 부서·첨부·메뉴를 제외한다. 제목·영역 누락과 중복 구조는 오류로 분리한다.
- source/parser/URL hash 결합, HTTPS443, 정확한 호스트·경로·중첩 query, 이름 일치, 같은 요청 외 redirect 금지, 최대10파일을 유지한다.
- 미지원 형식·이름 불일치·미해석 링크와 정상 descriptor를 분리한다. 문서 역할 UNKNOWN, 파일명·확장자·MIME·서명 검증을 유지한다.
- 카탈로그2건은 참조용·expectation null이다. 키워드·공통 다운로드 엔진·DB/API·migration·추출기를 변경하지 않는다.

## 검증 결과

첫 검증은 Java287개·Node23개·bootJar 통과,3분46초·종료0이다. 김해 본문44자와 창녕 본문64자·각 첨부1개를 확인했으나 두 파일 모두 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 다운로드 검증에 실패했다. 최초 실패2건을 그대로 보존했다.

호환 처리 후 같은 검증 명령을 실행하여 Java288개 통과·실패/오류/생략0, bootJar 성공,3분33초·종료0이다. 명령은 `gradlew.bat --no-daemon :test`의 김해·창녕 계약/본문/카탈로그/snapshot/inventory/파일 형식/worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GYEONGNAM_NEXT -PsanebCollectionWindowsTrust=true`다.

| 관측 시각(KST) / 공고 | 본문 | 실제 파일 검증 | 남은 오류 |
|---|---|---|---|
| 2026-09-30 14:07:42 / GIMHAE-109947 | AVAILABLE44자 | HWPX148,680byte 성공 | ATTACHMENT_LINK_UNRESOLVED, 정상 파일은 보존 |
| 2026-09-30 14:07:57 / CHANGNYEONG-46407 | AVAILABLE64자 | HWP82,432byte 성공 | 없음 |

- 파일 SHA-256: 김해 `9502560bb56cb9da5ae02deb8a049830de8a0239302e6762ee2081dfc55788c6`, 창녕 `90ecff21599c316f4d7433bf462e43889716cdb9a0dba03bbceb75c68c687a0a`다. 실패 때 수신한 byte/hash와 같아도 최종 파일 검증 전에는 성공으로 집계하지 않았다.
- 재관측 예약은 각4/6회이며 byte 예약은 김해2,400,256·창녕2,228,224다. 최초 관측 포함 총16예약·9,256,960byte이며 직접 조사18GET과 별도다. 각 시도 원본 정리·운영 쓰기0을 확인했다.
- Node23개·영수증309건/최신246공고 재현 통과. 기존 지역 상태·근거·프로필 지문을 보존했다.
- 수집 가능 현황은 **168/223수집원(75.3%)·55잔여(미연결27+등록 다운로드 미확인28)**다. 오류가 있는 지역44개는 성공 지역과 겹칠 수 있으며 잔여55개에 더하지 않는다.
- 프로필197개(지역196+기업마당1), 카탈로그255공고/196대상/참조254+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로다. 첨부 추출·정책 승인·상시 유입·운영 E2E 완료를 뜻하지 않는다.

운영 DB/설정/worker/정책/ENFORCE/배치/배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신 및 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다.
