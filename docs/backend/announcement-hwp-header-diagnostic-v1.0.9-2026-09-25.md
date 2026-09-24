# HWP 헤더 확장 구조 진단1.0.9와 고정 확장 지원1.0.10

## 목적과 성공 기준

1.0.8 실파일 비교에서 달성51022의 TABLE_CONTROL_HEADER19회가 유지됐다.40바이트 지원이 이 파일 문제의 해법이라는 가정은 폐기한다. 임의 길이를 허용하지 않고 실제 컨트롤 헤더의 길이·확장 형태를 관측한다. 전체 목표는 제목→본문→전체 첨부→관리자 최종 검증이며, 이 진단만으로 Gate를 완료 처리하지 않는다.

- [x] 원문 없는 형식 진단·신뢰 경계 검증과 고정 단일 공고 실행을 구현했다.
- [x] 로컬 회귀·빌드 및 서울51022 PDF/HWP 전체2파일의 실제 진단을 확인했다.
- [x] 확인한 형태를 공개 읽기/쓰기 구현과 대조하고 실측된 고정0확장만 지원·재검증했다.
- [ ] 완전 추출·구간/역할·전체 Provider 정답·상시 worker/DB/API·운영 업무 E2E.

## 진단 계약

`hwpStructure.controlHeaders`는 선택적인 격리 IPC 확장이다. 기존4필드 응답은 그대로 읽고 신규 응답의 목록이 있으면 전부 검증한다. 외부 API·DB·migration을 변경하지 않는다. 원문·설명 문자열·파일명·알 수 없는 컨트롤 ID는 기록하지 않는다.

- `kind`: 기존 코드로 식별하는 TABLE/SECTION/COLUMN/HYPERLINK/OTHER의 고정 열거.
- `bytes`: 실제 CTRL_HEADER payload byte 길이.
- `shape`: TABLE 공통40/FIXED44/SHORT/EXTENDED_EXACT/DECLARED_TOO_LONG/EXTRA_ZERO/EXTRA_NONZERO와 다른 종류의 NOT_TABLE. EXTENDED 판별은 현재 검사와 동일한 offset44의 길이 선언을 기준으로 하며, 다른 형식의 의미를 확정한 값이 아니다.
- `tailBytes`: 현재 설명 길이 선언 이후 남는 byte 수. 남은 내용을 출력하거나 정상 필드로 인정하지 않는다.
- `count`: 같은 형태의 실제 레코드 개수.

목록은 종류→길이→형태→잔여 크기 순서이며 중복 금지다. 집계 합은 전체 tag71 개수와 정확히 같아야 한다. 원문 문자열·추가 필드·잘못된 자료형/범위/순서·모순된 형태를 거부한다. 서로 다른 형태128개 초과 시 LIMIT_EXCEEDED로 중단하고 진단을 조용히 잘라 성공시키지 않는다. 기존 입력/출력/압축/시간/메모리/격리 한도를 유지한다.

추출기와 runtime1.0.9로 구분한다. 텍스트/블록/부분 품질/역할/구간 규칙은 변경하지 않는다. 진단은 미지원 구조를 무시하거나 정상 후보로 승격하는 근거가 아니다.

## 실행 범위

`DALSEONG_HEADER`는51022 한 공고·전체2파일(PDF/HWP)의 고정 제목/본문/공식 첨부/기존 binary 지문을 검증한다. HWP 헤더 진단이 없으면 관측 완료로 처리하지 않는다. 최대6요청/23MiB/20분·CPU1·768MiB·임시공간1GiB다.

이전 서울2회와 로컬 영수증의 누적42회/33,664,851byte를 전송 전 재조회한다. 새 실행도 전체60회/96MiB 안에서 차감하며, 다른 모드로 원장을 초기화하지 않는다. 기존3공고 모드는 잔여byte가 부족하므로 이번 전송 도구에서 차단한다. 운영 설치/DB/정책/worker/기존 데이터를 변경하지 않는다. 원본과 전송본은 임시 자원으로 정리하고 비식별 영수증은 남긴다.

## 1.0.9 실측 및 검증

표적 준비 빌드2분5초·Java67,추출기144,패키지20·Node4·Python24건 실패/생략0이다. 확대 첨부 회귀3분24초에서root1988=1965통과/23조건부 생략·실패/오류0, 기타 동일 산출물은 UP-TO-DATE였다. 검증된 동일 산출물의 서울 QA를 확대 테스트와 병행했다.

서울 실행 `affdcce8d1824c738742c179a9a4ba56`, SSM `f6c36ea8-c7a3-4288-8d10-d93bcb3eb1ea`,34.261초·관측1/1·실패/생략/중단0이다. 코드 `112488dcb73792b3e73ab7eec9bece1acf82e3fe0eb289dee3b4597c9567d957`,probe `df16a2a7fd35e3c360a7fe00e9f2a3d313ea2c61f41f62812a6c1dc0e4bad972`,archive `4d0fef0b762d5edd7439d8c211e88ff5bc1cfded19643305fb20a30f0bc9f3f4`다.

| 종류 | payload byte | 형태 / 잔여byte | 개수 |
|---|---:|---|---:|
| COLUMN | 16 | NOT_TABLE / 0 | 3 |
| OTHER | 16 | NOT_TABLE / 0 | 2 |
| OTHER | 182 | NOT_TABLE / 0 | 1 |
| SECTION | 47 | NOT_TABLE / 0 | 2 |
| TABLE | 48 | EXTRA_ZERO / 2 | 19 |

TABLE19개가 모두48바이트이며 offset44의 빈 설명 선언 뒤0값2바이트가 남는다. 이는 TABLE_CONTROL_HEADER19회와 일치한다. OTHER의 정확한 의미는 출력하지 않았으므로 추측하지 않는다.1.0.9에서는 진단만 추가했으며 동일 body/binary/text/locator 지문·문자/블록수·PARTIAL_TEXT·검수 판정을 유지했다.

실제5요청/3,489,905예약byte를 더한 당시 누적47/60회·37,154,756/100,663,296byte다. unit비활성·probe/transport정리·운영DB미사용/쓰기0·설치JAR불변/healthUP을 확인했다. S3 자기 객체와 검증된 로컬 ZIP만 삭제하고 plan/result 및 사용자 미추적 파일을 보존했다.

## 1.0.10 호환 처리

공개 [읽기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/gso/part/ForCtrlHeaderGso.java)·[쓰기](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/gso/part/ForCtrlHeaderGso.java)는 설명 뒤 잔여 확장 byte를 허용/보존하지만 의미를 정의하지 않는다. 따라서 임의 확장을 수용하지 않고 실측된 **48바이트/빈 설명/끝2바이트0** 조합만 호환 처리한다. 이를 표준의 모든 예약 영역으로 일반화하지 않는다.

다른 길이·비영값 확장은 계속 부분 처리하고 셀 격자/문단/순서/문자수 검증을 모두 적용한다. 진단에는 EXTRA_ZERO를 그대로 남기며 숨기지 않는다. 텍스트 추출기는1.0.10으로 구분하고 이전 정책 QA 지문을 재사용하지 않는다. 동일 probe를 사용한 실측 비교에는 변경된 본체 코드 지문을 요구한다.

### 1.0.10 실제 개선 결과

표적45·추출기147·패키지20건 실패/오류/생략0, 준비 빌드1분6초 성공. 확대 첨부 회귀3분27초 성공,root1988=1965통과/23조건부 생략·실패/오류0이다. 추출기/패키지/bootJar는 앞선 동일 소스 실행·생성 결과를 재사용했다. Node4·Python24·PowerShell 구문0오류·diff 검사를 확인했다. 선행f6e56c8 [Linux36027350033](https://github.com/FrostyCityMan/saneB/actions/runs/36027350033)는 success이며 새 변경 CI와 구분한다.

서울 실행 `606f67bbd6e14dbda50b40d280350e86`, SSM `de02ec63-a017-42a1-851c-237a9b0b1003`,37.29초·관측1/1·실패/생략/중단0,Success/exit0이다. 코드 `3f5ca74ecba04f63928bfc425c1b46b5ae37acb32245f861983ce185bb4cc068`,probe `52a7badf5799a53fb4f333d5f0e6ac9db593bcd278bc4c6bd531fd43ade82903`,archive `da79e5160214d7aba05cd540ac842ced2bb6270ecdb57f43d583f0bd29a38db7`다.

**TABLE_CONTROL_HEADER19→0**, HWP 미지원 record2/control3만 남는다. 실제2파일의 binary/text/locator 지문·문자/블록 수와 body/profile/rules 지문·최종 판정이 이전1.0.9와 같은지 영수증을 직접 비교했다. HWP8,993자/347블록·PDF1,629자/21블록을 유지한다. 두 파일은 여전히PARTIAL_TEXT이고 공고는REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE/wholeText=false다. 정상 후보나 검수량 감소 전체 달성을 주장하지 않는다.

신규5요청/3,489,905예약byte를 더한 **최신 누적52/60회·40,644,661/100,663,296byte**, 잔여8회/60,018,635byte다. 다음 실행 전 이 사용량을 공제해야 하며 현재 전송 guard의 선행3회 조건을 그대로 재사용할 수 없다. 원격unit비활성·probe/transport/임시 원본 정리, 운영DB미사용/쓰기0·운영JAR불변/healthUP을 확인했다. S3 자기 객체·exact path/hash가 일치한 재생성용 로컬 ZIP을 삭제하고 영수증·사용자 미추적 파일을 보존했다. 소유 단발 Node/Java/PG 잔여0이다.

다음은 남은 미지원record/control의 실제 성격과 PDF 부분 추출 원인이다. 불명확한 개체를 무시하거나 파일 전체를 정상으로 처리하지 않는다. 운영 설정·정책 게시/ENFORCE·기존 데이터·배포는 변경하지 않았고 브라우저는 현재 명시 요청 정책상 미실행이다. 전체9Gate=8부분/1차단·goal active를 유지한다.
