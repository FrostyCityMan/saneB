package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnnouncementSourceProviderAttachmentExclusionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bizInfoExcludesAttachmentMetadataFromItemPayloadAndHash() throws Exception {
        BizInfoAnnouncementSourceProviderClient client = new BizInfoAnnouncementSourceProviderClient(
                objectMapper, "https://example.go.kr/bizinfo", "test-key", 1000
        );
        JsonNode first = objectMapper.readTree("""
                {
                  "pblancId":"BIZ-1",
                  "pblancNm":"소상공인 지원",
                  "pblancUrl":"https://example.go.kr/notices/1",
                  "bsnsSumryCn":"지원 본문",
                  "fileNm":"공고문.pdf",
                  "flpthNm":"https://files.example.go.kr/a.pdf",
                  "nested":{"printFileNm":"신청서.hwp","printFlpthNm":"https://files.example.go.kr/a.hwp"}
                }
                """);
        JsonNode second = objectMapper.readTree("""
                {
                  "pblancId":"BIZ-1",
                  "pblancNm":"소상공인 지원",
                  "pblancUrl":"https://example.go.kr/notices/1",
                  "bsnsSumryCn":"지원 본문",
                  "fileNm":"변경공고문.pdf",
                  "flpthNm":"https://files.example.go.kr/b.pdf",
                  "nested":{"printFileNm":"변경신청서.hwp","printFlpthNm":"https://files.example.go.kr/b.hwp"}
                }
                """);

        AnnouncementSourceProviderItem firstItem = client.selectProviderItem(first);
        AnnouncementSourceProviderItem secondItem = client.selectProviderItem(second);

        assertThat(firstItem.attachments()).isEmpty();
        assertThat(firstItem.rawPayloadJson())
                .contains("소상공인 지원")
                .doesNotContain("fileNm", "flpthNm", "printFileNm", "printFlpthNm", "files.example.go.kr");
        assertThat(firstItem.rawHash()).isEqualTo(secondItem.rawHash());
    }

    @Test
    void gov24ExcludesAttachmentMetadataFromItemPayload() throws Exception {
        Gov24PublicServiceAnnouncementSourceProviderClient client =
                new Gov24PublicServiceAnnouncementSourceProviderClient(
                        objectMapper, "https://example.go.kr/gov24", "test-key", 1000
                );
        JsonNode node = objectMapper.readTree("""
                {
                  "서비스ID":"GOV-1",
                  "서비스명":"정책자금 지원",
                  "상세조회URL":"https://example.go.kr/services/1",
                  "지원내용":"정책자금 본문",
                  "첨부파일명":"안내서.pdf",
                  "첨부파일URL":"https://files.example.go.kr/guide.pdf",
                  "nested":{"fileName":"양식.hwp","fileUrl":"https://files.example.go.kr/form.hwp"}
                }
                """);

        AnnouncementSourceProviderItem item = client.selectProviderItem(node);

        assertThat(item.attachments()).isEmpty();
        assertThat(item.rawPayloadJson())
                .contains("정책자금 지원")
                .doesNotContain("첨부파일명", "첨부파일URL", "fileName", "fileUrl", "files.example.go.kr");
    }
}
