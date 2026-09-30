package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.*;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.NamhaeNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 남해 _view 상세 계약을 기존 SCMS 첨부 해석기에 연결한다. 변환 URI는 네트워크에 요청하지 않는다. */
@Component
public final class NamhaeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final AttachmentDiscoveryProfile delegate=new ScmsSaeolAttachmentDiscoveryProfile("NAMHAE","namhae.go.kr","LGS-000236","SCMS_CARD_NOTICE",NamhaeNoticePage.PATH,"/emwp/jsp/ofr/FileDown.jsp",true,false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("NAMHAE:1|fixed-module-view|no-ephemeral-storage|same-request|"+delegate.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",NamhaeNoticePage.class),getClass());
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000236".equals(source.localSourceCode())
                    ||!"SCMS_CARD_NOTICE".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return NamhaeNoticePage.selectCanonicalDetail(URI.create(source.sourceUrl()),false);
        }catch(IllegalArgumentException exception){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(uri==null)return false;
        if(NamhaeNoticePage.HOST.equals(uri.getHost()))try{return NamhaeNoticePage.selectCanonicalDetail(uri,false).equals(uri);}catch(IllegalArgumentException exception){return false;}
        return delegate.selectApprovedRequest(uri);
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI actual=selectDetailUri(source);
        // 공식 URI의 고정 필드 검증 후 해석기 내부에만 amode=view 표현을 전달한다.
        String id=actual.getRawQuery().split("&")[1].substring("not_ancmt_mgt_no=".length());
        String virtual="https://"+NamhaeNoticePage.HOST+NamhaeNoticePage.PATH+"?amode=view&not_ancmt_mgt_no="+id;
        return delegate.selectDescriptors(new Source(selectProviderCode(),normalizer.hash(normalizer.canonicalizeUrl(virtual)),virtual,source.localSourceCode(),source.listParserProfileCode()),html);
    }
}
