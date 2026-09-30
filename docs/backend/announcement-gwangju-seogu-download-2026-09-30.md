# 광주 서구 공식 본문·첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 제목→본문→첨부→관리자 최종 검증, 정상 파일 보존·개별 오류 기록을 유지한다. 전체 worker·DB/API·운영 E2E 완료와 구분한다.

- [x] 공식 목록·상세 구조 및 지원 공고53423 확인.
- [x] 전용 본문 정제와 첨부 프로필·미승인 카탈로그 참조 구현.
- [x] 공식 HTML fixture·본문 회귀127건 실패·생략0.
- [x] 확장 Java272건·bootJar·실제 HWP 다운로드·Node23건·294영수증/235공고 재현.
- [ ] 첨부 추출·운영 재사용·운영 반영·운영 E2E·전체 Goal 완료.

## 공식 검색 조사

등록 목록은 `/menu.es?mid=a10807010000`이며 앞선 조사에서 같은 호스트 `/api/eminwon/gosiXmlList.es?mid=a10807010000`으로 이동하는 것을 확인했다. 이번 첫 페이지는200이었으나 공식 페이지 이동 POST는205/781byte 오류였다. 공개 세션·위조 요청 방지 필드를 포함한 curl 파이프 POST도 같은 결과였다.

이후 PowerShell WebSession과 폼 딕셔너리로 공식 GET→POST 절차를 그대로 구성했을 때200 검색 결과를 받았다. 세션·폼 인코딩을 함께 바꾼 진단이므로 원인을 특정 필드 하나로 확정하지 않는다. 앞선 curl 파이프의 마지막 필드에 줄바꿈이 붙을 수 있다는 차이도 있다. 인증 우회·보호장치 해제·TLS 완화는 하지 않았다. 세션 값은 운영 설정·코드·문서·영수증에 저장하지 않는다. 상시 수집 worker에 이 조사 세션을 주입하지 않는다.

검색에서 `2026년 서구 소상공인 카드 수수료 지원사업 공고`53423을 확보했다. 첨부 검증용 표본일 뿐 현재 예산 잔액이나 신청 가능 상태를 확인한 것은 아니다. 첫 페이지의56101(XLSX),56115(HWPX) 상세는 구조 fixture로만 사용하고 해당 파일은 요청하지 않았다.

## 구현 범위

`LOCAL_GWANGJU_SEOGU_GET_V1`은 `LOCAL_GOV_NOTICE / LGS-000067 / SPRING_BBS`에만 결합한다. 다른 SPRING_BBS 기관을 자동 지원하지 않는다. 상세는 공식 호스트의 `/api/eminwon/gosiXmlView.es`와 고정 mid·method·methodnm·공고 ID를 검증한다.

- 본문: `form[name=form1][method=post] > article.board_view > div.contents`만 사용한다. 제목·메뉴·담당자·첨부명은 포함하지 않는다.
- 첨부: 해당 article의 첨부파일 label 아래 `div.file > ul.list > li`만 순회한다.
- 다운로드: 공식 `goDownLoad`의 고정3인수를 해석하여 `eminwon.seogu.gwangju.kr/emwp/jsp/ofr/FileDown.jsp` GET만 구성한다. JS·iframe을 실행하지 않는다.
- source identity·host·HTTPS443·path·query·파일명·저장 경로·확장자를 검증하고 다른 파일 redirect·POST·추가 파라미터를 거부한다.
- 정상 HWP/PDF/HWPX와 미지원 형식, 알 수 없는 링크·구조 오류·10개 상한을 구분한다. 다른 파일의 실패가 정상 descriptor를 삭제하지 않는다.
- 신규 참조는 expectation=null이다. DB schema·기존 API·규칙·관리자 선택 정책·외부 공고 자동 활성화를 변경하지 않는다.

공식 페이지의 공공누리 출처표시·상업적 이용금지·변경금지 안내를 관측했다. 이번 수집 진단을 운영의 상업적 재사용 승인으로 해석하지 않으며, 운영 이용 범위는 별도 확인 대상으로 남긴다.

## 검증·자원 기록

표적 Java127건 실패·생략0으로 공식 HTML3건의 발견 결과와 본문 경계, 잘못된 링크·경로·요청·중복·한도를 확인했다. 확장 Java272건 실패·생략0, probe JAR·bootJar·실제 관측은2분52초에 통과했다. Node23건 실패·생략0 및294영수증/235공고 재현을 확인했다. 원본 fixture는 `SANEB_GWANGJU_SEOGU_SURVEY_FIXTURE=true`일 때만 실행하며 정리 후 기본 실행에서는1건 조건부 생략한다.

2026-09-30 11:15:58 KST 관측은 본문366자 AVAILABLE, 상세 식별 성공, 첨부 FOUND/complete=true, HWP178,688byte 다운로드·형식 검증 성공이다. 파일 SHA-256은 `3859f4ab9c78f393ac60bb8e6384cfd72b22c4e27dc0723f68827ad0f77ad3d5`다. 상태는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 HWP 텍스트 추출·정책 QA·최종 관리자 승인을 뜻하지 않는다.

**160→161/223수집원(72.2%) 다운로드 확인, 잔여63→62(미등록37+등록 미확인25)**다. 오류가 있는 수집원39개, 기존3표본·전체 첨부 Gate16충족/207잔여는 유지한다. 지역 등록 프로필185→186개와 기업마당1개를 합쳐187개다. 카탈로그244공고/186대상,243개 참조 전용+기존 기대값1이며 신규 기대값 승인은 없다. 기존293영수증/234공고와 다른 지역의 근거·프로필 지문은 보존했다. 분모는2026-09-28 활성 수집원 스냅샷이며 고유 행정구역 수·운영 완료율과 구분한다.

공식 조사9요청(GET6·POST3)이며, 잘못된 curl 옵션1회는 네트워크 요청 전에 종료했다. curl 조회는 연결7초·요청15초·응답2MiB 상한·자동 redirect0·TLS 검증을 적용했다. WebSession 진단2요청은 각각15초·자동 redirect0이고 실제 HTML은100KiB 미만이었다. Java 관측은 최대6요청·23MiB 중 본문 포함 예약4회·2,365,952byte, 조사 포함 요청 예약 상한13회다. 공개 세션 쿠키 파일은 즉시 삭제했다. 조사 HTML8개543,667byte는 소유 경로·크기·해시를 확인하여 삭제했고 관측 원본도 정리했다. 원본 복구에는 재수집이 필요하며 비식별 보고서·해시는 보존한다. 사용한 단발 Node와 이번 Gradle/Java는 종료하고 기존 사용자 프로세스는 유지했다.

```powershell
.\gradlew.bat :test --tests '*GwangjuSeoguDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon
.\gradlew.bat :test --tests '*GwangjuSeoguDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GWANGJU_SEOGU' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

직전 커밋458bfbc의 GitHub Actions [실행36657944644](https://github.com/FrostyCityMan/saneB/actions/runs/36657944644)는 success로 확인했다. 이번 변경의 CI·운영 결과와 구분한다. 운영 DB·정책·ENFORCE·기존 데이터·배포는 변경하지 않는다. 브라우저는 현재 단계의 명시적 지시가 없어 사용자 정책상 생략한다.
