# 보은군 BBS 본문·첨부 모델 검증

## 현재 단계 / Gate

P3 기관 모델 구현 증분이다. 전체 Gate0~8/ATT001~062는 **Not ready**다. 제목 → 정제 본문 → 실제 PDF/HWP/HWPX 텍스트 → 최종 관리자 검증 순서를 유지한다. 본문 A/B·정보 부족은 첨부 처리를 중단시키지 않으며 파일명·미리보기로 내용을 판정하지 않는다.

- [x] 보은군 공식 지원사업3공고의 본문·첨부 영역을 실측했다.
- [x] 전용 BODY 정제와 고정 BBS profile을 연결했다.
- [x] BODY3건 및 전체3파일(HWPX2/PDF1)의 발견·다운로드·signature 검증을 통과했다.
- [x] schema2 catalog에 REFERENCE_ONLY3건을 추가했다. 전체 참조18/실행 기대값0이다.
- [ ] 보은 실제 Linux 텍스트 추출·역할/내용 기대값·정상 다중첨부 표본을 검증한다.
- [ ] 현재 운영 parser 결합·동일 SHA 배포·승인 범위 배치·운영 브라우저 및 전체 대상 Gate를 완료한다.

## 고정 범위와 계약

`LOCAL_GOV_NOTICE / LGS-000139 / HEURISTIC_NOTICE`에 `LOCAL_BOEUN_BBS_V1`을 결합한다. V61 공식 목록과 V62 parser seed가 근거이며 현재 운영 설정 확인을 대신하지 않는다. 기존6종 엔진 중 BBS 엔진의 `COMPACT_BOARD_PREVIEW` 변형이며 새 공통 엔진이 아니다.

상세는 `https://www.boeun.go.kr/www/selectBbsNttView.do`, `key=194/bbsNo=66/nttNo=양의정수`로 제한한다. 저장 URL identity·기관·parser·HTTPS443·동일 host·정확한 경로·중복/미지 query 검증을 유지한다. 다른 보은 게시판/기관으로 확장하지 않는다.

단일 `div.p-wrap.bbs.bbs__view > table.p-table.block` 안에서 같은 표에 소속된 유일한 `span.p-table__subject_text`와 `td[title=내용]`을 요구한다. BODY는 내용 셀만 사용한다. 실제 본문 A/B 문구·신청 링크·중첩 업무 표는 보존하고 메뉴·담당자·첨부명은 제외한다. 구조 변경/중복/중첩 가짜 제목은 BODY_SELECTOR_CHANGED, 정제 후 빈 본문은 BODY_TEXT_EMPTY다.

첨부는 `파일` 표제의 인접 셀·직접 `ul.p-attach`·`li.p-attach__item`의 다운로드 anchor만 사용한다. 다운로드는 `/www/downloadBbsFile.do?atchmnflNo=양의정수`만 승인한다. 미리보기의 `/www/previewBbsFile.do?key=194&bbsNo=66&atchmnflNo=같은값`은 구조만 검증하고 요청하지 않는다. 표식 일부 누락·추가 링크·다른 게시판/첨부번호·활성 요소는 미완료로 남긴다.

실제3파일은 모두 `application/x-msdownload`다. 보은 profile에만 이 기존 구형 MIME 예외를 지정했고 실제 signature·명시적 예상 형식·attachment Content-Disposition/파일 확장자 검증을 유지한다. UTF-8 header octet 복원은 사용하지 않는다. 다른 기관의 MIME/형식 오류를 허용하지 않는다.

## 실제 공개 검증

최종 실행2026-09-15 06:47 KST, production pinned transport·URL guard·BODY client·profile·파일 형식 검사기 기준이다.

| 공식 공고 | BODY | 전체 첨부 | 결과 |
|---|---|---:|---|
| [221499](https://www.boeun.go.kr/www/selectBbsNttView.do?key=194&bbsNo=66&nttNo=221499) | AVAILABLE/시도1/redirect0 | HWPX1 | signature 통과 |
| [221497](https://www.boeun.go.kr/www/selectBbsNttView.do?key=194&bbsNo=66&nttNo=221497) | AVAILABLE/시도1/redirect0 | HWPX1 | signature 통과 |
| [218812](https://www.boeun.go.kr/www/selectBbsNttView.do?key=194&bbsNo=66&nttNo=218812) | AVAILABLE/시도1/redirect0 | PDF1 | signature 통과 |

6시험/생략0/실패0,28초 성공이다. 첨부 시험의 상세3+파일3=6요청, 예약1,100,217bytes, 임시 원본 정리3/3이며 BODY는 별도3요청이다. 앞선 구조 조사/실패 진단 트래픽을 포함한 총량은 아니다. 파일20MiB/표본50MiB 등 기존 상한은 늘리지 않았다.

초기6시험은 BODY3통과/첨부3개 CONTENT_TYPE_MISMATCH였다. 기본 .NET 진단은 User-Agent가 없어 HTTP500 HTML을 반환했으며 이를 파일 MIME 근거로 사용하지 않았다. 실제 collector와 같은 User-Agent/Accept-Encoding으로 제한 조회한3파일에서 HTTP200/구형 MIME/ZIP2·PDF1 서명을 확인했고 profile 예외만 추가했다. 정상 파일로 HTML을 허용하거나 TLS 검증을 끄지 않았다.

공개 참조3건은 모두 단일 첨부 공고다. **정상3공고·정상 다중첨부1건 Gate를 충족했다고 계산하지 않는다.** 실행 기대값은0이며 역할은 UNKNOWN이다. 실제 본문/파일 다운로드 성공을 텍스트 추출·분류·worker DB/API/UI·운영 성공으로 확대하지 않는다. 이전 schema1 리소스·V1~V83·v1 계약·운영 정책/플래그/데이터를 변경하지 않았다.

```powershell
.\gradlew.bat :test --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaPlanTest' --tests '*AttachmentProviderQaCatalogTest' bootJar --no-daemon --max-workers=1
.\gradlew.bat attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest.discoversBoeunFilesAndChecksBoundedProductionTransport' --tests '*StandardBbsBodyContentLiveQaTest.readsBoeunBodyWithoutRequestingFiles' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' --no-daemon --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1
```

초기 표적167건은30초에 통과했다. MIME/catalog 후 전체 회귀 최종 결과는 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다. 등록 profile15개(지자체14), 전용 BODY14기관, 엔진6종/추출 형식3개다. 운영223기관의 과거 결합11/미결합212 snapshot은 새 모델 수와 별도다. 사용자가 요청한 Word2개는 보존한다.
