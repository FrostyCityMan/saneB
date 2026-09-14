package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;

/** 프로필뿐 아니라 공통 요청 경계 변경도 게시된 profile hash와 구분한다. */
public final class AttachmentProfileFingerprint {
    private AttachmentProfileFingerprint() { }
    public static String selectHash(String definition, Class<?> implementation) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(definition.getBytes(StandardCharsets.UTF_8));
            var classes = new LinkedHashSet<>(List.of(implementation, AttachmentProfileFingerprint.class,
                    AttachmentDiscoveryProfile.class, AttachmentDiscoveryProfile.Descriptor.class, AttachmentDiscoveryProfile.Source.class, AttachmentDiscoveryProfile.SourceBinding.class, AttachmentDownloadInvocation.class,
                    com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator.class,
                    com.saneb.domain.announcementattachment.worker.AttachmentDownloadGateway.class,
                    AttachmentDownloadFlowProfile.class, AttachmentDownloadFlowProfile.Operation.class, AttachmentProfileDownloadFlow.class, AttachmentProfileDownloadFlow.Transport.class,
                    AttachmentPinnedDownloadClient.class, AttachmentPinnedDownloadClient.Request.class));
            for (Class<?> type : classes) {
                String binaryName = type.getName().substring(type.getName().lastIndexOf('.') + 1) + ".class";
                try (var stream = type.getResourceAsStream(binaryName)) {
                    if (stream == null) throw new IllegalStateException("첨부 프로필 실행 코드를 확인할 수 없습니다.");
                    byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
                    if (bytes.length > 1024 * 1024) throw new IllegalStateException("첨부 프로필 실행 코드가 한도를 초과했습니다.");
                    digest.update(type.getName().getBytes(StandardCharsets.UTF_8));
                    digest.update(bytes);
                }
            }
            // package-private 검증기의 가시성을 넓히지 않고 같은 패키지의 class resource를 고정한다.
            try (var stream = AttachmentPinnedDownloadClient.class.getResourceAsStream("ProviderContentUrlValidator.class")) {
                if (stream == null) throw new IllegalStateException("첨부 URL 검증 코드를 확인할 수 없습니다.");
                byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) throw new IllegalStateException("첨부 URL 검증 코드가 한도를 초과했습니다.");
                digest.update("ProviderContentUrlValidator".getBytes(StandardCharsets.UTF_8));
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("첨부 프로필 실행 hash를 확인할 수 없습니다.");
        }
    }
}
