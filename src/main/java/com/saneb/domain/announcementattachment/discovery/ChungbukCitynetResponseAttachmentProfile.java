package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 충북의 실측 Citynet application/file 응답만 기존 정규화기로 검증한다. */
final class ChungbukCitynetResponseAttachmentProfile implements AttachmentDownloadFlowProfile {
    private final AttachmentDiscoveryProfile delegate;
    private final String hash;
    ChungbukCitynetResponseAttachmentProfile(AttachmentDiscoveryProfile delegate) {
        if(delegate==null || !"LOCAL_CHUNGBUK_BOARD_V1".equals(delegate.selectProfileCode())
                || delegate instanceof AttachmentDownloadFlowProfile) throw new IllegalArgumentException("CHUNGBUK_PROFILE_REQUIRED");
        this.delegate=delegate;
        hash=AttachmentProfileFingerprint.selectHash("CHUNGBUK_CITYNET_RESPONSE:1|"+delegate.selectProfileHash()+"|"
                +AttachmentProfileFingerprint.selectHash("CITYNET_RESPONSE:1",CitynetAttachmentFileResponse.class),getClass());
    }
    @Override public Download selectDownload(Request initial,Path output,long maximumBytes,Operation operation)throws IOException {
        if(!selectApprovedRequest(initial))throw new IOException("ATTACHMENT_PATH_NOT_APPROVED");
        return CitynetAttachmentFileResponse.selectNormalized(operation.selectDownload(initial,maximumBytes),output);
    }
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public Set<String> selectLegacyBinaryContentTypes(){return Set.of(CitynetAttachmentFileResponse.LEGACY_MIME);}
    @Override public URI selectDetailUri(String id){return delegate.selectDetailUri(id);}
    @Override public URI selectDetailUri(Source source){return delegate.selectDetailUri(source);}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(Request request){return delegate.selectApprovedRequest(request);}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return delegate.selectApprovedRequest(initial,next);}
    @Override public Result selectDescriptors(String id,String html){return delegate.selectDescriptors(id,html);}
    @Override public Result selectDescriptors(Source source,String html){return delegate.selectDescriptors(source,html);}
}
