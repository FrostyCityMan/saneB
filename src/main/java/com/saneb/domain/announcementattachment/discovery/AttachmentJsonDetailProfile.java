package com.saneb.domain.announcementattachment.discovery;

import java.io.IOException;
import java.io.InputStream;

/** 명시적으로 등록한 JSON 기관만 사용한다. 기존 HTML 프로필의 MIME/문자 인코딩 계약은 바꾸지 않는다. */
public interface AttachmentJsonDetailProfile {
    String selectJsonPayload(InputStream stream, String contentType) throws IOException;
    String selectJsonTitle(AttachmentDiscoveryProfile.Source source, String payload);
}
