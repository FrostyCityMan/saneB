# HWP 미지원 컨트롤 종류 진단 1.0.11

## 범위와 완료 기준

제목→본문→전체 첨부→관리자 최종 검증의 순서를 유지한다. 달성51022의 1.0.10에서 남은 미지원 record2/control3을 식별하는 격리 진단이다. 완전 추출·후보 승인·운영 정책 변경과 구분한다.

- [x] 공개 구현으로 레코드 번호와 컨트롤 ID를 대조한다.
- [x] 고정 종류만 비식별 진단으로 확장하고 임의 ID·payload·문자열은 출력하지 않는다.
- [x] 알려진 종류/잘못된 길이/임의 이름 거부, 그림의 부분 추출 유지, 로컬 회귀·빌드를 검증했다.
- [x] 누적 예산을 재검증한 서울 단일 공고 PDF/HWP 관측과 이전 영수증 비교.
- [x] 원격·전송·로컬 임시 원본 정리와 결과 기록.

성공은 식별 정확성·기존 품질 보존·동일 파일 지문·격리/예산/정리 증거다. 부분 추출을 정상으로 승격하거나 원문 노출·예산 초기화·운영 데이터 변경 시 실패다. 전체9Gate=8부분/1차단은 이 진단 완료와 별개다.

## 근거와 설계

고정 revision의 [HWPTag](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/object/etc/HWPTag.java)에서 76은 SHAPE_COMPONENT, 85는 SHAPE_COMPONENT_PICTURE다. 각주 모양74·쪽 테두리75와 다르다. 남은2개를 단순 배치 레코드로 무시할 근거가 없다. 그림이 장식인지 지원 조건을 담았는지는 숫자 진단만으로 판별할 수 없다.

[ControlType](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/object/bodytext/control/ControlType.java)의 고정 ID를 GSO/AUTO_NUMBER/NEW_NUMBER/PAGE_HIDE/PAGE_ODD_EVEN/PAGE_NUMBER/HEADER/FOOTER/FOOTNOTE/ENDNOTE/EQUATION/INDEX_MARK/BOOKMARK/OVERLAPPING_LETTER/ADDITIONAL_TEXT/HIDDEN_COMMENT/FORM/CLICK_HERE로 구분한다. 기존 TABLE/SECTION/COLUMN/HYPERLINK/OTHER와 정렬·개수·범위·전체tag71합 검증을 유지한다. 모든 알려진 종류는 ID를 담을 최소4byte를 요구하며 임의 종류/추가 필드를 거부한다.

이것은 `hwpStructure.controlHeaders` 격리 IPC의 고정 열거 확장이다. 종류 식별은 파서 지원·텍스트 없음·안전한 생략을 뜻하지 않는다. 추출/표/구간/역할/제목 A/B 규칙을 변경하지 않는다. DB·migration·외부 API·관리자 UI 계약은 바꾸지 않는다. 추출기와 runtime은1.0.11로 구분하며 이전 정책 QA 지문은 재사용하지 않는다.

## 실행 경계

`DALSEONG_HEADER`의 기존51022 한 공고·전체 PDF/HWP2파일만 사용한다. 최대6요청/23MiB/20분, CPU1/768MiB/임시공간1GiB다. 선행 로컬16회와 서울13+13+5+5회 영수증을 대조하여 **누적52/60회·40,644,661/100,663,296byte**를 먼저 공제한다. 전송 guard는 선행4회 성공/정리와 변경된 코드 지문을 요구한다. 일반3공고 실행 또는 예산 초기화는 허용하지 않는다.

운영 설치·DB·정책·worker·기존 데이터·배포를 변경하지 않는다. 새 원격 실행이 끝나면 실행unit/probe/transport/임시 원본과 자기 S3/로컬ZIP만 정리하고 비식별 영수증을 보존한다. 브라우저는 현재 명시 요청 정책상 실행하지 않는다.

## 실행 명령과 검증 결과

```powershell
.\gradlew.bat :attachment-extractor:test :test --tests '*Hwp*DiagnosticTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
.\gradlew.bat :test --tests 'com.saneb.domain.announcementattachment.*' :attachment-extractor:test :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
python -B -m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py
```

준비1분6초 성공: Java46·추출기149·패키지20건 실패/오류/생략0. 확대3분25초 성공: root1989=1966통과/23조건부 생략·실패/오류0. 확대 실행의 추출기/패키지/probe/bootJar는 앞선 동일 코드 검증·생성 결과 UP-TO-DATE다. Node4·Python24건 통과, PowerShell 구문0오류·diff 검사를 확인했다. 전체 기본 test 또는 생략23건의 실환경 성공을 주장하지 않는다.

선행 `ec50efe`의 [Linux CI36029869054](https://github.com/FrostyCityMan/saneB/actions/runs/36029869054)는 최종조회 completed/success다. 이1.0.11 변경의 CI 또는 운영 배포 증거와 혼동하지 않는다.

서울 실행 `194388c3e3bf4da58dedecba5030c641`, SSM `aa9ab9bf-2faf-46e8-8b42-20d5387b6449`,32.302초·1/1·실패/생략/중단0,Success/exit0이다. 코드 `562cf6bdb16519015981b9348ccb4f8d8ef5a9ccd744db2ab8ae862d623e7c1e`,probe `fae7f9b3f8e51c9532d01e95ecedc1b41984824a186760f18719c6de48b4945a`,archive `dcaceaeb68f94f6a15db3922e6c7550fa7eeb0177794766d967da8471179d77e`다. 패키지는138파일/188,245,928byte다.

| 실측 종류 | payload byte | 개수 | 의미 |
|---|---:|---:|---|
| COLUMN | 16 | 3 | 기존 식별 유지 |
| GSO | 182 | 1 | 이전 OTHER182의 일반 도형 컨트롤 식별 |
| PAGE_NUMBER | 16 | 2 | 이전 OTHER16의 쪽 번호 컨트롤 식별 |
| SECTION | 47 | 2 | 기존 식별 유지 |
| TABLE | 48 | 19 | 기존 EXTRA_ZERO/끝2byte0 유지 |

미지원 record2/control3은 그대로다. 그림 레코드와 도형 컨트롤이 있다는 사실은 확인했지만 그림 내용을 확인하거나 OCR한 것은 아니다. 쪽 번호의 실제 속성/장식 문자는 출력하지 않았으며 길이16만으로 모든 쪽 번호 헤더를 안전한 생략 대상으로 확정하지 않는다.

이전1.0.10 영수증과 body/profile/rules 지문·파일별 binary/text/locator 지문·문자/블록 수·품질·최종 판정·matchCount를 직접 비교해 동일함을 확인했다. HWP8,993자/347블록,PDF1,629자/21블록이며 **둘 다 PARTIAL_TEXT, REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE, wholeText=false**다. 관측 시험 통과를 정책 QA·정답 승인·상시worker DB/API·관리자 DRAFT 성공으로 바꾸지 않는다.

신규5요청/3,489,905예약byte를 더한 **최신 누적57/60회·44,134,566/100,663,296byte**, 잔여3회/56,528,730byte다. 원격unit비활성·probe/transport/임시 원본 정리, 운영DB미사용/쓰기0·설치JAR불변/healthUP을 확인했다. 자기 S3 객체와 exact path/hash가 일치한 로컬 ZIP을 삭제했다. 비식별 plan/result와 기존 사용자 미추적 파일은 보존했고 소유 Node/Java/PG 잔여0이다.

## 남은 업무

- [!] 동일51022 전체2파일 재실행은 최대6요청이므로 잔여3회로 실행할 수 없다. 새 모드·원장 초기화로 우회하지 않는다. 전송 guard의 선행4회 조건 또한 재사용 불가다.
- [ ] 쪽 번호 처리 개선은 고정 헤더·앵커·속성·잘못된 구조의 회귀 조건을 먼저 정한다. 이것만으로 그림/PDF 부분 품질 문제가 해소되는 것은 아니다.
- [ ] 그림의 의미/텍스트 누락 여부와 PDF 부분 추출 원인을 별도로 검증하고, 확인 불가능한 부분은 최종 검수 쟁점으로 남긴다.
- [ ] 전체 Provider 기대값·구간 근거·상시worker DB/API·정책/기존 데이터 승인·동일SHA 배포/업무 E2E는 전체 goal의 남은 범위다. 전체9Gate=8부분/1차단·active를 유지한다.
