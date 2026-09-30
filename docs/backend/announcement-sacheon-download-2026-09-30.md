# 사천 본문·세션 경로 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결 단계다. 정상 파일은 수집하고 개별 오류는 분리한다. HWP 추출기 추가 개선은 보류하며 전체 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E goal은 미완료다.

- [x] 사천 공식 지원 공고·첨부 경로 확인.
- [x] 본문 선택기·세션 요청 한정 첨부 프로필·회귀 테스트·카탈로그 구현.
- [x] Node23개 검사 통과.
- [x] Java290개 회귀·bootJar·실제 HWPX 파일 관측.
- [x] 영수증310건·최신247공고 수집 대장 재현 검증.

## 근거와 구현

수집원은 `LGS-000227 / HEURISTIC_NOTICE`, 목록은 `https://www.sacheon.go.kr/news/00009/00014.web`이다. 고정 표본은 [2026년 하반기 사천시 소상공인 육성자금 지원 공고](https://www.sacheon.go.kr/news/00009/00014.web?gcode=2017&idx=2106170&amode=view)다. 목록·검색·상세 구조 조사는 앞선 김해·창녕 작업에 기록했고 이번에는 상세 GET1회·파일 헤더 및 선두8byte GET1회를 추가했다.

첨부는 같은 호스트 `/board/download.do`의 `gcode=2017`, `name` query이며, 공식 HTML의 선택적 세션 경로를 전송 요청에만 사용한다. 해당 값은 locator·파일 식별 hash·문서·대장에 저장하지 않는다. 세션이 바뀌어도 같은 공고·파일의 locator는 동일하고, 실행 중 다른 세션/파일로의 redirect는 허용하지 않는다.

- `SacheonNoticePage`: `div.bbs1view1 > h1.h1#sns_bbs_title`, `div.substance > div.substanceautolink`를 분리한다. 부서·파일명·인쇄·스크립트는 본문이 아니다.
- `SacheonAttachmentDiscoveryProfile`: 공식 `div.attach1`의 파일명 링크와 같은 항목의 다운로드 버튼을 대조·중복 제거한다. 이름이나 링크가 다르면 오류를 남긴다.
- `/sn3hcv_convert.jsp`의 같은 파일 바로보기·`tts=1` 바로듣기는 요청하지 않는다. 알 수 없는 링크·제어 요소는 오류로 분리하고 정상 descriptor는 유지한다.
- HTTPS443·정확한 기관/게시판/source hash·최대10파일·UNKNOWN 역할·미지원 형식 분리·파일명/서명 검증을 유지한다.
- 헤더 조사에서 HTTP200, `application/x-msdownload`, Content-Length89,353, ZIP 선두 `50 4B 03 04`, 비ASCII 파일명 헤더를 확인했다. 사천에만 기존 legacy MIME·UTF-8 파일명 호환 옵션을 적용하며 공통 검증기를 바꾸지 않는다. 선두8byte 확인을 전체 HWPX 검증 성공으로 간주하지 않는다.
- 카탈로그는 참조용·expectation null이며 키워드·DB/API·migration·추출기·운영 설정은 변경하지 않는다.

## 다음 지역 조사

이번 직접 조사는 총9GET, 요청당12초·수집기 User-Agent·TLS 검증·자동 redirect 금지를 유지했다. 사천2, 남해2, 하동3, 거창2회다. 원문 HTML·파일을 조사 산출물로 저장하지 않았다. 실제 Java 관측은 별도6요청·23MiB 상한이다.

- 남해 `LGS-000236`: 등록 주소가 HTTP302로 같은 사이트 새올 모듈에 이동하며 일회성 요청 검증 값이 포함된다. 이동 경로의 값은 문서·대장에 저장하지 않으며 아직 첨부 수집 미확인이다.
- 하동 `LGS-000237`: 공식 목록·소상공인 검색·상세45193 HTTP200. 제목은 `2026년 하동군 소상공인 디지털 인프라 지원사업 추가공고`, `form#saeolGosiVO > div.bbs1view1`의 제목·본문·첨부 구조다. 공식 `eminwon.hadong.go.kr/emwp/jsp/ofr/FileDown.jsp` HTTPS GET3필드 HWPX1개를 발견했으나 아직 다운로드하지 않았다. 기존 SCMS 직접 파일 엔진 연결 후보이며 성공 수에 포함하지 않는다.
- 거창 `LGS-000240`: 등록 목록·공식 소상공인 검색 HTTP200이나 이번 검색 출력에서 고정 지원 표본은 확보하지 못했다. 공고 없음이나 수집 성공으로 간주하지 않는다.

## 검증 결과

- 실행: `gradlew.bat --no-daemon :test`의 사천 계약·본문·카탈로그·snapshot·inventory·파일 형식·worker probe 선택 검사와 `:bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SACHEON -PsanebCollectionWindowsTrust=true`다. 3분11초·종료0, Java290개 통과·실패/오류/생략0, bootJar 성공이다.
- 실제 관측: 2026-09-30 14:18:03 KST, `SACHEON-2106170`, 제목 조합 통과, 본문70자 AVAILABLE, 첨부 FOUND/complete, HWPX1개89,353byte 다운로드·형식 검증 성공이다.
- 파일 SHA-256: `af5df8afe98e7a3d346b400196e73950d2a93db8bb9802a2d21e8cf2e40ccf7e`.
- 요청 예약 상한 포함 실적4/6회·2,366,729/24,117,248byte, 관측 임시 원본 정리·운영 쓰기0이다. 첨부 텍스트 추출·전체 분석·정책 승인은 미실행이며 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다.
- Node23개 검사와 영수증310건·최신247공고 재현 검증 통과. 타 지역 상태·프로필 지문·기존 관측을 보존했다.
- **169/223수집원(75.8%) 확인·54잔여(미연결26+등록 다운로드 미확인28)**다. 프로필198개(지역197+기업마당1), 카탈로그256공고/197대상/참조255+기존 기대값1이다. 전체 첨부 세트 엄격 Gate16/223은 그대로이며 상시 유입·운영 E2E 완료가 아니다.

운영 DB·정책·worker·ENFORCE·배치·배포 변경은 없다. 브라우저는 사용자 정책상 미실행이다. AWS 인증 갱신과 이전 연제·구례 조사 원본 정리 미완료는 별도 후속이다.
