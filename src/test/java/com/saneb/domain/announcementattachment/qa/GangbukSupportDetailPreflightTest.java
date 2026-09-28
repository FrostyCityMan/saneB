package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** 지원사업 후보 한 건의 상세 식별/발견만 관측. 첨부 다운로드·추출·본문 판정·기대값 승인은 별도다. */
class GangbukSupportDetailPreflightTest {
    static final String ID="179490";
    static final String TITLE="2026년 청년 어학・자격시험 응시료 지원 사업 모집 공고";
    static final String URL="https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId="+ID;
    @TempDir Path directory;

    static void validateIdentity(Document document) {
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,TITLE,
                AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout.GANGBUK_SUBJECT);
    }
    private static String fixture(){return "<form id='board'><input type='hidden' name='nttId' value='"+ID+"'><div class='bd-view'><h3 class='bd-view__subject'>"+TITLE+"</h3></div></form>";}
    @Test void titleIsBoundToDirectUniqueFormAndNoticeId() {
        validateIdentity(Jsoup.parse(fixture()));validateIdentity(Jsoup.parse(fixture().replace("청년 ","청년  ")));
        for(String invalid:List.of(fixture()+fixture(),fixture().replace(ID,"179491"),fixture().replace(TITLE,"다른 제목"),
                fixture().replace("type='hidden'","type='text'"),fixture().replace("<input ","<div><input ").replace("<div class='bd-view'>","</div><div class='bd-view'>"),
                fixture().replace("</h3>","</h3><h3 class='bd-view__subject'>"+TITLE+"</h3>")))
            assertThrows(AssertionError.class,()->validateIdentity(Jsoup.parse(invalid)));
    }

    @Test @Timeout(90)
    @EnabledIfEnvironmentVariable(named="SANEB_GANGBUK_SUPPORT_DETAIL_PREFLIGHT",matches="true")
    void observesOneFixedDetailWithNoRedirectRetryOrAttachmentDownload() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var title=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE",TITLE,null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
        assertTrue(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title),"TITLE_NOT_ELIGIBLE_NOT_FETCHED");
        var profile=new LegalBoardAttachmentProfileConfiguration().selectGangbukLegalProfileDetails();
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(URL)),URL,"LGS-000010","SPRING_BBS");
        var initial=AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source));
        Path root=Path.of("build/qa-results"),reservation=root.resolve("gangbuk-support-detail-179490-reservation.json");
        Files.createDirectories(root);
        // 실패/중단도 예약을 남긴다. 같은 표본을 새 모드/재실행으로 자동 재요청하지 않는다.
        Files.writeString(reservation,new ObjectMapper().writeValueAsString(Map.of("caseCode","GANGBUK-"+ID,
                "maximumRequests",1,"maximumBytes",1048576,"maximumNetworkSeconds",30,"startedAt",Instant.now().toString())),StandardOpenOption.CREATE_NEW);
        var report=new LinkedHashMap<String,Object>();var attempts=new AtomicInteger();var bytes=new java.util.concurrent.atomic.AtomicLong();
        report.put("caseCode","GANGBUK-"+ID);report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("titleStage",title.titleStageCode());report.put("rulesSource","EPHEMERAL_DB_DRAFT_SEED");
        report.put("rulesHash",AnnouncementAttachmentOfficialObservationTest.selectHash(rules));
        report.put("status","FAILED");report.put("productionWriteCount",0);report.put("attachmentDownloads",0);
        report.put("isExpectationApproved",false);report.put("isPolicyQaPassed",false);report.put("bodyClassificationExecuted",false);
        Path detail=directory.resolve("detail.html");String stage="DETAIL_FETCH";
        try(var client=new AttachmentPinnedDownloadClient()) {
            var download=client.selectDownload(initial,profile.selectApprovedHosts(),
                    request->initial.equals(request)&&profile.selectApprovedRequest(request)&&attempts.compareAndSet(0,1),detail,1048576,
                    count->{if(count<0||bytes.get()>1048576-count)return false;bytes.addAndGet(count);return true;});
            assertTrue(download.contentType()!=null&&download.contentType().toLowerCase(Locale.ROOT).startsWith("text/html"),"DETAIL_CONTENT_TYPE_CHANGED");
            report.put("detailHash",download.sha256());stage="TITLE_IDENTITY";
            Document page;try(var stream=Files.newInputStream(detail)){page=Jsoup.parse(stream,null,URL);}
            validateIdentity(page);report.put("fixedTitleVerified",true);stage="ATTACHMENT_DISCOVERY";
            var discovered=profile.selectDescriptors(source,page.outerHtml());
            report.put("discoveryStatus",discovered.status());report.put("discoveryComplete",discovered.complete());
            report.put("discoveredCount",discovered.descriptors().size());report.put("warnings",discovered.warnings());
            var files=new ArrayList<Map<String,Object>>();
            for(var d:discovered.descriptors()){var file=new LinkedHashMap<String,Object>();
                file.put("locatorHash",AnnouncementAttachmentOfficialObservationTest.selectHash(d.locator()));
                file.put("formatHint",d.expectedFormat());file.put("downloadAllowed",d.downloadAllowed());files.add(file);}
            report.put("files",files);
            assertTrue(discovered.complete()&&"FOUND".equals(discovered.status()),"DISCOVERY_INCOMPLETE");
            report.put("status","DETAIL_IDENTITY_AND_DISCOVERY_OBSERVED");
        } catch(Exception|AssertionError failure) {
            report.put("failedStage",stage);report.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));
            throw new AssertionError("GANGBUK_DETAIL_PREFLIGHT_FAILED:"+stage);
        } finally {
            Files.deleteIfExists(detail);report.put("temporaryOriginalRemoved",!Files.exists(detail));report.put("requestReservations",attempts.get());
            report.put("reservedBytes",bytes.get());report.put("finishedAt",Instant.now().toString());
            Files.writeString(root.resolve("gangbuk-support-detail-179490-result.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report),StandardOpenOption.CREATE_NEW);
        }
    }
}
