package com.saneb.domain.announcementsource.provider.content;

/**
 * provider 공식 상세 페이지에서 분류용 본문만 조회하는 내부 계약입니다.
 */
public interface ProviderContentClient {

    String selectProviderCode();

    boolean isEnabled();

    ProviderContentResult selectContent(ProviderContentRequest request);
}
