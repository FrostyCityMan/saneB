package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** 고정 공개 공고 A/B 진단. 전체 최대5요청·7MiB, 원문·URL·헤더 출력과 정책 승인은 없다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_HTTP400_DIAGNOSTIC", matches="true")
class AttachmentHttp400DiagnosticTest {
    @TempDir Path root;
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    @org.junit.jupiter.api.Timeout(90)
    void selectChungjuTransportFailureCategoryWithoutOriginalOutput(boolean withBodyAndDetail) throws Exception {
        String oldType=System.getProperty("javax.net.ssl.trustStoreType"),oldStore=System.getProperty("javax.net.ssl.trustStore");
        try {
        if(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows")) {
            System.setProperty("javax.net.ssl.trustStoreType","Windows-ROOT");
            System.setProperty("javax.net.ssl.trustStore","NONE");
        }
        System.out.println("HTTP400_DIAGNOSTIC withBodyAndDetail="+withBodyAndDetail+" javaRuntime="+System.getProperty("java.runtime.version"));
        String name="공고문.hwp", system="공고문_ofr_ofr_u7taq8IU0lrV4gGu_20260428133101602_1.hwp";
        URI uri=URI.create("https://eminwon.chungju.go.kr/emwp/jsp/ofr/FileDown.jsp?file_path="
                +URLEncoder.encode("/ntishome/file/upload/ofr/ofr/20260428",StandardCharsets.UTF_8)
                +"&sys_file_nm="+URLEncoder.encode(system,StandardCharsets.UTF_8)
                +"&user_file_nm="+URLEncoder.encode(name,StandardCharsets.UTF_8));
        var method=AttachmentPinnedDownloadClient.class.getDeclaredMethod("selectHttpResponse",
                ProviderContentRequestTarget.class,AttachmentPinnedDownloadClient.Request.class,Duration.class,
                AttachmentPinnedDownloadClient.ResponseHandler.class);
        method.setAccessible(true);
        int[] calls={0};Path output=root.resolve("file.bin"),detail=root.resolve("detail.bin");
        String detailUrl="https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=70852";
        var profile=new com.saneb.domain.announcementattachment.discovery.ChungjuEminwonAttachmentDiscoveryProfile();
        try(var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(InetAddress::getAllByName),
                (target,request,remaining,handler)->{
                    assertThat(++calls[0]).isLessThanOrEqualTo(withBodyAndDetail?2:1);
                    assertThat(request.uri()).isIn(uri,URI.create(detailUrl));
                    AttachmentPinnedDownloadClient.ResponseHandler inspected=response->{
                        if(response.status()==400 && response.body()!=null) {
                            String text=new String(response.body().readNBytes(2048),StandardCharsets.UTF_8);
                            System.out.println("HTTP400_DIAGNOSTIC invalidTarget="+text.contains("Invalid character found in the request target")
                                    +" invalidProtocol="+text.contains("Invalid character found in the HTTP protocol")
                                    +" tomcat="+text.contains("Apache Tomcat")+" fileMissing="+text.contains("FileNotFoundException"));
                        }
                        return handler.handle(response);
                    };
                    try{return (AttachmentPinnedDownloadClient.Reply)method.invoke(null,target,request,remaining,inspected);}
                    catch(IllegalAccessException e){throw new IOException("DIAGNOSTIC_ACCESS_FAILED");}
                    catch(InvocationTargetException e){if(e.getCause() instanceof IOException failure)throw failure;throw new IOException("DIAGNOSTIC_INVOCATION_FAILED");}
                },Duration.ofSeconds(30))) {
            try {
                var selected=AttachmentPinnedDownloadClient.Request.selectGet(uri);
                if(withBodyAndDetail) {
                    var bodyClient=new LocalGovernmentNoticeProviderContentClient(true,3000,7000,1048576,0,1,"saneB-notice-collector/1.0");
                    var body=bodyClient.selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",java.util.UUID.randomUUID(),
                            "https://www.chungju.go.kr/www/selectEminwonList.do?key=510",detailUrl));
                    System.out.println("HTTP400_DIAGNOSTIC bodyStatus="+body.statusCode()+" bodyAttempts="+body.attemptCount());
                    client.selectDownload(URI.create(detailUrl),Set.of("www.chungju.go.kr"),detail,1048576);
                    String html;try(var input=Files.newInputStream(detail)){html=org.jsoup.Jsoup.parse(input,null,detailUrl).outerHtml();}
                    var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
                    var source=new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                            n.hash(n.canonicalizeUrl(detailUrl)),detailUrl,"LGS-000137","SAEOL_GOSI");
                    var found=profile.selectDescriptors(source,html);assertThat(found.descriptors()).hasSize(1);
                    selected=found.descriptors().getFirst().selectRequest();assertThat(selected.uri()).isEqualTo(uri);
                }
                var initial=selected;
                var result=com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow.selectDownload(profile,initial,output,2097152,
                        (request,limit,approved)->client.selectDownload(request,profile.selectApprovedHosts(),approved,output,limit,bytes->true));
                System.out.println("HTTP400_DIAGNOSTIC status=DOWNLOADED bytes="+result.bytes()+" sha256="+result.sha256());
            } catch(IOException e) {
                String code=e instanceof javax.net.ssl.SSLException?"TLS_FAILED":e.getMessage();
                if(code==null||!code.matches("[A-Z0-9_]{1,100}"))code="UNCLASSIFIED_TRANSPORT_FAILURE";
                System.out.println("HTTP400_DIAGNOSTIC status="+code);
            }
        } finally {Files.deleteIfExists(output);Files.deleteIfExists(detail);}
        assertThat(output).doesNotExist();assertThat(detail).doesNotExist();assertThat(calls[0]).isEqualTo(withBodyAndDetail?2:1);
        System.out.println("HTTP400_DIAGNOSTIC originalRemoved=true policyApproved=false operatingVerified=false");
        } finally {
            if(oldType==null)System.clearProperty("javax.net.ssl.trustStoreType");else System.setProperty("javax.net.ssl.trustStoreType",oldType);
            if(oldStore==null)System.clearProperty("javax.net.ssl.trustStore");else System.setProperty("javax.net.ssl.trustStore",oldStore);
        }
    }
}
