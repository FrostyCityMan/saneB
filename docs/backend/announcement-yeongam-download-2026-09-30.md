# 영암 본문·공개 POST 첨부 연결

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 보존하고 발견·다운로드·파싱 실패는 각각 기록한다. 제목→본문→첨부→관리자 최종 검증, 상시 worker·DB/API·관리자 화면·운영 E2E 전체 목표는 유지한다. HWP 추출기 추가 개선은 이번 범위가 아니다.

- [x] 영암 공식 목록·지원 검색·상세 표·공개 다운로드 폼 확인.
- [x] 영암 한정 프로필·본문 선택기·참조 카탈로그 구현.
- [x] 본문 정제·부분 실패·요청 경계·공식 HTML fixture 계약127건 통과.
- [x] 등록·카탈로그·worker 패키징·bootJar 및 실제 HWPX1개 관측 검증.
- [x] Java355건·Node23건·287영수증/229최신 공고 재현, 임시 원본 정리.
- [ ] 추출·상시 운영·운영 E2E와 전체 Goal Gate.

## 공식 경로와 구현

영암 LGS-000192 / SPRING_BBS의 목록은 `/home/www/open_information/yeongam_news/announcement/yeongam.go`다. 과거 `소상공인` 검색0건은 지역 전체 공고·첨부 부재를 뜻하지 않았다. 공식 제목 검색 `지원`에서 청년·가족 지원 공고를 확인했다. 검색 폼은 `search=search_title`, `not_ancmt_sj`를 POST한다. 검색 범위 확대는 운영 분류 규칙 변경이 아니다.

고정 표본은 공고40370, `전남광주 청년 문화복지카드 지원사업 3차 모집 공고`다. `YeongamNoticePage`는 유일한 `table.show_form`에서 제목·내용·첨부파일 행을 구분한다. 본문은 `내용` 행의 `td.content`만 사용하고 부서·연락처·메뉴·첨부 이름을 포함하지 않는다.

`YeongamAttachmentDiscoveryProfile`은 공식 첨부 셀의 `goDownLoad` 세 인자와 공개 `nnn` 폼의 세 hidden 필드만 해석한다. JavaScript는 실행하지 않는다. 파일명 뒤의 `다운로드` 표시만 제거하고, `eminwon.yeongam.go.kr/emwp/jsp/ofr/FileDownNew.jsp`에 POST한다. 다른 호스트·경로·임의 폼 필드·추가 스크립트·redirect를 허용하지 않는다. 원시 다운로드 인자는 요청 메모리에만 두고 locator에는 해시만 남긴다.

미지원 확장자·미해석 링크가 있어도 정상 descriptor는 보존한다. 첨부 영역 누락과 첨부 없음은 구분한다. 공통 다운로드 검증기·기존 프로필·migration·DB/API·운영 설정은 변경하지 않았다. 카탈로그 기대값은 null이며 정책 승인·운영 활성화를 의미하지 않는다.

상세 화면에서 `출처표시+상업적 이용금지` 안내를 확인했다. 이번 기술 QA를 상업 서비스 재사용 승인으로 취급하지 않으며 운영 이용조건 확인은 별도 보류 항목이다. 첫 조사 표본40456은 경로 확인에만 사용했고 다운로드하지 않았다.

## 화성·울주 조사 분리

| 수집원 | 확인 결과 | 후속 |
|---|---|---|
| 화성 LGS-000088 | 등록 경로의 `q_notAncmtSeCode=01` 제목 ‘지원’ 검색에서 계획 승인·재산 공시 등 고시가 확인됨. 이번에 적합한 지원 공고를 선정하지 못함 | 현재 목록 범위와 일반공고 경로의 수집 계약 확인. 첨부 실패로 집계하지 않음 |
| 울주 LGS-000082 | 등록 메뉴0403010000은 공식 고시 목록으로 이동하며 `seCode=01` 사용. 일반공고0403020000 메뉴가 별도로 존재 | 두 목록의 역할을 구분하고 기존 등록 범위를 무단 변경하지 않음 |

두 지역에서 상세·파일 다운로드는 실행하지 않았다. 다른 메뉴가 있다는 사실만으로 해당 지역 수집 성공을 주장하지 않는다.

## 검증 기록

초기 테스트의 Descriptor 접근자 오기 때문에 compileTestJava가 실패했고 실제 `expectedFormat` 계약에 맞게 수정했다. 이후 Java127건 실패·생략0으로 통과했다. 이는 외부 다운로드 성공을 의미하지 않는다.

최종 확장 검증은 Java355건 실패·오류·생략0, bootJar·공식 관측 성공(3분), Node23/23이다. 2026-09-30 10:15:14 KST 공고40370의 제목 조합 통과, 본문368자 AVAILABLE/ACCEPTED, 첨부 FOUND/complete를 확인했다. HWPX1개124,481byte의 다운로드·형식 검증에 성공했으며 상태는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 파일 SHA-256은 `bddf0fde4e6b380f10edad37afe1a87bc6397b29f50e59fe592ab97d3a822d3d`다. 텍스트 추출·구간 분석·운영 이용 승인은 수행하지 않았다.

기존286영수증과 다른 지역의 최신 근거를 보존하고 287영수증/229최신 공고의 재현을 확인했다. 다운로드 확인은 **154→155/223(69.5%)**, 잔여는 **69→68(미등록38+등록 미확인30)**이다. 지역185+기업마당1=186프로필, 카탈로그241공고/184대상/240참조전용+기존 승인 기대값1개다. 오류를 가진 수집원38개와 기존3표본·전체 첨부 Gate16충족/207잔여는 그대로다. 분모는 2026-09-28 15:56:12 KST 활성 지역 수집원 스냅샷이며 고유 행정구역 수나 운영 완료율이 아니다.

조사는 화성2·울주2·영암4=8요청(GET7·POST1)이었다. 요청당15초/연결7초/최대2MiB/자동 redirect0/TLS 검증을 유지했다. 실제 관측은 최대6요청·23MiB 이내에서 예약4회·2,311,745byte였고 조사 포함 예약 상한 사용은12회다. 조사 원본13개는 경로·크기·SHA-256 검증 후 삭제했다. 관측 원본도 자동 정리되었으며 복구하려면 다시 수집해야 한다. 보고서·해시·실패 근거는 보존했다. 사용한 단발 Node·Gradle/Java는 종료되었고 기존 사용자 프로세스는 유지했다. 원본 삭제 후 공식 HTML fixture1건은 환경변수 조건에 따라 기본 테스트에서 생략된다.

```powershell
# 공식 HTML fixture 환경변수는 실행 중에만 설정하고 종료 후 복원한다.
.\gradlew.bat :test --tests '*YeongamDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon
.\gradlew.bat :test --tests '*YeongamDownloadContractTest' --tests '*JeonnamThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=YEONGAM' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

전체 프로젝트 테스트·운영 배포·브라우저 검증은 이번 지역 연결 범위에서 실행하지 않았다. 브라우저는 현재 단계의 명시적 실행 지시가 없어 정책상 생략한다. 운영 정책 게시·ENFORCE·기존 데이터 적용도 실행하지 않는다.
