package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class LocalGovernmentNoticeProviderContentClientTest {
    @Test void seongnamBodyExcludesMetadataAndAttachments(){
        String url="https://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=144735&subCheck=Y";
        String page="<form name=form1 method=post><div class=boardWrap><table class=bd00view><tr><th>제목</th><td>소상공인 특례보증</td><th>담당부서</th><td>수출 부서</td></tr><tr><th>첨부파일</th><td>특허.hwp</td></tr><tr><td class=bd01tdC colspan=4>소상공인 특례보증 지원</td></tr></table></div></form>";
        for(String content:List.of(page,page+page,page.replace("bd01tdC","other"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>투자 메뉴</nav>"+content));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(content.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 특례보증 지원");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }
    @Test void yeonjeGuryeBodyExcludesMetadataAndRejectsChangedStructure(){
        var pages=Map.of(
                "https://www.yeonje.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=43358&mId=0206030000","<form id=detailForm><div class=bod_wrap><div class=bod_view><h4>다자녀 지원</h4><div class=view_info>수출 부서</div><div class=view_cont>다자녀 대출이자 지원금</div><dl class=view_file><dt>특허.hwpx</dt></dl></div></div></form>",
                "https://www.gurye.go.kr/board/GosiView.do?pageIndex=1&menuNo=115004002001&not_ancmt_se_code=01,04,06,07&not_ancmt_mgt_no=25440","<div class=boardGroup><div class=board_view><h3>청년 지원</h3><ul class=write_info><li>수출 부서</li></ul><div class=board_con>청년 문화복지카드 지원금</div><ul class=file_down><li>특허.pdf</li></ul></div></div>");
        for(var entry:pages.entrySet())for(String page:List.of(entry.getValue(),entry.getValue()+entry.getValue(),entry.getValue().replace("class=view_cont","class=other").replace("class=board_con","class=other"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>투자 메뉴</nav>"+page));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,entry.getKey(),entry.getKey()));
            if(page.equals(entry.getValue())){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isIn("다자녀 대출이자 지원금","청년 문화복지카드 지원금");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }
    @Test void tongyeongBodyUsesOnlySubstanceAndRejectsChangedStructure(){
        String url="https://www.tongyeong.go.kr/00852/00853/00858.web?amode=view&not_ancmt_mgt_no=49251";
        String page="<form id=saeolGosiVO><div class=bbs1view1><h1 class=h1>소상공인 육성자금 지원사업</h1><div class=info1>수출 담당부서</div><div class=attach1>특허.hwp</div><div class=substance>소상공인 육성자금 지원</div></div></form>";
        for(String body:List.of(page,page+page,page.replace("class=substance","class=other"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>투자 메뉴</nav>"+body));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(body.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 육성자금 지원");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }
    @Test void yeongamBodyUsesOnlyOfficialContentCell(){
        String url="https://www.yeongam.go.kr/home/www/open_information/yeongam_news/announcement/announcement_01/show/40370?page=1";
        String page="<table class=show_form><tr><th scope=row>제목</th><td>청년 지원</td></tr><tr><th scope=row>담당부서</th><td>수출 부서</td></tr><tr><th scope=row>내용</th><td class=content>청년 문화복지카드 지원금</td></tr><tr><th scope=row>첨부파일</th><td>특허.hwpx</td></tr></table>";
        for(String html:List.of(page,page+page,page.replace("class=content","class=other"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>투자 메뉴</nav>"+html));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(html.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년 문화복지카드 지원금");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }
    @Test void gangnamAndDobongBodiesExcludeTitlesMetadataAndAttachments(){
        var pages=Map.of(
                "https://www.gangnam.go.kr/notice/view.do?not_ancmt_mgt_no=64668&mid=ID05_040201","<div class='board view'><div class=bbs-view><div class=post-title>중소기업 지원<br>수출 공고번호</div><div class=post-info>특허 부서</div><div class=post-content>중소기업 지원금</div><div class=bbs-view-file>제조.hwpx</div></div></div>",
                "https://www.dobong.go.kr/WDB_DEV/gosigong_go/detail.asp?idx=4734","<div class=bbsView><table class=boardView><tr><td class=title>청년 지원</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr></table><div class=bbsCont>청년 지원금</div></div>");
        for(var entry:pages.entrySet())for(String page:List.of(entry.getValue(),entry.getValue()+entry.getValue(),entry.getValue().replace("class=post-content","class=other").replace("class=bbsCont","class=other"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>투자 메뉴</nav>"+page));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,entry.getKey(),entry.getKey()));
            if(page.equals(entry.getValue())){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isIn("중소기업 지원금","청년 지원금");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }
    @Test void damyangFreshJsonBodyExcludesMetadataAndKeepsAttachmentFailuresSeparate() throws Exception {
        String url="https://www.damyang.go.kr/eminwon/searchDetail?notAncmtMgtNo=37086&listType=01";
        String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("RSLT_CD","0000","RSLT_DATA",Map.of("searchDetail",Map.of(
                "col4","소상공인 지원","col5","수출 담당부서","col8","<nav>특허 메뉴</nav><p>소상공인 지원금</p><script>투자</script>","fileNameArrList","깨진 첨부 목록"))));
        for(String value:List.of(json,json+"{}",json.replace("0000","9999"),json.replace("\"col8\":","\"col8\":\"duplicate\",\"col8\":"))){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("application/json;charset=UTF-8")),value.getBytes(StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(json)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(transport.requestTargets).extracting(ProviderContentRequestTarget::uri)
                    .containsExactly(URI.create("https://www.damyang.go.kr/eminwon/refreshSearchDetail?notAncmtMgtNo=37086"));
        }
    }
    @Test void damyangRejectsUnexpectedQueryMimeAndRedirectWithoutFallback() {
        String url="https://www.damyang.go.kr/eminwon/searchDetail?notAncmtMgtNo=37086";
        var invalid=new StubTransport();
        assertThat(client(true,invalid,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url+"&extra=1")).failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);
        assertThat(invalid.requestTargets).isEmpty();
        for(String type:List.of("","text/html","application/json;charset=EUC-KR")){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of(type)),"{}".getBytes(StandardCharsets.UTF_8)));
            assertThat(client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url)).failureCode()).isEqualTo(FailureCode.CONTENT_TYPE_UNSUPPORTED);
        }
        var redirect=new StubTransport();redirect.enqueue(new ProviderContentHttpResponse(302,Map.of("location",List.of(url)),new byte[0]));
        assertThat(client(true,redirect,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url)).failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);
        assertThat(redirect.requestTargets).hasSize(1);
    }
    @Test void nowonBodyExcludesMetadataAndDoesNotTreatImagesAsText() {
        String page="<nav>수출 메뉴</nav><div class=article-view><h1 class=article-subject>청년 응시료 지원</h1><table class=table-article><tr><th>첨부파일</th><td>특허.hwp</td></tr></table><div class=article-body><div class=txt>청년 지원금</div></div></div>";
        String url="https://www.nowon.kr/www/user/bbs/BD_selectBbs.do?q_bbsCode=1003&q_clCode=0&q_estnColumn1=11&q_ntceSiteCode=11&q_bbscttSn=20260915151630474";
        for(String value:List.of(page,page+page,page.replace("청년 지원금","<img src='/file.png'>"),page.replace("class=txt","class=unknown"))){
            var transport=new StubTransport();transport.enqueue(html(value));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void seohaeBodyExcludesMetadataFilesAndRejectsChangedStructure() {
        String page="<div class=board_view><h4 class=title>소상공인 지원</h4><ul class=datalist><li><dl><dt>담당부서</dt><dd>수출 부서</dd></dl></li><li><dl><dt>첨부파일</dt><dd>특허.hwp</dd></dl></li></ul><div class=con>소상공인 지원금</div></div>";
        String url="https://seohae.go.kr/open_content/main/bbs/bbsMsgDetail.do?msg_seq=42495&bcd=gosi";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("class=con","class=unknown"))){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("text/html;charset=UTF-8")),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void ulsanCityBodySeparatesContactAndAttachments() {
        String page="<div id=contents_inner><table class=tbl_bd_view><tr><th scope=row>제목</th><td colspan=3>소상공인 지원</td></tr><tr><th scope=row>담당부서</th><td>수출 부서</td></tr><tr><th scope=row>첨부파일</th><td colspan=3>특허.hwpx</td></tr><tr><th scope=row colspan=4>내용</th></tr><tr><td colspan=4>소상공인 지원금</td></tr></table></div>";
        String url="https://www.ulsan.go.kr/u/rep/transfer/notice/47059.ulsan?mId=001004002000000000&gosiGbn=A";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("<td colspan=4>","<td colspan=2>"))){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("text/html;charset=UTF-8")),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void osanBodyExcludesMetadataFilesAndRejectsChangedStructure() {
        String page="<form id=detailForm name=detailForm method=post><div class=bod_view><h4>소상공인 지원</h4><div class=view_info>수출 부서</div><div class=view_cont>소상공인 지원금</div><dl class=view_file><dt>첨부 파일</dt><dd>특허.hwp</dd></dl></div></form>";
        String url="https://www.osan.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=50603&mId=0302010000";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("class=view_cont","class=unknown"))) {
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("text/html;charset=UTF-8")),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void daejeonAggregatorBridgeIsExactAndKeepsBodyIsolated() {
        String detail="https://eminwon.seogu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?subCheck=Y&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=49944";
        String source="https://www.daejeon.go.kr/drh/MediaList.do?notiType=NOTI_06&menuSeq=2564";
        String page="<form name=form1 method=post><table class=tbl_board><tr><th>제목</th><td colspan=3>소상공인 지원</td></tr><tr><td class='aleft end' colspan=4>소상공인 지원금</td></tr><tr><th>첨부파일</th><td colspan=3>특허.hwp</td></tr></table></form>";
        for(String url:List.of(source,source.replace("2564","9999"),source.replace("www.daejeon.go.kr","evil.example"),source+"&extra=1")) {
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("text/html;charset=UTF-8")),page.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,detail));
            if(url.equals(source)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        }
        var redirect=new StubTransport();redirect.enqueue(new ProviderContentHttpResponse(302,Map.of("location",List.of(detail)),new byte[0]));
        assertThat(client(true,redirect,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,source,detail)).failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);
    }
    @Test void incheonCityBodyDoesNotFollowRedirectOrDowngrade() {
        String url="http://announce.incheon.go.kr/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchDetail&flag=gosiGL&svp=Y&sido=ic&sno=66970&gosiGbn=A";
        for(String location:List.of(url,url.replace("http:","https:"),"https://evil.example/")){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(302,Map.of("location",List.of(location)),new byte[0]));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);assertThat(result.redirectCount()).isZero();assertThat(result.attemptCount()).isEqualTo(1);
        }
    }
    @Test void incheonCityEucKrBodyUsesHttpsAndExcludesMetadata() {
        String page="<html><head><meta http-equiv='Content-Type' content='text/html; charset=euc-kr'></head><body><form name=myform><input type=hidden name=sno value=66970><input type=hidden name=gosiGbn value=A><input type=hidden name=flag value=gosiGL><table><tbody><tr><th class=tb_tit_center>제목</th><td class=tb_left colspan=3>소상공인 지원</td></tr><tr><th class=tb_tit_center>담당부서</th><td class=tb_left>수출 부서</td></tr><tr><th class=tb_tit_center colspan=4>내 용</th></tr><tr><td class=board_line></td></tr><tr><td class=tb_left colspan=4 wrap=VIRTUAL>소상공인 지원금</td></tr><tr><th class=tb_tit_center>첨부파일</th><td class=tb_left>특허.hwpx</td></tr></tbody></table></form></body></html>";
        String url="http://announce.incheon.go.kr/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchDetail&flag=gosiGL&svp=Y&sido=ic&sno=66970&gosiGbn=A";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("wrap=VIRTUAL","wrap=other"),page.replace("value=66970","value=999"))){
            var transport=new StubTransport();transport.enqueue(new ProviderContentHttpResponse(200,Map.of("content-type",List.of("text/html; charset=euc-kr")),value.getBytes(java.nio.charset.Charset.forName("EUC-KR"))));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");assertThat(result.finalUrl()).startsWith("https://announce.incheon.go.kr/");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void daeguCityBodyExcludesFilesAndMetadataAndChecksIdentity() {
        String page="<form id=sidoGosiAPIVO><input type=hidden name=sno value=33505><input type=hidden name=gosi_gbn value=A><div id=bbsView><div class=form_group><dl class=title><dt>제목</dt><dd>소상공인 지원</dd></dl></div><div class=form_group><dl><dt>담당부서</dt><dd>수출 부서</dd></dl></div><div class=form_group><dl class=content><dt>내용</dt><dd>소상공인 지원금</dd></dl></div><div class=form_group><dl class=attfile><dt>첨부파일</dt><dd>특허.hwp</dd></dl></div></div></form>";
        String url="https://www.daegu.go.kr/index.do?menu_id=00940170&menu_link=/front/daeguSidoGosi/daeguSidoGosiView.do&sno=33505&gosi_gbn=A";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("class=content","class=unknown"),page.replace("value=33505","value=999"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void geumcheonBodyExcludesFilesAndMetadata() {
        String page="<div id=contents class=cts294><div class=program><div class='veterinary_contract view'><div class='p-wrap bbs bbs__view'><table class='p-table block'><tbody><tr><td colspan=4 data-brl-flag=1>소상공인 지원</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><td colspan=4 data-brl-flag=7>소상공인 지원금</td></tr><tr><th>첨부파일</th><td colspan=3>특허.hwp</td></tr></tbody></table></div></div></div></div>";
        String url="https://www.geumcheon.go.kr/portal/tblSeolGosiDetailView.do?key=294&notAncmtMgtNo=27579";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("data-brl-flag=7","data-brl-flag=9"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void pohangBodyExcludesFilesAndMetadata() {
        String page="<form id=detailForm name=detailForm method=post><div class=bod_view><div class=subject>소상공인 지원</div><div class=view_info>수출 담당부서</div><dl class=view_file><dt>첨부파일</dt><dd>특허.hwpx</dd></dl><div class=view_cont>소상공인 지원금</div></div></form>";
        String url="https://www.pohang.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=73525&mid=0202010000";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("view_cont","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void andongBodyExcludesFilesAndMetadata() {
        String page="<form id=detailForm name=detailForm method=post></form><h4 class=hidden>전체 게시판 내용보기</h4><table class=bod_view><tr><th scope=col colspan=4 class=title>소상공인 지원</th></tr><tr><td>수출 담당부서</td></tr><tr><th scope=row class=list_file>첨부파일</th><td colspan=3 class=box_file><ul class=list_file>특허.pdf</ul></td></tr><tr><td colspan=4 class=cont><div class=cont_box>소상공인 지원금</div></td></tr></table>";
        String url="https://www.andong.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=63386&isLinkage=Y&mId=0401020100";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("cont_box","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void sangjuBodyExcludesFilesAndMetadata() {
        String page="<form id=form1 name=form1 method=post><table class=comp-tbl_datatype><tr><th scope=row>제목</th><td colspan=3>소상공인 지원</td></tr><tr><th scope=row>부서</th><td>수출 부서</td></tr><tr><th scope=row>첨부파일</th><td colspan=3>특허.pdf</td></tr><tr><th scope=row>고시공고 내용</th><td colspan=3>소상공인 지원금</td></tr></table></form>";
        String url="https://www.sangju.go.kr/gosi/detail.tc?mn=10297&mgtNo=27590";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("고시공고 내용","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void gunwiBodyExcludesFilesAndMetadata() {
        String page="<div class=boardView><div class=title><h4>소상공인 지원</h4><div><dl>수출 부서</dl></div><div><ul><li>특허.hwp</li></ul></div></div><div class=cont><div class=board_content>소상공인 지원금</div></div></div>";
        String url="https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&not_ancmt_mgt_no=25554&cmd=2";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("board_content","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void daeguSeoguBothMenusExcludeMetadataAndFileNamesFromBody() {
        String page="<form id=detailForm name=detailForm method=post><div class=bod_view><div class=subject>소상공인 지원</div><div class=view_info>수출 담당부서</div><dl class=view_file><dt>첨부파일</dt><dd>특허.hwp</dd></dl><div class=view_cont>소상공인 지원금</div></div></form>";
        for(String menu:List.of("0601020100","0601020200"))for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("view_cont","unknown"))){
            String url="https://www.dgs.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=26070&mid="+menu;
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void daeguDongguBodyExcludesFilesAndMetadata() {
        String page="<form id=detailForm name=detailForm method=post><div class=bod_view><div class=subject>소상공인 지원</div><div class=view_info>수출 담당부서</div><dl class=view_file><dt>첨부파일</dt><dd>특허.hwpx</dd></dl><div class=view_cont>소상공인 지원금</div></div></form>";
        String url="https://www.dong.daegu.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=60818&mid=0201020000";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("view_cont","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void hongcheonBodyExcludesFilesAndMetadata() {
        String page="<div class='p-wrap bbs bbs__view'><table class='p-table block'><tbody><tr class=p-table__subject><th>제목</th><td colspan=3><span class=p-table__subject_text>소상공인 지원</span></td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><td colspan=4>소상공인 지원금</td></tr><tr><th>첨부파일</th><td colspan=3>특허.hwp</td></tr></tbody></table></div>";
        String url="https://www.hongcheon.go.kr/www/selectEminwonView.do?key=278&not_ancmt_mgt_no=53249";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("colspan=4","colspan=2"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void samcheokBodyExcludesFilesAndMetadata() {
        String page="<form id=saeolGosiVO name=saeolGosiVO method=get><div class=bbs1view1><h1 class=h1>소상공인 지원</h1><div class=info1>수출 담당부서</div><div class=attach1>특허.hwp</div><div class=substance>소상공인 지원금</div></div></form>";
        String url="https://www.samcheok.go.kr/media/00084/00095.web?amode=view&mgtNo=36177&cd=01";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""),page.replace("substance","unknown"))){
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}
            else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);
        }
    }
    @Test void chuncheonJsonBodyUsesFixedApiAndExcludesMetadata() {
        String url="https://www.chuncheon.go.kr/cityhall/administrative-info/notice-info/notice-announcement/view/?notAncmtMgtNo=73071";
        String json="{\"board\":{\"not_ancmt_mgt_no\":\"73071\",\"not_ancmt_sj\":\"소상공인 지원\",\"not_ancmt_cn\":\"<p>소상공인 지원금</p><nav>수출 메뉴</nav><script>특허</script>\",\"dep_nm\":\"수출 부서\",\"chr_nm\":\"합성 담당자\"},\"file\":[{\"file_nm\":\"특허.pdf\"}]}";
        for(String mime:List.of("","application/json; charset=UTF-8")){
            var transport=new StubTransport();transport.enqueue(response(200,mime,json.getBytes(StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");
            assertThat(transport.requestTargets).hasSize(1);assertThat(transport.requestTargets.getFirst().uri().toString()).isEqualTo("https://www.chuncheon.go.kr/_chuncheon/noticeView.do?notAncmtMgtNo=73071");
        }
        for(String bad:List.of(json.replace("73071","999"),json+"{}",json.replace("\"board\":","\"board\":{},\"board\":"),"<html>접근 오류</html>")){
            var transport=new StubTransport();transport.enqueue(response(200,"",bad.getBytes(StandardCharsets.UTF_8)));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(transport.requestTargets).hasSize(1);
        }
        var badUrl=new StubTransport();assertThat(client(true,badUrl,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url+"&other=1")).failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);assertThat(badUrl.requestTargets).isEmpty();
        var redirectTransport=new StubTransport();redirectTransport.enqueue(redirect("/_chuncheon/noticeView.do?notAncmtMgtNo=999"));
        assertThat(client(true,redirectTransport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url)).failureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID);assertThat(redirectTransport.requestTargets).hasSize(1);
        var ordinary=new StubTransport();ordinary.enqueue(response(200,"",json.getBytes(StandardCharsets.UTF_8)));
        assertThat(client(true,ordinary,publicValidator()).selectContent(request("42")).failureCode()).isEqualTo(FailureCode.CONTENT_TYPE_UNSUPPORTED);
    }
    @Test void pyeongchangBodyExcludesFileAndDepartment() {
        String page="<div id=contentsArea><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class=skinTb-tr><div class=skinTb-th>제목</div><div class=skinTb-td>소상공인 지원</div></div><div class=skinTb-tr><div class=skinTb-th>부서</div><div class=skinTb-td>수출 부서</div></div><div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td>특허.hwp</div></div><div class=skinTb-tr><div class=skinTb-th>내용</div><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div></div></div>";
        String url="https://www.pc.go.kr/portal/government/government-notification?noticeMgrNo=41378";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void gangwonProvinceBodyExcludesFileAndDepartment() {
        String page="<div id=content-bx><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class=skinTb-tr><div class=skinTb-th>제목</div><div class=skinTb-td>소상공인 지원</div></div><div class=skinTb-tr><div class=skinTb-th>부서</div><div class=skinTb-td>수출 부서</div></div><div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td>특허.hwp</div></div><div class=skinTb-tr><div class=skinTb-th>내용</div><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div></div></div>";
        String url="https://state.gwd.go.kr/portal/bulletin/notification?articleSeq=272329";
        for(String value:List.of(page,page+page,page.replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+value+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));if(value.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void capitalEighthBodiesExcludeAttachmentAndMetadata() {
        String p="<article id=content><div class=content-body><div class=container><div class=article-view><div class=article-header><div class=info-area><h1 class=article-subject><span class=category-label>고시공고</span>소상공인 지원</h1></div><ul class='file-list file-shortcut'>수출 파일.pdf</ul></div><div class=article-body><div class=article-conetnt>소상공인 지원금</div></div></div></div></div></article>";
        String g="<div class=sub_content_cont_rt_cont><table class='table_style2 bbsView'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><th>첨부파일</th><td>수출 파일.hwp</td></tr><tr><th>내용</th><td>소상공인 지원금</td></tr></table></div>";
        var samples=List.of(new String[]{p,"https://www.paju.go.kr/user/board/BD_board.view.do?bbsCd=1022&seq=20260119101158902&q_ctgCd=4063"},new String[]{g,"https://www.gm.go.kr/pt/user/nftcBbs/BD_selectNftcBbsDetail.do?q_nftcBbsCode=1001&q_nftcBbsMgtno=65908"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void sejongBodyExcludesFilesAndDepartment() {
        String good="<div id=txt><div class=table-responsive><table class='table table-bordered'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><th>파일첨부</th><td>수출 특허.hwp</td></tr><tr><td class='tbl_cnts cell_left'>소상공인 지원금</td></tr></table></div></div>";
        String url="https://www.sejong.go.kr/prog/publicNotice/kor/sub02_030301/C1_1/view.do?not_ancmt_mgt_no=68219";
        for(String page:List.of(good,good+good,good.replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,url,url));if(page.equals(good)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void daejeonNextBodiesExcludeFilesAndMetadata() {
        String y="<div class=program--contents><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>수출 부서</span></div><div class='ui bbs--view--file'>수출 특허.hwp</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>소상공인 지원금</div></div></div></div></div>";
        String d="<table class=table2023><tr><th class=tit00>소상공인 지원</th></tr><tr><th>부서</th><td>수출 부서</td></tr><tr><th>첨부파일</th><td>수출 특허.hwp</td></tr><tr><td class=cont_area>소상공인 지원금</td></tr></table>";
        var samples=List.of(new String[]{y,"https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=35533"},new String[]{d,"https://www.daedeok.go.kr/dpt/dpt04/DPT040204_cmmBoardView.do?boardId=DPT_000087&ntatcSeq=1102472206"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void metroNextBodiesExcludeAttachmentAndMetadata() {
        String gw="<form name=form1 method=post><div class=tstyle_view><div class=title>청년 지원</div><ul class=head><li>수출 부서</li></ul><div class=tb_contents>청년 지원금</div><div class=add_file>수출 특허.hwpx</div></div></form>";
        String dj="<div class=program--contents><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>청년 지원</h2><span>수출 부서</span></div><div class='ui bbs--view--file'>수출 특허.hwpx</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>청년 지원금</div></div></div></div></div>";
        var samples=List.of(new String[]{gw,"https://eminwon.namgu.gwangju.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=45698&subCheck=Y"},new String[]{dj,"https://www.djjunggu.go.kr/prog/saeolGosi/GOSI/sub03_06/view.do?notAncmtMgtNo=46404"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("청년 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void incheonThirdBodiesExcludeAttachmentAndDepartmentText() {
        String gd="<div class=board-view><div class=title>청년 지원</div><ul class=info-data><li>수출 특허.hwpx</li></ul><div class=con-box><div class=detail>청년 지원금</div></div></div>";
        String yj="<div class=cm_board_detail1><div class=board_header_wrap><div class=board_header><div class=board_title>청년 지원</div></div><div>수출 부서</div></div><div class=board_content><div class=editor_content>청년 지원금</div></div><ul class=cm_file_list2><li>수출 특허.hwpx</li></ul></div>";
        var samples=List.of(new String[]{gd,"https://www.geomdan.go.kr/main/bbs/bbsMsgDetail.do?bcd=notice&msg_seq=235"},new String[]{yj,"https://www.yeongjong.go.kr/main/pst/view.do?pst_id=mn_pub_ntc&pst_sn=332418"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("청년 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void incheonSecondBodiesExcludeFilesMetadataAndNestedDuplication() {
        String je="<div class=board-view><div class=title>소상공인 지원</div><ul class=info-data><li>수출 특허.hwpx</li></ul><div class=con-box><div class=detail>소상공인 지원금</div></div></div>";
        String mi="<div class=board-view-s1><h3 class=board-title>소상공인 지원</h3><div class=file-area>수출 특허.hwpx</div><div class='content editor_content'><div class='content editor_content'>소상공인 지원금</div></div></div>";
        var samples=List.of(new String[]{je,"https://www.jemulpo.go.kr/main/bbs/bbsMsgDetail.do?bcd=announce&msg_seq=14094"},new String[]{mi,"https://www.michuhol.go.kr/main/board/view.do?board_code=board_13&sq=309943"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void incheonPortalBodiesExcludeFilesAndMetadata() {
        String gy="<div class='general_board board_view'><div class=tit><p class=title>소상공인 지원</p><dl class=file><dt>첨부파일</dt><dd>수출 특허.hwp</dd></dl></div><div class=con>소상공인 지원금</div></div>";
        String gh="<div class=board_view><p class=title>소상공인 지원</p><dl class=data><dt>부서</dt><dd>수출 부서</dd></dl><dl class=file><dt>첨부파일</dt><dd>수출 특허.hwpx</dd></dl><div class=con>소상공인 지원금</div></div>";
        var samples=List.of(new String[]{gy,"https://www.gyeyang.go.kr/open_content/main/eminwon/announce/eminwonDetail.do?seq=53151"},new String[]{gh,"https://www.ganghwa.go.kr/open_content/main/eminwon/announce/eminwonDetail.do?seq=52786"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void seoulSeventhBodiesExcludeAttachmentMetadataAndViewer() {
        String seoul="<div class=sib-viw-type-basic><h3>소상공인 지원</h3><div class=sib-viw-file-list>수출 특허.hwp</div><div class=sib-viw-type-basic-content><iframe src='https://invalid.example/viewer'></iframe><div id=scrabArea>소상공인 지원금</div></div></div>";
        String jj="<div class=board_view_02><table><tr><th class=view_tit>소상공인 지원</th></tr><tr><th>첨부</th><td>수출 특허.hwp</td></tr><tr><td class=article_body>소상공인 지원금</td></tr></table></div>";
        String ys="<div id=content><div class=bd-view><h2 class=subject>소상공인 지원</h2><div class=table-dl>수출 부서</div><div class=dbdata>소상공인 지원금</div></div></div>";
        var samples=List.of(new String[]{seoul,"https://www.seoul.go.kr/news/news_notice.do?bbsNo=277&nttNo=466130"},new String[]{jj,"https://www.junggu.seoul.kr/content.do?cmsid=14232&mode=view&cid=1475799545"},new String[]{ys,"https://health.yongsan.go.kr/portal/bbs/B0000095/view.do?nttId=766830&menuNo=200233"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void seoulSixthBodiesExcludeAttachmentsAndMetadata() {
        String yc="<form id=SeolCollectVo><div class='new-basic-view basic-view'><div class=view-subj><div id=bbsTitle>소상공인 지원</div></div><div class=view-info>수출 부서</div><div class=view-content><div class=txt-area>소상공인 지원금</div></div><div class=view-attachment>수출 특허.hwp</div></div></form>";
        String ga="<div class=board><div class=board-view><div class=tit><strong>소상공인 지원</strong></div><div class=view-info>수출 부서</div><div class=view-attachment>수출 특허.hwpx</div><div class=view_contents><div class=txt-area><pre>소상공인 지원금</pre></div></div></div></div>";
        var samples=List.of(new String[]{yc,"https://www.yangcheon.go.kr/site/yangcheon/ex/seol/seolContentDeailView.do?not_ancmt_mgt_no=46207"},new String[]{ga,"https://www.gwanak.go.kr/site/gwanak/ex/bbsNew/View.do?typeCode=1&not_ancmt_mgt_no=41842"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void seoulFifthBodiesExcludeAttachmentNamesAndMetadata() {
        String ddm="<div class='p-wrap bbs bbs__view'><div class=table-responsive><table class='p-table scroll'><tr><th>고시공고명</th><td>소상공인 지원</td></tr><tr><th>내용</th><td>소상공인 지원금</td></tr><tr><th>첨부파일</th><td>수출 첨부.hwpx</td></tr></table></div></div>";
        String sb="<table class='p-table block'><tr><th>제목</th><td>소상공인 지원</td><th>담당부서</th><td>메타데이터</td></tr><tr><th>내용</th><td>소상공인 지원금</td></tr><tr><th>첨부파일</th><td>수출 첨부.hwp</td></tr></table>";
        String ydp="<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr class=p-table__subject><td><span class=p-table__subject_text>소상공인 지원</span></td></tr><tr><td class=p-table__content colspan=4>소상공인 지원금</td></tr><tr><th>파일</th><td>수출 첨부.pdf</td></tr></table></div>";
        var samples=List.of(new String[]{ddm,"https://www.ddm.go.kr/www/selectEminwonWebView.do?key=3291&notAncmtMgtNo=22587&searchNotAncmtSeCode=01%2C02%2C04%2C05%2C06%2C07"},new String[]{sb,"https://www.sb.go.kr/www/selectEminwonView.do?key=6920&notAncmtMgtNo=43006"},new String[]{ydp,"https://www.ydp.go.kr/www/selectEminwonView.do?key=2851&menuFlag=01&notAncmtMgtNo=38256"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void seoulFourthBodiesExcludeAttachmentsMetadataAndPreviewFrames() {
        String sd="<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr class=p-table__subject><td><span class=p-table__subject_text>소상공인 지원</span></td></tr><tr><th>첨부파일</th><td>수출 첨부.pdf</td></tr><tr><td class=p-table__content><div class=preview_frame><iframe src='/preview'></iframe></div><div class=ntt_cn_container>소상공인 지원금</div></td></tr></table></div>";
        String sp="<div class='p-wrap bbs bbs__view'><form name=gosiFrm><table class='p-table block'><tr><th>제목</th><td>소상공인 지원</td><th>담당부서</th><td>메타데이터</td></tr><tr><th>내용</th><td>소상공인 지원금</td></tr><tr><th>파일</th><td>수출 첨부.pdf</td></tr></table></form></div>";
        String gj="<div class=view><div class=t><dl><dt>공고명</dt><dd>소상공인 지원</dd></dl><dl><dt>내용</dt><dd>소상공인 지원금</dd></dl><dl><dt>첨부파일</dt><dd>수출 첨부.pdf</dd></dl></div></div>";
        var samples=List.of(new String[]{sd,"https://www.sd.go.kr/main/selectBbsNttView.do?key=1473&bbsNo=184&nttNo=356569"},new String[]{sp,"https://www.songpa.go.kr/www/selectGosiData.do?key=2776&not_ancmt_mgt_no=33174"},new String[]{gj,"https://www.gwangjin.go.kr/portal/bbs/B0000003/view.do?menuNo=200192&nttId=6415244"});
        for(var sample:samples)for(String page:List.of(sample[0],sample[0]+sample[0],sample[0].replace("소상공인 지원금",""))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,sample[1],sample[1]));if(page.equals(sample[0])){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isIn(FailureCode.BODY_SELECTOR_CHANGED,FailureCode.BODY_TEXT_EMPTY);}
    }
    @Test void seodaemunBoardSeparatesBodyAndDoesNotFetchImagesOrAttachments() {
        String selected="<table class=boardWrite><tbody><tr><td class=subject>소상공인 지원</td></tr><tr><th>담당부서</th><td>메타데이터</td></tr><tr><td id=viewCon class=viewCon colspan=4>소상공인 지원금<img src='/image'></td></tr><tr><th scope=row>첨부파일</th><td colspan=3><a href='/downloadFile.do'>수출 특허.pdf</a></td></tr></tbody></table>";
        String url="https://www.sdm.go.kr/news/notice/notice.do?sdmBoardConfSeq=82&mode=view&sdmBoardSeq=313956";
        for(String page:List.of(selected,selected+selected,selected.replace("id=viewCon","id=changed"))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://www.sdm.go.kr/news/notice/notice.do",url));if(page.equals(selected)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);}
    }
    @Test void mapoPortalSeparatesBodyFromMetadataAndCommentedAttachments() {
        String selected="<div class=bbs_view><div class=bbs_view_body><div class=tbl_wrap3><table><tbody><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>메타데이터</td></tr><tr><td colspan=4>소상공인 지원금</td></tr><!-- <tr><td colspan=4>과거 수출 첨부</td></tr> --></tbody></table></div></div><div class=bbs_view_file>수출 특허.pdf</div></div>";
        String url="https://www.mapo.go.kr/site/main/nPortal/detail?bcId=19775";
        for(String page:List.of(selected,selected+selected,selected.replace("colspan=4","colspan=3"))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://www.mapo.go.kr/site/main/nPortal/list",url));if(page.equals(selected)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);}
    }
    @Test void sokchoPortalSeparatesBodyMetadataAndAttachments() {
        String selected="<div id=content-bx><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class=skinTb-tr><div class=skinTb-th>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div><div class=skinTb-th>담당부서</div><div class=skinTb-td>메타데이터</div></div><div class=skinTb-tr><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div><div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td><a href='/file'>수출 특허.pdf</a></div></div></div></div>";
        String url="https://www.sokcho.go.kr/sc/portal/sokchonews/notification?notAncmtMgtNo=32983";
        for(String page:List.of(selected,selected+selected,selected.replace("skinTb-conts","changed"))){var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+page+"<footer>푸터</footer>"));var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://www.sokcho.go.kr/sc/portal/sokchonews/notification",url));if(page.equals(selected)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금");}else assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);}
    }

    private static final int TWO_MEBIBYTES = 2 * 1024 * 1024;
    private static final String HOST = "city.example.go.kr";
    private static final String REGISTERED_URL = "https://" + HOST + "/notices";
    private static final String DETAIL_URL = "https://" + HOST + "/notices/42";
    private static final UUID SOURCE_ID = UUID.fromString("77000000-0000-0000-0000-000000000001");

    @Test void hampyeongBodyPreservesConditionsAndExcludesFiles() {
        String page="<div id='board_view'><table class='basic_table'><tbody><tr><th>제목</th><td colspan='3'>소상공인 지원</td></tr><tr><th>담당부서</th><td>담당자</td></tr><tr><td colspan='4'>소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table></td></tr><tr><th>첨부파일</th><td colspan='3'>특허 첨부</td></tr></tbody></table></div>";
        for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://www.hampyeong.go.kr/pg/GosiList.do?pageId=www273","https://www.hampyeong.go.kr/pg/GosiDetail.do?SEQ=32368&pageId=www273&notAncmtSeCode=01,02,03,04"));
            if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
            else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
        }
    }

    @Test void boseongBodyPreservesConditionsAndExcludesFiles() {
        String page="<div id='content'><div id='board_basic_view'><div class='news_tit'><h3>청년 지원</h3><dl><dd>담당자</dd></dl></div><div class='file_attach'>특허 첨부</div><div class='board_cont'>청년 지원금 <table><tr><td>수출기업 제외</td></tr></table></div></div></div>";
        String list="https://www.boseong.go.kr/www/open_administration/city_news/notification";
        for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
            var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
            var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,list,list+"?idx=37905&mode=view"));
            if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년 지원금 수출기업 제외");}
            else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
        }
    }

    @Test void jeonbukThirdBodyPreservesConditionsAndExcludesAttachmentMetadata() {
        for(var site:JeonbukThirdNoticePage.Site.values()) {
            String body="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=site==JeonbukThirdNoticePage.Site.JEONJU?"<div id='board_wrap'><div class='view-group'><div class='view-table'><ul><li><strong>제목</strong><span>소상공인 지원</span></li><li><strong>첨부파일</strong><div>특허 첨부</div></li></ul></div><div class='view-list'><div class='view-con'>"+body+"</div></div></div></div>":"<div class='bbs_skin'><div class='bbs_view'><div class='bbs_vtop'><h4>소상공인 지원</h4><ul><li>담당자</li></ul></div><div class='bbs_con'>"+body+"</div><p class='bbs_filedown'>특허 첨부</p></div></div>";
            String url="https://"+site.host+site.path+"?"+site.boardKey+"="+site.board+"&"+site.menuKey+"="+site.menu+"&"+site.idKey+"="+(site==JeonbukThirdNoticePage.Site.JEONJU?"7c37d618236741a28b133aa279d05c63":"670379");
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
            }
        }
    }

    @Test void chungcheongSixthBodySeparatesFilesAndKeepsSameHostBoundary() {
        for(var site:ChungcheongSixthNoticePage.Site.values()) {
            boolean asan=site==ChungcheongSixthNoticePage.Site.ASAN;String body="청년농업인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=asan?"<div class='customContents'><div class='viewForm'><dl class='ct_th04'><dt>청년농업인 지원</dt><dd>담당자</dd></dl><div class='ct_tc14'><div class='ct_btn04'><b>첨부파일</b>특허 첨부</div><div class='ct_tc14'><div class='field-name-body'><div class='field-items'>"+body+"</div></div></div></div></div></div>"
                    :"<form name='form1'><table class='bbs_default view'><tbody><tr class='subject'><th>제목</th><td><span class='subject_text'>청년농업인 지원</span></td></tr><tr><td title='내용' class='bbs_content'>"+body+"</td></tr><tr><th>첨부파일</th><td>특허 첨부</td></tr></tbody></table></form>";
            String url="https://"+site.host+site.path+(asan?"?no=257&m_mode=view&mgt_no=80727":"?jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=65309&homepage_pbs_yn=Y&subCheck=Y");
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("청년농업인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
            }
            if(asan){var transport=new StubTransport();var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://www.asan.go.kr/main/cms/?no=257",url.replace("www.asan.go.kr","asan.go.kr")));assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);}
        }
    }

    @Test void chungcheongFifthBodyPreservesConditionsAndExcludesFiles() {
        for(var site:ChungcheongFifthNoticePage.Site.values()) {
            String body="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=site==ChungcheongFifthNoticePage.Site.GEUMSAN?"<div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>특허 첨부</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>"+body+"</div></div></div></div></div>"
                    :"<section id='con_body'><div id='txt'><div class='board_viewTit'><h4>소상공인 지원</h4></div><ul class='board_viewInfo'><li>담당자</li></ul><div class='board_viewDetail'>"+body+"</div><ul class='board_viewInfo'><li class='file'><span>파일</span><div>특허 첨부</div></li></ul></div></section>";
            String url="https://"+site.host+site.path+"?mode=V&mng_no="+(site==ChungcheongFifthNoticePage.Site.GEUMSAN?"ea6a4e53c07c7f05a9e9240dbb006d43&site_dvs_cd=kr":"2125452");
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+site.listPath,url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
            }
        }
    }

    @Test void chungcheongFourthBodyPreservesConditionsAndExcludesFiles() {
        for(var site:ChungcheongFourthNoticePage.Site.values()) {
            String body="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=site==ChungcheongFourthNoticePage.Site.HONGSEONG?"<div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>특허 첨부</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>"+body+"</div></div></div></div></div>"
                    :"<div class='card program--view'><div class='card-body prog bucket-form'><div class='form-group'><div class='control-label'><label for='notAncmtSj'>제목</label></div><div><span id='notAncmtSj'>소상공인 지원</span></div></div><div class='form-group'><div class='control-label'><label for='notAncmtCn'>내용</label></div><div><span id='notAncmtCn'>"+body+"</span></div></div><div>특허 첨부 담당자</div></div></div>";
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+site.listPath,"https://"+site.host+site.path+"?notAncmtMgtNo=1"));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
            }
        }
    }

    @Test void chungcheongThirdBodySeparatesMetadataFilesAndNavigation() {
        for(var site:ChungcheongThirdNoticePage.Site.values()) {
            String content="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            boolean cb=site==ChungcheongThirdNoticePage.Site.CHUNGBUK;
            String page=cb?"<div class='p-wrap bbs bbs__view uiux_type'><div class='bbs_viewbox'><div class='subjectbox'><span class='subject'>소상공인 지원</span><div class='fieldlistbox'>담당자</div></div><div class='viewcontentbox'><div class='viewcontent'><div class='contenttext'>"+content+"</div></div><div class='viewcontent'><div class='attachedfile'>특허 첨부</div></div></div></div></div>"
                    :"<div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>특허 첨부</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>"+content+"</div></div></div></div></div>";
            String url="https://"+site.host+site.path+(cb?"?key=422&no=67302":"?notAncmtMgtNo=59971");
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void gangwonSecondBodySeparatesMetadataFilesAndNavigation() {
        for(var site:GangwonSecondNoticePage.Site.values()) {
            String content="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            boolean yang=site==GangwonSecondNoticePage.Site.YANGGU;
            String page=yang?"<div id='user_board_whole'><form id='registform' name='registform' method='post'><fieldset><div id='user_board_read_title'>소상공인 지원</div><div class='read_information'>담당자</div><div id='user_board_read_view'><div class='user_board_read_view_pre'>"+content+"</div></div><div id='user_board_read_file'>특허 첨부</div></fieldset></form></div>"
                    :"<div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class='skinTb-tr'><div class='skinTb-th'>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div></div><div class='skinTb-tr'><div class='skinTb-conts'>"+content+"</div></div><div class='skinTb-tr'><div class='skinTb-th'>첨부파일</div><div class='skinTb-td'>특허 첨부</div></div></div>";
            String url="https://"+site.host+site.path+(yang?"?gfnc=www&bk=IHINR260828133751437&mu_idx=226&bt=rd&bcd=announcement":"?articleSeq=245926");
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalSeventhBodyPreservesConditionsButExcludesNavigationAndFiles() {
        for(var site:CapitalSeventhNoticePage.Site.values()) {
            String content="사업자 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=site==CapitalSeventhNoticePage.Site.POCHEON
                    ?"<div class='p-wrap bbs bbs__view uiux_type'><div class='bbs_viewbox'><div class='subjectbox'><span class='subject'>소상공인 지원</span><div class='fieldlistbox'>담당자</div></div><div class='viewcontentbox'><div class='viewcontent'><div class='contenttext'>"+content+"</div></div><div class='viewcontent'><div class='attachedfile'>특허 첨부</div></div></div></div></div>"
                    :"<table class='bbs_default view'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>내용</th><td>"+content+"</td></tr><tr><th>파일</th><td>특허 첨부</td></tr></table>";
            String url="https://"+site.host+site.path+"?key="+site.menu+"&"+site.idKey+"=1&"+site.typeKey+"="+site.type;
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)) { assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("사업자 지원금 수출기업 제외"); }
                else { assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull(); }
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalSixthBodyPreservesConditionsButExcludesNavigationAndFiles() {
        for(var site:CapitalSixthNoticePage.Site.values()) {
            String content="사업자 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page=site==CapitalSixthNoticePage.Site.SIHEUNG
                    ?"<form id='detailForm' name='detailForm' method='post'><div class='bod_wrap'><div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>메타데이터</div><div class='view_cont'>"+content+"</div><dl class='view_file'>특허 첨부</dl></div></div></form>"
                    :"<form id='aform' method='get'><div class='p-wrap bbs bbs__view'><table class='p-table'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>내용</th><td>"+content+"</td></tr><tr><th>파일</th><td>특허 첨부</td></tr></table></div></form>";
            String url="https://"+site.host+site.path+"?"+site.menuKey+"="+site.menu+"&"+site.idKey+"=1";
            for(String selected:List.of(page,page+page,"<main>내용 없음</main>")) {
                var transport=new StubTransport();transport.enqueue(html("<nav>수출 메뉴</nav>"+selected+"<footer>푸터</footer>"));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)) { assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("사업자 지원금 수출기업 제외"); }
                else { assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull(); }
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalFifthBodyPreservesConditionsButExcludesNavigationAndFiles() {
        for(var site:CapitalFifthNoticePage.Site.values()) {
            boolean post=site==CapitalFifthNoticePage.Site.UIJEONGBU;
            String page="<main><nav>특허 메뉴</nav><form id='detailForm' name='detailForm' method='post'>"+(post?"":"<div class='bod_wrap'>")
                    +"<div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>담당부서</div><div class='view_cont'>사업자 지원금 <table><tr><td>수출기업 제외</td></tr></table></div><dl class='view_file'><dt>첨부 파일</dt><dd>스타트업 신청서</dd></dl></div>"
                    +(post?"":"</div>")+"</form><footer>푸터</footer></main>";
            String url="https://"+site.host+site.path+"?mId="+site.menu+"&notAncmtMgtNo=1";
            for(String selected:List.of(page,page+page,page.replace("class='view_cont'","class='changed'"))) {
                var transport=new StubTransport();transport.enqueue(html(selected));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/list",url));
                if(selected.equals(page)) { assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE); assertThat(result.bodyText()).isEqualTo("사업자 지원금 수출기업 제외"); }
                else { assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED); assertThat(result.bodyText()).isNull(); }
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalFourthBodyDoesNotUseMenusMetadataOrExpiredPageFallback() {
        for(var s:CapitalFourthNoticePage.Site.values()) {
            boolean pt=s==CapitalFourthNoticePage.Site.PYEONGTAEK,gimpo=s==CapitalFourthNoticePage.Site.GIMPO;
            String body="소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table>";
            String page="<main><header>특허 메뉴</header>"+(pt?"<form id='detailForm' name='detailForm' method='post'><div class='bod_wrap'><div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>담당부서</div><div class='view_cont'>"+body+"</div></div></div></form>":"<div id='contents'><table class='"+(gimpo?"p-table block":"bbs_default view")+"'><tr><th>"+(gimpo?"제목":"제 목")+"</th><td>소상공인 지원</td></tr>"+(gimpo?"<tr><td colspan='4'><div>"+body+"</div></td></tr>":"<tr><th>내용</th><td>"+body+"</td></tr>")+"<tr><th>첨부파일</th><td>스타트업 신청서</td></tr></table></div>")+"<footer>푸터</footer></main>";
            String url="https://"+s.host+s.path+"?"+s.menuKey+"="+s.menu+"&"+s.idKey+"=1"+(gimpo?"&cate_cd=1":!pt?"&not_ancmt_se_code=04":"");
            for(String selected:List.of(page,page+page,"<main>게시기간이 아닙니다.</main>")) {
                var t=new StubTransport();t.enqueue(html(selected));var r=client(true,t,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+s.host+"/list",url));
                if(selected.equals(page)){assertThat(r.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(r.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외");}
                else {assertThat(r.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(r.bodyText()).isNull();}assertThat(t.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalThirdBodyUsesOnlyOfficialContentIncludingDisabledTextarea() {
        for(var site:CapitalThirdNoticePage.Site.values()) {
            boolean hanam=site==CapitalThirdNoticePage.Site.HANAM, nyj=site==CapitalThirdNoticePage.Site.NAMYANGJU;
            String page="<main><header>특허 수출 메뉴</header><div class='p-wrap bbs bbs__view'>"
                    +(nyj?"<div class='card board_bottom'><div class='card_title'><div class='bbs_view_title'>소상공인 지원</div></div></div>":"")
                    +"<table class='p-table block'>"+(hanam?"<tr><td><span class='p-table__subject_text'>소상공인 지원</span></td></tr>":!nyj?"<tr><th>제목</th><td>소상공인 지원</td></tr>":"")
                    +"<tr><th>담당부서</th><td>메타데이터</td></tr>"+(hanam?"<tr><td class='p-table__content'><textarea title='내용' disabled>사업자 지원금 수출기업 제외</textarea></td></tr>":"<tr><th>내용</th><td>사업자 지원금 <table><tr><td>수출기업 제외</td></tr></table></td></tr>")
                    +"<tr><th>첨부파일</th><td>특허 첨부</td></tr></table></div><footer>푸터</footer></main>";
            String fixed=nyj?"&sa1Join=01;02;04;05&sc4=2024":hanam?"&not_ancmt_se_code=01,04":"&searchGosiSe=01,04,06";
            String url="https://"+site.host+site.path+"?key="+site.menu+"&"+site.idKey+"=1"+fixed;
            for(String selected:List.of(page,page+page,page.replace(hanam?"title='내용'":"<th>내용</th>",hanam?"title='변경'":"<th>변경</th>"))) {
                var transport=new StubTransport();transport.enqueue(html(selected));
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/www/list.do",url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("사업자 지원금 수출기업 제외");}
                else {assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    @Test void capitalEminwonBodyDoesNotIncludeMenuMetadataOrAttachments() {
        for(var site:CapitalEminwonNoticePage.Site.values()) {
            boolean yang=site==CapitalEminwonNoticePage.Site.YANGJU, yeo=site==CapitalEminwonNoticePage.Site.YEOJU;
            String page="<main><header>수출 특허 메뉴</header><div class='p-wrap bbs bbs__view'><table class='"+(yang?"bbs_default view":"p-table block")+"'>"
                    +(yeo?"<tr><td><span class='p-table__subject_text'>청년 지원 공고</span></td></tr>":"<tr><th>제목</th><td>청년 지원 공고</td></tr>")
                    +"<tr><th>담당자</th><td>메타데이터</td></tr><tr>"+(yeo?"":"<th>"+(yang?"내용":"상세내용")+"</th>")
                    +"<td title='내용' class='"+(yang?"bbs_content":"p-table__content")+"'>사업자 지원금 <table><tr><th>대상</th><td>청년</td></tr></table> 수출기업 제외</td></tr>"
                    +"<tr><td class='p-table--attach'>스타트업 첨부</td></tr></table></div><footer>특허 푸터</footer></main>";
            for(String selected:List.of(page,page.replace("title='내용'","title='변경'"),page+page)) {
                var transport=new StubTransport();transport.enqueue(html(selected));
                String url="https://"+site.host+CapitalEminwonNoticePage.DETAIL+"?key="+site.menu+"&not_ancmt_mgt_no=1"+(site==CapitalEminwonNoticePage.Site.GUNPO?"&Not_ancmt_se_code=01&notAncmtSeCd=01":"");
                var result=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,"https://"+site.host+"/www/selectEminwonList.do?key="+site.menu,url));
                if(selected.equals(page)){assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);assertThat(result.bodyText()).isEqualTo("사업자 지원금 대상 청년 수출기업 제외");}
                else{assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);assertThat(result.bodyText()).isNull();}
                assertThat(transport.callCount()).isEqualTo(1);
            }
        }
    }

    private ProviderContentResult chungjuResult(String query, String html) {
        var transport = new StubTransport(); transport.enqueue(html(html));
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE", SOURCE_ID,
                "https://www.chungju.go.kr/www/selectEminwonList.do?key=510", "https://www.chungju.go.kr/www/selectEminwonView.do" + query));
        assertThat(transport.callCount()).isEqualTo(1); return result;
    }
    private String chungjuHtml(String body) {
        return "<main><nav>수출 메뉴</nav><table class='bbs_default view'><tr><th>제목</th><td>중소기업 지원 공고</td></tr>"
                + "<tr><th>담당부서</th><td>메타데이터</td></tr><tr><th>내용</th><td title='내용' class='bbs_content'>" + body
                + "</td></tr><tr><th>파일</th><td>스타트업 신청서</td></tr></table><footer>기관 푸터</footer></main>";
    }
    @Test void chungjuUsesOnlyOwnBodyAndPreservesBusinessTablesAndExclusionContext() {
        var result = chungjuResult("?key=510&ancmt_mgt_no=72039&pageIndex=1&method=", chungjuHtml(
                "소상공인 지원금 <table><tr><th>지원대상</th><td>사업자</td></tr></table><nav>메뉴</nav>수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원대상 사업자 수출기업 제외 신청");
    }
    @Test void chungjuNeverSubstitutesMissingDuplicateOrNestedSelectorsWithPageText() {
        String valid = chungjuHtml("본문");
        for (String html : List.of("<main>본문</main>", valid + valid, valid.replace("bbs_default view", "changed"),
                valid.replace("bbs_content", "changed"), valid.replace("<th>내용</th>", "<th>변경</th>"),
                valid.replace("<th>제목</th><td>중소기업 지원 공고</td>", "<td><table><tr><th>제목</th><td>가짜 제목</td></tr></table></td>"))) {
            var result = chungjuResult("?key=510&ancmt_mgt_no=72039", html);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED); assertThat(result.bodyText()).isNull();
        }
    }
    @Test void chungjuOtherMenuAmbiguousQueryAndEmptyContentAreExplicitFailures() {
        for (String query : List.of("?key=509&ancmt_mgt_no=1", "?key=510&ancmt_mgt_no=0", "?key=510&ancmt_mgt_no=1&key=510",
                "?key=510&ancmt_mgt_no=1&unknown=1", "?key=510", "?key=510&ancmt_mgt_no=1&ancmt_sj=%0A"))
            assertThat(chungjuResult(query, chungjuHtml("본문")).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        assertThat(chungjuResult("?key=510&ancmt_mgt_no=72039", chungjuHtml("<nav>메뉴</nav>"))
                .failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    private static String bbsHtml(boolean compact, String body) {
        return "<main><header>수출 특허 메뉴</header><p>스타트업 관련 공고</p>"
                + (compact ? "<div class='p-wrap bbs bbs__view'><table class='p-table block'>" : "<table class='bbs_default view'>")
                + (compact ? "<tr><td><span class='p-table__subject_text'>지원사업 제목</span></td></tr>" : "<tr><th>제목</th><td>지원사업 제목</td></tr>")
                + "<tr><td title='내용'>" + body + "</td></tr>"
                + "<tr><th>파일</th><td><span>수출 특허 자료.pdf</span><a href='/www/downloadBbsFile.do?atchmnflNo=1'>다운로드</a></td></tr>"
                + "</table>" + (compact ? "</div>" : "") + "<footer>의회 감사 고시</footer></main>";
    }

    private static String wonjuHtml(String body) {
        return "<main><header>스타트업 메뉴</header><div class='bbs_wrap'><div class='p-wrap bbs bbs__view'>"
                + "<table class='p-table'><tr><th>제목</th><td>사업자 지원 공고</td></tr><tr><th>작성자</th><td>담당 부서</td></tr>"
                + "<tr><td title='내용'>" + body + "</td></tr><tr><th>파일</th><td>특허자료.hwpx</td></tr>"
                + "</table></div></div><footer>수출 관련 공고</footer></main>";
    }

    @Test void wonjuBodyKeepsOnlyMeasuredContentWithoutLosingNestedBusinessTable() {
        var result = bbsResult("www.wonju.go.kr", "?key=216&bbsNo=140&nttNo=123", wonjuHtml(
                "소상공인 지원금 <nav>메뉴</nav><table><tr><th>지원대상</th><td>사업자</td></tr></table>"
                        + "수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원대상 사업자 수출기업 제외 신청");
    }

    @Test void wonjuMissingOrAmbiguousStructureNeverUsesPageText() {
        String valid = wonjuHtml("지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("bbs_wrap", "changed"),
                valid.replace("class='p-table'", "class='changed'"), valid.replace("제목</th>", "변경</th>"),
                valid.replace("title='내용'", "title='변경'"), valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                valid.replace("<th>제목</th><td>사업자 지원 공고</td>", "<td><table><tr><th>제목</th><td>가짜 제목</td></tr></table></td>"))) {
            var result = bbsResult("www.wonju.go.kr", "?key=216&bbsNo=140&nttNo=123", page);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void wonjuQueryAndEmptyBodyRemainExplicitFailures() {
        for (String query : List.of("?key=216&bbsNo=140", "?key=216&bbsNo=140&nttNo=0", "?key=999&bbsNo=140&nttNo=123",
                "?key=216&bbsNo=999&nttNo=123", "?key=216&bbsNo=140&nttNo=123&bbsNo=140", "?key=216&bbsNo=140&nttNo=123&other=1"))
            assertThat(bbsResult("www.wonju.go.kr", query, wonjuHtml("본문")).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        assertThat(bbsResult("www.wonju.go.kr", "?key=216&bbsNo=140&nttNo=123", wonjuHtml("<nav>메뉴</nav>"))
                .failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    private ProviderContentResult bbsResult(String host, String suffix, String body) {
        var transport = new StubTransport(); transport.enqueue(html(body));
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, "https://" + host + "/www/selectBbsNttList.do",
                "https://" + host + "/www/selectBbsNttView.do" + suffix));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    private static String jecheonHtml(String body) {
        return wonjuHtml(body).replace("class='bbs_wrap'", "class='presentation'").replace("class='p-table'", "class='p-table block'");
    }

    @Test void jecheonBodyUsesOnlyMeasuredContentWithBlankPresentationId() {
        var result = bbsResult("www.jecheon.go.kr", "?key=5233&bbsNo=18&nttNo=123&id=", jecheonHtml(
                "소상공인 지원금 <nav>스타트업 메뉴</nav><table><tr><th>지원형태</th><td>보조금</td></tr></table>"
                        + "수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원형태 보조금 수출기업 제외 신청");
    }

    @Test void jecheonMissingOrNestedStructureDoesNotUsePageOrFileNames() {
        String valid = jecheonHtml("지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("p-table block", "p-table"),
                valid.replace("제목</th>", "변경</th>"), valid.replace("title='내용'", "title='변경'"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                valid.replace("<th>제목</th><td>사업자 지원 공고</td>", "<td><table><tr><th>제목</th><td>중첩 제목</td></tr></table></td>"))) {
            var result = bbsResult("www.jecheon.go.kr", "?key=5233&bbsNo=18&nttNo=123", page);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
        assertThat(bbsResult("www.jecheon.go.kr", "?key=5233&bbsNo=18&nttNo=123", jecheonHtml("<nav>메뉴</nav>"))
                .failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    @Test void jecheonOnlyPermitsEmptyPresentationIdAndExactBoard() {
        for (String suffix : List.of("&id=other", "&id=%20", "&id=&id=", "&id=&&", "&other=1", "&bbsNo=18"))
            assertThat(bbsResult("www.jecheon.go.kr", "?key=5233&bbsNo=18&nttNo=123" + suffix, jecheonHtml("본문"))
                    .failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        for (String query : List.of("?key=5233&bbsNo=18", "?key=5233&bbsNo=18&nttNo=0", "?key=5233&bbsNo=999&nttNo=123", "?key=999&bbsNo=18&nttNo=123"))
            assertThat(bbsResult("www.jecheon.go.kr", query, jecheonHtml("본문")).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        assertThat(bbsResult("www.wonju.go.kr", "?key=216&bbsNo=140&nttNo=123&id=", wonjuHtml("본문"))
                .failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    @Test void boeunKeepsBodyContextButNotMenusMetadataOrFileNames() {
        var result = bbsResult("www.boeun.go.kr", "?key=194&bbsNo=66&nttNo=123", bbsHtml(true,
                "소상공인 지원금 <nav>스타트업 메뉴</nav><table><tr><th>지원형태</th><td>보조금</td></tr></table>"
                        + "특허 보유 수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원형태 보조금 특허 보유 수출기업 제외 신청");
    }

    private static String yangpyeongHtml(String body) {
        return bbsHtml(true, body).replace("<td title='내용'>", "<th scope='row'>내용</th><td class='p-table__content'>");
    }

    @Test void yangpyeongUsesOnlyOwnedLabelledBodyAndPreservesActualKeywordContext() {
        String host = "www.yp21.go.kr", query = "?key=1119&bbsNo=5&nttNo=123";
        var result = bbsResult(host, query, yangpyeongHtml("소상공인 지원금 <nav>스타트업 메뉴</nav>"
                + "<table><tr><th>지원형태</th><td>보조금</td></tr></table>특허 보유 수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원형태 보조금 특허 보유 수출기업 제외 신청");
        String valid = yangpyeongHtml("지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("p-table block", "p-table"),
                valid.replace("p-table__subject_text", "changed"), valid.replace("내용</th>", "변경</th>"),
                valid.replace("p-table__content", "changed"), valid.replace("지원사업 본문</td>", "지원사업 본문</td><td>잘못된 형제</td>"),
                valid.replace("<th scope='row'>내용</th>", "<td><table><tr><th>내용</th></tr></table></td>"),
                valid.replace("<td class='p-table__content'>지원사업 본문</td>", "<td><table><tr><td class='p-table__content'>중첩 본문</td></tr></table></td>"),
                valid.replace("<span class='p-table__subject_text'>지원사업 제목</span>", "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>"))) {
            var failed = bbsResult(host, query, page); assertThat(failed.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(failed.bodyText()).isNull();
        }
        assertThat(bbsResult(host, query, yangpyeongHtml("<nav>메뉴</nav>")).failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        for (String invalid : List.of("?key=1119&bbsNo=5", "?key=1119&bbsNo=5&nttNo=0", "?key=999&bbsNo=5&nttNo=123",
                "?key=1119&bbsNo=999&nttNo=123", query + "&bbsNo=5", query + "&other=1"))
            assertThat(bbsResult(host, invalid, valid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        assertThat(bbsResult("www.oc.go.kr", "?key=236&bbsNo=40&nttNo=123", valid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    @Test void cheorwonBodyRequiresExactLegalBoardAndOwnedMarkers() {
        String host = "www.cwg.go.kr", query = "?key=1226&bbsNo=25&nttNo=288915";
        var result = bbsResult(host, query, bbsHtml(true, "소상공인 지원금 <nav>스타트업 메뉴</nav>"
                + "<table><tr><th>지원형태</th><td>보조금</td></tr></table>특허 보유 수출기업 제외"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원형태 보조금 특허 보유 수출기업 제외");
        String valid = bbsHtml(true, "지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("p-table block", "p-table"),
                valid.replace("p-table__subject_text", "changed"), valid.replace("title='내용'", "title='변경'"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                valid.replace("<span class='p-table__subject_text'>지원사업 제목</span>",
                        "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>"))) {
            var failed = bbsResult(host, query, page);
            assertThat(failed.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED); assertThat(failed.bodyText()).isNull();
        }
        assertThat(bbsResult(host, query, bbsHtml(true, "<nav>메뉴</nav>")).failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        for (String invalid : List.of("?key=1226&bbsNo=25", "?key=1226&bbsNo=25&nttNo=0", "?key=352&bbsNo=25&nttNo=288915",
                "?key=1226&bbsNo=24&nttNo=288915", query + "&bbsNo=25", query + "&other=1"))
            assertThat(bbsResult(host, invalid, valid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    @Test void okcheonKeepsActualBodyContextAndRejectsAmbiguousOrChangedBoard() {
        String host = "www.oc.go.kr", query = "?key=236&bbsNo=40&nttNo=123";
        var result = bbsResult(host, query, bbsHtml(true,
                "소상공인 지원금 <nav>스타트업 메뉴</nav><table><tr><th>지원형태</th><td>보조금</td></tr></table>"
                        + "특허 보유 수출기업 제외 <a href='/apply'>신청</a>"));
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 지원형태 보조금 특허 보유 수출기업 제외 신청");
        String valid = bbsHtml(true, "지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("p-table block", "p-table"),
                valid.replace("p-table__subject_text", "changed"), valid.replace("title='내용'", "title='변경'"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                valid.replace("</span></td>", "</span><span class='p-table__subject_text'></span></td>"),
                valid.replace("<span class='p-table__subject_text'>지원사업 제목</span>",
                        "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>"))) {
            var failed = bbsResult(host, query, page);
            assertThat(failed.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(failed.bodyText()).isNull();
        }
        assertThat(bbsResult(host, query, bbsHtml(true, "<nav>메뉴</nav>")).failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        for (String invalid : List.of("?key=236&bbsNo=40", "?key=236&bbsNo=40&nttNo=0", "?key=999&bbsNo=40&nttNo=123",
                "?key=236&bbsNo=999&nttNo=123", query + "&bbsNo=40", query + "&other=1"))
            assertThat(bbsResult(host, invalid, valid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    @Test void boeunMissingAmbiguousOrNestedMarkersNeverBecomeBodySuccess() {
        String valid = bbsHtml(true, "지원사업 본문");
        for (String page : List.of("<main>지원사업</main>", valid + valid, valid.replace("p-table block", "p-table"),
                valid.replace("p-table__subject_text", "changed"), valid.replace("title='내용'", "title='변경'"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                valid.replace("</span></td>", "</span><span class='p-table__subject_text'></span></td>"),
                valid.replace("<span class='p-table__subject_text'>지원사업 제목</span>",
                        "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>"))) {
            var result = bbsResult("www.boeun.go.kr", "?key=194&bbsNo=66&nttNo=123", page);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
        assertThat(bbsResult("www.boeun.go.kr", "?key=194&bbsNo=66&nttNo=123", bbsHtml(true, "<nav>메뉴</nav>"))
                .failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    @Test void boeunRejectsChangedBoardMissingIdentityAndUnknownQuery() {
        for (String query : List.of("?key=194&bbsNo=66", "?key=194&bbsNo=66&nttNo=0", "?key=999&bbsNo=66&nttNo=123",
                "?key=194&bbsNo=999&nttNo=123", "?key=194&bbsNo=66&nttNo=123&bbsNo=66", "?key=194&bbsNo=66&nttNo=123&other=1"))
            assertThat(bbsResult("www.boeun.go.kr", query, bbsHtml(true, "본문")).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    @Test void verifiedBbsModelsExtractOnlyOfficialBodyAndRetainActualExclusionContext() {
        String[][] sites = {{"www.taebaek.go.kr", "25"}, {"www.hsg.go.kr", "65"}, {"www.yw.go.kr", "17"}};
        for (var site : sites) {
            var result = bbsResult(site[0], "?bbsNo=" + site[1] + "&nttNo=42", bbsHtml("65".equals(site[1]),
                    "소상공인 지원금 <nav>투자유치 메뉴</nav> 수출기업 제외 <a href='/apply'>온라인 신청</a>"));
            assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
            assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 온라인 신청");
        }
    }

    @Test void verifiedBbsMissingOrAmbiguousStructureNeverFallsBackToPageText() {
        String valid = bbsHtml(false, "소상공인 지원금");
        for (String html : List.of("<main>소상공인 지원금</main>", valid + valid,
                valid.replace("title='내용'", "title='변경'"), valid.replace("제목</th>", "변경</th>"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"))) {
            var result = bbsResult("www.taebaek.go.kr", "?bbsNo=25&nttNo=42", html);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void verifiedBbsEmptyBodyCannotUseAttachmentFilenameAsBody() {
        var result = bbsResult("www.yw.go.kr", "?bbsNo=17&nttNo=42", bbsHtml(false, "<nav>메뉴</nav>"));
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(result.bodyText()).isNull();
    }

    @Test void verifiedBbsDuplicateOrMissingBoardParameterCannotSelectGenericFallback() {
        for (String query : List.of("?nttNo=42", "?bbsNo=25&bbsNo=99", "?bbsNo=25&%62bsNo=25", "?bbsNo", "?bbsNo=", "?bbsNo=25%20", "?bbsNo=025")) {
            var result = bbsResult("www.taebaek.go.kr", query, bbsHtml(false, "본문"));
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }

    @Test void unmeasuredHostOrBoardRetainsExistingGenericContract() {
        assertThat(bbsResult("another.example.go.kr", "?bbsNo=25", "<main>기존 본문</main>").bodyText()).isEqualTo("기존 본문");
        assertThat(bbsResult("www.taebaek.go.kr", "?bbsNo=999", "<main>다른 게시판</main>").bodyText()).isEqualTo("다른 게시판");
    }

    @Test void observedHoengseongSessionPathStillRequiresUniqueOfficialBody() {
        var result = bbsResult("www.hsg.go.kr", ";jsessionid=synthetic?bbsNo=65&nttNo=42", bbsHtml(true, "소상공인 지원금"));
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금");
        var duplicate = bbsResult("www.hsg.go.kr", "?bbsNo=65&nttNo=42", bbsHtml(true, "본문")
                .replace("</span>", "</span><span class='p-table__subject_text'></span>"));
        assertThat(duplicate.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    private static String seoguHtml(String body) {
        return "<main><header>수출 특허 메뉴</header><div class='card mb-4 program--view'>"
                + "<h2 class='card-header h2'>상세정보</h2><div class='card-body prog bucket-form'>"
                + "<span id='notAncmtMgtNo'>51668</span><span id='notAncmtSj'>소상공인 지원사업</span>"
                + "<span id='depNm'>기관정보</span><span id='chrNm'>담당자 대역</span><span id='telno'>연락처 대역</span>"
                + "<span id='notAncmtCn'>" + body + "</span>"
                + "<div class='bbs--view--file'><span>수출 특허 신청서.hwp</span><a href='/file/download'>다운로드</a></div>"
                + "</div></div><footer>의회 감사 고시</footer></main>";
    }

    private ProviderContentResult seoguResult(String pathAndQuery, String page) {
        var transport = new StubTransport(); transport.enqueue(html(page));
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, "https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do",
                "https://www.seogu.go.kr" + pathAndQuery));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    private ProviderContentResult seoguResult(String query, String page, boolean officialPath) {
        return seoguResult((officialPath ? "/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do" : "/another/view.do") + query, page);
    }

    @Test void observedSeoguCardExcludesStaffMenusAndFileNamesButPreservesBodyContext() {
        var result = seoguResult("?notAncmtMgtNo=51668", seoguHtml(
                "소상공인 지원금 <nav>투자유치 메뉴</nav> <span role='navigation'>특허 메뉴</span>"
                        + "수출기업 제외 <a href='/apply'>온라인 신청</a>"), true);
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 온라인 신청");
    }

    @Test void observedSeoguMissingAmbiguousOrChangedCardNeverUsesWholePageFallback() {
        String valid = seoguHtml("소상공인 지원금");
        for (String page : List.of("<main>소상공인 지원금</main>", valid + valid,
                valid.replace("id='notAncmtCn'", "id='changed'"),
                valid.replace("<span id='notAncmtCn'>", "<span id='notAncmtCn'>중복</span><span id='notAncmtCn'>"),
                valid.replace("<span id='notAncmtSj'>소상공인 지원사업</span>", "<span id='notAncmtSj'></span>"),
                valid.replace("<span id='notAncmtSj'>", "<span id='notAncmtSj'>중복</span><span id='notAncmtSj'>"),
                valid.replace("<span id='notAncmtMgtNo'>", "<span id='notAncmtMgtNo'>51668</span><span id='notAncmtMgtNo'>"),
                valid.replace("id='notAncmtMgtNo'>51668", "id='notAncmtMgtNo'>99999"))) {
            var result = seoguResult("?notAncmtMgtNo=51668", page, true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void observedSeoguInvalidIdentityQueryCannotFallBackToPageText() {
        for (String query : List.of("", "?notAncmtMgtNo=", "?notAncmtMgtNo=51668&notAncmtMgtNo=51668",
                "?notAncmtMgtNo=51668&other=1", "?notAncmtMgtNo=51668%20", "?notAncmtMgtNo=99999")) {
            var result = seoguResult(query, seoguHtml("소상공인 지원금"), true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void observedSeoguEmptyBodyCannotBecomeStaffOrAttachmentText() {
        var result = seoguResult("?notAncmtMgtNo=51668", seoguHtml("<nav>메뉴</nav>"), true);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(result.bodyText()).isNull();
    }

    @Test void otherSeoguBoardKeepsExistingContract() {
        assertThat(seoguResult("?notice=1", "<main>기존 본문</main>", false).bodyText()).isEqualTo("기존 본문");
    }

    private static final String SAEOL_QUERY = "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=42&subCheck=Y";

    private static String saeolHtml(boolean namgu, String body) {
        return "<main><header>수출 특허 메뉴</header><form name='form1' method='post'><table class='"
                + (namgu ? "table_03" : "bbsView") + "'>"
                + (namgu ? "<tr><th colspan='4'>소상공인 지원사업</th></tr>" : "<tr><th>제목</th><td colspan='3'>소상공인 지원사업</td></tr>")
                + "<tr><th>담당부서</th><td>기관 대역</td><th>연락처</th><td>연락처 대역</td></tr>"
                + (namgu ? "<tr><td colspan='4'><div class='view01_con'>" : "<tr><td colspan='4' class='con l'>")
                + body + (namgu ? "</div>" : "") + "</td></tr>"
                + "<tr><th>첨부파일</th><td colspan='3'><a href='/FileDown.jsp'>수출 특허.hwp</a></td></tr>"
                + "</table></form><footer>고시 의회 감사</footer></main>";
    }

    private ProviderContentResult saeolResult(String host, String query, String page) {
        var transport = new StubTransport(); transport.enqueue(html(page));
        String base = "https://" + host + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, base, base + query));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    @Test void observedSaeolModelsRetainOnlyBodyIncludingRealExclusionContextAndApplicationLink() {
        for (boolean namgu : List.of(true, false)) {
            String host = namgu ? "eminwon.bsnamgu.go.kr" : "eminwon.dalseong.daegu.kr";
            var result = saeolResult(host, SAEOL_QUERY, saeolHtml(namgu,
                    "소상공인 지원금 <nav>투자유치 메뉴</nav> 수출기업 제외 <a href='/apply'>신청</a>"));
            assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
            assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 신청");
        }
    }

    @Test void observedSaeolModelsRejectMissingDuplicateOrChangedStructures() {
        for (boolean namgu : List.of(true, false)) {
            String host = namgu ? "eminwon.bsnamgu.go.kr" : "eminwon.dalseong.daegu.kr";
            String valid = saeolHtml(namgu, "소상공인 지원금");
            for (String page : List.of("<main>다른 페이지</main>", valid + valid,
                    valid.replace("name='form1'", "name='changed'"), valid.replace("method='post'", "method='get'"),
                    valid.replace(namgu ? "table_03" : "bbsView", "changed"),
                    valid.replace(namgu ? "view01_con" : "con l", "changed"),
                    valid.replace("소상공인 지원사업", ""),
                    valid.replace(namgu ? "<th colspan='4'>" : "<th>제목</th>", namgu
                            ? "<th colspan='4'>중복</th><th colspan='4'>" : "<th>제목</th><td>중복</td><th>제목</th>"))) {
                var result = saeolResult(host, SAEOL_QUERY, page);
                assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
                assertThat(result.bodyText()).isNull();
            }
        }
    }

    @Test void observedSaeolModelsRejectDifferentActionsAndAmbiguousQueries() {
        for (String host : List.of("eminwon.bsnamgu.go.kr", "eminwon.dalseong.daegu.kr", "eminwon.jung.daegu.kr", "eminwon.haman.go.kr"))
            for (String query : List.of("", SAEOL_QUERY + "&not_ancmt_mgt_no=43", SAEOL_QUERY + "&extra=1",
                    SAEOL_QUERY.replace("selectOfrNotAncmtRegst", "otherAction"), SAEOL_QUERY.replace("subCheck=Y", "subCheck=N"),
                    SAEOL_QUERY.replace("not_ancmt_mgt_no=42", "not_ancmt_mgt_no=x"), SAEOL_QUERY.replace("context=NTIS", "context=OTHER"))) {
                var result = saeolResult(host, query, saeolHtml(host.contains("bsnamgu"), "본문"));
                assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
                assertThat(result.bodyText()).isNull();
            }
    }

    @Test void observedSaeolEmptyBodyDoesNotUseMetadataOrAttachmentName() {
        for (String host : List.of("eminwon.bsnamgu.go.kr", "eminwon.dalseong.daegu.kr")) {
            var result = saeolResult(host, SAEOL_QUERY, saeolHtml(host.contains("bsnamgu"), "<nav>메뉴</nav>"));
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void unmeasuredSaeolHostKeepsExistingBodyContract() {
        assertThat(saeolResult("another.example.go.kr", SAEOL_QUERY, "<main>기존 본문</main>").bodyText()).isEqualTo("기존 본문");
    }

    private static String saeolPlainCellHtml(boolean junggu, String body) {
        String heading = junggu ? "th" : "td";
        return "<main>수출 메뉴<form name='form1' method='post'><table "
                + (junggu ? "class='boardView'" : "width='100%' border='0' cellspacing='1' cellpadding='0'") + ">"
                + "<tr><"+heading+">제목</"+heading+"><td>지원사업 제목</td><"+heading+">담당부서</"+heading+"><td>기관 대역</td></tr>"
                + "<tr><td colspan='4' height='1'></td></tr><tr><td colspan='4' style='word-break:break-all;'>"+body+"</td></tr>"
                + "<tr><td colspan='4'><div class='tal'>첨부파일 <a href='/FileDown.jsp'>수출 특허.hwp</a></div></td></tr>"
                + "</table></form><footer>고시 의회 감사</footer></main>";
    }

    @Test void hwasunReusesMeasuredPlainBodyBoundaryWithoutRelaxingRegisteredHost() {
        String valid=saeolPlainCellHtml(false,"청년 지원금 <nav>메뉴</nav>");
        var result=saeolResult("eminwon.hwasun.go.kr",SAEOL_QUERY,valid);
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("청년 지원금");
        for(String invalid:List.of(valid+valid,valid.replace("word-break:break-all;","word-break:normal;"),valid.replace("name='form1'","name='other'")))
            assertThat(saeolResult("eminwon.hwasun.go.kr",SAEOL_QUERY,invalid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        var transport=new StubTransport();
        var mismatch=client(true,transport,publicValidator()).selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",SOURCE_ID,
                "https://www.hwasun.go.kr/contents.do?S=S01&M=020104000000",
                "https://eminwon.hwasun.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?"+SAEOL_QUERY));
        assertThat(mismatch.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
    }

    @Test void measuredPlainCellsExcludeMetadataAndPreserveNestedBodyAndRealKeywordContext() {
        for (boolean junggu : List.of(true,false)) {
            String host = junggu ? "eminwon.jung.daegu.kr" : "eminwon.haman.go.kr";
            var result = saeolResult(host,SAEOL_QUERY,saeolPlainCellHtml(junggu,
                    "소상공인 지원금 <nav>수출 메뉴</nav><table><tr><td colspan='4' style='word-break:break-all;'>"
                            + "수출기업 제외</td></tr></table><a href='/apply'>신청</a>"));
            assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
            assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 신청");
        }
    }

    @Test void measuredPlainCellsFailClosedForMissingDuplicatedOrChangedBodyMarkers() {
        for (boolean junggu : List.of(true,false)) {
            String host = junggu ? "eminwon.jung.daegu.kr" : "eminwon.haman.go.kr";
            String valid = saeolPlainCellHtml(junggu,"소상공인 지원금");
            String heading = junggu ? "th" : "td";
            for (String page : List.of("<main>다른 화면</main>", valid+valid,
                    valid.replace("name='form1'","name='changed'"), valid.replace("method='post'","method='get'"),
                    valid.replace(junggu ? "class='boardView'" : "cellspacing='1'", junggu ? "class='changed'" : "cellspacing='2'"),
                    valid.replace("style='word-break:break-all;'","style='word-break:normal;'"),
                    valid.replace("style='word-break:break-all;'",""), valid.replace("지원사업 제목",""),
                    valid.replace("<"+heading+">제목</"+heading+">", "<"+heading+">변경된 항목</"+heading+">"),
                    valid.replace("<td colspan='4' height='1'></td>","<td colspan='4' style='word-break:break-all;'>중복 본문</td>"))) {
                var result=saeolResult(host,SAEOL_QUERY,page);
                assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
                assertThat(result.bodyText()).isNull();
            }
        }
    }

    @Test void measuredPlainCellEmptyBodyCannotFallBackToAttachmentsOrSurroundings() {
        for (boolean junggu : List.of(true,false)) {
            var result=saeolResult(junggu ? "eminwon.jung.daegu.kr" : "eminwon.haman.go.kr",SAEOL_QUERY,
                    saeolPlainCellHtml(junggu,"<nav>메뉴</nav><a href='/FileDown.jsp'>첨부파일.hwp</a>"));
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
            assertThat(result.bodyText()).isNull();
        }
    }

    private static String busanBodyHtml(String body) {
        return "<main><header>수출 특허 메뉴</header><div class='boardView'>"
                + "<div class='form-group'><h4 class='form-data-subject'>소상공인 지원사업</h4></div>"
                + "<div class='form-group'><dl class='form-data-info'><dt>담당자</dt><dd>기관 대역</dd></dl></div>"
                + "<div class='form-group'><dl class='form-data-info'><dt>첨부파일</dt><dd><a href='/nbgosi/download?fileId=F123&seq=1'>특허.hwp</a></dd></dl></div>"
                + "<div class='form-group'><dl class='form-data-content'><dt><span>내용</span></dt><dd>" + body
                + "</dd></dl></div></div><footer>행정 공고</footer></main>";
    }

    private static String gangbukBodyHtml(String body) {
        return "<main><header>수출 특허 메뉴</header><form id='board'><input type='hidden' name='nttId' value='42'>"
                + "<div class='bd-view'><h3 class='bd-view__subject'>소상공인 지원사업</h3>"
                + "<div class='table-dl'><dl><dt>담당자</dt><dd>기관 대역</dd></dl>"
                + "<dl class='file-lists'><dt>첨부</dt><dd class='item'><a href='/FileDown.jsp'>특허.hwp</a></dd></dl></div>"
                + "<dl><dd>" + body + "</dd></dl><div class='opentype'><dl><dd>공공누리 안내</dd></dl></div>"
                + "</div></form><footer>행정 공고</footer></main>";
    }

    private ProviderContentResult legalBodyResult(boolean busan, String query, String page, boolean officialPath) {
        var transport = new StubTransport(); transport.enqueue(html(page));
        String host = busan ? "www.busan.go.kr" : "child.gangbuk.go.kr";
        String path = officialPath ? (busan ? "/nbgosi/view" : "/portal/bbs/B0000245/view.do") : "/other/view.do";
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, "https://" + host, "https://" + host + path + query));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    @Test void busanBodyUsesOnlyUniqueContentDefinitionAndRetainsRealExclusionContext() {
        var result = legalBodyResult(true, "?sno=42&gosiGbn=A&curPage=1", busanBodyHtml(
                "소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table><a href='/apply'>신청</a><nav>특허 메뉴</nav>"), true);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 신청");
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
    }

    @Test void busanBodyRejectsMissingDuplicatedAndChangedDefinitions() {
        String valid = busanBodyHtml("소상공인 지원금");
        for (String page : List.of(valid.replace("class='boardView'", "class='changed'"),
                valid.replace("class='form-data-subject'", "class='changed'"), valid.replace("class='form-data-content'", "class='changed'"),
                valid.replace("<span>내용</span>", "<span>변경</span>"),
                valid.replace("<dd>소상공인 지원금</dd>", "<dd>소상공인 지원금</dd><dd>추가</dd>"),
                valid.replace("<h4 class='form-data-subject'>소상공인 지원사업</h4>", ""),
                valid + valid, valid.replace("소상공인 지원사업", " "))) {
            var result = legalBodyResult(true, "?sno=42&gosiGbn=A", page, true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void busanBodyRejectsUnmeasuredActionsAndAmbiguousQueryValues() {
        for (String query : List.of("", "?sno=42", "?sno=42&gosiGbn=B", "?sno=x&gosiGbn=A", "?sno=42&sno=43&gosiGbn=A",
                "?sno=42&gosiGbn=A&extra=1", "?sno=42&gosiGbn=A&curPage=0", "?sno=42&gosiGbn=A&curPage=1&curPage=2")) {
            var result = legalBodyResult(true, query, busanBodyHtml("소상공인 지원금"), true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
        }
    }

    @Test void gangbukBodyMatchesHiddenNoticeIdAndExcludesMetadataAndLicenceText() {
        var result = legalBodyResult(false, "?menuNo=200082&nttId=42", gangbukBodyHtml(
                "소상공인 지원금 <dl><dt>수출기업</dt><dd>제외</dd></dl><a href='/apply'>신청</a>"), true);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 신청");
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
    }

    @Test void gangbukBodyRejectsNoticeMismatchAndMissingDuplicateOrNestedOnlyContainers() {
        String valid = gangbukBodyHtml("소상공인 지원금");
        for (String page : List.of(valid.replace("value='42'", "value='43'"), valid.replace("name='nttId'", "name='changed'"),
                valid.replace("type='hidden'", "type='text'"),
                valid.replace("<input type='hidden' name='nttId' value='42'>", "<div><input type='hidden' name='nttId' value='42'></div>"),
                valid.replace("<div class='bd-view'>", "<input type='hidden' name='nttId' value='42'><div class='bd-view'>"),
                valid.replace("class='bd-view__subject'", "class='changed'"), valid.replace("소상공인 지원사업", " "),
                valid.replace("<dl><dd>소상공인 지원금</dd></dl>", ""),
                valid.replace("<dl><dd>소상공인 지원금</dd></dl>", "<div><dl><dd>소상공인 지원금</dd></dl></div>"),
                valid.replace("<dl><dd>소상공인 지원금</dd></dl>", "<dl><dd>소상공인 지원금</dd></dl><dl><dd>추가</dd></dl>"), valid + valid)) {
            var result = legalBodyResult(false, "?menuNo=200082&nttId=42", page, true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void gangbukBodyRejectsDifferentBoardAndAmbiguousNoticeQueries() {
        for (String query : List.of("", "?menuNo=200082", "?menuNo=200083&nttId=42", "?menuNo=200082&nttId=x",
                "?menuNo=200082&nttId=42&nttId=43", "?menuNo=200082&nttId=42&extra=1")) {
            var result = legalBodyResult(false, query, gangbukBodyHtml("소상공인 지원금"), true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
        }
    }

    @Test void legalBoardEmptyBodiesCannotBecomeMetadataAndOtherPathsKeepExistingContract() {
        for (boolean busan : List.of(true, false)) {
            String query = busan ? "?sno=42&gosiGbn=A" : "?menuNo=200082&nttId=42";
            String body = "<nav>메뉴</nav><a href='/FileDown.jsp'>지원사업.hwp</a>";
            var result = legalBodyResult(busan, query, busan ? busanBodyHtml(body) : gangbukBodyHtml(body), true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
            assertThat(result.bodyText()).isNull();
            assertThat(legalBodyResult(busan, "?other=42", "<main>기존 다른 게시판 본문</main>", false).bodyText())
                    .isEqualTo("기존 다른 게시판 본문");
        }
    }

    private static String hwacheonBodyHtml(String body) {
        return saeolPlainCellHtml(false, body).replace("<td>제목</td>", "<th>제목</th>");
    }

    @Test void hwacheonBodyUsesThLabelAndSubCheckNWithNoAttachmentTableContamination() {
        var result = saeolResult("eminwon.ihc.go.kr", SAEOL_QUERY.replace("subCheck=Y", "subCheck=N"), hwacheonBodyHtml(
                "소상공인 지원금 <table><tr><td>수출기업 제외</td></tr></table><a href='/apply'>신청</a>"));
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 신청");
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
    }

    @Test void hwacheonBodyRejectsOtherActionAndMissingDuplicatedOrChangedCells() {
        String valid = hwacheonBodyHtml("소상공인 지원금");
        for (String page : List.of(valid.replace("<th>제목</th>", "<td>제목</td>"), valid.replace("cellpadding='0'", "cellpadding='1'"),
                valid.replace("word-break:break-all;", "color:red;"), valid.replace("name='form1'", "name='changed'"), valid + valid)) {
            var result = saeolResult("eminwon.ihc.go.kr", SAEOL_QUERY.replace("subCheck=Y", "subCheck=N"), page);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
        assertThat(saeolResult("eminwon.ihc.go.kr", SAEOL_QUERY, valid).failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        var empty = saeolResult("eminwon.ihc.go.kr", SAEOL_QUERY.replace("subCheck=Y", "subCheck=N"),
                hwacheonBodyHtml("<nav>메뉴</nav><a href='/FileDown.jsp'>지원사업.hwp</a>"));
        assertThat(empty.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(empty.bodyText()).isNull();
    }

    @Test
    void selectContentDoesNothingWhileFeatureFlagIsOff() {
        AtomicInteger resolutionCount = new AtomicInteger();
        StubTransport transport = new StubTransport();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(host -> {
            resolutionCount.incrementAndGet();
            return new InetAddress[]{publicAddress()};
        });
        LocalGovernmentNoticeProviderContentClient client = client(false, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.DISABLED);
        assertThat(result.bodySourceCode()).isEqualTo(BodySourceCode.NONE);
        assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.UNSUPPORTED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.FEATURE_DISABLED);
        assertThat(transport.callCount()).isZero();
        assertThat(resolutionCount).hasValue(0);
    }

    @Test
    void selectContentExtractsStaticHtmlWithoutExecutingScriptOrFollowingLinks() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <main>소상공인 지원금 안내</main>
                        <a href="/files/notice.pdf">첨부파일</a>
                        <script>fetch('https://outside.example/track')</script>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodySourceCode()).isEqualTo(BodySourceCode.DETAIL_PAGE_TEXT);
        assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 안내");
        assertThat(result.bodyText()).doesNotContain("첨부파일", "fetch", "outside.example");
        assertThat(result.finalUrl()).isEqualTo(DETAIL_URL);
        assertThat(result.attemptCount()).isEqualTo(1);
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
        assertThat(transport.lastPinnedAddresses()).containsExactly(publicAddress());
        assertThat(transport.lastReadTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(transport.lastMaxResponseBytes()).isEqualTo(TWO_MEBIBYTES);
    }

    @Test
    void selectContentExcludesAttachmentNamesFromClassificationBodyEvidence() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <main>
                        <p>소상공인 지원금 본문</p>
                        <section class="attachment-list">
                        <span>첨부파일</span>
                        <span>수출자료.pdf</span>
                        <a href="/download?fileId=101">다운로드</a>
                        </section>
                        <a download href="/storage/102">수출 통계</a>
                        <a href="/files/research.hwp">R&amp;D자료.hwp</a>
                        <a href="/attachment/download?fileId=103">제조 기술 자료</a>
                        <a href="#" onclick="downloadFile('104')">특허자료.docx</a>
                        <a href="/notices/42/details">상세보기</a>
                        <a href="/apply?noticeId=42">온라인 신청</a>
                        </main>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 상세보기 온라인 신청");
        assertThat(result.bodyText()).doesNotContain(
                "수출자료.pdf",
                "R&D자료.hwp",
                "제조 기술 자료",
                "특허자료.docx",
                "수출자료",
                "R&D자료",
                "수출 통계",
                "첨부파일"
        );
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentRejectsArbitraryDetailHostBeforeTransport() {
        StubTransport transport = new StubTransport();
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());
        ProviderContentRequest request = new ProviderContentRequest(
                "LOCAL_GOV_NOTICE",
                SOURCE_ID,
                REGISTERED_URL,
                "https://outside.example/notices/42"
        );

        ProviderContentResult result = client.selectContent(request);

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentBlocksPrivateAddressBeforeTransport() {
        StubTransport transport = new StubTransport();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(
                host -> new InetAddress[]{address(10, 0, 0, 7)}
        );
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.ADDRESS_BLOCKED);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentRechecksDnsImmediatelyBeforeTransport() {
        StubTransport transport = new StubTransport();
        AtomicInteger resolutionCount = new AtomicInteger();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(host -> {
            if (resolutionCount.incrementAndGet() < 3) {
                return new InetAddress[]{publicAddress()};
            }
            return new InetAddress[]{address(10, 0, 0, 8)};
        });
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.ADDRESS_BLOCKED);
        assertThat(resolutionCount).hasValue(3);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentUsesSemanticMainAreaInsteadOfNavigationAndFooter() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <nav>해외진출 수출바우처 메뉴</nav>
                        <main><h1>소상공인 정책자금 안내</h1><p>신청 대상과 지원 내용입니다.</p></main>
                        <footer>기관 채용공고</footer>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 정책자금 안내 신청 대상과 지원 내용입니다.");
        assertThat(result.bodyText()).doesNotContain("수출바우처", "채용공고");
    }

    @Test
    void selectContentExcludesNestedNavigationButKeepsActualNoticeTermsAndApplicationLinks() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("""
                <main>
                <nav><a href="/exports">수출바우처 메뉴</a></nav>
                <div role="navigation"><a href="/patents">특허 메뉴</a></div>
                <p>소상공인 지원금 본문</p>
                <p>수출기업은 지원 대상에서 제외합니다.</p>
                <a href="/apply?noticeId=42">온라인 신청</a>
                </main>
                """));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 수출기업은 지원 대상에서 제외합니다. 온라인 신청");
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentExcludesNavigationWhenOnlyBodyFallbackIsAvailable() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("""
                <body>
                <nav>수출바우처 메뉴</nav>
                <div role="navigation">기술창업 메뉴</div>
                <section role="note">소상공인 지원금 본문</section>
                <p>탐색 메뉴 변경 안내도 본문 문장이면 보존합니다.</p>
                </body>
                """));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 탐색 메뉴 변경 안내도 본문 문장이면 보존합니다.");
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentDoesNotTreatNavigationOnlyPageAsAvailableBody() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("<main><nav>소상공인 지원금</nav><div role='navigation'>융자 지원</div></main>"));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentDoesNotFallBackToNavigationWhenSemanticAreaIsEmpty() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <nav>소상공인 수출바우처 메뉴</nav>
                        <main><a href="/files/export.pdf">수출자료.pdf</a></main>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    @Test
    void selectContentAllowsThreeSameHostRedirects() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("/notices/43"));
        transport.enqueue(redirect("/notices/44"));
        transport.enqueue(redirect("/notices/45"));
        transport.enqueue(html("<html><body>소상공인 정책자금</body></html>"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.redirectCount()).isEqualTo(3);
        assertThat(result.finalUrl()).isEqualTo("https://" + HOST + "/notices/45");
        assertThat(transport.callCount()).isEqualTo(4);
    }

    @Test
    void selectContentRejectsFourthRedirect() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("/notices/43"));
        transport.enqueue(redirect("/notices/44"));
        transport.enqueue(redirect("/notices/45"));
        transport.enqueue(redirect("/notices/46"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.REDIRECT_LIMIT_EXCEEDED);
        assertThat(result.redirectCount()).isEqualTo(3);
        assertThat(transport.callCount()).isEqualTo(4);
    }

    @Test
    void selectContentRejectsCrossHostRedirectWithoutFollowingIt() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("https://outside.example/notices/42"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        assertThat(transport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentRetriesTimeoutOnce() {
        StubTransport transport = new StubTransport();
        transport.enqueue(new TimeoutException("stub timeout"));
        transport.enqueue(html("<html><body>청년 지원사업</body></html>"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(transport.callCount()).isEqualTo(2);
    }

    @Test
    void selectContentRetriesServerErrorOnlyOnce() {
        StubTransport transport = new StubTransport();
        transport.enqueue(response(503, "text/html", "일시 오류".getBytes(StandardCharsets.UTF_8)));
        transport.enqueue(response(503, "text/html", "반복 오류".getBytes(StandardCharsets.UTF_8)));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.HTTP_SERVER_ERROR);
        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(transport.callCount()).isEqualTo(2);
    }

    @Test
    void selectContentDoesNotRetryClientErrorOrNetworkFailure() {
        StubTransport clientErrorTransport = new StubTransport();
        clientErrorTransport.enqueue(response(404, "text/html", new byte[0]));
        StubTransport networkTransport = new StubTransport();
        networkTransport.enqueue(new IOException("stub connection failure"));

        ProviderContentResult clientError = client(
                true,
                clientErrorTransport,
                publicValidator()
        ).selectContent(request());
        ProviderContentResult networkError = client(
                true,
                networkTransport,
                publicValidator()
        ).selectContent(request());

        assertThat(clientError.failureCode()).isEqualTo(FailureCode.HTTP_STATUS_ERROR);
        assertThat(clientErrorTransport.callCount()).isEqualTo(1);
        assertThat(networkError.failureCode()).isEqualTo(FailureCode.NETWORK_ERROR);
        assertThat(networkTransport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentRejectsNonHtmlAndOversizedResponse() {
        StubTransport nonHtmlTransport = new StubTransport();
        nonHtmlTransport.enqueue(response(200, "application/pdf", new byte[]{1, 2, 3}));
        StubTransport oversizedTransport = new StubTransport();
        oversizedTransport.enqueue(response(200, "text/html", new byte[1025]));

        ProviderContentResult nonHtml = client(
                true,
                nonHtmlTransport,
                publicValidator()
        ).selectContent(request());
        ProviderContentResult oversized = client(
                true,
                1024,
                oversizedTransport,
                publicValidator()
        ).selectContent(request());

        assertThat(nonHtml.failureCode()).isEqualTo(FailureCode.CONTENT_TYPE_UNSUPPORTED);
        assertThat(oversized.failureCode()).isEqualTo(FailureCode.RESPONSE_TOO_LARGE);
    }

    @Test
    void selectContentDecodesMs949DeclaredByHtmlMeta() {
        Charset ms949 = Charset.forName("MS949");
        byte[] body = """
                <html><head><meta charset="MS949"></head>
                <body>소상공인 보조금 안내</body></html>
                """.getBytes(ms949);
        StubTransport transport = new StubTransport();
        transport.enqueue(response(200, "text/html", body));

        ProviderContentResult result = client(
                true,
                transport,
                publicValidator()
        ).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 보조금 안내");
    }

    @Test
    void selectContentEnforcesSizeLimitAfterGzipDecompression() {
        StubTransport transport = new StubTransport();
        transport.enqueue(gzipHtml("<html><body>" + "소상공인".repeat(1000) + "</body></html>"));

        ProviderContentResult result = client(
                true,
                1024,
                transport,
                publicValidator()
        ).selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.RESPONSE_TOO_LARGE);
        assertThat(transport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentLimitsSameHostConcurrencyToTwo() throws Exception {
        BlockingTransport transport = new BlockingTransport();
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<ProviderContentResult>> futures = List.of(
                    executor.submit(() -> client.selectContent(request("101"))),
                    executor.submit(() -> client.selectContent(request("102"))),
                    executor.submit(() -> client.selectContent(request("103")))
            );

            assertThat(transport.awaitFirstTwo()).isTrue();
            assertThat(transport.callCount()).isEqualTo(2);
            assertThat(transport.maxActive()).isEqualTo(2);
            transport.release();

            for (Future<ProviderContentResult> future : futures) {
                assertThat(future.get(3, TimeUnit.SECONDS).statusCode()).isEqualTo(StatusCode.AVAILABLE);
            }
            assertThat(transport.callCount()).isEqualTo(3);
            assertThat(transport.maxActive()).isEqualTo(2);
        } finally {
            transport.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }

    private LocalGovernmentNoticeProviderContentClient client(
            boolean enabled,
            ProviderContentHttpTransport transport,
            ProviderContentUrlValidator validator
    ) {
        return client(enabled, TWO_MEBIBYTES, transport, validator);
    }

    private LocalGovernmentNoticeProviderContentClient client(
            boolean enabled,
            int maxResponseBytes,
            ProviderContentHttpTransport transport,
            ProviderContentUrlValidator validator
    ) {
        return new LocalGovernmentNoticeProviderContentClient(
                enabled,
                Duration.ofSeconds(7),
                maxResponseBytes,
                3,
                2,
                "saneB-test-client/1.0",
                transport,
                validator
        );
    }

    private ProviderContentUrlValidator publicValidator() {
        return new ProviderContentUrlValidator(host -> new InetAddress[]{publicAddress()});
    }

    private ProviderContentRequest request() {
        return request("42");
    }

    private ProviderContentRequest request(String detailId) {
        return new ProviderContentRequest(
                "LOCAL_GOV_NOTICE",
                SOURCE_ID,
                REGISTERED_URL,
                "https://" + HOST + "/notices/" + detailId
        );
    }

    private static ProviderContentHttpResponse html(String html) {
        return response(200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private static ProviderContentHttpResponse redirect(String location) {
        return new ProviderContentHttpResponse(
                302,
                Map.of("Location", List.of(location)),
                new byte[0]
        );
    }

    private static ProviderContentHttpResponse response(int status, String contentType, byte[] body) {
        return new ProviderContentHttpResponse(
                status,
                Map.of("Content-Type", List.of(contentType)),
                body
        );
    }

    private static ProviderContentHttpResponse gzipHtml(String html) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(html.getBytes(StandardCharsets.UTF_8));
            }
            return new ProviderContentHttpResponse(
                    200,
                    Map.of(
                            "Content-Type", List.of("text/html; charset=UTF-8"),
                            "Content-Encoding", List.of("gzip")
                    ),
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static InetAddress publicAddress() {
        return address(203, 0, 113, 10);
    }

    private static InetAddress address(int first, int second, int third, int fourth) {
        try {
            return InetAddress.getByAddress(new byte[]{
                    (byte) first,
                    (byte) second,
                    (byte) third,
                    (byte) fourth
            });
        } catch (UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class StubTransport implements ProviderContentHttpTransport {

        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<ProviderContentRequestTarget> requestTargets = new ArrayList<>();
        private Duration lastReadTimeout;
        private int lastMaxResponseBytes;

        private void enqueue(Object outcome) {
            outcomes.addLast(outcome);
        }

        @Override
        public ProviderContentHttpResponse selectResponse(
                ProviderContentRequestTarget requestTarget,
                Duration readTimeout,
                int maxResponseBytes,
                String userAgent
        ) throws IOException, TimeoutException {
            requestTargets.add(requestTarget);
            lastReadTimeout = readTimeout;
            lastMaxResponseBytes = maxResponseBytes;
            Object outcome = outcomes.removeFirst();
            if (outcome instanceof IOException ioException) {
                throw ioException;
            }
            if (outcome instanceof TimeoutException timeoutException) {
                throw timeoutException;
            }
            return (ProviderContentHttpResponse) outcome;
        }

        private int callCount() {
            return requestTargets.size();
        }

        private List<URI> requestUris() {
            return requestTargets.stream().map(ProviderContentRequestTarget::uri).toList();
        }

        private List<InetAddress> lastPinnedAddresses() {
            return requestTargets.getLast().pinnedAddresses();
        }

        private Duration lastReadTimeout() {
            return lastReadTimeout;
        }

        private int lastMaxResponseBytes() {
            return lastMaxResponseBytes;
        }
    }

    private static final class BlockingTransport implements ProviderContentHttpTransport {

        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final AtomicInteger callCount = new AtomicInteger();
        private final CountDownLatch firstTwo = new CountDownLatch(2);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public ProviderContentHttpResponse selectResponse(
                ProviderContentRequestTarget requestTarget,
                Duration readTimeout,
                int maxResponseBytes,
                String userAgent
        ) throws InterruptedException {
            int currentActive = active.incrementAndGet();
            maxActive.accumulateAndGet(currentActive, Math::max);
            callCount.incrementAndGet();
            firstTwo.countDown();
            try {
                release.await();
                return html("<html><body>소상공인 지원사업</body></html>");
            } finally {
                active.decrementAndGet();
            }
        }

        private boolean awaitFirstTwo() throws InterruptedException {
            return firstTwo.await(3, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }

        private int callCount() {
            return callCount.get();
        }

        private int maxActive() {
            return maxActive.get();
        }
    }
}
