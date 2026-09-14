# 제천 BBS 본문·첨부 모델과 공개 QA 참조 확장

## 단계 / Gate

2026-09-15 P3 구현·검증 증분이다. **전체 Gate0~8/ATT001~062는 미완료**다. 제목 → 정제 본문 → 실제 첨부 텍스트 → 관리자 최종 검증을 유지한다. 이번 공식 표본 검증은 BODY 정제와 첨부 발견·다운로드·signature까지이며, 텍스트 추출/역할·내용 판정/DB/API/UI/운영 성공을 대신하지 않는다.

- [x] 제천3공고의 고유 본문·첨부 영역/표제/SVG/다운로드 경로를 실측했다.
- [x] BBS 엔진에 고정 COMPACT_SVG 변형과 제천 프로필을 추가했다.
- [x] 실제 BODY3건과 HWPX4첨부의 발견·다운로드·형식 검증을 통과했다.
- [x] 원주·제천6공고를 REFERENCE_ONLY catalog에 추가했다. 전체 참조15/실행 기대값0이다.
- [ ] 새 기관2개의 현재 운영 목록 parser 결합, 실제 Linux 추출·역할/내용 기대값을 검증한다.
- [ ] 전체 Provider 적용성·정책 QA·승인 범위 처리·동일 SHA 배포·운영 브라우저를 완료한다.

## 측정한 구조와 변경

기관 결합은 `LOCAL_GOV_NOTICE / LGS-000138 / HEURISTIC_NOTICE`, 프로필은 `LOCAL_JECHEON_BBS_V1`이다. 목록 parser는 V62 저장소 기준이며 만료된 AWS 세션의 재인증 전까지 현재 운영 일치를 확정하지 않는다. 다른 기관이나 제천의 다른 게시판에 포괄 적용하지 않는다.

1. 상세 host/path는 `www.jecheon.go.kr /www/selectBbsNttView.do`, query는 `key=5233`, `bbsNo=18`, 양의 정수 `nttNo`다. provider/source code/parser와 저장 URL identity hash를 확인한다. HTTPS443·같은 host·정확한 경로·중복/미지 query 거부·전송 상한을 유지한다.
2. 실제 목록 링크에는 빈 `id=`와 `&&` 구분자가 있었다. 기존 `parseHeuristicDocument`는 저장 전 `AnnouncementSourceIdentityNormalizer.canonicalizeUrl`을 호출해 빈 구분자를 제거한다. 새 프로필은 그 저장 URL의 **빈 id만** 허용하고 네트워크에는 canonical 상세3개 query만 보낸다. 비어 있지 않은 id/중복 id/원시 빈 query 조각을 넓게 허용하지 않는다. 기존 normalizer와 source identity를 변경하지 않았다.
3. 단일 `div.p-wrap.bbs.bbs__view > table.p-table.block` 안의 직접 소속 제목 th/인접 비어 있지 않은 td, 단일 `td[title=내용]`을 확인한다. 횡성의 제목 span 구조로 추정하지 않는다. BODY는 내용 td만 사용하며 중첩 업무 표·실제 A/B·일반 신청 링크는 유지하고 메뉴/담당자/첨부명은 제외한다. BODY_SELECTOR_CHANGED/BODY_TEXT_EMPTY는 주변 페이지로 대체하지 않는다.
4. 파일 표제는 `파일`이 아닌 **첨부파일**이다. 인접 td의 직접 `ul.p-attach`, 실제 클래스명 `li.p-attch__item`, 다운로드 anchor의 아이콘 span/파일명 span/SVG3자식을 검증한다. 사이트의 클래스 철자를 임의로 교정하지 않는다.
5. 다운로드는 `/www/downloadBbsFile.do?atchmnflNo=양의정수`만 승인한다. 미리보기 `/previewBbs.do?atchmnflNo=같은값`은 구조만 검증하며 요청하지 않는다. SVG의 고정 `p-icon.svg#arrow-circle-down`, `#search` 참조도 다운로드하지 않는다. 알 수 없는 SVG 참조·중첩 링크·추가/활성 속성·외부 host·부분 목록은 실패/미완료로 유지한다.
6. 실제4파일은 `application/x-msdownload`로 제공되었다. 이 제천 프로필에서만 기존 지원 MIME 예외를 지정했다. 실제 signature·명시적 예상 형식·attachment Content-Disposition/파일 확장자 의무는 그대로다. header octet 복원 없이 기본 검사로4파일 모두 통과했다. 임의 MIME 또는 HTML 응답을 허용하지 않는다.

파일명으로 역할/내용을 추측하지 않으며 UNKNOWN을 유지한다. 원주의 실제 형식 불일치 파일도 허용하지 않는다. 모델·catalog 지문 변경으로 기존 정책/QA snapshot을 재사용할 수 있다고 보지 않고 운영 정책을 갱신하지 않는다.

## 실제 공개 검증

최종 실행은2026-09-15 04:52 KST이며 production pinned transport·URL guard·본문 정제/프로필/파일 형식 검사기를 사용했다.

| 공식 공고 ID | BODY | 전체 첨부 | 파일 검사 |
|---|---|---:|---|
| 403587 | AVAILABLE / 시도1·redirect0 | 1 | HWPX1 통과 |
| 403530 | AVAILABLE / 시도1·redirect0 | 1 | HWPX1 통과 |
| 403490 | AVAILABLE / 시도1·redirect0 | 2 | HWPX2 통과 |

첨부 검증은 상세3+파일4=7요청, 예약1,261,135bytes, 원본 정리3/3이다. BODY는 별도3요청이다. 앞선 구조 조사·실패 진단을 포함한 회차 전체 트래픽 합계가 아니다. 파일20MiB/표본50MiB/전송30초 등 기존 제한은 늘리지 않았다.

초기6시험은 BODY3통과/첨부3실패였다. 첫 실패는 공식 첨부 표제의 차이로 발견 단계가 실패했고, 표제를 고친 뒤에는 구형 MIME 검사에서 실패했다.4파일 전체를 실제 다운로드해 MIME/형식을 확인한 뒤 프로필 예외만 지정했다. 최종6시험은24초에 생략 없이 통과했다.3공고가 정상 최종 후보라는 뜻은 아니며 PDF/HWP가 제천에 제공되지 않는다는 뜻도 아니다.

원주·제천3건씩을 catalog version `2026-09-15-reference-v2`에 추가했다. 참조15건 모두 expectation=null이고 실행/정상 공고/형식 coverage에는 산입하지 않는다. 원주491507의 표시 HWPX/실제 HWP 오류 표본도 참조 목록에서 숨기지 않는다. 참조 추가는 HTTP 실행·정책 게시·QA PASSED·기존 데이터 적용을 유발하지 않는다.

```powershell
.\gradlew.bat :test --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaPlanTest' --tests '*AttachmentProviderQaCatalogTest' --no-daemon --max-workers=1
.\gradlew.bat attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest.discoversJecheonFilesAndChecksBoundedProductionTransport' --tests '*StandardBbsBodyContentLiveQaTest.readsJecheonBodyWithoutRequestingFiles' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' --no-daemon --max-workers=1
```

표적 단위/계약138건은25초에 전부 통과했다. 전체 회귀/패키징/최종 SHA는 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다. 신규 migration/API shape/UI/규칙·운영 플래그 변경은 없다. 임시 원본과 조사 소스는 정리하며 사용자 Word 산출물은 보존한다.

현재 코드의 첨부 엔진은6종, 등록 프로필14개(지자체13개), 전용 본문13기관, 추출 형식3개다. 직전 운영223개·확인된 결합11/미결합212 snapshot과 구분한다. 새 원주/제천2개 운영 결합과 미구현 전체 기관을 확인하기 전에는 미결합 수를 줄여 보고하지 않는다.
