package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 금산에서 실측한 bare hwp 응답만 추가 보정한다. 공통 MIME 허용 목록은 바꾸지 않는다. */
final class GeumsanFileResponseAttachmentProfile implements AttachmentDownloadFlowProfile, AttachmentDetailLimitProfile {
    private static final String LEGACY_MIME="application/x-msdownload";
    private final LegacyFileResponseAttachmentDiscoveryProfile delegate=new LegacyFileResponseAttachmentDiscoveryProfile(
            new ChungcheongFifthAttachmentDiscoveryProfile(Site.GEUMSAN),"BARE_HWPX");
    private final String hash=AttachmentProfileFingerprint.selectHash(
            "GEUMSAN_BARE_HWP:1|"+delegate.selectProfileHash()+"|one-request|hwp-signature-and-attachment-name-required",getClass());

    @Override public Download selectDownload(Request initial, Path output, long maximumBytes, Operation operation) throws IOException {
        Download response=delegate.selectDownload(initial,output,maximumBytes,operation);
        String mime=response.contentType()==null?"":response.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT);
        if(!"hwp".equals(mime))return response;
        // 내부 검증 표현만 정규화한다. 실제 서버 MIME이 표준 MIME이었다고 기록하지 않는다.
        Download normalized=new Download(response.bytes(),response.sha256(),LEGACY_MIME,response.contentDisposition());
        new AttachmentFileTypeValidator().selectFormat(output,normalized,"HWP",delegate.selectUtf8DispositionOctets(),Set.of(LEGACY_MIME));
        return normalized;
    }

    @Override public long selectDetailMaximumBytes(){return delegate.selectDetailMaximumBytes();}
    @Override public boolean selectUtf8DispositionOctets(){return delegate.selectUtf8DispositionOctets();}
    @Override public Set<String> selectLegacyBinaryContentTypes(){return delegate.selectLegacyBinaryContentTypes();}
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public URI selectDetailUri(String id){return delegate.selectDetailUri(id);}
    @Override public URI selectDetailUri(Source source){return delegate.selectDetailUri(source);}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(Request request){return delegate.selectApprovedRequest(request);}
    @Override public boolean selectApprovedRequest(Request initial,Request request){return delegate.selectApprovedRequest(initial,request);}
    @Override public Result selectDescriptors(String id,String html){return delegate.selectDescriptors(id,html);}
    @Override public Result selectDescriptors(Source source,String html){return delegate.selectDescriptors(source,html);}
}
