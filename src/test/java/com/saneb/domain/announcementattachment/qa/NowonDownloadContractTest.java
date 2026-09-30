package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.NowonNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class NowonDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=NowonDownloadCases.selectCase();
    private String item(int id,String ext){String uuid="00000000-0000-0000-0000-"+String.format("%012d",id),url="/component/file/ND_fileDownload.do?q_fileSn=308827&amp;q_fileId="+uuid;return "<li><a href='"+url+"'><i class='ico ico-file'></i><span>공고."+ext+"</span></a><span class=file-size>49 KB</span><button type=button class=btn-preveal onclick=\"opPreviewFile('"+uuid+"','"+url+"');\">미리보기</button></li>";}
    private String page(String items){return "<nav>수출 기관</nav><div class=article-view><h1 class=article-subject>청년 응시료 지원사업</h1><table class=table-article><tbody><tr><th scope=row>첨부파일</th><td colspan=3><ul class=file-list>"+items+"</ul></td></tr></tbody></table><div class=article-body><div class=txt>청년 지원금<img src='/component/file/ND_fileDownload.do?q_fileSn=1&amp;q_fileId=00000000-0000-0000-0000-000000000999'></div></div></div>";}
    @Test void preservesSupportedFilesAndRecordsUnsupportedAndUnresolvedSeparately(){
        var p=sample.profile();var result=p.selectDescriptors(sample.source(),page(item(1,"hwp")+item(2,"png")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(2);assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(result.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String bad:List.of("<script>loadOtherFiles()</script>","<a href='/unknown'>추가 파일</a>",item(3,"pdf").replace("href='/component","href='https://evil.example/component"),item(3,"pdf").replace("q_fileSn=308827","q_fileSn=308827&amp;extra=1"))){var partial=p.selectDescriptors(sample.source(),page(item(1,"hwp")+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);}
    }
    @Test void distinguishesConfirmedAbsenceMissingAreaDuplicatesAndLimit(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(sample.source(),"<p>error</p>").complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"pdf")+item(1,"pdf"))).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"pdf")+item(1,"pdf").replace("공고.pdf","다른.pdf"))).complete()).isFalse();
        var limit=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"pdf"))+page(item(1,"pdf"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("").replace("첨부파일","파일 없음")).complete()).isFalse();
    }
    @Test void previewIsOnlyIgnoredWhenPairedWithTheExactFile(){
        var p=sample.profile();String one=item(1,"hwp");
        for(String bad:List.of(one.replace("opPreviewFile('","opPreviewFile('9"),one.replace("미리보기","실행"),one.replace("<button type=button","<button type=submit"),one.replace("<span>공고", "<span onclick='run()'>공고"))){var result=p.selectDescriptors(sample.source(),page(bad));assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);}
    }
    @Test void sourceIdentityQueryAndTransportStayBounded(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url.replace("q_bbsCode=1003","q_bbsCode=1001"),url+"&q_bbsCode=1003",url+"&extra=1",url.replace("https:","http:"),url.replace("www.nowon.kr","127.0.0.1"),url+"#x"))assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000012","NOWON_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000012","NOWON_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000011","NOWON_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        var first=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for(String bad:List.of(first.uri()+"&q_fileSn=1",first.uri()+"#f",first.uri().toString().replace("www.nowon.kr","evil.example"),first.uri().toString().replace("ND_fileDownload","zipdownload")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(first.uri().toString().replace("q_fileSn=308827","q_fileSn=308828"))))).isFalse();
    }
    @Test void selectsOnlyBodyAndPreservesTitleGateAndFiniteBudget() throws Exception {
        var doc=Jsoup.parse(page(item(1,"pdf")));assertThat(NowonNoticePage.selectContent(doc,sample.profile().selectDetailUri(sample.source())).text()).isEqualTo("청년 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"청년 응시료 지원사업",sample.titleLayout());
        assertThatThrownBy(()->NowonNoticePage.selectContent(Jsoup.parse(page("").replace("class=txt","class=unknown")),sample.profile().selectDetailUri(sample.source()))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile());assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        var decision=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine().selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_NOWON_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlRecognizesOnlyTheMeasuredAccessibilityScript() throws Exception {
        String html=Files.readString(Path.of("build/qa-nowon-damyang-20260930/NOWON-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);assertThat(result.complete()).isTrue();assertThat(result.warnings()).isEmpty();
        assertThat(result.descriptors()).hasSize(2);assertThat(result.descriptors().getFirst().expectedFormat()).isEqualTo("HWP");assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        var changed=sample.profile().selectDescriptors(sample.source(),html.replace("button.attr('aria-label', variable);","button.attr('href', variable);"));
        assertThat(changed.complete()).isFalse();assertThat(changed.descriptors()).hasSize(2);
    }
    @Test void catalogIsReferenceOnly() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var refs=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(refs.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
    @Test void additionalSupportSampleKeepsTheOldFailureIndependentAndUsesExistingTitlePolicy() throws Exception {
        var support=NowonDownloadCases.selectSupportCase();
        assertThat(support.code()).isNotEqualTo(sample.code());
        assertThat(support.source().providerNoticeId()).isNotEqualTo(sample.source().providerNoticeId());
        assertThat(support.profile().selectProfileHash()).isEqualTo(sample.profile().selectProfileHash());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("NOWON_SUPPORT").map(s->s.code())).containsExactly(support.code());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("NOWON").map(s->s.code())).containsExactly(sample.code());
        var decision=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine().selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",support.title(),null,null,List.of(),com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var result=support.profile().selectDescriptors(support.source(),page(item(1,"pdf")+item(2,"hwp")+item(3,"hwp")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(support.listedFileCount());
        assertThat(result.descriptors()).extracting(d->d.expectedFormat()).containsExactly("PDF","HWP","HWP");
        assertThat(result.descriptors()).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(support.profile());
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var refs=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(refs.spliterator(),false).filter(r->support.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(support.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_NOWON_SURVEY_FIXTURE",matches="true")
    void observedZipSignatureWithHwpFilenameRemainsASeparateFormatFailure() throws Exception {
        var path=Path.of("build/qa-nowon-damyang-20260930/NOWON-file.bin");
        byte[] bytes=Files.readAllBytes(path);
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(bytes.length).isEqualTo(51156);
        assertThat(hash).isEqualTo("f9683fedcec21ae72f244cc049188b3fb36e9010d6eefc78598bcab0c88294c4");
        assertThat(Arrays.copyOf(bytes,4)).containsExactly((byte)'P',(byte)'K',(byte)3,(byte)4);
        var response=new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download(bytes.length,hash,null,"attachment; filename=notice.hwp");
        assertThatThrownBy(()->new com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator().selectFormat(path,response,"HWP"))
                .hasMessage("ATTACHMENT_FORMAT_MISMATCH");
    }
}
