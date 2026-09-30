package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 공식 고정 상세 5개와 포천 전송 대조를 진단한다. 최대6요청·5MiB+1KiB, 운영 성공 근거가 아니다. */
@EnabledIfEnvironmentVariable(named="SANEB_REGIONAL_TRANSPORT_DIAGNOSTIC", matches="true")
class RegionalTransportDiagnosticTest {
    private static final Map<String,String> URLS=Map.of(
            "POCHEON","https://www.pocheon.go.kr/www/selectEminwonView.do?key=3712&notAncmtMgtNo=64129&notAncmtSeCode=01",
            "GANGNEUNG","https://www.gn.go.kr/www/selectGosiNttView.do?key=263&gosiNttNo=60798&searchGosiSe=01,04,06",
            "CHUNGBUK","https://www.chungbuk.go.kr/www/selectGosiPblancView.do?key=422&no=67302",
            "GONGJU","https://www.gongju.go.kr/prog/saeolGosi/GOSI_03/sub04_03_03/view.do?notAncmtMgtNo=59971",
            "PYEONGTAEK","https://www.pyeongtaek.go.kr/pyeongtaek/saeol/gosi/view.do?mid=0401020100&notAncmtMgtNo=95902");
    @TempDir Path root;

    @org.junit.jupiter.api.Test
    @Timeout(15)
    void selectPocheonRawHttpControl() throws Exception {
        selectPocheonControl("TLSv1.3",true);
    }

    /** 포천 단일 호스트에서 TLS 버전별 handshake만 비교한다. HTTP·파일 요청은 하지 않는다. */
    @ParameterizedTest
    @ValueSource(strings={"TLSv1.2","TLSv1.3"})
    @Timeout(15)
    void selectPocheonTlsControlWithoutHttp(String protocol) throws Exception {
        selectPocheonControl(protocol,false);
    }

    private void selectPocheonControl(String protocol,boolean http) throws Exception {
        String oldType=System.getProperty("javax.net.ssl.trustStoreType"),oldStore=System.getProperty("javax.net.ssl.trustStore");
        var report=new LinkedHashMap<String,Object>();
        report.put("caseCode","POCHEON");report.put("protocolRequested",protocol);report.put("httpRequests",0);
        report.put("productionWriteCount",0);report.put("isAttachmentCollectionVerified",false);
        try {
            if(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows")) {
                System.setProperty("javax.net.ssl.trustStoreType","Windows-ROOT");System.setProperty("javax.net.ssl.trustStore","NONE");
            }
            URI uri=URI.create(URLS.get("POCHEON"));
            var validator=new ProviderContentUrlValidator(InetAddress::getAllByName);
            var target=validator.selectRequestTarget(uri,uri.getHost());
            var context=javax.net.ssl.SSLContext.getInstance("TLS");context.init(null,null,null);
            try(var tcp=new java.net.Socket()) {
                tcp.connect(new java.net.InetSocketAddress(target.selectPinnedAddressArray()[0],443),3000);
                try(var secure=(javax.net.ssl.SSLSocket)context.getSocketFactory().createSocket(tcp,uri.getHost(),443,true)) {
                    secure.setSoTimeout(5000);secure.setEnabledProtocols(new String[]{protocol});
                    var parameters=secure.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");
                    parameters.setServerNames(java.util.List.of(new javax.net.ssl.SNIHostName(uri.getHost())));secure.setSSLParameters(parameters);
                    secure.startHandshake();report.put("status","TLS_CONNECTED_NOT_COLLECTION_VERIFIED");report.put("protocol",secure.getSession().getProtocol());
                    if(http) {
                        String request="GET "+uri.getRawPath()+"?"+uri.getRawQuery()+" HTTP/1.1\r\nHost: "+uri.getHost()
                                +"\r\nUser-Agent: saneB-attachment-collector/1.0\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n";
                        report.put("httpRequests",1);
                        secure.getOutputStream().write(request.getBytes(java.nio.charset.StandardCharsets.US_ASCII));secure.getOutputStream().flush();
                        var line=new java.io.ByteArrayOutputStream();var input=secure.getInputStream();
                        while(line.size()<1024){int b=input.read();if(b<0||b=='\n')break;line.write(b);}
                        String status=line.toString(java.nio.charset.StandardCharsets.US_ASCII).strip();
                        report.put("status","HTTP_STATUS_OBSERVED_NOT_COLLECTION_VERIFIED");
                        report.put("httpStatus",status.matches("HTTP/1\\.[01] [0-9]{3}.*")?Integer.parseInt(status.substring(9,12)):0);
                    }
                }
            } catch(IOException failure) {
                report.put("status","FAILED");report.put("causeType",failure.getClass().getSimpleName());
                String message=failure.getMessage()==null?"":failure.getMessage().toLowerCase(java.util.Locale.ROOT);
                report.put("connectionReset",message.contains("connection reset"));report.put("handshakeTerminated",message.contains("terminated the handshake"));
                report.put("certificatePathFailure",message.contains("pkix")||message.contains("certification path"));
            }
        } finally {
            if(oldType==null)System.clearProperty("javax.net.ssl.trustStoreType");else System.setProperty("javax.net.ssl.trustStoreType",oldType);
            if(oldStore==null)System.clearProperty("javax.net.ssl.trustStore");else System.setProperty("javax.net.ssl.trustStore",oldStore);
        }
        System.out.println("REGIONAL_TLS_CONTROL "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(report));
    }

    @ParameterizedTest
    @ValueSource(strings={"POCHEON","GANGNEUNG","CHUNGBUK","GONGJU","PYEONGTAEK"})
    @Timeout(30)
    void selectFixedDetailTransportCategoryWithoutOriginalOutput(String code) throws Exception {
        String oldType=System.getProperty("javax.net.ssl.trustStoreType"),oldStore=System.getProperty("javax.net.ssl.trustStore");
        Path output=root.resolve("detail.bin");
        var report=new LinkedHashMap<String,Object>();
        report.put("caseCode",code);report.put("scope","FIXED_DETAIL_TRANSPORT_ONLY");
        report.put("productionWriteCount",0);report.put("isAttachmentCollectionVerified",false);
        int[] calls={0};
        try {
            if(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows")) {
                System.setProperty("javax.net.ssl.trustStoreType","Windows-ROOT");
                System.setProperty("javax.net.ssl.trustStore","NONE");
            }
            URI uri=URI.create(URLS.get(code));var initial=AttachmentPinnedDownloadClient.Request.selectGet(uri);
            var method=AttachmentPinnedDownloadClient.class.getDeclaredMethod("selectHttpResponse",
                    ProviderContentRequestTarget.class,AttachmentPinnedDownloadClient.Request.class,Duration.class,
                    AttachmentPinnedDownloadClient.ResponseHandler.class);
            method.setAccessible(true);
            try(var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(InetAddress::getAllByName),
                    (target,request,remaining,handler)->{
                        assertThat(++calls[0]).isEqualTo(1);assertThat(request).isEqualTo(initial);
                        AttachmentPinnedDownloadClient.ResponseHandler inspected=response->{
                            report.put("httpStatus",response.status());return handler.handle(response);
                        };
                        try{return (AttachmentPinnedDownloadClient.Reply)method.invoke(null,target,request,remaining,inspected);}
                        catch(IllegalAccessException e){throw new IOException("DIAGNOSTIC_ACCESS_FAILED");}
                        catch(InvocationTargetException e){if(e.getCause() instanceof IOException failure)throw failure;throw new IOException("DIAGNOSTIC_INVOCATION_FAILED");}
                    },Duration.ofSeconds(15))) {
                try {
                    var result=client.selectDownload(initial,Set.of(uri.getHost()),initial::equals,output,1048576,bytes->true);
                    report.put("status","DETAIL_TRANSFERRED_NOT_COLLECTION_VERIFIED");report.put("bytes",result.bytes());report.put("sha256",result.sha256());
                } catch(IOException failure) {
                    report.put("status","FAILED");
                    String message=failure.getMessage();
                    report.put("errorCode",message!=null&&message.matches("[A-Z0-9_]{1,100}")?message:"UNCLASSIFIED_TRANSPORT_FAILURE");
                    var causes=new java.util.ArrayList<String>();boolean pkix=false,handshake=false;
                    Throwable cause=failure;
                    for(int i=0;cause!=null&&i<8;i++,cause=cause.getCause()) {
                        String type=cause.getClass().getSimpleName();
                        if(type.matches("[A-Za-z0-9]{1,100}"))causes.add(type);
                        String detail=cause.getMessage();
                        pkix|=detail!=null&&(detail.contains("PKIX path building failed")||detail.contains("unable to find valid certification path"));
                        handshake|=cause instanceof javax.net.ssl.SSLHandshakeException;
                    }
                    report.put("causeTypes",causes);report.put("certificatePathFailure",pkix);report.put("tlsHandshakeFailure",handshake);
                }
            }
        } finally {
            Files.deleteIfExists(output);
            if(oldType==null)System.clearProperty("javax.net.ssl.trustStoreType");else System.setProperty("javax.net.ssl.trustStoreType",oldType);
            if(oldStore==null)System.clearProperty("javax.net.ssl.trustStore");else System.setProperty("javax.net.ssl.trustStore",oldStore);
        }
        assertThat(output).doesNotExist();assertThat(calls[0]).isLessThanOrEqualTo(1);
        report.put("transportInvocations",calls[0]);report.put("originalFilesRemoved",true);
        System.out.println("REGIONAL_TRANSPORT_DIAGNOSTIC "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(report));
    }
}
