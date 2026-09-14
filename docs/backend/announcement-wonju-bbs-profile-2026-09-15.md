# 원주 BBS 본문·첨부 프로필 증분

## 상태와 경계

2026-09-15 P3 구현·검증 증분이다. 제목 → 정제 본문 → 실제 첨부 텍스트 → 관리자 최종 검증 순서는 변경하지 않는다. 이번 실제 검증은 **본문 HTTP / 첨부 발견·다운로드·signature**까지다. Linux 텍스트 추출, 역할·분류 기대값, worker→DB→API→UI, 운영 활성화 성공은 아니다. 전체 Gate0~8/ATT001~062는 Not ready다.

- [x] 공식 HTML의 원주 게시판 구조와3개 공개 지원 관련 표본을 실측했다.
- [x] 기관별 고정 프로필과 본문 전용 정제를 연결했다.
- [x] 실제 본문3건, 전체 첨부6개 중5개 형식 통과·1개 형식 불일치 거부를 확인했다.
- [ ] 새 프로필과 현재 운영 목록 parser의 일치를 재조회한다. AWS 세션 만료로 미확인이다.
- [ ] 실제 격리 텍스트 추출·역할/내용 기대값·기관 QA를 완료한다.
- [ ] 같은 SHA 운영 배포·승인 범위 처리·운영 브라우저 E2E를 완료한다.

## 시스템 결합

`LOCAL_WONJU_BBS_V1`은 `LOCAL_GOV_NOTICE / LGS-000118 / HEURISTIC_NOTICE`에만 결합한다. 목록 parser는 저장소 V62의 원주 값을 기준으로 했으며 이번 회차 운영 재확인에는 실패했다. 다른 HEURISTIC_NOTICE 기관이나 다른 원주 게시판을 자동 지원하지 않는다. 목록 parser와 첨부 엔진을 동일 개념으로 보지 않는다.

기존 BBS 엔진에 `COMPACT_MENU_KEY` 변형을 추가했다. 첨부 엔진 수6은 그대로이고 전체 등록 프로필은12→13, 그중 지자체는11→12다. 전용 본문 정제는11→12기관이다. 운영 전체223개는 직전 읽기 전용 snapshot 수치이며, 새 원주의 운영 결합을 확인하지 않았으므로 기존 운영 일치11/미결합212를 현재211로 갱신하지 않는다.

공식 상세는 `https://www.wonju.go.kr/www/selectBbsNttView.do`와 `key=216`, `bbsNo=140`, 양의 정수 `nttNo`로 제한한다. source URL identity hash, 정확한 기관·목록 parser·host·HTTPS443·허용 query 검증을 유지한다. 다른 host, redirect·preview 요청, 추가/중복 파라미터, HTTP, session 경로는 승인하지 않는다.

## 본문·첨부 분리

- 실제 컨테이너는 `div.bbs_wrap > div.p-wrap.bbs.bbs__view > table.p-table`이다. 단일 표 안의 `제목` th/인접 비어 있지 않은 td와 `td[title=내용]`을 확인한다. 중첩 업무 표 안의 제목/본문 표식으로 바깥 공고 구조를 대신하지 않는다.
- BODY는 내용 td만 사용한다. 담당 부서·첨부명·메뉴/푸터를 섞지 않으며 실제 A/B 문구·업무 표·일반 신청 링크는 유지한다. 구조 누락/중복/변경은 BODY_SELECTOR_CHANGED, 정제 후 빈 본문은 BODY_TEXT_EMPTY다.
- 공식 `파일` th의 인접 td에 직접 속한 `ul.p-attach`와 전체 li를 확인한다. `a.p-attach__link` 안의 아이콘 span/파일명 span 구조만 허용한다. 알 수 없는 링크·요소·부분 목록은 성공 또는 첨부 없음으로 숨기지 않는다.
- 다운로드는 같은 host의 `/www/downloadBbsFile.do?key=216&atchmnflNo=양의정수`만 사용한다. `/www/previewUrl.do?key=216&atchmnflNo=같은값`은 구조만 확인하며 요청하지 않는다.
- 파일명의 내용/역할 추정은 없다. 최초 역할은 UNKNOWN이다. 이름의 확장자, 실제 signature, 응답 MIME/Content-Disposition을 기존 검사기로 대조한다. 같은 bytes라도 서로 다른 attachment ID를 임의로 합치지 않는다.
- 실제6개 응답에서 확인한 UTF-8 header octet은 기존 엄격 복원기로 처리한다. 원주 MIME은 표준 `application/octet-stream`이므로 legacy MIME 예외는 추가하지 않았다. 실제 HWP를 표시 이름만 보고 HWPX로 허용하지 않는다.

공유 BBS 코드/모델 지문이 바뀐다. 기존 태백·횡성·영월 프로필 hash와 이전 정책 QA 근거를 새 코드에서 그대로 재사용할 수 있다고 가정하지 않는다. 신규 정책 snapshot/QA가 필요하며 이전 정책 게시·기존 데이터 재분류는 실행하지 않았다.

## 실제 검증 결과

최종 표본 실행 시각은2026-09-15 04:34 KST다. 원문·파일명·헤더 인증값은 결과에 보관하지 않았다.

| 공식 공고 ID | BODY | 발견 첨부 | 형식 검사 결과 |
|---|---|---:|---|
| 491704 | AVAILABLE / 요청1·redirect0 | 4 | HWPX4개 통과 |
| 491507 | AVAILABLE / 요청1·redirect0 | 1 | 표시 HWPX / 실제 OLE-HWP signature → ATTACHMENT_FORMAT_MISMATCH 거부 |
| 491340 | AVAILABLE / 요청1·redirect0 | 1 | HWPX1개 통과 |

491507의 거부 시험은 `DOWNLOAD_REJECTED_AS_EXPECTED`로 기록한다. 테스트 통과가 정상 공고 또는 추출 성공을 뜻하지 않는다. 정상 표본3건 요구를 충족했다고 세지 않으며, 6파일 중1파일은 여전히 기술 예외다. 이번 표본으로 PDF/HWP가 원주에 제공되지 않는다고 판단하지 않는다.

최종 첨부 시험은 상세3회+파일6회=요청9회, 예약1,434,064bytes, 원본 정리3/3이다. 별도 BODY 시험은3요청이다. 앞선 구조 조사/헤더 진단 요청을 포함한 회차 전체 트래픽 합계가 아니다. 기존 단일 파일20MiB, 표본 예약50MiB,30초 전송 deadline 및 재시도/SSRF/TLS 제한을 늘리지 않았다.

초기 검증은 BODY3통과, 파일 시험3실패였다.2건은 응답 header octet 복원 필요,1건은 실제 형식 불일치였다.6파일 전체를 제한 다운로드해 원인을 구분한 뒤 header 복원만 연결하고 형식 거부를 유지했다. 마지막6시험은 양성5시험(BODY3/파일 표본2)과 음성1시험(형식 불일치 거부)으로29초에 통과했다. 텍스트 추출·DB 쓰기·정책 활성화는0이다.

## 실행 및 후속

```powershell
.\gradlew.bat :test --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaPlanTest' --no-daemon --max-workers=1
.\gradlew.bat attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest.discoversWonjuFilesAndChecksBoundedProductionTransport' --tests '*StandardBbsBodyContentLiveQaTest.readsWonjuBodyWithoutRequestingFiles' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' --no-daemon --max-workers=1
```

표적 단위25초 성공 후 실제 표본29초 성공이다. 임시 구조 조사 소스/HTML/파일은 정리한다. 전체 회귀/최종 SHA 기록은 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다. API shape·DB migration·규칙·UI·운영 플래그 변경은 없다.

제천도 공식 목록/상세 구조를 조사했으나 별도 링크/preview/SVG 구조 차이와 목록 query의 빈 `id`/구분자 변형이 있어 아직 프로필을 등록하지 않았다. 관리자에게 parser 선택을 맡기거나 원주 모델로 처리하지 않는다. 다음 기관 증분에서 실제 규칙과 전체 파일을 별도로 검증한다.
