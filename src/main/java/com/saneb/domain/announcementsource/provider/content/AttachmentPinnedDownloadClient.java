package com.saneb.domain.announcementsource.provider.content;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

/** 시스템 QA allowlist를 벗어난 redirect를 따라가지 않는 bounded streaming GET. */
@Component
public class AttachmentPinnedDownloadClient {
    public record Download(long bytes,String sha256,String contentType) { }
    private final ProviderContentUrlValidator validator;
    public AttachmentPinnedDownloadClient() { this(new ProviderContentUrlValidator(InetAddress::getAllByName)); }
    AttachmentPinnedDownloadClient(ProviderContentUrlValidator validator) { this.validator=validator; }

    public Download selectDownload(URI uri,Set<String> approvedHosts,Path output,long remainingSourceBytes) throws IOException {
        long byteLimit=Math.min(20L*1024*1024,remainingSourceBytes);
        if (byteLimit<=0) throw new IOException("SOURCE_BYTE_LIMIT");
        URI current=uri;
        boolean[] createdOutput={false};
        try {
            for (int redirects=0;redirects<=3;redirects++) {
                if (!"https".equalsIgnoreCase(current.getScheme()) || current.getHost()==null
                        || !approvedHosts.contains(current.getHost().toLowerCase(java.util.Locale.ROOT)))
                    throw new IOException("ATTACHMENT_HOST_NOT_APPROVED");
                var validated=validator.selectValidatedRequest("https://"+current.getHost(),current.toASCIIString());
                var target=validator.selectRequestTarget(validated.detailUri(),validated.allowedHost());
                var manager=PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(new PinnedProviderContentHttpTransport.PinnedDnsResolver(target))
                        .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(3))
                                .setSocketTimeout(Timeout.ofSeconds(10)).build())
                        .setMaxConnTotal(1).setMaxConnPerRoute(1).build();
                HttpGet request=new HttpGet(target.uri());
                request.setConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(10))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(3)).setRedirectsEnabled(false).build());
                request.setHeader("Accept-Encoding","identity");
                request.setHeader("User-Agent","saneB-attachment-collector/1.0");
                try (var watchdog=Executors.newSingleThreadScheduledExecutor();
                     var client=HttpClients.custom().setConnectionManager(manager).disableRedirectHandling()
                             .disableAutomaticRetries().disableCookieManagement().disableContentCompression().build()) {
                    watchdog.schedule(request::cancel,30,TimeUnit.SECONDS);
                    Reply reply;
                    try { reply=client.execute(request,response -> {
                        int status=response.getCode();
                        if (Set.of(301,302,303,307,308).contains(status)) {
                            var location=response.getFirstHeader("Location");
                            if (location==null) throw new IOException("ATTACHMENT_REDIRECT_INVALID");
                            return new Reply(location.getValue(),null);
                        }
                        if (status!=200) throw new IOException("ATTACHMENT_HTTP_"+status);
                        var entity=response.getEntity();
                        if (entity==null || entity.getContentLength()>byteLimit) throw new IOException("ATTACHMENT_BYTE_LIMIT");
                        var encoding=response.getFirstHeader("Content-Encoding");
                        if (encoding!=null && !"identity".equalsIgnoreCase(encoding.getValue())) throw new IOException("ATTACHMENT_ENCODING_UNSUPPORTED");
                        MessageDigest digest;
                        try { digest=MessageDigest.getInstance("SHA-256"); } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
                        long total=0;
                        try (var input=entity.getContent(); var file=Files.newOutputStream(output,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
                            createdOutput[0]=true;
                            byte[] buffer=new byte[8192];
                            for (int count;(count=input.read(buffer))!=-1;) {
                                total+=count;
                                if (total>byteLimit) throw new IOException("ATTACHMENT_BYTE_LIMIT");
                                file.write(buffer,0,count); digest.update(buffer,0,count);
                            }
                        }
                        if (total==0) throw new IOException("ATTACHMENT_EMPTY_FILE");
                        String type=entity.getContentType();
                        return new Reply(null,new Download(total,HexFormat.of().formatHex(digest.digest()),type));
                    }); } finally { watchdog.shutdownNow(); }
                    if (reply.download()!=null) return reply.download();
                    current=current.resolve(reply.redirect());
                }
            }
            throw new IOException("ATTACHMENT_REDIRECT_LIMIT");
        } catch (ProviderContentValidationException | IllegalArgumentException exception) {
            throw new IOException("ATTACHMENT_URL_BLOCKED");
        } catch (IOException exception) {
            // 호출자가 소유한 UUID 임시 디렉터리의 신규 파일만 정리한다.
            if (createdOutput[0]) Files.deleteIfExists(output);
            throw exception;
        }
    }
    private record Reply(String redirect,Download download) { }
}
