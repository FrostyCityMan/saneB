package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.Map;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 시스템 코드로 배포하는 프로필. 관리자 입력 URL/selector/header를 실행하지 않습니다. */
public interface AttachmentDiscoveryProfile {
    String selectProviderCode();
    String selectProfileCode();
    String selectProfileHash();
    /** 시스템 기관/목록 parser 결합만 선언한다. 이 일치는 상세 URL·실파일 QA 성공을 뜻하지 않는다. */
    default List<SourceBinding> selectSourceBindings() { return List.of(); }
    record SourceBinding(String localSourceCode, String listParserProfileCode) { }
    Set<String> selectApprovedHosts();
    /** 실측한 구형 서버에만 적용한다. 일반 응답의 헤더 charset을 추측하지 않는다. */
    default boolean selectUtf8DispositionOctets() { return false; }
    /** 실측한 기관의 오기/구형 binary MIME만 고정한다. 관리자 입력이나 text/html 허용은 금지한다. */
    default Set<String> selectLegacyBinaryContentTypes() { return Set.of(); }
    URI selectDetailUri(String providerNoticeId);
    boolean selectApprovedRequest(URI uri);
    default boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request request) {
        return request != null && "GET".equals(request.method()) && selectApprovedRequest(request.uri());
    }
    Result selectDescriptors(String providerNoticeId, String html);

    /** URL hash를 notice ID로 쓰는 지자체는 시스템 source 연결과 실제 상세 주소를 함께 검증한다. */
    default URI selectDetailUri(Source source) {
        validateProvider(source);
        return selectDetailUri(source.providerNoticeId());
    }
    default Result selectDescriptors(Source source, String html) {
        validateProvider(source);
        return selectDescriptors(source.providerNoticeId(), html);
    }
    private void validateProvider(Source source) {
        if (source == null || !selectProviderCode().equals(source.providerCode()))
            throw new IllegalArgumentException("PROFILE_REQUIRED");
    }
    record Source(String providerCode, String providerNoticeId, String sourceUrl,
                  String localSourceCode, String listParserProfileCode) {
        @Override public String toString() { return "AttachmentDiscoverySource[requestValues=REDACTED]"; }
    }

    record Descriptor(URI fetchUri, AttachmentSetEvidence.Locator locator, String displayName,
                      String expectedFormat, String documentRole, boolean downloadAllowed, Map<String, String> postForm) {
        public Descriptor { postForm = Map.copyOf(postForm); }
        public Descriptor(URI fetchUri, AttachmentSetEvidence.Locator locator, String displayName,
                          String expectedFormat, String documentRole, boolean downloadAllowed) {
            this(fetchUri, locator, displayName, expectedFormat, documentRole, downloadAllowed, Map.of());
        }
        public AttachmentPinnedDownloadClient.Request selectRequest() {
            return new AttachmentPinnedDownloadClient.Request(fetchUri, postForm.isEmpty() ? "GET" : "POST", postForm);
        }
        @Override public String toString() { return "AttachmentDescriptor[requestValues=REDACTED]"; }
    }
    record Result(String status, boolean complete, List<Descriptor> descriptors, List<String> warnings) {
        public Result { descriptors = List.copyOf(descriptors); warnings = List.copyOf(warnings); }
    }
}
